package model.utils;

import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match.main.levels.Level;
import model.match.main.season.travellog.beach.Beach;
import model.match.main.season.travellog.beach.Flood;
import model.match.main.season.travellog.egypt.Egypt;
import model.match.main.season.travellog.egypt.SandStorm;
import model.match.waves.EntryCorridor;
import model.match.waves.HazardLanding;
import model.match.waves.LaneBag;
import model.match.waves.ScheduledSpawn;
import model.match.waves.SpawnPlacement;
import model.match.waves.WavePacing;
import model.match.waves.WavePlan;
import model.match.waves.WavePlanner;
import model.match.waves.WaveType;
import model.match_mechanisms.ZombieWave;
import model.match_mechanisms.vector.Position;
import service.GameClock;
import view.GeneralPrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class WaveScheduler {

    private static final double HAZARD_SPAWN_WINDOW_SECONDS = 4.0;
    private static final double SANDSTORM_SAME_LANE_CHANCE = 0.65;
    private static final double LANDING_MIN_COLUMN = 0.5;
    private static final double LANDING_MAX_COLUMN_INSET = 0.4;

    private final GameSession session;
    private final Random random = new Random();
    private final WavePlanner planner = new WavePlanner(random);
    private final LaneBag laneBag = new LaneBag(5, random);
    private final HazardLanding landings = new HazardLanding();

    private List<ZombieWave> waves = new ArrayList<>();
    private int nextWaveIndex = 0;
    private boolean wavesStarted = false;
    private double wavesStartedAtSeconds = 0;

    private double clockSeconds = 0;
    private double lullStartedAt = 0;
    private double currentInterval = 0;
    private double lastSpawnEndedAt = 0;
    private boolean hugeWaveAlertShown = false;
    private double waveProgress = 0;

    private WavePlan activePlan;
    private int planCursor = 0;
    private double planStartedAt = 0;
    private boolean activeSandStorm = false;
    private boolean activeBeachBigWave = false;

    private List<Zombie> currentWaveZombies = new ArrayList<>();
    private int currentWaveStartingHp = 0;

    WaveScheduler(GameSession session) {
        this.session = session;
    }

    void tickWaveScheduler(double deltaTimeSeconds) {
        clockSeconds += deltaTimeSeconds;
        announceIncomingHugeWave();
        tryStartNextWave();
        advanceActivePlan();
        advanceWaveProgress();
        EntryCorridor.separate(session.getZombies(), session.getRows(), session.getCols(),
                zombie -> session.hazards().isEnteringWithHazard(zombie)
                        || session.isFrozenInIceBlock(zombie)
                        || ZombieFactory.isStationaryMover(zombie.getAlias()));
    }

    private void announceIncomingHugeWave() {
        if (activePlan != null || nextWaveIndex >= waves.size() || hugeWaveAlertShown) return;
        if (!waveTypeOf(nextWaveIndex).isHuge()) return;
        double alertAt = Math.max(lullStartedAt,
                predictedNextWaveStart() - WavePacing.HUGE_WAVE_ALERT_LEAD_SECONDS);
        if (!GameClock.hasReached(clockSeconds, alertAt)) return;
        hugeWaveAlertShown = true;
        GeneralPrinter.print("A huge wave of zombies is approaching!");
    }

    private void tryStartNextWave() {
        if (activePlan != null || nextWaveIndex >= waves.size()) return;

        boolean due = GameClock.hasReached(clockSeconds, lullStartedAt + currentInterval);
        if (!due && !canBePulledInEarly()) return;
        if (!GameClock.hasReached(clockSeconds,
                lastSpawnEndedAt + WavePacing.minGapAfterSpawnSeconds())) {
            return;
        }
        startWave(waves.get(nextWaveIndex));
    }

    private boolean canBePulledInEarly() {
        if (nextWaveIndex == 0) return false;
        double earliest = lullStartedAt + currentInterval * WavePacing.EARLY_TRIGGER_AT;
        if (!GameClock.hasReached(clockSeconds, earliest)) return false;
        return remainingWavePressure() <= WavePacing.EARLY_TRIGGER_PRESSURE;
    }

    private double remainingWavePressure() {
        if (currentWaveStartingHp <= 0) return 0;
        int remainingHp = 0;
        for (Zombie zombie : currentWaveZombies) {
            if (zombie.isAlive()) remainingHp += zombie.getHp();
        }
        return remainingHp / (double) currentWaveStartingHp;
    }

    private void advanceActivePlan() {
        if (activePlan == null) return;

        List<ScheduledSpawn> spawns = activePlan.getSpawns();
        while (planCursor < spawns.size()
                && GameClock.hasReached(clockSeconds,
                planStartedAt + spawns.get(planCursor).offsetSeconds())) {
            spawnScheduled(spawns.get(planCursor));
            planCursor++;
        }
        if (planCursor >= spawns.size()) {
            lastSpawnEndedAt = clockSeconds;
            activePlan = null;
            activeSandStorm = false;
            activeBeachBigWave = false;
            landings.clear();
        }
    }

    private void startWave(ZombieWave wave) {
        Level level = session.getLevel();
        int waveIndex = nextWaveIndex;
        WaveType type = waveTypeOf(waveIndex);

        lullStartedAt = clockSeconds;
        laneBag.reset(session.getRows());
        landings.clear();
        currentWaveZombies = new ArrayList<>();
        currentWaveStartingHp = 0;

        if (type == WaveType.FINAL) {
            GeneralPrinter.print("The final wave has come.");
        } else {
            GeneralPrinter.print("Wave " + (waveIndex + 1) + " started.");
        }
        GeneralPrinter.print("Wave difficulty: " + wave.getWaveCost() + ".");

        if (level != null && level.getSeason() != null) {
            try {
                level.getSeason().onWaveStart(session, waveIndex);
            } catch (Exception e) {
                service.Log.error("GameSession",
                        "Season.onWaveStart() failed for wave " + (waveIndex + 1), e);
            }
        }

        beginSeasonHazards(wave, waveIndex, level);
        if (type.isHuge()) spawnCosmeticFlagZombie();

        WavePlan plan = planner.plan(entryAliases(wave), waveIndex, waves.size(), type,
                session.getDifficultyLevel(), laneBag);
        if (activeSandStorm || activeBeachBigWave) {
            plan = plan.compressed(HAZARD_SPAWN_WINDOW_SECONDS);
        }

        activePlan = plan;
        planCursor = 0;
        planStartedAt = clockSeconds;
        nextWaveIndex++;
        hugeWaveAlertShown = false;
        currentInterval = nextWaveIndex < waves.size()
                ? WavePlanner.intervalSeconds(waves.get(nextWaveIndex).getDelay(), nextWaveIndex,
                waves.size(), waveTypeOf(nextWaveIndex), session.getDifficultyLevel())
                : 0;
    }

    /**
     * Purely cosmetic: spawns a single flag zombie at the start of a huge wave (the flag and
     * final waves the progress meter plants a flag on) so the player sees the "wave incoming"
     * banner-carrier at the same moment the meter reaches that flag. This zombie is NOT part of
     * the level's authored wave data, is NOT counted in wave cost / difficulty
     * calculations, and is NOT drawn from the level's zombie pool - it is added
     * straight to the on-screen zombie list only, so it never touches
     * registerWaveZombie/currentWaveZombies/currentWaveStartingHp or entryAliases().
     */
    private void spawnCosmeticFlagZombie() {
        try {
            int cols = session.getCols();
            int rows = session.getRows();
            int lane = random.nextInt(Math.max(1, rows));
            Zombie flag = ZombieFactory.create("ZombieFlag", lane, Math.max(0, cols - 1));
            flag.setPosition(new Position(SpawnPlacement.entryX(cols), lane));
            session.spawnZombie(flag);
        } catch (Exception e) {
            // Never let the cosmetic flag zombie break wave spawning / level loading.
            service.Log.error("GameSession", "Failed to spawn cosmetic flag zombie", e);
        }
    }

    private void beginSeasonHazards(ZombieWave wave, int waveIndex, Level level) {
        activeSandStorm = false;
        activeBeachBigWave = false;
        if (level == null || level.getSeason() == null) return;

        if (level.getSeason() instanceof Beach beach && beach.isBigWave(wave)) {
            activeBeachBigWave = true;
            session.hazards().beginBeachBigWave(waveIndex);
            Flood.applyBigWaveWash(level, session);
            GeneralPrinter.print("A huge wave is rushing across the beach!");
        } else if (level.getSeason() instanceof Egypt && SandStorm.shouldTrigger(wave, waveIndex)) {
            activeSandStorm = true;
            session.hazards().beginSandStorm(waveIndex);
            GeneralPrinter.print("Sandstorm incoming! Zombies are being carried onto the lawn.");
        }
    }

    private List<String> entryAliases(ZombieWave wave) {
        List<String> aliases = new ArrayList<>();
        if (wave.getWaveZombies() == null) return aliases;
        boolean frostbite = isFrostbiteCaves();
        for (Zombie template : wave.getWaveZombies()) {
            if (template == null) continue;
            if (frostbite && ZombieFactory.shouldSpawnFrosted(template.getAlias())) continue;
            aliases.add(template.getAlias());
        }
        return aliases;
    }

    private void spawnScheduled(ScheduledSpawn spawn) {
        int cols = session.getCols();
        int rows = session.getRows();
        double baseX = entryColumnFor(spawn.alias(), cols);

        List<Integer> laneOrder = laneBag.preferenceOrder(spawn.preferredLane());
        SpawnPlacement.Placement placement = !activeSandStorm
                && ZombieFactory.isStationaryMover(spawn.alias())
                ? SpawnPlacement.resolveInward(session.getZombies(), spawn.alias(), cols,
                laneOrder, baseX)
                : SpawnPlacement.resolve(session.getZombies(), spawn.alias(), cols,
                laneOrder, baseX);
        int lane = Math.max(0, Math.min(rows - 1, placement.lane()));
        double spawnX = placement.x();
        int waveNumber = activePlan == null ? nextWaveIndex : activePlan.getWaveNumber();

        Zombie zombie;
        try {
            zombie = ZombieFactory.create(spawn.alias(), lane, Math.max(0, cols - 1));
        } catch (Exception e) {
            service.Log.error("GameSession",
                    "Failed to spawn zombie " + spawn.alias() + " for wave " + waveNumber, e);
            return;
        }
        zombie.setPosition(new Position(spawnX, lane));

        session.spawnZombie(zombie);
        registerWaveZombie(zombie);

        GeneralPrinter.print("Zombie " + zombie.getName() + " spawned at wave " + waveNumber
                + " in lane " + (lane + 1) + " which cost "
                + ZombieFactory.getZombieCost(zombie.getAlias()) + ".");

        if (activeBeachBigWave) {
            attachBeachBigWaveEntry(zombie, lane, spawnX, cols);
        } else if (activeSandStorm) {
            attachSandStormEntry(zombie, lane, spawnX, rows, cols);
        }
    }

    private double entryColumnFor(String alias, int cols) {
        if (activeSandStorm) return SandStorm.entryX(cols);
        if (ZombieFactory.isStationaryMover(alias)) return Math.max(0, cols - 1);
        return SpawnPlacement.entryX(cols);
    }

    private void attachBeachBigWaveEntry(Zombie zombie, int lane, double spawnX, int cols) {
        Level level = session.getLevel();
        int tideColumn = level == null ? 0 : level.getCurrentTideColumn();
        int targetColumn = Math.max(0, cols - tideColumn);
        double desiredX = Math.max(0.0,
                targetColumn - SessionHazards.BEACH_BIG_WAVE_ENTRY_TARGET_OFFSET);
        double targetX = landings.claim(lane, zombie.getAlias(), desiredX,
                LANDING_MIN_COLUMN, cols - LANDING_MAX_COLUMN_INSET, session.getZombies());
        session.hazards().addBeachBigWaveEntry(zombie, lane, spawnX, targetX);
    }

    private void attachSandStormEntry(Zombie zombie, int lane, double spawnX, int rows, int cols) {
        int targetRow = random.nextDouble() < SANDSTORM_SAME_LANE_CHANCE
                ? lane : SandStorm.randomRow(rows);
        double desiredX = SandStorm.randomLandingColumn(cols) + 0.15;
        double targetX = landings.claim(targetRow, zombie.getAlias(), desiredX,
                LANDING_MIN_COLUMN, cols - LANDING_MAX_COLUMN_INSET, session.getZombies());
        session.hazards().addSandStormEntry(zombie, lane, targetRow, targetX, spawnX,
                SandStorm.arrivalDurationSeconds(), SandStorm.entryDelaySeconds());
    }

    private void registerWaveZombie(Zombie zombie) {
        currentWaveZombies.add(zombie);
        currentWaveStartingHp += zombie.getHp();
    }

    private WaveType waveTypeOf(int waveIndex) {
        if (waveIndex >= 0 && waveIndex < waves.size() && waves.get(waveIndex).isFinalWave()) {
            return WaveType.FINAL;
        }
        return WavePlanner.classify(waveIndex, waves.size());
    }

    private boolean isFrostbiteCaves() {
        Level level = session.getLevel();
        return level != null && level.getSeason() != null
                && "Frostbite Caves".equalsIgnoreCase(level.getSeason().getName());
    }

    boolean allWavesSpawned() {
        return nextWaveIndex >= waves.size() && activePlan == null;
    }

    int getTotalWaveCount() {
        return waves.size();
    }

    int getWavesSpawnedCount() {
        return nextWaveIndex;
    }

    double getSecondsUntilNextWave() {
        if (nextWaveIndex >= waves.size()) return -1;
        return Math.max(0, predictedNextWaveStart() - clockSeconds);
    }

    private double predictedNextWaveStart() {
        if (nextWaveIndex >= waves.size()) return clockSeconds;
        double spawnWindowEndsAt = activePlan != null
                ? planStartedAt + activePlan.getSpawnWindowSeconds()
                : lastSpawnEndedAt;
        return Math.max(lullStartedAt + currentInterval,
                spawnWindowEndsAt + WavePacing.minGapAfterSpawnSeconds());
    }

    private double rawWaveProgress() {
        int total = waves.size();
        if (total <= 0) return 0;
        if (nextWaveIndex >= total) return 1.0;
        double segmentEnd = predictedNextWaveStart();
        double span = segmentEnd - lullStartedAt;
        double fraction = span <= GameClock.TIME_EPSILON
                ? 1.0
                : WavePacing.clamp((clockSeconds - lullStartedAt) / span, 0, 1);
        return WavePacing.clamp((nextWaveIndex + fraction) / total, 0, 1);
    }

    private void advanceWaveProgress() {
        waveProgress = WavePacing.clamp(Math.max(waveProgress, rawWaveProgress()), 0, 1);
    }

    double getWaveProgress() {
        return waveProgress;
    }

    List<Integer> getHugeWaveNumbers() {
        List<Integer> numbers = new ArrayList<>();
        for (int index = 0; index < waves.size(); index++) {
            if (waveTypeOf(index).isHuge()) numbers.add(index + 1);
        }
        return numbers;
    }

    boolean isHugeWaveIncoming() {
        return hugeWaveAlertShown && activePlan == null && nextWaveIndex < waves.size();
    }

    boolean isSpawningWave() {
        return activePlan != null;
    }

    void spawnZombieForCurrentWave(Zombie zombie) {
        if (zombie == null) return;
        session.getZombies().add(zombie);
        registerWaveZombie(zombie);
    }

    void startWaves(double elapsedSeconds) {
        if (wavesStarted) return;
        ZombieFactory.init();
        resetSchedule();
        session.economy().resetSkySunTimer();
        wavesStartedAtSeconds = elapsedSeconds;
        wavesStarted = true;
    }

    boolean isWavesStarted() {
        return wavesStarted;
    }

    boolean areWavesDone() {
        return wavesStarted && allWavesSpawned() && session.getZombies().isEmpty();
    }

    List<ZombieWave> getWaves() {
        return List.copyOf(waves);
    }

    void setWaves(List<ZombieWave> waves) {
        this.waves = waves != null ? waves : new ArrayList<>();
        resetSchedule();
    }

    private void resetSchedule() {
        nextWaveIndex = 0;
        clockSeconds = 0;
        lullStartedAt = 0;
        lastSpawnEndedAt = 0;
        hugeWaveAlertShown = false;
        waveProgress = 0;
        activePlan = null;
        planCursor = 0;
        planStartedAt = 0;
        activeSandStorm = false;
        activeBeachBigWave = false;
        landings.clear();
        laneBag.reset(session.getRows());
        currentWaveZombies = new ArrayList<>();
        currentWaveStartingHp = 0;
        currentInterval = waves.isEmpty() ? 0
                : WavePlanner.intervalSeconds(waves.get(0).getDelay(), 0, waves.size(),
                waveTypeOf(0), session.getDifficultyLevel());
    }

    void resetWavesStarted() {
        wavesStarted = false;
        wavesStartedAtSeconds = 0;
    }

    double getElapsedSecondsSinceWavesStarted(double elapsedSeconds) {
        return wavesStarted ? Math.max(0, elapsedSeconds - wavesStartedAtSeconds) : 0;
    }
}