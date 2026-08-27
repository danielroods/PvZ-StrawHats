package model.collections.plant;

import model.collections.animations.AnimationFactory;
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

    private static final double DEFAULT_FUSE_SECONDS = 1.0;
    private static final double GRAPESHOT_FUSE_SECONDS = 1.67;
    private static final double MIN_FUSE_SECONDS = 0.7;
    private static final double MAX_FUSE_SECONDS = 2.5;

    private static Map<Integer, PlantJsonParser.PlantConfig> blueprints = new HashMap<>();
    private static boolean loaded = false;
    private static String imitaterTargetName;

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

    public static void setImitaterTargetName(String name) {
        imitaterTargetName = name;
    }

    public static String getImitaterTargetName() {
        return imitaterTargetName;
    }

    public static int findPlantIdByName(String name) {
        autoInit();
        if (name == null) return -1;
        for (PlantJsonParser.PlantConfig config : blueprints.values()) {
            if (config != null && config.name != null && config.name.equalsIgnoreCase(name)) {
                return config.id;
            }
        }
        return -1;
    }

    public static Plant createPlantByName(String name, int level, Position position) {
        int id = findPlantIdByName(name);
        if (id < 0) throw new IllegalArgumentException("Plant " + name + " does not exist in dataset.");
        return createPlant(id, level, position);
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
        double runtimeRange = config.attackRange;
        double runtimeLifespan = config.lifespan;
        double runtimePlantFoodValue = config.plantFoodValue;
        List<String> specialTags = new ArrayList<>();
        Map<String, Double> specialValues = new HashMap<>();

        if (config.upgrades != null) {
            for (PlantJsonParser.UpgradeConfig upgrade : config.upgrades) {
                if (upgrade.level <= level) {
                    switch (upgrade.type) {
                        case BUFF_HP -> runtimeHp += (int) upgrade.value;
                        case BUFF_COST -> runtimeCost += (int) upgrade.value;
                        case BUFF_ACTION_INTERVAL -> runtimeInterval += upgrade.value;
                        case BUFF_DAMAGE -> runtimeDamage += (int) upgrade.value;
                        case BUFF_RECHARGE -> runtimeRecharge += upgrade.value;
                        case SPECIAL_MECHANIC -> {
                            specialTags.add(upgrade.specialTag);
                            if (upgrade.specialTag != null && !upgrade.specialTag.isBlank()) {
                                specialValues.merge(upgrade.specialTag, upgrade.value, Double::sum);
                            }
                            switch (upgrade.specialTag == null ? "" : upgrade.specialTag) {
                                case "TILE_RANGE_EXT" -> {
                                    if (runtimeRange > 0) runtimeRange += upgrade.value;
                                }
                                case "LIFESPAN_EXT" -> runtimeLifespan += upgrade.value;
                                case "SUN_AMOUNT_BUFF", "SUN_DROP_INCREMENT", "ADDITIONAL_PIERCE" ->
                                        runtimeAbility += upgrade.value;
                                case "FREEZE_DURATION_EXT", "BONUS_GRAB_TARGETS" ->
                                        runtimePlantFoodValue += upgrade.value;
                                default -> { }
                            }
                        }
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
        plant.setAttackRange(runtimeRange);
        plant.setLifespanSeconds(Math.max(0, runtimeLifespan));
        plant.setLevel(level);
        if (config.name.equalsIgnoreCase("Imitater")) {
            plant.setImitaterTargetName(imitaterTargetName);
        }
        plant.setPlantFoodType(config.plantFoodType);
        plant.setWrampUp(config.wrampUp, specialValues.getOrDefault("GROW_TIME_REDUCTION", 0.0));
        plant.getRawUpgrades().addAll(specialTags);
        specialValues.forEach(plant::addSpecialUpgrade);

        plant.setActStrategy(buildActStrategy(config));

        plant.setPlantFoodEffect(buildPlantFoodEffect(config, runtimePlantFoodValue, level));
        plant.setShootingVectors(buildShootingVectors(config));
        if (config.category == PlantType.SHOOTER && plant.getTags().contains(PlantTag.STACK)) {
            plant.setMaxStackNumber((int) runtimeAbility);
        }
        if ("Potato Mine".equalsIgnoreCase(config.name)
                || "Primal Potato Mine".equalsIgnoreCase(config.name)) {
            double baseArmTime = 14.0;
            double armReduction = specialValues.getOrDefault("ARM_TIME_REDUCTION", 0.0);
            plant.setInternalTimer(Math.max(0.1, baseArmTime - armReduction));
            plant.setState(Plant.PlantState.PREPPING);
        } else if ("Cherry Bomb".equalsIgnoreCase(config.name)) {
            plant.setInternalTimer(0.70);
            plant.setState(Plant.PlantState.PREPPING);
        } else if ("Jalapeno".equalsIgnoreCase(config.name)) {
            plant.setInternalTimer(0.67);
            plant.setState(Plant.PlantState.PREPPING);
        } else if (plant.getTags().contains(PlantTag.CHARGE)) {
            plant.setInternalTimer(plant.getActionInterval());
        }

        double fuse = resolveFuseSeconds(config);
        if (fuse > 0) {
            plant.setInternalTimer(fuse);
            plant.setState(Plant.PlantState.PREPPING);
        } else if (config.abilityType == AbilityType.PRODUCE_SUN) {
            plant.setInternalTimer(plant.getActionInterval());
        }
        return plant;
    }

    private static double resolveFuseSeconds(PlantJsonParser.PlantConfig config) {
        if ("Jalapeno".equalsIgnoreCase(config.name)) {
            return 0.67;
        }

        if ("Grapeshot".equalsIgnoreCase(config.name)) {
            return GRAPESHOT_FUSE_SECONDS;
        }

        // Doom-shroom deliberately has a long fuse so its three GrowthTracker stages
        // can actually be reached before the explosion. Its visual explosion state is
        // selected separately by PlantRenderer/EffectRenderer.
        if ("Doom-shroom".equalsIgnoreCase(config.name)) return 20.0;

        String clipState = switch (config.abilityType) {
            case INSTANT_EXPLOSIVE -> AnimationFactory.firstAvailableClipState(
                    config.name, "explode", "attack");
            case INSTANT_SUN_BURST -> "attack";
            case MINT_FAMILY_BOOST -> "intro";
            default -> null;
        };
        if (config.abilityType == AbilityType.INSTANT_EXPLOSIVE && clipState == null) {
            clipState = "attack";
        }
        if (clipState == null) return 0;

        float clipSeconds = AnimationFactory.clipDurationForDisplayName(config.name, clipState);
        double fuse = clipSeconds > 0 ? clipSeconds : DEFAULT_FUSE_SECONDS;
        return Math.min(MAX_FUSE_SECONDS, Math.max(MIN_FUSE_SECONDS, fuse));
    }

    private static ActStrategy buildActStrategy(PlantJsonParser.PlantConfig config) {
        if (config.abilityType == AbilityType.MINT_FAMILY_BOOST) return new MintStrategy();
        if (config.abilityType == AbilityType.MODIFIER_UTILITY) return new ModifyStrategy();
        if ("Bowling Bulb".equalsIgnoreCase(config.name)) return new BowlingBulbStrategy();

        if (config.category == null) return null;
        return switch (config.category) {
            case SUN_PRODUCER -> new SunProduceStrategy();
            case SHOOTER -> new ShootStrategy();
            case HOMING -> new HomingStrategy();
            case STRIKE_THROUGH -> new StrikeStrategy();
            case LOBBER -> new LobberStrategy();
            case EXPLOSIVE -> {
                if ("Squash".equalsIgnoreCase(config.name)) yield new SquashStrategy();
                if ("Tangle Kelp".equalsIgnoreCase(config.name)) yield new TangleKelpStrategy();
                if ("Grapeshot".equalsIgnoreCase(config.name)) yield new GrapeshotStrategy();
                yield new ExplodeStrategy();
            }
            case MELEE -> new MeleeStrategy();
            case WALL_NUT -> new WallNutStrategy();
            case MODIFIER -> new ModifyStrategy();
            default -> null;
        };
    }

    private static PlantFoodEffect buildPlantFoodEffect(PlantJsonParser.PlantConfig config,
                                                        double plantFoodValue, int level) {
        if (config.plantFoodType == null) return null;
        int value = (int) plantFoodValue;

        return switch (config.plantFoodType) {
            case NONE -> null;
            case SPAWN_SUN_ITEMS -> new SpawnSun(value);
            case PROJECTILE_BURST -> // Fume-shroom's Plant Food window is a fixed 5.33s "plantfood" loop
                // (not the usual burst-count-derived duration) with a larger burst of shots.
                    new TimedProjectileBurst(projectileBurstCount(config, plantFoodValue));
            case SPAWN_CLONES -> new SpawnClones(Math.max(1, value));
            case LOCAL_AOE_ATTACK -> {
                if ("Bonk Choy".equalsIgnoreCase(config.name)) {
                    yield new MeleeAreaPlantFood(false, Math.max(config.damage, value));
                }
                if ("Phat Beet".equalsIgnoreCase(config.name)) {
                    yield new MeleeAreaPlantFood(true, Math.max(config.damage, value));
                }
                if ("Kiwibeast".equalsIgnoreCase(config.name)) {
                    yield new MeleeAreaPlantFood(false, Math.max(config.damage, value), true);
                }
                if ("Ice-shroom".equalsIgnoreCase(config.name)) {
                    yield new IceShroomPlantFood(Math.max(config.damage, value));
                }
                yield new LocalAttack(2.0, Math.max(config.damage, value));
            }
            case GRANT_PERMANENT_ARMOR -> {
                if ("Pumpkin".equalsIgnoreCase(config.name)) {
                    int pumpkinArmor = configLevelPumpkinPlantFoodArmor(level, plantFoodValue);
                    yield new GrantArmor(pumpkinArmor);
                }
                if ("Wall-nut".equalsIgnoreCase(config.name)) {
                    int wallNutArmor = configLevelWallNutPlantFoodArmor(level, plantFoodValue);
                    yield new GrantArmor(wallNutArmor);
                }
                if ("Tall-nut".equalsIgnoreCase(config.name)) {
                    int tallNutArmor = configLevelTallNutPlantFoodArmor(level, plantFoodValue);
                    yield new GrantArmor(tallNutArmor);
                }
                if ("Explode-o-nut".equalsIgnoreCase(config.name)) {
                    int nutArmor = configLevelExplodeONutPlantFoodArmor(level, plantFoodValue);
                    yield new GrantArmor(nutArmor);
                }
                yield new GrantArmor(value);
            }
            case RANDOM_HYPNOTIZE -> new RandomHypnotize(Math.max(1, value));
            case KNOCKBACK_BLAST -> new KnockBackBlast(value, 2.0);
            case PULL_UNDERWATER -> {
                if ("Chomper".equalsIgnoreCase(config.name)) yield new ChomperPlantFood();
                if ("Tangle Kelp".equalsIgnoreCase(config.name)) yield new TangleKelpPlantFood(Math.max(1, value));
                yield new PullUnderWater(Math.max(1, value));
            }
            case MAP_WIDE_FREEZE -> new MapWideFreeze(plantFoodValue);
            case MAP_WIDE_BUTTER -> new MapWideButter(plantFoodValue);
            case SELF_BOOST -> new SelfBoost(plantFoodValue);
            case INSTANT_KILL -> new InstantKill();
            case LOBBER_BARRAGE -> new LobberBarrage(projectileBurstCount(config, plantFoodValue));
            case RANDOM_INSTANT_KILL -> "Squash".equalsIgnoreCase(config.name)
                    ? new SquashPlantFood(Math.max(1, value))
                    : new RandomInstantKill(Math.max(1, value));
            case DISARM_BLAST -> new DisarmBlast(Math.max(1, value));
            case LANE_REDIRECT -> "Garlic".equalsIgnoreCase(config.name)
                    ? new GarlicPlantFood()
                    : new LaneRedirectBlast();
            case PULL_AND_HEAL -> "Sweet Potato".equalsIgnoreCase(config.name)
                    ? new PullAndHeal(4.0)
                    : new PullAndHeal(plantFoodValue);
        };
    }

    private static int configLevelPumpkinPlantFoodArmor(int level, double plantFoodValue) {
        int actual = level >= 8 ? 16000 : level >= 4 ? 12000 : 8000;
        return Math.max(actual, (int) Math.round(plantFoodValue));
    }

    private static int configLevelWallNutPlantFoodArmor(int level, double plantFoodValue) {
        return Math.max(8000, (int) Math.round(plantFoodValue));
    }

    private static int configLevelTallNutPlantFoodArmor(int level, double plantFoodValue) {
        return Math.max(8000, (int) Math.round(plantFoodValue));
    }

    private static int configLevelExplodeONutPlantFoodArmor(int level, double plantFoodValue) {
        return Math.max(8000, (int) Math.round(plantFoodValue));
    }

    private static int projectileBurstCount(PlantJsonParser.PlantConfig config, double plantFoodValue) {
        int baseDamage = Math.max(1, config.damage);
        return Math.max(1, Math.min(12, (int) Math.ceil(plantFoodValue / baseDamage)));
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