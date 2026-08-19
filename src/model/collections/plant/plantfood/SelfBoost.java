package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.utils.GameSession;

public class SelfBoost implements PlantFoodEffect {
    private static final double DEFAULT_BOOST_SECONDS = 3.0;

    private final double boostSeconds;

    public SelfBoost(double boostSeconds) {
        this.boostSeconds = boostSeconds > 0 ? boostSeconds : DEFAULT_BOOST_SECONDS;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }

    @Override
    public double getDurationSeconds() {
        return boostSeconds;
    }
}
