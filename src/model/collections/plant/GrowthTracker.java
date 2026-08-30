package model.collections.plant;

import java.util.List;
import java.util.Map;

public class GrowthTracker {
    private final List<Map<String, Object>> stages;
    private final double stageTimeShift;
    private int currentStage = 1;
    private double ageInSeconds = 0.0;

    public GrowthTracker(List<Map<String, Object>> stages) {
        this(stages, 0.0);
    }

    public GrowthTracker(List<Map<String, Object>> stages, double stageTimeShift) {
        this.stages = stages;
        this.stageTimeShift = stageTimeShift;
    }

    private double stageTime(Map<String, Object> stageData) {
        double raw = ((Number) stageData.get("time")).doubleValue();
        return Math.max(0.5, raw + stageTimeShift);
    }

    public boolean hasStages() {
        return stages != null && !stages.isEmpty();
    }

    public void update(double deltaTime) {
        if (!hasStages()) return;
        ageInSeconds += deltaTime;
        for (Map<String, Object> stageData : stages) {
            int stage = ((Number) stageData.get("stage")).intValue();
            double targetTime = stageTime(stageData);
            if (ageInSeconds >= targetTime && stage > currentStage) {
                currentStage = stage;
            }
        }
    }

    public Double getStageValue(String key) {
        if (!hasStages()) return null;
        for (Map<String, Object> stageData : stages) {
            int stage = ((Number) stageData.get("stage")).intValue();
            if (stage == currentStage && stageData.containsKey(key)) {
                return ((Number) stageData.get(key)).doubleValue();
            }
        }
        return null;
    }

    public int advanceStage() {
        if (!hasStages()) return currentStage;
        int maxStage = currentStage;
        for (Map<String, Object> stageData : stages) {
            int stage = ((Number) stageData.get("stage")).intValue();
            if (stage > maxStage) maxStage = stage;
        }
        if (currentStage < maxStage) currentStage++;
        return currentStage;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(int stage) {
        this.currentStage = Math.max(1, stage);
    }

    public double getAgeInSeconds() {
        return ageInSeconds;
    }

    public void skipToMaxStage() {
        if (!hasStages()) return;

        int maxStage = currentStage;
        double maxTime = ageInSeconds;

        for (Map<String, Object> stageData : stages) {
            int stage = ((Number) stageData.get("stage")).intValue();
            double targetTime = stageTime(stageData);

            if (stage > maxStage) maxStage = stage;
            if (targetTime > maxTime) maxTime = targetTime;
        }

        this.currentStage = maxStage;
        this.ageInSeconds = maxTime;
    }

    public List<Map<String, Object>> getRawStages() {
        return stages;
    }
}
