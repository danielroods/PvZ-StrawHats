package view.screens.ui_menus;

import model.collections.plant.AbilityType;
import model.collections.plant.PlantJsonParser;
import model.collections.plant.PlantProgression;
import model.collections.plant.PlantStats;
import model.collections.plant.PlantType;
import model.user_data.UserState;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class PlantStatRows {

    public static final String SPLASH_DAMAGE_TAG = "SPLASH_DAMAGE_BUFF";
    public static final String REFLECT_DAMAGE_TAG = "REFLECT_DAMAGE_BUFF";

    public static final String MAX_LEVEL_PREFIX = "Max level";
    public static final String NEXT_UPGRADE_PREFIX = "Next upgrade:";

    private static final String IMITATER = "Imitater";

    private PlantStatRows() {
    }

    public static PlantStats statsFor(UserState state, PlantJsonParser.PlantConfig config,
                                      boolean unlocked) {
        return unlocked ? PlantProgression.statsFor(state, config)
                : PlantStats.of(config, PlantStats.MIN_LEVEL);
    }

    public static List<String> of(UserState state, PlantJsonParser.PlantConfig config,
                                  boolean unlocked) {
        PlantStats stats = statsFor(state, config, unlocked);
        PlantStats base = PlantStats.of(config, PlantStats.MIN_LEVEL);
        List<String> rows = new ArrayList<>();

        rows.add("Level: " + (unlocked ? stats.level() + " / " + stats.maxLevel() : "-"));
        rows.add("Type: " + config.category);
        rows.add("HP: " + stats.hp() + delta(stats.hp() - base.hp()));
        rows.add("Damage: " + stats.damage() + delta(stats.damage() - base.damage())
                + perkDamage(stats));

        if (shows(config, PlantStatRows::costOf)) {
            rows.add("Sun cost: " + stats.cost() + delta(stats.cost() - base.cost()));
        }
        if (shows(config, PlantStatRows::rechargeOf)) {
            rows.add("Recharge: " + stats.recharge() + "s"
                    + delta(stats.recharge() - base.recharge()));
        }
        rows.add("Attack speed: " + trim(stats.actionInterval()) + "s"
                + delta(stats.actionInterval() - base.actionInterval()));
        if (shows(config, PlantStatRows::rangeOf)) {
            rows.add("Range: " + trim(stats.attackRange()) + " tiles"
                    + delta(stats.attackRange() - base.attackRange()));
        }
        if (shows(config, PlantStatRows::lifespanOf)) {
            rows.add("Lifespan: " + trim(stats.lifespan()) + "s"
                    + delta(stats.lifespan() - base.lifespan()));
        }
        if (shows(config, PlantStatRows::abilityOf)) {
            rows.add(abilityLabel(config) + ": " + trim(stats.abilityValue())
                    + delta(stats.abilityValue() - base.abilityValue()));
        }
        if (shows(config, PlantStatRows::plantFoodOf)) {
            rows.add("Plant Food power: " + trim(stats.plantFoodValue())
                    + delta(stats.plantFoodValue() - base.plantFoodValue()));
        }
        if (copiesOtherPlants(config)) {
            rows.add("Copies plants at level: " + stats.level());
        }
        if (!stats.specialTags().isEmpty()) {
            rows.add("Perks: " + perkNames(stats));
        }
        rows.add("Tags: " + tagsOf(config));
        return rows;
    }

    public static boolean isMaxLevel(UserState state, PlantJsonParser.PlantConfig config,
                                     boolean unlocked) {
        return statsFor(state, config, unlocked).isMaxLevel();
    }

    public static String upgradeLine(UserState state, PlantJsonParser.PlantConfig config,
                                     boolean unlocked) {
        if (!unlocked) return null;
        PlantStats stats = statsFor(state, config, unlocked);
        if (stats.isMaxLevel()) {
            return MAX_LEVEL_PREFIX + " " + stats.level() + "/" + stats.maxLevel()
                    + " - no further upgrades";
        }
        String summary = nextLevelSummary(state, config, unlocked);
        return summary == null ? null : NEXT_UPGRADE_PREFIX + " " + summary;
    }

    public static String nextLevelSummary(UserState state, PlantJsonParser.PlantConfig config,
                                          boolean unlocked) {
        PlantStats stats = statsFor(state, config, unlocked);
        if (stats.isMaxLevel()) return null;
        PlantStats next = PlantStats.of(config, stats.level() + 1);
        List<String> parts = new ArrayList<>();

        addDelta(parts, next.hp() - stats.hp(), " HP");
        addDelta(parts, next.damage() - stats.damage(), " damage");
        addDelta(parts, next.cost() - stats.cost(), " sun cost");
        addDelta(parts, next.recharge() - stats.recharge(), "s recharge");
        addDelta(parts, next.actionInterval() - stats.actionInterval(), "s attack interval");
        addDelta(parts, next.attackRange() - stats.attackRange(), " tile range");
        addDelta(parts, next.lifespan() - stats.lifespan(), "s lifespan");
        addDelta(parts, next.abilityValue() - stats.abilityValue(),
                " " + abilityLabel(config).toLowerCase());
        addDelta(parts, next.plantFoodValue() - stats.plantFoodValue(), " Plant Food power");
        addDelta(parts, next.specialValue(SPLASH_DAMAGE_TAG, 0)
                - stats.specialValue(SPLASH_DAMAGE_TAG, 0), " splash damage");
        addDelta(parts, next.specialValue(REFLECT_DAMAGE_TAG, 0)
                - stats.specialValue(REFLECT_DAMAGE_TAG, 0), " reflect damage");

        for (String tag : next.specialTags()) {
            if (stats.specialTags().contains(tag)) continue;
            if (PlantStats.foldsIntoNumericStat(tag)
                    && Math.abs(PlantStats.foldedDelta(tag, stats, next)) >= 0.005) {
                continue;
            }
            if (SPLASH_DAMAGE_TAG.equals(tag) || REFLECT_DAMAGE_TAG.equals(tag)) continue;
            String friendly = friendly(tag);
            if (!parts.contains(friendly)) parts.add(friendly);
        }
        if (parts.isEmpty() && copiesOtherPlants(config)) {
            parts.add("copies plants at level " + (stats.level() + 1));
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static boolean copiesOtherPlants(PlantJsonParser.PlantConfig config) {
        return IMITATER.equalsIgnoreCase(config.name);
    }

    private static String perkDamage(PlantStats stats) {
        StringBuilder extra = new StringBuilder();
        double splash = stats.specialValue(SPLASH_DAMAGE_TAG, 0);
        double reflect = stats.specialValue(REFLECT_DAMAGE_TAG, 0);
        if (splash > 0) extra.append("  +").append(trim(splash)).append(" splash");
        if (reflect > 0) extra.append("  +").append(trim(reflect)).append(" reflect");
        return extra.toString();
    }

    private static String perkNames(PlantStats stats) {
        List<String> perks = new ArrayList<>();
        for (String tag : stats.specialTags()) {
            String friendly = friendly(tag);
            if (!perks.contains(friendly)) perks.add(friendly);
        }
        return String.join(", ", perks);
    }

    private static String tagsOf(PlantJsonParser.PlantConfig config) {
        return (config.tags == null || config.tags.isEmpty())
                ? "None"
                : config.tags.stream().map(Enum::name).collect(Collectors.joining(", "));
    }

    private static String abilityLabel(PlantJsonParser.PlantConfig config) {
        if (config.abilityType == AbilityType.PRODUCE_SUN
                || config.abilityType == AbilityType.INSTANT_SUN_BURST) {
            return "Sun produced";
        }
        if (config.category == PlantType.STRIKE_THROUGH) return "Pierce";
        if (config.category == PlantType.SHOOTER) return "Shots";
        return "Ability";
    }

    private interface StatReader {
        double read(PlantStats stats);
    }

    private static double costOf(PlantStats stats) { return stats.cost(); }

    private static double rechargeOf(PlantStats stats) { return stats.recharge(); }

    private static double rangeOf(PlantStats stats) { return stats.attackRange(); }

    private static double lifespanOf(PlantStats stats) { return stats.lifespan(); }

    private static double abilityOf(PlantStats stats) { return stats.abilityValue(); }

    private static double plantFoodOf(PlantStats stats) { return stats.plantFoodValue(); }

    private static boolean shows(PlantJsonParser.PlantConfig config, StatReader reader) {
        int maxLevel = PlantStats.maxLevel(config);
        double first = reader.read(PlantStats.of(config, PlantStats.MIN_LEVEL));
        if (Math.abs(first) > 0.0001) return true;
        for (int level = PlantStats.MIN_LEVEL + 1; level <= maxLevel; level++) {
            if (Math.abs(reader.read(PlantStats.of(config, level)) - first) > 0.0001) return true;
        }
        return false;
    }

    private static void addDelta(List<String> parts, int change, String suffix) {
        if (change == 0) return;
        parts.add((change > 0 ? "+" : "") + change + suffix);
    }

    private static void addDelta(List<String> parts, double change, String suffix) {
        if (Math.abs(change) < 0.005) return;
        parts.add((change > 0 ? "+" : "") + trim(change) + suffix);
    }

    private static String delta(int change) {
        if (change == 0) return "";
        return change > 0 ? "  (+" + change + ")" : "  (" + change + ")";
    }

    private static String delta(double change) {
        if (Math.abs(change) < 0.005) return "";
        return change > 0 ? "  (+" + trim(change) + ")" : "  (" + trim(change) + ")";
    }

    private static String trim(double value) {
        return value == Math.rint(value)
                ? String.valueOf((long) value) : String.format("%.2f", value);
    }

    private static String friendly(String tag) {
        return tag.replace('_', ' ').toLowerCase();
    }
}
