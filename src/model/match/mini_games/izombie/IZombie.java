package model.match.mini_games.izombie;

import model.collections.Item;
import model.collections.item.GroundItem;
import model.collections.item.GroundPlantFood;
import model.collections.item.GroundSun;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match.mini_games.MiniGameMode;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
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

public class IZombie extends MiniGameMode {

    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final int BRAIN_COLUMN = 0;
    private static final int REDLINE_COLUMN = 5;
    private static final int DEFENDER_FIRST_COLUMN = 1;
    private static final double ESCAPE_COLUMN = -1.2;

    
    
    
    
    private static final int ZOMBIE_STAT_TIER = 3;

    private static final Map<Integer, Integer> SUN_BUDGET = Map.of(1, 3000, 2, 4000, 3, 4500);

    private static final Map<Character, Integer> LAYOUT_PLANT_IDS = buildLayoutPlantIds();
    private static final Map<Integer, String[]> LAYOUTS = buildLayouts();
    private static final Map<Integer, List<ZombiePacket>> ROSTERS = buildRosters();

    private final GameSession session;
    private final Brain[] brains = new Brain[ROWS];
    private final List<ZombiePacket> roster;
    private final Set<Zombie> playerZombies = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<String> eventLog = new ArrayList<>();

    private final int startingSun;
    private int brainsEaten;
    private int zombiesPlaced;
    private int zombiesLost;
    private double elapsedSeconds;
    private boolean won;
    private boolean lost;

    public IZombie(int difficulty) {
        setDifficulty(difficulty);
        this.session = new GameSession(ROWS, COLS);
        configureSession(session);
        session.setDifficultyLevel(ZOMBIE_STAT_TIER);
        session.setSkySunEnabled(false);
        session.setZombieBreachesEnabled(false);
        
        
        session.setZombieSunProductionMode(true);
        this.startingSun = SUN_BUDGET.getOrDefault(getDifficulty(), 1500);
        session.addSun(startingSun);
        this.roster = ROSTERS.getOrDefault(getDifficulty(), ROSTERS.get(1)).stream()
                .map(packet -> new ZombiePacket(packet.getAlias(), packet.getDisplayName(),
                        packet.getCost(), packet.getRecharge()))
                .collect(Collectors.toList());
        seedDefendingPlants();
        placeBrains();
        log("I, Zombie level " + getDifficulty() + " started with " + startingSun + " sun.");
        log("Eat all " + ROWS + " brains to win.");
    }

    private void seedDefendingPlants() {
        String[] layout = LAYOUTS.getOrDefault(getDifficulty(), LAYOUTS.get(1));
        for (int row = 0; row < ROWS && row < layout.length; row++) {
            String line = layout[row];
            for (int i = 0; i < line.length(); i++) {
                Integer plantId = LAYOUT_PLANT_IDS.get(line.charAt(i));
                if (plantId == null) continue;
                int col = DEFENDER_FIRST_COLUMN + i;
                if (col <= BRAIN_COLUMN || col > REDLINE_COLUMN) continue;
                Plant plant = PlantFactory.createPlant(plantId, 1, new Position(col, row));
                session.plantAt(row, col, plant);
            }
        }
        log("The lawn is defended by " + session.getPlants().size() + " plants.");
    }

    private void placeBrains() {
        for (int row = 0; row < ROWS; row++) {
            brains[row] = new Brain(new Position(BRAIN_COLUMN, row));
            Cell cell = session.getEnvironment().getCell(row, BRAIN_COLUMN);
            if (cell != null) cell.setObstacle(brains[row]);
        }
    }

    public boolean placeZombie(String alias, int row) {
        return placeZombie(alias, row, COLS - 1);
    }

