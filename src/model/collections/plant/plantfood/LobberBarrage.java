package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.utils.GameSession;

public class LobberBarrage implements PlantFoodEffect {
    private static final double TAIL_SECONDS = 0.6;

    private final int shots;
    private final double fireInterval;
    private double elapsed = 0;
    private int fired = 0;

    public LobberBarrage(int shots, double fireInterval) {
        this.shots = Math.max(1, shots);
        this.fireInterval = fireInterval > 0 ? fireInterval : 0.4;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        fireOnce(plant, session);
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        elapsed += deltaTimeSeconds;
        while (elapsed >= fireInterval && fired < shots) {
            elapsed -= fireInterval;
            fireOnce(plant, GameSession.peekInstance());
        }
    }

    private void fireOnce(Plant plant, GameSession session) {
        if (plant.getActStrategy() == null || session == null || fired >= shots) return;
        plant.setInternalTimer(0);
        plant.getActStrategy().act(plant, session);
        fired++;
    }

    @Override
    public double getDurationSeconds() {
        return shots * fireInterval + TAIL_SECONDS;
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
