package model.match.mini_games;

import model.collections.item.GroundItem;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match.waves.SpawnPlacement;
import model.match.waves.WavePacing;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
import model.pitches.obstacles.Crater;
import model.utils.GameSession;
import view.GeneralPrinter;

import java.util.*;
import java.util.stream.Collectors;

public class Beghouled extends MiniGameMode {
    private static final Random RAND = new Random();
    private static final int SUN_PER_UNIT = 50;

    public enum ResolveStage { IDLE, INVALID_SWAP_BOUNCE, MATCH_PAUSE, GRAVITY_FALL }

    public static final double SWAP_INVALID_BOUNCE_SECONDS = 0.22;
    public static final double MATCH_PAUSE_SECONDS = 0.30;
    public static final double GRAVITY_SETTLE_SECONDS = 0.5;
    private static final int CASCADE_SAFETY_LIMIT = 50;

    private final GameSession session;
    private final int[] boardPlantIds; 
    private final Map<String, UpgradePath> upgradePaths;
    private final int matchesNeeded;
    private final double respawnWaveInterval;
    private final int zombiesPerSpawn;
    private final List<String> zombiePool;

    private int matchesMade = 0;
    private double timeSinceLastWave = 0;
    private final List<String> eventLog = new ArrayList<>();

    private ResolveStage resolveStage = ResolveStage.IDLE;
    private double stageElapsed = 0;
    private double stageDuration = 0;
    private int[] pendingRevert;
    private List<List<int[]>> pendingMatchedGroups;
    private boolean pendingCascade;
    private int cascadeSafety;
    private final Set<Position> matchHighlight = new LinkedHashSet<>();

    public Beghouled(int difficulty) {
        setDifficulty(difficulty);
        this.session = new GameSession(5, 9);
        configureSession(session);
        session.setSkySunEnabled(false);
        this.boardPlantIds = plantPoolFor(getDifficulty());
        this.upgradePaths = buildUpgradePaths();
        this.matchesNeeded = 5 + getDifficulty() * 3;
        this.respawnWaveInterval = switch (getDifficulty()) {
            case 2 -> 8.0;
            case 3 -> 6.0;
            default -> 10.0;
        } * WavePacing.SPAWN_TIMING_MULTIPLIER;
        this.zombiesPerSpawn = switch (getDifficulty()) {
            case 2 -> 6;
            case 3 -> 8;
            default -> 4;
        };
        this.zombiePool = zombiePoolFor(getDifficulty());
        seedBoard();
        
        
        this.timeSinceLastWave = respawnWaveInterval;
        log("Beghouled level " + getDifficulty() + " started with a playable board.");
    }

    private int[] plantPoolFor(int difficulty) {
        return switch (difficulty) {
            case 2 -> new int[] { 1, 6, 44, 25, 27 };  
            case 3 -> new int[] { 1, 6, 44, 23, 25 };  
            default -> new int[] { 1, 6, 44, 23, 25 }; 
        };
    }

    private Map<String, UpgradePath> buildUpgradePaths() {
        Map<String, UpgradePath> map = new LinkedHashMap<>();
        map.put("Peashooter", new UpgradePath(6, 7, 500));
        map.put("Repeater", new UpgradePath(7, 21, 1500));
        map.put("Wall-nut", new UpgradePath(44, 45, 500));
        map.put("Puff-shroom", new UpgradePath(23, 24, 250));
        map.put("Cabbage-pult", new UpgradePath(25, 27, 1000));
        map.put("Melon-pult", new UpgradePath(27, 28, 750));
        return map;
    }

    private void seedBoard() {
        resetBoard();
        ensurePlayableBoard();
    }

    private void placeRandomPlant(int row, int col) {
        int plantId = boardPlantIds[RAND.nextInt(boardPlantIds.length)];
        Plant plant = PlantFactory.createPlant(plantId, 1, new Position(col, row));
        session.plantAt(row, col, plant);
    }

