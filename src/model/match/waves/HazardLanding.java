package model.match.waves;

import model.collections.zombie.Zombie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HazardLanding {

    private final Map<Integer, List<Claim>> claimsByRow = new HashMap<>();

    private record Claim(double x, String alias) {
    }

    public void clear() {
        claimsByRow.clear();
    }

    public double claim(int row, String alias, double desiredX, double minX, double maxX,
                        List<Zombie> liveZombies) {
        List<Claim> claims = claimsByRow.computeIfAbsent(row, key -> new ArrayList<>());
        List<Claim> blockers = new ArrayList<>(claims);
        addStandingZombies(blockers, row, liveZombies);
        double x = WavePacing.clamp(desiredX, minX, maxX);

        double pushedRight = resolve(blockers, alias, x, 1, minX, maxX);
        if (!Double.isNaN(pushedRight)) {
            claims.add(new Claim(pushedRight, alias));
            return pushedRight;
        }
        double pushedLeft = resolve(blockers, alias, x, -1, minX, maxX);
        if (!Double.isNaN(pushedLeft)) {
            claims.add(new Claim(pushedLeft, alias));
            return pushedLeft;
        }

        claims.add(new Claim(x, alias));
        return x;
    }

    private void addStandingZombies(List<Claim> blockers, int row, List<Zombie> liveZombies) {
        if (liveZombies == null) return;
        for (Zombie zombie : liveZombies) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            if ((int) Math.round(zombie.getPosition().y()) != row) continue;
            blockers.add(new Claim(zombie.getPosition().x(), zombie.getAlias()));
        }
    }

    private double resolve(List<Claim> claims, String alias, double startX, int direction,
                           double minX, double maxX) {
        double x = startX;
        for (int attempt = 0; attempt <= claims.size(); attempt++) {
            Claim blocker = firstConflict(claims, alias, x);
            if (blocker == null) return x;
            double separation = WavePacing.minSeparation(blocker.alias(), alias);
            x = direction > 0 ? blocker.x() + separation : blocker.x() - separation;
            if (x < minX || x > maxX) return Double.NaN;
        }
        return Double.NaN;
    }

    private Claim firstConflict(List<Claim> claims, String alias, double x) {
        for (Claim claim : claims) {
            if (Math.abs(claim.x() - x) < WavePacing.minSeparation(claim.alias(), alias)) {
                return claim;
            }
        }
        return null;
    }
}
