package model.utils;

import model.collections.zombie.Zombie;
import model.match.main.season.travellog.egypt.SandStorm;
import model.match.waves.SpawnPlacement;
import model.match_mechanisms.vector.Position;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-season entry hazards owned by a match: the Egypt sandstorm and the Big Wave Beach
 * huge wave. Holds their timers, their per-zombie entry animations, and the read-only
 * state {@link GameSession} exposes to the screens.
 */
class SessionHazards {

    static final double BEACH_BIG_WAVE_ENTRY_TARGET_OFFSET = 0.20;
    private static final double LANDING_MIN_COLUMN = 0.5;
    private static final double LANDING_MAX_COLUMN_INSET = 0.4;

    private static final double BEACH_BIG_WAVE_DURATION_SECONDS = 5.50;
    private static final double BEACH_BIG_WAVE_ENTRY_DURATION_SECONDS = 2.80;

    // How long a zombie visibly glides across a smooth row shift (tile sliders,
    // Garlic's redirect, Sweet Potato's pull) so the row change reads as a
    // glide instead of the zombie's row snapping in a single tick.
    static final double SLIDER_RIDE_DURATION_SECONDS = 0.55;

    private double sandStormTimer = 0.0;
    private boolean sandStormActive = false;
    private int sandStormWaveIndex = -1;
    private final Map<Zombie, SandStormEntry> sandStormEntries = new IdentityHashMap<>();

    private boolean beachBigWaveActive = false;
    private double beachBigWaveTimer = 0.0;
    private int beachBigWaveIndex = -1;
    private final Map<Zombie, BeachBigWaveEntry> beachBigWaveEntries = new IdentityHashMap<>();

    private final Map<Zombie, SliderRideEntry> sliderRideEntries = new IdentityHashMap<>();

    private final GameSession session;

    SessionHazards(GameSession session) {
        this.session = session;
    }

    /**
     * Where a hazard may actually set this zombie down. The spot it was promised at spawn time
     * can have been taken since, because the zombies that landed first have already walked on.
     */
    private double landingSpotFor(Zombie zombie, int row, double targetX) {
        if (session == null) return targetX;
        return SpawnPlacement.clearSpot(session.getZombies(), zombie, row, targetX,
                LANDING_MIN_COLUMN, session.getCols() - LANDING_MAX_COLUMN_INSET);
    }

    private static final class BeachBigWaveEntry {
        final int row;
        final double startX;
        final double targetX;
        double elapsed;

        BeachBigWaveEntry(int row, double startX, double targetX) {
            this.row = row;
            this.startX = startX;
            this.targetX = targetX;
        }
    }

    private static final class SandStormEntry {
        final int startRow;
        final int targetRow;
        final double targetX;
        final double startX;
        final double duration;
        final double startDelay;
        double elapsed;
        double animationElapsed;

        SandStormEntry(int startRow, int targetRow, double targetX, double startX,
                       double duration, double startDelay) {
            this.startRow = startRow;
            this.targetRow = targetRow;
            this.targetX = targetX;
            this.startX = startX;
            this.duration = duration;
            this.startDelay = startDelay;
        }
    }

    /** A zombie currently being carried sideways by a Frostbite Caves tile slider. */
    private static final class SliderRideEntry {
        double startX;
        final double startRow;
        final double targetRow;
        double elapsed;

        SliderRideEntry(double startX, double startRow, double targetRow) {
            this.startX = startX;
            this.startRow = startRow;
            this.targetRow = targetRow;
        }
    }

    void beginSandStorm(int waveIndex) {
        sandStormActive = true;
        sandStormTimer = SandStorm.EVENT_DURATION_SECONDS;
        sandStormWaveIndex = waveIndex;
    }

