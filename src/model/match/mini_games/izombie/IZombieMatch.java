package model.match.mini_games.izombie;

import model.collections.Item;
import model.collections.item.GroundItem;
import model.collections.item.GroundPlantFood;
import model.collections.item.GroundSun;
import model.collections.item.ItemType;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.utils.GameSession;
import service.GameClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IZombieMatch {

    public enum Role { PLANTS, ZOMBIES }

    public static final int ROWS = 5;
    public static final int COLS = 9;
    public static final int BRAIN_COLUMN = 0;
    public static final int REDLINE_COLUMN = 5;
    public static final double MATCH_SECONDS = 120.0;
    public static final double COUCH_MATCH_SECONDS = 360.0;

    private static final int ZOMBIE_STAT_TIER = 3;
    private static final int ZOMBIE_START_SUN = 400;
    private static final int PLANT_START_SUN = 150;
    private static final int PLANT_SUN_GRANT = 25;
    private static final double PLANT_SUN_INTERVAL = 6.0;
    private static final double ESCAPE_COLUMN = -1.2;

    private static final String[] SEED_BANK = {
            "Sunflower", "Peashooter", "Wall-nut", "Snow Pea", "Repeater", "Potato Mine",
    };

    private static final String[] DEFENDER_LAYOUT = {
            "SP.N.",
            "S.P..",
            "SPUN.",
            "S.P..",
            "SP.N.",
    };

    private static final Map<Character, Integer> LAYOUT_PLANT_IDS = Map.of(
            'S', 1, 'P', 6, 'N', 44, 'U', 23);

    private static final Map<String, ZombiePacketTemplate> KNOWN_ZOMBIE_PACKETS = Map.of(
            "ZombieImp", new ZombiePacketTemplate("Imp", 25, 5.0),
            "ZombieDefault", new ZombiePacketTemplate("Browncoat", 50, 5.0),
            "ZombieArmor1", new ZombiePacketTemplate("Conehead", 75, 7.5),
            "ZombieNewspaper", new ZombiePacketTemplate("Newspaper Zombie", 100, 12.0),
            "ZombieRa", new ZombiePacketTemplate("Ra Zombie", 100, 12.0),
            "ZombieArmor2", new ZombiePacketTemplate("Buckethead", 125, 15.0));

    /** Default roster used whenever no co-op zombie loadout was selected. */
    private static final List<String> DEFAULT_ROSTER = List.of(
            "ZombieImp", "ZombieDefault", "ZombieArmor1", "ZombieNewspaper", "ZombieRa", "ZombieArmor2");

    /** At most this many roster entries are usable (keyboard slots are 1-6 in the couch UI). */
    private static final int MAX_ROSTER_SIZE = 6;

    private record ZombiePacketTemplate(String displayName, int cost, double recharge) {}

    private final GameSession session;
    private final Brain[] brains = new Brain[ROWS];
    private final List<ZombiePacket> roster = new ArrayList<>();
    private final List<SeedCard> seeds = new ArrayList<>();
    private final Set<Zombie> zombieSideUnits = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<MatchEvent> pendingEvents = new ArrayList<>();

    private final double matchSeconds;
    private int plantSun = PLANT_START_SUN;
    private double plantSunTimer;
    private double elapsedSeconds;
    private int brainsEaten;
    private boolean finished;
    private Role winner;
    private String endReason = "";

    public IZombieMatch() {
        this(MATCH_SECONDS);
    }

    public IZombieMatch(double matchSeconds) {
        this(matchSeconds, null, null);
    }

    /**
     * @param plantLoadout  plant names to build the seed tray from (see BeforeMenu.selectedPlants);
     *                      falls back to the built-in {@link #SEED_BANK} when null/empty.
     * @param zombieLoadout zombie aliases to build the roster from (see BeforeMenu.selectedZombies);
     *                      falls back to {@link #DEFAULT_ROSTER} when null/empty.
     */
    public IZombieMatch(double matchSeconds, List<String> plantLoadout, List<String> zombieLoadout) {
        this.matchSeconds = matchSeconds;
        this.session = new GameSession(ROWS, COLS);
        session.setDifficultyLevel(ZOMBIE_STAT_TIER);
        session.setSkySunEnabled(false);
        session.setZombieBreachesEnabled(false);
        session.setLawnMowersEnabled(false);
        session.setZombieSunProductionMode(true);
        session.addSun(ZOMBIE_START_SUN);
        buildRoster(zombieLoadout);
        buildSeedBank(plantLoadout);
        seedDefendingPlants();
        placeBrains();
    }

    private void buildRoster(List<String> zombieLoadout) {
        List<String> aliases = (zombieLoadout == null || zombieLoadout.isEmpty()) ? DEFAULT_ROSTER : zombieLoadout;
        for (String alias : aliases) {
            if (alias == null || alias.isBlank() || roster.size() >= MAX_ROSTER_SIZE) continue;
            roster.add(buildPacket(alias));
        }
        if (roster.isEmpty()) {
            for (String alias : DEFAULT_ROSTER) roster.add(buildPacket(alias));
        }
    }

    private ZombiePacket buildPacket(String alias) {
        ZombiePacketTemplate known = KNOWN_ZOMBIE_PACKETS.get(alias);
        if (known != null) {
            return new ZombiePacket(alias, known.displayName(), known.cost(), known.recharge());
        }

        // No hand-tuned entry for this alias (e.g. a co-op pick outside the default six) -
        // derive a rough cost/recharge from the zombie's own HP instead of leaving it unusable.
        String displayName = friendlyZombieName(alias);
        int cost = 100;
        double recharge = 10.0;
        try {
            Zombie sample = ZombieFactory.create(alias, 0, 0);
            if (sample != null && sample.getMaxHp() > 0) {
                cost = Math.round(Math.max(25, Math.min(300, sample.getMaxHp() * 0.6f)) / 25f) * 25;
                recharge = Math.max(5.0, Math.min(20.0, sample.getMaxHp() / 40.0));
            }
        } catch (Throwable ignored) {
        }
        return new ZombiePacket(alias, displayName, cost, recharge);
    }

    private String friendlyZombieName(String alias) {
        if (alias == null || alias.isBlank()) return "Zombie";
        String withoutPrefix = alias.startsWith("Zombie") ? alias.substring("Zombie".length()) : alias;
        return withoutPrefix.isBlank() ? alias : withoutPrefix;
    }

    private void buildSeedBank(List<String> plantLoadout) {
        List<String> names = (plantLoadout == null || plantLoadout.isEmpty())
                ? java.util.Arrays.asList(SEED_BANK) : plantLoadout;
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            int id = PlantFactory.findPlantIdByName(name);
            if (id < 0) continue;
            PlantJsonParser.PlantConfig config = PlantFactory.getBlueprints().get(id);
            if (config == null) continue;
            seeds.add(new SeedCard(id, config.name, config.cost, Math.max(1.0, config.recharge)));
        }
        if (seeds.isEmpty()) {
            for (String name : SEED_BANK) {
                int id = PlantFactory.findPlantIdByName(name);
                if (id < 0) continue;
                PlantJsonParser.PlantConfig config = PlantFactory.getBlueprints().get(id);
                if (config == null) continue;
                seeds.add(new SeedCard(id, config.name, config.cost, Math.max(1.0, config.recharge)));
            }
        }
    }

    private void seedDefendingPlants() {
        for (int row = 0; row < ROWS && row < DEFENDER_LAYOUT.length; row++) {
            String line = DEFENDER_LAYOUT[row];
            for (int i = 0; i < line.length(); i++) {
                Integer plantId = LAYOUT_PLANT_IDS.get(line.charAt(i));
                if (plantId == null) continue;
                int col = 1 + i;
                if (col <= BRAIN_COLUMN || col > REDLINE_COLUMN) continue;
                Plant plant = PlantFactory.createPlant(plantId, 1, new Position(col, row));
                session.plantAt(row, col, plant);
            }
        }
    }

    private void placeBrains() {
        for (int row = 0; row < ROWS; row++) {
            brains[row] = new Brain(new Position(BRAIN_COLUMN, row));
            Cell cell = session.getEnvironment().getCell(row, BRAIN_COLUMN);
            if (cell != null) cell.setObstacle(brains[row]);
        }
    }

    public String applyIntent(Role role, String action, String target, int row, int col) {
        if (finished) return "The match is over.";
        if (role == null || action == null) return "Malformed action.";
        return switch (action) {
            case "PLACE_ZOMBIE" -> role == Role.ZOMBIES
                    ? placeZombie(target, row, col) : "You are playing the plants.";
            case "PLANT" -> role == Role.PLANTS
                    ? plantAt(target, row, col) : "You are playing the zombies.";
            case "DIG" -> role == Role.PLANTS
                    ? digAt(row, col) : "You are playing the zombies.";
            case "COLLECT_SUN" -> role == Role.PLANTS
                    ? collectAt(row, col) : "Only the plant player collects sun.";
            case "USE_PLANT_FOOD" -> role == Role.PLANTS
                    ? usePlantFood(row, col) : "Only the plant player uses plant food.";
            default -> "Unknown action: " + action;
        };
    }

    private String placeZombie(String alias, int row, int col) {
        if (row < 0 || row >= ROWS) return "That lane does not exist.";
        if (col <= REDLINE_COLUMN || col >= COLS) {
            return "Zombies drop in to the right of the red line.";
        }
        ZombiePacket packet = findPacket(alias);
        if (packet == null) return "\"" + alias + "\" is not in your roster.";
        if (!packet.isReady()) {
            return packet.getDisplayName() + " is still recharging.";
        }
        if (packet.getCost() > session.getSunCount()) {
            return "Not enough sun for " + packet.getDisplayName() + ".";
        }
        if (!session.spendSun(packet.getCost())) return "Not enough sun.";

        Zombie zombie = ZombieFactory.create(packet.getAlias(), row, col);
        zombie.setPosition(new Position(col, row));
        session.spawnZombie(zombie);
        zombieSideUnits.add(zombie);
        packet.startCooldown();
        return null;
    }

    private String plantAt(String plantName, int row, int col) {
        if (row < 0 || row >= ROWS) return "That lane does not exist.";
        if (col <= BRAIN_COLUMN || col > REDLINE_COLUMN) {
            return "Plants go between the brains and the red line.";
        }
        SeedCard card = findSeed(plantName);
        if (card == null) return "\"" + plantName + "\" is not in your seed bank.";
        if (!session.isPlantReady(card.plantId())) {
            return card.name() + " is still recharging.";
        }
        if (card.cost() > plantSun) return "Not enough sun for " + card.name() + ".";
        Cell cell = session.getEnvironment().getCell(row, col);
        if (cell == null) return "That tile does not exist.";
        if (cell.hasPlant()) return "There is already a plant there.";

        Plant plant = PlantFactory.createPlant(card.plantId(), 1, new Position(col, row));
        if (!session.plantAt(row, col, plant)) return "You cannot plant there.";
        plantSun -= card.cost();
        session.startPlantCooldown(card.plantId(), card.recharge());
        return null;
    }

    private String digAt(int row, int col) {
        if (col <= BRAIN_COLUMN || col > REDLINE_COLUMN) return "Nothing of yours is there.";
        Plant dug = session.digPlantAt(row, col);
        return dug == null ? "There is no plant there." : null;
    }

    private String collectAt(int row, int col) {
        Position target = new Position(col, row);
        boolean collected = false;
        for (Item item : new ArrayList<>(session.getItems())) {
            if (!(item instanceof GroundItem ground)) continue;
            if (!ground.isAlive() || ground.isCollected() || !ground.isNear(target)) continue;
            if (ground instanceof GroundSun sun) {
                plantSun += sun.getSunValue();
                sun.setAlive(false);
                collected = true;
            } else if (ground instanceof GroundPlantFood food) {
                session.addPlantFood();
                food.setAlive(false);
                collected = true;
            }
        }
        return collected ? null : "Nothing to collect there.";
    }

    private String usePlantFood(int row, int col) {
        Plant plant = session.getPlantAt(row, col);
        if (plant == null || !plant.isAlive()) return "No plant there to feed.";
        if (plant.getPlantFoodEffect() == null) return "That plant has no plant food effect.";
        if (plant.isPlantFoodActive()) return "That plant is already boosted.";
        if (!session.spendPlantFood()) return "You have no plant food.";
        if (!plant.activatePlant(session)) {
            session.addPlantFood();
            return "Plant food could not be used on that plant.";
        }
        return null;
    }

    public void tick() {
        if (finished) return;

        GameSession.setCurrent(session);
        elapsedSeconds += GameClock.SECONDS_PER_TICK;

        for (ZombiePacket packet : roster) {
            packet.tick(GameClock.SECONDS_PER_TICK);
        }

        plantSunTimer += GameClock.SECONDS_PER_TICK;
        if (plantSunTimer >= PLANT_SUN_INTERVAL) {
            plantSunTimer -= PLANT_SUN_INTERVAL;
            plantSun += PLANT_SUN_GRANT;
        }

        session.tick();

        discardUncollectableDrops();
        resolveBrains();
        despawnEscapedZombies();
        pruneDeadZombieUnits();
        evaluateOutcome();
    }

    private void discardUncollectableDrops() {
        session.getItems().removeIf(item -> item instanceof GroundItem ground
                && (ground.getItemType() == ItemType.COIN
                || ground.getItemType() == ItemType.DIAMOND
                || ground.getItemType() == ItemType.SEED_PACK));
    }

    private void resolveBrains() {
        for (int row = 0; row < ROWS; row++) {
            Brain brain = brains[row];
            if (brain == null || brain.isEaten() || brain.getHP() > 0) continue;
            brain.markEaten();
            brainsEaten++;
            Cell cell = session.getEnvironment().getCell(row, BRAIN_COLUMN);
            if (cell != null && cell.getObstacle() == brain) cell.setObstacle(null);
            pendingEvents.add(new MatchEvent("BRAIN_EATEN", row));
        }
    }

    private void despawnEscapedZombies() {
        for (Zombie zombie : new ArrayList<>(session.getZombies())) {
            if (zombie.getPosition() == null) continue;
            if (zombie.getPosition().x() <= ESCAPE_COLUMN) {
                zombieSideUnits.remove(zombie);
                session.getZombies().remove(zombie);
            }
        }
    }

    private void pruneDeadZombieUnits() {
        zombieSideUnits.removeIf(zombie -> !zombie.isAlive()
                || !session.getZombies().contains(zombie));
    }

    private void evaluateOutcome() {
        if (brainsEaten >= ROWS) {
            finish(Role.ZOMBIES, "All the brainz were eaten.");
            return;
        }
        if (elapsedSeconds >= matchSeconds) {
            finish(Role.PLANTS, "The plants held the lawn until time ran out.");
            return;
        }
        if (zombieSideUnits.isEmpty() && cheapestZombieCost() > session.getSunCount()) {
            finish(Role.PLANTS, "The zombie player ran out of sun and zombies.");
        }
    }

    private void finish(Role victor, String reason) {
        finished = true;
        winner = victor;
        endReason = reason;
    }

    public void forfeit(Role loser, String reason) {
        if (finished) return;
        finish(loser == Role.PLANTS ? Role.ZOMBIES : Role.PLANTS, reason);
    }

    private int cheapestZombieCost() {
        return roster.stream().mapToInt(ZombiePacket::getCost).min().orElse(Integer.MAX_VALUE);
    }

    public ZombiePacket findPacket(String alias) {
        if (alias == null) return null;
        String needle = alias.trim();
        for (ZombiePacket packet : roster) {
            if (packet.getAlias().equalsIgnoreCase(needle)
                    || packet.getDisplayName().equalsIgnoreCase(needle)) {
                return packet;
            }
        }
        return null;
    }

    public SeedCard findSeed(String name) {
        if (name == null) return null;
        String needle = name.trim();
        for (SeedCard card : seeds) {
            if (card.name().equalsIgnoreCase(needle)) return card;
        }
        return null;
    }

    public List<MatchEvent> drainEvents() {
        List<MatchEvent> drained = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        return drained;
    }

    public GameSession getSession() {
        return session;
    }

    public List<ZombiePacket> getRoster() {
        return Collections.unmodifiableList(roster);
    }

    public List<SeedCard> getSeeds() {
        return Collections.unmodifiableList(seeds);
    }

    public Brain[] getBrains() {
        return brains;
    }

    public int getPlantSun() {
        return plantSun;
    }

    public int getZombieSun() {
        return session.getSunCount();
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public double getMatchSeconds() {
        return matchSeconds;
    }

    public double getRemainingSeconds() {
        return Math.max(0.0, matchSeconds - elapsedSeconds);
    }

    public int getBrainsEaten() {
        return brainsEaten;
    }

    public int getBrainCount() {
        return ROWS;
    }

    public boolean isFinished() {
        return finished;
    }

    public Role getWinner() {
        return winner;
    }

    public String getEndReason() {
        return endReason;
    }

    public boolean isZombieSideUnit(Zombie zombie) {
        return zombieSideUnits.contains(zombie);
    }

    public record SeedCard(int plantId, String name, int cost, double recharge) { }

    public record MatchEvent(String kind, int row) { }
}