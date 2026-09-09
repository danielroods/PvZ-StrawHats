package model.utils;

import controller.QuestManager;
import model.collections.plant.Plant;
import service.GameClock;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class SessionEconomy {

    private static final int MAX_PLANT_FOOD = 3;
    private static final double MIN_SKY_SUN_INTERVAL = 12.0;
    private static final double SKY_SUN_INTERVAL_START = 6.0;
    private static final double SKY_SUN_INTERVAL_GROWTH = 0.05;
    private static final double BOSS_SUN_RATE_SCALE = 0.5;

    private final GameSession session;

    private int sunCount;
    private int plantFoodCount;
    private double skySunTimer = 0;

    private final Map<Integer, Double> plantCooldowns = new HashMap<>();
    private final Set<Integer> matchBoostedPlantIds = new HashSet<>();

    private int plantsLostThisMatch = 0;
    private final Set<String> plantFamiliesUsedThisMatch = new HashSet<>();
    private boolean plantedAnyPlantThisMatch = false;
    private boolean usedNonNightPlantThisMatch = false;

    SessionEconomy(GameSession session) {
        this.session = session;
    }

    double getEffectiveSkySunInterval() {
        double elapsed = session.getElapsedSecondsSinceWavesStarted();
        double baseInterval = Math.max(SKY_SUN_INTERVAL_START
                + SKY_SUN_INTERVAL_GROWTH * elapsed, MIN_SKY_SUN_INTERVAL);
        if (session.getDifficultyLevel() > 0) {
            baseInterval *= session.getDifficultyLevel() / 3.0;
        }
        double interval = session.isDoubleSunRate() ? baseInterval * BOSS_SUN_RATE_SCALE : baseInterval;
        return interval * session.getSkySunIntervalMultiplier();
    }

    double advanceSkySunTimer(double deltaTimeSeconds) {
        skySunTimer += deltaTimeSeconds;
        return skySunTimer;
    }

    void resetSkySunTimer() {
        skySunTimer = 0;
    }

    void tickPlantCooldowns(double deltaTimeSeconds) {
        plantCooldowns.replaceAll((plantId, remaining) -> GameClock.countDown(remaining, deltaTimeSeconds));
        plantCooldowns.entrySet().removeIf(entry -> GameClock.isZero(entry.getValue()));
    }

    int getSunCount() {
        return sunCount;
    }

    void setSunCount(int amount) {
        sunCount = Math.max(0, amount);
    }

    void setPlantFoodCount(int amount) {
        plantFoodCount = Math.max(0, Math.min(MAX_PLANT_FOOD, amount));
    }

    void addSun(int amount) {
        sunCount += amount;
        if (amount > 0) {
            QuestManager.updateProgress("COLLECT_SUN", amount, Collections.emptyMap());
        }
    }

    boolean spendSun(int amount) {
        if (sunCount < amount) return false;
        sunCount -= amount;
        return true;
    }

    int getPlantFoodCount() {
        return plantFoodCount;
    }

    boolean addPlantFood() {
        if (plantFoodCount >= MAX_PLANT_FOOD) return false;
        plantFoodCount++;
        return true;
    }

    boolean spendPlantFood() {
        if (plantFoodCount <= 0) return false;
        plantFoodCount--;
        return true;
    }

    boolean grantMatchBoost(int plantId) {
        return matchBoostedPlantIds.add(plantId);
    }

    boolean hasMatchBoost(int plantId) {
        return matchBoostedPlantIds.contains(plantId);
    }

    Set<Integer> getMatchBoostedPlantIds() {
        return Set.copyOf(matchBoostedPlantIds);
    }

    void restoreMatchBoosts(Collection<Integer> plantIds) {
        matchBoostedPlantIds.clear();
        if (plantIds != null) matchBoostedPlantIds.addAll(plantIds);
    }

    void removeAllCooldowns() {
        session.getPlants().forEach(p -> p.setInternalTimer(0));
        plantCooldowns.clear();
    }

    boolean isPlantReady(int plantId) {
        return GameClock.isZero(plantCooldowns.getOrDefault(plantId, 0.0));
    }

    double getPlantCooldown(int plantId) {
        return plantCooldowns.getOrDefault(plantId, 0.0);
    }

    void startPlantCooldown(int plantId, double seconds) {
        if (seconds > 0) plantCooldowns.put(plantId, seconds);
    }

    void clearPlantCooldown(int plantId) {
        plantCooldowns.remove(plantId);
    }

    int getPlantsLostThisMatch() {
        return plantsLostThisMatch;
    }

    void countPlantsLost(Iterable<Plant> plants) {
        for (Plant plant : plants) {
            if (plant.isAlive()) continue;
            if (plant.isGraveBuster() && plant.hasGraveBusterConsumedGrave()) continue;
            plantsLostThisMatch++;
        }
    }

    void recordPlantFamily(String family) {
        plantFamiliesUsedThisMatch.add(family);
    }

    void markPlantedAnyPlant() {
        plantedAnyPlantThisMatch = true;
    }

    void markUsedNonNightPlant() {
        usedNonNightPlantThisMatch = true;
    }

    boolean hasUsedPlantFamily(String family) {
        return family != null && plantFamiliesUsedThisMatch.contains(family.trim().toLowerCase());
    }

    boolean usedOnlyNightPlants() {
        return plantedAnyPlantThisMatch && !usedNonNightPlantThisMatch;
    }

    void clearCooldownsAndBoosts() {
        plantCooldowns.clear();
        matchBoostedPlantIds.clear();
    }

    
    void resetMatchStats(int initialSun) {
        plantsLostThisMatch = 0;
        plantFamiliesUsedThisMatch.clear();
        plantedAnyPlantThisMatch = false;
        usedNonNightPlantThisMatch = false;
        plantFoodCount = 0;
        sunCount = initialSun;
    }
}