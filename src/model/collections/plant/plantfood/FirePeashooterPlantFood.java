package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class FirePeashooterPlantFood extends PeaBarrage {
    private static final double SCORCH_INTERVAL = 0.35;
    private static final double BURN_SECONDS = 5.0;

    private final int scorchDamage;
    private double sinceScorch = 0.0;

    public FirePeashooterPlantFood(int shots, double fireInterval, int giantPeaDamage,
                                   int scorchDamage) {
        super(shots, fireInterval, giantPeaDamage, null);
        this.scorchDamage = Math.max(1, scorchDamage);
    }

    @Override
    public void reset() {
        super.reset();
        sinceScorch = 0.0;
    }

    @Override
    protected void onBurstStart(Plant plant, GameSession session) {
        igniteRow(plant, session);
    }

    @Override
    protected void onBurstTick(Plant plant, GameSession session, double deltaTimeSeconds) {
        sinceScorch += deltaTimeSeconds;
        if (sinceScorch < SCORCH_INTERVAL) return;
        sinceScorch = 0.0;
        igniteRow(plant, session);
    }

    private void igniteRow(Plant plant, GameSession session) {
        Position center = plant.getPosition();
        if (center == null || session == null) return;
        for (Zombie zombie : new java.util.ArrayList<>(session.getZombies())) {
            if (zombie == null || !zombie.isAlive() || zombie.isHypnotized()) continue;
            Position at = zombie.getPosition();
            if (at == null || Math.abs(at.y() - center.y()) >= 0.5) continue;
            if (at.x() < center.x() - 0.5) continue;
            zombie.applyStatus(Zombie.Status.FIRED, BURN_SECONDS);
            zombie.takeDamage(scorchDamage, plant);
        }
    }
}
