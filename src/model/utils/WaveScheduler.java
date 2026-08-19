package model.utils;

import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match.main.season.travellog.beach.Beach;
import model.match.main.season.travellog.beach.Flood;
import model.match.main.season.travellog.egypt.Egypt;
import model.match.main.season.travellog.egypt.SandStorm;
import model.match.main.levels.Level;
import model.match_mechanisms.ZombieWave;
import model.match_mechanisms.vector.Position;
import service.GameClock;
import view.GeneralPrinter;

import java.util.ArrayList;
import java.util.List;

/**
 * Wave bookkeeping for a match: the wave list, when the next one is due, and the actual
 * spawning of a wave's zombies (including the Egypt sandstorm / Big Wave Beach entries
 * that ride in with it).
 */
class WaveScheduler {

    private static final double HUGE_WAVE_ALERT_LEAD_SECONDS = 5.0;
    private static final double NORMAL_ENTRY_EXTRA_COLUMNS = 4.5;

    private final GameSession session;

    private List<ZombieWave> waves = new ArrayList<>();
    private int nextWaveIndex = 0;
    private double waveTimer = 0;
    private boolean wavesStarted = false;
    private double wavesStartedAtSeconds = 0;

    private List<Zombie> currentWaveZombies = new ArrayList<>();
    private int currentWaveStartingHp = 0;
    private boolean hugeWaveAlertShown = false;

    WaveScheduler(GameSession session) {
        this.session = session;
    }

    void tickWaveScheduler(double deltaTimeSeconds) {
        if (nextWaveIndex >= waves.size()) return;

        waveTimer += deltaTimeSeconds;
        ZombieWave nextWave = waves.get(nextWaveIndex);

        if (nextWave.isFinalWave() && !hugeWaveAlertShown
                && GameClock.hasReached(waveTimer,
                Math.max(0, nextWave.getDelay() - HUGE_WAVE_ALERT_LEAD_SECONDS))) {
            hugeWaveAlertShown = true;
            GeneralPrinter.print("A huge wave of zombies is approaching!");
        }

        if (!GameClock.hasReached(waveTimer, nextWave.getDelay())) return;
        if (!previousWaveMostlyCleared()) return;

        spawnWave(nextWave);
        nextWaveIndex++;
        waveTimer = 0;
    }

    private boolean previousWaveMostlyCleared() {
        if (currentWaveZombies.isEmpty()) return true;
        int remainingHp = currentWaveZombies.stream()
                .filter(Zombie::isAlive)
                .mapToInt(Zombie::getHp)
                .sum();
        return remainingHp <= currentWaveStartingHp * 0.25;
    }

