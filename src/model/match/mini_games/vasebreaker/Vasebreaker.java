package model.match.mini_games.vasebreaker;

import model.collections.item.GroundItem;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.zombie.Zombie;
import model.match.mini_games.MiniGameMode;
import model.match.mini_games.vasebreaker.vase.*;
import model.match_mechanisms.vector.Position;
import model.pitches.Environment;
import model.utils.GameSession;
import view.GeneralPrinter;

import java.util.*;
import java.util.stream.Collectors;

public class Vasebreaker extends MiniGameMode {
    private static final double DROPPED_SEED_LIFETIME_SECONDS = 10.0;
    private static final Random RAND = new Random();
    private static final int[] DEFAULT_PLANTS = {1, 6, 44, 23, 30};

    private final GameSession session;
    private final List<Vase> vases = new ArrayList<>();
    private final List<DroppedSeedPacket> droppedPackets = new ArrayList<>();
    private final Map<Integer, Integer> seedInventory = new LinkedHashMap<>();
    private final List<String> eventLog = new ArrayList<>();
    private final List<String> zombiePool;
    private final int[] unlockedPlantIds;

    public Vasebreaker(int difficulty, int[] unlockedPlantIds) {
        setDifficulty(difficulty);
        this.session = new GameSession(5, 9);
        configureSession(session);
        session.setSkySunEnabled(false);
        this.unlockedPlantIds = normalisePlantIds(unlockedPlantIds);
        this.zombiePool = zombiePoolFor(getDifficulty());
        layoutVases();
        log("Vasebreaker level " + getDifficulty() + " is ready. Break every vase.");
    }

    private int[] normalisePlantIds(int[] requested) {
        if (requested == null || requested.length == 0) return DEFAULT_PLANTS.clone();

        List<Integer> valid = new ArrayList<>();
        Map<Integer, PlantJsonParser.PlantConfig> blueprints = PlantFactory.getBlueprints();
        for (int id : requested) {
            if (blueprints.containsKey(id) && !valid.contains(id)) valid.add(id);
        }
        if (valid.isEmpty()) return DEFAULT_PLANTS.clone();
        return valid.stream().mapToInt(Integer::intValue).toArray();
    }

    private void layoutVases() {
        // Column 0 is reserved for planting (matches PvZ2's real Vasebreaker
        // boards - a plant-only lane on the left, confirmed against real
        // gameplay screenshots). How many of the remaining 8 columns actually
        // get filled with vases scales with difficulty - easier levels use a
        // narrower board, harder ones use the full width - same idea as real
        // Vasebreaker levels growing in size later in the game.
        Environment env = session.getEnvironment();
        List<Position> spots = candidateSpots(env);
        Collections.shuffle(spots, RAND);

        int vaseCount = spots.size(); // every cell in the difficulty-selected columns
        int gargantuarVases = Math.max(1, getDifficulty() / 2);
        int plantVases = 5 + getDifficulty() * 2;

        for (int index = 0; index < vaseCount; index++) {
            Position spot = spots.get(index);
            if (index < gargantuarVases) {
                vases.add(new GargantuarVase(spot));
            } else if (index < gargantuarVases + plantVases) {
                vases.add(new PlantVase(spot, randomPlantId()));
            } else {
                vases.add(new RandomVase(spot, unlockedPlantIds, zombiePool.toArray(String[]::new)));
            }
        }
    }

    private int randomPlantId() {
        return unlockedPlantIds[RAND.nextInt(unlockedPlantIds.length)];
    }

    /** Number of vase columns (out of the 8 columns to the right of the
     *  planting column) to fill, scaled by difficulty: 4 at difficulty 1,
     *  6 at difficulty 2, all 8 at difficulty 3. */
    private int filledColumnCountFor(int cols) {
        int vaseColumns = cols - 1; // every column except the planting one
        return switch (getDifficulty()) {
            case 1 -> Math.min(vaseColumns, 4);
            case 2 -> Math.min(vaseColumns, 6);
            default -> vaseColumns;
        };
    }

