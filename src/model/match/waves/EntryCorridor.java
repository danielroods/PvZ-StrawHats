package model.match.waves;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class EntryCorridor {

    private static final double MAX_QUEUE_DEPTH_COLUMNS = 4.0;

    private EntryCorridor() {
    }

    public static void separate(List<Zombie> zombies, int rows, int cols,
                                Predicate<Zombie> exempt) {
        if (zombies == null || zombies.isEmpty()) return;
        double corridorStart = SpawnPlacement.corridorStart(cols);

        for (int lane = 0; lane < rows; lane++) {
            List<Zombie> inLane = new ArrayList<>();
            for (Zombie zombie : zombies) {
                if (zombie == null || !zombie.isAlive()) continue;
                if (exempt != null && exempt.test(zombie)) continue;
                if (zombie.isHypnotized()) continue;
                Position position = zombie.getPosition();
                if (position == null || position.x() < corridorStart) continue;
                if ((int) Math.round(position.y()) != lane) continue;
                inLane.add(zombie);
            }
            if (inLane.size() < 2) continue;

            inLane.sort(Comparator.comparingDouble(z -> z.getPosition().x()));
            double queueLimit = cols + MAX_QUEUE_DEPTH_COLUMNS;
            for (int i = 1; i < inLane.size(); i++) {
                Zombie leader = inLane.get(i - 1);
                Zombie follower = inLane.get(i);
                double minX = leader.getPosition().x()
                        + WavePacing.minSeparation(leader.getAlias(), follower.getAlias());
                if (minX > queueLimit) continue;
                if (follower.getPosition().x() < minX) {
                    follower.setPosition(new Position(minX, follower.getPosition().y()));
                }
            }
        }
    }
}
