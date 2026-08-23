package model.match.mini_games;

import model.collections.item.GroundItem;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.game_exceptions.GameException;
import model.match_mechanisms.ZombieWave;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import view.GeneralPrinter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Zombotany extends MiniGameMode {

    public static final String PEASHOOTER_ZOMBIE = "ZombiePeashooter";
    public static final String GATLING_ZOMBIE = "ZombieGatlingPea";
    public static final String WALLNUT_ZOMBIE = "ZombieWallnut";
    public static final String TALLNUT_ZOMBIE = "ZombieTallnut";
    public static final String JALAPENO_ZOMBIE = "ZombieJalapeno";
    public static final String SQUASH_ZOMBIE = "ZombieSquash";
    public static final String BASIC_ZOMBIE = "ZombieDefault";

    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final int MAX_EVENT_LOG = 14;

    /** Human-readable names for the plant-headed zombies, for logs and the almanac strip. */
    private static final Map<String, String> DISPLAY_NAMES = buildDisplayNames();

    private final GameSession session;
    private final List<String> availablePlants;
    private final List<String> zombiePool;
    private final List<String> eventLog = new ArrayList<>();
    private final Set<Zombie> announcedZombies = Collections.newSetFromMap(new IdentityHashMap<>());

    private final int startingSun;
    private int zombiesKilled;
    private int plantsLost;
    private double elapsedSeconds;
    private boolean won;
    private boolean lost;

    public Zombotany(int difficulty) {
        setDifficulty(difficulty);
        this.session = new GameSession(ROWS, COLS);
        configureSession(session);
        session.setSkySunEnabled(true);
        session.setZombieBreachesEnabled(true);
        this.availablePlants = availablePlantsFor(getDifficulty());
        this.zombiePool = zombiePoolFor(getDifficulty());
        this.startingSun = startingSunFor(getDifficulty());
        session.addSun(startingSun);
        session.setWaves(wavesFor(getDifficulty()));
        session.startWaves();
        log("Zombotany level " + getDifficulty() + " started with " + startingSun + " sun.");
        log("Watch out - these zombies fight back with the plants they are wearing.");
    }

    public void tick(double deltaSeconds) {
        if (won || lost) return;

        List<Plant> plantsBefore = new ArrayList<>(session.getPlants());
        List<Zombie> zombiesBefore = new ArrayList<>(session.getZombies());

        session.tick();
        elapsedSeconds += deltaSeconds;

        for (Plant plant : plantsBefore) {
            if (!plant.isAlive()) {
                plantsLost++;
                log(plant.getName() + " was lost.");
            }
        }
        for (Zombie zombie : zombiesBefore) {
            if (!zombie.isAlive()) {
                zombiesKilled++;
                announcedZombies.remove(zombie);
            }
        }
        announceNewZombies();
        announcedZombies.retainAll(session.getZombies());
        evaluateOutcome();
    }

    private void announceNewZombies() {
        for (Zombie zombie : session.getZombies()) {
            if (zombie.getPosition() == null || !announcedZombies.add(zombie)) continue;
            log(displayName(zombie.getAlias()) + " is coming down lane "
                    + ((int) Math.round(zombie.getPosition().y()) + 1) + ".");
        }
    }

    private void evaluateOutcome() {
        if (won || lost) return;
        if (session.isGameOver()) {
            lost = true;
            log("A zombie got past the last lawn mower.");
            return;
        }
        if (session.areWavesDone()) {
            won = true;
            log("Every Zombotany wave has been cleared.");
        }
    }

    public boolean isWon() {
        return won;
    }

    public boolean isLost() {
        return lost;
    }

    public boolean isFinished() {
        return won || lost;
    }

    public boolean plantAt(String plantName, int row, int col) {
        if (isFinished()) throw new GameException("the match is already over.");
        if (row < 0 || row >= session.getRows() || col < 0 || col >= session.getCols()) {
            throw new GameException("that tile is off the lawn.");
        }
        PlantJsonParser.PlantConfig config = findConfig(plantName);
        if (config == null || availablePlants.stream()
                .noneMatch(name -> name.equalsIgnoreCase(config.name))) {
            throw new GameException("\"" + plantName
                    + "\" is not in this level's seed packet row.");
        }
        if (!session.isPlantReady(config.id)) {
            throw new GameException(config.name + " is recharging for "
                    + String.format("%.1f", session.getPlantCooldown(config.id))
                    + " more seconds.");
        }

        Plant plant = PlantFactory.createPlant(config.id, 1, new Position(col, row));
        if (session.getSunCount() < plant.getCost()) {
            throw new GameException("not enough sun; " + config.name
                    + " costs " + plant.getCost() + ".");
        }
        if (!session.plantAt(row, col, plant)) {
            throw new GameException("that tile is occupied or blocked.");
        }

        session.spendSun(plant.getCost());
        session.startPlantCooldown(config.id, plant.getRecharge());
        log(plant.getName() + " planted at (" + (col + 1) + ", " + (row + 1)
                + "). Sun: " + session.getSunCount() + ".");
        return true;
    }

    public Plant digPlantAt(int row, int col) {
        if (isFinished()) return null;
        Plant dug = session.digPlantAt(row, col);
        if (dug != null) {
            log(dug.getName() + " was dug up at (" + (col + 1) + ", " + (row + 1) + ").");
        }
        return dug;
    }

    public boolean feedPlantAt(int row, int col) {
        if (isFinished()) throw new GameException("the match is already over.");
        Plant plant = plantAt(row, col);
        if (plant == null) throw new GameException("there is no plant there to feed.");
        if (plant.getPlantFoodEffect() == null) {
            throw new GameException(plant.getName() + " has no plant food effect.");
        }
        if (plant.isPlantFoodActive()) {
            throw new GameException(plant.getName() + " is already supercharged.");
        }
        if (!session.spendPlantFood()) throw new GameException("no plant food available.");
        if (!plant.activatePlant(session)) {
            session.addPlantFood();
            throw new GameException("plant food could not be used on " + plant.getName() + ".");
        }
        log("Plant food supercharged " + plant.getName() + " at ("
                + (col + 1) + ", " + (row + 1) + ").");
        return true;
    }

    public Plant plantAt(int row, int col) {
        return session.getPlantAt(row, col);
    }

    public List<GroundItem> collectItemsAt(int x, int y) {
        return session.collectItemsNear(new Position(x, y));
    }

    public void addSunCheat(int amount) {
        if (amount <= 0) return;
        session.addSun(amount);
        log("Cheat added " + amount + " sun. Total: " + session.getSunCount() + ".");
    }

    public boolean addPlantFoodCheat() {
        boolean added = session.addPlantFood();
        log(added ? "Cheat added 1 plant food." : "Plant food storage is already full.");
        return added;
    }

    public Zombie spawnZombie(String alias, int row) {
        String resolved = resolveAlias(alias);
        if (resolved == null) {
            throw new GameException("\"" + alias + "\" is not a Zombotany zombie.");
        }
        int lane = Math.max(0, Math.min(session.getRows() - 1, row));
        Zombie zombie = ZombieFactory.create(resolved, lane, session.getCols() - 1);
        zombie.setPosition(new Position(session.getCols() - 1 + 0.5, lane));
        session.spawnZombie(zombie);
        log(displayName(resolved) + " spawned in lane " + (lane + 1) + ".");
        return zombie;
    }

    private String resolveAlias(String alias) {
        if (alias == null) return null;
        String normalized = alias.trim().replace("-", "").replace("_", "").replace(" ", "");
        for (String candidate : DISPLAY_NAMES.keySet()) {
            if (candidate.equalsIgnoreCase(normalized)
                    || candidate.equalsIgnoreCase("Zombie" + normalized)
                    || displayName(candidate).replace(" ", "").equalsIgnoreCase(normalized)) {
                return candidate;
            }
        }
        return null;
    }

    public GameSession getSession() {
        return session;
    }

    public List<String> getAvailablePlants() {
        return List.copyOf(availablePlants);
    }

    public List<String> getZombiePool() {
        return List.copyOf(zombiePool);
    }

    public int getStartingSun() {
        return startingSun;
    }

    public int getZombiesKilled() {
        return zombiesKilled;
    }

    public int getPlantsLost() {
        return plantsLost;
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getWavesSurvived() {
        return session.getWavesSpawnedCount();
    }

    public int getTotalWaves() {
        return session.getTotalWaveCount();
    }

    public List<String> getEventLog() {
        return List.copyOf(eventLog);
    }

    public static String displayName(String alias) {
        return DISPLAY_NAMES.getOrDefault(alias, alias);
    }

    public String renderZombiesInfo() {
        return session.renderZombiesInfo();
    }

    public String renderPlantsInfo() {
        String rendered = session.getPlants().stream()
                .filter(Plant::isAlive)
                .map(plant -> plant.getName() + " | hp: " + plant.getHP()
                        + " | position: (" + ((int) plant.getLocation().x() + 1)
                        + ", " + ((int) plant.getLocation().y() + 1) + ")")
                .collect(Collectors.joining("\n"));
        return rendered.isEmpty() ? "no plants on the field" : rendered;
    }

    public String renderRoster() {
        return zombiePool.stream()
                .map(alias -> displayName(alias) + " (" + alias + ")")
                .collect(Collectors.joining("\n  "));
    }

    public String renderState() {
        String plantsOnField = session.getPlants().stream()
                .filter(Plant::isAlive)
                .map(plant -> plant.getName() + " at ("
                        + ((int) plant.getLocation().x() + 1) + ", "
                        + ((int) plant.getLocation().y() + 1) + ")"
                        + " hp=" + plant.getHP())
                .collect(Collectors.joining("\n  "));
        if (plantsOnField.isEmpty()) plantsOnField = "none";

        StringBuilder result = new StringBuilder(getStageDetails()
                + " | Sun: " + session.getSunCount()
                + " | Plant food: " + session.getPlantFoodCount()
                + " | Waves: " + getWavesSurvived() + "/" + getTotalWaves()
                + "\nSeed packets: " + availablePlants
                + "\nZombie roster:\n  " + renderRoster()
                + "\n" + session.renderMap()
                + "\nPlants on the field:\n  " + plantsOnField
                + "\nZombies:\n  " + renderZombiesInfo().replace("\n", "\n  "));
        if (!eventLog.isEmpty()) {
            result.append("\nRecent events:\n  ").append(String.join("\n  ", eventLog));
        }
        return result.toString();
    }

    private void log(String message) {
        eventLog.add(message);
        if (eventLog.size() > MAX_EVENT_LOG) eventLog.remove(0);
        GeneralPrinter.print(message);
    }

    private PlantJsonParser.PlantConfig findConfig(String plantName) {
        if (plantName == null) return null;
        for (PlantJsonParser.PlantConfig config : PlantFactory.getBlueprints().values()) {
            if (config.name.equalsIgnoreCase(plantName.trim())) return config;
        }
        return null;
    }

    private static Map<String, String> buildDisplayNames() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put(PEASHOOTER_ZOMBIE, "Peashooter Zombie");
        names.put(GATLING_ZOMBIE, "Gatling Pea Zombie");
        names.put(WALLNUT_ZOMBIE, "Wall-nut Zombie");
        names.put(TALLNUT_ZOMBIE, "Tall-nut Zombie");
        names.put(JALAPENO_ZOMBIE, "Jalapeno Zombie");
        names.put(SQUASH_ZOMBIE, "Squash Zombie");
        names.put(BASIC_ZOMBIE, "Browncoat Zombie");
        return Collections.unmodifiableMap(names);
    }

    private int startingSunFor(int level) {
        return switch (level) {
            case 2 -> 250;
            case 3 -> 200;
            default -> 300;
        };
    }

    private List<String> availablePlantsFor(int level) {
        return switch (level) {
            case 2 -> List.of("Sunflower", "Peashooter", "Repeater", "Snow Pea",
                    "Wall-nut", "Potato Mine", "Cherry Bomb");
            case 3 -> List.of("Sunflower", "Peashooter", "Repeater",
                    "Wall-nut", "Tall-nut", "Squash", "Jalapeno");
            default -> List.of("Sunflower", "Peashooter", "Wall-nut", "Potato Mine");
        };
    }

    private List<String> zombiePoolFor(int level) {
        return switch (level) {
            case 2 -> List.of(BASIC_ZOMBIE, PEASHOOTER_ZOMBIE, WALLNUT_ZOMBIE,
                    SQUASH_ZOMBIE, JALAPENO_ZOMBIE);
            case 3 -> List.of(PEASHOOTER_ZOMBIE, GATLING_ZOMBIE, WALLNUT_ZOMBIE,
                    TALLNUT_ZOMBIE, SQUASH_ZOMBIE, JALAPENO_ZOMBIE);
            default -> List.of(BASIC_ZOMBIE, PEASHOOTER_ZOMBIE, WALLNUT_ZOMBIE);
        };
    }

    private List<ZombieWave> wavesFor(int level) {
        return switch (level) {
            case 2 -> MiniGameWaves.create(session,
                    new double[] {22, 26, 28, 30, 34},
                    new String[][] {
                        {PEASHOOTER_ZOMBIE, BASIC_ZOMBIE},
                        {PEASHOOTER_ZOMBIE, PEASHOOTER_ZOMBIE, SQUASH_ZOMBIE},
                        {PEASHOOTER_ZOMBIE, PEASHOOTER_ZOMBIE, WALLNUT_ZOMBIE, SQUASH_ZOMBIE},
                        {PEASHOOTER_ZOMBIE, PEASHOOTER_ZOMBIE, WALLNUT_ZOMBIE,
                            JALAPENO_ZOMBIE, SQUASH_ZOMBIE, BASIC_ZOMBIE},
                        {PEASHOOTER_ZOMBIE, PEASHOOTER_ZOMBIE, PEASHOOTER_ZOMBIE,
                            PEASHOOTER_ZOMBIE, WALLNUT_ZOMBIE, WALLNUT_ZOMBIE,
                            JALAPENO_ZOMBIE, JALAPENO_ZOMBIE, SQUASH_ZOMBIE,
                            SQUASH_ZOMBIE, SQUASH_ZOMBIE},
                    });
            case 3 -> MiniGameWaves.create(session,
                    new double[] {20, 24, 26, 28, 32},
                    new String[][] {
                        {PEASHOOTER_ZOMBIE, WALLNUT_ZOMBIE},
                        {PEASHOOTER_ZOMBIE, WALLNUT_ZOMBIE, JALAPENO_ZOMBIE, SQUASH_ZOMBIE},
                        {GATLING_ZOMBIE, WALLNUT_ZOMBIE, TALLNUT_ZOMBIE,
                            JALAPENO_ZOMBIE, SQUASH_ZOMBIE},
                        {GATLING_ZOMBIE, GATLING_ZOMBIE, TALLNUT_ZOMBIE, TALLNUT_ZOMBIE,
                            JALAPENO_ZOMBIE, SQUASH_ZOMBIE, PEASHOOTER_ZOMBIE},
                        {GATLING_ZOMBIE, GATLING_ZOMBIE, GATLING_ZOMBIE, GATLING_ZOMBIE,
                            TALLNUT_ZOMBIE, TALLNUT_ZOMBIE, TALLNUT_ZOMBIE,
                            WALLNUT_ZOMBIE, WALLNUT_ZOMBIE, JALAPENO_ZOMBIE,
                            JALAPENO_ZOMBIE, JALAPENO_ZOMBIE, SQUASH_ZOMBIE, SQUASH_ZOMBIE},
                    });
            default -> MiniGameWaves.create(session,
                    new double[] {25, 30, 32, 36},
                    new String[][] {
                        {BASIC_ZOMBIE, PEASHOOTER_ZOMBIE},
                        {PEASHOOTER_ZOMBIE, PEASHOOTER_ZOMBIE, BASIC_ZOMBIE},
                        {PEASHOOTER_ZOMBIE, PEASHOOTER_ZOMBIE, PEASHOOTER_ZOMBIE,
                            WALLNUT_ZOMBIE},
                        {PEASHOOTER_ZOMBIE, PEASHOOTER_ZOMBIE, PEASHOOTER_ZOMBIE,
                            PEASHOOTER_ZOMBIE, PEASHOOTER_ZOMBIE, WALLNUT_ZOMBIE,
                            WALLNUT_ZOMBIE, BASIC_ZOMBIE, BASIC_ZOMBIE},
                    });
        };
    }
}
