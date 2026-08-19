package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.utils.GameSession;

public class MapWideButter implements PlantFoodEffect {
    private static final double DEFAULT_BUTTER_SECONDS = 4.0;

    private final double butterSeconds;

    public MapWideButter(double butterSeconds) {
        this.butterSeconds = butterSeconds > 0 ? butterSeconds : DEFAULT_BUTTER_SECONDS;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        for (Zombie zombie : session.getZombies()) {
            if (zombie != null && zombie.isAlive() && !zombie.isHypnotized()) {
                zombie.applyStatus(Zombie.Status.BUTTER, butterSeconds);
            }
        }
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}