    private boolean isCrater(int row, int col) {
        Cell cell = session.getEnvironment().getCell(row, col);
        return cell != null && cell.getObstacle() != null;
    }

    public boolean trySwap(int row1, int col1, int row2, int col2) {
        if (isWon() || isLost() || resolveStage != ResolveStage.IDLE) return false;
        if (!inBounds(row1, col1) || !inBounds(row2, col2)) return false;
        if (!areAdjacent(row1, col1, row2, col2)) return false;
        if (isCrater(row1, col1) || isCrater(row2, col2)) return false;

        swapCells(row1, col1, row2, col2);

        if (!hasMatchThrough(row1, col1) && !hasMatchThrough(row2, col2)) {
            pendingRevert = new int[] { row1, col1, row2, col2 };
            resolveStage = ResolveStage.INVALID_SWAP_BOUNCE;
            stageElapsed = 0;
            stageDuration = SWAP_INVALID_BOUNCE_SECONDS;
            return false;
        }

        log("Swapped (" + (col1 + 1) + ", " + (row1 + 1) + ") and ("
                + (col2 + 1) + ", " + (row2 + 1) + ").");
        cascadeSafety = 0;
        beginMatchPause(false);
        return true;
    }

    public boolean isResolving() {
        return resolveStage != ResolveStage.IDLE;
    }

    public Set<Position> getMatchHighlightPositions() {
        return Set.copyOf(matchHighlight);
    }

    private void advanceResolution(double deltaSeconds) {
        if (resolveStage == ResolveStage.IDLE) return;
        stageElapsed += deltaSeconds;
        if (stageElapsed < stageDuration) return;

        switch (resolveStage) {
            case INVALID_SWAP_BOUNCE -> {
                int[] r = pendingRevert;
                pendingRevert = null;
                resolveStage = ResolveStage.IDLE;
                if (r != null) swapCells(r[0], r[1], r[2], r[3]);
            }
            case MATCH_PAUSE -> {
                int bonusUnits = pendingCascade ? 1 : 0;
                for (List<int[]> group : pendingMatchedGroups) {
                    awardSun(group.size(), bonusUnits);
                    matchesMade++;
                    log((pendingCascade ? "Cascade" : "Match") + " of " + group.size()
                            + " plants cleared. Progress: " + matchesMade + "/" + matchesNeeded + ".");
                    for (int[] cell : group) {
                        session.removePlantAt(cell[0], cell[1]);
                    }
                }
                pendingMatchedGroups = null;
                matchHighlight.clear();
                applyGravityAndRefill();
                resolveStage = ResolveStage.GRAVITY_FALL;
                stageElapsed = 0;
                stageDuration = GRAVITY_SETTLE_SECONDS;
            }
            case GRAVITY_FALL -> {
                resolveStage = ResolveStage.IDLE;
                if (cascadeSafety++ < CASCADE_SAFETY_LIMIT) {
                    beginMatchPause(true);
                } else {
                    log("Match resolution reached its safety limit; the board was reset.");
                    resetBoard();
                    finishResolution();
                }
            }
            default -> resolveStage = ResolveStage.IDLE;
        }
    }

