package model.collections.plant;

import model.collections.armour.ArmourFactory;
import model.collections.armour.ArmourType;
import model.collections.armour.PlantArmour;
import model.collections.plant.actstrategy.*;
import model.collections.plant.plantfood.*;
import model.match_mechanisms.vector.Position;
import model.utils.ResourceResolver;
import view.GeneralPrinter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlantFactory {

    private static Map<Integer, PlantJsonParser.PlantConfig> blueprints = new HashMap<>();
    private static boolean loaded = false;

    public static void init(InputStream jsonStream) {
        blueprints = PlantJsonParser.loadConfigs(jsonStream);
        loaded = true;
    }

    public static void autoInit() {
        if (loaded) return;
        try (java.io.InputStream is = ResourceResolver.open("Plants.json")) {
            if (is != null) {
                init(is);
                return;
            }
        } catch (java.io.IOException e) {
            GeneralPrinter.print("Could not load Plants.json: " + e.getMessage());
        }
        GeneralPrinter.print("Could not find Plants.json in any known location.");
    }

    public static Map<Integer, PlantJsonParser.PlantConfig> getBlueprints() {
        autoInit();
        return blueprints;
    }

    public static Plant createPlant(int id, int level, Position position) {
        autoInit();
        PlantJsonParser.PlantConfig config = blueprints.get(id);
        if (config == null) {
            throw new IllegalArgumentException("Plant ID " + id + " does not exist in dataset.");
        }

        int runtimeHp = config.baseHp;
        int runtimeCost = config.cost;
        double runtimeInterval = config.actionInterval;
        int runtimeDamage = config.damage;
        double runtimeRecharge = config.recharge;
        double runtimeAbility = config.abilityValue;
        List<String> specialTags = new ArrayList<>();

        if (config.upgrades != null) {
            for (PlantJsonParser.UpgradeConfig upgrade : config.upgrades) {
                if (upgrade.level <= level) {
                    switch (upgrade.type) {
                        case BUFF_HP -> runtimeHp += (int) upgrade.value;
                        case BUFF_COST -> runtimeCost += (int) upgrade.value;
                        case BUFF_ACTION_INTERVAL -> runtimeInterval += upgrade.value;
                        case BUFF_DAMAGE -> runtimeDamage += (int) upgrade.value;
                        case BUFF_RECHARGE -> runtimeRecharge += upgrade.value;
                        case SPECIAL_MECHANIC -> specialTags.add(upgrade.specialTag);
                    }
                }
            }
        }

        boolean oneShotPlant = runtimeHp <= 0 && (config.abilityType == AbilityType.INSTANT_EXPLOSIVE
                || config.abilityType == AbilityType.INSTANT_SUN_BURST
                || config.abilityType == AbilityType.MINT_FAMILY_BOOST
                || config.name.equalsIgnoreCase("Imitater"));
        Plant plant = new GenericPlant(config.name, position, oneShotPlant ? 1 : Math.max(0, runtimeHp));
        plant.setPosition(position);
        plant.setId(config.id);
        plant.setType(config.category);
        plant.setAbilityType(config.abilityType);
        if (config.tags != null) {
            plant.getTags().addAll(config.tags);
        }

        plant.setCost(Math.max(0, runtimeCost));
        plant.setActionInterval(Math.max(0.05, runtimeInterval));
        plant.setDamage(runtimeDamage);
        plant.setRecharge((int) Math.max(0, runtimeRecharge));
        plant.setAbilityValue(runtimeAbility);
        plant.setLevel(level);
        plant.setPlantFoodType(config.plantFoodType);
        plant.setWrampUp(config.wrampUp);
        plant.getRawUpgrades().addAll(specialTags);

        plant.setActStrategy(buildActStrategy(config));

        plant.setPlantFoodEffect(buildPlantFoodEffect(config));
        plant.setShootingVectors(buildShootingVectors(config));
        if (plant.getTags().contains(PlantTag.CHARGE)) {
            plant.setInternalTimer(plant.getActionInterval());
        }
        return plant;
    }

    private static ActStrategy buildActStrategy(PlantJsonParser.PlantConfig config) {
        if (config.abilityType == AbilityType.MINT_FAMILY_BOOST) return new MintStrategy();
        if (config.abilityType == AbilityType.MODIFIER_UTILITY) return new ModifyStrategy();

        if (config.category == null) return null;
        return switch (config.category) {
            case SUN_PRODUCER -> new SunProduceStrategy();
            case SHOOTER -> new ShootStrategy();
            case HOMING -> new HomingStrategy();
            case STRIKE_THROUGH -> new StrikeStrategy();
            case LOBBER -> new LobberStrategy();
            case EXPLOSIVE -> new ExplodeStrategy();
            case MELEE -> new MeleeStrategy();
            case WALL_NUT -> new WallNutStrategy();
            case MODIFIER -> new ModifyStrategy();
            default -> null;
        };
    }

    private static PlantFoodEffect buildPlantFoodEffect(PlantJsonParser.PlantConfig config) {
        if (config.plantFoodType == null) return null;
        int value = (int) config.plantFoodValue;

        return switch (config.plantFoodType) {
            case NONE -> null;
            case SPAWN_SUN_ITEMS -> new SpawnSun(value);
            case PROJECTILE_BURST -> new TimedProjectileBurst(projectileBurstCount(config));
            case SPAWN_CLONES -> new SpawnClones(Math.max(1, value));
            case LOCAL_AOE_ATTACK -> new LocalAttack(2.0, Math.max(config.damage, value));
            case GRANT_PERMANENT_ARMOR -> new GrantArmor(value);
            case RANDOM_HYPNOTIZE -> new RandomHypnotize(Math.max(1, value));
            case KNOCKBACK_BLAST -> new KnockBackBlast(value, 2.0);
            case PULL_UNDERWATER -> new PullUnderWater(Math.max(1, value));
            case MAP_WIDE_FREEZE -> new MapWideFreeze();
            case INSTANT_KILL -> new InstantKill();
            case LOBBER_BARRAGE -> new LobberBarrage(projectileBurstCount(config));
            case RANDOM_INSTANT_KILL -> new RandomInstantKill(Math.max(1, value));
            case DISARM_BLAST -> new DisarmBlast(Math.max(1, value));
            case LANE_REDIRECT -> new LaneRedirectBlast();
            case PULL_AND_HEAL -> new PullAndHeal(config.plantFoodValue);
        };
    }

    private static int projectileBurstCount(PlantJsonParser.PlantConfig config) {
        int baseDamage = Math.max(1, config.damage);
        return Math.max(1, Math.min(12, (int) Math.ceil(config.plantFoodValue / baseDamage)));
    }

    private static final Map<String, List<Position>> NAMED_SHOOT_PATTERNS = Map.of(
            "Threepeater", List.of(new Position(1, -1), new Position(1, 0), new Position(1, 1)),
            "Split Pea", List.of(new Position(1, 0), new Position(1, 0), new Position(-1, 0)),
            "Rotobaga", List.of(new Position(1, 1), new Position(1, -1), new Position(-1, 1), new Position(-1, -1)),
            "Starfruit", List.of(
                    new Position(1, 0), new Position(-1, 0),
                    new Position(0, 1), new Position(0, -1),
                    new Position(0.7, 0.7)
            ),
            "Bowling Bulb", List.of(new Position(1, -1), new Position(1, 0), new Position(1, 1))
    );

    private static List<Position> buildShootingVectors(PlantJsonParser.PlantConfig config) {
        List<Position> vectors = new ArrayList<>();
        if (config.category != PlantType.SHOOTER) return vectors;

        List<Position> named = NAMED_SHOOT_PATTERNS.get(config.name);
        if (named != null) {
            vectors.addAll(named);
            return vectors;
        }

        int shots = Math.min(6, Math.max(1, (int) config.abilityValue));
        for (int i = 0; i < shots; i++) {
            vectors.add(new Position(1, 0));
        }
        return vectors;
    }

    private static class GenericPlant extends Plant {
        public GenericPlant(String name, Position position, int HP) { super(name, position, HP); }
    }
}
