package model.projectile.targeting;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
import model.utils.GameSession;

import java.util.function.Predicate;

public final class TargetFinder {

    public static final double VECTOR_ROW_TOLERANCE = 0.75;
    public static final double LANE_ROW_TOLERANCE = 0.5;

    private static final double CONE_DOT_THRESHOLD = 0.6;
    private static final double LANE_EPSILON = 1.0e-6;

    private TargetFinder() {
    }

    public static TargetScan alongVector(Plant user, Position direction, GameSession session,
                                         boolean lobbed) {
        if (user == null || direction == null || user.getPosition() == null) return TargetScan.empty();
        Position origin = user.getPosition();
        return scan(user, session, lobbed,
                at -> isInCone(at.x() - origin.x(), at.y() - origin.y(),
                        direction.x(), direction.y()) && user.isWithinAttackRange(at));
    }

    public static TargetScan inLaneAhead(Plant user, GameSession session, double rowTolerance,
                                         boolean lobbed) {
        if (user == null || user.getPosition() == null) return TargetScan.empty();
        Position origin = user.getPosition();
        return scan(user, session, lobbed,
                at -> Math.abs(at.y() - origin.y()) < rowTolerance && at.x() > origin.x()
                        && user.isWithinAttackRange(at));
    }

    public static TargetScan anywhere(Plant user, GameSession session, boolean lobbed) {
        if (user == null || user.getPosition() == null) return TargetScan.empty();
        return scan(user, session, lobbed, at -> true);
    }

    public static boolean isTargetable(Plant user, Zombie zombie, boolean lobbed) {
        if (zombie == null || !zombie.isAlive() || zombie.isHypnotized()
                || zombie.getPosition() == null) {
            return false;
        }
        return !zombie.isAirborne() || lobbed || zombie.acceptsAttackFrom(user);
    }

    private static TargetScan scan(Plant user, GameSession session, boolean lobbed,
                                   Predicate<Position> accepts) {
        if (session == null) return TargetScan.empty();
        Position origin = user.getPosition();

        PlantTarget nearest = null;
        Zombie nearestZombie = null;
        double bestDistance = Double.MAX_VALUE;
        double bestZombieDistance = Double.MAX_VALUE;

        for (Zombie zombie : session.getZombies()) {
            if (!isTargetable(user, zombie, lobbed)) continue;
            Position at = zombie.getPosition();
            if (!accepts.test(at)) continue;
            double distance = at.distanceTo(origin);
            if (distance < bestZombieDistance) {
                bestZombieDistance = distance;
                nearestZombie = zombie;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = PlantTarget.ofZombie(zombie);
            }
        }

        Environment environment = session.getEnvironment();
        if (environment != null) {
            for (int row = 0; row < environment.getRows(); row++) {
                for (int col = 0; col < environment.getCols(); col++) {
                    Cell cell = environment.getCell(row, col);
                    if (cell == null || !PlantTarget.isDestructible(cell.getObstacle())) continue;
                    Position at = new Position(col, row);
                    if (!accepts.test(at)) continue;
                    double distance = at.distanceTo(origin);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        nearest = PlantTarget.ofCell(cell);
                    }
                }
            }
        }

        for (PushableStructure structure : session.getPushableStructures()) {
            if (structure == null || !structure.isAlive()) continue;
            Position at = structure.getPosition();
            if (at == null || !accepts.test(at)) continue;
            double distance = at.distanceTo(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = PlantTarget.ofStructure(structure);
            }
        }

        return new TargetScan(nearest, nearestZombie);
    }

    public static boolean isInCone(double relX, double relY, double dx, double dy) {
        double dirLen = Math.sqrt(dx * dx + dy * dy);
        if (dirLen == 0) return false;

        if (dy == 0) {
            return Math.abs(relY) < VECTOR_ROW_TOLERANCE && Math.signum(relX) == Math.signum(dx);
        }

        if (isLaneShift(dx, dy)) {
            boolean correctXDir = Math.signum(relX) == Math.signum(dx) && Math.abs(relX) > 0;
            boolean correctRow = Math.abs(relY - dy) < VECTOR_ROW_TOLERANCE;
            return correctXDir && correctRow;
        }

        double relLen = Math.sqrt(relX * relX + relY * relY);
        if (relLen == 0) return false;
        double ndx = dx / dirLen;
        double ndy = dy / dirLen;
        double dot = (relX / relLen) * ndx + (relY / relLen) * ndy;
        return dot > CONE_DOT_THRESHOLD;
    }

    public static boolean isLaneShift(double dx, double dy) {
        return dx != 0 && Math.abs(Math.abs(dy) - 1.0) <= LANE_EPSILON;
    }
}