    public boolean placeZombie(String alias, int row, int col) {
        if (won || lost) return false;
        if (row < 0 || row >= ROWS || alias == null) return false;
        if (col <= REDLINE_COLUMN || col >= COLS) return false;

        ZombiePacket packet = findPacket(alias);
        if (packet == null || !packet.isReady()) return false;
        if (!session.spendSun(packet.getCost())) return false;

        Zombie zombie = ZombieFactory.create(packet.getAlias(), row, col);
        zombie.setPosition(new Position(col, row));
        session.spawnZombie(zombie);
        playerZombies.add(zombie);
        packet.startCooldown();
        zombiesPlaced++;
        log(packet.getDisplayName() + " placed in lane " + (row + 1) + " at column "
                + (col + 1) + " for " + packet.getCost() + " sun. Sun left: "
                + session.getSunCount() + ".");
        return true;
    }

    public ZombiePacket findPacket(String alias) {
        if (alias == null) return null;
        String needle = alias.trim();
        for (ZombiePacket packet : roster) {
            if (packet.getAlias().equalsIgnoreCase(needle)
                    || packet.getDisplayName().equalsIgnoreCase(needle)
                    || packet.getDisplayName().replace(" ", "").equalsIgnoreCase(needle)) {
                return packet;
            }
        }
        return null;
    }

    public void tick(double deltaSeconds) {
        if (won || lost) return;
        double safeDelta = Math.max(0, deltaSeconds);
        elapsedSeconds += safeDelta;
        for (ZombiePacket packet : roster) {
            packet.tick(safeDelta);
        }
        session.tick();
        discardPlantSideDrops();
        resolveBrains();
        despawnEscapedZombies();
        trackLostZombies();
        evaluateOutcome();
    }

    private void discardPlantSideDrops() {
        List<Item> items = session.getItems();
        items.removeIf(item -> item instanceof GroundSun || item instanceof GroundPlantFood);
    }

    private void resolveBrains() {
        for (int row = 0; row < ROWS; row++) {
            Brain brain = brains[row];
            if (brain == null || brain.isEaten()) continue;
            if (brain.getHP() > 0) continue;

            brain.markEaten();
            brainsEaten++;
            Cell cell = session.getEnvironment().getCell(row, BRAIN_COLUMN);
            if (cell != null && cell.getObstacle() == brain) cell.setObstacle(null);
            log("A brain was eaten in lane " + (row + 1) + ". Brains left: "
                    + (ROWS - brainsEaten) + ".");
        }
    }

    private void despawnEscapedZombies() {
        for (Zombie zombie : new ArrayList<>(session.getZombies())) {
            if (zombie.getPosition() == null) continue;
            if (zombie.getPosition().x() <= ESCAPE_COLUMN) {
                playerZombies.remove(zombie);
                session.getZombies().remove(zombie);
            }
        }
    }

    private void trackLostZombies() {
        for (Zombie zombie : new ArrayList<>(playerZombies)) {
            if (!zombie.isAlive() || !session.getZombies().contains(zombie)) {
                playerZombies.remove(zombie);
                if (!zombie.isAlive()) zombiesLost++;
            }
        }
    }

    private void evaluateOutcome() {
        if (brainsEaten >= ROWS) {
            won = true;
            log("All " + ROWS + " brains were eaten. I, Zombie is won.");
            return;
        }
        if (!playerZombies.isEmpty()) return;
        if (session.getZombies().stream().anyMatch(Zombie::isAlive)) return;
        int cheapest = cheapestCost();
        if (cheapest <= session.getSunCount()) return;
        lost = true;
        log("No zombies left on the lawn and only " + session.getSunCount()
                + " sun - not enough for the cheapest zombie (" + cheapest + ").");
    }

    private int cheapestCost() {
        return roster.stream().mapToInt(ZombiePacket::getCost).min().orElse(Integer.MAX_VALUE);
    }

    public boolean isWon() { return won; }

    public boolean isLost() { return lost; }

    public boolean isFinished() { return won || lost; }

    public List<ZombiePacket> getRoster() { return Collections.unmodifiableList(roster); }

    public Brain[] getBrains() { return brains; }

    public Brain getBrain(int row) {
        return row >= 0 && row < brains.length ? brains[row] : null;
    }

    public GameSession getSession() { return session; }

    public int getRedLineColumn() { return REDLINE_COLUMN; }

    public int getBrainColumn() { return BRAIN_COLUMN; }

