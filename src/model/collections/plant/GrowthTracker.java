package model.collections.plant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GrowthTracker {
    private final List<Map<String, Object>> stages;
    private final double stageTimeShift;
    private int currentStage = 1;
    private double ageInSeconds = 0.0;

    public GrowthTracker(List<Map<String, Object>> stages) {
        this(stages, 0.0, 0);
    }

    public GrowthTracker(List<Map<String, Object>> stages, double stageTimeShift) {
        this(stages, stageTimeShift, 0);
    }

    public GrowthTracker(List<Map<String, Object>> stages, double stageTimeShift, int extraStages) {
        this.stages = withExtraStages(stages, extraStages);
        this.stageTimeShift = stageTimeShift;
    }

    private static List<Map<String, Object>> withExtraStages(List<Map<String, Object>> source,
                                                             int extraStages) {
        if (source == null || source.size() < 2 || extraStages <= 0) return source;

        List<Map<String, Object>> extended = new ArrayList<>(source);
        for (int added = 0; added < extraStages; added++) {
            Map<String, Object> last = extended.get(extended.size() - 1);
            Map<String, Object> previous = extended.get(extended.size() - 2);
            Map<String, Object> next = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : last.entrySet()) {
                Object previousValue = previous.get(entry.getKey());
                if (!(entry.getValue() instanceof Number lastNumber)
                        || !(previousValue instanceof Number previousNumber)) {
                    next.put(entry.getKey(), entry.getValue());
                    continue;
                }
                double step = lastNumber.doubleValue() - previousNumber.doubleValue();
                next.put(entry.getKey(), lastNumber.doubleValue() + step);
            }
            extended.add(next);
        }
        return extended;
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
