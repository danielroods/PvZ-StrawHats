import controller.assets.ProjectileEffectAssets;
import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.actstrategy.GrapeshotStrategy;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match_mechanisms.vector.Position;
import model.projectile.GrapeshotProjectile;
import model.projectile.Projectile;
import model.utils.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.GameClock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrapeshotTest {

    private static final String GRAPESHOT_PAM =
            "768/INITIAL/PLANT/GRAPESHOT/GRAPESHOT.PAM";
    private static final String GRAPESHOT_PROJECTILE_PAM =
            "768/INITIAL/EFFECTS/GRAPESHOT_PROJECTILE/GRAPESHOT_PROJECTILE.PAM";

    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final int FUSE_TICKS = 17;

    private GameSession session;

    @BeforeEach
    void setUp() {
        session = new GameSession(ROWS, COLS);
        Projectile.setGlobalSpeedMultiplier(0.60);
    }

    private Plant plantGrapeshot(int level, int row, int col) {
        Plant grapeshot = PlantFactory.createPlantByName("Grapeshot", level, new Position(col, row));
        assertTrue(session.plantAt(row, col, grapeshot), "Grapeshot should be plantable");
        return grapeshot;
    }

    private Zombie spawnZombie(int row, double col, int hp) {
        Zombie zombie = ZombieFactory.create("ZombieDefault", row, (int) Math.round(col));
        zombie.setPosition(new Position(col, row));
        zombie.setHP(hp);
        session.getZombies().add(zombie);
        return zombie;
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) session.tick();
    }

    private List<Projectile> grapes() {
        return session.getProjectiles();
    }

    @Test
    void armsWithTheAttackClipLengthAndDoesNotFireEarly() {
        Plant grapeshot = plantGrapeshot(1, 2, 4);

        assertEquals(Plant.PlantState.PREPPING, grapeshot.getPlantState());
        assertEquals(1.67, grapeshot.getIntervalTimer(), 1.0e-9);
        assertEquals(1.6667f,
                AnimationFactory.exactClipDurationForPath(GRAPESHOT_PAM, "attack_t2"), 0.01f,
                "fuse must match the attack_t2 clip the renderer plays");

        tick(FUSE_TICKS - 1);
        assertTrue(grapeshot.isAlive(), "Grapeshot must not detonate before its fuse ends");
        assertTrue(grapes().isEmpty(), "no grapes before detonation");

        tick(1);
        assertFalse(grapeshot.isAlive(), "Grapeshot detonates when the fuse reaches zero");
        assertEquals(8, grapes().size(), "detonation launches 8 grapes");
    }

    private void detonateWithoutTickingProjectiles(Plant grapeshot) {
        grapeshot.setInternalTimer(0.0);
        new GrapeshotStrategy().act(grapeshot, session);
    }

    @Test
    void blastDamagesTheThreeByThreeAreaOnly() {
        Plant grapeshot = plantGrapeshot(1, 2, 4);
        Zombie inside = spawnZombie(1, 5.0, 100000);
        Zombie outside = spawnZombie(4, 8.0, 100000);
        int insideHp = inside.getHP();
        int outsideHp = outside.getHP();

        detonateWithoutTickingProjectiles(grapeshot);

        assertEquals(insideHp - 1800, inside.getHP(), "3x3 blast deals the full plant damage");
        assertEquals(outsideHp, outside.getHP(), "zombies outside the 3x3 take no blast damage");
    }

    @Test
    void levelTwoBlastIsBuffed() {
        Plant grapeshot = plantGrapeshot(2, 2, 4);
        Zombie inside = spawnZombie(2, 5.0, 100000);
        int hp = inside.getHP();

        detonateWithoutTickingProjectiles(grapeshot);

        assertEquals(hp - 2400, inside.getHP(), "level 2 adds +600 blast damage");
    }

    @Test
    void grapesFlyOutwardInEightDistinctDirections() {
        plantGrapeshot(1, 2, 4);
        tick(FUSE_TICKS);

        List<Projectile> grapes = grapes();
        assertEquals(8, grapes.size());
        for (Projectile grape : grapes) {
            assertTrue(grape instanceof GrapeshotProjectile);
            assertNotNull(grape.getPosition());
            assertTrue(grape.isVisible(), "grapes appear immediately, with no spawn delay");
            assertNotNull(grape.getSpeed());
            assertTrue(grape.getSpeed().length() > 0.1, "every grape actually moves");
        }

        long distinctHeadings = grapes.stream()
                .map(p -> Math.round(Math.toDegrees(
                        Math.atan2(p.getSpeed().y(), p.getSpeed().x()))))
                .distinct()
                .count();
        assertEquals(8, distinctHeadings, "the 8 grapes spread over 8 different headings");
    }

    @Test
    void grapesDamageZombiesAndKeepGoingUntilTheirBounceBudgetRunsOut() {
        plantGrapeshot(1, 2, 0);
        Zombie tank = spawnZombie(2, 3.0, 1000000);

        tick(FUSE_TICKS);
        int afterBlast = tank.getHP();

        tick(30);

        assertTrue(tank.getHP() < afterBlast, "a grape hit the zombie in its path");
        int expectedGrapeDamage = 1800 / 6;
        assertEquals(0, (afterBlast - tank.getHP()) % expectedGrapeDamage,
                "grape shrapnel deals damage/6 per hit");
    }

    @Test
    void grapesBounceOffTheBoardEdgesInsteadOfLeavingTheLawn() {
        plantGrapeshot(1, 2, 4);
        tick(FUSE_TICKS);

        Projectile upward = grapes().stream()
                .filter(p -> p.getSpeed().y() < -0.1 && Math.abs(p.getSpeed().x()) < 0.1)
                .findFirst()
                .orElse(null);
        assertNotNull(upward, "one grape leaves straight along -y");

        for (int i = 0; i < 8 && upward.isAlive(); i++) {
            session.tick();
            assertTrue(upward.getPosition().y() >= 0.0,
                    "grape must never pass the top edge of the lawn");
            assertTrue(upward.getPosition().x() >= 0.0 && upward.getPosition().x() <= COLS - 1.0,
                    "grape must stay within the lawn horizontally");
        }
        assertTrue(upward.getSpeed().y() > 0, "hitting the top edge reverses the grape");
    }

    @Test
    void grapesSelfDestructWhenTheirLifetimeExpires() {
        plantGrapeshot(1, 2, 4);
        tick(FUSE_TICKS);
        assertFalse(grapes().isEmpty());

        tick((int) Math.round(GrapeshotStrategy.BASE_LIFETIME_SECONDS
                / GameClock.SECONDS_PER_TICK) + 2);

        assertTrue(grapes().isEmpty(),
                "no grape outlives the base lifetime, even with nothing to hit");
    }

    @Test
    void bounceUpgradeExtendsBouncesAndLifetime() {
        plantGrapeshot(3, 2, 4);
        tick(FUSE_TICKS);

        List<Projectile> grapes = grapes();
        assertEquals(8, grapes.size());
        for (Projectile grape : grapes) {
            GrapeshotProjectile shrapnel = (GrapeshotProjectile) grape;
            assertEquals(5, shrapnel.getRemainingBounces(),
                    "GRAPE_BOUNCE_EXT raises the bounce budget to 5");
            assertTrue(shrapnel.getRemainingLifetime() > GrapeshotStrategy.BASE_LIFETIME_SECONDS,
                    "GRAPE_BOUNCE_EXT extends the grape lifetime past the base lifetime");
        }
    }

    @Test
    void grapeshotTakesNoPlantFood() {
        Plant grapeshot = plantGrapeshot(1, 2, 4);

        assertNull(grapeshot.getPlantFoodEffect(),
                "Grapeshot is an instant-use plant and has no Plant Food ability");
        assertFalse(grapeshot.canUsePlantFood());
        assertFalse(grapeshot.activatePlant(session));
        assertFalse(grapeshot.isPlantFoodActive());
    }

    @Test
    void rendererResolvesTheExactClipsTheDesignCallsFor() {
        assertEquals(GRAPESHOT_PAM, AnimationFactory.pathForDisplayName("Grapeshot"));
        assertEquals("attack_t2",
                AnimationFactory.resolveClipNameForPath(GRAPESHOT_PAM, "attack_t2"),
                "the fuse clip must resolve to attack_t2, not fall back to attack");
        assertEquals("animation_forward",
                AnimationFactory.resolveClipNameForPath(GRAPESHOT_PROJECTILE_PAM,
                        "animation_forward"));
    }

    @Test
    void usesTheSpecifiedProjectileAndHitAssets() {
        List<ProjectileEffectAssets.AssetEntry> projectiles = ProjectileEffectAssets.get(
                "Grapeshot", ProjectileEffectAssets.Kind.PROJECTILE,
                ProjectileEffectAssets.Variant.NORMAL);
        assertEquals(1, projectiles.size());
        ProjectileEffectAssets.AssetEntry projectile = projectiles.get(0);
        assertEquals(GRAPESHOT_PROJECTILE_PAM, projectile.path());
        assertEquals("animation_forward", projectile.state());
        assertEquals(ProjectileEffectAssets.PlayMode.LOOP, projectile.playMode());
        assertEquals(0.1f,
                AnimationFactory.exactClipDurationForPath(GRAPESHOT_PROJECTILE_PAM,
                        "animation_forward"), 1.0e-4f);

        List<ProjectileEffectAssets.AssetEntry> hits = ProjectileEffectAssets.get(
                "Grapeshot", ProjectileEffectAssets.Kind.HIT,
                ProjectileEffectAssets.Variant.NORMAL);
        assertEquals(3, hits.size(), "three grape impact variants are registered");
        for (ProjectileEffectAssets.AssetEntry hit : hits) {
            assertTrue(hit.path().endsWith("GRAPESHOT_HIT.PAM"));
            assertEquals(ProjectileEffectAssets.PlayMode.ONCE, hit.playMode());
        }
    }
}