    public int getBrainsEaten() { return brainsEaten; }

    public int getBrainCount() { return ROWS; }

    public int getStartingSun() { return startingSun; }

    public int getZombiesPlaced() { return zombiesPlaced; }

    public int getZombiesLost() { return zombiesLost; }

    public double getElapsedSeconds() { return elapsedSeconds; }

    public boolean isPlayerZombie(Zombie zombie) { return playerZombies.contains(zombie); }

    public List<GroundItem> collectItemsAt(int x, int y) {
        return session.collectItemsNear(new Position(x, y));
    }

    public void addSunCheat(int amount) {
        if (amount <= 0) return;
        session.addSun(amount);
        log("Cheat added " + amount + " sun. Total: " + session.getSunCount() + ".");
    }

    public String renderPlantAt(int row, int col) {
        return session.renderTileStatus(row, col);
    }

    public String renderDefendingPlants() {
        return renderPlants();
    }

    public String renderZombiesInfo() {
        return renderZombies();
    }

    public String renderRoster() {
        return roster.stream()
                .map(packet -> packet.getDisplayName() + " [" + packet.getAlias() + "] "
                        + packet.getCost() + " sun"
                        + (packet.isReady() ? "" : String.format(" (recharging %.1fs)",
                        packet.getCooldown())))
                .collect(Collectors.joining("\n  "));
    }

