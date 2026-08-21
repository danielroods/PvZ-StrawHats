package model.match.waves;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;

import java.util.List;

public final class SpawnPlacement {

    public record Placement(int lane, double x) {
    }

    private SpawnPlacement() {
    }

    public static double entryX(int cols) {
        return cols + WavePacing.ENTRY_MARGIN_COLUMNS;
    }

    public static double corridorStart(int cols) {
        return cols - WavePacing.CORRIDOR_START_OFFSET_COLUMNS;
    }

    public static Placement resolve(List<Zombie> liveZombies, String alias, int cols,
                                    List<Integer> laneOrder) {
        return resolve(liveZombies, alias, cols, laneOrder, entryX(cols));
    }

    public static Placement resolve(List<Zombie> liveZombies, String alias, int cols,
                                    List<Integer> laneOrder, double baseX) {
        int bestLane = laneOrder.isEmpty() ? 0 : laneOrder.get(0);
        double bestX = Double.MAX_VALUE;

        for (int lane : laneOrder) {
            double required = requiredX(liveZombies, alias, cols, lane, baseX);
            if (required <= baseX + WavePacing.MAX_ENTRY_PUSHBACK_COLUMNS) {
                return new Placement(lane, required);
            }
            if (required < bestX) {
                bestX = required;
                bestLane = lane;
            }
        }
        return new Placement(bestLane, bestX == Double.MAX_VALUE ? baseX : bestX);
    }

    public static Placement resolveInward(List<Zombie> liveZombies, String alias, int cols,
                                          List<Integer> laneOrder, double baseX) {
        int bestLane = laneOrder.isEmpty() ? 0 : laneOrder.get(0);
        for (int lane : laneOrder) {
            for (double x = baseX; x >= 0.5; x -= 0.5) {
                if (isClear(liveZombies, alias, lane, x)) return new Placement(lane, x);
            }
        }
        return new Placement(bestLane, baseX);
    }

    private static boolean isClear(List<Zombie> liveZombies, String alias, int lane, double x) {
        return isClear(liveZombies, null, alias, lane, x);
    }

    private static boolean isClear(List<Zombie> liveZombies, Zombie ignored, String alias,
                                   int lane, double x) {
        for (Zombie other : liveZombies) {
            if (other == null || other == ignored || !other.isAlive()) continue;
            if (other.getPosition() == null) continue;
            if ((int) Math.round(other.getPosition().y()) != lane) continue;
            if (Math.abs(other.getPosition().x() - x)
                    < WavePacing.minSeparation(other.getAlias(), alias)) {
                return false;
            }
        }
        return true;
    }

    public static double clearSpot(List<Zombie> liveZombies, Zombie mover, int lane,
                                   double desiredX, double minX, double maxX) {
        if (mover == null) return desiredX;
        if (isClear(liveZombies, mover, mover.getAlias(), lane, desiredX)) return desiredX;
        for (double step = 0.25; step <= 4.0; step += 0.25) {
            double left = desiredX - step;
            if (left >= minX && isClear(liveZombies, mover, mover.getAlias(), lane, left)) {
                return left;
            }
            double right = desiredX + step;
            if (right <= maxX && isClear(liveZombies, mover, mover.getAlias(), lane, right)) {
                return right;
            }
        }
        return desiredX;
    }

    public static double requiredX(List<Zombie> liveZombies, String alias, int cols, int lane,
                                   double baseX) {
        double required = baseX;
        double corridorStart = corridorStart(cols) - 1.0;
        for (Zombie other : liveZombies) {
            if (other == null || !other.isAlive()) continue;
            Position position = other.getPosition();
            if (position == null) continue;
            if ((int) Math.round(position.y()) != lane) continue;
            if (position.x() < corridorStart) continue;
            double separation = WavePacing.minSeparation(other.getAlias(), alias);
            required = Math.max(required, position.x() + separation);
        }
        return required;
    }
}
