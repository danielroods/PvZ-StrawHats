package model.collections.plant.plantfood;

import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;

final class PlantFoodClipSequence {
    private final String onState;
    private final String loopState;
    private final String offState;
    private final double onDuration;
    private final double loopDuration;
    private final double offDuration;
    private final double activeDuration;

    private double elapsed = 0.0;

    private PlantFoodClipSequence(String onState, String loopState, String offState,
                                  double onDuration, double loopDuration, double offDuration,
                                  double activeDuration) {
        this.onState = onState;
        this.loopState = loopState;
        this.offState = offState;
        this.onDuration = onDuration;
        this.loopDuration = loopDuration;
        this.offDuration = offDuration;
        this.activeDuration = Math.max(loopDuration, activeDuration);
    }

    static PlantFoodClipSequence forPlant(Plant plant, String onState, String loopState,
                                          String offState, double activeDuration) {
        return new PlantFoodClipSequence(onState, loopState, offState,
                clip(plant, onState), clip(plant, loopState), clip(plant, offState),
                activeDuration);
    }

    private static double clip(Plant plant, String state) {
        if (state == null) return 0.0;
        float duration = AnimationFactory.exactClipDurationForPath(
                AnimationFactory.pathForDisplayName(plant.getName()), state);
        return duration > 0f ? duration : 0.0;
    }

    double totalDuration() {
        return onDuration + activeDuration + offDuration;
    }

    double introDuration() {
        return onDuration;
    }

    boolean isActive() {
        return elapsed >= onDuration && elapsed < onDuration + activeDuration;
    }

    void reset() {
        elapsed = 0.0;
    }

    void advance(Plant plant, double deltaTimeSeconds) {
        elapsed += deltaTimeSeconds;
        apply(plant);
    }

    void apply(Plant plant) {
        if (elapsed < onDuration && onDuration > 0) {
            setState(plant, onState, elapsed, onDuration - elapsed);
            return;
        }
        double intoActive = elapsed - onDuration;
        if (intoActive < activeDuration && loopDuration > 0) {
            double withinLoop = intoActive % loopDuration;
            setState(plant, loopState, withinLoop, loopDuration - withinLoop);
            return;
        }
        if (offDuration > 0) {
            double intoOff = Math.max(0.0, intoActive - activeDuration);
            setState(plant, offState, intoOff, Math.max(0.05, offDuration - intoOff));
        }
    }

    private void setState(Plant plant, String state, double playedSeconds, double remaining) {
        if (state == null) return;
        plant.setVisualAnimationProgress(state, Math.max(0.0, remaining), Math.max(0.0, playedSeconds));
    }
}
