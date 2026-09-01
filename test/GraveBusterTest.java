import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantType;
import model.collections.plant.actstrategy.GraveBusterStrategy;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Tile;
import model.pitches.TileType;
import model.pitches.obstacles.Grave;
import model.pitches.obstacles.IceBlock;
import model.utils.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraveBusterTest {

    private static final String GRAVE_BUSTER_PAM = "768/INITIAL/PLANT/GRAVEBUSTER/GRAVEBUSTER.PAM";

    private static final int ROWS = 5;
    private static final int COLS = 9;
    private static final int ROW = 2;
    private static final int COL = 4;

    private GameSession session;

    @BeforeEach
    void setUp() {
        PlantFactory.autoInit();
        ZombieFactory.init();
        session = new GameSession(ROWS, COLS);
        session.setSkySunEnabled(false);
        session.setZombieBreachesEnabled(false);
    }

    private Cell cell(int row, int col) {
        return session.getEnvironment().getCell(row, col);
    }

    private Grave placeGrave(int row, int col, Grave.Reward reward) {
        Grave grave = new Grave(reward);
        cell(row, col).setObstacle(grave);
        return grave;
    }

    private Plant newBuster(int level, int row, int col) {
        return PlantFactory.createPlantByName("Grave Buster", level, new Position(col, row));
    }

    private Plant plantBuster(int level, int row, int col) {
        Plant buster = newBuster(level, row, col);
        assertTrue(session.plantAt(row, col, buster), "Grave Buster should land on the grave");
        return buster;
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) session.tick();
    }

    private int ticksFor(double seconds) {
        return (int) Math.round(seconds / service.GameClock.SECONDS_PER_TICK);
    }

    private Zombie spawnZombie(int row, int col, int hp) {
        Zombie zombie = ZombieFactory.create("ZombieDefault", row, col);
        zombie.setPosition(new Position(col, row));
        zombie.setHP(hp);
        session.getZombies().add(zombie);
        return zombie;
    }

    @Test
    void blueprintMatchesTheDataset() {
        Plant buster = newBuster(1, ROW, COL);

        assertEquals(60, buster.getId());
        assertEquals(PlantType.EXPLOSIVE, buster.getType());
        assertTrue(buster.isGraveBuster());
        assertEquals(900, buster.getMaxHp(), "300 standard plant hp, raised by 200%");
        assertEquals(900, buster.getHP());
        assertEquals(3.0, buster.getActionInterval(), 1.0e-9);
        assertEquals(Plant.PlantState.ACTIVE, buster.getPlantState(),
                "it eats a grave, it does not burn a fuse");
        assertEquals(3.0, buster.getIntervalTimer(), 1.0e-9,
                "the interval is pre-loaded as the chew countdown");
        assertNull(buster.getPlantFoodEffect(), "Grave Buster takes no Plant Food");
        assertTrue(buster.getActStrategy() instanceof GraveBusterStrategy);
    }

    @Test
    void theLevelTwoUpgradeShortensTheChew() {
        assertEquals(2.0, newBuster(2, ROW, COL).getActionInterval(), 1.0e-9,
                "EAT_TIME_REDUCTION takes a second off the chew");
    }

    @Test
    void everyClipTheLifecycleUsesExistsInThePam() {
        assertEquals(GRAVE_BUSTER_PAM, AnimationFactory.pathForDisplayName("Grave Buster"));
        for (String state : new String[] {
                GraveBusterStrategy.CHEW_STATE,
                GraveBusterStrategy.FINISH_STATE,
                GraveBusterStrategy.WATER_STATE }) {
            assertTrue(AnimationFactory.exactClipDurationForPath(GRAVE_BUSTER_PAM, state) > 0f,
                    "GRAVEBUSTER.PAM must ship a '" + state + "' clip");
        }
    }


    @Test
    void itOnlyLandsOnAGrave() {
        placeGrave(ROW, COL, Grave.Reward.NONE);
        assertTrue(session.plantAt(ROW, COL, newBuster(1, ROW, COL)));
    }

    @Test
    void itIsRejectedOnBareLawn() {
        Plant buster = newBuster(1, ROW, COL);
        assertFalse(session.plantAt(ROW, COL, buster), "bare lawn is not a legal tile");
        assertFalse(session.getPlants().contains(buster));
        assertNull(cell(ROW, COL).getPlant());
    }

    @Test
    void itIsRejectedOnAnotherObstacle() {
        cell(ROW, COL).setObstacle(new IceBlock((Plant) null, IceBlock.BASE_HP));
        assertFalse(session.plantAt(ROW, COL, newBuster(1, ROW, COL)),
                "an ice block is not a grave");
        assertTrue(cell(ROW, COL).getObstacle() instanceof IceBlock, "the ice must survive");
    }

    @Test
    void itIsRejectedOnATileThatAlreadyHoldsAPlant() {
        Plant wallNut = PlantFactory.createPlantByName("Wall-nut", 1, new Position(COL, ROW));
        assertTrue(session.plantAt(ROW, COL, wallNut));

        Plant buster = newBuster(1, ROW, COL);
        assertFalse(session.plantAt(ROW, COL, buster));
        assertSame(wallNut, cell(ROW, COL).getPlant());
    }

    @Test
    void landingOnAGraveLeavesTheGraveStandingUntilTheChewFinishes() {
        Grave grave = placeGrave(ROW, COL, Grave.Reward.NONE);
        Plant buster = plantBuster(1, ROW, COL);

        assertSame(grave, cell(ROW, COL).getObstacle(),
                "the grave must survive the moment of planting - it is eaten, not deleted");
        assertSame(buster, cell(ROW, COL).getPlant());
        assertEquals(Grave.MAX_HP, grave.getHp());
    }


    @Test
    void itSurvivesTheWholeChewAndOnlyThenTakesTheGrave() {
        Grave grave = placeGrave(ROW, COL, Grave.Reward.NONE);
        Plant buster = plantBuster(1, ROW, COL);

        tick(5);
        assertTrue(buster.isAlive(), "half a second in it must still be chewing");
        assertSame(grave, cell(ROW, COL).getObstacle());

        tick(ticksFor(3.0) - 1 - 5);
        assertTrue(buster.isAlive(), "still alive on the last tick before the chew ends");
        assertSame(grave, cell(ROW, COL).getObstacle());

        tick(1);
        assertFalse(buster.isAlive(), "the chew is over, it leaves with the grave");
        assertNull(cell(ROW, COL).getObstacle(), "the grave is consumed");
        assertNull(cell(ROW, COL).getPlant());
        assertFalse(session.getPlants().contains(buster));
        assertTrue(buster.hasGraveBusterConsumedGrave());
    }

    @Test
    void aLevelTwoBusterFinishesAfterItsShorterChew() {
        placeGrave(ROW, COL, Grave.Reward.NONE);
        Plant buster = plantBuster(2, ROW, COL);

        tick(ticksFor(2.0) - 1);
        assertTrue(buster.isAlive());

        tick(1);
        assertFalse(buster.isAlive());
        assertNull(cell(ROW, COL).getObstacle());
    }


    @Test
    void theChewLoopsAndHandsOverToTheClosingBiteForItsFullLength() {
        placeGrave(ROW, COL, Grave.Reward.NONE);
        Plant buster = plantBuster(1, ROW, COL);

        float chewLength = AnimationFactory.exactClipDurationForPath(
                GRAVE_BUSTER_PAM, GraveBusterStrategy.CHEW_STATE);
        float finishLength = AnimationFactory.exactClipDurationForPath(
                GRAVE_BUSTER_PAM, GraveBusterStrategy.FINISH_STATE);

        assertEquals(GraveBusterStrategy.CHEW_STATE, buster.getVisualAnimationState(),
                "it is already chewing on the frame it lands");
        assertEquals(0.0, buster.getVisualAnimationElapsed(), 1.0e-9);

        int chewTicks = 0;
        int finishTicks = 0;
        int chewWraps = 0;
        double previousChewElapsed = -1.0;
        while (buster.isAlive()) {
            tick(1);
            String state = buster.getVisualAnimationState();
            double elapsed = buster.getVisualAnimationElapsed();

            if (GraveBusterStrategy.CHEW_STATE.equals(state)) {
                assertEquals(0, finishTicks, "the chew never comes back after the closing bite");
                assertTrue(elapsed < chewLength + 1.0e-6,
                        "the chew clip loops instead of running off its end");
                if (elapsed < previousChewElapsed) chewWraps++;
                previousChewElapsed = elapsed;
                chewTicks++;
            } else if (GraveBusterStrategy.FINISH_STATE.equals(state)) {
                assertTrue(elapsed < finishLength + 1.0e-6,
                        "the closing bite never runs past its own last frame");
                finishTicks++;
            }
        }

        assertTrue(chewWraps > 0, "a 3s chew must loop the 1s clip more than once");
        assertEquals(GraveBusterStrategy.FINISH_STATE, buster.getVisualAnimationState(),
                "it leaves on the closing bite, not mid-chew");
        assertEquals(0.0, buster.getVisualAnimationRemaining(), 1.0e-9,
                "the closing bite is held on its final frame as it goes");

        double finishSeconds = finishTicks * service.GameClock.SECONDS_PER_TICK;
        assertTrue(finishSeconds >= finishLength - service.GameClock.SECONDS_PER_TICK,
                "the closing bite gets its whole " + finishLength + "s, got " + finishSeconds);
        assertEquals(3.0, (chewTicks + finishTicks) * service.GameClock.SECONDS_PER_TICK,
                service.GameClock.SECONDS_PER_TICK,
                "chew plus closing bite is the full eat time");
    }

    @Test
    void aFloodedGraveUsesTheWaterClip() {
        cell(ROW, COL).setTile(new Tile(TileType.Water));
        placeGrave(ROW, COL, Grave.Reward.NONE);

        Plant buster = plantBuster(1, ROW, COL);
        tick(1);

        assertTrue(buster.isAlive(), "a Grave Buster clamped to a grave does not drown");
        assertEquals(GraveBusterStrategy.WATER_STATE, buster.getVisualAnimationState());
    }


    @Test
    void aSunGraveDropsItsSunWhenTheChewFinishes() {
        placeGrave(ROW, COL, Grave.Reward.SUN);
        plantBuster(1, ROW, COL);

        tick(ticksFor(3.0));

        assertTrue(session.getItems().stream()
                        .anyMatch(item -> item instanceof model.collections.item.GroundSun),
                "the grave's sun must be dropped by the Grave Buster, not swallowed with it");
    }

    @Test
    void aPlantFoodGraveDropsItsPlantFoodWhenTheChewFinishes() {
        placeGrave(ROW, COL, Grave.Reward.PLANT_FOOD);
        plantBuster(1, ROW, COL);

        tick(ticksFor(3.0));

        assertTrue(session.getItems().stream()
                        .anyMatch(item -> item instanceof model.collections.item.GroundPlantFood),
                "the grave's plant food must be dropped");
    }


    @Test
    void aZombieCanEatItOffTheGraveAndTheGraveSurvives() {
        Grave grave = placeGrave(ROW, COL, Grave.Reward.NONE);
        Plant buster = plantBuster(1, ROW, COL);

        buster.takeDamage(buster.getHP(), spawnZombie(ROW, COL, 200));

        assertFalse(buster.isAlive());
        assertFalse(buster.hasGraveBusterConsumedGrave(),
                "being eaten is not the same as finishing the job");

        tick(1);
        assertSame(grave, cell(ROW, COL).getObstacle(), "the grave must still be standing");
        assertEquals(Grave.MAX_HP, grave.getHp());
        assertNull(cell(ROW, COL).getPlant());
    }

    @Test
    void itTakesTheFullThreeHundredHitsItsHpIsWorth() {
        placeGrave(ROW, COL, Grave.Reward.NONE);
        Plant buster = plantBuster(1, ROW, COL);

        buster.takeDamage(899, spawnZombie(ROW, COL, 200));
        assertTrue(buster.isAlive(), "900 hp means 899 damage is survivable");
        assertEquals(1, buster.getHP());

        buster.takeDamage(1, spawnZombie(ROW, COL, 200));
        assertFalse(buster.isAlive());
    }

    @Test
    void aZombieOnItsTileTargetsTheGraveBusterItself() {
        placeGrave(ROW, COL, Grave.Reward.NONE);
        Plant buster = plantBuster(1, ROW, COL);

        Zombie zombie = spawnZombie(ROW, COL, 500);
        assertSame(buster,
                model.collections.zombie.zombie_attack.ZombieTargeting.findTarget(zombie, session),
                "a zombie standing on the grave eats the Grave Buster");
    }

    @Test
    void losingTheGraveMidChewEndsItInsteadOfStrandingItOnABareTile() {
        Grave grave = placeGrave(ROW, COL, Grave.Reward.NONE);
        Plant buster = plantBuster(1, ROW, COL);

        tick(5);
        assertTrue(buster.isAlive());

        session.damageGrave(cell(ROW, COL), Grave.MAX_HP);
        assertNull(cell(ROW, COL).getObstacle());

        tick(1);
        assertFalse(buster.isAlive(), "with no grave left there is nothing to chew");
        assertFalse(buster.hasGraveBusterConsumedGrave(), "it did not eat that grave itself");
        assertNull(buster.getVisualAnimationState());
    }

    @Test
    void shovellingItOffMidChewLeavesTheGraveIntact() {
        Grave grave = placeGrave(ROW, COL, Grave.Reward.NONE);
        Plant buster = plantBuster(1, ROW, COL);

        tick(5);
        assertTrue(session.removePlantAt(ROW, COL));

        assertFalse(buster.isAlive());
        assertSame(grave, cell(ROW, COL).getObstacle());
        assertEquals(Grave.MAX_HP, grave.getHp());
    }

    @Test
    void aSecondBusterCanFinishTheJobTheFirstOneStarted() {
        placeGrave(ROW, COL, Grave.Reward.NONE);
        Plant first = plantBuster(1, ROW, COL);

        tick(5);
        first.takeDamage(first.getHP(), spawnZombie(ROW, COL, 200));
        tick(1);

        Plant second = newBuster(1, ROW, COL);
        assertTrue(session.plantAt(ROW, COL, second), "the grave is free again");

        tick(ticksFor(3.0));
        assertFalse(second.isAlive());
        assertNull(cell(ROW, COL).getObstacle(), "the second one gets the grave");
    }

    @Test
    void aSpentGraveBusterIsNotCountedAsAPlantLost() {
        placeGrave(ROW, COL, Grave.Reward.NONE);
        plantBuster(1, ROW, COL);

        tick(ticksFor(3.0) + 1);

        assertEquals(0, session.getPlantsLostThisMatch(),
                "eating a grave is what it is for, not a loss to the zombies");
    }

    @Test
    void aGraveBusterEatenOffTheGraveDoesCountAsAPlantLost() {
        placeGrave(ROW, COL, Grave.Reward.NONE);
        Plant buster = plantBuster(1, ROW, COL);

        buster.takeDamage(buster.getHP(), spawnZombie(ROW, COL, 200));
        tick(1);

        assertEquals(1, session.getPlantsLostThisMatch());
    }

    @Test
    void itDoesNotDamageZombiesOnItsWayOut() {
        placeGrave(ROW, COL, Grave.Reward.NONE);
        plantBuster(1, ROW, COL);

        Zombie zombie = spawnZombie(ROW, COL, 500);
        zombie.setSpeed(new Position(0, 0));
        int hpBefore = zombie.getHP();

        tick(ticksFor(3.0) + 2);

        assertNotNull(zombie);
        assertEquals(hpBefore, zombie.getHP(),
                "Grave Buster is not a bomb - finishing a grave hurts nothing");
    }
}