    void updateSandStorm(double deltaTimeSeconds) {
        if (sandStormTimer > 0) {
            sandStormTimer = Math.max(0, sandStormTimer - deltaTimeSeconds);
        }

        if (!sandStormEntries.isEmpty()) {
            var iterator = sandStormEntries.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Zombie, SandStormEntry> entry = iterator.next();
                Zombie zombie = entry.getKey();
                SandStormEntry stormEntry = entry.getValue();

                if (zombie == null || !zombie.isAlive()) {
                    iterator.remove();
                    continue;
                }

                stormEntry.elapsed += deltaTimeSeconds;
                stormEntry.animationElapsed += deltaTimeSeconds;
                if (stormEntry.elapsed < stormEntry.startDelay) {
                    zombie.setPosition(new Position(stormEntry.startX, stormEntry.startRow));
                    continue;
                }

                double travelElapsed = stormEntry.elapsed - stormEntry.startDelay;
                double progress = Math.min(1.0, travelElapsed / stormEntry.duration);
                double smooth = progress * progress * (3.0 - 2.0 * progress);
                double eased = 1.0 - Math.pow(1.0 - smooth, 2.2);
                double targetX = stormEntry.targetX;
                double x = stormEntry.startX + (targetX - stormEntry.startX) * eased;
                double y = stormEntry.startRow + (stormEntry.targetRow - stormEntry.startRow) * eased;
                zombie.setPosition(new Position(x, y));

                if (progress >= 1.0) {
                    double landingX = landingSpotFor(zombie, stormEntry.targetRow, targetX);
                    zombie.setPosition(new Position(landingX, stormEntry.targetRow));
                    Position speed = zombie.getSpeed();
                    if (speed != null) zombie.setSpeed(new Position(-Math.abs(speed.x()), 0));

                    if (stormEntry.animationElapsed >= SandStorm.EVENT_DURATION_SECONDS) {
                        iterator.remove();
                    }
                }
            }
        }

