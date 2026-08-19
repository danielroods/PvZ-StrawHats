package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.utils.GameSession;

public class TimedProjectileBurst implements PlantFoodEffect {
    private static final double FIRE_INTERVAL = 0.5;

    private final int burstCount;
    private double elapsed = 0;
    private int fired = 0;

    public TimedProjectileBurst(int burstCount) {
        this.burstCount = Math.max(1, burstCount);
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        fireOnce(plant, session);
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        elapsed += deltaTimeSeconds;
        if (elapsed >= FIRE_INTERVAL && fired < burstCount) {
            elapsed = 0;
            fireOnce(plant, GameSession.getInstance());
        }
    }

    private void fireOnce(Plant plant, GameSession session) {
        if (plant.getActStrategy() == null || session == null || fired >= burstCount) return;
        plant.setInternalTimer(0);
        plant.getActStrategy().act(plant, session);
        fired++;
    }

    @Override
    public double getDurationSeconds() {
        // +2.5s beyond the last shot so the boosted state (and "plantfood" animation)
        // stays visible for a moment after the burst finishes firing, not just for the
        // exact span of the shots themselves.
        return Math.max(0.0, (burstCount - 1) * FIRE_INTERVAL + 0.1) + 2.5;
    }

    @Override
    public boolean drivesActStrategy() {
        return true;
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }

    @Override
    public void reset() {
        elapsed = 0;
        fired = 0;
    }
}