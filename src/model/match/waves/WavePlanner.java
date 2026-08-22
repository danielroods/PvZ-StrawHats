package model.match.waves;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class WavePlanner {

    private final Random random;

    public WavePlanner(Random random) {
        this.random = random == null ? new Random() : random;
    }

    public static WaveType classify(int waveIndex, int totalWaves) {
        if (totalWaves <= 0) return WaveType.NORMAL;
        if (waveIndex >= totalWaves - 1) return WaveType.FINAL;
        if (totalWaves >= 5 && waveIndex == (totalWaves - 1) / 2) return WaveType.FLAG;
        return WaveType.NORMAL;
    }

    public static double intervalSeconds(double authoredDelay, int waveIndex, int totalWaves,
                                         WaveType type, int difficultyLevel) {
        double base = authoredDelay > 0 ? authoredDelay : WavePacing.DEFAULT_WAVE_INTERVAL_SECONDS;
        double progress = totalWaves > 1 ? waveIndex / (double) (totalWaves - 1) : 0;
        double interval = base * WavePacing.LULL_MULTIPLIER
                * (1.0 - WavePacing.INTERVAL_RAMP * progress);
        interval *= WavePacing.difficultyFactor(difficultyLevel);
        if (type == WaveType.FINAL) interval *= WavePacing.HUGE_WAVE_INTERVAL_BONUS;
        double floor = waveIndex == 0
                ? Math.max(WavePacing.MIN_WAVE_INTERVAL_SECONDS, WavePacing.FIRST_WAVE_MIN_SECONDS)
                : WavePacing.MIN_WAVE_INTERVAL_SECONDS;
        return Math.max(floor, interval) * WavePacing.SPAWN_TIMING_MULTIPLIER;
    }

    public WavePlan plan(List<String> aliases, int waveIndex, int totalWaves, WaveType type,
                         int difficultyLevel, LaneBag laneBag) {
        List<ScheduledSpawn> spawns = new ArrayList<>();
        if (aliases == null || aliases.isEmpty()) {
            return new WavePlan(waveIndex, type, spawns);
        }

        List<String> ordered = orderForEntry(aliases);
        double density = WavePacing.densityFactor(ordered.size());
        double typeFactor = WavePacing.waveTypeFactor(type);
        double progression = WavePacing.progressionFactor(waveIndex);
        double difficulty = WavePacing.difficultyFactor(difficultyLevel);

        int clusterSize = WavePacing.clusterSize(ordered.size(), type);
        double offset = 0;
        for (int i = 0; i < ordered.size(); i++) {
            String alias = ordered.get(i);
            spawns.add(new ScheduledSpawn(alias, offset, laneBag.draw()));

            double gap = baseGapFor(alias, density, typeFactor, progression, difficulty);
            boolean closesCluster = (i + 1) % clusterSize == 0;
            gap *= closesCluster ? WavePacing.CLUSTER_PAUSE_FACTOR : WavePacing.CLUSTER_BEAT_FACTOR;
            double clampedGap = WavePacing.clamp(gap,
                    WavePacing.MIN_SPAWN_GAP_SECONDS, WavePacing.MAX_SPAWN_GAP_SECONDS);
            offset += clampedGap * WavePacing.SPAWN_TIMING_MULTIPLIER;
        }
        return new WavePlan(waveIndex, type, spawns);
    }

    private double baseGapFor(String alias, double density, double typeFactor,
                              double progression, double difficulty) {
        return WavePacing.BASE_SPAWN_GAP_SECONDS
                * density * typeFactor * progression * difficulty
                * WavePacing.leadFactor(alias)
                * jitter();
    }

    private List<String> orderForEntry(List<String> aliases) {
        List<String> ordered = new ArrayList<>(aliases);
        ordered.sort(Comparator.comparingDouble(WavePacing::vanguardScore).reversed());
        for (int i = 0; i + 1 < ordered.size(); i++) {
            if (random.nextDouble() < 0.30) {
                String swap = ordered.get(i);
                ordered.set(i, ordered.get(i + 1));
                ordered.set(i + 1, swap);
            }
        }
        return ordered;
    }

    private double jitter() {
        return 0.85 + random.nextDouble() * 0.35;
    }
}