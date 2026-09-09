package model.match.endless;

import model.collections.zombie.Zombie;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Live bookkeeping for one endless (Lottery) run: how long it has lasted, how many
 * zombies it has killed, and the score those kills are worth.
 * <p>
 * The scoring is the five-pattern Meow Point formula the Lottery mode already used -
 * piercing bonus, how fast the zombie went down, how many died at once, the running
 * combo, all multiplied by a difficulty factor that climbs with match time. Two things
 * changed, both because the old version could not produce a correct number: the "at
 * once" window and the difficulty ramp are measured in match time instead of wall-clock
 * milliseconds (so pausing or the 2x/3x speed button no longer distorts them), and the
 * running total is a saturating {@code long} with a capped multiplier so an endless run
 * can never wrap it around.
 */
public final class EndlessRun {

    public static final double SCORE_MULTIPLIER_RAMP_SECONDS = 20.0;
    public static final double MAX_SCORE_MULTIPLIER = 100.0;

    private static final double SIMULTANEOUS_KILL_WINDOW_SECONDS = 1.0;
    private static final int PIERCING_POINTS = 25;
    private static final int PLAIN_KILL_POINTS = 5;
    private static final int FAST_KILL_POINTS = 30;
    private static final int MEDIUM_KILL_POINTS = 15;
    private static final int SLOW_KILL_POINTS = 5;
    private static final double FAST_KILL_SECONDS = 3.0;
    private static final double MEDIUM_KILL_SECONDS = 6.0;
    private static final int CROWD_KILL_POINTS = 50;
    private static final int PAIR_KILL_POINTS = 20;
    private static final int COMBO_POINTS_PER_KILL = 2;
    private static final int MAX_COMBO_POINTS = 40;
    private static final double PIERCING_WEIGHT = 1.5;
    private static final double SPEED_WEIGHT = 2.0;
    private static final double CROWD_WEIGHT = 2.5;
    private static final double COMBO_WEIGHT = 1.0;

    private final Map<Zombie, Double> spawnTimes = new IdentityHashMap<>();
    private final Deque<Double> recentKillTimes = new ArrayDeque<>();

    private double elapsedSeconds;
    private long score;
    private int kills;
    private int combo;

    public void onZombieSpawned(Zombie zombie, double atSeconds) {
        if (zombie == null) return;
        elapsedSeconds = Math.max(elapsedSeconds, atSeconds);
        spawnTimes.put(zombie, atSeconds);
    }

    public int onZombieKilled(Zombie zombie, double atSeconds) {
        elapsedSeconds = Math.max(elapsedSeconds, atSeconds);
        Double spawnedAt = zombie == null ? null : spawnTimes.remove(zombie);
        double timeAlive = spawnedAt == null ? Double.MAX_VALUE : atSeconds - spawnedAt;

        kills++;
        combo++;
        recentKillTimes.addLast(atSeconds);
        while (!recentKillTimes.isEmpty()
                && atSeconds - recentKillTimes.peekFirst() > SIMULTANEOUS_KILL_WINDOW_SECONDS) {
            recentKillTimes.removeFirst();
        }

        int award = pointsFor(timeAlive, recentKillTimes.size(), combo, false, atSeconds);
        score = saturatingAdd(score, award);
        return award;
    }

    /**
     * Drops spawn times for zombies that left the lawn without dying (a wiped board, a
     * mini-game style reset), so a run that lasts for hours cannot accumulate them.
     */
    public void pruneSpawnTimes(List<Zombie> liveZombies) {
        if (spawnTimes.isEmpty()) return;
        if (liveZombies == null || liveZombies.isEmpty()) {
            spawnTimes.clear();
            return;
        }
        if (spawnTimes.size() <= liveZombies.size()) return;
        Map<Zombie, Boolean> live = new IdentityHashMap<>();
        for (Zombie zombie : liveZombies) live.put(zombie, Boolean.TRUE);
        spawnTimes.keySet().removeIf(zombie -> !live.containsKey(zombie));
    }

    public void setElapsedSeconds(double seconds) {
        elapsedSeconds = Math.max(0, seconds);
    }

    public static double scoreMultiplierAt(double elapsedSeconds) {
        double ramped = 1.0 + Math.max(0, elapsedSeconds) / SCORE_MULTIPLIER_RAMP_SECONDS;
        return Math.min(MAX_SCORE_MULTIPLIER, ramped);
    }

    public long getScore() {
        return score;
    }

    public int getKills() {
        return kills;
    }

    public int getCombo() {
        return combo;
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    private int pointsFor(double timeAlive, int simultaneousKills, int comboCount,
                          boolean piercing, double atSeconds) {
        int piercingPoints = piercing ? PIERCING_POINTS : PLAIN_KILL_POINTS;
        int speedPoints = timeAlive <= FAST_KILL_SECONDS ? FAST_KILL_POINTS
                : timeAlive <= MEDIUM_KILL_SECONDS ? MEDIUM_KILL_POINTS : SLOW_KILL_POINTS;
        int crowdPoints = simultaneousKills >= 4 ? CROWD_KILL_POINTS
                : simultaneousKills >= 2 ? PAIR_KILL_POINTS : 0;
        int comboPoints = Math.min(comboCount * COMBO_POINTS_PER_KILL, MAX_COMBO_POINTS);

        double raw = piercingPoints * PIERCING_WEIGHT
                + speedPoints * SPEED_WEIGHT
                + crowdPoints * CROWD_WEIGHT
                + comboPoints * COMBO_WEIGHT;
        return (int) Math.round(raw * scoreMultiplierAt(atSeconds));
    }

    private static long saturatingAdd(long total, int award) {
        long sum = total + award;
        return sum < total ? Long.MAX_VALUE : sum;
    }
}
