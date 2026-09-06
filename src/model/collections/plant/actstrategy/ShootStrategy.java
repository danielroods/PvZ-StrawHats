package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.obstacles.Grave;
import model.projectile.LaneShiftMove;
import model.projectile.MoveStrategy;
import model.projectile.Projectile;
import model.projectile.StraightMove;
import model.projectile.hit.*;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShootStrategy implements ActStrategy {

    public static final double SHOT_SPEED = 3.6;

    private static final double VOLLEY_STAGGER_SECONDS = 0.16;
    private static final double LANE_EPSILON = 1.0e-6;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        List<Position> vectors = user.getShootingVectors();
        if (vectors == null || vectors.isEmpty()) return;
        if (user.getTags().contains(PlantTag.STACK) && user.getMaxStackNumber() > 1) {
            vectors = vectors.subList(0, Math.min(user.getStackNumber(), vectors.size()));
        }

        boolean boosted = user.isPlantFoodActive();

        List<Zombie> targets = new ArrayList<>(vectors.size());
        boolean anyTarget = false;
        for (Position direction : vectors) {
            Zombie target = findTargetAlongVector(user, direction, session);
            targets.add(target);
            if (target != null || findGraveAlongVector(user, direction, session) != null
                    || findStructureAlongVector(user, direction, session) != null) {
                anyTarget = true;
            }
        }
        if (!anyTarget && !boosted) return;

        HitEffectStrategy hitEffect = buildHitEffect(user);
        Map<String, Integer> directionCounts = new HashMap<>();

        for (int i = 0; i < vectors.size(); i++) {
            Position direction = vectors.get(i);
            Zombie target = targets.get(i);
            if (target == null && findGraveAlongVector(user, direction, session) == null
                    && findStructureAlongVector(user, direction, session) == null && !boosted) {
                continue;
            }

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
        if (!isLaneShift(dx, dy)) return new StraightMove();

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

    private Zombie findTargetAlongVector(Plant user, Position direction, GameSession session) {
        Position origin = user.getPosition();
        double dx = direction.x();
        double dy = direction.y();
        Zombie nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive()) continue;
            if (zombie.isAirborne() && !zombie.acceptsAttackFrom(user)) continue;
            Position zp = zombie.getPosition();
            if (zp == null) continue;

            double relX = zp.x() - origin.x();
            double relY = zp.y() - origin.y();
            if (!isInCone(relX, relY, dx, dy)) continue;
            if (!user.isWithinAttackRange(zp)) continue;

            double dist = Math.sqrt(relX * relX + relY * relY);
            if (dist < bestDist) {
                bestDist = dist;
                nearest = zombie;
            }
        }
        return nearest;
    }

    private Cell findGraveAlongVector(Plant user, Position direction, GameSession session) {
        Position origin = user.getPosition();
        double dx = direction.x();
        double dy = direction.y();
        Cell nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (int row = 0; row < session.getEnvironment().getRows(); row++) {
            for (int col = 0; col < session.getEnvironment().getCols(); col++) {
                Cell cell = session.getEnvironment().getCell(row, col);
                if (cell == null || !(cell.getObstacle() instanceof Grave)) continue;

                double relX = col - origin.x();
                double relY = row - origin.y();
                if (!isInCone(relX, relY, dx, dy)) continue;
                if (!user.isWithinAttackRange(new Position(col, row))) continue;

                double dist = Math.sqrt(relX * relX + relY * relY);
                if (dist < bestDist) {
                    bestDist = dist;
                    nearest = cell;
                }
            }
        }
        return nearest;
    }

    // Plants only used to fire when a live Zombie (or grave) sat in the lane. A
    // pushed structure (e.g. the barrel a Barrel Roller Zombie shoves) has no
    // effect on that check, so once its owning zombie died and only the structure
    // was left in the lane, plants stopped firing entirely and the structure could
    // never take damage. Treat a live structure in the lane the same as a target
    // so plants keep firing at it - Projectile's own blocker logic is what
    // actually applies the damage once the shot is on its way.
    private PushableStructure findStructureAlongVector(Plant user, Position direction, GameSession session) {
        Position origin = user.getPosition();
        double dx = direction.x();
        double dy = direction.y();
        PushableStructure nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (PushableStructure structure : session.getPushableStructures()) {
            if (structure == null || !structure.isAlive()) continue;
            Position sp = structure.getPosition();
            if (sp == null) continue;

            double relX = sp.x() - origin.x();
            double relY = sp.y() - origin.y();
            if (!isInCone(relX, relY, dx, dy)) continue;
            if (!user.isWithinAttackRange(sp)) continue;

            double dist = Math.sqrt(relX * relX + relY * relY);
            if (dist < bestDist) {
                bestDist = dist;
                nearest = structure;
            }
        }
        return nearest;
    }

    private boolean isInCone(double relX, double relY, double dx, double dy) {
        double dirLen = Math.sqrt(dx * dx + dy * dy);
        if (dirLen == 0) return false;

        if (dy == 0) {
            return Math.abs(relY) < 0.75 && Math.signum(relX) == Math.signum(dx);
        }

        if (isLaneShift(dx, dy)) {
            boolean correctXDir = Math.signum(relX) == Math.signum(dx) && Math.abs(relX) > 0;
            boolean correctRow = Math.abs(relY - dy) < 0.75;
            return correctXDir && correctRow;
        }

        double relLen = Math.sqrt(relX * relX + relY * relY);
        if (relLen == 0) return false;
        double ndx = dx / dirLen;
        double ndy = dy / dirLen;
        double dot = (relX / relLen) * ndx + (relY / relLen) * ndy;
        return dot > 0.6;
    }

    private static boolean isLaneShift(double dx, double dy) {
        return dx != 0 && Math.abs(Math.abs(dy) - 1.0) <= LANE_EPSILON;
    }
}