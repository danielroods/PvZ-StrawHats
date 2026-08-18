package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.obstacles.Grave;
import model.projectile.Projectile;
import model.projectile.StraightMove;
import model.projectile.hit.*;
import model.utils.GameSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShootStrategy implements ActStrategy {
    private static final double VOLLEY_SPAWN_STAGGER = 0.18;

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        List<Position> vectors = user.getShootingVectors();
        if (vectors == null || vectors.isEmpty()) return;

        boolean anyTarget = vectors.stream()
                .anyMatch(v -> findTargetAlongVector(user, v, session) != null
                        || findGraveAlongVector(user, v, session) != null);
        if (!anyTarget) return;

        HitEffectStrategy hitEffect = buildHitEffect(user);
        Map<String, Integer> directionCounts = new HashMap<>();

        for (Position direction : vectors) {
            Zombie target = findTargetAlongVector(user, direction, session);
            if (target == null) {
                Cell grave = findGraveAlongVector(user, direction, session);
                if (grave == null) continue;
            }

            Position normalizedDirection = direction.normalize();
            Position velocity = normalizedDirection.scale(20.0);

            String directionKey = normalizedDirection.x() + "," + normalizedDirection.y();
            int repeatIndex = directionCounts.merge(directionKey, 1, Integer::sum) - 1;
            double stagger = VOLLEY_SPAWN_STAGGER * repeatIndex;
            Position startPosition = repeatIndex == 0
                    ? user.getPosition()
                    : user.getPosition().sub(normalizedDirection.scale(stagger));

            session.getProjectiles().add(new Projectile(user,
                    startPosition,
                    velocity,
                    target,
                    user.getDamage(),
                    new StraightMove(),
                    hitEffect
            ));
        }

        user.setInternalTimer(user.getActionInterval());
    }

    private HitEffectStrategy buildHitEffect(Plant user) {
        int areaLength = user.getTags().contains(PlantTag.AOE) ? 3 : 1;
        if (user.getTags().contains(PlantTag.FIRE)) return new FireHit(areaLength);
        if (user.getTags().contains(PlantTag.ICE)) return new IceHit(areaLength);
        if (user.getTags().contains(PlantTag.POISON)) return new PoisonHit(areaLength);
        if (user.getTags().contains(PlantTag.PIERCE)) return new PierceHit(-1);
        if (user.getTags().contains(PlantTag.BUTTER)) return new ButterHit(1);
        return new NormalHit(areaLength);
    }

    private Zombie findTargetAlongVector(Plant user, Position direction, GameSession session) {
        Position origin = user.getPosition();
        double dx = direction.x();
        double dy = direction.y();
        Zombie nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive()) continue;
            Position zp = zombie.getPosition();
            if (zp == null) continue;

            double relX = zp.x() - origin.x();
            double relY = zp.y() - origin.y();
            if (!isInCone(relX, relY, dx, dy)) continue;

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

                double dist = Math.sqrt(relX * relX + relY * relY);
                if (dist < bestDist) {
                    bestDist = dist;
                    nearest = cell;
                }
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

        if (dx != 0 && Math.abs(dy) <= 1.5) {
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
}
