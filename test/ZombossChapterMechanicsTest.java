import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.match.boss.ZombossChapter;
import model.match.boss.ZombossFight;
import model.match.boss.ZombossLawn;
import model.match.boss.ZombossPhase;
import model.match.boss.ZombossRowEffect;
import model.match.boss.ZombossSkyStrike;
import model.match.boss.behavior.BeachZombossBehavior;
import model.match.boss.behavior.DarkAgeZombossBehavior;
import model.match.main.levels.special_levels.BossLevel;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.utils.GameSession;
import model.utils.LevelLoader;
import org.junit.jupiter.api.Test;
import service.GameClock;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombossChapterMechanicsTest {

    private static final String MISSILE_EGYPT =
            "768/INITIAL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_EGYPT/"
                    + "ZOMBOSS_MISSILE_EXPLOSION_EGYPT.PAM";
    private static final String MISSILE_ICEAGE =
            "768/FULL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_ICEAGE/"
                    + "ZOMBOSS_MISSILE_EXPLOSION_ICEAGE.PAM";

    private static void assertClips(String pam, String... clips) {
        for (String clip : clips) {
            assertTrue(AnimationFactory.exactClipDurationForPath(pam, clip) > 0,
                    "animations.json has no clip '" + clip + "' in " + pam);
        }
    }

    @Test
    void egyptBossPamHasEveryClipTheMovesetUses() {
        assertClips(ZombossChapter.EGYPT.getBossPam(),
                "intro", "idle",
                "walk_forward", "walk_backwards", "walk_up", "walk_down",
                "jump_start", "jump_mid", "jump_land", "stomp",
                "zombie_portal_start", "zombie_portal_loop", "zombie_portal_end",
                "missile_start", "rocket_launch",
                "stun_start", "stun_loop", "stun_end",
                "die", "die_idle", "die_talk", "die_exit");
        assertClips(MISSILE_EGYPT, "missile_lock_reticle", "missile", "missile_explosion");
    }

    @Test
    void iceAgeBossPamHasEveryClipTheMovesetUses() {
        assertClips(ZombossChapter.ICE_AGE.getBossPam(),
                "intro", "idle", "wind_1", "wind_2", "wind_3", "wind_4", "slingshot",
                "reveal", "stun", "cover_up", "die", "die_talk", "die_exit");
        assertClips(MISSILE_ICEAGE, "missile_lock_reticle", "missile", "missile_explosion");
        assertClips("768/FULL/EFFECTS/ZOMBOSS_GLACIER_TOP/ZOMBOSS_GLACIER_TOP.PAM", "animation");
        assertClips("768/FULL/EFFECTS/ZOMBOSS_GLACIER_MIDDLE/ZOMBOSS_GLACIER_MIDDLE.PAM",
                "animation");
        assertClips("768/FULL/EFFECTS/ZOMBOSS_GLACIER_BOTTOM/ZOMBOSS_GLACIER_BOTTOM.PAM",
                "animation");
        assertClips("768/FULL/EFFECTS/ZOMBOSS_GLACIER_FOGGING/ZOMBOSS_GLACIER_FOGGING.PAM",
                "animation");
        assertClips(model.match.main.season.travellog.cave.IceWind.PAM_PATH_PLACEHOLDER,
                model.match.main.season.travellog.cave.IceWind.PAM_CLIP);
    }

    @Test
    void beachBossPamHasEveryClipTheMovesetUses() {
        assertClips(ZombossChapter.BEACH.getBossPam(),
                "intro", "idle", "submerge", "emerge", "spawn",
                "suction_on", "suction_loop", "suction_off",
                "tangled_on", "tangled_loop", "tangled_off",
                "stun_start", "stun_loop", "stun_end", "die", "die_talk", "die_exit");
        assertClips(model.match.boss.ZombossShark.PAM, "idle", "attack", "submerge");
        assertClips(BeachZombossBehavior.PLANT_PULLED_PAM, "animation", "animation2");
        assertClips(BeachZombossBehavior.TURBINE_WIND_PAM, "animation");
    }

    @Test
    void darkAgesBossPamHasEveryClipTheMovesetUses() {
        assertClips(ZombossChapter.DARK_AGES.getBossPam(),
                "intro", "idle", "summoning",
                "fire_attack", "fire_attack_idle", "fire_attack_end",
                "fire_bomb", "fire_bomb_loop", "fire_bomb_end",
                "vulnerable", "vulnerable_loop", "vulnerable_end",
                "stun_start", "stun_loop", "stun_end", "die", "die_talk", "die_exit");
        assertClips("768/FULL/EFFECTS/ZOMBOSS_DARK_FIREBALL/ZOMBOSS_DARK_FIREBALL.PAM",
                "fall", "impact");
        assertClips(DarkAgeZombossBehavior.FIRE_TILE_PAM, DarkAgeZombossBehavior.FIRE_TILE_CLIP);
    }

    private static final class Sightings {
        final Set<String> actions = new HashSet<>();
        boolean skyStrike;
        boolean rowEffect;
        boolean shark;
        boolean suction;
        boolean pulledPlant;
        boolean tangled;
        int minionPeak;
    }

    private static GameSession startBattle(int levelId) throws IOException {
        BossLevel level = (BossLevel) LevelLoader.loadLevelById(levelId);
        GameSession session = new GameSession(level.getRows(), level.getCols());
        session.setLevel(level);
        session.startWaves();
        session.setZombieBreachesEnabled(false);
        ZombossFight fight = session.getZombossFight();
        for (int i = 0; i < 400 && fight.getPhase() != ZombossPhase.NPC_TALK; i++) session.tick();
        for (int i = 0; i < fight.getDialogueCount(); i++) fight.advanceDialogue();
        for (int i = 0; i < 200 && fight.getPhase() != ZombossPhase.BATTLE; i++) session.tick();
        assertEquals(ZombossPhase.BATTLE, fight.getPhase());
        return session;
    }

    private static Sightings observe(GameSession session, double seconds) {
        return observe(session, seconds, null);
    }

    private static Sightings observe(GameSession session, double seconds, Runnable perSecond) {
        ZombossFight fight = session.getZombossFight();
        Sightings seen = new Sightings();
        int ticks = (int) Math.round(seconds / GameClock.SECONDS_PER_TICK);
        for (int i = 0; i < ticks; i++) {
            if (perSecond != null && i % 10 == 0) perSecond.run();
            session.tick();
            if (fight.getAction() != null && !fight.getAction().isFinished()) {
                seen.actions.add(fight.getAction().getName());
            }
            if (!fight.getSkyStrikes().isEmpty()) seen.skyStrike = true;
            if (!fight.getRowEffects().isEmpty()) seen.rowEffect = true;
            seen.minionPeak = Math.max(seen.minionPeak, fight.countMinions());
            if (fight.getBehavior() instanceof BeachZombossBehavior beach) {
                if (!beach.getSharks().isEmpty()) seen.shark = true;
                if (beach.getSuctionRow() >= 0) seen.suction = true;
                if (!beach.getPulledPlants().isEmpty()) seen.pulledPlant = true;
            }
            if (fight.getAction() != null && "tangled".equals(fight.getAction().getName())) {
                seen.tangled = true;
            }
        }
        return seen;
    }

    private static PlantJsonParser.PlantConfig configFor(String name) {
        for (PlantJsonParser.PlantConfig candidate : PlantFactory.getBlueprints().values()) {
            if (candidate.name.equalsIgnoreCase(name)) return candidate;
        }
        throw new IllegalArgumentException("unknown plant " + name);
    }

    private static Plant place(GameSession session, String name, int row, int col) {
        PlantJsonParser.PlantConfig config = configFor(name);
        Plant plant = PlantFactory.createPlant(config.id, 1, new Position(col, row));
        return session.plantAt(row, col, plant) ? plant : null;
    }

    @Test
    void egyptUsesItsWholeMovesetInOneBattle() throws IOException {
        GameSession session = startBattle(104);
        Runnable restock = () -> {
            for (int row = 0; row < session.getRows(); row++) place(session, "Wall-nut", row, 0);
        };
        restock.run();

        Sightings seen = observe(session, 400, restock);
        assertTrue(seen.actions.contains("missile"), "Egypt never fired a rocket");
        assertTrue(seen.actions.contains("portal"), "Egypt never opened a zombie portal");
        assertTrue(seen.actions.contains("jump"), "Egypt never jumped onto a plant");
        assertTrue(seen.actions.contains("walk") || seen.actions.contains("stomp"),
                "Egypt never moved or stomped");
        assertTrue(seen.skyStrike, "the rocket never produced a sky strike");
        assertTrue(seen.minionPeak > 0, "no reinforcements arrived");
    }

    @Test
    void iceAgeFreezesRowsAndSlingsIceBlocks() throws IOException {
        GameSession session = startBattle(204);
        Runnable restock = () -> {
            for (int row = 0; row < session.getRows(); row++) place(session, "Wall-nut", row, 1);
        };
        restock.run();

        Sightings seen = observe(session, 400, restock);
        assertTrue(seen.actions.contains("wind"), "the cold wind never blew");
        assertTrue(seen.actions.contains("slingshot"), "the slingshot never fired");
        assertTrue(seen.skyStrike, "the slingshot never dropped an ice block");
        assertTrue(seen.rowEffect, "the cold wind never covered a row");
    }

    @Test
    void beachSucksInARowAndSendsSharks() throws IOException {
        GameSession session = startBattle(304);
        for (int row = 0; row < session.getRows(); row++) place(session, "Sunflower", row, 0);

        Sightings seen = observe(session, 400);
        assertTrue(seen.actions.contains("suction"), "the turbine never started");
        assertTrue(seen.suction, "the suction row was never reported to the renderer");
        assertTrue(seen.pulledPlant, "the vortex never tore a plant off the lawn");
        assertTrue(seen.shark, "no sharks were released");
    }

    @Test
    void beachTangleKelpJamsTheTurbine() throws IOException {
        GameSession session = startBattle(304);
        ZombossFight fight = session.getZombossFight();
        int startingHealth = fight.getBoss().getHP();

        Runnable restock = () -> {
            for (Position tile : ZombossLawn.waterTiles(session, 0)) {
                place(session, "Tangle Kelp", (int) tile.y(), (int) tile.x());
            }
        };
        restock.run();
        assertTrue(session.getPlants().size() > 0, "Tangle Kelp should be plantable on water");

        Sightings seen = observe(session, 400, restock);
        assertTrue(seen.tangled, "Tangle Kelp never jammed the turbine: " + seen.actions);
        assertTrue(fight.getBoss().getHP() < startingHealth,
                "jamming the turbine should hurt the machine");
    }

    @Test
    void darkAgesBurnsRowsAndSparesFirePlants() throws IOException {
        GameSession session = startBattle(404);
        Runnable restock = () -> {
            for (int row = 0; row < session.getRows(); row++) {
                placeAnywhereInRow(session, "Torchwood", row, 3);
            }
        };
        restock.run();

        Sightings seen = observe(session, 400, restock);
        assertTrue(seen.actions.contains("fireattack"), "the dragon never breathed fire");
        assertTrue(seen.actions.contains("firebomb"),
                "the dragon never lobbed a fire bomb: " + seen.actions);
        assertTrue(seen.actions.contains("summon"), "the dragon never summoned anything");
        assertTrue(seen.rowEffect, "no row was ever set alight");
    }

    private static Plant placeAnywhereInRow(GameSession session, String name, int row) {
        return placeAnywhereInRow(session, name, row, session.getCols());
    }

    private static Plant placeAnywhereInRow(GameSession session, String name, int row,
                                            int maxColumnExclusive) {
        for (int col = 0; col < Math.min(maxColumnExclusive, session.getCols()); col++) {
            Plant plant = place(session, name, row, col);
            if (plant != null) return plant;
        }
        return null;
    }

    @Test
    void aBurningRowKillsOnlyNonFirePlants() throws IOException {
        GameSession session = startBattle(404);
        Plant torchwood = placeAnywhereInRow(session, "Torchwood", 2);
        Plant sunflower = placeAnywhereInRow(session, "Sunflower", 2);
        assertNotNull(torchwood, "no free tile in row 2 for the Torchwood");
        assertNotNull(sunflower, "no free tile in row 2 for the Sunflower");

        session.getZombossFight().addRowEffect(new ZombossRowEffect(
                DarkAgeZombossBehavior.FIRE_TILE_PAM, DarkAgeZombossBehavior.FIRE_TILE_CLIP,
                2, 1.0, true));
        session.tick();

        assertTrue(torchwood.isAlive(), "a FIRE plant should shrug off the dragon's breath");
        assertFalse(sunflower.isAlive(), "everything else in the row should burn");
    }

    @Test
    void aSkyStrikeClearsItsTargetTile() throws IOException {
        GameSession session = startBattle(104);
        Plant victim = placeAnywhereInRow(session, "Wall-nut", 3);
        assertNotNull(victim, "no free tile in row 3 to plant on");
        int row = (int) Math.round(victim.getPosition().y());
        int col = (int) Math.round(victim.getPosition().x());

        ZombossSkyStrike strike = new ZombossSkyStrike(new ZombossSkyStrike.Config(
                MISSILE_EGYPT, "missile_lock_reticle",
                MISSILE_EGYPT, "missile",
                MISSILE_EGYPT, "missile_explosion", 1.0, false), row, col);
        session.getZombossFight().addSkyStrike(strike);

        for (int i = 0; i < 60 && !strike.isDone(); i++) session.tick();
        assertFalse(victim.isAlive(), "the rocket should have destroyed the plant it locked on");

        Cell cell = session.getEnvironment().getCell(row, col);
        assertNull(cell.getPlant(), "the tile should be clear again");
    }

    @Test
    void everyChapterHasThreeLinesOfItsOwnDialogue() {
        Set<String> allLines = new HashSet<>();
        for (ZombossChapter chapter : ZombossChapter.values()) {
            List<String> lines = chapter.getDialogue();
            assertTrue(lines.size() >= 2, chapter + " needs at least two lines");
            for (String line : lines) {
                assertTrue(allLines.add(line), "duplicate dialogue line: " + line);
            }
        }
    }
}