    private void beginMatchPause(boolean cascade) {
        List<List<int[]>> groups = findMatchedGroups();
        if (groups.isEmpty()) {
            resolveStage = ResolveStage.IDLE;
            finishResolution();
            return;
        }

        pendingMatchedGroups = groups;
        pendingCascade = cascade;
        matchHighlight.clear();
        for (List<int[]> group : groups) {
            for (int[] cell : group) {
                matchHighlight.add(new Position(cell[1], cell[0]));
            }
        }
        resolveStage = ResolveStage.MATCH_PAUSE;
        stageElapsed = 0;
        stageDuration = MATCH_PAUSE_SECONDS;
    }

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < session.getRows() && col >= 0 && col < session.getCols();
    }

    private boolean areAdjacent(int row1, int col1, int row2, int col2) {
        int rowDiff = Math.abs(row1 - row2);
        int colDiff = Math.abs(col1 - col2);
        return (rowDiff + colDiff) == 1;
    }

    private void swapCells(int row1, int col1, int row2, int col2) {
        Cell a = session.getEnvironment().getCell(row1, col1);
        Cell b = session.getEnvironment().getCell(row2, col2);
        if (a == null || b == null) return;
        Plant plantA = a.getPlant();
        Plant plantB = b.getPlant();
        a.setPlant(plantB);
        b.setPlant(plantA);
        if (plantB != null) plantB.setPosition(new Position(col1, row1));
        if (plantA != null) plantA.setPosition(new Position(col2, row2));
    }

    private boolean hasMatchThrough(int row, int col) {
        return matchLength(row, col, 0, 1) + matchLength(row, col, 0, -1) >= 2
                || matchLength(row, col, 1, 0) + matchLength(row, col, -1, 0) >= 2;
    }

    private int matchLength(int row, int col, int rowStep, int colStep) {
        Integer id = plantIdAt(row, col);
        if (id == null) return 0;
        int length = 0;
        int r = row + rowStep;
        int c = col + colStep;
        while (id.equals(plantIdAt(r, c))) {
            length++;
            r += rowStep;
            c += colStep;
        }
        return length;
    }

    private Integer plantIdAt(int row, int col) {
        Cell cell = session.getEnvironment().getCell(row, col);
        if (cell == null || cell.getPlant() == null || !cell.getPlant().isAlive()) return null;
        return cell.getPlant().getId();
    }


    private void finishResolution() {
        if (!anyMoveWouldMatch()) {
            resetBoard();
            ensurePlayableBoard();
            log("No legal move remained. The board was reset.");
        }
        if (matchesMade >= matchesNeeded) {
            session.killAllZombies();
            log("The required number of matches was reached; all zombies were cleared.");
        }
    }

    private void awardSun(int matchSize, int bonusUnits) {
        int units = Math.max(1, matchSize - 2) + bonusUnits;
        session.addSun(units * SUN_PER_UNIT);
        log("Collected " + (units * SUN_PER_UNIT) + " sun from a "
                + matchSize + "-plant combination.");
    }


    private List<List<int[]>> findMatchedGroups() {
        java.util.Set<Long> matched = new java.util.LinkedHashSet<>();
        Environment env = session.getEnvironment();
        for (int row = 0; row < env.getRows(); row++) {
            scanLineForMatches(matched, row, 0, 0, 1, env.getCols());
        }
        for (int col = 0; col < env.getCols(); col++) {
            scanLineForMatches(matched, 0, col, 1, 0, env.getRows());
        }
        return groupConnectedCells(matched);
    }

    private List<List<int[]>> groupConnectedCells(java.util.Set<Long> matched) {
        List<List<int[]>> groups = new ArrayList<>();
        java.util.Set<Long> visited = new java.util.HashSet<>();

        for (long key : matched) {
            if (visited.contains(key)) continue;
            List<int[]> group = new ArrayList<>();
            java.util.Deque<Long> queue = new java.util.ArrayDeque<>();
            queue.add(key);
            visited.add(key);

            while (!queue.isEmpty()) {
                long current = queue.poll();
                int r = (int) (current >> 32);
                int c = (int) (current & 0xFFFFFFFFL);
                group.add(new int[] { r, c });
                for (long neighbor : neighborsOf(r, c)) {
                    if (matched.contains(neighbor) && visited.add(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
            groups.add(group);
        }
        return groups;
    }

    private long[] neighborsOf(int row, int col) {
        return new long[] {
                key(row - 1, col), key(row + 1, col), key(row, col - 1), key(row, col + 1)
        };
    }

    private long key(int row, int col) {
        return ((long) row << 32) | (col & 0xFFFFFFFFL);
    }

    private void scanLineForMatches(java.util.Set<Long> matched, int startRow, int startCol,
                                    int rowStep, int colStep, int length) {
        int runStart = 0;
        for (int i = 1; i <= length; i++) {
            Integer prevId = i - 1 < length ? plantIdAt(startRow + rowStep * (i - 1), startCol + colStep * (i - 1)) : null;
            Integer curId = i < length ? plantIdAt(startRow + rowStep * i, startCol + colStep * i) : null;
            if (curId == null || !curId.equals(prevId)) {
                if (i - runStart >= 3) {
                    for (int j = runStart; j < i; j++) {
                        int r = startRow + rowStep * j;
                        int c = startCol + colStep * j;
                        matched.add(((long) r << 32) | (c & 0xFFFFFFFFL));
                    }
                }
                runStart = i;
            }
        }
    }

    private void applyGravityAndRefill() {
        Environment env = session.getEnvironment();
        for (int col = 0; col < env.getCols(); col++) {
            applyGravityToColumn(col, env.getRows());
        }
    }

    private void applyGravityToColumn(int col, int rows) {
        int writeRow = rows - 1;
        for (int row = rows - 1; row >= 0; row--) {
            if (isCrater(row, col)) {
                writeRow = row - 1;
                continue;
            }
            Plant plant = session.getEnvironment().getCell(row, col).getPlant();
            if (plant == null) continue;
            moveDown(row, col, writeRow, plant);
            writeRow--;
        }
        fillRemainingGaps(col, rows);
    }

    private void moveDown(int fromRow, int col, int toRow, Plant plant) {
        if (fromRow == toRow) return;
        session.getEnvironment().getCell(fromRow, col).setPlant(null);
        session.getEnvironment().getCell(toRow, col).setPlant(plant);
        plant.setPosition(new Position(col, toRow));
    }

    private void fillRemainingGaps(int col, int rows) {
        for (int row = rows - 1; row >= 0; row--) {
            if (isCrater(row, col)) continue;
            if (session.getEnvironment().getCell(row, col).getPlant() == null) {
                placeRandomPlant(row, col);
            }
        }
    }

    private boolean anyMoveWouldMatch() {
        Environment env = session.getEnvironment();
        for (int row = 0; row < env.getRows(); row++) {
            for (int col = 0; col < env.getCols(); col++) {
                if (isCrater(row, col)) continue;
                if (col + 1 < env.getCols() && !isCrater(row, col + 1) && wouldMatchIfSwapped(row, col, row, col + 1)) return true;
                if (row + 1 < env.getRows() && !isCrater(row + 1, col) && wouldMatchIfSwapped(row, col, row + 1, col)) return true;
            }
        }
        return false;
    }

    private boolean wouldMatchIfSwapped(int row1, int col1, int row2, int col2) {
        swapCells(row1, col1, row2, col2);
        boolean result = hasMatchThrough(row1, col1) || hasMatchThrough(row2, col2);
        swapCells(row1, col1, row2, col2);
        return result;
    }

    private void resetBoard() {
        Environment env = session.getEnvironment();
        for (int row = 0; row < env.getRows(); row++) {
            for (int col = 0; col < env.getCols(); col++) {
                if (!isCrater(row, col)) {
                    session.removePlantAt(row, col);
                    placeRandomPlant(row, col);
                }
            }
        }
    }

    private void ensurePlayableBoard() {
        int attempts = 0;
        while ((hasAnyMatch() || !anyMoveWouldMatch()) && attempts++ < 100) resetBoard();
    }

    private boolean hasAnyMatch() {
        return !findMatchedGroups().isEmpty();
    }

    public boolean upgrade(String plantName) {
        if (isWon() || isLost()) return false;
        String requested = normaliseName(plantName);
        UpgradePath path = upgradePaths.entrySet().stream()
                .filter(entry -> normaliseName(entry.getKey()).equals(requested))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        if (path == null || !session.spendSun(path.cost)) return false;

        Environment env = session.getEnvironment();
        int upgradedCount = 0;
        for (int row = 0; row < env.getRows(); row++) {
            for (int col = 0; col < env.getCols(); col++) {
                Integer id = plantIdAt(row, col);
                if (id != null && id == path.fromId) {
                    Plant upgraded = PlantFactory.createPlant(path.toId, 1, new Position(col, row));
                    session.removePlantAt(row, col);
                    session.plantAt(row, col, upgraded);
                    upgradedCount++;
                }
            }
        }
        if (upgradedCount == 0) {
            session.addSun(path.cost);
            return false;
        }
        log("Upgraded " + upgradedCount + " " + plantName
                + " plant(s) for " + path.cost + " sun.");
        return true;
    }

    private String normaliseName(String value) {
        return value == null ? "" : value.toLowerCase()
                .replace("-", "").replace("_", "").replace(" ", "").trim();
    }

    public void tick(double deltaSeconds) {
        if (isWon() || isLost()) return;
        advanceResolution(Math.max(0, deltaSeconds));
        Set<Long> occupiedBeforeTick = occupiedPlantCells();
        int zombiesBefore = session.getZombies().size();
        session.tick();
        markCratersWherePlantsWereEaten(occupiedBeforeTick);
        if (session.getZombies().size() < zombiesBefore) {
            log("A zombie was removed from the board.");
        }
        timeSinceLastWave += Math.max(0, deltaSeconds);
        if (timeSinceLastWave >= respawnWaveInterval) {
            timeSinceLastWave -= respawnWaveInterval;
            spawnEndlessWave();
        }
    }

    private Set<Long> occupiedPlantCells() {
        Set<Long> occupied = new HashSet<>();
        Environment env = session.getEnvironment();
        for (int row = 0; row < env.getRows(); row++) {
            for (int col = 0; col < env.getCols(); col++) {
                Cell cell = env.getCell(row, col);
                if (cell.getPlant() != null && cell.getPlant().isAlive()) occupied.add(key(row, col));
            }
        }
        return occupied;
    }

    private void markCratersWherePlantsWereEaten(Set<Long> occupiedBeforeTick) {
        Environment env = session.getEnvironment();
        for (long position : occupiedBeforeTick) {
            int row = (int) (position >> 32);
            int col = (int) position;
            Cell cell = env.getCell(row, col);
            if (cell != null && cell.getPlant() == null && cell.getObstacle() == null) {
                cell.setObstacle(new Crater());
                log("A zombie ate the plant at (" + (col + 1) + ", " + (row + 1)
                        + "); a permanent crater was created.");
            }
        }
    }

    private void spawnEndlessWave() {
        int rows = session.getEnvironment().getRows();
        int cols = session.getEnvironment().getCols();
        double baseX = SpawnPlacement.entryX(cols);

        
        
        for (int i = 0; i < zombiesPerSpawn; i++) {
            List<Integer> laneOrder = new ArrayList<>();
            for (int row = 0; row < rows; row++) laneOrder.add(row);
            Collections.shuffle(laneOrder, RAND);

            String alias = zombiePool.get(RAND.nextInt(zombiePool.size()));
            SpawnPlacement.Placement placement = SpawnPlacement.resolve(
                    session.getZombies(), alias, cols, laneOrder, baseX);
            int lane = Math.max(0, Math.min(rows - 1, placement.lane()));

            Zombie zombie = ZombieFactory.create(alias, lane, cols - 1);
            zombie.setPosition(new Position(placement.x(), lane));
            session.spawnZombie(zombie);
            log(alias + " spawned in Beghouled lane " + (lane + 1) + ".");
        }
    }

    public boolean isWon() {
        return matchesMade >= matchesNeeded;
    }

    public boolean isLost() {
        return session.isGameOver();
    }

    public int getMatchesMade() { return matchesMade; }
    public int getMatchesNeeded() { return matchesNeeded; }
    public GameSession getSession() { return session; }
    public List<String> getZombiePool() { return List.copyOf(zombiePool); }
    public List<Integer> getBoardPlantIds() {
        return java.util.Arrays.stream(boardPlantIds).boxed().toList();
    }

    /**
     * Names of every plant that's directly seeded on this level's own board (boardPlantIds)
     * and has an upgrade defined - nothing else. A level only offers upgrades for the plants
     * that actually exist on it, full stop - no other level's plants included.
     */
    public List<String> getUpgradeablePlantNames() {
        Set<Integer> onThisBoard = new HashSet<>();
        for (int id : boardPlantIds) onThisBoard.add(id);

        List<String> names = new ArrayList<>();
        for (Map.Entry<String, UpgradePath> entry : upgradePaths.entrySet()) {
            if (onThisBoard.contains(entry.getValue().fromId)) {
                names.add(entry.getKey());
            }
        }
        return names;
    }

    
    public int getUpgradeCost(String plantName) {
        UpgradePath path = upgradePaths.get(plantName);
        return path == null ? -1 : path.cost;
    }

    public List<GroundItem> collectItemsAt(int x, int y) {
        return session.collectItemsNear(new Position(x, y));
    }

    public void addSunCheat(int amount) {
        if (amount <= 0) return;
        session.addSun(amount);
        log("Cheat added " + amount + " sun. Total: " + session.getSunCount() + ".");
    }

    public String renderPlantsInfo() {
        if (session.getPlants().isEmpty()) return "no plants on the board";
        return session.getPlants().stream()
                .filter(Plant::isAlive)
                .map(plant -> plant.getName() + " | hp: " + plant.getHP()
                        + " | position: (" + ((int) plant.getPosition().x() + 1)
                        + ", " + ((int) plant.getPosition().y() + 1) + ")")
                .collect(Collectors.joining("\n"));
    }

    public String renderZombiesInfo() {
        return session.renderZombiesInfo();
    }

    public String renderState() {
        StringBuilder result = new StringBuilder(getStageDetails())
                .append(" | Matches: ").append(matchesMade).append("/").append(matchesNeeded)
                .append(" | Sun: ").append(session.getSunCount())
                .append(" | Next zombies in: ").append(String.format("%.1fs", Math.max(0, respawnWaveInterval - timeSinceLastWave)))
                .append("\n");
        Environment env = session.getEnvironment();
        for (int row = 0; row < env.getRows(); row++) {
            result.append(row + 1).append(" ");
            for (int col = 0; col < env.getCols(); col++) {
                Cell cell = env.getCell(row, col);
                if (cell.getObstacle() != null) result.append(" XX");
                else if (cell.getPlant() == null) result.append(" ..");
                else result.append(String.format(" %02d", cell.getPlant().getId()));
            }
            result.append("\n");
        }
        String groundItems = renderGroundItems();
        return result.append("Live zombies: ")
                .append(session.getZombies().stream().filter(Zombie::isAlive).count())
                .append(" | Zombie pool: ").append(zombiePool)
                .append("\nPlants:\n  ").append(renderPlantsInfo().replace("\n", "\n  "))
                .append(groundItems.isBlank() ? "" : "\nGround items:\n  "
                        + groundItems.replace("\n", "\n  "))
                .append("\nRecent events:\n  ")
                .append(eventLog.isEmpty() ? "none" : String.join("\n  ", eventLog))
                .toString();
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

    private void log(String message) {
        eventLog.add(message);
        if (eventLog.size() > 14) eventLog.remove(0);
        GeneralPrinter.print(message);
    }

    private List<String> zombiePoolFor(int level) {
        return switch (level) {
            case 2 -> List.of("ZombieDefault", "ZombieImp", "ZombieArmor1");
            case 3 -> List.of("ZombieImp", "ZombieArmor1", "ZombieArmor2", "ZombieProspector");
            default -> List.of("ZombieDefault");
        };
    }

    private record UpgradePath(int fromId, int toId, int cost) {
    }
}