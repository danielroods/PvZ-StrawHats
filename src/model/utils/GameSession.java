package model.utils;

import model.collections.Item;
import model.collections.item.*;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match.main.levels.Level;
import model.match.main.season.travellog.beach.Flood;
import model.match.main.season.travellog.cave.IceWind;
import model.match_mechanisms.ZombieWave;
import model.match_mechanisms.vector.Position;
import model.pitches.*;
import model.projectile.Projectile;
import model.projectile.zombie_projectile.ZombieProjectile;
import service.GameClock;

import java.util.*;
import java.util.function.ToIntFunction;

public class GameSession {

    public static ToIntFunction<? super Zombie> difficulty = Zombie::getMaxHp;
    private static GameSession instance;
    static final Random ITEM_RANDOM = new Random();

    private final GameClock clock = new GameClock();

    private List<Plant> plants = new ArrayList<>();
    private List<Zombie> zombies = new ArrayList<>();
    private List<Item> items = new ArrayList<>();
    private List<GroundItem> groundItems = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<ZombieProjectile> zombieProjectiles = new ArrayList<>();

    private Level level;

    private Environment environment;
    private LawnMower[] lawnMowers;

    boolean gameOver = false;
    boolean gameWon = false;
    private boolean zombieBreachesEnabled = true;
    private boolean lawnMowersEnabled = true;
    private Boolean skySunEnabledOverride = null;
    private int difficultyLevel;
    /// When true, the player is on the zombies' side (e.g. the I, Zombie mini-game) and
    /// zombie effects that would normally raid the opponent's sun bank (Ra Zombie's
    /// SunThief effect) instead generate sun for the player.
    private boolean zombieSunProductionMode = false;

    private final SessionHazards hazards = new SessionHazards(this);
    private final SessionEconomy economy = new SessionEconomy(this);
    private final SessionBoard board = new SessionBoard(this);
    private final SessionDrops drops = new SessionDrops(this);
    private final SessionReporter reporter = new SessionReporter(this);
    private final WaveScheduler waves = new WaveScheduler(this);
    private final Map<Integer, Double> scorchedTiles = new HashMap<>();
    private final SessionTicker ticker = new SessionTicker(this);

    public GameSession() {
        this(5, 9);
    }

    public GameSession(int rows, int cols) {
        setGridSize(rows, cols);
        instance = this;
    }

    public static GameSession getInstance() {
        if (instance == null) {
            instance = new GameSession();
        }
        return instance;
    }


    public static GameSession peekInstance() {
        return instance;
    }

    SessionHazards hazards() {
        return hazards;
    }

    SessionEconomy economy() {
        return economy;
    }

    SessionBoard board() {
        return board;
    }

    WaveScheduler waves() {
        return waves;
    }

    GameClock clock() {
        return clock;
    }

    public void setGridSize(int rows, int cols) {
        scorchedTiles.clear();
        this.environment = new Environment(rows, cols);
        this.lawnMowers = new LawnMower[rows];
        for (int r = 0; r < rows; r++) {
            lawnMowers[r] = new LawnMower(r);
            lawnMowers[r].setRow(environment.getRowCells(r));
        }
    }

    public void tick() {
        ticker.tick();
    }

    public void damageGrave(Cell cell, int damage) {
        board.damageGrave(cell, damage);
    }

    boolean isSkySunEnabledForSession() {
        if (skySunEnabledOverride != null) return skySunEnabledOverride;
        return level == null || level.isSkySunEnabled();
    }

    public model.match.boss.ZombossFight getZombossFight() {
        return level instanceof model.match.main.levels.special_levels.BossLevel bossLevel
                ? bossLevel.getFight() : null;
    }

    public boolean isCutsceneActive() {
        model.match.boss.ZombossFight fight = getZombossFight();
        return fight != null && fight.isCutscene();
    }

    public boolean isDoubleSunRate() {
        return level instanceof model.match.main.levels.special_levels.BossLevel;
    }

    public boolean isSandStormActive() { return hazards.isSandStormActive(); }

    public double getSandStormRemainingSeconds() { return hazards.getSandStormRemainingSeconds(); }

    public double getSandStormProgress() { return hazards.getSandStormProgress(); }

    public int getSandStormWaveIndex() { return hazards.getSandStormWaveIndex(); }

    public boolean isZombieInSandStorm(Zombie zombie) { return hazards.isZombieInSandStorm(zombie); }

    public double getSandStormAnimationTime(Zombie zombie) { return hazards.getSandStormAnimationTime(zombie); }

    public boolean isBeachBigWaveActive() { return hazards.isBeachBigWaveActive(); }