    private void spawnWave(ZombieWave wave) {
        if (wave.getWaveZombies() == null) return;

        Level level = session.getLevel();
        SessionHazards hazards = session.hazards();

        int waveNumber = nextWaveIndex + 1;
        if (wave.isFinalWave()) {
            GeneralPrinter.print("The final wave has come.");
        } else {
            GeneralPrinter.print("Wave " + waveNumber + " started.");
        }
        GeneralPrinter.print("Wave difficulty: " + wave.getWaveCost() + ".");

        currentWaveZombies = new ArrayList<>();
        int totalHp = 0;
        currentWaveStartingHp = 0;

        if (level != null && level.getSeason() != null) {
            try {
                level.getSeason().onWaveStart(session, nextWaveIndex);
            } catch (Exception e) {
                com.badlogic.gdx.Gdx.app.error("GameSession", "Season.onWaveStart() failed for wave " + waveNumber, e);
            }
        }
        totalHp += currentWaveStartingHp;

        boolean isEgyptLevel = level != null && level.getSeason() instanceof Egypt;
        boolean isBeachLevel = level != null && level.getSeason() instanceof Beach;
        boolean isSandstormWave = isEgyptLevel && SandStorm.shouldTrigger(wave, nextWaveIndex);
        boolean isBeachBigWave = isBeachLevel && ((Beach) level.getSeason()).isBigWave(wave);
        if (isBeachBigWave) {
            hazards.beginBeachBigWave(nextWaveIndex);
            Flood.applyBigWaveWash(level, session);
            GeneralPrinter.print("A huge wave is rushing across the beach!");
        }
        if (isSandstormWave) {
            hazards.beginSandStorm(nextWaveIndex);
            GeneralPrinter.print("Sandstorm incoming! Zombies are being carried onto the lawn.");
        }

        for (Zombie template : wave.getWaveZombies()) {
            if (isFrostbiteCaves() && ZombieFactory.shouldSpawnFrosted(template.getAlias())) {
                // Frosted zombies are pre-placed, already trapped in ice, on the map at match
                // start (see Cave.placeSeasonObstacles) - they no longer ride in with a wave.
                continue;
            }
            try {
                int lane;
                double spawnX;
                if (isSandstormWave) {
                    lane = SandStorm.randomRow(session.getRows());
                } else {
                    lane = GameSession.ITEM_RANDOM.nextInt(session.getRows());
                }
                spawnX = session.getCols() - 1 + NORMAL_ENTRY_EXTRA_COLUMNS;

                Zombie zombie = ZombieFactory.create(template.getAlias(), lane, Math.max(0, session.getCols() - 1));
                zombie.setPosition(new Position(spawnX, lane));

                int cost = ZombieFactory.getZombieCost(zombie.getAlias());
                GeneralPrinter.print("Zombie " + zombie.getName() + " spawned at wave " + waveNumber
                        + " in lane " + (lane + 1) + " which cost " + cost + ".");

                session.spawnZombie(zombie);
                currentWaveZombies.add(zombie);
                totalHp += zombie.getHp();

                if (isBeachBigWave) {
                    int targetColumn = Math.max(0, session.getCols() - level.getCurrentTideColumn());
                    double targetX = Math.max(0.0, targetColumn - SessionHazards.BEACH_BIG_WAVE_ENTRY_TARGET_OFFSET);
                    hazards.addBeachBigWaveEntry(zombie, lane, spawnX, targetX);
                }

                if (isSandstormWave) {
                    int targetRow = SandStorm.randomRow(session.getRows());
                    int targetColumn = SandStorm.randomLandingColumn(session.getCols());
                    if (Math.random() < 0.65) targetRow = lane;
                    hazards.addSandStormEntry(zombie,
                            lane,
                            targetRow,
                            targetColumn,
                            spawnX,
                            SandStorm.arrivalDurationSeconds(),
                            SandStorm.entryDelaySeconds());
                }
            } catch (Exception e) {
                com.badlogic.gdx.Gdx.app.error("GameSession",
                        "Failed to spawn zombie \"" + template.getAlias() + "\" for wave " + waveNumber, e);
            }
        }

        currentWaveStartingHp = totalHp;
    }

    private boolean isFrostbiteCaves() {
        Level level = session.getLevel();
        return level != null && level.getSeason() != null
                && "Frostbite Caves".equalsIgnoreCase(level.getSeason().getName());
    }

    boolean allWavesSpawned() {
        return nextWaveIndex >= waves.size();
    }

    int getTotalWaveCount() {
        return waves.size();
    }

    int getWavesSpawnedCount() {
        return nextWaveIndex;
    }

    double getSecondsUntilNextWave() {
        if (allWavesSpawned()) return -1;
        return Math.max(0, waves.get(nextWaveIndex).getDelay() - waveTimer);
    }

    void spawnZombieForCurrentWave(Zombie zombie) {
        if (zombie == null) return;
        session.getZombies().add(zombie);
        currentWaveZombies.add(zombie);
        currentWaveStartingHp += zombie.getHp();
    }

    void startWaves(double elapsedSeconds) {
        if (wavesStarted) return;
        ZombieFactory.init();
        nextWaveIndex = 0;
        waveTimer = 0;
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
        this.nextWaveIndex = 0;
        this.waveTimer = 0;
        this.currentWaveZombies = new ArrayList<>();
        this.currentWaveStartingHp = 0;
        this.hugeWaveAlertShown = false;
    }

    void resetWavesStarted() {
        wavesStarted = false;
        wavesStartedAtSeconds = 0;
    }

    double getElapsedSecondsSinceWavesStarted(double elapsedSeconds) {
        return wavesStarted ? Math.max(0, elapsedSeconds - wavesStartedAtSeconds) : 0;
    }
}
