import model.collections.plant.PlantFactory;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match.main.levels.Level;
import model.match.mini_games.Zombotany;
import model.match.mini_games.wallnutbowlling.WallnutBowling;
import model.match_mechanisms.ZombieWave;
import model.utils.GameSession;
import model.utils.LevelLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaveProgressTest {

    private static final int ROWS = 5;
    private static final int COLS = 9;

    private static GameSession sessionWithWaves(double[] delays, String[][] aliases) {
        ZombieFactory.init();
        GameSession session = new GameSession(ROWS, COLS);
        session.setSkySunEnabled(false);
        session.setZombieBreachesEnabled(false);
        List<ZombieWave> waves = new ArrayList<>();
        for (int i = 0; i < delays.length; i++) {
            List<Zombie> zombies = new ArrayList<>();
            for (String alias : aliases[i]) {
                zombies.add(ZombieFactory.create(alias, 0, COLS - 1));
            }
            waves.add(new ZombieWave(delays[i], zombies));
        }
        session.setWaves(waves);
        return session;
    }

    @Test
    void waveProgressNeverMovesBackwards() {
        GameSession session = sessionWithWaves(
                new double[]{10, 20, 25, 30},
                new String[][]{
                        {"ZombieDefault"},
                        {"ZombieDefault", "ZombieImp"},
                        {"ZombieArmor1", "ZombieDefault"},
                        {"ZombieArmor1", "ZombieArmor1", "ZombieDefault", "ZombieDefault"}});
        session.startWaves();

        double previous = session.getWaveProgress();
        double worstDrop = 0;
        int worstTick = -1;
        for (int tick = 0; tick < 4000; tick++) {
            session.tick();
            double now = session.getWaveProgress();
            if (previous - now > worstDrop) {
                worstDrop = previous - now;
                worstTick = tick;
            }
            previous = now;
        }
        assertTrue(worstDrop <= 1e-9,
                "wave progress went backwards by " + worstDrop + " at tick " + worstTick);
    }

    @Test
    void waveProgressReachesOneWhenTheFinalWaveLaunches() {
        GameSession session = sessionWithWaves(
                new double[]{10, 20, 25, 30},
                new String[][]{
                        {"ZombieDefault"},
                        {"ZombieDefault", "ZombieImp"},
                        {"ZombieArmor1", "ZombieDefault"},
                        {"ZombieArmor1", "ZombieArmor1", "ZombieDefault", "ZombieDefault"}});
        session.startWaves();

        int total = session.getTotalWaveCount();
        for (int tick = 0; tick < 6000 && session.getWavesSpawnedCount() < total; tick++) {
            session.tick();
        }
        assertEquals(total, session.getWavesSpawnedCount(), "every wave should launch");
        assertEquals(1.0, session.getWaveProgress(), 1e-6,
                "the meter must be full once the final wave launches");
    }

    @Test
    void waveProgressPassesEachWaveBoundaryWhenThatWaveLaunches() {
        GameSession session = sessionWithWaves(
                new double[]{10, 20, 25, 30},
                new String[][]{
                        {"ZombieDefault"},
                        {"ZombieDefault", "ZombieImp"},
                        {"ZombieArmor1", "ZombieDefault"},
                        {"ZombieArmor1", "ZombieArmor1", "ZombieDefault", "ZombieDefault"}});
        session.startWaves();

        int total = session.getTotalWaveCount();
        int launchedBefore = 0;
        for (int tick = 0; tick < 6000; tick++) {
            session.tick();
            int launched = session.getWavesSpawnedCount();
            if (launched > launchedBefore) {
                double expected = launched / (double) total;
                assertEquals(expected, session.getWaveProgress(), 0.02,
                        "wave " + launched + " launched at progress "
                                + session.getWaveProgress() + " (expected " + expected + ")");
                launchedBefore = launched;
            }
            if (launched >= total) break;
        }
    }

    @Test
    void clearingAWaveEarlyOnlyEverPullsTheMeterForwards() {
        GameSession session = sessionWithWaves(
                new double[]{10, 20, 25, 30},
                new String[][]{
                        {"ZombieDefault"},
                        {"ZombieDefault", "ZombieImp"},
                        {"ZombieArmor1", "ZombieDefault"},
                        {"ZombieArmor1", "ZombieArmor1", "ZombieDefault", "ZombieDefault"}});
        session.startWaves();

        double previous = session.getWaveProgress();
        for (int tick = 0; tick < 4000; tick++) {
            session.tick();
            // Wipe the lawn every tick so every wave qualifies for the early pull-in.
            for (Zombie zombie : new ArrayList<>(session.getZombies())) {
                zombie.setHp(0);
            }
            session.getZombies().clear();
            double now = session.getWaveProgress();
            assertTrue(now >= previous - 1e-9,
                    "pulling a wave in early moved the meter back at tick " + tick);
            previous = now;
        }
    }

    @Test
    void aFlagWaveLevelPlantsAFlagOnItsMiddleAndFinalWaves() {
        GameSession session = sessionWithWaves(
                new double[]{10, 15, 18, 20, 22, 25, 30},
                new String[][]{
                        {"ZombieDefault"}, {"ZombieDefault"}, {"ZombieDefault"},
                        {"ZombieDefault"}, {"ZombieDefault"}, {"ZombieDefault"},
                        {"ZombieDefault"}});

        assertEquals(List.of(4, 7), session.getHugeWaveNumbers(),
                "a 7 wave level flags its middle wave and its final wave");
    }

    @Test
    void aLevelWithNoWavesReportsNoProgressAndNoFlags() {
        ZombieFactory.init();
        GameSession session = new GameSession(ROWS, COLS);
        session.setWaves(new ArrayList<>());
        session.startWaves();

        assertEquals(0, session.getTotalWaveCount());
        assertTrue(session.getHugeWaveNumbers().isEmpty());
        for (int tick = 0; tick < 200; tick++) {
            session.tick();
            assertEquals(0.0, session.getWaveProgress(), 1e-9,
                    "a level with no wave schedule has nothing to meter");
        }
    }

    @Test
    void restartingAMatchRewindsTheMeter() {
        double[] delays = {10, 20, 25, 30};
        String[][] aliases = {
                {"ZombieDefault"},
                {"ZombieDefault", "ZombieImp"},
                {"ZombieArmor1", "ZombieDefault"},
                {"ZombieArmor1", "ZombieArmor1", "ZombieDefault", "ZombieDefault"}};
        GameSession session = sessionWithWaves(delays, aliases);
        session.startWaves();
        for (int tick = 0; tick < 1000; tick++) {
            session.tick();
        }
        assertTrue(session.getWaveProgress() > 0.2, "the meter should have moved off zero");

        List<ZombieWave> replacement = new ArrayList<>();
        for (int i = 0; i < delays.length; i++) {
            List<Zombie> zombies = new ArrayList<>();
            for (String alias : aliases[i]) {
                zombies.add(ZombieFactory.create(alias, 0, COLS - 1));
            }
            replacement.add(new ZombieWave(delays[i], zombies));
        }
        session.setWaves(replacement);

        assertEquals(0.0, session.getWaveProgress(), 1e-9,
                "re-arming the schedule must reset the meter, not leave it full");
        assertEquals(0, session.getWavesSpawnedCount());
    }

    /**
     * Every authored adventure level, driven end to end: the meter has to climb from empty to
     * full without a single step back, and land on 1.0 exactly when the final wave launches.
     */
    @ParameterizedTest
    @ValueSource(ints = {101, 102, 103, 201, 202, 203, 301, 302, 303, 401, 402, 403})
    void everyAuthoredLevelRunsItsMeterFromEmptyToFull(int levelId) throws Exception {
        PlantFactory.autoInit();
        ZombieFactory.init();
        Level level = LevelLoader.loadLevelById(levelId);
        GameSession session = new GameSession(level.getRows(), level.getCols());
        session.setLevel(level);
        session.setZombieBreachesEnabled(false);
        session.startWaves();

        int total = session.getTotalWaveCount();
        assertTrue(total > 0, "level " + levelId + " should have a wave schedule");
        assertEquals(0.0, session.getWaveProgress(), 1e-9,
                "level " + levelId + " should start with an empty meter");

        double previous = 0;
        double progressWhenLastWaveLaunched = -1;
        for (int tick = 0; tick < 12000; tick++) {
            int launchedBefore = session.getWavesSpawnedCount();
            session.tick();
            double now = session.getWaveProgress();
            assertTrue(now >= previous - 1e-9, "level " + levelId
                    + " meter slipped from " + previous + " to " + now + " at tick " + tick);
            previous = now;
            if (session.getWavesSpawnedCount() > launchedBefore
                    && session.getWavesSpawnedCount() == total) {
                progressWhenLastWaveLaunched = now;
                break;
            }
            // An undefended lawn loses some special levels (the deadline, save-our-seeds and
            // survival ones) long before the schedule runs out; the meter still has to have
            // behaved on the way there, which the monotonic check above already covers.
            if (session.isGameOver() || session.isGameWon()) break;
        }

        assertTrue(session.getWavesSpawnedCount() > 0,
                "level " + levelId + " should have launched at least one wave");
        if (session.getWavesSpawnedCount() == total) {
            assertEquals(1.0, progressWhenLastWaveLaunched, 1e-6,
                    "level " + levelId + " should fill the meter as its final wave launches");
        } else {
            assertTrue(session.isGameOver() || session.isGameWon(),
                    "level " + levelId + " stalled with " + session.getWavesSpawnedCount()
                            + "/" + total + " waves launched and the match still running");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void zombotanyMetersItsOwnWaveSchedule(int difficulty) {
        PlantFactory.autoInit();
        ZombieFactory.init();
        Zombotany game = new Zombotany(difficulty);
        GameSession session = game.getSession();
        assertTrue(session.getTotalWaveCount() > 0, "Zombotany runs on a real wave schedule");

        double previous = session.getWaveProgress();
        for (int tick = 0; tick < 4000; tick++) {
            game.tick(0.1);
            double now = session.getWaveProgress();
            assertTrue(now >= previous - 1e-9,
                    "Zombotany level " + difficulty + " meter slipped at tick " + tick);
            previous = now;
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void wallnutBowlingMetersItsOwnWaveSchedule(int difficulty) {
        PlantFactory.autoInit();
        ZombieFactory.init();
        WallnutBowling game = new WallnutBowling(difficulty);
        GameSession session = game.getSession();
        assertTrue(session.getTotalWaveCount() > 0, "Wall-nut Bowling runs on a real schedule");

        double previous = session.getWaveProgress();
        for (int tick = 0; tick < 4000; tick++) {
            game.tick(0.1);
            double now = session.getWaveProgress();
            assertTrue(now >= previous - 1e-9,
                    "Wall-nut Bowling level " + difficulty + " meter slipped at tick " + tick);
            previous = now;
        }
    }
}
