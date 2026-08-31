package model.collections.plant.plantfood;

import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.match_mechanisms.vector.Position;
import model.projectile.Projectile;
import model.projectile.StraightMove;
import model.projectile.hit.FumeCloudHit;
import model.utils.GameSession;

public class FumeBlast implements PlantFoodEffect {
    private static final double FALLBACK_DURATION = 5.33;
    private static final double PULSE_INTERVAL = 0.45;
    private static final double CLOUD_SPEED = 2.6;
    private static final double FALLBACK_REACH = 4.5;
    private static final int CLOUD_LANES = 3;

    private final int damagePerPulse;

    private double duration = FALLBACK_DURATION;
    private double elapsed = 0.0;
    private double sincePulse = 0.0;

    public FumeBlast(int damagePerPulse) {
        this.damagePerPulse = Math.max(1, damagePerPulse);
    }

    @Override
    public double getDurationSeconds() {
        return duration;
    }

    @Override
    public boolean drivesActStrategy() {
        return true;
    }

    @Override
    public void reset() {
        elapsed = 0.0;
        sincePulse = 0.0;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        float clip = AnimationFactory.clipDurationForDisplayName(plant.getName(), "plantfood");
        duration = clip > 0f ? clip : FALLBACK_DURATION;
        pulse(plant, session);
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        elapsed += deltaTimeSeconds;
        sincePulse += deltaTimeSeconds;
        if (elapsed > duration) return;
        if (sincePulse < PULSE_INTERVAL) return;
        sincePulse = 0.0;
        pulse(plant, GameSession.peekInstance());
    }

    private void pulse(Plant plant, GameSession session) {
        Position center = plant.getPosition();
        if (center == null || session == null) return;

        double reach = plant.getAttackRange() > 0 ? plant.getAttackRange() : FALLBACK_REACH;
        Projectile cloud = new Projectile(plant,
                center,
                new Position(session.projectileSpeed(CLOUD_SPEED), 0.0),
                null,
                damagePerPulse,
                new StraightMove(),
                new FumeCloudHit(CLOUD_LANES)
        );
        cloud.setSpawnDelaySeconds(0.0);
        cloud.setMaxTravelDistance(reach);
        session.getProjectiles().add(cloud);
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}
