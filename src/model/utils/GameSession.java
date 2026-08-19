package model.utils;

import controller.QuestManager;
import model.collections.Item;
import model.collections.armour.ArmourFactory;
import model.collections.armour.ArmourType;
import model.collections.armour.PlantArmour;
import model.collections.item.*;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match.main.levels.Level;
import model.match.main.season.travellog.beach.Beach;
import model.match.main.season.travellog.beach.Flood;
import model.match.main.season.travellog.egypt.Egypt;
import model.match.main.season.travellog.egypt.SandStorm;
import model.match.main.season.travellog.cave.IceWind;
import model.pitches.obstacles.IceBlock;
import model.match_mechanisms.ZombieWave;
import model.match_mechanisms.vector.Position;
import model.pitches.*;
import model.pitches.obstacles.Grave;
import model.projectile.Projectile;
import model.projectile.zombie_projectile.ZombieProjectile;
import model.user_data.User;
import model.user_data.UserState;
import service.GameClock;
import view.GeneralPrinter;

import java.util.*;
import java.util.function.ToIntFunction;

public class GameSession {

    public static ToIntFunction<? super Zombie> difficulty = Zombie::getMaxHp;
    private static GameSession instance;
    private static final Random ITEM_RANDOM = new Random();
    private static final double MIN_SKY_SUN_INTERVAL = 12.0;
    private static final double SKY_SUN_INTERVAL_START = 6.0;
    private static final double SKY_SUN_INTERVAL_GROWTH = 0.05;

    private final GameClock clock = new GameClock();

    private List<Plant> plants = new ArrayList<>();
    private List<Zombie> zombies = new ArrayList<>();
    private List<Item> items = new ArrayList<>();
    private List<GroundItem> groundItems = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<ZombieProjectile> zombieProjectiles = new ArrayList<>();

    private Level level;
    private List<ZombieWave> waves = new ArrayList<>();
    private int nextWaveIndex = 0;
    private double waveTimer = 0;
    private boolean wavesStarted = false;
    private double wavesStartedAtSeconds = 0;

    private List<Zombie> currentWaveZombies = new ArrayList<>();
    private int currentWaveStartingHp = 0;
    private static final double HUGE_WAVE_ALERT_LEAD_SECONDS = 5.0;
    private boolean hugeWaveAlertShown = false;

    private int sunCount;
    private int plantFoodCount;

    private Environment environment;
    private LawnMower[] lawnMowers;

    private boolean gameOver = false;
    private boolean gameWon = false;
    private boolean zombieBreachesEnabled = true;
    private Boolean skySunEnabledOverride = null;
    private int difficultyLevel;
    private int plantsLostThisMatch = 0;
    private final Set<String> plantFamiliesUsedThisMatch = new HashSet<>();
    private boolean plantedAnyPlantThisMatch = false;
    private boolean usedNonNightPlantThisMatch = false;
    private final Map<Integer, Double> plantCooldowns = new HashMap<>();
    private final Set<Integer> matchBoostedPlantIds = new HashSet<>();

    private double skySunTimer = 0;

    private double sandStormTimer = 0.0;
    private boolean sandStormActive = false;
    private int sandStormWaveIndex = -1;
    private final Map<Zombie, SandStormEntry> sandStormEntries = new IdentityHashMap<>();

    private static final double BEACH_BIG_WAVE_DURATION_SECONDS = 1.35;
    private static final double BEACH_BIG_WAVE_ENTRY_DURATION_SECONDS = 1.05;
    private static final double BEACH_BIG_WAVE_ENTRY_TARGET_OFFSET = 0.20;
    private boolean beachBigWaveActive = false;
    private double beachBigWaveTimer = 0.0;
    private int beachBigWaveIndex = -1;
    private final Map<Zombie, BeachBigWaveEntry> beachBigWaveEntries = new IdentityHashMap<>();

    private static final double NORMAL_ENTRY_EXTRA_COLUMNS = 4.5;

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
        final int targetColumn;
        final double startX;
        final double duration;
        final double startDelay;
        double elapsed;
        double animationElapsed;

