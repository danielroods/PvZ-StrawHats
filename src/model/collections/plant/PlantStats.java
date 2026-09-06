package model.collections.plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlantStats {

    public static final int MIN_LEVEL = 1;
    public static final double POTATO_MINE_BASE_ARM_SECONDS = 14.0;

    private static final Set<String> RANGE_TAGS = Set.of("TILE_RANGE_EXT");
    private static final Set<String> LIFESPAN_TAGS = Set.of("LIFESPAN_EXT");
    private static final Set<String> ABILITY_TAGS =
            Set.of("SUN_AMOUNT_BUFF", "SUN_DROP_INCREMENT", "ADDITIONAL_PIERCE");
    private static final Set<String> PLANT_FOOD_TAGS =
            Set.of("FREEZE_DURATION_EXT", "BONUS_GRAB_TARGETS");

    private final PlantJsonParser.PlantConfig config;
    private final int level;
    private final int maxLevel;
    private final int hp;
    private final int cost;
    private final int damage;
    private final int recharge;
    private final double actionInterval;
    private final double abilityValue;
    private final double attackRange;
    private final double lifespan;
    private final double plantFoodValue;
    private final List<String> specialTags;
    private final Map<String, Double> specialValues;

    private PlantStats(PlantJsonParser.PlantConfig config, int level, int maxLevel,
                       int hp, int cost, int damage, int recharge, double actionInterval,
                       double abilityValue, double attackRange, double lifespan,
                       double plantFoodValue, List<String> specialTags,
                       Map<String, Double> specialValues) {
        this.config = config;
        this.level = level;
        this.maxLevel = maxLevel;
        this.hp = hp;
        this.cost = cost;
        this.damage = damage;
        this.recharge = recharge;
        this.actionInterval = actionInterval;
        this.abilityValue = abilityValue;
        this.attackRange = attackRange;
        this.lifespan = lifespan;
        this.plantFoodValue = plantFoodValue;
        this.specialTags = Collections.unmodifiableList(specialTags);
        this.specialValues = Collections.unmodifiableMap(specialValues);
    }

    public static boolean foldsIntoNumericStat(String tag) {
        return RANGE_TAGS.contains(tag) || LIFESPAN_TAGS.contains(tag)
                || ABILITY_TAGS.contains(tag) || PLANT_FOOD_TAGS.contains(tag);
    }

    public static double foldedDelta(String tag, PlantStats from, PlantStats to) {
        if (from == null || to == null) return 0.0;
        if (RANGE_TAGS.contains(tag)) return to.attackRange() - from.attackRange();
        if (LIFESPAN_TAGS.contains(tag)) return to.lifespan() - from.lifespan();
        if (ABILITY_TAGS.contains(tag)) return to.abilityValue() - from.abilityValue();
        if (PLANT_FOOD_TAGS.contains(tag)) return to.plantFoodValue() - from.plantFoodValue();
        return 0.0;
    }

    public static int maxLevel(PlantJsonParser.PlantConfig config) {
        if (config == null || config.upgrades == null || config.upgrades.isEmpty()) return MIN_LEVEL;
        int max = MIN_LEVEL;
        for (PlantJsonParser.UpgradeConfig upgrade : config.upgrades) {
            if (upgrade != null && upgrade.level > max) max = upgrade.level;
        }
        return max;
    }

    public static int clampLevel(PlantJsonParser.PlantConfig config, int level) {
        return Math.max(MIN_LEVEL, Math.min(maxLevel(config), level));
    }

    public static PlantStats of(PlantJsonParser.PlantConfig config, int requestedLevel) {
        if (config == null) throw new IllegalArgumentException("plant config cannot be null");

        int maxLevel = maxLevel(config);
        int level = Math.max(MIN_LEVEL, Math.min(maxLevel, requestedLevel));

        int hp = config.baseHp;
        int cost = config.cost;
        int damage = config.damage;
        double recharge = config.recharge;
        double actionInterval = config.actionInterval;
        double abilityValue = config.abilityValue;
        double attackRange = config.attackRange;
        double lifespan = config.lifespan;
        double plantFoodValue = config.plantFoodValue;
        List<String> specialTags = new ArrayList<>();
        Map<String, Double> specialValues = new LinkedHashMap<>();

        if (config.upgrades != null) {
            for (PlantJsonParser.UpgradeConfig upgrade : config.upgrades) {
                if (upgrade == null || upgrade.type == null || upgrade.level > level) continue;

                String tag = upgrade.specialTag == null ? "" : upgrade.specialTag.trim();
                if (!tag.isEmpty()) {
                    specialTags.add(tag);
                    specialValues.merge(tag, upgrade.value, Double::sum);
                }

                switch (upgrade.type) {
                    case BUFF_HP -> hp += (int) upgrade.value;
                    case BUFF_COST -> cost += (int) upgrade.value;
                    case BUFF_ACTION_INTERVAL -> actionInterval += upgrade.value;
                    case BUFF_DAMAGE -> damage += (int) upgrade.value;
                    case BUFF_RECHARGE -> recharge += upgrade.value;
                    case SPECIAL_MECHANIC -> {
                        if (RANGE_TAGS.contains(tag) && attackRange > 0) {
                            attackRange += upgrade.value;
                        } else if (LIFESPAN_TAGS.contains(tag)) {
                            lifespan += upgrade.value;
                        } else if (ABILITY_TAGS.contains(tag)) {
                            abilityValue += upgrade.value;
                        } else if (PLANT_FOOD_TAGS.contains(tag)) {
                            plantFoodValue += upgrade.value;
                        }
                    }
                }
            }
        }

        return new PlantStats(config, level, maxLevel,
                Math.max(0, hp), Math.max(0, cost), damage,
                (int) Math.max(0, recharge), Math.max(0.05, actionInterval),
                abilityValue, attackRange, Math.max(0, lifespan), plantFoodValue,
                specialTags, specialValues);
    }

    public PlantJsonParser.PlantConfig config() { return config; }

    public PlantStats baseline() {
        return level == MIN_LEVEL ? this : of(config, MIN_LEVEL);
    }

    public int level() { return level; }

    public int maxLevel() { return maxLevel; }

    public boolean isMaxLevel() { return level >= maxLevel; }

    public int hp() { return hp; }

    public int cost() { return cost; }

    public int damage() { return damage; }

    public int recharge() { return recharge; }

    public double actionInterval() { return actionInterval; }

    public double abilityValue() { return abilityValue; }

    public double attackRange() { return attackRange; }

    public double lifespan() { return lifespan; }

    public double plantFoodValue() { return plantFoodValue; }

    public List<String> specialTags() { return specialTags; }

    public Map<String, Double> specialValues() { return new HashMap<>(specialValues); }

    public double specialValue(String tag, double fallback) {
        Double value = specialValues.get(tag);
        return value == null ? fallback : value;
    }

    public boolean hasSpecial(String tag) { return specialValues.containsKey(tag); }

    public double potatoMineArmSeconds() {
        return Math.max(0.1, POTATO_MINE_BASE_ARM_SECONDS + specialValue("ARM_TIME_REDUCTION", 0.0));
    }
}
