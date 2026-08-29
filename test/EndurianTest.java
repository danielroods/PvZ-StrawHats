import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantType;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndurianTest {

    private static final String ENDURIAN_PAM = "768/FULL/PLANT/ENDURIAN/ENDURIAN.PAM";

    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final int ROW = 2;
    private static final int COL = 4;

    private GameSession session;

    @BeforeEach
    void setUp() {
        session = new GameSession(ROWS, COLS);
    }

    private Plant plantEndurian(int level) {
        Plant endurian = PlantFactory.createPlantByName("Endurian", level, new Position(COL, ROW));
        assertTrue(session.plantAt(ROW, COL, endurian), "Endurian should be plantable");
        return endurian;
    }

    private Zombie spawnZombie(double col, int hp) {
        Zombie zombie = ZombieFactory.create("ZombieDefault", ROW, (int) Math.round(col));
        zombie.setPosition(new Position(col, ROW));
        zombie.setHP(hp);
        session.getZombies().add(zombie);
        return zombie;
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) session.tick();
    }

    @Test
    void blueprintMatchesTheDataset() {
        Plant endurian = plantEndurian(1);

        assertEquals(46, endurian.getId());
        assertEquals(PlantType.WALL_NUT, endurian.getType());
        assertEquals(100, endurian.getCost());
        assertEquals(5000, endurian.getMaxHp());
        assertEquals(5000, endurian.getHP());
        assertEquals(20, endurian.getDamage());
        assertEquals(15, endurian.getRecharge());
        assertEquals(1.0, endurian.getActionInterval(), 1.0e-9,
                "the spike cadence is one hit per action interval, so it must not be 0");
        assertEquals(Plant.PlantState.ACTIVE, endurian.getPlantState());
        assertTrue(endurian.isEndurian());
        assertNotNull(endurian.getPlantFoodEffect());
    }

    @Test
    void everyClipTheRendererAsksForExistsInThePam() {
        String[] states = {
            "idle", "damage", "damage2", "damage3",
            "attack_start", "attack_loop", "attack_end",
            "attack_start_damage", "attack_loop_damage", "attack_end_damage",
            "attack_start_damage2", "attack_loop_damage2", "attack_end_damage2",
            "attack_start_damage3", "attack_loop_damage3", "attack_end_damage3",
            "plantfood_on", "water",
        };
        for (String state : states) {
            assertTrue(AnimationFactory.exactClipDurationForPath(ENDURIAN_PAM, state) > 0f,
                    "missing Endurian clip: " + state);
        }
        assertEquals(ENDURIAN_PAM, AnimationFactory.pathForDisplayName("Endurian"));
    }

    @Test
    void spikesHitTheChewingZombieOncePerActionInterval() {
        Plant endurian = plantEndurian(1);
        Zombie zombie = spawnZombie(COL, 190);

        tick(1);
        assertEquals(170, zombie.getHP(), "first bite is answered immediately by the spikes");

        tick(9);
        assertEquals(170, zombie.getHP(), "spikes stay on cooldown for the whole action interval");

        tick(1);
        assertEquals(150, zombie.getHP(), "spikes hit again once the action interval elapses");
    }

    @Test
    void spikesEventuallyKillTheZombieThatIsEatingIt() {
        Plant endurian = plantEndurian(1);
        Zombie zombie = spawnZombie(COL, 190);

        tick(91);
        assertFalse(zombie.isAlive(), "10 spike hits (200 damage) kill a 190 HP zombie");
        assertTrue(endurian.isAlive(), "Endurian outlasts a single basic zombie");
        assertTrue(endurian.getHP() < endurian.getMaxHp(), "it still took bite damage meanwhile");
    }

    @Test
    void aRangedAttackerIsNotSpiked() {
        Plant endurian = plantEndurian(1);
        Zombie zombie = spawnZombie(COL + 4, 190);

        endurian.takeDamage(50, zombie);

        assertEquals(190, zombie.getHP(), "only zombies in contact with Endurian take spike damage");
        assertFalse(endurian.isEndurianUnderAttack(), "no contact means no spike animation either");
        assertEquals(4950, endurian.getHP());
    }

    @Test
    void theUnderAttackFlagIsRaisedByABiteAndExpires() {
        Plant endurian = plantEndurian(1);
        Zombie zombie = spawnZombie(COL, 190);

        assertFalse(endurian.isEndurianUnderAttack());

        endurian.takeDamage(10, zombie);
        assertTrue(endurian.isEndurianUnderAttack());

        session.getZombies().clear();
        tick(2);
        assertTrue(endurian.isEndurianUnderAttack(), "the hold covers the gap between bites");

        tick(1);
        assertFalse(endurian.isEndurianUnderAttack(), "it lapses once nothing is chewing");
    }

    @Test
    void levelUpgradesBuffTheSpikesAndTheBody() {
        assertEquals(20, plantEndurian(1).getEndurianSpikeDamage());

        session = new GameSession(ROWS, COLS);
        Plant levelTwo = plantEndurian(2);
        assertEquals(25, levelTwo.getEndurianSpikeDamage(), "level 2 adds REFLECT_DAMAGE_BUFF");

        session = new GameSession(ROWS, COLS);
        Plant levelThree = plantEndurian(3);
        assertEquals(6000, levelThree.getMaxHp(), "level 3 adds 1000 HP");
        assertEquals(25, levelThree.getEndurianSpikeDamage());

        session = new GameSession(ROWS, COLS);
        Plant levelFour = plantEndurian(4);
        assertEquals(75, levelFour.getCost(), "level 4 discounts the sun cost");
    }

    @Test
    void damageTiersFollowTheRemainingHealth() {
        Plant endurian = plantEndurian(1);

        assertEquals(0, endurian.getEndurianDamageTier());
        assertEquals("idle", endurian.getEndurianHealthAnimationState());

        endurian.setHP(4000);
        assertEquals(1, endurian.getEndurianDamageTier());
        assertEquals("damage", endurian.getEndurianHealthAnimationState());

        endurian.setHP(2500);
        assertEquals(2, endurian.getEndurianDamageTier());
        assertEquals("damage2", endurian.getEndurianHealthAnimationState());

        endurian.setHP(1000);
        assertEquals(3, endurian.getEndurianDamageTier());
        assertEquals("damage3", endurian.getEndurianHealthAnimationState());
    }

    @Test
    void plantFoodHealsItGrantsTheSpikyArmorAndPlaysPlantfoodOn() {
        Plant endurian = plantEndurian(1);
        endurian.setHP(500);
        assertEquals(3, endurian.getEndurianDamageTier());

        assertTrue(endurian.canUsePlantFood());
        assertTrue(endurian.activatePlant(session));

        assertEquals(5000, endurian.getHP(), "Plant Food restores the nut to full health");
        assertEquals(0, endurian.getEndurianDamageTier());
        assertNotNull(endurian.getArmor());
        assertEquals(3000, endurian.getArmor().getHP());
        assertEquals(1, endurian.getEndurianPlantFoodArmorStage());
        assertEquals("plantfood_on", endurian.getVisualAnimationState());
        assertEquals(AnimationFactory.exactClipDurationForPath(ENDURIAN_PAM, "plantfood_on"),
                endurian.getVisualAnimationRemaining(), 0.01,
                "the one-shot visual lasts exactly as long as the clip");
    }

    @Test
    void plantFoodArmorAbsorbsDamageAndDoublesTheSpikes() {
        Plant endurian = plantEndurian(1);
        Zombie zombie = spawnZombie(COL, 500);
        assertTrue(endurian.activatePlant(session));

        assertEquals(40, endurian.getEndurianSpikeDamage(), "the spiky armor hits twice as hard");

        endurian.takeDamage(100, zombie);
        assertEquals(5000, endurian.getHP(), "the armor soaks the bite");
        assertEquals(2900, endurian.getArmor().getHP());
        assertEquals(460, zombie.getHP(), "the spikes still answer while armored");
    }

    @Test
    void armorStagesFollowTheRemainingArmorHealth() {
        Plant endurian = plantEndurian(1);
        assertEquals(0, endurian.getEndurianPlantFoodArmorStage());

        assertTrue(endurian.activatePlant(session));
        assertEquals(1, endurian.getEndurianPlantFoodArmorStage());

        endurian.getArmor().setHP(1000);
        assertEquals(2, endurian.getEndurianPlantFoodArmorStage());

        endurian.getArmor().setHP(500);
        assertEquals(3, endurian.getEndurianPlantFoodArmorStage());
    }

    @Test
    void theArmorIsDroppedOnceItIsChewedThroughAndTheBodyTakesOver() {
        Plant endurian = plantEndurian(1);
        Zombie zombie = spawnZombie(COL, 5000);
        assertTrue(endurian.activatePlant(session));
        endurian.getArmor().setHP(60);

        endurian.takeDamage(100, zombie);

        assertNull(endurian.getArmor(), "a spent Plant Food armor is discarded");
        assertEquals(4960, endurian.getHP(), "the overflow carries through to the nut");

        session.getZombies().clear();
        tick(30);
        assertEquals(20, endurian.getEndurianSpikeDamage(), "the spike bonus goes with the armor");
    }

    @Test
    void aFullyChewedEndurianDies() {
        Plant endurian = plantEndurian(1);
        Zombie zombie = spawnZombie(COL, 100000);

        endurian.takeDamage(5000, zombie);

        assertEquals(0, endurian.getHP());
        assertEquals(Plant.PlantState.DYING, endurian.getPlantState());
    }
}