    /** Every cell EXCEPT column 0 (reserved for planting), restricted to the
     *  first filledColumnCountFor() columns of the remaining board - the
     *  columns closest to the planting lane fill in first, matching PvZ2's
     *  real Vasebreaker boards where lower-difficulty levels use a narrower
     *  vase area instead of the full width. */
    private List<Position> candidateSpots(Environment env) {
        int filledColumns = filledColumnCountFor(env.getCols());
        List<Position> spots = new ArrayList<>();
        for (int row = 0; row < env.getRows(); row++) {
            for (int col = 1; col <= filledColumns; col++) {
                spots.add(new Position(col, row));
            }
        }
        return spots;
    }

    public boolean breakVaseAt(int row, int col) {
        if (row < 0 || row >= session.getRows() || col < 0 || col >= session.getCols()) {
            return false;
        }

        for (Vase vase : vases) {
            if (!vase.isBroken()
                    && (int) vase.getPosition().y() == row
                    && (int) vase.getPosition().x() == col) {
                String before = vase.getDisplayName();
                vase.breakVase(session, this);
                String result = vase.getRevealedContents();
                log("Broke " + before + " at (" + (col + 1) + ", " + (row + 1)
                        + "). Result: " + result + ".");
                announceBreakResult(vase, row, col);
                return true;
            }
        }
        return false;
    }

    private void announceBreakResult(Vase vase, int row, int col) {
        if (vase instanceof PlantVase || (vase instanceof RandomVase random
                && random.getContent() == RandomVase.Content.PLANT)) {
            DroppedSeedPacket packet = droppedPackets.stream()
                    .filter(candidate -> !candidate.collected
                            && (int) candidate.position.y() == row
                            && (int) candidate.position.x() == col)
                    .findFirst().orElse(null);
            if (packet != null) {
                log("A " + plantName(packet.plantId) + " seed packet dropped at ("
                        + (col + 1) + ", " + (row + 1)
                        + "). Collect it before it disappears.");
            }
        } else if (vase instanceof GargantuarVase) {
            log("A Gargantuar appeared at (" + (col + 1) + ", " + (row + 1) + ").");
        } else if (vase instanceof RandomVase random && random.getContent() == RandomVase.Content.ZOMBIE) {
            log("A zombie appeared at (" + (col + 1) + ", " + (row + 1) + ").");
        } else {
            log("The vase was empty.");
        }
    }

    public void dropSeedPacket(Position position, int plantId) {
        droppedPackets.add(new DroppedSeedPacket(position, plantId));
    }

    public boolean collectSeedPacket(int row, int col) {
        if (row < 0 || row >= session.getRows() || col < 0 || col >= session.getCols()) {
            return false;
        }

        for (DroppedSeedPacket packet : droppedPackets) {
            if (!packet.collected
                    && (int) packet.position.y() == row
                    && (int) packet.position.x() == col) {
                packet.collected = true;
                seedInventory.merge(packet.plantId, 1, Integer::sum);
                log("Collected " + plantName(packet.plantId) + " seed packet from ("
                        + (col + 1) + ", " + (row + 1) + "). Inventory: "
                        + seedInventory.get(packet.plantId) + ".");
                return true;
            }
        }
        return false;
    }

    public boolean plantSeed(String plantName, int row, int col) {
        Integer plantId = findPlantId(plantName);
        if (plantId == null) return false;
        return plantSeed(plantId, row, col);
    }

    public boolean plantSeed(int plantId, int row, int col) {
        if (row < 0 || row >= session.getRows() || col < 0 || col >= session.getCols()) {
            return false;
        }
        int amount = seedInventory.getOrDefault(plantId, 0);
        if (amount <= 0) return false;

        Plant plant = PlantFactory.createPlant(plantId, 1, new Position(col, row));
        if (!session.plantAt(row, col, plant)) return false;

        if (amount == 1) seedInventory.remove(plantId);
        else seedInventory.put(plantId, amount - 1);
        log("Planted " + plant.getName() + " from a seed packet at ("
                + (col + 1) + ", " + (row + 1) + ").");
        return true;
    }

