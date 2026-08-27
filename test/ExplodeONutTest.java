import controller.assets.ProjectileEffectAssets;
import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantTag;
import model.collections.plant.PlantType;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplodeONutTest {

    private static final String EXPLODE_O_NUT_PAM = "768/INITIAL/PLANT/EXPLODEONUT/EXPLODEONUT.PAM";
    private static final String BLINK_PAM =
            "768/INITIAL/EFFECTS/EXPLODEONUT_BLINK/EXPLODEONUT_BLINK.PAM";
    private static final String EXPLOSION_BACK_PAM =
            "768/INITIAL/EFFECTS/GENERIC_EXPLOSION_BACK/GENERIC_EXPLOSION_BACK.PAM";
    private static final String EXPLOSION_FRONT_PAM =
            "768/INITIAL/EFFECTS/GENERIC_EXPLOSION_FRONT/GENERIC_EXPLOSION_FRONT.PAM";

    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final int ROW = 2;
    private static final int COL = 4;

    private GameSession session;

    @BeforeEach
    void setUp() {
        session = new GameSession(ROWS, COLS);
    }

    private Plant plantNut(int level) {
        Plant nut = PlantFactory.createPlantByName("Explode-o-nut", level, new Position(COL, ROW));
        assertTrue(session.plantAt(ROW, COL, nut), "Explode-o-nut should be plantable");
        return nut;
    }

    private Zombie spawnZombie(double col, int row, int hp) {
        Zombie zombie = ZombieFactory.create("ZombieDefault", row, (int) Math.round(col));
        zombie.setPosition(new Position(col, row));
        zombie.setHP(hp);
        session.getZombies().add(zombie);
        return zombie;
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) session.tick();
    }

    @Test
    void blueprintMatchesTheDataset() {
        Plant nut = plantNut(1);

        assertEquals(49, nut.getId());
        assertEquals(PlantType.WALL_NUT, nut.getType());
        assertEquals(50, nut.getCost());
        assertEquals(4000, nut.getMaxHp());
        assertEquals(4000, nut.getHP());
        assertEquals(1800, nut.getDamage());
        assertEquals(20, nut.getRecharge());
        assertTrue(nut.getTags().contains(PlantTag.EXPLOSIVE));
        assertTrue(nut.isExplodeONut());
        assertFalse(nut.isWallNut(), "it is its own plant, not the plain Wall-nut");
        assertNotNull(nut.getPlantFoodEffect());
        assertEquals(Plant.PlantState.ACTIVE, nut.getPlantState(),
                "a nut has no fuse to burn down, it is live the moment it lands");
    }

    @Test
    void everyClipTheRendererAsksForExistsInThePam() {
        String[] states = {
            "idle", "damage", "damage2", "damage3",
            "plantfood_on", "plantfood", "plantfood2", "plantfood3", "plantfood_off",
            "water",
        };
        for (String state : states) {
            assertTrue(AnimationFactory.exactClipDurationForPath(EXPLODE_O_NUT_PAM, state) > 0f,
                    "missing Explode-o-nut clip: " + state);
        }
        assertEquals(EXPLODE_O_NUT_PAM, AnimationFactory.pathForDisplayName("Explode-o-nut"));

        assertTrue(AnimationFactory.exactClipDurationForPath(BLINK_PAM, "animation") > 0f);
        assertTrue(AnimationFactory.exactClipDurationForPath(
                EXPLOSION_BACK_PAM, "animation2") > 0f);
        assertTrue(AnimationFactory.exactClipDurationForPath(
                EXPLOSION_FRONT_PAM, "animation2") > 0f);
    }

    @Test
    void theExplosionAssetsAreRegisteredForTheNut() {
        List<ProjectileEffectAssets.AssetEntry> hits = ProjectileEffectAssets.get(
                "Explode-o-nut", ProjectileEffectAssets.Kind.HIT,
                ProjectileEffectAssets.Variant.NORMAL);
        assertEquals(2, hits.size(), "the blast is drawn as a rear and a front layer");
        assertEquals(EXPLOSION_BACK_PAM, hits.get(0).path());
        assertEquals(EXPLOSION_FRONT_PAM, hits.get(1).path());

        List<ProjectileEffectAssets.AssetEntry> effects = ProjectileEffectAssets.get(
                "Explode-o-nut", ProjectileEffectAssets.Kind.EFFECT,
                ProjectileEffectAssets.Variant.NORMAL);
        assertEquals(1, effects.size());
        assertEquals(BLINK_PAM, effects.get(0).path());
    }

    @Test
    void damageTiersFollowTheRemainingHealth() {
        Plant nut = plantNut(1);

        assertEquals(0, nut.getExplodeONutDamageTier());
        assertEquals("idle", nut.getExplodeONutHealthAnimationState());

        nut.setHP(3000);
        assertEquals(1, nut.getExplodeONutDamageTier());
        assertEquals("damage", nut.getExplodeONutHealthAnimationState());

        nut.setHP(1600);
        assertEquals(2, nut.getExplodeONutDamageTier());
        assertEquals("damage2", nut.getExplodeONutHealthAnimationState());

        nut.setHP(400);
        assertEquals(3, nut.getExplodeONutDamageTier());
        assertEquals("damage3", nut.getExplodeONutHealthAnimationState());
    }

    @Test
    void itStandsInTheWayAndSoaksBitesWithoutFightingBack() {
        Plant nut = plantNut(1);
        Zombie zombie = spawnZombie(COL, ROW, 190);

        tick(20);

        assertTrue(nut.isAlive(), "a basic zombie cannot chew through 4000 HP in two seconds");
        assertTrue(nut.getHP() < nut.getMaxHp(), "but it is being eaten");
        assertEquals(190, zombie.getHP(), "the nut has no attack of its own while it is alive");
        assertFalse(nut.isExplodeONutDetonated());
    }

    @Test
    void beingDestroyedDetonatesForFullDamageOverTheThreeByThree() {
        Plant nut = plantNut(1);
        Zombie eater = spawnZombie(COL, ROW, 6000);
        Zombie diagonal = spawnZombie(COL + 1, ROW - 1, 6000);
        Zombie farAway = spawnZombie(COL + 2, ROW, 6000);

        nut.takeDamage(nut.getHP(), eater);

        assertFalse(nut.isAlive());
        assertTrue(nut.isExplodeONutDetonated());
        assertEquals(4200, eater.getHP(), "the zombie on the nut tile takes the blast");
        assertEquals(4200, diagonal.getHP(), "so does one a tile away diagonally");
        assertEquals(6000, farAway.getHP(), "two tiles away is outside the blast");
    }

    @Test
    void theBlastBurnsWhatItKills() {
        Plant nut = plantNut(1);
        Zombie zombie = spawnZombie(COL, ROW, 190);

        nut.takeDamage(nut.getHP(), zombie);

        assertFalse(zombie.isAlive());
        assertTrue(zombie.diedFromAsh(), "an explosion leaves ash, not a corpse");
    }

    @Test
    void itDetonatesExactlyOnce() {
        Plant nut = plantNut(1);
        Zombie zombie = spawnZombie(COL, ROW, 6000);

        nut.takeDamage(nut.getHP(), zombie);
        assertEquals(4200, zombie.getHP());

        nut.takeDamage(500, zombie);
        assertEquals(4200, zombie.getHP(), "a dead nut cannot blow up a second time");
    }

    @Test
    void diggingItUpDoesNotSetItOff() {
        Plant nut = plantNut(1);
        Zombie zombie = spawnZombie(COL, ROW, 6000);

        assertTrue(session.removePlantAt(ROW, COL));

        assertFalse(nut.isAlive());
        assertFalse(nut.isExplodeONutDetonated(), "the shovel defuses it, it does not trigger it");
        assertEquals(6000, zombie.getHP());
    }

    @Test
    void itKillsTheZombieThatAteIt() {
        Plant nut = plantNut(1);
        Zombie zombie = spawnZombie(COL, ROW, 1500);

        tick(600);

        assertFalse(nut.isAlive(), "the zombie eventually chews through the nut");
        assertTrue(nut.isExplodeONutDetonated());
        assertFalse(zombie.isAlive(), "and the blast takes it with it");
    }

    @Test
    void plantFoodHealsItAndBoltsOnTheShell() {
        Plant nut = plantNut(1);
        nut.setHP(500);
        assertEquals(3, nut.getExplodeONutDamageTier());

        assertTrue(nut.canUsePlantFood());
        assertTrue(nut.activatePlant(session));

        assertEquals(4000, nut.getHP(), "Plant Food restores the nut to full health");
        assertEquals(0, nut.getExplodeONutDamageTier());
        assertNotNull(nut.getArmor());
        assertEquals(8000, nut.getArmor().getHP());
        assertTrue(nut.isExplodeONutArmored());
        assertEquals(1, nut.getExplodeONutPlantFoodArmorStage());
        assertEquals("plantfood_on", nut.getVisualAnimationState());
        assertEquals(AnimationFactory.exactClipDurationForPath(EXPLODE_O_NUT_PAM, "plantfood_on"),
                nut.getVisualAnimationRemaining(), 0.01,
                "the one-shot intro lasts exactly as long as the clip");
    }

    @Test
    void armorStagesFollowTheRemainingArmorHealth() {
        Plant nut = plantNut(1);
        assertEquals(0, nut.getExplodeONutPlantFoodArmorStage());

        assertTrue(nut.activatePlant(session));
        assertEquals(1, nut.getExplodeONutPlantFoodArmorStage());

        nut.getArmor().setHP(4000);
        assertEquals(2, nut.getExplodeONutPlantFoodArmorStage());

        nut.getArmor().setHP(1000);
        assertEquals(3, nut.getExplodeONutPlantFoodArmorStage());
    }

    @Test
    void theShellSoaksDamageAndBreakingItDoesNotDetonateTheNut() {
        Plant nut = plantNut(1);
        Zombie zombie = spawnZombie(COL, ROW, 6000);
        assertTrue(nut.activatePlant(session));

        nut.takeDamage(3000, zombie);
        assertEquals(4000, nut.getHP(), "the shell soaks the bite");
        assertEquals(5000, nut.getArmor().getHP());
        assertEquals(6000, zombie.getHP(), "the shell does not hit back");

        nut.takeDamage(5000, zombie);
        assertNull(nut.getArmor(), "the shell is gone");
        assertTrue(nut.isAlive(), "but the nut underneath is untouched");
        assertEquals(4000, nut.getHP());
        assertFalse(nut.isExplodeONutDetonated(),
                "losing the shell is not the same as being destroyed");
        assertEquals(6000, zombie.getHP());

        nut.takeDamage(4000, zombie);
        assertFalse(nut.isAlive());
        assertTrue(nut.isExplodeONutDetonated(), "destroying the nut itself does set it off");
        assertEquals(4200, zombie.getHP());
    }

    @Test
    void levelUpgradesBuffTheNutAndTheBlast() {
        assertEquals(4000, plantNut(1).getMaxHp());

        session = new GameSession(ROWS, COLS);
        Plant levelTwo = plantNut(2);
        assertEquals(5000, levelTwo.getMaxHp(), "level 2 adds 1000 HP");
        assertEquals(1800, levelTwo.getDamage());

        session = new GameSession(ROWS, COLS);
        Plant levelThree = plantNut(3);
        assertEquals(2000, levelThree.getDamage(), "level 3 adds EXPLODE_DAMAGE_BUFF");

        session = new GameSession(ROWS, COLS);
        Plant levelFour = plantNut(4);
        assertEquals(25, levelFour.getCost(), "level 4 discounts the sun cost");
    }

    @Test
    void theUpgradedBlastActuallyHitsHarder() {
        Plant nut = plantNut(3);
        Zombie zombie = spawnZombie(COL, ROW, 6000);

        nut.takeDamage(nut.getHP(), zombie);

        assertEquals(4000, zombie.getHP(), "2000 damage at level 3");
    }
}
