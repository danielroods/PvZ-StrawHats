package model.projectile.targeting;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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

    public static List<PlantTarget> allWithin(Plant user, GameSession session,
                                              Predicate<Position> area, boolean lobbed) {
        List<PlantTarget> targets = new ArrayList<>();
        if (user == null || area == null || user.getPosition() == null) return targets;
        forEachTarget(user, session, lobbed, area, targets::add);
        return targets;
    }

    public static Predicate<Position> box(Position origin, double reachX, double reachY) {
        return at -> Math.abs(at.x() - origin.x()) <= reachX
                && Math.abs(at.y() - origin.y()) <= reachY;
    }

    public static Predicate<Position> within(Position origin, double radius) {
        return at -> at.distanceTo(origin) <= radius;
    }

    public static Predicate<Position> sameRowWithin(Position origin, double rowTolerance,
                                                    double range, double selfTileEpsilon) {
        return at -> Math.abs(at.y() - origin.y()) < rowTolerance
                && Math.abs(at.x() - origin.x()) <= range
                && Math.abs(at.x() - origin.x()) > selfTileEpsilon;
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
        Position origin = user.getPosition();
        NearestCollector collector = new NearestCollector(origin);
        forEachTarget(user, session, lobbed, accepts, collector);
        return collector.result();
    }

    private static void forEachTarget(Plant user, GameSession session, boolean lobbed,
                                      Predicate<Position> accepts, Consumer<PlantTarget> sink) {
        if (session == null) return;

        for (Zombie zombie : session.getZombies()) {
            if (!isTargetable(user, zombie, lobbed)) continue;
            if (accepts.test(zombie.getPosition())) sink.accept(PlantTarget.ofZombie(zombie));
        }

        Environment environment = session.getEnvironment();
        if (environment != null) {
            for (int row = 0; row < environment.getRows(); row++) {
                for (int col = 0; col < environment.getCols(); col++) {
                    Cell cell = environment.getCell(row, col);
                    if (cell == null || !PlantTarget.isDestructible(cell.getObstacle())) continue;
                    if (accepts.test(new Position(col, row))) sink.accept(PlantTarget.ofCell(cell));
                }
            }
        }

        for (PushableStructure structure : session.getPushableStructures()) {
            if (structure == null || !structure.isAlive()) continue;
            Position at = structure.getPosition();
            if (at != null && accepts.test(at)) sink.accept(PlantTarget.ofStructure(structure));
        }
    }

    private static final class NearestCollector implements Consumer<PlantTarget> {
        private final Position origin;
        private PlantTarget nearest;
        private Zombie nearestZombie;
        private double bestDistance = Double.MAX_VALUE;
        private double bestZombieDistance = Double.MAX_VALUE;

        private NearestCollector(Position origin) {
            this.origin = origin;
        }

        @Override
        public void accept(PlantTarget target) {
            Position at = target.getPosition();
            if (at == null) return;
            double distance = at.distanceTo(origin);
            if (target.isZombie() && distance < bestZombieDistance) {
                bestZombieDistance = distance;
                nearestZombie = target.getZombie();
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = target;
            }
        }

        private TargetScan result() {
            return new TargetScan(nearest, nearestZombie);
        }
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
