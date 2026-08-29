import model.collections.Item;
import model.collections.item.GroundSun;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.match.boss.ZombossFight;
import model.match.boss.ZombossPhase;
import model.match.main.levels.special_levels.BossLevel;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import model.utils.LevelLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import service.GameClock;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BossLevelBalanceTest {

    private static final int TRIALS = 5;
    private static final int TIME_LIMIT_SECONDS = 600;

    private static GameSession startBattle(int levelId) throws java.io.IOException {
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

    private record Outcome(boolean won, int seconds, double bossHealthLeft, int stunWindows) { }

    private static Outcome playOnce(int levelId, String[] layout) throws java.io.IOException {
        GameSession session = startBattle(levelId);
        ZombossFight fight = session.getZombossFight();
        replant(session, layout);

        int stunWindows = 0;
        boolean wasStunned = false;
        int ticks = (int) (TIME_LIMIT_SECONDS / GameClock.SECONDS_PER_TICK);
        for (int i = 0; i < ticks; i++) {
            collectSun(session);
            if (i % 10 == 0) replant(session, layout);
            session.tick();
            if (fight.isStunned() && !wasStunned) stunWindows++;
            wasStunned = fight.isStunned();
            if (session.isGameWon() || session.isGameOver()) {
                return new Outcome(session.isGameWon(), (int) (i * GameClock.SECONDS_PER_TICK),
                        fight.getBossHealthFraction(), stunWindows);
            }
        }
        return new Outcome(false, TIME_LIMIT_SECONDS, fight.getBossHealthFraction(), stunWindows);
    }

    private static String[] layoutFor(int levelId) {
        String shooter = levelId == 204 ? "Fire Peashooter" : "Repeater";
        return new String[] {"Sunflower", "Sunflower", shooter, shooter, "Wall-nut"};
    }

    @ParameterizedTest
    @ValueSource(ints = {104, 204, 304, 404})
    void aPlainLawnCanWinEveryChapter(int levelId) throws java.io.IOException {
        String[] layout = layoutFor(levelId);
        int wins = 0;
        int fastest = Integer.MAX_VALUE;
        double bestHealthLeft = 1.0;
        for (int trial = 0; trial < TRIALS; trial++) {
            Outcome outcome = playOnce(levelId, layout);
            bestHealthLeft = Math.min(bestHealthLeft, outcome.bossHealthLeft());
            if (outcome.won()) {
                wins++;
                fastest = Math.min(fastest, outcome.seconds());
            }
        }
        assertTrue(wins > 0, "level " + levelId + " was unwinnable in " + TRIALS
                + " runs with a basic lawn; closest run left the boss on "
                + Math.round(bestHealthLeft * 100) + "% health");
        assertTrue(fastest >= 45, "level " + levelId + " fell over in " + fastest
                + "s - a Zomboss fight should not be a formality");
    }

    @ParameterizedTest
    @ValueSource(ints = {104, 204, 304, 404})
    void reinforcementsStayWithinTheCrowdCap(int levelId) throws java.io.IOException {
        GameSession session = startBattle(levelId);
        ZombossFight fight = session.getZombossFight();
        session.setZombieBreachesEnabled(false);

        int peak = 0;
        for (int i = 0; i < 3000; i++) {
            session.tick();
            peak = Math.max(peak, fight.countMinions());
        }
        assertTrue(peak <= 12, "level " + levelId + " let " + peak
                + " reinforcements pile onto the lawn at once");
        assertTrue(peak > 0, "level " + levelId + " never sent any reinforcements");
    }

    @ParameterizedTest
    @ValueSource(ints = {104, 204, 304, 404})
    void theBossPausesBetweenAttacks(int levelId) throws java.io.IOException {
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