        SandStormEntry(int startRow, int targetRow, int targetColumn, double startX, double duration, double startDelay) {
            this.startRow = startRow;
            this.targetRow = targetRow;
            this.targetColumn = targetColumn;
            this.startX = startX;
            this.duration = duration;
            this.startDelay = startDelay;
        }
    }

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

    public void setGridSize(int rows, int cols) {
        this.environment = new Environment(rows, cols);
        this.lawnMowers = new LawnMower[rows];
        for (int r = 0; r < rows; r++) {
            lawnMowers[r] = new LawnMower(r);
            lawnMowers[r].setRow(environment.getRowCells(r));
        }
    }

    public void tick() {
        if (gameOver || gameWon) return;

        clock.tick();
        double deltaTimeSeconds = GameClock.SECONDS_PER_TICK;

        plantCooldowns.replaceAll((plantId, remaining) -> GameClock.countDown(remaining, deltaTimeSeconds));
        plantCooldowns.entrySet().removeIf(entry -> GameClock.isZero(entry.getValue()));

        if (level != null) {
            level.updateTide(deltaTimeSeconds, this);
            if (level.getSeason() != null) {
                level.getSeason().applyPerTickEffect(this, deltaTimeSeconds);
            }
        }

        updateSandStorm(deltaTimeSeconds);
        IceWind.tick(this, deltaTimeSeconds);
        updateBeachBigWave(deltaTimeSeconds);

        for (int i = plants.size() - 1; i >= 0; i--) {
            Plant plant = plants.get(i);
            if (plant.isAlive()) plant.tick(deltaTimeSeconds, this);
        }
        for (int i = zombies.size() - 1; i >= 0; i--) {
            Zombie zombie = zombies.get(i);
            if (zombie.isAlive() && !sandStormEntries.containsKey(zombie)
                    && !beachBigWaveEntries.containsKey(zombie)
                    && !isFrozenInIceBlock(zombie)) {
                zombie.tick(deltaTimeSeconds, this);
            }
        }
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            if (projectile.isAlive()) {
                projectile.tick();
            }
        }
        for (int i = zombieProjectiles.size() - 1; i >= 0; i--) {
            ZombieProjectile zombieProjectile = zombieProjectiles.get(i);
            if (zombieProjectile.isAlive()) zombieProjectile.tick();
        }
        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            if (item.isAlive()) item.tick();
        }
        for (int i = 0; i < lawnMowers.length; i++) {
            if (lawnMowers[i] != null) {
                lawnMowers[i].tick(deltaTimeSeconds, this);
            }
        }

        if (wavesStarted) tickWaveScheduler(deltaTimeSeconds);

        if (wavesStarted && isSkySunEnabledForSession()) {
            skySunTimer += deltaTimeSeconds;
            if (GameClock.hasReached(skySunTimer, getEffectiveSkySunInterval())) {
                skySunTimer = 0;
                int col = ITEM_RANDOM.nextInt(environment.getCols());
                int row = ITEM_RANDOM.nextInt(environment.getRows());
                GroundSun sun = GroundSun.fallFromSky(new Position(col, row));
                items.add(sun);
                view.GeneralPrinter.print("New " + sun.getDropType().name().toLowerCase()
                        + " sun is dropping at position (" + (col + 1) + ", " + (row + 1) + ").");
            }
        }

        for (int i = zombies.size() - 1; i >= 0; i--) {
            Zombie zombie = zombies.get(i);
            if (!zombie.isAlive() && zombie.isPlantFoodPending()) {
                items.add(new GroundPlantFood(zombie.getPosition()));
                zombie.clearPlantFoodPending();
            }
        }

        sandStormEntries.entrySet().removeIf(entry -> !entry.getKey().isAlive() || !zombies.contains(entry.getKey()));
        clearDeadPlantsFromGrid();
        clearDeadStructuresFromGrid();
        refreshZombieOccupancy();

        recordLevelSpecificDeaths();
        tickLevelSpecificLogic(deltaTimeSeconds);

        for (Plant plant : plants) {
            if (!plant.isAlive()) plantsLostThisMatch++;
        }
        plants.removeIf(p -> !p.isAlive());
        zombies.removeIf(z -> !z.isAlive());
        items.removeIf(i -> !i.isAlive());
        projectiles.removeIf(p -> !p.isAlive());
        zombieProjectiles.removeIf(p -> !p.isAlive());

        if (zombieBreachesEnabled) checkZombieBreaches();

        if (level != null && level.checkLossCondition(this)) {
            gameOver = true;
        }

        boolean levelWon = level != null
                ? level.checkWinCondition(this)
                : wavesStarted && allWavesSpawned() && zombies.isEmpty();
        if (levelWon && !gameOver) {
            if (!gameWon) {
                gameWon = true;
                controller.QuestManager.notifyLevelWon(this);
            }
        }
    }
    public void damageGrave(Cell cell, int damage) {
        if (cell == null || !(cell.getObstacle() instanceof Grave grave)) return;
        if (!grave.takeDamage(damage)) return;

        int row = cell.getRow();
        int col = cell.getCol();
        destroyGrave(row, col, grave);
    }

    private void destroyGrave(int row, int col, Grave grave) {
        Cell cell = environment.getCell(row, col);
        if (cell == null || cell.getObstacle() != grave) return;
        cell.setObstacle(null);
        Position position = new Position(col, row);
        if (grave.getReward() == Grave.Reward.SUN) {
            items.add(new GroundSun(position, 50));
        } else if (grave.getReward() == Grave.Reward.PLANT_FOOD) {
            items.add(new GroundPlantFood(position));
        }
    }

    private double getEffectiveSkySunInterval() {
        double elapsed = getElapsedSecondsSinceWavesStarted();
        double baseInterval = Math.max(SKY_SUN_INTERVAL_START
                + SKY_SUN_INTERVAL_GROWTH * elapsed, MIN_SKY_SUN_INTERVAL);
        if (difficultyLevel <= 0) return baseInterval;
        return baseInterval * (difficultyLevel / 3.0);
    }

    private boolean isSkySunEnabledForSession() {
        if (skySunEnabledOverride != null) return skySunEnabledOverride;
        return level == null || level.isSkySunEnabled();
    }

    private void recordLevelSpecificDeaths() {
        if (level instanceof model.match.main.levels.special_levels.LoveYourPlantsLevel loveLevel) {
            plants.stream().filter(p -> !p.isAlive()).forEach(p -> loveLevel.recordPlantLoss());
        }
        if (level instanceof model.match.main.levels.special_levels.TimedWarLevel timedWarLevel) {
            zombies.stream().filter(z -> !z.isAlive()).forEach(z -> timedWarLevel.recordZombieKill());
            timedWarLevel.tickTimer(GameClock.SECONDS_PER_TICK);
        }
    }

    private void tickLevelSpecificLogic(double deltaTimeSeconds) {
        if (level instanceof model.match.main.levels.special_levels.ConveyorBeltLevel conveyorLevel) {
            conveyorLevel.tickConveyor(deltaTimeSeconds);
        }
    }

    private void clearDeadPlantsFromGrid() {
        for (int r = 0; r < environment.getRows(); r++) {
            for (int c = 0; c < environment.getCols(); c++) {
                Cell cell = environment.getCell(r, c);
                if (cell.getPlant() != null && !cell.getPlant().isAlive()) {
                    Plant bottom = cell.getPlant().getBottom();
                    cell.setPlant(bottom != null && bottom.isAlive() ? bottom : null);
                }
            }
        }
    }

    private void clearDeadStructuresFromGrid() {
        for (int r = 0; r < environment.getRows(); r++) {
            for (int c = 0; c < environment.getCols(); c++) {
                Cell cell = environment.getCell(r, c);
                if (cell.getStructure() != null && !cell.getStructure().isAlive()) cell.setStructure(null);
            }
        }
    }

    private void refreshZombieOccupancy() {
        for (int r = 0; r < environment.getRows(); r++) {
            for (int c = 0; c < environment.getCols(); c++) {
                environment.getCell(r, c).clearZombies();
            }
        }
        for (Zombie zombie : zombies) {
            if (!zombie.isAlive() || zombie.getPosition() == null) continue;
            int col = (int) Math.round(zombie.getPosition().x());
            int row = (int) Math.round(zombie.getPosition().y());
            if (row < 0 || row >= environment.getRows() || col < 0 || col >= environment.getCols()) continue;
            Cell cell = environment.getCell(row, col);
            if (cell != null) cell.addZombie(zombie);
        }
    }

    private void tickWaveScheduler(double deltaTimeSeconds) {
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
                level.getSeason().onWaveStart(this, nextWaveIndex);
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
            beginBeachBigWave(nextWaveIndex);
            Flood.applyBigWaveWash(level, this);
            GeneralPrinter.print("A huge wave is rushing across the beach!");
        }
        if (isSandstormWave) {
            beginSandStorm(nextWaveIndex);
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
                    lane = SandStorm.randomRow(getRows());
                } else {
                    lane = ITEM_RANDOM.nextInt(getRows());
                }
                spawnX = getCols() - 1 + NORMAL_ENTRY_EXTRA_COLUMNS;

                Zombie zombie = ZombieFactory.create(template.getAlias(), lane, Math.max(0, getCols() - 1));
                zombie.setPosition(new Position(spawnX, lane));

                int cost = ZombieFactory.getZombieCost(zombie.getAlias());
                GeneralPrinter.print("Zombie " + zombie.getName() + " spawned at wave " + waveNumber
                        + " in lane " + (lane + 1) + " which cost " + cost + ".");

                spawnZombie(zombie);
                currentWaveZombies.add(zombie);
                totalHp += zombie.getHp();

                if (isBeachBigWave) {
                    int targetColumn = Math.max(0, getCols() - level.getCurrentTideColumn());
                    double targetX = Math.max(0.0, targetColumn - BEACH_BIG_WAVE_ENTRY_TARGET_OFFSET);
                    beachBigWaveEntries.put(zombie, new BeachBigWaveEntry(lane, spawnX, targetX));
                }

                if (isSandstormWave) {
                    int targetRow = SandStorm.randomRow(getRows());
                    int targetColumn = SandStorm.randomLandingColumn(getCols());
                    if (Math.random() < 0.65) targetRow = lane;
                    sandStormEntries.put(zombie, new SandStormEntry(
                            lane,
                            targetRow,
                            targetColumn,
                            spawnX,
                            SandStorm.arrivalDurationSeconds(),
                            SandStorm.entryDelaySeconds()));
                }
            } catch (Exception e) {
                com.badlogic.gdx.Gdx.app.error("GameSession",
                        "Failed to spawn zombie \"" + template.getAlias() + "\" for wave " + waveNumber, e);
            }
        }

        currentWaveStartingHp = totalHp;
    }

    private void beginSandStorm(int waveIndex) {
        sandStormActive = true;
        sandStormTimer = SandStorm.EVENT_DURATION_SECONDS;
        sandStormWaveIndex = waveIndex;
    }

    private void updateSandStorm(double deltaTimeSeconds) {
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
                double targetX = stormEntry.targetColumn + 0.15;
                double x = stormEntry.startX + (targetX - stormEntry.startX) * eased;
                double y = stormEntry.startRow + (stormEntry.targetRow - stormEntry.startRow) * eased;
                zombie.setPosition(new Position(x, y));

                if (progress >= 1.0) {
                    zombie.setPosition(new Position(targetX, stormEntry.targetRow));
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

    public boolean isSandStormActive() {
        return sandStormActive;
    }

    public double getSandStormRemainingSeconds() {
        return sandStormTimer;
    }

    public double getSandStormProgress() {
        if (!sandStormActive) return 0.0;
        return 1.0 - Math.max(0.0, Math.min(1.0, sandStormTimer / SandStorm.EVENT_DURATION_SECONDS));
    }

    public int getSandStormWaveIndex() {
        return sandStormWaveIndex;
    }

    public boolean isZombieInSandStorm(Zombie zombie) {
        return zombie != null && sandStormEntries.containsKey(zombie);
    }

    public double getSandStormAnimationTime(Zombie zombie) {
        SandStormEntry entry = zombie == null ? null : sandStormEntries.get(zombie);
        return entry == null ? -1.0 : entry.animationElapsed;
    }

    private void beginBeachBigWave(int waveIndex) {
        beachBigWaveActive = true;
        beachBigWaveTimer = BEACH_BIG_WAVE_DURATION_SECONDS;
        beachBigWaveIndex = waveIndex;
    }

    private void updateBeachBigWave(double deltaTimeSeconds) {
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
                    zombie.setPosition(new Position(waveEntry.targetX, waveEntry.row));
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

    public boolean isBeachBigWaveActive() {
        return beachBigWaveActive;
    }

    public double getBeachBigWaveProgress() {
        if (!beachBigWaveActive) return 0.0;
        return 1.0 - Math.max(0.0, Math.min(1.0, beachBigWaveTimer / BEACH_BIG_WAVE_DURATION_SECONDS));
    }

    public boolean isBeachBigWaveCrash() {
        return beachBigWaveActive && getBeachBigWaveProgress() >= 0.72;
    }

    public int getBeachBigWaveIndex() {
        return beachBigWaveIndex;
    }

    public boolean isZombieInBeachBigWave(Zombie zombie) {
        return zombie != null && beachBigWaveEntries.containsKey(zombie);
    }

    public double getBeachBigWaveEntryProgress(Zombie zombie) {
        BeachBigWaveEntry entry = zombie == null ? null : beachBigWaveEntries.get(zombie);
        if (entry == null) return -1.0;
        return Math.min(1.0, entry.elapsed / BEACH_BIG_WAVE_ENTRY_DURATION_SECONDS);
    }

    public boolean allWavesSpawned() {
        return nextWaveIndex >= waves.size();
    }

    public int getTotalWaveCount() {
        return waves.size();
    }

    public int getWavesSpawnedCount() {
        return nextWaveIndex;
    }

    public double getSecondsUntilNextWave() {
        if (allWavesSpawned()) return -1;
        return Math.max(0, waves.get(nextWaveIndex).getDelay() - waveTimer);
    }

    private void checkZombieBreaches() {
        for (Zombie zombie : zombies) {
            if (!zombie.isAlive() || zombie.getPosition() == null) continue;

            if (zombie.getPosition().x() < 0.0) {
                int row = (int) Math.round(zombie.getPosition().y());
                if (row < 0 || row >= lawnMowers.length) continue;

                LawnMower mower = lawnMowers[row];
                if (mower.getState() == LawnMower.MowerState.DEAD) {
                    if (zombie.getPosition().x() < -0.8) {
                        onZombieReachedEnd();
                        return;
                    }
                } else if (mower.getState() == LawnMower.MowerState.IDLE) {
                    boolean survived = mower.killZombiesInRow(zombiesInRow(row));
                    if (!survived) {
                        onZombieReachedEnd();
                        return;
                    }
                } else {
                    if (zombie.getPosition().x() < -2.0) {
                        onZombieReachedEnd();
                        return;
                    }
                }
            }
        }
    }

    private List<Zombie> zombiesInRow(int row) {
        List<Zombie> result = new ArrayList<>();
        for (Zombie zombie : zombies) {
            if (zombie.isAlive() && zombie.getPosition() != null
                    && (int) Math.round(zombie.getPosition().y()) == row) {
                result.add(zombie);
            }
        }
        return result;
    }

    private boolean isFrostbiteCaves() {
        return level != null && level.getSeason() != null
                && "Frostbite Caves".equalsIgnoreCase(level.getSeason().getName());
    }

    public void spawnZombie(Zombie zombie) {
        if (zombie == null) return;
        zombies.add(zombie);
    }

    public void spawnZombieForCurrentWave(Zombie zombie) {
        if (zombie == null) return;
        zombies.add(zombie);
        currentWaveZombies.add(zombie);
        currentWaveStartingHp += zombie.getHp();
    }

    public void onZombieReachedEnd() {
        gameOver = true;
    }

    public void notifyZombieDied(Zombie zombie, String killerName) {
        if (zombie == null) return;
        Position dropPosition = zombie.getPosition();
        if (dropPosition == null) return;

        int displayX = (int) Math.round(dropPosition.x()) + 1;
        int displayY = (int) Math.round(dropPosition.y()) + 1;
        GeneralPrinter.print("Zombie of type " + zombie.getName() + " is dead at ("
                + displayX + ", " + displayY + ").");

        GroundCoin coin = new GroundCoin(dropPosition, GroundCoin.CoinTier.rollRandom());
        items.add(coin);
        GeneralPrinter.print("A " + coin.getTier().name().toLowerCase()
                + " coin dropped at (" + displayX + ", " + displayY + ").");

        if (ITEM_RANDOM.nextInt(100) < 10) {
            items.add(new GroundDiamond(dropPosition, 1));
            GeneralPrinter.print("A diamond dropped at (" + displayX + ", " + displayY + ").");
        }

        if (User.currentUser != null) {
            UserState state = User.currentUser.userState;
            if (!state.unlockedPlantIds.isEmpty() && ITEM_RANDOM.nextInt(100) < 5) {
                List<Integer> unlocked = new ArrayList<>(state.unlockedPlantIds);
                int plantId = unlocked.get(ITEM_RANDOM.nextInt(unlocked.size()));
                items.add(new GroundSeedPack(dropPosition, plantId, 1));
                GeneralPrinter.print("A seed pack dropped at (" + displayX + ", " + displayY + ").");
            }
        }

        QuestManager.notifyZombieKilled(this, zombie, killerName);
    }

    public List<GroundItem> collectItemsNear(Position target) {
        List<GroundItem> collectedItems = new ArrayList<>();
        if (User.currentUser == null || target == null) return collectedItems;

        UserState state = User.currentUser.userState;
        for (Item item : new ArrayList<>(items)) {
            if (item instanceof GroundItem groundItem
                    && groundItem.isAlive()
                    && !groundItem.isCollected()
                    // All ground items, including every type of sky sun,
                    // are collectible by an explicit player click.
                    && groundItem.isNear(target)) {
                groundItem.collect(this, state);
                collectedItems.add(groundItem);
                announceCollection(groundItem, state);
            }
        }
        return collectedItems;
    }

    private void announceCollection(GroundItem item, UserState state) {
        switch (item.getItemType()) {
            case SUN -> GeneralPrinter.print("You collected a sun; you have " + getSunCount() + " sun now.");
            case PLANT_FOOD -> GeneralPrinter.print("The glowing zombie dropped a plant food; you have " + getPlantFoodCount() + " plant foods now.");
            case COIN -> GeneralPrinter.print("A zombie dropped a coin; you have " + state.coins + " coins now.");
            case DIAMOND -> GeneralPrinter.print("A zombie dropped a diamond; you have " + state.diamonds + " diamonds now.");
            case SEED_PACK -> {
                int totalPots = state.seedPacketInventory.values().stream().mapToInt(Integer::intValue).sum();
                GeneralPrinter.print("A zombie dropped a pot; you have " + totalPots + " pots now.");
            }
            default -> {
            }
        }
    }

    public void startWaves() {
        if (wavesStarted) return;
        ZombieFactory.init();
        nextWaveIndex = 0;
        waveTimer = 0;
        skySunTimer = 0;
        wavesStartedAtSeconds = clock.getElapsedSeconds();
        wavesStarted = true;
    }

    public boolean isWavesStarted() {
        return wavesStarted;
    }

    public boolean areWavesDone() {
        return wavesStarted && allWavesSpawned() && zombies.isEmpty();
    }

    public boolean freezeZombieInIceBlock(Zombie zombie, int row, int col) {
        return model.match.main.season.travellog.cave.FrostbiteFreezing.freezeZombieInIce(this, zombie, row, col);
    }

    public boolean isFrozenInIceBlock(Zombie zombie) {
        if (zombie == null || environment == null || zombie.getPosition() == null) return false;
        int row = (int) Math.round(zombie.getPosition().y());
        int col = (int) Math.round(zombie.getPosition().x());
        Cell cell = environment.getCell(row, col);
        return cell != null && cell.getObstacle() instanceof IceBlock iceBlock
                && iceBlock.getFrozenZombie() == zombie;
    }

    public int getSunCount() {
        return sunCount;
    }

    public void addSun(int amount) {
        sunCount += amount;
        if (amount > 0) {
            QuestManager.updateProgress("COLLECT_SUN", amount, Collections.emptyMap());
        }
    }

    public int getPlantsLostThisMatch() {
        return plantsLostThisMatch;
    }

    public boolean spendSun(int amount) {
        if (sunCount < amount) return false;
        sunCount -= amount;
        return true;
    }

    private static final int MAX_PLANT_FOOD = 3;

    public int getPlantFoodCount() {
        return plantFoodCount;
    }

    public boolean addPlantFood() {
        if (plantFoodCount >= MAX_PLANT_FOOD) return false;
        plantFoodCount++;
        return true;
    }

    public boolean spendPlantFood() {
        if (plantFoodCount <= 0) return false;
        plantFoodCount--;
        return true;
    }

    public boolean grantMatchBoost(int plantId) {
        return matchBoostedPlantIds.add(plantId);
    }

    public boolean hasMatchBoost(int plantId) {
        return matchBoostedPlantIds.contains(plantId);
    }

    public Set<Integer> getMatchBoostedPlantIds() {
        return Set.copyOf(matchBoostedPlantIds);
    }

    public void restoreMatchBoosts(Collection<Integer> plantIds) {
        matchBoostedPlantIds.clear();
        if (plantIds != null) matchBoostedPlantIds.addAll(plantIds);
    }

    public void killAllZombies() {
        zombies.forEach(z -> z.setHp(0));
        zombies.clear();
    }

    public void removeAllCooldowns() {
        plants.forEach(p -> p.setInternalTimer(0));
        plantCooldowns.clear();
    }

    public boolean isPlantReady(int plantId) {
        return GameClock.isZero(plantCooldowns.getOrDefault(plantId, 0.0));
    }

    public double getPlantCooldown(int plantId) {
        return plantCooldowns.getOrDefault(plantId, 0.0);
    }

    public void startPlantCooldown(int plantId, double seconds) {
        if (seconds > 0) plantCooldowns.put(plantId, seconds);
    }

    public boolean plantAt(int row, int col, Plant plant) {
        Cell cell = environment.getCell(row, col);
        if (cell == null || plant == null) return false;

        boolean handlesIceBlock = plant.getName().equalsIgnoreCase("Hot Potato")
                && cell.getObstacle() instanceof IceBlock;
        boolean handlesGrave = plant.getName().equalsIgnoreCase("Grave Buster")
                && cell.getObstacle() instanceof Grave;
        boolean handlesObstacle = handlesIceBlock || handlesGrave;
        if (cell.getObstacle() != null && !handlesObstacle) return false;

        if (cell.getTile() != null && cell.getTile().type() == TileType.Slippery) return false;

        boolean flooded = cell.getTile() != null && cell.getTile().type() == TileType.Water;
        Plant existing = cell.hasPlant() ? cell.getPlant() : null;
        boolean lilySupport = existing != null && existing.getTags().contains(PlantTag.WATER)
                && existing.getTags().contains(PlantTag.STACK);
        boolean incomingIsShell = plant.getName().equalsIgnoreCase("Pumpkin");
        boolean existingIsShell = existing != null && !lilySupport
                && existing.getName().equalsIgnoreCase("Pumpkin");

        if (existing != null && !lilySupport && incomingIsShell && !existingIsShell
                && existing.getArmor() == null) {
            existing.setArmor((PlantArmour) ArmourFactory.createArmour(
                    ArmourType.PLANT_SHIELD, plant.getMaxHp(), 0, false));
            plant.setAlive(false);
            plantedAnyPlantThisMatch = true;
            return true;
        }

       if (existing != null && !lilySupport && !incomingIsShell
                && existing.getId() == plant.getId()
                && plant.getTags().contains(PlantTag.STACK)
                && plant.getMaxStackNumber() > 1) {
            if (!existing.addStack()) return false;
            plant.setAlive(false);
            plantedAnyPlantThisMatch = true;
            return true;
        }

        Integer inheritedShellHp = null;
        if (existingIsShell && !incomingIsShell) {
            inheritedShellHp = Math.max(1, existing.getHP());
            plants.remove(existing);
            cell.setPlant(null);
            existing = null;
        }

        if (existing != null && !lilySupport) return false;
        if (flooded && !plant.getTags().contains(PlantTag.WATER) && !lilySupport) return false;

        if (lilySupport) plant.setBottom(existing);

        boolean destroysGrave = plant.getName().equalsIgnoreCase("Grave Buster")
                && cell.getObstacle() instanceof Grave grave;
        cell.setPlant(plant);
        plant.setPosition(new Position(col, row));
        plants.add(plant);
        if (inheritedShellHp != null) {
            plant.setArmor((PlantArmour) ArmourFactory.createArmour(
                    ArmourType.PLANT_SHIELD, inheritedShellHp, 0, false));
        }

        if (handlesIceBlock) {
            model.match.main.season.travellog.cave.FrostbiteFreezing.damageIce(cell, IceBlock.BASE_HP, true);
        }

        if (destroysGrave) {
            destroyGrave(row, col, (Grave) cell.getObstacle());
        }

        plantedAnyPlantThisMatch = true;
        boolean nightPlant = false;
        if (plant.getTags() != null) {
            for (PlantTag tag : plant.getTags()) {
                plantFamiliesUsedThisMatch.add(tag.getName().toLowerCase());
                plantFamiliesUsedThisMatch.add(tag.name().toLowerCase());
            }
            nightPlant = plant.getTags().contains(PlantTag.NIGHT)
                    || plant.getTags().contains(PlantTag.SHROOM);
        }
        if (!nightPlant) usedNonNightPlantThisMatch = true;

        if (plant.getTags() != null && plant.getTags().contains(model.collections.plant.PlantTag.EXPLOSIVE)) {
            QuestManager.updateProgress("USE_EXPLOSIVE_PLANTS", 1, Collections.emptyMap());
        }

        return true;
    }

    public boolean removePlantAt(int row, int col) {
        Cell cell = environment.getCell(row, col);
        if (cell == null || !cell.hasPlant()) return false;

        Plant plant = cell.getPlant();
        Plant bottom = plant.getBottom();

        plant.setAlive(false);
        plant.setBottom(null);
        if (bottom != null && bottom.isAlive()) {
            cell.setPlant(bottom);
        } else {
            cell.setPlant(null);
        }
        plants.remove(plant);
        return true;
    }

    public Plant digPlantAt(int row, int col) {
        Cell cell = environment.getCell(row, col);
        if (cell == null || !cell.hasPlant()) return null;

        Plant plant = cell.getPlant();
        Plant bottom = plant.getBottom();
        cell.setPlant(bottom != null && bottom.isAlive() ? bottom : null);
        plants.remove(plant);
        return plant;
    }

    private Plant findPlantAt(int row, int col) {
        Cell cell = environment.getCell(row, col);
        return (cell != null && cell.hasPlant()) ? cell.getPlant() : null;
    }

    public String renderMap() {
        StringBuilder sb = new StringBuilder();
        if (level != null) {
            sb.append("Stage: ").append(level.getName())
                    .append(" | Game mode: ").append(level.getGameMode()).append("\n");
        }
        sb.append("Wave: ").append(getWavesSpawnedCount()).append("/").append(getTotalWaveCount())
                .append(" | Sun: ").append(sunCount)
                .append(" | Plant food: ").append(plantFoodCount);
        if (!allWavesSpawned()) {
            sb.append(" | Next wave: ")
                    .append(String.format("%.1fs", getSecondsUntilNextWave()));
        }
        sb.append("\n");
        for (int r = 0; r < environment.getRows(); r++) {
            sb.append(r + 1);
            if (!zombieBreachesEnabled) {
                sb.append(" [B] ");
            } else {
                sb.append(lawnMowers[r].isUsed() ? " [ ] " : " [M] ");
            }
            for (int c = 0; c < environment.getCols(); c++) {
                sb.append(mapSymbolFor(environment.getCell(r, c)));
            }
            sb.append("\n");
        }
        sb.append("(M=lawn mower, B=brain mode, P=plant, Z=zombie, E=zombie eating a plant, X=obstacle, ~=ice, W=water, .=empty)");
        List<GroundItem> visibleItems = items.stream()
                .filter(GroundItem.class::isInstance)
                .map(GroundItem.class::cast)
                .filter(Item::isAlive)
                .toList();
        if (!visibleItems.isEmpty()) {
            sb.append("\nGround items:");
            for (GroundItem item : visibleItems) {
                Position position = item.getPosition();
                if (position == null) continue;
                sb.append("\n  ").append(item.getItemType().name().toLowerCase())
                        .append(" at (").append((int) Math.round(position.x()) + 1)
                        .append(", ").append((int) Math.round(position.y()) + 1).append(")");
                if (item instanceof GroundSun sun && sun.isFalling()) sb.append(" [falling]");
            }
        }
        return sb.toString().trim();
    }

    private char mapSymbolFor(Cell cell) {
        if (cell == null) return '?';
        boolean hasZombie = !cell.getZombies().isEmpty();
        boolean hasPlant = cell.hasPlant();
        if (hasZombie && hasPlant) return 'E';
        if (hasZombie) return 'Z';
        if (hasPlant) return 'P';
        if (cell.getObstacle() != null) return 'X';
        if (cell.getTile() != null && cell.getTile().type() == TileType.Slippery) return '~';
        if (cell.getTile() != null && cell.getTile().type() == TileType.Water) return 'W';
        return '.';
    }

    public String renderPlantsStatus() {
        StringBuilder sb = new StringBuilder("Loadout planting status:");
        List<String> selected = controller.menus.match.BeforeMenu.selectedPlants;
        if (selected.isEmpty()) {
            sb.append("\n  no plants selected");
        }
        for (String selectedName : selected) {
            PlantJsonParser.PlantConfig config = PlantFactory.getBlueprints().values().stream()
                    .filter(candidate -> candidate.name.equalsIgnoreCase(selectedName))
                    .findFirst().orElse(null);
            if (config == null) continue;
            double cooldown = getPlantCooldown(config.id);
            sb.append("\n  ").append(config.name)
                    .append(" | cost: ").append(config.cost)
                    .append(" | ").append(GameClock.isZero(cooldown)
                            ? "ready" : String.format("recharging: %.1fs", cooldown));
        }
        sb.append("\nPlants on the field:");
        if (plants.isEmpty()) {
            sb.append("\n  none");
            return sb.toString();
        }
        for (Plant plant : plants) {
            Position position = plant.getPosition();
            sb.append("\n  ").append(plant.getName())
                    .append(" | hp: ").append(plant.getHP())
                    .append(" | level: ").append(plant.getLevel());
            if (position != null) {
                sb.append(" | position: (").append((int) position.x() + 1)
                        .append(", ").append((int) position.y() + 1).append(")");
            }
        }
        return sb.toString();
    }

    public String renderTileStatus(int row, int col) {
        Cell cell = environment.getCell(row, col);
        StringBuilder sb = new StringBuilder();
        sb.append("tile (").append(col + 1).append(", ").append(row + 1).append("): ");

        if (cell == null) {
            sb.append("out of bounds");
            return sb.toString();
        }

        List<String> parts = new ArrayList<>();

        if (cell.hasPlant()) {
            Plant plant = cell.getPlant();
            parts.add("plant=" + plant.getName() + " hp=" + plant.getHP() + " level=" + plant.getLevel());
        } else {
            parts.add("no plant");
        }

        if (cell.getObstacle() != null) {
            parts.add("obstacle=" + cell.getObstacle().getName());
        }

        if (cell.getTile() != null) {
            Tile tile = cell.getTile();
            String terrain = tile.type().toString();
            if (tile.slipperyDirection() != null) {
                terrain += " (" + tile.slipperyDirection() + ")";
            }
            parts.add("terrain=" + terrain);
        }

        if (cell.getStructure() != null) {
            parts.add("structure=" + cell.getStructure().getClass().getSimpleName());
        }

        if (level != null && level.getSeason() != null && level.getSeason().hasTide()
                && col >= environment.getCols() - level.getCurrentTideColumn()) {
            parts.add("flooded (tide)");
        }

        if (!cell.getZombies().isEmpty()) {
            parts.add("zombies=" + cell.getZombies().size());
        }

        sb.append(String.join(", ", parts));
        return sb.toString();
    }

    public String renderZombiesInfo() {
        if (zombies.isEmpty()) return "no zombies on the field";
        StringBuilder sb = new StringBuilder();
        for (Zombie zombie : zombies) {
            Position position = zombie.getPosition();
            sb.append(zombie.getName())
                    .append(" | hp: ").append(zombie.getHp())
                    .append("/").append(zombie.getMaxHp());
            if (position != null) {
                sb.append(" | position: (").append(String.format("%.2f", position.x() + 1))
                        .append(", ").append((int) Math.round(position.y()) + 1).append(")");
            }
            if (zombie.getArmor() != null && zombie.getArmor().getHP() > 0) {
                sb.append(" | armor: ").append(zombie.getArmor().getHP());
            }
            sb.append(" | state: ").append(zombie.getZombieState())
                    .append("\n");
        }
        return sb.toString().trim();
    }

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
        return List.copyOf(waves);
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
            plantCooldowns.clear();
            matchBoostedPlantIds.clear();
            IceWind.reset(this);
            beachBigWaveEntries.clear();
            beachBigWaveActive = false;
            beachBigWaveTimer = 0.0;
            beachBigWaveIndex = -1;
            Projectile.setGlobalSpeedMultiplier(0.60f);
            clock.reset();
            gameOver = false;
            gameWon = false;
            zombieBreachesEnabled = true;
            skySunEnabledOverride = null;
            wavesStarted = false;
            wavesStartedAtSeconds = 0;
            plantsLostThisMatch = 0;
            plantFamiliesUsedThisMatch.clear();
            plantedAnyPlantThisMatch = false;
            usedNonNightPlantThisMatch = false;
            plantFoodCount = 0;
            sunCount = level.getInitialSun();
            setWaves(level.getWaves());
            QuestManager.notifyLevelStarted(this);
            level.initSpecial(this);
            if (level.getSeason() != null) {
                level.getSeason().placeSeasonObstacles(this);
                if (level.getSeason().hasTide()) Flood.initialize(level, this);
            }
        }
    }

    public void setWaves(List<ZombieWave> waves) {
        this.waves = waves != null ? waves : new ArrayList<>();
        this.nextWaveIndex = 0;
        this.waveTimer = 0;
        this.currentWaveZombies = new ArrayList<>();
        this.currentWaveStartingHp = 0;
        this.hugeWaveAlertShown = false;
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
        return wavesStarted ? Math.max(0, clock.getElapsedSeconds() - wavesStartedAtSeconds) : 0;
    }

    public boolean hasUsedPlantFamily(String family) {
        return family != null && plantFamiliesUsedThisMatch.contains(family.trim().toLowerCase());
    }

    public boolean usedOnlyNightPlants() {
        return plantedAnyPlantThisMatch && !usedNonNightPlantThisMatch;
    }

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

    public void setSkySunEnabled(boolean enabled) {
        this.skySunEnabledOverride = enabled;
    }

    public boolean isSkySunEnabled() {
        return isSkySunEnabledForSession();
    }


    public List<PushableStructure> getPushableStructures() {
        Set<PushableStructure> structures = Collections.newSetFromMap(new IdentityHashMap<>());
        if (environment == null) return new ArrayList<>();
        for (int r = 0; r < environment.getRows(); r++) {
            for (int c = 0; c < environment.getCols(); c++) {
                PushableStructure structure = environment.getCell(r, c).getStructure();
                if (structure != null && structure.isAlive()) structures.add(structure);
            }
        }
        return new ArrayList<>(structures);
    }

    public void registerStructure(PushableStructure structure) {
        if (structure == null || environment == null || structure.getPosition() == null) return;
        for (int r = 0; r < environment.getRows(); r++) {
            for (int c = 0; c < environment.getCols(); c++) {
                Cell existing = environment.getCell(r, c);
                if (existing.getStructure() == structure) existing.setStructure(null);
            }
        }
        if (!structure.isAlive()) return;
        int row = (int) Math.round(structure.getPosition().y());
        int col = (int) Math.round(structure.getPosition().x());
        Cell cell = environment.getCell(row, col);
        if (cell != null && (cell.getStructure() == null || cell.getStructure() == structure)) {
            cell.setStructure(structure);
        }
    }

    public Environment getLawn() {
        return environment;
    }

    public LawnMower[] getLawnMowers() {
        return lawnMowers;
    }
}