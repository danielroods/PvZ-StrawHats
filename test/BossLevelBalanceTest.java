import model.collections.Item;
import model.collections.item.GroundSun;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.zombie.Zombie;
import model.match.boss.ZombossFight;
import model.match.boss.ZombossPhase;
import model.match.main.levels.special_levels.BossLevel;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import model.utils.LevelLoader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import service.GameClock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BossLevelBalanceTest {

    private static final int EGYPT = 104;
    private static final int ICE_AGE = 204;
    private static final int BEACH = 304;
    private static final int DARK_AGES = 404;

    private static final int TIME_LIMIT_SECONDS = 600;

    private static GameSession startBattle(int levelId) throws IOException {
        BossLevel level = (BossLevel) LevelLoader.loadLevelById(levelId);
        GameSession session = new GameSession(level.getRows(), level.getCols());
        session.setLevel(level);
        session.startWaves();
        ZombossFight fight = session.getZombossFight();
        for (int i = 0; i < 400 && fight.getPhase() != ZombossPhase.NPC_TALK; i++) session.tick();
        for (int i = 0; i < fight.getDialogueCount(); i++) fight.advanceDialogue();
        for (int i = 0; i < 200 && fight.getPhase() != ZombossPhase.BATTLE; i++) session.tick();
        return session;
    }

    private static PlantJsonParser.PlantConfig configFor(String name) {
        for (PlantJsonParser.PlantConfig candidate : PlantFactory.getBlueprints().values()) {
            if (candidate.name.equalsIgnoreCase(name)) return candidate;
        }
        throw new IllegalArgumentException("unknown plant " + name);
    }

    private static String[] layoutFor(int levelId) {
        String shooter = levelId == ICE_AGE ? "Fire Peashooter" : "Repeater";
        String lobber = levelId == ICE_AGE ? "Pepper-pult" : "Melon-pult";
        return new String[] {"Sunflower", "Sunflower", shooter, shooter, lobber, "Wall-nut"};
    }

    private static void collectSun(GameSession session) {
        for (Item item : new ArrayList<>(session.getItems())) {
            if (item instanceof GroundSun sun && sun.isAlive()) {
                session.addSun(sun.getSunValue());
                sun.setAlive(false);
            }
        }
    }

    private static void replant(GameSession session, String[] layout) {
        for (int col = 0; col < layout.length; col++) {
            PlantJsonParser.PlantConfig config = configFor(layout[col]);
            for (int row = 0; row < session.getRows(); row++) {
                if (session.getPlantAt(row, col) != null) continue;
                if (session.getSunCount() < config.cost) continue;
                if (session.plantAt(row, col,
                        PlantFactory.createPlant(config.id, 1, new Position(col, row)))) {
                    session.spendSun(config.cost);
                }
            }
        }
    }

    private record Outcome(boolean won, int seconds, Set<String> zombiesSeen, int peakMinions) { }

    private static Outcome playOnce(int levelId) throws IOException {
        GameSession session = startBattle(levelId);
        ZombossFight fight = session.getZombossFight();
        String[] layout = layoutFor(levelId);
        replant(session, layout);

        Set<String> seen = new LinkedHashSet<>();
        int peak = 0;
        int ticks = (int) (TIME_LIMIT_SECONDS / GameClock.SECONDS_PER_TICK);
        for (int i = 0; i < ticks; i++) {
            collectSun(session);
            if (i % 10 == 0) replant(session, layout);
            session.tick();
            for (Zombie zombie : session.getZombies()) {
                if (zombie != fight.getBoss()) seen.add(zombie.getAlias());
            }
            peak = Math.max(peak, fight.countMinions());
            if (session.isGameWon() || session.isGameOver()) {
                return new Outcome(session.isGameWon(),
                        (int) (i * GameClock.SECONDS_PER_TICK), seen, peak);
            }
        }
        return new Outcome(false, TIME_LIMIT_SECONDS, seen, peak);
    }

    @ParameterizedTest
    @ValueSource(ints = {EGYPT, ICE_AGE, BEACH, DARK_AGES})
    void aPlainLawnCanWinEveryChapter(int levelId) throws IOException {
        int trials = 8;
        int wins = 0;
        int fastest = Integer.MAX_VALUE;
        for (int trial = 0; trial < trials; trial++) {
            Outcome outcome = playOnce(levelId);
            if (outcome.won()) {
                wins++;
                fastest = Math.min(fastest, outcome.seconds());
            }
        }
        assertTrue(wins > 0, "level " + levelId + " was unwinnable across " + trials
                + " runs with a basic lawn");
        assertTrue(fastest >= 45, "level " + levelId + " fell over in " + fastest
                + "s - a Zomboss fight should not be a formality");
    }

    @ParameterizedTest
    @ValueSource(ints = {EGYPT, ICE_AGE, BEACH, DARK_AGES})
    void everyZombieInThePoolTurnsUp(int levelId) throws IOException {
        BossLevel level = (BossLevel) LevelLoader.loadLevelById(levelId);
        List<String> pool = new ArrayList<>(level.getZombiePool());
        pool.remove(level.getChapter().getAlias());

        GameSession session = startBattle(levelId);
        ZombossFight fight = session.getZombossFight();
        session.setZombieBreachesEnabled(false);

        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < 4000; i++) {
            session.tick();
            for (Zombie zombie : session.getZombies()) {
                if (zombie == fight.getBoss()) continue;
                seen.add(zombie.getAlias());
                if (zombie.getPosition() != null && zombie.getPosition().x() < -1.0) {
                    zombie.setAlive(false);
                }
            }
        }

        Set<String> missing = new LinkedHashSet<>(pool);
        missing.removeAll(seen);
        assertTrue(missing.isEmpty(), "level " + levelId
                + " never sent these zombies across a whole match: " + missing
                + " (saw " + seen + ")");
    }

    @ParameterizedTest
    @ValueSource(ints = {EGYPT, ICE_AGE, BEACH, DARK_AGES})
    void reinforcementsStayWithinTheCrowdCap(int levelId) throws IOException {
        GameSession session = startBattle(levelId);
        ZombossFight fight = session.getZombossFight();
        session.setZombieBreachesEnabled(false);

        int peak = 0;
        for (int i = 0; i < 3000; i++) {
            session.tick();
            peak = Math.max(peak, fight.countMinions());
        }
        assertTrue(peak <= 14, "level " + levelId + " let " + peak
                + " reinforcements pile onto the lawn at once");
        assertTrue(peak > 0, "level " + levelId + " never sent any reinforcements");
    }

    @ParameterizedTest
    @ValueSource(ints = {EGYPT, ICE_AGE, BEACH, DARK_AGES})
    void theBossPausesBetweenAttacks(int levelId) throws IOException {
        GameSession session = startBattle(levelId);
        ZombossFight fight = session.getZombossFight();
        session.setZombieBreachesEnabled(false);

        int idleTicks = 0;
        int ticks = 3000;
        for (int i = 0; i < ticks; i++) {
            session.tick();
            if (fight.getAction() == null || fight.getAction().isFinished()) idleTicks++;
        }
        double idleShare = idleTicks / (double) ticks;
        assertTrue(idleShare > 0.25, "level " + levelId + " only rested "
                + Math.round(idleShare * 100) + "% of the fight; the player needs room to act");
    }
}
