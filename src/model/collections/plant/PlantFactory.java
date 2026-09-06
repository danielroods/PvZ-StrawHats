package model.collections.plant;

import model.collections.animations.AnimationFactory;
import model.collections.armour.ArmourFactory;
import model.collections.armour.ArmourType;
import model.collections.armour.PlantArmour;
import model.collections.plant.actstrategy.*;
import model.collections.plant.plantfood.*;
import model.collections.zombie.Zombie;
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

        PlantStats stats = PlantStats.of(config, level);
        int runtimeHp = stats.hp();
        int runtimeCost = stats.cost();
        double runtimeInterval = stats.actionInterval();
        int runtimeDamage = stats.damage();
        int runtimeRecharge = stats.recharge();
        double runtimeAbility = stats.abilityValue();
        double runtimeRange = stats.attackRange();
        double runtimeLifespan = stats.lifespan();
        double runtimePlantFoodValue = stats.plantFoodValue();
        List<String> specialTags = stats.specialTags();
        Map<String, Double> specialValues = stats.specialValues();

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
        plant.setLevel(stats.level());
        PlantStats baseline = stats.baseline();
        plant.setUpgradeStatBonuses(stats.damage() - baseline.damage(),
                stats.abilityValue() - baseline.abilityValue());
        if (config.name.equalsIgnoreCase("Imitater")) {
            plant.setImitaterTargetName(imitaterTargetName);
        }
        plant.setPlantFoodType(config.plantFoodType);
        plant.setWrampUp(config.wrampUp, specialValues.getOrDefault("GROW_TIME_REDUCTION", 0.0),
                (int) Math.round(specialValues.getOrDefault(
                        UpgradeEffects.GROWTH_STAGE_MAX_UP_TAG, 0.0)));
        plant.getRawUpgrades().addAll(specialTags);
        specialValues.forEach(plant::addSpecialUpgrade);

        plant.setActStrategy(buildActStrategy(config));

        plant.setPlantFoodEffect(buildPlantFoodEffect(config, runtimePlantFoodValue, stats.level()));
        plant.setShootingVectors(buildShootingVectors(config));
        if (config.category == PlantType.SHOOTER && plant.getTags().contains(PlantTag.STACK)) {
            plant.setMaxStackNumber((int) runtimeAbility);
        }
        if ("Potato Mine".equalsIgnoreCase(config.name)
                || "Primal Potato Mine".equalsIgnoreCase(config.name)) {
            plant.setInternalTimer(stats.potatoMineArmSeconds());
            plant.setState(Plant.PlantState.PREPPING);
        } else if ("Cherry Bomb".equalsIgnoreCase(config.name)) {
            plant.setInternalTimer(0.70);
            plant.setState(Plant.PlantState.PREPPING);
        } else if ("Jalapeno".equalsIgnoreCase(config.name)) {
            plant.setInternalTimer(0.67);
            plant.setState(Plant.PlantState.PREPPING);
        } else if ("Grave Buster".equalsIgnoreCase(config.name)) {
            plant.setInternalTimer(plant.getActionInterval());
            float chew = AnimationFactory.exactClipDurationForPath(
                    AnimationFactory.pathForDisplayName(config.name),
                    GraveBusterStrategy.CHEW_STATE);
            plant.setVisualAnimationProgress(GraveBusterStrategy.CHEW_STATE,
                    chew > 0f ? chew : plant.getActionInterval(), 0.0);
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

        if ("Grave Buster".equalsIgnoreCase(config.name)) return 0;

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
                if ("Grave Buster".equalsIgnoreCase(config.name)) yield new GraveBusterStrategy();
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
            case PROJECTILE_BURST -> buildBarrage(config, plantFoodValue);
            case SPAWN_CLONES -> new SpawnClones(Math.max(1, value));
            case LOCAL_AOE_ATTACK -> buildAreaSuperpower(config, value);
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
            case LOBBER_BARRAGE -> {
                BarrageProfile lobProfile = barrageProfile(config, plantFoodValue);
                yield new LobberBarrage(lobProfile.shots(), lobProfile.interval());
            }
            case RANDOM_INSTANT_KILL -> {
                if ("Squash".equalsIgnoreCase(config.name)) yield new SquashPlantFood(Math.max(1, value));
                if ("Electric Blueberry".equalsIgnoreCase(config.name)) {
                    yield new ElectricBlueberryPlantFood();
                }
                yield new RandomInstantKill(Math.max(1, value));
            }
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

    private record BarrageProfile(int shots, double interval) { }

    private static final Map<String, BarrageProfile> BARRAGE_PROFILES = buildBarrageProfiles();

    private static Map<String, BarrageProfile> buildBarrageProfiles() {
        Map<String, BarrageProfile> profiles = new HashMap<>();
        profiles.put("Peashooter", new BarrageProfile(16, 0.20));
        profiles.put("Repeater", new BarrageProfile(16, 0.22));
        profiles.put("Threepeater", new BarrageProfile(14, 0.24));
        profiles.put("Snow Pea", new BarrageProfile(16, 0.20));
        profiles.put("Fire Peashooter", new BarrageProfile(14, 0.22));
        profiles.put("Pea Pod", new BarrageProfile(14, 0.26));
        profiles.put("Split Pea", new BarrageProfile(14, 0.24));
        profiles.put("Mega Gatling Pea", new BarrageProfile(32, 0.11));
        profiles.put("Goo Peashooter", new BarrageProfile(14, 0.22));
        profiles.put("Starfruit", new BarrageProfile(14, 0.24));
        profiles.put("Rotobaga", new BarrageProfile(22, 0.14));
        profiles.put("Cat-tail", new BarrageProfile(24, 0.12));
        profiles.put("Cactus", new BarrageProfile(12, 0.28));
        profiles.put("Citron", new BarrageProfile(4, 0.70));
        profiles.put("Bowling Bulb", new BarrageProfile(6, 0.45));
        profiles.put("Sea-shroom", new BarrageProfile(12, 0.28));
        profiles.put("Puff-shroom", new BarrageProfile(12, 0.28));
        profiles.put("Cabbage-pult", new BarrageProfile(6, 0.40));
        profiles.put("Kernel-pult", new BarrageProfile(8, 0.35));
        profiles.put("Melon-pult", new BarrageProfile(5, 0.50));
        profiles.put("Winter Melon", new BarrageProfile(5, 0.50));
        profiles.put("Pepper-pult", new BarrageProfile(5, 0.50));
        return profiles;
    }

    private static BarrageProfile barrageProfile(PlantJsonParser.PlantConfig config,
                                                 double plantFoodValue) {
        BarrageProfile named = BARRAGE_PROFILES.get(config.name);
        if (named != null) return named;
        return new BarrageProfile(projectileBurstCount(config, plantFoodValue),
                TimedProjectileBurst.DEFAULT_FIRE_INTERVAL);
    }

    private static PlantFoodEffect buildBarrage(PlantJsonParser.PlantConfig config,
                                                double plantFoodValue) {
        BarrageProfile profile = barrageProfile(config, plantFoodValue);
        int finisherDamage = (int) Math.round(Math.max(0.0, plantFoodValue));

        if ("Snow Pea".equalsIgnoreCase(config.name)) {
            return new SnowPeaPlantFood(profile.shots(), profile.interval(), finisherDamage);
        }
        if ("Fire Peashooter".equalsIgnoreCase(config.name)) {
            return new FirePeashooterPlantFood(profile.shots(), profile.interval(),
                    finisherDamage, Math.max(1, config.damage));
        }
        if ("Cat-tail".equalsIgnoreCase(config.name)) {
            return new HomingBarrage(profile.shots(), profile.interval(), 3.0);
        }
        if ("Rotobaga".equalsIgnoreCase(config.name)) {
            return new SpreadBarrage(profile.shots(), profile.interval(),
                    Math.max(config.damage, finisherDamage), SpreadBarrage.eightWay());
        }
        if (config.tags != null && config.tags.contains(PlantTag.PEA)) {
            return new PeaBarrage(profile.shots(), profile.interval(), finisherDamage,
                    giantPeaAsset(config.name));
        }
        return new TimedProjectileBurst(profile.shots(), profile.interval());
    }

    private static String giantPeaAsset(String plantName) {
        if ("Pea Pod".equalsIgnoreCase(plantName)) {
            return "768/FULL/EFFECTS/PEAPOD_PLANTFOOD_GIANTPEA/PEAPOD_PLANTFOOD_GIANTPEA.PAM";
        }
        return "768/INITIAL/EFFECTS/REPEATER_PLANTFOOD_GIANTPEA/REPEATER_PLANTFOOD_GIANTPEA.PAM";
    }

    private static PlantFoodEffect buildAreaSuperpower(PlantJsonParser.PlantConfig config,
                                                       int value) {
        int perHit = Math.max(config.damage, value);
        if ("Fume-shroom".equalsIgnoreCase(config.name)) {
            return new FumeBlast(perHit);
        }
        if ("Bonk Choy".equalsIgnoreCase(config.name)) {
            return new MeleeAreaPlantFood(new MeleeAreaPlantFood.Profile(
                    1.0, 1.0, 14, 2.0, perHit, null, 0.0, 0.0,
                    "plantfood_on", "plantfood", "plantfood_off"));
        }
        if ("Phat Beet".equalsIgnoreCase(config.name)) {
            return new MeleeAreaPlantFood(new MeleeAreaPlantFood.Profile(
                    2.0, 1.0, 8, 4.0, perHit, Zombie.Status.BUTTER,
                    1.2, 0.0, null, "plantfood", null));
        }
        if ("Wasabi Whip".equalsIgnoreCase(config.name)) {
            return new MeleeAreaPlantFood(new MeleeAreaPlantFood.Profile(
                    3.0, 0.4, 10, 2.2, perHit, Zombie.Status.FIRED,
                    4.0, 0.0, "plantfood_on", "plantfood", "plantfood_off"));
        }
        if ("Kiwibeast".equalsIgnoreCase(config.name)) {
             return new MeleeAreaPlantFood(new MeleeAreaPlantFood.Profile(
                    1.5, 1.0, 3, 2.9, perHit, null, 0.0, 0.5,
                    null, "plantfood_stage3", null));
        }
        if ("Ice-shroom".equalsIgnoreCase(config.name)) {
            return new IceShroomPlantFood(perHit);
        }
        return new LocalAttack(2.0, perHit);
    }

    private static int projectileBurstCount(PlantJsonParser.PlantConfig config, double plantFoodValue) {
        int baseDamage = Math.max(1, config.damage);
        return Math.max(3, Math.min(12, (int) Math.ceil(plantFoodValue / baseDamage)));
    }

    private static final Map<String, List<Position>> NAMED_SHOOT_PATTERNS = Map.of(
            "Threepeater", List.of(new Position(1, -1), new Position(1, 0), new Position(1, 1)),
            "Split Pea", List.of(new Position(1, 0), new Position(-1, 0), new Position(-1, 0)),
            "Rotobaga", List.of(new Position(1, 1), new Position(1, -1), new Position(-1, 1), new Position(-1, -1)),
            "Starfruit", List.of(
                    new Position(-1, 0),
                    new Position(0, 1), new Position(0, -1),
                    new Position(0.7, 0.7), new Position(0.7, -0.7)
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