package model.match.waves;

import model.collections.zombie.ZombieFactory;
import model.collections.zombie.ZombieRace;

public final class WavePacing {

    public static final double DEFAULT_WAVE_INTERVAL_SECONDS = 25.0;

    public static final double LULL_MULTIPLIER = 1.30;
    public static final double FIRST_WAVE_MIN_SECONDS = 16.0;
    public static final double MIN_WAVE_INTERVAL_SECONDS = 12.0;
    public static final double MIN_GAP_AFTER_SPAWN_SECONDS = 5.0;
    public static final double INTERVAL_RAMP = 0.08;
    public static final double HUGE_WAVE_INTERVAL_BONUS = 1.25;
    public static final double HUGE_WAVE_ALERT_LEAD_SECONDS = 6.5;

    public static final double EARLY_TRIGGER_AT = 0.85;
    public static final double EARLY_TRIGGER_PRESSURE = 0.12;

    public static final double BASE_SPAWN_GAP_SECONDS = 1.90;
    public static final double MIN_SPAWN_GAP_SECONDS = 0.70;
    public static final double MAX_SPAWN_GAP_SECONDS = 5.00;

    public static final double CLUSTER_BEAT_FACTOR = 0.35;
    public static final double CLUSTER_PAUSE_FACTOR = 1.90;

    public static final double ENTRY_MARGIN_COLUMNS = 0.75;
    public static final double MAX_ENTRY_PUSHBACK_COLUMNS = 2.60;
    public static final double CORRIDOR_START_OFFSET_COLUMNS = 0.60;
    public static final double SPACING_PADDING_COLUMNS = 0.10;

    private static final double IMP_WIDTH_COLUMNS = 0.50;
    private static final double DEFAULT_WIDTH_COLUMNS = 0.80;
    private static final double GARGANTUAR_WIDTH_COLUMNS = 1.30;

    private static final double REFERENCE_SPEED = 0.185;

    private WavePacing() {
    }

    public static double laneWidth(ZombieRace race) {
        if (race == ZombieRace.IMP) return IMP_WIDTH_COLUMNS;
        if (race == ZombieRace.GARGANTUAR) return GARGANTUAR_WIDTH_COLUMNS;
        return DEFAULT_WIDTH_COLUMNS;
    }

    public static double laneWidth(String alias) {
        return laneWidth(ZombieFactory.getZombieRace(alias));
    }

    public static double minSeparation(String leaderAlias, String followerAlias) {
        return 0.5 * (laneWidth(leaderAlias) + laneWidth(followerAlias)) + SPACING_PADDING_COLUMNS;
    }

    public static double leadFactor(String alias) {
        double factor = 1.0;
        ZombieRace race = ZombieFactory.getZombieRace(alias);
        if (race == ZombieRace.GARGANTUAR) {
            factor *= 1.90;
        } else if (race == ZombieRace.IMP) {
            factor *= 0.62;
        }

        int cost = ZombieFactory.getZombieCost(alias);
        if (cost >= 900) {
            factor *= 1.35;
        } else if (cost >= 500) {
            factor *= 1.18;
        } else if (cost >= 300) {
            factor *= 1.06;
        }

        double speed = ZombieFactory.getZombieSpeed(alias);
        if (speed > 0) {
            factor *= clamp(REFERENCE_SPEED / speed, 0.85, 1.45);
        }
        return factor;
    }

    public static double vanguardScore(String alias) {
        double score = ZombieFactory.getZombieCost(alias) / 100.0;
        double speed = ZombieFactory.getZombieSpeed(alias);
        if (speed > 0) score += clamp(REFERENCE_SPEED / speed, 0.4, 2.5);
        ZombieRace race = ZombieFactory.getZombieRace(alias);
        if (race == ZombieRace.GARGANTUAR) score += 6.0;
        if (race == ZombieRace.IMP) score -= 2.0;
        return score;
    }

    public static double densityFactor(int waveSize) {
        return clamp(2.10 / (1.0 + 0.16 * Math.max(0, waveSize - 1)), 0.50, 1.30);
    }

    public static double waveTypeFactor(WaveType type) {
        return switch (type) {
            case FINAL -> 0.68;
            case FLAG -> 0.82;
            default -> 1.0;
        };
    }

    public static double progressionFactor(int waveIndex) {
        return Math.pow(0.97, Math.max(0, waveIndex));
    }

    public static int clusterSize(int waveSize, WaveType type) {
        int size;
        if (waveSize <= 3) {
            size = 1;
        } else if (waveSize <= 6) {
            size = 2;
        } else if (waveSize <= 10) {
            size = 3;
        } else {
            size = 4;
        }
        if (type.isHuge()) size++;
        return Math.max(1, Math.min(size, waveSize));
    }

    public static double difficultyFactor(int difficultyLevel) {
        if (difficultyLevel <= 0) return 1.0;
        return clamp(1.0 - 0.05 * (difficultyLevel - 1), 0.80, 1.10);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