        if (sandStormActive && sandStormTimer <= 0 && sandStormEntries.isEmpty()) {
            sandStormActive = false;
            sandStormWaveIndex = -1;
        }
    }

    boolean isSandStormActive() {
        return sandStormActive;
    }

    double getSandStormRemainingSeconds() {
        return sandStormTimer;
    }

    double getSandStormProgress() {
        if (!sandStormActive) return 0.0;
        return 1.0 - Math.max(0.0, Math.min(1.0, sandStormTimer / SandStorm.EVENT_DURATION_SECONDS));
    }

    int getSandStormWaveIndex() {
        return sandStormWaveIndex;
    }

    boolean isZombieInSandStorm(Zombie zombie) {
        return zombie != null && sandStormEntries.containsKey(zombie);
    }

    double getSandStormAnimationTime(Zombie zombie) {
        SandStormEntry entry = zombie == null ? null : sandStormEntries.get(zombie);
        return entry == null ? -1.0 : entry.animationElapsed;
    }

    void beginBeachBigWave(int waveIndex) {
        beachBigWaveActive = true;
        beachBigWaveTimer = BEACH_BIG_WAVE_DURATION_SECONDS;
        beachBigWaveIndex = waveIndex;
    }

    void updateBeachBigWave(double deltaTimeSeconds) {
        if (beachBigWaveTimer > 0) {
            beachBigWaveTimer = Math.max(0.0, beachBigWaveTimer - deltaTimeSeconds);
        }

        if (!beachBigWaveEntries.isEmpty()) {
            var iterator = beachBigWaveEntries.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Zombie, BeachBigWaveEntry> entry = iterator.next();
                Zombie zombie = entry.getKey();
                BeachBigWaveEntry waveEntry = entry.getValue();

                if (zombie == null || !zombie.isAlive()) {
                    iterator.remove();
                    continue;
                }

                waveEntry.elapsed += deltaTimeSeconds;
                double progress = Math.min(1.0, waveEntry.elapsed / BEACH_BIG_WAVE_ENTRY_DURATION_SECONDS);
                double smooth = progress * progress * (3.0 - 2.0 * progress);
                double x = waveEntry.startX + (waveEntry.targetX - waveEntry.startX) * smooth;
                zombie.setPosition(new Position(x, waveEntry.row));

                if (progress >= 1.0) {
                    double landingX = landingSpotFor(zombie, waveEntry.row, waveEntry.targetX);
                    zombie.setPosition(new Position(landingX, waveEntry.row));
                    Position speed = zombie.getSpeed();
                    if (speed != null) zombie.setSpeed(new Position(-Math.abs(speed.x()), 0));
                    iterator.remove();
                }
            }
        }

        if (beachBigWaveActive && beachBigWaveTimer <= 0 && beachBigWaveEntries.isEmpty()) {
            beachBigWaveActive = false;
            beachBigWaveIndex = -1;
        }
    }

    boolean isBeachBigWaveActive() {
        return beachBigWaveActive;
    }

    double getBeachBigWaveProgress() {
        if (!beachBigWaveActive) return 0.0;
        return 1.0 - Math.max(0.0, Math.min(1.0, beachBigWaveTimer / BEACH_BIG_WAVE_DURATION_SECONDS));
    }

    boolean isBeachBigWaveCrash() {
        return beachBigWaveActive && getBeachBigWaveProgress() >= 0.72;
    }

    int getBeachBigWaveIndex() {
        return beachBigWaveIndex;
    }

    boolean isZombieInBeachBigWave(Zombie zombie) {
        return zombie != null && beachBigWaveEntries.containsKey(zombie);
    }

    double getBeachBigWaveEntryProgress(Zombie zombie) {
        BeachBigWaveEntry entry = zombie == null ? null : beachBigWaveEntries.get(zombie);
        if (entry == null) return -1.0;
        return Math.min(1.0, entry.elapsed / BEACH_BIG_WAVE_ENTRY_DURATION_SECONDS);
    }

    boolean isEnteringWithHazard(Zombie zombie) {
        return sandStormEntries.containsKey(zombie) || beachBigWaveEntries.containsKey(zombie)
                || sliderRideEntries.containsKey(zombie);
    }

    /**
     * Starts (or restarts) a smooth row glide for a zombie, instead of the
     * row snapping instantly. Originally built for Frostbite Caves tile
     * sliders, this is now the shared row-change glide used by Garlic's
     * redirect and Sweet Potato's pull as well. No-op if the zombie is
     * already gliding to this exact target row (so a mover calling this
     * every tick while still on the same tile doesn't reset the animation
     * each frame).
     */
    void beginSliderRide(Zombie zombie, double currentX, int fromRow, int toRow) {
        if (zombie == null) return;
        SliderRideEntry existing = sliderRideEntries.get(zombie);
        if (existing != null && existing.targetRow == toRow) return;
        sliderRideEntries.put(zombie, new SliderRideEntry(currentX, fromRow, toRow));
    }

    boolean isRidingSlider(Zombie zombie) {
        return zombie != null && sliderRideEntries.containsKey(zombie);
    }

    void updateSliderRide(double deltaTimeSeconds) {
        if (sliderRideEntries.isEmpty()) return;

        var iterator = sliderRideEntries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Zombie, SliderRideEntry> entry = iterator.next();
            Zombie zombie = entry.getKey();
            SliderRideEntry ride = entry.getValue();

            if (zombie == null || !zombie.isAlive()) {
                iterator.remove();
                continue;
            }

            Position speed = zombie.getSpeed();
            double stepX = speed != null ? speed.x() * deltaTimeSeconds : 0.0;

            ride.elapsed += deltaTimeSeconds;
            double progress = Math.min(1.0, ride.elapsed / SLIDER_RIDE_DURATION_SECONDS);
            double smooth = progress * progress * (3.0 - 2.0 * progress);
            double y = ride.startRow + (ride.targetRow - ride.startRow) * smooth;
            // The zombie keeps walking forward at its own speed the whole time; only
            // the row is eased, so the ride's x anchor advances by the same step the
            // normal walk would have taken this tick.
            ride.startX = ride.startX + stepX;
            zombie.setPosition(new Position(ride.startX, y));

            if (progress >= 1.0) {
                zombie.setPosition(new Position(ride.startX, ride.targetRow));
                iterator.remove();
            }
        }
    }

    void addSandStormEntry(Zombie zombie, int startRow, int targetRow, double targetX,
                           double startX, double duration, double startDelay) {
        sandStormEntries.put(zombie, new SandStormEntry(startRow, targetRow, targetX,
                startX, duration, startDelay));
    }

    void addBeachBigWaveEntry(Zombie zombie, int row, double startX, double targetX) {
        beachBigWaveEntries.put(zombie, new BeachBigWaveEntry(row, startX, targetX));
    }

    void pruneSandStormEntries(List<Zombie> zombies) {
        sandStormEntries.entrySet().removeIf(entry -> !entry.getKey().isAlive() || !zombies.contains(entry.getKey()));
    }

    void pruneSliderRideEntries(List<Zombie> zombies) {
        sliderRideEntries.entrySet().removeIf(entry -> !entry.getKey().isAlive() || !zombies.contains(entry.getKey()));
    }

    /** Clears every hazard entry animation, e.g. when a fresh level is loaded. */
    void reset() {
        sandStormEntries.clear();
        sandStormActive = false;
        sandStormTimer = 0.0;
        sandStormWaveIndex = -1;
        sliderRideEntries.clear();
        resetBeachBigWave();
    }

    void resetBeachBigWave() {
        beachBigWaveEntries.clear();
        beachBigWaveActive = false;
        beachBigWaveTimer = 0.0;
        beachBigWaveIndex = -1;
    }
}