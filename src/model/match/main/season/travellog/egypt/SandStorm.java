package model.match.main.season.travellog.egypt;

import model.match_mechanisms.ZombieWave;

import java.util.Random;

public final class SandStorm {
    private static final Random RANDOM = new Random();

    public static final String PAM_PATH_PLACEHOLDER = "768/INITIAL/EFFECTS/SANDSTORM_TOP/SANDSTORM_TOP.PAM";

    public static final double EVENT_DURATION_SECONDS = 3.9;
    public static final double INTRO_DURATION_SECONDS = 0.33;
    public static final double OUTRO_DURATION_SECONDS = 0.33;
    public static final double LOOP_DURATION_SECONDS =
            EVENT_DURATION_SECONDS - INTRO_DURATION_SECONDS - OUTRO_DURATION_SECONDS;
    public static final double ZOMBIE_ARRIVAL_MIN_SECONDS = 4.00;
    public static final double ZOMBIE_ARRIVAL_MAX_SECONDS = 5.50;
    public static final double REGULAR_WAVE_TRIGGER_CHANCE = 0.7;
    public static final double ENTRY_STAGGER_MAX_SECONDS = 0.75;

    private SandStorm() {
    }

    public static boolean shouldTrigger(ZombieWave wave, int waveIndex) {
        if (wave == null) return false;
        if (wave.isFinalWave()) return true;
        if (waveIndex <= 0) return false;
        return RANDOM.nextDouble() < REGULAR_WAVE_TRIGGER_CHANCE;
    }

    public static int randomRow(int rows) {
        if (rows <= 1) return 0;
        return RANDOM.nextInt(rows);
    }

    public static int randomLandingColumn(int cols) {
        if (cols <= 1) return 0;

        int maxTravelTiles = Math.min(2, cols - 1);
        int minTravelTiles = Math.min(1, maxTravelTiles);
        int travelTiles = minTravelTiles + RANDOM.nextInt(maxTravelTiles - minTravelTiles + 1);

        return Math.max(0, cols - 1 - travelTiles);
    }

    public static double entryX(int cols) {
        return cols + 3.0;
    }

    public static double entryDelaySeconds() {
        return RANDOM.nextDouble() * ENTRY_STAGGER_MAX_SECONDS;
    }

    public static double arrivalDurationSeconds() {
        return ZOMBIE_ARRIVAL_MIN_SECONDS
                + RANDOM.nextDouble() * (ZOMBIE_ARRIVAL_MAX_SECONDS - ZOMBIE_ARRIVAL_MIN_SECONDS);
    }
}