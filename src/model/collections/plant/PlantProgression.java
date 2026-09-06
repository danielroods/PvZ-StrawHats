package model.collections.plant;

import model.user_data.User;
import model.user_data.UserState;

public final class PlantProgression {

    public static final int UPGRADE_COIN_COST_PER_LEVEL = 500;

    private PlantProgression() {
    }

    public static PlantJsonParser.PlantConfig config(int plantId) {
        return PlantFactory.getBlueprints().get(plantId);
    }

    public static int maxLevel(int plantId) {
        return PlantStats.maxLevel(config(plantId));
    }

    public static int maxLevel(PlantJsonParser.PlantConfig config) {
        return PlantStats.maxLevel(config);
    }

    public static int levelOf(UserState state, int plantId) {
        if (state == null) return PlantStats.MIN_LEVEL;
        PlantJsonParser.PlantConfig config = config(plantId);
        int stored = state.getPlantLevel(plantId);
        return config == null ? Math.max(PlantStats.MIN_LEVEL, stored)
                : PlantStats.clampLevel(config, stored);
    }

    public static int levelOf(UserState state, PlantJsonParser.PlantConfig config) {
        if (state == null || config == null) return PlantStats.MIN_LEVEL;
        return PlantStats.clampLevel(config, state.getPlantLevel(config.id));
    }

    public static int currentLevelOf(int plantId) {
        User user = User.currentUser;
        return user == null ? PlantStats.MIN_LEVEL : levelOf(user.userState, plantId);
    }

    public static boolean isMaxLevel(UserState state, PlantJsonParser.PlantConfig config) {
        return config != null && levelOf(state, config) >= maxLevel(config);
    }

    public static int upgradeCoinCost(int currentLevel) {
        return Math.max(0, currentLevel) * UPGRADE_COIN_COST_PER_LEVEL;
    }

    public static int upgradePacketsRequired(int currentLevel) {
        return Math.max(0, currentLevel);
    }

    public static PlantStats statsFor(UserState state, PlantJsonParser.PlantConfig config) {
        return PlantStats.of(config, levelOf(state, config));
    }

    public static PlantStats currentStatsFor(PlantJsonParser.PlantConfig config) {
        User user = User.currentUser;
        return PlantStats.of(config, user == null
                ? PlantStats.MIN_LEVEL : levelOf(user.userState, config));
    }

    public static PlantStats currentStatsFor(int plantId) {
        PlantJsonParser.PlantConfig config = config(plantId);
        return config == null ? null : currentStatsFor(config);
    }

    public static int currentCostOf(int plantId) {
        PlantStats stats = currentStatsFor(plantId);
        return stats == null ? 0 : stats.cost();
    }
}