    private Integer findPlantId(String plantName) {
        if (plantName == null) return null;
        String wanted = plantName.trim();
        String normalizedWanted = wanted.toLowerCase()
                .replace("-", "").replace("_", "").replace(" ", "");
        return PlantFactory.getBlueprints().values().stream()
                .filter(config -> config.name.equalsIgnoreCase(wanted)
                        || config.name.toLowerCase().replace("-", "")
                        .replace("_", "").replace(" ", "").equals(normalizedWanted)
                        || String.valueOf(config.id).equals(wanted))
                .map(config -> config.id)
                .findFirst().orElse(null);
    }

    private String plantName(int plantId) {
        PlantJsonParser.PlantConfig config = PlantFactory.getBlueprints().get(plantId);
        return config == null ? "Plant#" + plantId : config.name;
    }

    public void tick(double deltaSeconds) {
        if (isWon() || isLost()) return;
        session.tick();
        for (DroppedSeedPacket packet : droppedPackets) {
            if (!packet.collected) packet.timeLeft -= Math.max(0, deltaSeconds);
        }

        List<DroppedSeedPacket> expired = droppedPackets.stream()
                .filter(packet -> !packet.collected && packet.timeLeft <= 0)
                .toList();
        for (DroppedSeedPacket packet : expired) {
            log("The " + plantName(packet.plantId) + " seed packet at ("
                    + ((int) packet.position.x() + 1) + ", "
                    + ((int) packet.position.y() + 1) + ") disappeared.");
        }
        droppedPackets.removeIf(packet -> packet.collected || packet.timeLeft <= 0);
    }

    public boolean isWon() {
        return vases.stream().allMatch(Vase::isBroken)
                && session.getZombies().stream().noneMatch(Zombie::isAlive)
                && !session.isGameOver();
    }

    public boolean isLost() {
        return session.isGameOver();
    }

    public GameSession getSession() { return session; }
    public List<Vase> getVases() { return vases; }
    public List<DroppedSeedPacket> getDroppedPackets() { return droppedPackets; }
    public List<String> getZombiePool() { return List.copyOf(zombiePool); }
    public Map<Integer, Integer> getSeedInventory() { return Map.copyOf(seedInventory); }

    public List<String> getSeedInventoryNames() {
        return seedInventory.entrySet().stream()
                .map(entry -> plantName(entry.getKey()) + " x" + entry.getValue())
                .toList();
    }

    public Vase getVaseAt(int row, int col) {
        return vases.stream()
                .filter(vase -> (int) vase.getPosition().y() == row
                        && (int) vase.getPosition().x() == col)
                .findFirst().orElse(null);
    }

    public String renderZombiesInfo() {
        return session.renderZombiesInfo();
    }

    public String renderPlantsInfo() {
        if (session.getPlants().isEmpty()) return "no plants on the field";
        return session.getPlants().stream()
                .filter(Plant::isAlive)
                .map(plant -> plant.getName() + " | hp: " + plant.getHP()
                        + " | position: (" + ((int) plant.getPosition().x() + 1)
                        + ", " + ((int) plant.getPosition().y() + 1) + ")")
                .collect(Collectors.joining("\n"));
    }

