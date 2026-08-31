package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.utils.GameSession;

public class MapWideFreeze implements PlantFoodEffect {
    private static final double DEFAULT_FREEZE_SECONDS = 5.0;
    private static final double WINDOW_SECONDS = 1.6;

    private final double freezeSeconds;
    private double elapsed = 0.0;

    public MapWideFreeze(double freezeSeconds) {
        this.freezeSeconds = freezeSeconds > 0 ? freezeSeconds : DEFAULT_FREEZE_SECONDS;
    }

    @Override
    public double getDurationSeconds() {
        return WINDOW_SECONDS;
    }

    @Override
    public void reset() {
        elapsed = 0.0;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        freezeEverything(session);
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        elapsed += deltaTimeSeconds;
        if (elapsed > WINDOW_SECONDS) return;
        freezeEverything(GameSession.peekInstance());
    }

    private void freezeEverything(GameSession session) {
        if (session == null) return;
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.isHypnotized()) continue;
            zombie.applyStatus(Zombie.Status.FROZEN, freezeSeconds);
        }
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}
