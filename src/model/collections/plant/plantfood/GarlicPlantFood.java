package model.collections.plant.plantfood;

import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.utils.GameSession;

public class GarlicPlantFood implements PlantFoodEffect {
    private static final double FALLBACK_DURATION = 2.5;
    private double runtimeDuration = FALLBACK_DURATION;

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        if (plant == null || session == null) return;

        plant.setHP(plant.getMaxHp());
        plant.executeGarlicPlantFood(session);

        float clipDuration = AnimationFactory.clipDurationForDisplayName(plant.getName(), "plantfood");
        runtimeDuration = clipDuration > 0f ? Math.max(FALLBACK_DURATION, clipDuration) : FALLBACK_DURATION;
        plant.setVisualAnimationState("plantfood", runtimeDuration);
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }

    @Override
    public double getDurationSeconds() {
        return runtimeDuration;
    }
}
