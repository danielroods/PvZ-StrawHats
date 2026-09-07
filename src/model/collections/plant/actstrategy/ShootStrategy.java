package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.LaneShiftMove;
import model.projectile.MoveStrategy;
import model.projectile.Projectile;
import model.projectile.StraightMove;
import model.projectile.hit.*;
import model.projectile.targeting.TargetFinder;
import model.projectile.targeting.TargetScan;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShootStrategy implements ActStrategy {

    public static final double SHOT_SPEED = 3.6;

    private static final double VOLLEY_STAGGER_SECONDS = 0.16;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        List<Position> vectors = user.getShootingVectors();
        if (vectors == null || vectors.isEmpty()) return;
        if (user.getTags().contains(PlantTag.STACK) && user.getMaxStackNumber() > 1) {
            vectors = vectors.subList(0, Math.min(user.getStackNumber(), vectors.size()));
        }

        boolean boosted = user.isPlantFoodActive();

        List<TargetScan> scans = new ArrayList<>(vectors.size());
        boolean anyTarget = false;
        for (Position direction : vectors) {
            TargetScan scan = TargetFinder.alongVector(user, direction, session, false);
            scans.add(scan);
            if (scan.hasTarget()) anyTarget = true;
        }
        if (!anyTarget && !boosted) return;

        HitEffectStrategy hitEffect = buildHitEffect(user);
        Map<String, Integer> directionCounts = new HashMap<>();

        for (int i = 0; i < vectors.size(); i++) {
            Position direction = vectors.get(i);
            TargetScan scan = scans.get(i);
            if (!scan.hasTarget() && !boosted) continue;

            Zombie target = scan.zombie();
            Position normalizedDirection = direction.normalize();
            double speed = session.projectileSpeed(SHOT_SPEED);
            Position velocity = normalizedDirection.scale(speed);

            String directionKey = normalizedDirection.x() + "," + normalizedDirection.y();
            int repeatIndex = directionCounts.merge(directionKey, 1, Integer::sum) - 1;

            Projectile projectile = new Projectile(user,
                    user.getPosition(),
                    velocity,
                    target,
                    user.getDamage(),
                    buildMoveStrategy(user, direction, speed),
                    hitEffect
            );
            projectile.setSpawnDelaySeconds(projectile.getSpawnDelaySeconds()
                    + VOLLEY_STAGGER_SECONDS * repeatIndex);
            projectile.setMaxTravelDistance(user.getAttackRange());
            session.getProjectiles().add(projectile);
        }

        user.setInternalTimer(user.getActionInterval());

        if (!boosted && user.canUsePlantFood()
                && model.collections.plant.UpgradeEffects.rollsAutoPlantFood(user)) {
            user.activatePlant(session);
        }
    }

    private MoveStrategy buildMoveStrategy(Plant user, Position direction, double speed) {
        double dx = direction.x();
        double dy = direction.y();
        if (!TargetFinder.isLaneShift(dx, dy)) return new StraightMove();

        double laneY = Math.round(user.getPosition().y()) + dy;
        return new LaneShiftMove(laneY, Math.signum(dx) * speed);
    }

    private HitEffectStrategy buildHitEffect(Plant user) {
        int areaLength = user.getTags().contains(PlantTag.AOE) ? 3 : 1;
        if (user.getTags().contains(PlantTag.FIRE)) return new FireHit(areaLength, 1.0);
        if (user.getTags().contains(PlantTag.ICE)) {
            return new IceHit(areaLength, 5.0 + user.getSpecialUpgrade("CHILL_DURATION_EXT", 0));
        }
        if (user.getTags().contains(PlantTag.POISON)) {
            return new PoisonHit(areaLength, PoisonHit.BASE_POISON_SECONDS
                    + user.getSpecialUpgrade("POISON_TICK_BUFF", 0));
        }
        if (user.getTags().contains(PlantTag.PIERCE)) return new PierceHit(-1);
        if (user.getTags().contains(PlantTag.BUTTER)) return new ButterHit(1);
        return new NormalHit(areaLength,
                (int) Math.round(user.getSpecialUpgrade("SPLASH_DAMAGE_BUFF", 0)));
    }
}
