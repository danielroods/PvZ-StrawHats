package model.collections.plant.plantfood;

import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.HomingMove;
import model.projectile.Projectile;
import model.projectile.hit.NormalHit;
import model.projectile.targeting.PlantTarget;
import model.projectile.targeting.TargetFinder;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ElectricBlueberryPlantFood implements PlantFoodEffect {
    private static final double CLOUD_SPEED = 7.0;
    private static final double TURN_RATE_PER_SECOND = 12.0;
    private static final double STAGGER_SECONDS = 0.07;
    private static final double FALLBACK_DURATION = 2.0;
    private static final double TAIL_SECONDS = 0.8;

    private double duration = FALLBACK_DURATION + TAIL_SECONDS;

    @Override
    public double getDurationSeconds() {
        return duration;
    }

    @Override
    public boolean drivesActStrategy() {
        return true;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        float clip = AnimationFactory.clipDurationForDisplayName(plant.getName(), "plantfood");
        double clipSeconds = clip > 0f ? clip : FALLBACK_DURATION;

        if (session == null || plant.getPosition() == null) {
            duration = clipSeconds + TAIL_SECONDS;
            return;
        }

        List<PlantTarget> targets = new ArrayList<>();
        for (Zombie zombie : session.getZombies()) {
            if (!TargetFinder.isTargetable(plant, zombie, false)) continue;
            targets.add(PlantTarget.ofZombie(zombie));
        }
        if (targets.isEmpty()) {
            PlantTarget object = TargetFinder.anywhere(plant, session, false).nearest();
            if (object != null && object.getPosition() != null) targets.add(object);
        }
        targets.sort(Comparator.comparingDouble(
                target -> target.getPosition().distanceTo(plant.getPosition())));

        int damage = Math.max(1, plant.getDamage());
        for (int i = 0; i < targets.size(); i++) {
            session.getProjectiles().add(buildZap(plant, session, targets.get(i), damage,
                    STAGGER_SECONDS * i));
        }

        double lastZap = targets.isEmpty() ? 0.0 : STAGGER_SECONDS * (targets.size() - 1);
        duration = Math.max(clipSeconds, lastZap) + TAIL_SECONDS;
    }

    private Projectile buildZap(Plant plant, GameSession session, PlantTarget target, int damage,
                                double delay) {
        double speed = session.projectileSpeed(CLOUD_SPEED);
        Position toTarget = target.getPosition().sub(plant.getPosition());
        Position direction = toTarget.length() > 0 ? toTarget.normalize() : Position.of(1, 0);

        Projectile zap = new Projectile(plant,
                plant.getPosition(),
                direction.scale(speed),
                target.getZombie(),
                damage,
                new HomingMove(target, speed, TURN_RATE_PER_SECOND),
                new NormalHit(1)
        );
        zap.setSpawnDelaySeconds(delay);
        return zap;
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }

    @Override
    public void reset() {
        duration = FALLBACK_DURATION + TAIL_SECONDS;
    }
}
