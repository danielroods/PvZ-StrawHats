import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.collections.animations.AnimationFactory;
import model.collections.animations.ZombieAnimationRegistry;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.collections.zombie.zombie_move.JetpackFlyMove;
import model.match_mechanisms.vector.Position;
import model.projectile.zombie_projectile.FutureGargantuarBeamProjectile;
import model.projectile.zombie_projectile.GargantuarImpProjectile;
import model.projectile.zombie_projectile.ZombieProjectile;
import model.utils.GameSession;
import model.utils.ResourceResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FutureChapterZombiesTest {

    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final int ROW = 2;

    private static final String PAM_BASIC =
            "768/FULL/ZOMBIE/ZOMBIE_FUTURE_BASIC/ZOMBIE_FUTURE_BASIC.PAM";
    private static final String PAM_BASIC_BRICK =
            "768/FULL/ZOMBIE/ZOMBIE_FUTURE_BASIC_BRICK/ZOMBIE_FUTURE_BASIC_BRICK.PAM";
    private static final String PAM_FLAG =
            "768/FULL/ZOMBIE/ZOMBIE_FUTURE_FLAG/ZOMBIE_FUTURE_FLAG.PAM";
    private static final String PAM_PROTECTOR =
            "768/FULL/ZOMBIE/ZOMBIE_FUTURE_PROTECTOR/ZOMBIE_FUTURE_PROTECTOR.PAM";
    private static final String PAM_JETPACK =
            "768/FULL/ZOMBIE/ZOMBIE_FUTURE_JETPACK/ZOMBIE_FUTURE_JETPACK.PAM";
    private static final String PAM_JETPACK_BASIC =
            "768/FULL/ZOMBIE/ZOMBIE_FUTURE_JETPACK_BASIC/ZOMBIE_FUTURE_JETPACK_BASIC.PAM";
    private static final String PAM_JETPACK_VETERAN =
            "768/FULL/ZOMBIE/ZOMBIE_FUTURE_JETPACK_VETERAN/ZOMBIE_FUTURE_JETPACK_VETERAN.PAM";
    private static final String PAM_MECH_CONE =
            "768/FULL/ZOMBIE/ZOMBIE_MECH_CONE/ZOMBIE_MECH_CONE.PAM";
    private static final String PAM_MECH_FOOTBALL =
            "768/FULL/ZOMBIE/ZOMBIE_MECH_FOOTBALL/ZOMBIE_MECH_FOOTBALL.PAM";
    private static final String PAM_GARGANTUAR =
            "768/FULL/ZOMBIE/GARGANTUAR/GARGANTUAR.PAM";
    private static final String PAM_IMP =
            "768/FULL/ZOMBIE/GARGANTUAR_IMP/GARGANTUAR_IMP.PAM";
    private static final String PAM_SHIELD =
            "768/FULL/EFFECTS/MOONFLOWER_PF_SHIELD/MOONFLOWER_PF_SHIELD.PAM";
    private static final String PAM_LASER_BEAM =
            "768/FULL/EFFECTS/ZOMBIE_FUTURE_GARGANTUAR_BEAM/ZOMBIE_FUTURE_GARGANTUAR_BEAM.PAM";
    private static final String PAM_LASER_BASE =
            "768/FULL/EFFECTS/ZOMBIE_FUTURE_GARGANTUAR_BASE/ZOMBIE_FUTURE_GARGANTUAR_BASE.PAM";
    private static final String PAM_DIRT_SPAWN =
            "768/FULL/EFFECTS/DIRT_SPAWN_FUTURE/DIRT_SPAWN_FUTURE.PAM";
    private static final String PAM_MOWER =
            "768/FULL/MOWERS/MOWER_FUTURE/MOWER_FUTURE.PAM";
    private static final String PAM_LASER_SCORCH =
            "768/FULL/EFFECTS/ZOMBIE_FUTURE_GARGANTUAR_SCORCH/ZOMBIE_FUTURE_GARGANTUAR_SCORCH.PAM";

    private GameSession session;

    @BeforeEach
    void setUp() {
        session = new GameSession(ROWS, COLS);
        ZombieFactory.init();
    }

    private Zombie spawn(String alias, double col) {
        Zombie zombie = ZombieFactory.create(alias, ROW, (int) Math.round(col));
        zombie.setPosition(new Position(col, ROW));
        session.getZombies().add(zombie);
        return zombie;
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) session.tick();
    }

    private static void assertClips(String pam, String... clips) {
        for (String clip : clips) {
            assertTrue(AnimationFactory.exactClipDurationForPath(pam, clip) > 0,
                    "animations.json has no clip '" + clip + "' in " + pam);
        }
    }

    

    @Test
    void everyFutureAliasResolvesToTheAuthoredPam() {
        assertEquals(PAM_BASIC, ZombieAnimationRegistry.pathFor("ZombieFutureBasic"));
        assertEquals(PAM_FLAG, ZombieAnimationRegistry.pathFor("ZombieFutureFlag"));
        assertEquals(PAM_PROTECTOR, ZombieAnimationRegistry.pathFor("ZombieFutureProtector"));
        assertEquals(PAM_JETPACK, ZombieAnimationRegistry.pathFor("ZombieFutureJetpack"));
        assertEquals(PAM_JETPACK_BASIC, ZombieAnimationRegistry.pathFor("ZombieFutureJetpackBasic"));
        assertEquals(PAM_JETPACK_VETERAN,
                ZombieAnimationRegistry.pathFor("ZombieFutureJetpackVeteran"));
        assertEquals(PAM_MECH_CONE, ZombieAnimationRegistry.pathFor("ZombieMechCone"));
        assertEquals(PAM_MECH_FOOTBALL, ZombieAnimationRegistry.pathFor("ZombieMechFootball"));
        
        assertEquals(PAM_GARGANTUAR, ZombieAnimationRegistry.pathFor("ZombieFutureGargantuar"));
        assertEquals(PAM_IMP, ZombieAnimationRegistry.pathFor("ZombieFutureImp"));
    }

    @Test
    void genericAliasesPickUpFutureArtInsideTheFutureSeason() {
        assertEquals(PAM_BASIC, ZombieAnimationRegistry.pathFor("ZombieDefault", "Future"));
        assertEquals(PAM_BASIC, ZombieAnimationRegistry.pathFor("ZombieArmor1", "Future"));
        assertEquals(PAM_BASIC, ZombieAnimationRegistry.pathFor("ZombieArmor2", "Future"));
        
        assertEquals(PAM_BASIC_BRICK, ZombieAnimationRegistry.pathFor("ZombieArmor4", "Future"));
        assertEquals(PAM_FLAG, ZombieAnimationRegistry.pathFor("ZombieFlag", "Future"));
        assertEquals(PAM_IMP, ZombieAnimationRegistry.pathFor("ZombieImp", "Future"));
        assertEquals(PAM_GARGANTUAR, ZombieAnimationRegistry.pathFor("ZombieGargantuar", "Future"));

        
        assertEquals("768/FULL/ZOMBIE/ZOMBIE_PIRATE_BASIC/ZOMBIE_PIRATE_BASIC.PAM",
                ZombieAnimationRegistry.pathFor("ZombieDefault", "Pirates"));
        assertEquals("768/FULL/ZOMBIE/ZOMBIE_DARK_BASIC/ZOMBIE_DARK_BASIC.PAM",
                ZombieAnimationRegistry.pathFor("ZombieDefault", "Dark Ages"));
    }

    @Test
    void everyClipTheFutureBehavioursAskForExists() {
        assertClips(PAM_BASIC, "idle", "walk", "eat", "die", "particles");
        assertClips(PAM_FLAG, "idle", "walk", "eat", "die", "particles");

        assertClips(PAM_PROTECTOR, "idle", "walk", "eat", "die",
                "stun_start", "stun_idle", "stun_end",
                "shield_start", "shield_idle", "shield_end");

        for (String jetpack : List.of(PAM_JETPACK, PAM_JETPACK_BASIC, PAM_JETPACK_VETERAN)) {
            assertClips(jetpack, "idle", "walk", "eat", "die", "particles",
                    "enter", "fly_up", "up_idle", "fly_down");
        }

        assertClips(PAM_MECH_CONE, "idle", "walk", "eat", "die",
                "stun_start", "stun_idle", "stun_end");
        assertClips(PAM_MECH_FOOTBALL, "idle", "walk", "eat", "die",
                "stun_start", "stun_idle", "stun_end");

        assertClips(PAM_GARGANTUAR, "idle", "walk", "eat", "die",
                "stun_start", "stun_idle", "stun_end",
                "laser_start", "laser_idle", "laser_end",
                "fire", "cannon_fire", "smash_left", "smash_right");

        assertClips(PAM_IMP, "idle", "walk", "eat", "die",
                "stun_start", "stun_idle", "stun_end",
                "fall", "drop", "impact", "idle_ball2", "transition");

        assertClips(PAM_SHIELD, "plantfood_shieldON", "plantfood_shieldIdle",
                "plantfood_shieldIdle_damage1", "plantfood_shieldIdle_damage2");
        assertClips(PAM_LASER_BEAM, "laser_beam");
        assertClips(PAM_LASER_SCORCH, "laser_hit");

        
        assertClips(PAM_LASER_BASE, "laser_start", "laser_idle", "laser_end");
        assertClips(PAM_DIRT_SPAWN, "tomb_dirt_anim");
        assertClips(PAM_MOWER, "idle", "transition", "attack");
    }

    @Test
    void theGargantuarChestEmitterMirrorsTheBodysLaserBeats() {
        
        
        for (String clip : List.of("laser_start", "laser_idle", "laser_end")) {
            assertTrue(AnimationFactory.hasExactClip(PAM_GARGANTUAR, clip));
            assertTrue(AnimationFactory.hasExactClip(PAM_LASER_BASE, clip),
                    "the chest emitter is missing the body's '" + clip + "' beat");
        }
    }

    @Test
    void pamsWithoutAParticlesClipAreNotAskedForOne() {
        
        
        
        for (String pam : List.of(PAM_PROTECTOR, PAM_MECH_CONE, PAM_MECH_FOOTBALL, PAM_IMP)) {
            assertFalse(AnimationFactory.hasExactClip(pam, "particles"),
                    pam + " unexpectedly has a particles clip");
        }
        assertTrue(AnimationFactory.hasExactClip(PAM_BASIC, "particles"));
        assertTrue(AnimationFactory.hasExactClip(PAM_JETPACK, "particles"));
    }

    @Test
    void exactClipLookupsSurviveAPamTheCatalogDoesNotList() {
        
        
        assertEquals(-1f,
                AnimationFactory.exactClipDurationForPath("768/NOT/A/REAL.PAM", "idle"));
        assertFalse(AnimationFactory.hasExactClip("768/NOT/A/REAL.PAM", "idle"));
        assertFalse(AnimationFactory.hasExactClip(null, "idle"));
    }

    

    @Test
    void jetpackVariantsDifferOnlyInToughness() {
        Zombie basic = spawn("ZombieFutureJetpackBasic", 8);
        Zombie standard = spawn("ZombieFutureJetpack", 8);
        Zombie veteran = spawn("ZombieFutureJetpackVeteran", 8);

        assertTrue(totalPool(basic) < totalPool(standard),
                "the basic jetpack is the frailest of the three");
        assertTrue(totalPool(standard) < totalPool(veteran),
                "the veteran jetpack is the toughest of the three");

        
        Zombie brick = ZombieFactory.create("ZombieArmor4", ROW, 8);
        assertNotNull(brick.getArmour(), "ZombieArmor4 should carry brick armour");
        assertNotNull(veteran.getArmour(), "the veteran carries the same brick plating");
        assertEquals(brick.getMaxHp() + brick.getArmour().getHP(),
                veteran.getMaxHp() + veteran.getArmour().getHP(),
                "the veteran jetpack should be as tough as an armour-4 zombie");

        Zombie cone = ZombieFactory.create("ZombieArmor1", ROW, 8);
        assertEquals(cone.getMaxHp() + cone.getArmour().getHP(),
                standard.getMaxHp() + standard.getArmour().getHP(),
                "the standard jetpack should be as tough as an armour-1 zombie");

        
        assertNull(basic.getArmour());

        for (Zombie jetpack : List.of(basic, standard, veteran)) {
            assertTrue(ZombieFactory.entersOnBoard(jetpack.getAlias()),
                    "jetpacks arrive on the board rather than off-screen");
        }
    }

    /** Everything that has to be chewed through to kill this zombie: health plus armour. */
    private static int totalPool(Zombie zombie) {
        return zombie.getMaxHp() + (zombie.getArmour() == null ? 0 : zombie.getArmour().getHP());
    }

    /** Ticks until the jetpack is off the ground, and reports how long that took. */
    private int tickUntilAirborne(Zombie jetpack) {
        for (int i = 1; i <= 200; i++) {
            session.tick();
            if (jetpack.isAirborne()) return i;
        }
        throw new AssertionError("the jetpack never lit its thrusters");
    }

    @Test
    void onlyTheJetpacksEnterOnTheBoard() {
        assertTrue(ZombieFactory.entersOnBoard("ZombieFutureJetpack"));
        assertFalse(ZombieFactory.entersOnBoard("ZombieFutureBasic"));
        assertFalse(ZombieFactory.entersOnBoard("ZombieDefault"));
        assertFalse(ZombieFactory.entersOnBoard("NoSuchZombie"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {601, 602, 603, 604})
    void everyFutureLevelLoadsAndRampsItsWaves(int levelId) throws Exception {
        
        
        
        model.match.main.levels.Level level = model.utils.LevelLoader.loadLevelById(levelId);
        assertNotNull(level, "level " + levelId + " should load");
        assertNotNull(level.getSeason());
        assertEquals("Future", level.getSeason().getName());

        List<model.match_mechanisms.ZombieWave> waves = level.getWaves();
        for (int i = 1; i < waves.size(); i++) {
            int previous = waves.get(i - 1).getWaveCost();
            int current = waves.get(i).getWaveCost();
            double required = waves.get(i).isFinalWave() ? 2.0 : 1.25;
            assertTrue(current + 0.001 >= previous * required,
                    "level " + levelId + " wave " + (i + 1) + " costs " + current
                            + ", needs at least " + required + "x " + previous);
        }
    }

    @Test
    void futureLevelsOnlyReferenceZombiesThatExist() throws Exception {
        Set<String> known = ZombieFactory.getAllZombieAliases();
        List<Map<String, Object>> levels;
        try (InputStream is = ResourceResolver.open("Levels.json")) {
            assertNotNull(is, "Levels.json should be on the classpath");
            Type listType = new TypeToken<List<Map<String, Object>>>() { }.getType();
            levels = new Gson().fromJson(new InputStreamReader(is), listType);
        }

        int futureLevels = 0;
        for (Map<String, Object> level : levels) {
            if (!"future".equals(level.get("season"))) continue;
            futureLevels++;

            @SuppressWarnings("unchecked")
            List<String> pool = (List<String>) level.get("zombiePool");
            assertNotNull(pool);
            for (String alias : pool) {
                assertTrue(known.contains(alias),
                        "unknown zombie '" + alias + "' in level " + level.get("name"));
                assertTrue(alias.contains("Future") || alias.contains("Mech"),
                        "level " + level.get("name") + " still lists a non-Future zombie: " + alias);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> waves = (List<Map<String, Object>>) level.get("waves");
            for (Map<String, Object> wave : waves) {
                @SuppressWarnings("unchecked")
                List<String> aliases = (List<String>) wave.get("zombies");
                for (String alias : aliases) {
                    assertTrue(pool.contains(alias),
                            "wave zombie '" + alias + "' is missing from the level's pool");
                }
            }

            @SuppressWarnings("unchecked")
            List<String> plants = (List<String>) level.get("availablePlants");
            assertTrue(plants.contains("Cactus") || plants.contains("Cabbage-pult")
                            || plants.contains("Melon-pult"),
                    "level " + level.get("name")
                            + " has no answer to an airborne jetpack zombie");
        }
        assertEquals(4, futureLevels, "the Future chapter should still have four levels");
    }

    

    @Test
    void aMechShortsOutAtHalfHealthAndThenWalksOnAgain() {
        Zombie mech = spawn("ZombieMechCone", 6);
        double startX = mech.getPosition().x();

        tick(5);
        assertTrue(mech.getPosition().x() < startX, "it walks normally at full health");
        assertFalse(mech.isStunTriggered());

        mech.setHP(mech.getMaxHp() / 2 - 1);
        tick(1);
        assertTrue(mech.isStunTriggered(), "half health should short it out");
        assertEquals("stun_start", mech.getActionAnimationState());

        double stunnedAt = mech.getPosition().x();
        tick(5);
        assertEquals(stunnedAt, mech.getPosition().x(), 1.0e-9,
                "a shorted-out zombie holds its ground");

        
        tick(60);
        assertTrue(mech.isStunCompleted(), "the mech should come back online");
        assertFalse(mech.isImmobilized(), "a mech recovers and keeps coming");
        assertNull(mech.getActionAnimationState());

        double recoveredAt = mech.getPosition().x();
        tick(10);
        assertTrue(mech.getPosition().x() < recoveredAt, "it walks on after recovering");
    }

    @Test
    void stunFiresOnceAndOnlyOnce() {
        Zombie mech = spawn("ZombieMechFootball", 6);
        mech.setHP(1);
        tick(120);
        assertTrue(mech.isStunCompleted());

        
        mech.setHP(1);
        tick(3);
        assertNull(mech.getActionAnimationState());
    }

    @Test
    void aStunnedZombieDoesNotBite() {
        Plant wallnut = PlantFactory.createPlantByName("Wall-nut", 1, new Position(5, ROW));
        assertTrue(session.plantAt(ROW, 5, wallnut));

        Zombie mech = spawn("ZombieMechCone", 5);
        mech.setHP(mech.getMaxHp() / 2 - 1);
        tick(1);
        assertTrue(mech.isStunTriggered());

        int hpBefore = wallnut.getHP();
        tick(10);
        assertEquals(hpBefore, wallnut.getHP(),
                "a zombie mid-stun must not be chewing on the plant in front of it");
    }

    

    @Test
    void theProtectorNeverMovesAgainOnceItShortsOut() {
        Zombie protector = spawn("ZombieFutureProtector", 7);
        protector.setHP(protector.getMaxHp() / 2 - 1);
        tick(1);
        assertEquals("stun_start", protector.getActionAnimationState());

        tick(60);
        assertTrue(protector.isStunCompleted());
        assertTrue(protector.isImmobilized(),
                "the Protector is rooted for the rest of its life");

        double restingAt = protector.getPosition().x();
        tick(40);
        assertEquals(restingAt, protector.getPosition().x(), 1.0e-9);
    }

    @Test
    void theProtectorShieldsANeighbourAndTheBubbleBreaksFirst() {
        Zombie protector = spawn("ZombieFutureProtector", 7);
        Zombie ward = spawn("ZombieFutureBasic", 5);

        protector.setHP(protector.getMaxHp() / 2 - 1);
        
        tick(200);

        assertTrue(protector.isStunCompleted());
        assertTrue(ward.hasShield(), "the Protector should have shielded its neighbour");
        assertTrue(ward.getShieldHp() > 0);
        assertEquals(1.0, ward.getShieldFraction(), 1.0e-9);

        int shield = ward.getShieldHp();
        int healthBefore = ward.getHP();

        ward.takeDamage(shield / 2, null);
        assertEquals(healthBefore, ward.getHP(),
                "the bubble has to be broken through before the zombie is touched");
        assertTrue(ward.getShieldHp() < shield);
        assertTrue(ward.getShieldHitFlash() > 0, "a soaked hit should flash the bubble");

        int remaining = ward.getShieldHp();
        ward.takeDamage(remaining + 40, null);
        assertFalse(ward.hasShield(), "the bubble should be spent");
        assertEquals(healthBefore - 40, ward.getHP(),
                "damage past the bubble carries through to the zombie");
    }

    @Test
    void theProtectorShieldsNobodyBeforeItShortsOut() {
        Zombie protector = spawn("ZombieFutureProtector", 7);
        Zombie ward = spawn("ZombieFutureBasic", 6);
        tick(120);
        assertFalse(protector.isStunTriggered());
        assertFalse(ward.hasShield(),
                "a healthy Protector is still just walking, not generating shields");
    }

    

    @Test
    void aJetpackDropsOntoItsColumnAndPlaysEnterBeforeWalking() {
        Zombie jetpack = spawn("ZombieFutureJetpack", COLS - 1);
        double entryX = jetpack.getPosition().x();

        tick(2);
        assertEquals(JetpackFlyMove.CLIP_ENTER, jetpack.getActionAnimationState());
        assertEquals(entryX, jetpack.getPosition().x(), 1.0e-9,
                "it holds its entry column while the enter clip plays");

        tick(40);
        assertNull(jetpack.getActionAnimationState(), "it walks normally once it has landed");
        assertTrue(jetpack.getPosition().x() < entryX);
        assertEquals(0.0, jetpack.getHoverHeight(), 1.0e-9);
    }

    @Test
    void anAirborneJetpackIsOnlyReachableByLobbersAndItsOwnAllowList() {
        Zombie jetpack = spawn("ZombieFutureJetpackBasic", 7);
        
        jetpack.setMoveBehavior(new JetpackFlyMove(0.0001, 1000.0, 5.0));

        tickUntilAirborne(jetpack);
        assertTrue(jetpack.getHoverHeight() > 0, "an airborne zombie is drawn above its lane");

        Plant peashooter = PlantFactory.createPlantByName("Peashooter", 1, new Position(1, ROW));
        Plant cactus = PlantFactory.createPlantByName("Cactus", 1, new Position(1, ROW));
        Plant blueberry =
                PlantFactory.createPlantByName("Electric Blueberry", 1, new Position(1, ROW));

        assertFalse(jetpack.acceptsAttackFrom(peashooter),
                "an ordinary shooter cannot reach a zombie flying over it");
        assertTrue(jetpack.acceptsAttackFrom(cactus), "Cactus shoots high enough");
        assertTrue(jetpack.acceptsAttackFrom(blueberry), "Electric Blueberry reaches it too");

        int hpBefore = jetpack.getHP();
        jetpack.takeDamage(50, peashooter);
        assertEquals(hpBefore, jetpack.getHP(), "the pea passes underneath");
        jetpack.takeDamage(50, cactus);
        assertEquals(hpBefore - 50, jetpack.getHP(), "the spike brings it down to size");
    }

    @Test
    void anAirborneJetpackFliesOverPlantsAndLandsAgain() {
        Plant wallnut = PlantFactory.createPlantByName("Wall-nut", 1, new Position(4, ROW));
        assertTrue(session.plantAt(ROW, 4, wallnut));

        Zombie jetpack = spawn("ZombieFutureJetpackBasic", 7);
        jetpack.setMoveBehavior(new JetpackFlyMove(0.0001, 1000.0, 2.0));

        tickUntilAirborne(jetpack);
        assertTrue(jetpack.isIgnoringTargetAcquisition(), "it cannot bite while it is up");
        int nutHpBefore = wallnut.getHP();
        double airborneAtX = jetpack.getPosition().x();

        
        boolean landed = false;
        for (int i = 0; i < 200 && !landed; i++) {
            session.tick();
            landed = !jetpack.isAirborne();
        }
        assertTrue(landed, "the jetpack never came back down");
        assertEquals(0.0, jetpack.getHoverHeight(), 1.0e-9);
        assertFalse(jetpack.isIgnoringTargetAcquisition());
        assertTrue(jetpack.getPosition().x() < airborneAtX, "it made ground while flying");
        assertEquals(nutHpBefore, wallnut.getHP(),
                "it flew over the wall-nut rather than stopping to eat it");
    }

    

    @Test
    void theGargantuarLasersThePlantInFrontOfIt() {
        Plant wallnut = PlantFactory.createPlantByName("Wall-nut", 1, new Position(2, ROW));
        assertTrue(session.plantAt(ROW, 2, wallnut));

        Zombie gargantuar = spawn("ZombieFutureGargantuar", 7);
        int nutHpBefore = wallnut.getHP();

        
        tick(140);

        assertTrue(wallnut.getHP() < nutHpBefore || !wallnut.isAlive(),
                "the laser should have burned the plant it locked onto");
        assertTrue(gargantuar.getHp() > gargantuar.getMaxHp() / 2,
                "this test must stay above the stun/imp threshold");
    }

    @Test
    void theLaserPlaysItsFullStartIdleEndTrioAndSpawnsABeam() {
        Plant wallnut = PlantFactory.createPlantByName("Wall-nut", 1, new Position(2, ROW));
        assertTrue(session.plantAt(ROW, 2, wallnut));
        Zombie gargantuar = spawn("ZombieFutureGargantuar", 7);

        List<String> seen = new ArrayList<>();
        boolean beamSeen = false;
        for (int i = 0; i < 160; i++) {
            session.tick();
            String state = gargantuar.getActionAnimationState();
            if (state != null && (seen.isEmpty() || !seen.get(seen.size() - 1).equals(state))) {
                seen.add(state);
            }
            for (ZombieProjectile projectile : session.getZombieProjectiles()) {
                if (projectile instanceof FutureGargantuarBeamProjectile) beamSeen = true;
            }
        }

        assertTrue(seen.containsAll(List.of("laser_start", "laser_idle", "laser_end")),
                "expected the full laser trio, saw " + seen);
        assertEquals(seen.indexOf("laser_start") + 1, seen.indexOf("laser_idle"),
                "laser_idle must follow laser_start directly, saw " + seen);
        assertEquals(seen.indexOf("laser_idle") + 1, seen.indexOf("laser_end"),
                "laser_end must follow laser_idle directly, saw " + seen);
        assertTrue(beamSeen, "the lit beam should exist as a drawable projectile");
    }

    @Test
    void theImpOnlyLeavesTheGargantuarsHandOnCannonFire() {
        Zombie gargantuar = spawn("ZombieFutureGargantuar", 7);
        gargantuar.setHP(gargantuar.getMaxHp() / 2 - 1);

        String stateWhenImpAppeared = null;
        boolean sawFireWithNoImp = false;
        for (int i = 0; i < 200 && stateWhenImpAppeared == null; i++) {
            session.tick();
            String state = gargantuar.getActionAnimationState();
            boolean impInFlight = session.getZombieProjectiles().stream()
                    .anyMatch(GargantuarImpProjectile.class::isInstance);
            if ("fire".equals(state) && !impInFlight) sawFireWithNoImp = true;
            if (impInFlight) stateWhenImpAppeared = state;
        }

        assertTrue(sawFireWithNoImp,
                "the gargantuar hefts the imp on 'fire' before it ever leaves its hand");
        assertEquals("cannon_fire", stateWhenImpAppeared,
                "the imp is only released once cannon_fire starts");
    }

    @Test
    void aThrownFutureImpUnfoldsFromABallBeforeItWalks() {
        Zombie imp = throwImpAndLand("ZombieFutureImp");
        assertNotNull(imp, "the thrown imp should have landed on the lawn");

        assertTrue(imp.isSequenceActive(), "it lands balled up, not walking");
        assertEquals(GargantuarImpProjectile.CLIP_IMPACT, imp.getActionAnimationState());

        double landedAt = imp.getPosition().x();
        tick(4);
        assertEquals(landedAt, imp.getPosition().x(), 1.0e-9,
                "it cannot move while it is still a ball");

        List<String> seen = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            session.tick();
            String state = imp.getActionAnimationState();
            if (state != null && (seen.isEmpty() || !seen.get(seen.size() - 1).equals(state))) {
                seen.add(state);
            }
        }
        assertTrue(seen.contains(GargantuarImpProjectile.CLIP_BALL_IDLE), "saw " + seen);
        assertTrue(seen.contains(GargantuarImpProjectile.CLIP_TRANSITION), "saw " + seen);
        assertFalse(imp.isSequenceActive(), "it should be a walking imp by now");
        assertTrue(imp.getPosition().x() < landedAt, "and it should be advancing");
    }

    @Test
    void impsFromOtherChaptersStillLandWalking() {
        
        
        Zombie imp = throwImpAndLand("ZombiePirateImp");
        assertNotNull(imp);
        assertFalse(imp.isSequenceActive());
        assertNull(imp.getActionAnimationState());
    }

    

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {601, 602, 603})
    void aWholeFutureLevelPlaysThroughWithoutBlowingUp(int levelId) throws Exception {
        PlantFactory.autoInit();
        model.match.main.levels.Level level = model.utils.LevelLoader.loadLevelById(levelId);

        session = new GameSession(level.getRows(), level.getCols());
        session.setLevel(level);
        session.setSkySunEnabled(false);
        session.setZombieBreachesEnabled(false);
        session.addSun(5000);
        
        
        
        for (int row = 0; row < level.getRows(); row++) {
            session.plantAt(row, 0,
                    PlantFactory.createPlantByName("Cabbage-pult", 1, new Position(0, row)));
            session.plantAt(row, 1,
                    PlantFactory.createPlantByName("Peashooter", 1, new Position(1, row)));
        }
        session.startWaves();

        
        
        
        
        Set<String> seen = new java.util.HashSet<>();
        double furthestRightJetpackSpawn = -1;

        for (int tick = 0; tick < 6000; tick++) {
            session.tick();
            for (Zombie zombie : session.getZombies()) {
                seen.add(zombie.getAlias());
                if (ZombieFactory.entersOnBoard(zombie.getAlias())
                        && zombie.getPosition() != null) {
                    furthestRightJetpackSpawn =
                            Math.max(furthestRightJetpackSpawn, zombie.getPosition().x());
                }
                if (zombie.hasShield()) {
                    assertTrue(zombie.getShieldHp() > 0 && zombie.getShieldFraction() > 0,
                            "a live shield must have strength left in it");
                }
                assertTrue(zombie.getHoverHeight() == 0 || zombie.getAlias().contains("Jetpack"),
                        zombie.getAlias() + " should never be off the ground");
            }
        }

        
        
        
        List<String> pool = level.getZombiePool();
        
        
        
        
        Set<String> engineSpawned = Set.of("ZombieDefault", "ZombieFlag");
        assertTrue(pool.containsAll(seen.stream()
                        .filter(alias -> !engineSpawned.contains(alias)).toList()),
                "level " + levelId + " spawned something outside its pool: " + seen);

        int wavesSpawned = session.getWavesSpawnedCount();
        assertTrue(wavesSpawned >= 2,
                "level " + levelId + " only got " + wavesSpawned + " wave(s) away");
        
        
        for (int i = 0; i < wavesSpawned - 1 && i < level.getWaves().size(); i++) {
            for (Zombie template : level.getWaves().get(i).getWaveZombies()) {
                assertTrue(seen.contains(template.getAlias()),
                        "level " + levelId + " wave " + (i + 1) + " never delivered "
                                + template.getAlias() + "; saw " + seen);
            }
        }

        assertTrue(furthestRightJetpackSpawn > 0
                        && furthestRightJetpackSpawn <= level.getCols() - 1 + 1.0e-9,
                "jetpacks must appear on the last board column, not off the right edge"
                        + " (furthest seen: " + furthestRightJetpackSpawn + ")");
    }

    private Zombie throwImpAndLand(String impAlias) {
        List<Zombie> before = new ArrayList<>(session.getZombies());
        session.addZombieProjectile(new GargantuarImpProjectile(
                new Position(7, ROW), new Position(2, ROW), 1.0, 250, ROW, impAlias, true, session));
        for (int i = 0; i < 12 && session.getZombies().size() == before.size(); i++) {
            session.tick();
        }
        for (Zombie zombie : session.getZombies()) {
            if (!before.contains(zombie)) return zombie;
        }
        return null;
    }
}
