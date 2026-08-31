package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class SnowPeaPlantFood extends PeaBarrage {
    private static final double FREEZE_SECONDS = 10.0;
    private static final double REFRESH_INTERVAL = 0.4;

    private double sinceRefresh = 0.0;

    public SnowPeaPlantFood(int shots, double fireInterval, int giantPeaDamage) {
        super(shots, fireInterval, giantPeaDamage, null);
    }

    @Override
    public void reset() {
        super.reset();
        sinceRefresh = 0.0;
    }

    @Override
    protected void onBurstStart(Plant plant, GameSession session) {
        freezeRow(plant, session);
    }

    @Override
    protected void onBurstTick(Plant plant, GameSession session, double deltaTimeSeconds) {
        sinceRefresh += deltaTimeSeconds;
        if (sinceRefresh < REFRESH_INTERVAL) return;
        sinceRefresh = 0.0;
        freezeRow(plant, session);
    }

    private void freezeRow(Plant plant, GameSession session) {
        Position center = plant.getPosition();
        if (center == null || session == null) return;
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.isHypnotized()) continue;
            Position at = zombie.getPosition();
            if (at == null || Math.abs(at.y() - center.y()) >= 0.5) continue;
            zombie.applyStatus(Zombie.Status.FROZEN, FREEZE_SECONDS);
        }
    }
}