    public String renderState() {
        int rows = session.getRows();
        int cols = session.getCols();
        char[][] board = new char[rows][cols];
        for (char[] row : board) java.util.Arrays.fill(row, '.');

        for (Vase vase : vases) {
            if (!vase.isBroken()) {
                board[(int) vase.getPosition().y()][(int) vase.getPosition().x()] = vase.getMapSymbol();
            }
        }
        for (DroppedSeedPacket packet : droppedPackets) {
            if (!packet.collected) {
                board[(int) packet.position.y()][(int) packet.position.x()] = 'S';
            }
        }
        for (Plant plant : session.getPlants()) {
            if (plant.isAlive() && plant.getPosition() != null) {
                int row = (int) Math.round(plant.getPosition().y());
                int col = (int) Math.round(plant.getPosition().x());
                if (row >= 0 && row < rows && col >= 0 && col < cols) board[row][col] = 'P';
            }
        }
        for (Zombie zombie : session.getZombies()) {
            if (zombie.isAlive() && zombie.getPosition() != null) {
                int row = (int) Math.round(zombie.getPosition().y());
                int col = (int) Math.round(zombie.getPosition().x());
                if (row >= 0 && row < rows && col >= 0 && col < cols) board[row][col] = 'Z';
            }
        }

        StringBuilder result = new StringBuilder(getStageDetails())
                .append(" | Vases left: ")
                .append(vases.stream().filter(vase -> !vase.isBroken()).count())
                .append(" | Live zombies: ")
                .append(session.getZombies().stream().filter(Zombie::isAlive).count())
                .append("\n");
        for (int row = 0; row < rows; row++) {
            result.append(row + 1).append(" ").append(board[row]).append("\n");
        }
        result.append("(?=normal vase, P=plant seed vase, G=Gargantuar vase, ")
                .append("S=seed packet, .=empty)\n");
        result.append("Vases:\n");
        for (Vase vase : vases) {
            if (!vase.isBroken()) {
                result.append("  ").append(vase.getDisplayName()).append(" at (")
                        .append((int) vase.getPosition().x() + 1).append(", ")
                        .append((int) vase.getPosition().y() + 1).append(")\n");
            }
        }
        result.append("Seeds: ")
                .append(seedInventory.isEmpty() ? "none" : getSeedInventoryNames())
                .append("\n");
        result.append("Plants:\n  ").append(renderPlantsInfo().replace("\n", "\n  ")).append("\n");
        result.append("Zombies:\n  ").append(renderZombiesInfo().replace("\n", "\n  "));
        String items = renderGroundItems();
        if (!items.isBlank()) result.append("\nGround items:\n  ")
                .append(items.replace("\n", "\n  "));
        if (!eventLog.isEmpty()) {
            result.append("\nRecent events:\n  ")
                    .append(String.join("\n  ", eventLog));
        }
        return result.toString();
    }

    private String renderGroundItems() {
        return session.getItems().stream()
                .filter(GroundItem.class::isInstance)
                .map(GroundItem.class::cast)
                .filter(item -> item.isAlive() && item.getPosition() != null)
                .map(item -> item.getItemType().name().toLowerCase() + " at ("
                        + ((int) Math.round(item.getPosition().x()) + 1) + ", "
                        + ((int) Math.round(item.getPosition().y()) + 1) + ")")
                .collect(Collectors.joining("\n"));
    }

    public String renderSeeds() {
        if (seedInventory.isEmpty()) return "Seeds: none";
        return "Seeds:\n" + getSeedInventoryNames().stream()
                .map(name -> "  " + name)
                .collect(Collectors.joining("\n"));
    }

    private void log(String message) {
        eventLog.add(message);
        if (eventLog.size() > 12) eventLog.remove(0);
        GeneralPrinter.print(message);
    }

    private List<String> zombiePoolFor(int level) {
        return switch (level) {
            case 2 -> List.of("ZombieDefault", "ZombieImp", "ZombieArmor1", "ZombieArmor2");
            case 3 -> List.of("ZombieDefault", "ZombieImp", "ZombieArmor1", "ZombieArmor2",
                    "ZombieNewspaper", "ZombieGargantuar");
            default -> List.of("ZombieDefault", "ZombieImp", "ZombieArmor1");
        };
    }

    public static class DroppedSeedPacket {
        public final Position position;
        public final int plantId;
        public double timeLeft = DROPPED_SEED_LIFETIME_SECONDS;
        public boolean collected = false;

        DroppedSeedPacket(Position position, int plantId) {
            this.position = position;
            this.plantId = plantId;
        }
    }
}