    public double getBeachBigWaveProgress() { return hazards.getBeachBigWaveProgress(); }

    public boolean isBeachBigWaveCrash() { return hazards.isBeachBigWaveCrash(); }

    public int getBeachBigWaveIndex() { return hazards.getBeachBigWaveIndex(); }

    public boolean isZombieInBeachBigWave(Zombie zombie) { return hazards.isZombieInBeachBigWave(zombie); }

    public double getBeachBigWaveEntryProgress(Zombie zombie) { return hazards.getBeachBigWaveEntryProgress(zombie); }

    public boolean allWavesSpawned() { return waves.allWavesSpawned(); }

    public int getTotalWaveCount() { return waves.getTotalWaveCount(); }

    public int getWavesSpawnedCount() { return waves.getWavesSpawnedCount(); }

    public double getSecondsUntilNextWave() { return waves.getSecondsUntilNextWave(); }

    public double getWaveProgress() { return waves.getWaveProgress(); }

    public boolean isHugeWaveIncoming() { return waves.isHugeWaveIncoming(); }

    public boolean isSpawningWave() { return waves.isSpawningWave(); }

    public void spawnZombie(Zombie zombie) {
        if (zombie == null) return;
        zombies.add(zombie);
    }

    public void spawnZombieForCurrentWave(Zombie zombie) { waves.spawnZombieForCurrentWave(zombie); }

    public void onZombieReachedEnd() {
        gameOver = true;
    }

    public void notifyZombieDied(Zombie zombie, String killerName) { drops.notifyZombieDied(zombie, killerName); }

    public List<GroundItem> collectItemsNear(Position target) { return drops.collectItemsNear(target); }

    public void startWaves() { waves.startWaves(clock.getElapsedSeconds()); }

    public boolean isWavesStarted() { return waves.isWavesStarted(); }

    public boolean areWavesDone() { return waves.areWavesDone(); }

    public boolean freezeZombieInIceBlock(Zombie zombie, int row, int col) {
        return model.match.main.season.travellog.cave.FrostbiteFreezing.freezeZombieInIce(this, zombie, row, col);
    }

    public boolean isFrozenInIceBlock(Zombie zombie) { return board.isFrozenInIceBlock(zombie); }

    public int getSunCount() { return economy.getSunCount(); }

    public void addSun(int amount) { economy.addSun(amount); }

    public int getPlantsLostThisMatch() { return economy.getPlantsLostThisMatch(); }

    public boolean spendSun(int amount) { return economy.spendSun(amount); }

    public int getPlantFoodCount() { return economy.getPlantFoodCount(); }

    public boolean addPlantFood() { return economy.addPlantFood(); }

    public boolean spendPlantFood() { return economy.spendPlantFood(); }

    public boolean grantMatchBoost(int plantId) { return economy.grantMatchBoost(plantId); }

    public boolean hasMatchBoost(int plantId) { return economy.hasMatchBoost(plantId); }

    public Set<Integer> getMatchBoostedPlantIds() { return economy.getMatchBoostedPlantIds(); }

    public void restoreMatchBoosts(Collection<Integer> plantIds) { economy.restoreMatchBoosts(plantIds); }

    public void killAllZombies() {
        zombies.forEach(z -> z.setHp(0));
        zombies.clear();
    }

    public void removeAllCooldowns() { economy.removeAllCooldowns(); }

    public boolean isPlantReady(int plantId) { return economy.isPlantReady(plantId); }

    public double getPlantCooldown(int plantId) { return economy.getPlantCooldown(plantId); }

    public void startPlantCooldown(int plantId, double seconds) { economy.startPlantCooldown(plantId, seconds); }

    public boolean plantAt(int row, int col, Plant plant) { return board.plantAt(row, col, plant); }

    public void scorchTile(int row, int col, double durationSeconds) {
        if (environment == null || row < 0 || row >= environment.getRows()
                || col < 0 || col >= environment.getCols()) return;
        scorchedTiles.put(row * environment.getCols() + col, Math.max(0.0, durationSeconds));
    }

    public boolean isScorchedTile(int row, int col) {
        if (environment == null) return false;
        return scorchedTiles.getOrDefault(row * environment.getCols() + col, 0.0) > 0.0;
    }

    public void tickScorchedTiles(double deltaSeconds) {
        if (scorchedTiles.isEmpty()) return;
        scorchedTiles.replaceAll((key, value) -> Math.max(0.0, value - deltaSeconds));
        scorchedTiles.entrySet().removeIf(e -> e.getValue() <= 0.0);
    }