    public String renderState() {
        StringBuilder result = new StringBuilder(getStageDetails())
                .append(" | Sun: ").append(session.getSunCount())
                .append(" | Brains left: ").append(ROWS - brainsEaten).append("/").append(ROWS)
                .append(" | Red line after column ").append(REDLINE_COLUMN + 1)
                .append("\nAvailable zombies:\n  ").append(renderRoster())
                .append("\nLawn (| = red line, zombies are placed to its right):\n");
        Environment env = session.getEnvironment();
        for (int row = 0; row < env.getRows(); row++) {
            result.append(row + 1).append(" ");
            for (int col = 0; col < env.getCols(); col++) {
                if (col == REDLINE_COLUMN + 1) result.append("| ");
                result.append(symbolAt(row, col)).append(" ");
            }
            result.append("\n");
        }
        result.append("Legend: B=brain, -=eaten brain, P=plant, Z=your zombie, .=empty\n");
        result.append("Plants:\n  ").append(renderPlants().replace("\n", "\n  "));
        result.append("\nZombies:\n  ").append(renderZombies().replace("\n", "\n  "));
        String groundItems = renderGroundItems();
        if (!groundItems.isBlank()) {
            result.append("\nGround items:\n  ").append(groundItems.replace("\n", "\n  "));
        }
        if (!eventLog.isEmpty()) {
            result.append("\nRecent events:\n  ").append(String.join("\n  ", eventLog));
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

    private char symbolAt(int row, int col) {
        Cell cell = session.getEnvironment().getCell(row, col);
        if (cell == null) return '?';
        boolean zombieHere = session.getZombies().stream()
                .anyMatch(z -> z.isAlive() && z.getPosition() != null
                        && Math.round(z.getPosition().x()) == col
                        && Math.round(z.getPosition().y()) == row);
        if (zombieHere) return 'Z';
        if (cell.getObstacle() instanceof Brain brain) return brain.isEaten() ? '-' : 'B';
        if (col == BRAIN_COLUMN && brains[row] != null && brains[row].isEaten()) return '-';
        if (cell.getPlant() != null && cell.getPlant().isAlive()) return 'P';
        return '.';
    }

    private String renderPlants() {
        if (session.getPlants().isEmpty()) return "none";
        return session.getPlants().stream()
                .filter(plant -> plant.isAlive() && plant.getPosition() != null)
                .map(plant -> plant.getName() + " | hp: " + plant.getHP()
                        + " | position: (" + ((int) plant.getPosition().x() + 1)
                        + ", " + ((int) plant.getPosition().y() + 1) + ")")
                .collect(Collectors.joining("\n"));
    }

    private String renderZombies() {
        if (session.getZombies().isEmpty()) return "none";
        return session.getZombies().stream()
                .filter(zombie -> zombie.isAlive() && zombie.getPosition() != null)
                .map(zombie -> displayNameFor(zombie)
                        + " | hp: " + zombie.getHp() + "/" + zombie.getMaxHp()
                        + " | position: (" + String.format("%.2f", zombie.getPosition().x() + 1)
                        + ", " + ((int) Math.round(zombie.getPosition().y()) + 1) + ")"
                        + " | state: " + zombie.getZombieState())
                .collect(Collectors.joining("\n"));
    }

    private String displayNameFor(Zombie zombie) {
        ZombiePacket packet = findPacket(zombie.getAlias());
        return packet == null ? zombie.getName() : packet.getDisplayName();
    }

    private void log(String message) {
        eventLog.add(message);
        if (eventLog.size() > 14) eventLog.remove(0);
        GeneralPrinter.print(message);
    }

    private static Map<Character, Integer> buildLayoutPlantIds() {
        Map<Character, Integer> ids = new LinkedHashMap<>();
        ids.put('S', 1);
        ids.put('P', 6);
        ids.put('R', 7);
        ids.put('H', 8);
        ids.put('W', 9);
        ids.put('D', 12);
        ids.put('A', 19);
        ids.put('U', 23);
        ids.put('F', 24);
        ids.put('C', 25);
        ids.put('K', 26);
        ids.put('M', 30);
        ids.put('X', 33);
        ids.put('B', 39);
        ids.put('N', 44);
        ids.put('T', 45);
        ids.put('G', 47);
        return Map.copyOf(ids);
    }

    private static Map<Integer, String[]> buildLayouts() {
        Map<Integer, String[]> layouts = new LinkedHashMap<>();
        layouts.put(1, new String[] {
                "SP.N.",
                "S.P..",
                "SPUN.",
                "S.P..",
                "SP.N.",
        });
        layouts.put(2, new String[] {
                "SW..M",
                "SP.N.",
                "SR...",
                "SP.N.",
                "SW..M",
        });
        layouts.put(3, new String[] {
                "SRF.X",
                "SHN.M",
                "SRFT.",
                "SHN.M",
                "SRF.X",
        });
        return Map.copyOf(layouts);
    }

    private static Map<Integer, List<ZombiePacket>> buildRosters() {
        Map<Integer, List<ZombiePacket>> rosters = new LinkedHashMap<>();
        rosters.put(1, List.of(
                new ZombiePacket("ZombieImp", "Imp", 25, 5.0),
                new ZombiePacket("ZombieDefault", "Browncoat", 50, 5.0),
                new ZombiePacket("ZombieArmor1", "Conehead", 75, 7.5),
                new ZombiePacket("ZombieNewspaper", "Newspaper Zombie", 100, 12.0),
                new ZombiePacket("ZombieRa", "Ra Zombie", 100, 12.0),
                new ZombiePacket("ZombieArmor2", "Buckethead", 125, 15.0)));
        rosters.put(2, List.of(
                new ZombiePacket("ZombieDefault", "Browncoat", 50, 5.0),
                new ZombiePacket("ZombieArmor1", "Conehead", 75, 7.5),
                new ZombiePacket("ZombieRa", "Ra Zombie", 100, 12.0),
                new ZombiePacket("ZombieArmor2", "Buckethead", 125, 15.0),
                new ZombiePacket("ZombieExplorer", "Explorer Zombie", 150, 20.0)));
        rosters.put(3, List.of(
                new ZombiePacket("ZombieImp", "Imp", 25, 5.0),
                new ZombiePacket("ZombieArmor2", "Buckethead", 125, 15.0),
                new ZombiePacket("ZombieRa", "Ra Zombie", 150, 18.0),
                new ZombiePacket("ZombieArmor4", "Brickhead", 175, 20.0),
                new ZombiePacket("ZombieModernAllStar", "All-Star Zombie", 175, 25.0),
                new ZombiePacket("ZombieGargantuar", "Gargantuar", 200, 30.0)));
        return Map.copyOf(rosters);
    }
}