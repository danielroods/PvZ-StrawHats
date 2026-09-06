import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.collections.zombie.zombie_defense.ParasolDeflection;
import model.match_mechanisms.vector.Position;
import model.projectile.LaneShiftMove;
import model.projectile.LobArcMove;
import model.projectile.Projectile;
import model.projectile.ProjectileImpact;
import model.projectile.StraightMove;
import model.collections.plant.actstrategy.ShootStrategy;
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

class ProjectileSystemTest {

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

    private Zombie zombie(int row, double col, int hp) {
        Zombie zombie = ZombieFactory.create("ZombieDefault", row, (int) Math.round(col));
        zombie.setPosition(new Position(col, row));
        zombie.setHP(hp);
        session.getZombies().add(zombie);
        return zombie;
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) session.tick();
    }

    private Projectile firstFlyingShot(int maxTicks) {
        for (int i = 0; i < maxTicks; i++) {
            for (Projectile projectile : session.getProjectiles()) {
                if (projectile.isVisible()) return projectile;
            }
            session.tick();
        }
        return null;
    }


    @Test
    void aPeaAdvancesOneStepPerTickAndRemembersWhereItCameFrom() {
        plant("Peashooter", 2, 0);
        zombie(2, 8.0, 100000);

        Projectile pea = firstFlyingShot(30);
        assertNotNull(pea, "the Peashooter never fired");

        double before = pea.getPosition().x();
        session.tick();

        double step = ShootStrategy.SHOT_SPEED * GameClock.SECONDS_PER_TICK;
        assertEquals(before + step, pea.getPosition().x(), 1.0e-9,
                "a pea moves one shot-speed step per tick");
        assertEquals(before, pea.getPreviousPosition().x(), 1.0e-9,
                "the previous position is kept so the view can interpolate between ticks");
        assertTrue(step < 0.5, "a shot must not skip past a zombie's hit radius in one tick");
    }

    @Test
    void theShotSpeedMultiplierIsPerSessionAndDoesNotLeakBetweenMatches() {
        session.setProjectileSpeedMultiplier(0.5);
        assertEquals(1.8, session.projectileSpeed(ShootStrategy.SHOT_SPEED), 1.0e-9);

        GameSession other = new GameSession(ROWS, COLS);
        assertEquals(ShootStrategy.SHOT_SPEED, other.projectileSpeed(ShootStrategy.SHOT_SPEED),
                1.0e-9, "a fresh match starts at full speed, whatever the last one used");
        assertEquals(0.5, session.getProjectileSpeedMultiplier(), 1.0e-9);
    }

    @Test
    void aHitQueuesOneImpactWhereTheShotActuallyConnected() {
        plant("Peashooter", 2, 0);
        Zombie target = zombie(2, 4.0, 100000);
        target.setSpeed(Position.ShowZero());

        Projectile pea = firstFlyingShot(30);
        assertNotNull(pea);
        session.drainProjectileImpacts();

        int hp = target.getHP();
        for (int i = 0; i < 40 && target.getHP() == hp; i++) session.tick();
        assertTrue(target.getHP() < hp, "the pea never reached the zombie");

        List<ProjectileImpact> impacts = session.drainProjectileImpacts();
        assertEquals(1, impacts.size(), "one hit queues exactly one splat");
        ProjectileImpact impact = impacts.get(0);
        assertEquals("Peashooter", impact.plantName());
        assertFalse(impact.plantFood());
        assertEquals(4.0 - Projectile.PEA_IMPACT_ALIGNMENT_TILES, impact.position().x(), 1.0e-9,
                "the splat is placed on the zombie's body, not a tick behind it");
        assertEquals(2.0, impact.position().y(), 0.01);
    }

    @Test
    void everyPeaSplatsTheSameDistanceIntoTheZombieWhereverTheTickLands() {
        plant("Peashooter", 2, 0);
        plant("Peashooter", 3, 0);
        Zombie onTheTile = zombie(2, 4.0, 100000);
        Zombie betweenTiles = zombie(3, 4.17, 100000);
        onTheTile.setSpeed(Position.ShowZero());
        betweenTiles.setSpeed(Position.ShowZero());
        session.drainProjectileImpacts();

        int firstHp = onTheTile.getHP();
        int secondHp = betweenTiles.getHP();
        for (int i = 0; i < 60 && (onTheTile.getHP() == firstHp || betweenTiles.getHP() == secondHp); i++) {
            session.tick();
        }
        assertTrue(onTheTile.getHP() < firstHp && betweenTiles.getHP() < secondHp,
                "both peas should have connected");

        List<ProjectileImpact> impacts = session.drainProjectileImpacts();
        assertEquals(2, impacts.size());
        for (ProjectileImpact impact : impacts) {
            double zombieX = Math.abs(impact.position().y() - 2.0) < 0.5 ? 4.0 : 4.17;
            assertEquals(zombieX - Projectile.PEA_IMPACT_ALIGNMENT_TILES,
                    impact.position().x(), 1.0e-9,
                    "the splat sits on the body whatever sub-tile offset the shot stopped at");
        }
    }

    @Test
    void aNonPeaShooterKeepsItsOriginalImpactPoint() {
        plant("Puff-shroom", 2, 0);
        Zombie target = zombie(2, 3.0, 100000);
        target.setSpeed(Position.ShowZero());

        Projectile puff = firstFlyingShot(30);
        assertNotNull(puff, "Puff-shroom never fired");
        assertFalse(puff.isPeaShot(), "a Puff-shroom's spore is not a pea");
        session.drainProjectileImpacts();

        int hp = target.getHP();
        for (int i = 0; i < 40 && target.getHP() == hp; i++) session.tick();
        assertTrue(target.getHP() < hp, "the spore never reached the zombie");

        List<ProjectileImpact> impacts = session.drainProjectileImpacts();
        assertFalse(impacts.isEmpty());
        assertEquals(3.0, impacts.get(0).position().x(), 0.5,
                "only pea shooters are realigned, everything else lands where it always did");
    }

    @Test
    void aPlantFoodShotSplatsOnTheBodyLikeAnOrdinaryShot() {
        Plant peashooter = plant("Peashooter", 2, 0);
        Zombie target = zombie(2, 5.0, 100000);
        target.setSpeed(Position.ShowZero());

        peashooter.setPlantFoodTimer(5.0);
        peashooter.setInternalTimer(0);
        session.drainProjectileImpacts();

        int hp = target.getHP();
        for (int i = 0; i < 60 && target.getHP() == hp; i++) session.tick();
        assertTrue(target.getHP() < hp, "the Plant Food volley never reached the zombie");

        List<ProjectileImpact> impacts = session.drainProjectileImpacts();
        assertFalse(impacts.isEmpty(), "a Plant Food hit still queues a splat");
        assertEquals(5.0 - Projectile.PEA_IMPACT_ALIGNMENT_TILES,
                impacts.get(0).position().x(), 1.0e-9,
                "Plant Food shots use the same body alignment as normal ones");
        assertTrue(peashooter.isPlantFoodActive());
    }

    @Test
    void aLobSplatsOnTheZombiesBodyWithItsOwnAlignment() {
        plant("Cabbage-pult", 2, 0);
        Zombie target = zombie(2, 5.0, 100000);
        target.setSpeed(Position.ShowZero());
        session.drainProjectileImpacts();

        int hp = target.getHP();
        for (int i = 0; i < 120 && target.getHP() == hp; i++) session.tick();
        assertTrue(target.getHP() < hp, "the cabbage never landed");

        List<ProjectileImpact> impacts = session.drainProjectileImpacts();
        assertFalse(impacts.isEmpty(), "a lob that connects still queues a splat");
        assertEquals(5.0 - Projectile.LOB_IMPACT_ALIGNMENT_TILES,
                impacts.get(0).position().x(), 1.0e-9,
                "a lob lands on the body, not a third of a tile past it");
    }

    @Test
    void aLobThatComesDownOnAnEmptyLaneSplashesAtItsAlignedLandingPoint() {
        plant("Cabbage-pult", 2, 0);
        Zombie target = zombie(2, 5.0, 100000);
        target.setSpeed(Position.ShowZero());

        Projectile cabbage = firstFlyingShot(30);
        assertNotNull(cabbage);
        assertTrue(cabbage.isLobbed());
        session.drainProjectileImpacts();

        Zombie replacement = zombie(2, 5.0, 100000);
        replacement.setSpeed(Position.ShowZero());
        target.setAlive(false);

        int hp = replacement.getHP();
        for (int i = 0; i < 80 && replacement.getHP() == hp; i++) session.tick();
        assertTrue(replacement.getHP() < hp, "the cabbage never came down on what was there");

        List<ProjectileImpact> impacts = session.drainProjectileImpacts();
        assertFalse(impacts.isEmpty());
        assertTrue(impacts.get(0).position().x() < 5.0,
                "the landing splash is aligned onto the body too, not left where the arc ended");
    }

    @Test
    void theLobAlignmentIsSeparateFromThePeaOne() {
        assertTrue(Projectile.LOB_IMPACT_ALIGNMENT_TILES > 0);
        assertTrue(Projectile.LOB_IMPACT_ALIGNMENT_TILES < Projectile.PEA_IMPACT_ALIGNMENT_TILES,
                "a lob's correction is its own, smaller one");
    }

    @Test
    void aShotThatLeavesTheLawnWithoutHittingAnythingQueuesNoSplat() {
        plant("Peashooter", 2, 0);
        Zombie bait = zombie(2, 5.0, 100000);
        bait.setSpeed(Position.ShowZero());

        Projectile pea = firstFlyingShot(30);
        assertNotNull(pea);
        session.drainProjectileImpacts();

        bait.setPosition(new Position(5.0, 4.0));

        tick(80);
        assertTrue(session.getProjectiles().isEmpty(), "the pea should have left the lawn");
        assertTrue(session.drainProjectileImpacts().isEmpty(),
                "a shot that hits nothing must not fake an impact splat");
    }

    @Test
    void aShotThatRunsOutOfRangeQueuesNoSplat() {
        plant("Puff-shroom", 2, 0);
        Zombie target = zombie(2, 3.0, 100000);
        target.setSpeed(Position.ShowZero());

        Projectile puff = firstFlyingShot(30);
        assertNotNull(puff, "Puff-shroom never fired");
        assertTrue(puff.getMaxTravelDistance() > 0, "Puff-shroom's shot is range limited");
        session.drainProjectileImpacts();

        target.setPosition(new Position(3.0, 4.0));
        tick(60);

        assertTrue(session.drainProjectileImpacts().isEmpty(),
                "a shot expiring at its maximum range must not fake an impact splat");
    }


    @Test
    void aLobArcsAboveItsLaneAndComesBackDownOnTheTarget() {
        plant("Cabbage-pult", 2, 0);
        Zombie target = zombie(2, 5.0, 100000);
        target.setSpeed(Position.ShowZero());

        Projectile cabbage = firstFlyingShot(30);
        assertNotNull(cabbage, "Cabbage-pult never fired");
        assertTrue(cabbage.isLobbed(), "a pult's shot must be recognisable as a lob");

        double highest = 2.0;
        boolean fractional = false;
        int hp = target.getHP();
        for (int i = 0; i < 80 && target.getHP() == hp; i++) {
            session.tick();
            Position at = cabbage.getPosition();
            highest = Math.min(highest, at.y());
            if (Math.abs(at.y() - Math.round(at.y())) > 0.05) fractional = true;
        }

        assertTrue(highest < 1.0, "the cabbage never rose out of its own lane");
        assertTrue(fractional, "the arc must be a continuous curve, not whole-row hops");
        assertTrue(target.getHP() < hp, "the cabbage never landed on its target");
    }

    @Test
    void aLobLeadsAFastZombieInsteadOfOvershootingIt() {
        plant("Cabbage-pult", 2, 0);
        Zombie runner = zombie(2, 6.0, 100000);
        runner.setSpeed(new Position(-0.9, 0));

        int hp = runner.getHP();
        for (int i = 0; i < 200 && runner.getHP() == hp; i++) session.tick();

        assertTrue(runner.getHP() < hp,
                "the lob must be aimed with the speed it really travels at, or it overshoots");
    }

    @Test
    void aNormalMelonLobSplashesTheNeighbouringLane() {
        plant("Melon-pult", 2, 0);
        Zombie target = zombie(2, 5.0, 100000);
        Zombie neighbour = zombie(1, 5.0, 100000);
        target.setSpeed(Position.ShowZero());
        neighbour.setSpeed(Position.ShowZero());

        int targetHp = target.getHP();
        int neighbourHp = neighbour.getHP();
        for (int i = 0; i < 120 && target.getHP() == targetHp; i++) session.tick();

        assertTrue(target.getHP() < targetHp, "the melon never hit its target");
        assertTrue(neighbour.getHP() < neighbourHp,
                "an AOE lob splashes the neighbouring lane even without Plant Food");
    }

    @Test
    void aLobStillLandsWhenItsTargetDiesInFlight() {
        plant("Cabbage-pult", 2, 0);
        Zombie target = zombie(2, 5.0, 100000);
        target.setSpeed(Position.ShowZero());

        Projectile cabbage = firstFlyingShot(30);
        assertNotNull(cabbage);
        session.drainProjectileImpacts();

        Zombie replacement = zombie(2, 5.0, 100000);
        replacement.setSpeed(Position.ShowZero());
        target.setAlive(false);

        int hp = replacement.getHP();
        for (int i = 0; i < 80 && replacement.getHP() == hp; i++) session.tick();

        assertTrue(replacement.getHP() < hp,
                "a lob whose target died should still come down and hit what is there");
    }

    @Test
    void parasolDeflectionStopsLobsAndLetsDirectShotsThrough() {
        ParasolDeflection parasol = new ParasolDeflection();
        Zombie zombie = zombie(2, 5.0, 1000);

        Projectile lob = new Projectile(null, new Position(0, 2), new Position(4.5, 0),
                (Zombie) null, 40, new LobArcMove(0, 2, 5, 2, 1.5, 4.5), null);
        Projectile pea = new Projectile(null, new Position(0, 2), new Position(3.6, 0),
                (Zombie) null, 20, new StraightMove(), null);

        assertEquals(0, parasol.handleDamage(zombie, 40, lob, session),
                "a parasol must actually block the lobbed shot it exists to block");
        assertEquals(20, parasol.handleDamage(zombie, 20, pea, session),
                "a parasol does not stop direct shots");
    }

    @Test
    void aThreepeaterLaneShotSlidesAcrossThenRunsStraightDownThatLane() {
        plant("Threepeater", 2, 0);
        zombie(1, 6.0, 100000);
        zombie(2, 6.0, 100000);
        zombie(3, 6.0, 100000);

        tick(5);
        Projectile upward = session.getProjectiles().stream()
                .filter(p -> p.getMoveStrategy() instanceof LaneShiftMove lane
                        && Math.round(lane.getTargetY()) == 1)
                .findFirst().orElse(null);
        assertNotNull(upward, "Threepeater must aim a shot at the lane above it");

        for (int i = 0; i < 12; i++) session.tick();
        assertEquals(1.0, upward.getPosition().y(), 0.01,
                "the shot settles into the lane it was aimed at");

        double y = upward.getPosition().y();
        for (int i = 0; i < 5 && upward.isAlive(); i++) session.tick();
        assertEquals(y, upward.getPosition().y(), 0.01,
                "once in its lane the shot runs straight instead of drifting off the lawn");
    }

    @Test
    void aRepeaterSpacesItsTwoPeasOutInsteadOfStackingThem() {
        plant("Repeater", 2, 0);
        zombie(2, 8.0, 100000);

        tick(1);
        List<Projectile> volley = session.getProjectiles();
        assertEquals(2, volley.size(), "a Repeater fires two peas per volley");
        assertTrue(Math.abs(volley.get(0).getSpawnDelaySeconds()
                        - volley.get(1).getSpawnDelaySeconds()) > 0.05,
                "the second pea leaves later than the first");

        tick(10);
        assertEquals(2, session.getProjectiles().size());
        double gap = Math.abs(session.getProjectiles().get(0).getPosition().x()
                - session.getProjectiles().get(1).getPosition().x());
        assertTrue(gap > 0.1, "the two peas fly with a visible gap between them, got " + gap);
    }


    @Test
    void aShotKeepsThePlantFoodLookItWasFiredWith() {
        Plant peashooter = plant("Peashooter", 2, 0);
        zombie(2, 8.0, 100000);

        Projectile pea = firstFlyingShot(30);
        assertNotNull(pea);
        assertEquals("Peashooter", pea.getSourcePlantName());
        assertFalse(pea.isPlantFoodShot());

        peashooter.setPlantFoodTimer(5.0);
        assertTrue(peashooter.isPlantFoodActive());
        assertFalse(pea.isPlantFoodShot(),
                "a pea already in the air keeps the art it was fired with");
    }

    @Test
    void aDirectShotBreaksAGraveItCrossesEvenAwayFromTheTileCentre() {
        plant("Peashooter", 2, 0);
        session.getEnvironment().getCell(2, 4)
                .setObstacle(new model.pitches.obstacles.Grave());
        Zombie behind = zombie(2, 7.0, 100000);
        behind.setSpeed(Position.ShowZero());

        int before = graveHp(2, 4);
        assertTrue(before > 0, "the grave should have been placed");

        tick(40);
        assertTrue(graveHp(2, 4) < before, "the pea must damage the grave in its path");
    }

    @Test
    void aPeaSplatsOnTheGravesOwnRowNotTheEdgeItCrossed() {
        plant("Peashooter", 0, 0);
        session.getEnvironment().getCell(0, 4)
                .setObstacle(new model.pitches.obstacles.Grave());
        session.drainProjectileImpacts();

        int before = graveHp(0, 4);
        assertTrue(before > 0);
        for (int i = 0; i < 40 && graveHp(0, 4) == before; i++) session.tick();
        assertTrue(graveHp(0, 4) < before, "the pea never reached the grave");

        List<ProjectileImpact> impacts = session.drainProjectileImpacts();
        assertFalse(impacts.isEmpty(), "breaking a grave queues a splat");
        assertEquals(0.0, impacts.get(0).position().y(), 1.0e-9,
                "the splat sits on the grave's row, never a row above it");
    }

    @Test
    void aLaneShiftingShotStillSplatsOnTheGravesRowWhileItIsStillSliding() {
        plant("Threepeater", 2, 0);
        session.getEnvironment().getCell(1, 2)
                .setObstacle(new model.pitches.obstacles.Grave());
        zombie(1, 8.0, 100000);
        zombie(2, 8.0, 100000);
        zombie(3, 8.0, 100000);
        session.drainProjectileImpacts();

        int before = graveHp(1, 2);
        for (int i = 0; i < 40 && graveHp(1, 2) == before; i++) session.tick();
        assertTrue(graveHp(1, 2) < before, "the upward shot never reached the grave");

        for (ProjectileImpact impact : session.drainProjectileImpacts()) {
            double y = impact.position().y();
            assertEquals(Math.round(y), y, 1.0e-9,
                    "an impact always lands on a whole row, never between two, got " + y);
        }
    }

    @Test
    void aLobDoesNotBreakAGraveInTheRowItMerelyArcsOver() {
        plant("Cabbage-pult", 2, 0);
        session.getEnvironment().getCell(1, 3)
                .setObstacle(new model.pitches.obstacles.Grave());
        session.getEnvironment().getCell(2, 5)
                .setObstacle(new model.pitches.obstacles.Grave());
        session.drainProjectileImpacts();

        int above = graveHp(1, 3);
        int inLane = graveHp(2, 5);
        for (int i = 0; i < 120 && graveHp(2, 5) == inLane; i++) session.tick();

        assertTrue(graveHp(2, 5) < inLane, "the cabbage never came down on its own lane's grave");
        assertEquals(above, graveHp(1, 3),
                "a lob must not detonate on a grave in a row it is only flying over");

        List<ProjectileImpact> impacts = session.drainProjectileImpacts();
        assertFalse(impacts.isEmpty());
        assertEquals(2.0, impacts.get(0).position().y(), 1.0e-9,
                "the splat lands on the grave's row, not the tile edge the arc crossed");
    }

    @Test
    void aPlantFoodBarrageLobAimsItsBlockerSearchAtTheRowItIsFlyingTo() {
        Plant pult = plant("Cabbage-pult", 2, 0);
        session.getEnvironment().getCell(2, 3)
                .setObstacle(new model.pitches.obstacles.Grave());
        pult.setPlantFoodTimer(5.0);
        pult.setInternalTimer(0);
        session.drainProjectileImpacts();

        boolean reachedFarRow = false;
        for (int i = 0; i < 200 && !reachedFarRow; i++) {
            session.tick();
            for (ProjectileImpact impact : session.drainProjectileImpacts()) {
                if (Math.abs(impact.position().y() - 4.0) < 1.0e-9) reachedFarRow = true;
            }
        }
        assertTrue(reachedFarRow,
                "a blind lob aimed at row 4 must not detonate on a grave sitting in the plant's row");
    }

    @Test
    void aLobsGraveSplatSitsRightOfWhereADirectShotsDoes() {
        double direct = graveImpactX("Peashooter");
        assertEquals(4.5, direct, 1.0e-9,
                "a direct shot still splats at the tile edge it crossed");

        double lobbed = graveImpactX("Cabbage-pult");
        assertTrue(lobbed > direct + Projectile.LOB_GRAVE_IMPACT_ALIGNMENT_TILES - 1.0e-9,
                "a lob's grave splat is nudged right, got " + lobbed + " against " + direct);
    }

    private double graveImpactX(String plantName) {
        session = new GameSession(ROWS, COLS);
        GameSession.setCurrent(session);
        session.setZombieBreachesEnabled(false);
        session.setLawnMowersEnabled(false);
        plant(plantName, 2, 0);
        session.getEnvironment().getCell(2, 5)
                .setObstacle(new model.pitches.obstacles.Grave());
        session.drainProjectileImpacts();

        for (int i = 0; i < 200; i++) {
            session.tick();
            List<ProjectileImpact> impacts = session.drainProjectileImpacts();
            if (!impacts.isEmpty()) return impacts.get(0).position().x();
        }
        throw new AssertionError(plantName + " never hit the grave");
    }

    private int graveHp(int row, int col) {
        Object obstacle = session.getEnvironment().getCell(row, col).getObstacle();
        return obstacle instanceof model.pitches.obstacles.Grave grave ? grave.getHp() : -1;
    }

    @Test
    void aLobIgnoresAGraveWhenItIsAimedAtAZombieBehindIt() {
        plant("Cabbage-pult", 2, 0);
        session.getEnvironment().getCell(2, 3)
                .setObstacle(new model.pitches.obstacles.Grave());
        Zombie target = zombie(2, 6.0, 100000);
        target.setSpeed(Position.ShowZero());

        int graveBefore = graveHp(2, 3);
        int hp = target.getHP();
        for (int i = 0; i < 120 && target.getHP() == hp; i++) session.tick();

        assertTrue(target.getHP() < hp, "the cabbage never reached the zombie");
        assertEquals(graveBefore, graveHp(2, 3),
                "a lob sails over obstacles instead of stopping on them");
    }

    @Test
    void aProjectileWithNoSourcePlantQueuesNoImpact() {
        Zombie target = zombie(2, 2.0, 100000);
        target.setSpeed(Position.ShowZero());
        Projectile orphan = new Projectile((model.collections.Item) null, new Position(0, 2),
                new Position(3.6, 0), 20, new StraightMove(), null);
        orphan.setSpawnDelaySeconds(0);
        session.getProjectiles().add(orphan);

        int hp = target.getHP();
        for (int i = 0; i < 30 && target.getHP() == hp; i++) session.tick();

        assertTrue(target.getHP() < hp, "the shot should still deal its damage");
        assertNull(orphan.getSourcePlantName());
        assertTrue(session.drainProjectileImpacts().isEmpty(),
                "with no source plant there is no art to play, so nothing is queued");
    }
}
