import model.collections.plant.PlantFactory;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match.endless.EndlessChapter;
import model.match.endless.EndlessLevel;
import model.match.endless.EndlessLevels;
import model.match.endless.EndlessRun;
import model.match.endless.EndlessWaveDirector;
import model.match.main.levels.Level;
import model.match_mechanisms.ZombieWave;
import model.user_data.UserState;
import model.utils.GameSession;
import model.utils.LevelLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndlessLotteryTest {

    private static List<Level> allLevels;

    @BeforeAll
    static void loadGameData() throws Exception {
        PlantFactory.autoInit();
        ZombieFactory.init();
        allLevels = LevelLoader.loadLevels();
    }

    private static EndlessLevel lotteryFor(EndlessChapter chapter) {
        EndlessLevel level = EndlessLevels.forChapter(chapter,
                EndlessLevels.levelsOf(chapter, allLevels));
        assertNotNull(level, chapter + " should have a Lottery level");
        return level;
    }

    @ParameterizedTest
    @EnumSource(EndlessChapter.class)
    void everyChapterLotteryLoadsItsOwnSeasonBoardAndRoster(EndlessChapter chapter) {
        EndlessLevel level = lotteryFor(chapter);

        assertNotNull(level.getSeason(), "a Lottery level must carry a season");
        assertEquals(chapter.seasonName(), level.getSeason().getName(),
                "the Lottery plays in its own chapter's season");
        assertEquals(chapter.levelId(), level.getId(), "each chapter has its own level id");
        assertEquals(chapter.levelName(), level.getName());
        assertTrue(level.getRows() > 0 && level.getCols() > 0);
        assertFalse(level.getAvailablePlants().isEmpty(),
                "the loadout offers the chapter's plants");

        List<String> chapterPool = new ArrayList<>();
        for (Level authored : EndlessLevels.levelsOf(chapter, allLevels)) {
            chapterPool.addAll(authored.getZombiePool());
        }
        assertFalse(level.getZombiePool().isEmpty(), "the roster must not be empty");
        for (String alias : level.getZombiePool()) {
            assertTrue(chapterPool.contains(alias),
                    alias + " is not part of " + chapter + "'s own zombie pool");
        }
    }

    @Test
    void theFourChapterLotteriesAreFourSeparateLevels() {
        List<Integer> ids = new ArrayList<>();
        for (EndlessChapter chapter : EndlessChapter.values()) {
            int id = lotteryFor(chapter).getId();
            assertFalse(ids.contains(id), "chapter ids must not collide");
            assertTrue(id < 0, "a Lottery id must stay off the adventure ladder");
            ids.add(id);
        }
        assertEquals(4, ids.size());
    }

    @Test
    void frostbiteLeavesOutTheZombiesItsSeasonPreFreezes() {
        EndlessLevel level = lotteryFor(EndlessChapter.FROSTBITE_CAVES);
        for (String alias : level.getZombiePool()) {
            assertFalse(ZombieFactory.shouldSpawnFrosted(alias),
                    alias + " spawns frozen and never enters through a wave");
        }
    }

    @Test
    void wavesGetBiggerAndTheLullGetsShorterAsTheRunGoesOn() {
        EndlessLevel level = lotteryFor(EndlessChapter.EGYPT);
        EndlessWaveDirector director = (EndlessWaveDirector) level.getWaveDirector();

        int earlyCost = director.waveAt(0).getWaveCost();
        int midCost = director.waveAt(15).getWaveCost();
        int lateCost = director.waveAt(40).getWaveCost();
        assertTrue(midCost > earlyCost, "wave 16 must outweigh wave 1");
        assertTrue(lateCost > midCost, "wave 41 must outweigh wave 16");

        assertTrue(director.delaySeconds(30) < director.delaySeconds(0),
                "the lull between waves has to tighten");
        assertTrue(director.powerMultiplier(60) > director.powerMultiplier(0),
                "zombies themselves have to get tougher");
    }

    @Test
    void theRosterOpensUpAsTheRunGoesOnButNeverLeavesTheChapter() {
        EndlessLevel level = lotteryFor(EndlessChapter.DARK_AGES);
        EndlessWaveDirector director = (EndlessWaveDirector) level.getWaveDirector();
        List<String> roster = director.getRoster();

        List<String> earlyAliases = aliasesIn(director, 0, 12);
        List<String> lateAliases = aliasesIn(director, 60, 12);

        assertTrue(lateAliases.size() >= earlyAliases.size(),
                "later waves should draw on at least as many kinds of zombie");
        for (String alias : lateAliases) {
            assertTrue(roster.contains(alias), alias + " is outside the chapter roster");
        }
        assertTrue(averageCost(lateAliases) > averageCost(earlyAliases),
                "later waves must lean on the more dangerous end of the roster");
    }

    private static List<String> aliasesIn(EndlessWaveDirector director, int waveIndex,
                                          int samples) {
        List<String> aliases = new ArrayList<>();
        for (int sample = 0; sample < samples; sample++) {
            for (Zombie zombie : director.waveAt(waveIndex).getWaveZombies()) {
                aliases.add(zombie.getAlias());
            }
        }
        return aliases;
    }

    private static double averageCost(List<String> aliases) {
        if (aliases.isEmpty()) return 0;
        double total = 0;
        for (String alias : aliases) total += ZombieFactory.getZombieCost(alias);
        return total / aliases.size();
    }

    /**
     * Wave one million, asked for directly: the budget, the wave size and the power
     * multiplier all have to stay finite and sane, or a long run would eventually wrap an
     * int around and hand the player a free lawn.
     */
    @Test
    void aWaveDeepIntoTheRunStaysWithinItsLimits() {
        EndlessLevel level = lotteryFor(EndlessChapter.BIG_WAVE_BEACH);
        EndlessWaveDirector director = (EndlessWaveDirector) level.getWaveDirector();

        ZombieWave wave = director.waveAt(1_000_000);
        assertTrue(wave.getWaveZombies().size() <= 45,
                "a wave cannot grow without bound: " + wave.getWaveZombies().size());
        assertTrue(wave.getWaveCost() > 0, "wave cost must not overflow into a negative");
        assertTrue(director.budgetFor(1_000_000) > 0, "the budget must not overflow");
        assertTrue(director.delaySeconds(1_000_000) >= 8.0, "the lull has a floor");

        for (Zombie zombie : wave.getWaveZombies()) {
            int before = zombie.getMaxHp();
            director.empower(zombie, 1_000_000);
            assertTrue(zombie.getMaxHp() >= before, "empowering must not wrap the hp around");
            assertTrue(zombie.getMaxHp() < Integer.MAX_VALUE / 2, "hp stays well inside an int");
        }
    }

    @ParameterizedTest
    @EnumSource(EndlessChapter.class)
    void anEndlessMatchKeepsSpawningAndIsNeverWon(EndlessChapter chapter) {
        EndlessLevel level = lotteryFor(chapter);
        GameSession session = new GameSession(level.getRows(), level.getCols());
        session.setDifficultyLevel(3);
        session.setZombieBreachesEnabled(false);
        session.setLevel(level);
        session.startWaves();

        assertTrue(session.isEndless(), "the Lottery level drives the session endlessly");

        int spawnedSoFar = 0;
        for (int tick = 0; tick < 20_000; tick++) {
            session.tick();
            
            for (Zombie zombie : new ArrayList<>(session.getZombies())) zombie.setHp(0);
            session.getZombies().clear();
            spawnedSoFar = Math.max(spawnedSoFar, session.getWavesSpawnedCount());
            assertFalse(session.isGameWon(), "an endless run is never won");
            assertFalse(session.allWavesSpawned(), "an endless run never runs out of waves");
        }

        assertTrue(spawnedSoFar > 20,
                chapter + " should have launched many waves, launched " + spawnedSoFar);
        assertEquals(spawnedSoFar, session.getTotalWaveCount(),
                "the wave count reports how far the run has got");
    }

    /**
     * Scaling has to reach every zombie on the lawn, not only the ones the wave schedule
     * placed - Dark Ages raises its own from graves and a Gargantuar throws its own imps,
     * and a deep run would be trivial if those stayed at their starting strength.
     */
    @Test
    void everyZombieThatReachesTheLawnIsScaledByHowDeepTheRunIs() {
        EndlessLevel level = lotteryFor(EndlessChapter.EGYPT);
        GameSession session = new GameSession(level.getRows(), level.getCols());
        session.setZombieBreachesEnabled(false);
        session.setLevel(level);
        session.startWaves();

        for (int tick = 0; tick < 20_000 && session.getWavesSpawnedCount() < 20; tick++) {
            session.tick();
            for (Zombie zombie : new ArrayList<>(session.getZombies())) zombie.setHp(0);
            session.getZombies().clear();
        }
        assertTrue(session.getWavesSpawnedCount() >= 20, "the run should be well under way");

        int unscaled = ZombieFactory.create("ZombieDefault", 0, level.getCols() - 1).getMaxHp();
        Zombie risen = ZombieFactory.create("ZombieDefault", 0, level.getCols() - 1);
        session.spawnZombie(risen);
        assertTrue(risen.getMaxHp() > unscaled,
                "a zombie joining at wave 20 is tougher than one at wave 1");
    }

    @Test
    void killsDuringAnEndlessRunAddUpToAScore() {
        EndlessLevel level = lotteryFor(EndlessChapter.EGYPT);
        GameSession session = new GameSession(level.getRows(), level.getCols());
        session.setDifficultyLevel(3);
        session.setZombieBreachesEnabled(false);
        session.setLevel(level);
        session.startWaves();

        EndlessRun run = session.getEndlessRun();
        assertNotNull(run, "an endless level carries a live run");
        assertEquals(0, run.getScore());

        for (int tick = 0; tick < 4000; tick++) {
            session.tick();
            for (Zombie zombie : new ArrayList<>(session.getZombies())) {
                zombie.takeDamage(zombie.getHp() + zombie.getMaxHp(), null);
            }
        }

        assertTrue(run.getKills() > 0, "zombies were killed");
        assertTrue(run.getScore() > 0, "kills are worth Meow Points");
        assertTrue(run.getCombo() >= run.getKills(), "the combo counts every kill");
    }

    @Test
    void skySunFallsMoreOftenInAnEndlessRunThanInAPlainMatchOfTheSameChapter() throws Exception {
        Level authored = LevelLoader.loadLevelById(101);
        GameSession plain = new GameSession(authored.getRows(), authored.getCols());
        plain.setDifficultyLevel(3);
        plain.setLevel(authored);
        plain.startWaves();

        EndlessLevel lottery = lotteryFor(EndlessChapter.EGYPT);
        GameSession endless = new GameSession(lottery.getRows(), lottery.getCols());
        endless.setDifficultyLevel(3);
        endless.setLevel(lottery);
        endless.startWaves();

        assertEquals(1.0, plain.getSkySunIntervalMultiplier(), 1e-9);
        assertTrue(endless.getSkySunIntervalMultiplier() < 1.0,
                "a shorter sky sun interval means more sun");
        assertTrue(endless.getSkySunIntervalMultiplier() >= 0.5,
                "only slightly more, not a sun fountain");
    }

    @Test
    void everyChapterKeepsItsOwnRecordPerAccount() {
        UserState state = new UserState(new ArrayList<>(), 0, 0, 0);
        for (EndlessChapter chapter : EndlessChapter.values()) {
            assertFalse(state.hasLotteryScore(chapter.key()),
                    "a chapter never played has no record at all");
        }

        assertTrue(state.recordLotteryScore(EndlessChapter.EGYPT.key(), 1200));
        assertTrue(state.recordLotteryScore(EndlessChapter.DARK_AGES.key(), 400));

        assertEquals(1200, state.getLotteryHighScore(EndlessChapter.EGYPT.key()));
        assertEquals(400, state.getLotteryHighScore(EndlessChapter.DARK_AGES.key()));
        assertFalse(state.hasLotteryScore(EndlessChapter.BIG_WAVE_BEACH.key()),
                "one chapter's run does not fill in another's");

        assertFalse(state.recordLotteryScore(EndlessChapter.EGYPT.key(), 900),
                "a worse run never lowers a record");
        assertEquals(1200, state.getLotteryHighScore(EndlessChapter.EGYPT.key()));

        assertEquals(1200, state.bonusHighScore,
                "My Point follows the best endless run across every chapter");
        assertTrue(state.recordLotteryScore(EndlessChapter.BIG_WAVE_BEACH.key(), 5000));
        assertEquals(5000, state.bonusHighScore);
    }

    @Test
    void aRunningScoreCannotOverflow() {
        EndlessRun run = new EndlessRun();
        assertTrue(EndlessRun.scoreMultiplierAt(Double.MAX_VALUE)
                <= EndlessRun.MAX_SCORE_MULTIPLIER, "the difficulty factor is capped");

        Zombie zombie = ZombieFactory.create("ZombieDefault", 0, 8);
        for (int kill = 0; kill < 5000; kill++) {
            run.onZombieSpawned(zombie, kill);
            assertTrue(run.onZombieKilled(zombie, kill + 1) > 0);
        }
        assertTrue(run.getScore() > 0, "the total stays positive, never wrapped");
        assertEquals(5000, run.getKills());
    }

    @Test
    void restartingAnEndlessLevelStartsTheScoreOver() {
        EndlessLevel level = lotteryFor(EndlessChapter.FROSTBITE_CAVES);
        GameSession first = new GameSession(level.getRows(), level.getCols());
        first.setLevel(level);
        first.startWaves();
        for (int tick = 0; tick < 1500; tick++) {
            first.tick();
            for (Zombie zombie : new ArrayList<>(first.getZombies())) {
                zombie.takeDamage(zombie.getHp() + zombie.getMaxHp(), null);
            }
        }
        assertTrue(first.getEndlessRun().getScore() > 0);

        GameSession second = new GameSession(level.getRows(), level.getCols());
        second.setLevel(level);
        assertEquals(0, second.getEndlessRun().getScore(),
                "a replay of the same Lottery node starts from zero");
        assertEquals(0, second.getEndlessRun().getKills());
    }

    @Test
    void theDirectorSurvivesAChapterWithNoRosterAtAll() {
        EndlessWaveDirector director = new EndlessWaveDirector(List.of(), 9, new Random(1));
        assertTrue(director.waveAt(0).getWaveZombies().isEmpty());
        assertTrue(director.delaySeconds(0) > 0);
    }
}
