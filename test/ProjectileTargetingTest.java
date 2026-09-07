import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match_mechanisms.vector.Position;
import model.pitches.obstacles.Grave;
import model.pitches.obstacles.IceBlock;
import model.pitches.obstacles.OctopusWrap;
import model.pitches.obstacles.PushableType;
import model.utils.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectileTargetingTest {

    private static final int ROWS = 5;
    private static final int COLS = 9;

    private GameSession session;

    @BeforeEach
    void setUp() {
        session = new GameSession(ROWS, COLS);
        GameSession.setCurrent(session);
        session.setZombieBreachesEnabled(false);
        session.setLawnMowersEnabled(false);
    }

    private Plant plant(String name, int row, int col) {
        Plant plant = PlantFactory.createPlantByName(name, 1, new Position(col, row));
        assertTrue(session.plantAt(row, col, plant), name + " should be plantable");
        return plant;
    }

    private Grave grave(int row, int col) {
        Grave grave = new Grave();
        session.getEnvironment().getCell(row, col).setObstacle(grave);
        return grave;
    }

    private OctopusWrap wrap(Plant victim, int row, int col, int hp) {
        OctopusWrap wrap = new OctopusWrap(victim, hp);
        session.getEnvironment().getCell(row, col).setObstacle(wrap);
        victim.setState(Plant.PlantState.INCAPACITATED);
        return wrap;
    }

    private PushableStructure structure(PushableType type, int row, int col) {
        PushableStructure structure = new PushableStructure(type, new Position(col, row));
        session.registerStructure(structure);
        return structure;
    }

    private Zombie zombie(int row, double col, int hp) {
        Zombie zombie = ZombieFactory.create("ZombieDefault", row, (int) Math.round(col));
        zombie.setPosition(new Position(col, row));
        zombie.setSpeed(Position.ShowZero());
        zombie.setHP(hp);
        session.getZombies().add(zombie);
        return zombie;
    }

    private void tickUntil(java.util.function.BooleanSupplier done, int maxTicks, String message) {
        for (int i = 0; i < maxTicks && !done.getAsBoolean(); i++) session.tick();
        assertTrue(done.getAsBoolean(), message);
    }

    @Test
    void aPeashooterBreaksAGraveWithNotASingleZombieOnTheLawn() {
        plant("Peashooter", 2, 0);
        Grave grave = grave(2, 5);

        assertTrue(session.getZombies().isEmpty());
        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 60,
                "the Peashooter stayed idle even though a grave was standing in its row");
    }

    @Test
    void aCactusStrikesAGraveWithNoZombiesOnTheLawn() {
        plant("Cactus", 2, 0);
        Grave grave = grave(2, 5);

        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 60,
                "Cactus never fired at the grave in its lane");
    }

    @Test
    void aCabbagePultLobsAtAGraveWithNoZombiesOnTheLawn() {
        plant("Cabbage-pult", 2, 0);
        Grave grave = grave(2, 5);

        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 120,
                "Cabbage-pult never came down on the grave in its lane");
    }

    @Test
    void aCatTailHomesOntoAGraveWithNoZombiesOnTheLawn() {
        plant("Cat-tail", 2, 0);
        Grave grave = grave(3, 5);

        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 120,
                "Cat-tail never went for the only destructible target on the lawn");
    }

    @Test
    void aBowlingBulbRollsAtAGraveWithNoZombiesOnTheLawn() {
        plant("Bowling Bulb", 2, 0);
        Grave grave = grave(2, 5);

        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 120,
                "Bowling Bulb never rolled at the grave in its lane");
    }

    @Test
    void rotobagaShootsTheGraveInTheRowBehindItWithNoZombiesOnTheLawn() {
        plant("Rotobaga", 2, 4);
        Grave behind = grave(1, 1);

        tickUntil(() -> behind.getHp() < Grave.MAX_HP, 90,
                "Rotobaga's backward lane-shift shot never reached the grave behind it");
    }

    @Test
    void starfruitShootsAGraveSittingOnOneOfItsOffAxisArms() {
        plant("Starfruit", 2, 4);
        Grave above = grave(0, 4);

        tickUntil(() -> above.getHp() < Grave.MAX_HP, 90,
                "Starfruit never fired along the arm the grave was sitting on");
    }

    @Test
    void aPeashooterBreaksAnOctopusWrapAndFreesTheWrappedPlant() {
        plant("Peashooter", 2, 0);
        Plant victim = plant("Wall-nut", 2, 4);
        OctopusWrap wrap = wrap(victim, 2, 4, 40);

        tickUntil(wrap::isDead, 90, "the Peashooter never chewed through the octopus wrap");
        assertEquals(Plant.PlantState.ACTIVE, victim.getPlantState(),
                "breaking the wrap must release the plant it was holding");
        assertFalse(wrap.blocksPlanting(), "a broken wrap no longer holds its tile");
    }

    @Test
    void aBrokenOctopusWrapStopsBlockingLaterShots() {
        plant("Peashooter", 2, 0);
        Plant victim = plant("Wall-nut", 2, 3);
        OctopusWrap wrap = wrap(victim, 2, 3, 40);
        Grave behind = grave(2, 6);

        tickUntil(wrap::isDead, 90, "the wrap was never destroyed");
        tickUntil(() -> behind.getHp() < Grave.MAX_HP, 120,
                "shots kept stopping on the dead wrap instead of flying past it");
    }

    @Test
    void aPeashooterShootsAnIceBlockWithNoZombiesOnTheLawn() {
        plant("Peashooter", 2, 0);
        Plant frozen = plant("Wall-nut", 2, 4);
        frozen.setState(Plant.PlantState.INCAPACITATED);
        IceBlock ice = new IceBlock(frozen, 40);
        session.getEnvironment().getCell(2, 4).setObstacle(ice);

        tickUntil(() -> session.getEnvironment().getCell(2, 4).getObstacle() == null, 120,
                "the Peashooter never thawed the ice block in its lane");
        assertEquals(Plant.PlantState.ACTIVE, frozen.getPlantState(),
                "shattering the ice releases the plant frozen inside it");
    }

    @Test
    void aPeashooterFiresAtAStructureLeftBehindByItsZombie() {
        plant("Peashooter", 2, 0);
        PushableStructure barrel = structure(PushableType.BARREL, 2, 5);
        int before = barrel.getHp();

        tickUntil(() -> barrel.getHp() < before, 60,
                "the Peashooter ignored the barrel standing alone in its row");
    }

    @Test
    void aLobberFiresAtAStructureLeftBehindByItsZombie() {
        plant("Cabbage-pult", 2, 0);
        PushableStructure barrel = structure(PushableType.BARREL, 2, 5);
        int before = barrel.getHp();

        tickUntil(() -> barrel.getHp() < before, 120,
                "the Cabbage-pult ignored the barrel standing alone in its row");
    }

    @Test
    void aCatTailPlantFoodBarrageStillHitsAGraveWithNoZombies() {
        Plant catTail = plant("Cat-tail", 2, 0);
        Grave grave = grave(2, 5);

        catTail.activatePlant(session);
        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 120,
                "the Cat-tail Plant Food barrage fizzled with a grave still standing");
    }

    @Test
    void aPeashooterPlantFoodBarrageStillHitsAGraveWithNoZombies() {
        Plant peashooter = plant("Peashooter", 2, 0);
        Grave grave = grave(2, 5);

        peashooter.activatePlant(session);
        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 120,
                "the Peashooter Plant Food barrage fizzled with a grave still standing");
    }

    @Test
    void aShooterWithNothingToShootAtStaysIdle() {
        plant("Peashooter", 2, 0);

        for (int i = 0; i < 60; i++) session.tick();
        assertTrue(session.getProjectiles().isEmpty(),
                "an empty lawn must not make plants fire at nothing");
    }

    @Test
    void aShooterIgnoresAGraveOutsideItsFiringArc() {
        plant("Peashooter", 2, 8);
        grave(2, 1);

        for (int i = 0; i < 60; i++) session.tick();
        assertTrue(session.getProjectiles().isEmpty(),
                "a Peashooter only fires forward, never at a grave behind it");
    }

    @Test
    void aLobWhoseZombieDiedMidFlightStillBreaksWhatItLandsOn() {
        Plant pult = plant("Cabbage-pult", 2, 0);
        Zombie target = zombie(2, 4.0, 100000);

        tickUntil(() -> !session.getProjectiles().isEmpty(), 60, "the lobber never fired");
        tickUntil(() -> pult.getIntervalTimer() > 0, 5, "the lobber never went on cooldown");

        target.setAlive(false);
        session.getZombies().remove(target);
        Grave grave = grave(2, 4);

        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 25,
                "the cabbage landed on a grave and did nothing to it");
        assertTrue(pult.getIntervalTimer() > 0,
                "the damage must come from the shot already in the air, not a fresh one");
    }

    @Test
    void aGrapeshotBlastBreaksTheGravesNextToIt() {
        Grave grave = grave(2, 2);
        plant("Grapeshot", 2, 1);

        tickUntil(() -> grave.getHp() < Grave.MAX_HP, 120,
                "the Grapeshot blast left the grave beside it untouched");
    }

    @Test
    void aDeadWrapIsNeitherATargetNorABlocker() {
        Plant victim = plant("Wall-nut", 2, 4);
        OctopusWrap wrap = wrap(victim, 2, 4, 1);
        wrap.takeDamage(5);
        assertTrue(wrap.isDead());

        plant("Peashooter", 2, 0);
        for (int i = 0; i < 60; i++) session.tick();
        assertTrue(session.getProjectiles().isEmpty(),
                "a wrap that has already been broken is not something to shoot at");
        assertNull(session.getEnvironment().getCell(2, 0).getObstacle());
    }
}