    public double getScorchedTileRemaining(int row, int col) {
        if (environment == null) return 0.0;
        return scorchedTiles.getOrDefault(row * environment.getCols() + col, 0.0);
    }

    public Plant getPlantAt(int row, int col) { return board.findPlantAt(row, col); }

    public boolean removePlantAt(int row, int col) { return board.removePlantAt(row, col); }

    public Plant digPlantAt(int row, int col) { return board.digPlantAt(row, col); }

    public String renderMap() { return reporter.renderMap(); }

    public String renderPlantsStatus() { return reporter.renderPlantsStatus(); }

    public String renderTileStatus(int row, int col) { return reporter.renderTileStatus(row, col); }

    public String renderZombiesInfo() { return reporter.renderZombiesInfo(); }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isGameWon() {
        return gameWon;
    }

    public int getRows() { return environment.getRows(); }
    public int getCols() { return environment.getCols(); }
    public Environment getEnvironment() { return environment; }

    public List<Plant> getPlants() {
        return plants;
    }

    public void setPlants(List<Plant> plants) {
        this.plants = plants;
    }

    public List<Zombie> getZombies() {
        return zombies;
    }

    public List<ZombieWave> getWaves() {
        return waves.getWaves();
    }

    public void setZombies(List<Zombie> zombies) {
        this.zombies = zombies;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
        if (level != null) {
            setGridSize(level.getRows(), level.getCols());
            plants.clear();
            zombies.clear();
            items.clear();
            groundItems.clear();
            projectiles.clear();
            zombieProjectiles.clear();
            economy.clearCooldownsAndBoosts();
            IceWind.reset(this);
            hazards.reset();
            Projectile.setGlobalSpeedMultiplier(0.60f);
            clock.reset();
            gameOver = false;
            gameWon = false;
            zombieBreachesEnabled = true;
            skySunEnabledOverride = null;
            waves.resetWavesStarted();
            economy.resetMatchStats(level.getInitialSun());
            setWaves(level.getWaves());
            controller.QuestManager.notifyLevelStarted(this);
            level.initSpecial(this);
            if (level.getSeason() != null) {
                level.getSeason().placeSeasonObstacles(this);
                if (level.getSeason().hasTide()) Flood.initialize(level, this);
            }
        }
    }

    public void setWaves(List<ZombieWave> waves) {
        this.waves.setWaves(waves);
    }

    public List<Item> getItems() {
        return items;
    }

    public List<GroundItem> getGroundItems() {
        return groundItems;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public void setGroundItems(List<GroundItem> groundItems) {
        this.groundItems = groundItems;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public void addZombieProjectile(ZombieProjectile projectile) {
        if (projectile != null) zombieProjectiles.add(projectile);
    }

    public List<ZombieProjectile> getZombieProjectiles() {
        return zombieProjectiles;
    }

    public double getElapsedSeconds() {
        return clock.getElapsedSeconds();
    }

    public double getElapsedSecondsSinceWavesStarted() {
        return waves.getElapsedSecondsSinceWavesStarted(clock.getElapsedSeconds());
    }

    public boolean hasUsedPlantFamily(String family) { return economy.hasUsedPlantFamily(family); }

    public boolean usedOnlyNightPlants() { return economy.usedOnlyNightPlants(); }

    public boolean isLawnMowerUsed(int row) {
        return row >= 0 && row < lawnMowers.length && lawnMowers[row].isUsed();
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public void setZombieBreachesEnabled(boolean zombieBreachesEnabled) {
        this.zombieBreachesEnabled = zombieBreachesEnabled;
    }

    public boolean isZombieBreachesEnabled() {
        return zombieBreachesEnabled;
    }

    public void setLawnMowersEnabled(boolean lawnMowersEnabled) {
        this.lawnMowersEnabled = lawnMowersEnabled;
    }

    public boolean areLawnMowersEnabled() {
        return lawnMowersEnabled;
    }

    public void setSkySunEnabled(boolean enabled) {
        this.skySunEnabledOverride = enabled;
    }

    public boolean isSkySunEnabled() {
        return isSkySunEnabledForSession();
    }

    public void setZombieSunProductionMode(boolean zombieSunProductionMode) {
        this.zombieSunProductionMode = zombieSunProductionMode;
    }

    public boolean isZombieSunProductionMode() {
        return zombieSunProductionMode;
    }


    public List<PushableStructure> getPushableStructures() { return board.getPushableStructures(); }

    public void registerStructure(PushableStructure structure) { board.registerStructure(structure); }

    public Environment getLawn() {
        return environment;
    }

    public LawnMower[] getLawnMowers() {
        return lawnMowers;
    }
}