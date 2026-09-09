import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.zombie.Zombie;
import model.match.boss.ZombossChapter;
import model.match.boss.ZombossFight;
import model.match.boss.ZombossPhase;
import model.match.main.levels.Level;
import model.match.main.levels.special_levels.BossLevel;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import model.utils.LevelLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import service.GameClock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombossFightTest {

    private static final int EGYPT = 104;
    private static final int ICE_AGE = 204;
    private static final int BEACH = 304;
    private static final int DARK_AGES = 404;

    private static BossLevel loadBossLevel(int id) throws java.io.IOException {
        Level level = LevelLoader.loadLevelById(id);
        assertNotNull(level, "level " + id + " should exist");
        assertTrue(level instanceof BossLevel, "level " + id + " should be a boss level");
        return (BossLevel) level;
    }

    private static GameSession startSession(BossLevel level) {
        GameSession session = new GameSession(level.getRows(), level.getCols());
        session.setLevel(level);
        session.startWaves();
        return session;
    }

    private static void tickSeconds(GameSession session, double seconds) {
        int ticks = (int) Math.round(seconds / GameClock.SECONDS_PER_TICK);
        for (int i = 0; i < ticks; i++) session.tick();
    }

    private static void tickUntilPhase(GameSession session, ZombossFight fight,
                                       ZombossPhase phase, double timeoutSeconds) {
        int ticks = (int) Math.round(timeoutSeconds / GameClock.SECONDS_PER_TICK);
        for (int i = 0; i < ticks && fight.getPhase() != phase; i++) session.tick();
        assertEquals(phase, fight.getPhase(),
                "fight never reached " + phase + " within " + timeoutSeconds + "s");
    }

    private static ZombossFight runIntro(GameSession session) {
        ZombossFight fight = session.getZombossFight();
        assertNotNull(fight, "boss levels must build a ZombossFight in initSpecial()");

        assertEquals(ZombossPhase.SILENCE, fight.getPhase());
        assertNull(fight.getBoss(), "nothing is on the lawn during the opening silence");
        assertTrue(session.getZombies().isEmpty(), "the opening silence spawns no zombies");

        tickSeconds(session, ZombossFight.SILENCE_SECONDS - 0.5);
        assertEquals(ZombossPhase.SILENCE, fight.getPhase(), "silence ends too early");

        tickUntilPhase(session, fight, ZombossPhase.BOSS_INTRO, 2.0);
        assertNotNull(fight.getBoss());
        assertTrue(fight.getBoss().isBoss());
        assertTrue(session.getZombies().contains(fight.getBoss()));

        tickUntilPhase(session, fight, ZombossPhase.NPC_ENTER, 20.0);
        tickUntilPhase(session, fight, ZombossPhase.NPC_TALK, 10.0);

        // The talk phase waits on the player, not on a timer.
        tickSeconds(session, 5.0);
        assertEquals(ZombossPhase.NPC_TALK, fight.getPhase(), "dialogue advanced by itself");
        assertNotNull(fight.getDialogueLine());

        for (int i = 0; i < fight.getDialogueCount(); i++) fight.advanceDialogue();
        assertEquals(ZombossPhase.NPC_EXIT, fight.getPhase());
        assertNull(fight.getDialogueLine(), "the box hides once the last line is read");

        tickUntilPhase(session, fight, ZombossPhase.BATTLE, 5.0);
        return fight;
    }

    private static void hammerBoss(GameSession session, ZombossFight fight, double downTo) {
        Zombie boss = fight.getBoss();
        int guard = 0;
        while (boss.isAlive() && boss.getHP() > boss.getMaxHp() * downTo && guard++ < 500) {
            boss.takeDamage(boss.getMaxHp(), null);
        }
        assertTrue(guard < 500, "could not damage the boss down to " + downTo);
    }

    private static void plantLoadout(GameSession session, String plantName, int count) {
        PlantJsonParser.PlantConfig config = null;
        for (PlantJsonParser.PlantConfig candidate : PlantFactory.getBlueprints().values()) {
            if (candidate.name.equalsIgnoreCase(plantName)) {
                config = candidate;
                break;
            }
        }
        assertNotNull(config, "unknown plant " + plantName);
        int placed = 0;
        for (int row = 0; row < session.getRows() && placed < count; row++) {
            Plant plant = PlantFactory.createPlant(config.id, 1, new Position(0, row));
            if (session.plantAt(row, 0, plant)) placed++;
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {EGYPT, ICE_AGE, BEACH, DARK_AGES})
    void everyChapterRunsFromSilenceToBattle(int levelId) throws java.io.IOException {
        BossLevel level = loadBossLevel(levelId);
        assertNotNull(level.getChapter(), "chapter must be resolved from Levels.json");
        assertTrue(level.getWaves().isEmpty(), "boss levels carry no scripted waves");

        GameSession session = startSession(level);
        ZombossFight fight = runIntro(session);

        assertFalse(level.checkWinCondition(session), "cannot be won before the boss dies");
        assertFalse(session.isGameWon());
    }

    @ParameterizedTest
    @ValueSource(ints = {EGYPT, ICE_AGE, BEACH, DARK_AGES})
    void chaptersSurviveALongBattleAndSpawnFromTheirPool(int levelId) throws java.io.IOException {
        BossLevel level = loadBossLevel(levelId);
        GameSession session = startSession(level);
        ZombossFight fight = runIntro(session);

        plantLoadout(session, "Sunflower", 5);
        tickSeconds(session, 180.0);

        assertTrue(fight.isBattleActive(), "the battle should still be running");
        assertTrue(fight.countMinions() > 0, "reinforcements should keep arriving");
        for (Zombie zombie : session.getZombies()) {
            if (zombie == fight.getBoss()) continue;
            assertTrue(level.getZombiePool().contains(zombie.getAlias()),
                    zombie.getAlias() + " is not in this level's zombie pool");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {EGYPT, ICE_AGE, BEACH, DARK_AGES})
    void losingHealthOpensAStunWindow(int levelId) throws java.io.IOException {
        GameSession session = startSession(loadBossLevel(levelId));
        ZombossFight fight = runIntro(session);

        Zombie boss = fight.getBoss();
        assertFalse(fight.isStunned());
        assertTrue(boss.getHP() > boss.getMaxHp() * 0.8,
                "one hit must never take more than a slice of the boss's health");
        hammerBoss(session, fight, 0.6);
        session.tick();

        assertTrue(fight.isStunned(), "crossing a health threshold must trigger the stun");
        assertTrue(boss.getDamageTakenMultiplier() > 1.0, "the stun window doubles damage");

        int before = fight.countMinions();
        tickSeconds(session, 4.0);
        assertEquals(before, fight.countMinions(), "no reinforcements arrive while stunned");

        tickSeconds(session, 16.0);
        assertFalse(fight.isStunned(), "the boss should recover from the stun");
        assertEquals(1.0, boss.getDamageTakenMultiplier(), 0.001,
                "damage goes back to normal once the machine closes up again");
    }

    @ParameterizedTest
    @ValueSource(ints = {EGYPT, ICE_AGE, BEACH, DARK_AGES})
    void theStunWindowReopensAsTheBossIsWornDown(int levelId) throws java.io.IOException {
        GameSession session = startSession(loadBossLevel(levelId));
        ZombossFight fight = runIntro(session);

        int windows = 0;
        boolean wasStunned = false;
        for (double health : new double[] {0.7, 0.45, 0.2}) {
            hammerBoss(session, fight, health);
            for (int i = 0; i < 300 && fight.getBoss().isAlive(); i++) {
                session.tick();
                if (fight.isStunned() && !wasStunned) windows++;
                wasStunned = fight.isStunned();
                if (!fight.isStunned() && windows > 0 && i > 200) break;
            }
        }
        assertTrue(windows >= 3,
                "the machine should crack open more than once over a fight, saw " + windows);
    }

    @ParameterizedTest
    @ValueSource(ints = {EGYPT, ICE_AGE, BEACH, DARK_AGES})
    void killingTheBossPlaysTheDeathRunAndWinsTheMatch(int levelId) throws java.io.IOException {
        BossLevel level = loadBossLevel(levelId);
        GameSession session = startSession(level);
        ZombossFight fight = runIntro(session);

        hammerBoss(session, fight, 0.0);
        assertFalse(fight.getBoss().isAlive());
        session.tick();
        assertEquals(ZombossPhase.DEFEATED, fight.getPhase());
        assertFalse(session.isGameWon(), "the match is not over until the death run finishes");

        tickUntilPhase(session, fight, ZombossPhase.FINISHED, 40.0);
        session.tick();
        assertTrue(session.isGameWon(), "the match is won once Zomboss has left");
    }

    @Test
    void bossZombiesIgnoreCrowdControl() throws java.io.IOException {
        GameSession session = startSession(loadBossLevel(EGYPT));
        ZombossFight fight = runIntro(session);
        Zombie boss = fight.getBoss();

        boss.hypnotize();
        assertFalse(boss.isHypnotized(), "Zomboss cannot be hypnotised");

        boss.applyStatus(Zombie.Status.FROZEN, 5.0);
        assertEquals(Zombie.Status.NORMAL, boss.getStatus(), "Zomboss cannot be frozen");

        boss.applyStatus(Zombie.Status.BUTTER, 5.0);
        assertEquals(Zombie.Status.NORMAL, boss.getStatus(), "Zomboss cannot be buttered");

        double before = boss.getPosition().x();
        boss.startKnockback(-2.0, 0.5);
        session.tick();
        assertEquals(before, boss.getPosition().x(), 1.5,
                "Zomboss cannot be knocked back off its tile");
    }

    @Test
    void zombossLevelsDoubleTheSunRate() throws java.io.IOException {
        GameSession bossSession = startSession(loadBossLevel(EGYPT));
        assertTrue(bossSession.isDoubleSunRate());

        Level normal = LevelLoader.loadLevelById(101);
        GameSession normalSession = new GameSession(normal.getRows(), normal.getCols());
        normalSession.setLevel(normal);
        assertFalse(normalSession.isDoubleSunRate());
    }

    @Test
    void theIceGlacierSealsOffTheLastTwoColumns() throws java.io.IOException {
        BossLevel level = loadBossLevel(ICE_AGE);
        GameSession session = startSession(level);
        runIntro(session);

        PlantJsonParser.PlantConfig sunflower = null;
        for (PlantJsonParser.PlantConfig candidate : PlantFactory.getBlueprints().values()) {
            if (candidate.name.equalsIgnoreCase("Sunflower")) sunflower = candidate;
        }
        assertNotNull(sunflower);

        int cols = session.getCols();
        for (int row = 0; row < session.getRows(); row++) {
            for (int col = cols - 2; col < cols; col++) {
                assertFalse(session.plantAt(row, col,
                                PlantFactory.createPlant(sunflower.id, 1, new Position(col, row))),
                        "the glacier covers (" + row + "," + col + ")");
            }
        }
        boolean plantedSomewhere = false;
        for (int row = 0; row < session.getRows() && !plantedSomewhere; row++) {
            for (int col = 0; col < cols - 2 && !plantedSomewhere; col++) {
                plantedSomewhere = session.plantAt(row, col,
                        PlantFactory.createPlant(sunflower.id, 1, new Position(col, row)));
            }
        }
        assertTrue(plantedSomewhere, "the rest of the lawn is still plantable");
    }

    @Test
    void beachZombossOnlyEverStandsOnWater() throws java.io.IOException {
        BossLevel level = loadBossLevel(BEACH);
        GameSession session = startSession(level);
        ZombossFight fight = runIntro(session);

        for (int i = 0; i < 1800; i++) {
            session.tick();
            Position position = fight.getBossPosition();
            int row = (int) Math.round(position.y());
            int col = (int) Math.round(position.x());
            boolean water = model.match.boss.ZombossLawn.isWater(session, row, col);
            assertTrue(water, "beach Zomboss left the water at (" + row + "," + col + ")");
        }
    }

    @Test
    void everyChapterResolvesItsBossBlueprintAndClips() {
        for (ZombossChapter chapter : ZombossChapter.values()) {
            Zombie boss = model.collections.zombie.ZombieFactory.create(chapter.getAlias(), 0, 0);
            assertNotNull(boss, chapter + " has no Zombie.json blueprint");
            assertTrue(boss.getMaxHp() > 5000, chapter + " should have boss-sized health");

            String pam = chapter.getBossPam();
            assertClip(pam, ZombossChapter.INTRO_CLIP);
            assertClip(pam, ZombossChapter.IDLE_CLIP);
            assertClip(pam, chapter.getStunStartClip());
            assertClip(pam, chapter.getStunLoopClip());
            assertClip(pam, chapter.getStunEndClip());
            for (String clip : chapter.getDeathClips()) assertClip(pam, clip);
        }
        for (String clip : List.of(ZombossChapter.NPC_ENTER_CLIP, ZombossChapter.NPC_TALK_CLIP,
                ZombossChapter.NPC_EXIT_CLIP)) {
            assertClip(ZombossChapter.NPC_PAM, clip);
        }
    }

    private static void assertClip(String pamPath, String clip) {
        double length = model.collections.animations.AnimationFactory
                .exactClipDurationForPath(pamPath, clip);
        assertTrue(length > 0, "animations.json has no clip '" + clip + "' in " + pamPath);
    }
}
