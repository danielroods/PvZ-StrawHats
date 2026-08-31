import model.collections.Item;
import model.collections.item.GroundSun;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.plantfood.TimedProjectileBurst;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.match_mechanisms.vector.Position;
import model.pitches.Tile;
import model.pitches.TileType;
import model.projectile.Projectile;
import model.projectile.StraightMove;
import model.projectile.hit.FumeCloudHit;
import model.utils.GameSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantFoodBehaviorTest {

    private static final int ROWS = 5;
    private static final int COLS = 9;

    private GameSession session;

    @BeforeEach
    void setUp() {
        session = new GameSession(ROWS, COLS);
        GameSession.setCurrent(session);
        session.setZombieBreachesEnabled(false);
        session.setLawnMowersEnabled(false);
        session.setSkySunEnabled(false);
    }

    private Plant plant(String name, int row, int col) {
        Plant plant = PlantFactory.createPlantByName(name, 1, new Position(col, row));
        assertTrue(session.plantAt(row, col, plant), name + " should be plantable at " + row + "," + col);
        return plant;
    }

    private Zombie zombie(int row, double col, int hp) {
        Zombie zombie = ZombieFactory.create("ZombieDefault", row, (int) Math.round(col));
        zombie.setPosition(new Position(col, row));
        zombie.setHP(hp);
        zombie.setSpeed(Position.ShowZero());
        session.getZombies().add(zombie);
        return zombie;
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) session.tick();
    }

    private void makeWater(int row, int col) {
        session.getEnvironment().getCell(row, col).setTile(new Tile(TileType.Water));
    }

    private int shotsFiredOverWholeBoost(Plant plant) {
        Set<Projectile> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        seen.addAll(session.getProjectiles());
        int guard = 0;
        while (plant.isPlantFoodActive() && guard++ < 2000) {
            session.tick();
            seen.addAll(session.getProjectiles());
        }
        return seen.size();
    }

    private int shotsOnActivation(Plant plant) {
        int before = session.getProjectiles().size();
        assertTrue(plant.activatePlant(session));
        return session.getProjectiles().size() - before;
    }

    @Test
    void aPeashooterBoostEmptiesAWholeVolleyInsteadOfThreePeas() {
        Plant peashooter = plant("Peashooter", 2, 0);
        zombie(2, 8.0, 1_000_000);

        assertTrue(peashooter.activatePlant(session));
        int fired = shotsFiredOverWholeBoost(peashooter);

        assertTrue(fired >= 17, "a boosted Peashooter fires a real barrage, got " + fired);
    }

    @Test
    void theBarrageEndsWithOneGiantPeaThatPloughsThroughTheWholeLane() {
        Plant repeater = plant("Repeater", 2, 0);
        Zombie front = zombie(2, 3.0, 1_000_000);
        Zombie back = zombie(2, 6.0, 1_000_000);

        assertTrue(repeater.activatePlant(session));

        Projectile giant = null;
        int guard = 0;
        while (repeater.isPlantFoodActive() && guard++ < 2000) {
            session.tick();
            for (Projectile projectile : session.getProjectiles()) {
                if (projectile.getDisplayPath() != null
                        && projectile.getDisplayPath().contains("GIANTPEA")) {
                    giant = projectile;
                }
            }
        }
        assertNotNull(giant, "the volley never produced its giant pea finisher");
        assertEquals(400, giant.getDamage(), "the giant pea carries the plantFoodValue payload");

        int frontHp = front.getHP();
        int backHp = back.getHP();
        tick(60);
        assertTrue(front.getHP() < frontHp && back.getHP() < backHp,
                "the giant pea pierces rather than stopping on the first zombie");
    }

    @Test
    void everyShotOfABarrageIsTaggedAsAPlantFoodShot() {
        Plant peashooter = plant("Peashooter", 2, 0);
        zombie(2, 8.0, 1_000_000);

        assertTrue(peashooter.activatePlant(session));
        session.tick();

        assertFalse(session.getProjectiles().isEmpty(), "the boost fired nothing");
        for (Projectile projectile : session.getProjectiles()) {
            assertTrue(projectile.isPlantFoodShot(),
                    "the first shot of a boost must already count as a Plant Food shot");
        }
    }

    @Test
    void aBoostedShooterFiresEvenWithAnEmptyLane() {
        Plant peashooter = plant("Peashooter", 2, 0);

        assertTrue(peashooter.activatePlant(session));
        session.tick();

        assertFalse(session.getProjectiles().isEmpty(),
                "a Plant Food barrage does not wait for a target");
    }

    @Test
    void snowPeaFreezesItsWholeRowSolid() {
        Plant snowPea = plant("Snow Pea", 2, 0);
        Zombie inRow = zombie(2, 6.0, 1_000_000);
        Zombie otherRow = zombie(3, 6.0, 1_000_000);

        assertTrue(snowPea.activatePlant(session));
        session.tick();

        assertEquals(Zombie.Status.FROZEN, inRow.getStatus(), "the boosted row ices over");
        assertEquals(Zombie.Status.NORMAL, otherRow.getStatus(), "only the plant's own row");
    }

    @Test
    void snowPeasOwnIcyBarrageDoesNotThawTheRowItJustFroze() {
        Plant snowPea = plant("Snow Pea", 2, 0);
        Zombie target = zombie(2, 6.0, 1_000_000);

        assertTrue(snowPea.activatePlant(session));
        int guard = 0;
        int hp = target.getHP();
        while (snowPea.isPlantFoodActive() && guard++ < 2000) {
            session.tick();
            assertEquals(Zombie.Status.FROZEN, target.getStatus(),
                    "a chilling pea must not downgrade the lane-wide freeze");
        }
        assertTrue(target.getHP() < hp, "the icy barrage still does its damage");
    }

    @Test
    void threepeaterCoversThreeLanesWithBothItsBarrageAndItsFinisher() {
        Plant threepeater = plant("Threepeater", 2, 0);
        Zombie above = zombie(1, 6.0, 1_000_000);
        Zombie middle = zombie(2, 6.0, 1_000_000);
        Zombie below = zombie(3, 6.0, 1_000_000);

        assertTrue(threepeater.activatePlant(session));
        int guard = 0;
        while (threepeater.isPlantFoodActive() && guard++ < 2000) session.tick();
        tick(80);

        assertTrue(above.getHP() < 1_000_000, "the lane above is covered");
        assertTrue(middle.getHP() < 1_000_000, "its own lane is covered");
        assertTrue(below.getHP() < 1_000_000, "the lane below is covered");
    }

    @Test
    void everyAquaticPlantIsRefusedOnDryLand() {
        for (String name : new String[] {"Tangle Kelp", "Lily Pad", "Sea-shroom"}) {
            Plant onLand = PlantFactory.createPlantByName(name, 1, new Position(5, 4));
            assertFalse(session.plantAt(4, 5, onLand), name + " must not go on dry land");
        }
    }

    @Test
    void firePeashooterSetsItsWholeRowAlight() {
        Plant firePea = plant("Fire Peashooter", 2, 4);
        Zombie ahead = zombie(2, 6.0, 1_000_000);
        Zombie behind = zombie(2, 1.0, 1_000_000);
        Zombie otherRow = zombie(3, 6.0, 1_000_000);

        int aheadHp = ahead.getHP();
        assertTrue(firePea.activatePlant(session));
        session.tick();

        assertEquals(Zombie.Status.FIRED, ahead.getStatus(), "the lane in front burns");
        assertTrue(ahead.getHP() < aheadHp, "the burning lane scorches what stands in it");
        assertEquals(Zombie.Status.NORMAL, otherRow.getStatus(), "only the plant's own row");
        assertEquals(Zombie.Status.NORMAL, behind.getStatus(),
                "the fire runs forwards from the plant, not behind it");
    }

    @Test
    void megaGatlingPeaFiresTheLongestBarrageOfTheFamily() {
        Plant gatling = plant("Mega Gatling Pea", 2, 0);
        Plant peashooter = plant("Peashooter", 4, 0);
        zombie(2, 8.0, 1_000_000);
        zombie(4, 8.0, 1_000_000);

        assertTrue(gatling.activatePlant(session));
        assertTrue(peashooter.activatePlant(session));

        int gatlingShots = 0;
        int peashooterShots = 0;
        int guard = 0;
        while ((gatling.isPlantFoodActive() || peashooter.isPlantFoodActive()) && guard++ < 3000) {
            int before = session.getProjectiles().size();
            session.tick();
            int added = session.getProjectiles().size() - before;
            if (added <= 0) continue;
            for (Projectile projectile : session.getProjectiles()) {
                if (!projectile.isVisible() && projectile.getSpawnDelaySeconds() > 0.38) {
                    if (Math.round(projectile.getPosition().y()) == 2) gatlingShots++;
                    else peashooterShots++;
                }
            }
            gatlingShots = Math.min(gatlingShots, 10_000);
            peashooterShots = Math.min(peashooterShots, 10_000);
        }
        assertTrue(gatlingShots > peashooterShots,
                "Mega Gatling Pea's barrage must out-shoot a plain Peashooter's");
    }

    @Test
    void aBoostedPultLobsAtEveryZombieOnTheLawnRepeatedly() {
        Plant melon = plant("Melon-pult", 2, 0);
        Zombie a = zombie(2, 5.0, 1_000_000);
        Zombie b = zombie(0, 6.0, 1_000_000);
        Zombie c = zombie(4, 7.0, 1_000_000);

        assertTrue(melon.activatePlant(session));
        int fired = shotsFiredOverWholeBoost(melon);
        assertTrue(fired >= 3 * 5, "each volley covers every zombie on the board, got " + fired);

        tick(80);
        assertTrue(a.getHP() < 1_000_000 && b.getHP() < 1_000_000 && c.getHP() < 1_000_000,
                "melons reach zombies in other rows too");
    }

    @Test
    void kernelPultsButterBarrageArrivesAsLobbedShotsThatStunOnImpact() {
        Plant kernel = plant("Kernel-pult", 2, 0);
        Zombie target = zombie(2, 5.0, 1_000_000);

        assertTrue(kernel.activatePlant(session));
        session.tick();
        assertFalse(session.getProjectiles().isEmpty(), "the butter barrage fires projectiles");

        int guard = 0;
        while (target.getStatus() != Zombie.Status.BUTTER && guard++ < 300) session.tick();
        assertEquals(Zombie.Status.BUTTER, target.getStatus(),
                "every butter shot of the barrage sticks the zombie it hits");
    }

    @Test
    void starfruitFiresAllFiveOfItsDirectionsOnEveryVolley() {
        Plant starfruit = plant("Starfruit", 2, 4);
        zombie(2, 8.0, 1_000_000);

        int volley = shotsOnActivation(starfruit);
        assertTrue(volley >= 5, "a boosted Starfruit volley is one star per point, got " + volley);
    }

    @Test
    void rotobagaFiresAllFourDiagonalsOnEveryVolley() {
        Plant rotobaga = plant("Rotobaga", 2, 4);
        zombie(2, 8.0, 1_000_000);

        int volley = shotsOnActivation(rotobaga);
        assertTrue(volley >= 4,
                "a boosted Rotobaga volley is one vegetable per diagonal, got " + volley);
    }


    @Test
    void electricBlueberryZapsEveryZombieOnTheLawn() {
        Plant blueberry = plant("Electric Blueberry", 2, 0);
        Zombie a = zombie(0, 5.0, 500);
        Zombie b = zombie(2, 6.0, 500);
        Zombie c = zombie(4, 7.0, 500);

        assertTrue(blueberry.activatePlant(session));
        tick(120);

        assertFalse(a.isAlive(), "the boost reaches the top row");
        assertFalse(b.isAlive(), "the boost reaches its own row");
        assertFalse(c.isAlive(), "the boost reaches the bottom row");
    }

    @Test
    void anElectricBlueberryKillIsAShockDeathSoItPlaysTheElectrocution() {
        Plant blueberry = plant("Electric Blueberry", 2, 0);
        Zombie target = zombie(2, 5.0, 500);

        assertTrue(blueberry.activatePlant(session));
        int guard = 0;
        while (target.isAlive() && guard++ < 200) session.tick();

        assertFalse(target.isAlive());
        assertTrue(target.diedFromShock(),
                "the boosted zap has to kill the same way the plant's normal zap does");
    }

    @Test
    void fumeShroomsBoostBillowsAsRealProjectilesSoItCanActuallyBeSeen() {
        Plant fume = plant("Fume-shroom", 2, 0);

        assertTrue(fume.activatePlant(session));

        assertFalse(session.getProjectiles().isEmpty(), "the boost produced no fume at all");
        Projectile cloud = session.getProjectiles().get(0);
        assertEquals("Fume-shroom", cloud.getSourcePlantName());
        assertTrue(cloud.isPlantFoodShot(), "the cloud has to draw with the boosted artwork");
        assertTrue(cloud.isVisible(), "the fume is already at the plant's mouth, not delayed");
    }

    @Test
    void fumeShroomsBoostFillsThreeLanesAndKeepsBillowing() {
        Plant fume = plant("Fume-shroom", 2, 0);
        Zombie ownRow = zombie(2, 3.0, 1_000_000);
        Zombie aboveRow = zombie(1, 3.0, 1_000_000);
        Zombie farAway = zombie(2, 8.0, 1_000_000);

        assertTrue(fume.activatePlant(session));
        int guard = 0;
        while (ownRow.getHP() == 1_000_000 && guard++ < 60) session.tick();

        int afterFirstBillow = ownRow.getHP();
        assertTrue(afterFirstBillow < 1_000_000, "the cloud never reached its own lane");
        assertTrue(aboveRow.getHP() < 1_000_000, "the cloud is three lanes tall");
        assertEquals(1_000_000, farAway.getHP(),
                "the cloud stops at the end of Fume-shroom's range");

        tick(30);
        assertTrue(ownRow.getHP() < afterFirstBillow,
                "the boost keeps billowing, it is not a single hit");
    }

    @Test
    void fumeShroomsBoostTearsOffArmourInsteadOfChewingThroughIt() {
        Plant fume = plant("Fume-shroom", 2, 0);
        Zombie armoured = ZombieFactory.create("ZombieArmor2", 2, 3);
        armoured.setPosition(new Position(3.0, 2));
        armoured.setSpeed(Position.ShowZero());
        session.getZombies().add(armoured);
        assertNotNull(armoured.getArmour(), "the test needs an armoured zombie");
        int hp = armoured.getHP();

        assertTrue(fume.activatePlant(session));
        int guard = 0;
        while (armoured.getArmour() != null && guard++ < 60) session.tick();

        assertNull(armoured.getArmour(),
                "the fume strips the bucket rather than grinding it down");
        assertTrue(armoured.getHP() < hp, "and the damage lands on the zombie underneath");
    }

    @Test
    void onePiercingAreaShotDamagesEachZombieItPassesThroughExactlyOnce() {
        Zombie first = zombie(2, 2.0, 1_000_000);
        Zombie second = zombie(2, 3.0, 1_000_000);

        Projectile cloud = new Projectile(null, new Position(0.0, 2.0),
                new Position(2.6, 0.0), null, 200, new StraightMove(), new FumeCloudHit(3));
        cloud.setSpawnDelaySeconds(0.0);
        cloud.setMaxTravelDistance(4.5);
        session.getProjectiles().add(cloud);

        int guard = 0;
        while (session.getProjectiles().contains(cloud) && guard++ < 60) session.tick();

        assertEquals(1_000_000 - 200, first.getHP(), "one shot hits a zombie once");
        assertEquals(1_000_000 - 200, second.getHP(),
                "including the ones further down the lane it pierces through");
    }

    @Test
    void splitPeaFiresOnePeaForwardAndTwoBackwards() {
        Plant splitPea = plant("Split Pea", 2, 4);
        Zombie ahead = zombie(2, 7.0, 1_000_000);
        Zombie behind = zombie(2, 1.0, 1_000_000);

        int guard = 0;
        while (session.getProjectiles().size() < 3 && guard++ < 60) session.tick();

        int forward = 0;
        int backward = 0;
        for (Projectile shot : session.getProjectiles()) {
            if (shot.getSpeed().x() > 0) forward++;
            else if (shot.getSpeed().x() < 0) backward++;
        }
        assertEquals(1, forward, "one pea goes forward");
        assertEquals(2, backward, "two peas go backward");

        tick(80);
        assertTrue(ahead.getHP() < 1_000_000, "the forward pea connects");
        assertTrue(behind.getHP() < 1_000_000, "and so do the backward ones");
    }

    @Test
    void aBackwardPeaIsMarkedSoTheViewCanMirrorItsMuzzle() {
        plant("Split Pea", 2, 4);
        zombie(2, 1.0, 1_000_000);

        int guard = 0;
        while (session.getProjectiles().isEmpty() && guard++ < 60) session.tick();

        boolean sawBackward = false;
        for (Projectile shot : session.getProjectiles()) {
            assertEquals(shot.getSpeed().x() < 0, shot.isFiredBackwards(),
                    "the spawn side has to match the direction the shot left in");
            sawBackward |= shot.isFiredBackwards();
        }
        assertTrue(sawBackward, "Split Pea never fired backwards");
    }

    @Test
    void rotobagaHitsHarderThanASinglePea() {
        Plant rotobaga = plant("Rotobaga", 2, 4);
        assertTrue(rotobaga.getDamage() >= 25, "a Rotobaga shot is worth its 150 sun");
    }

    @Test
    void rotobagasBoostSpraysEveryDirectionAtOnce() {
        Plant rotobaga = plant("Rotobaga", 2, 4);

        int volley = shotsOnActivation(rotobaga);
        assertEquals(8, volley, "the boosted spray covers all four lanes and all four diagonals");

        int fired = shotsFiredOverWholeBoost(rotobaga);
        assertTrue(fired >= 8 * 20, "and keeps spraying for the whole boost, got " + fired);

        for (Projectile shot : session.getProjectiles()) {
            assertTrue(shot.getDamage() > 25,
                    "boosted rutabagas hit harder than the plant's normal shot");
        }
    }

    @Test
    void rotobagasBoostReachesZombiesBehindAndAboveIt() {
        Plant rotobaga = plant("Rotobaga", 2, 4);
        Zombie behind = zombie(2, 1.0, 1_000_000);
        Zombie above = zombie(1, 7.0, 1_000_000);
        Zombie ahead = zombie(2, 7.0, 1_000_000);

        assertTrue(rotobaga.activatePlant(session));
        tick(120);

        assertTrue(behind.getHP() < 1_000_000, "the spray covers the lane behind");
        assertTrue(above.getHP() < 1_000_000, "and the neighbouring lanes");
        assertTrue(ahead.getHP() < 1_000_000, "and straight ahead");
    }


    @Test
    void bonkChoyPunchesEveryNeighbouringTileManyTimes() {
        Plant bonkChoy = plant("Bonk Choy", 2, 4);
        Zombie ahead = zombie(2, 5.0, 1_000_000);
        Zombie diagonal = zombie(1, 5.0, 1_000_000);
        Zombie outOfReach = zombie(2, 7.0, 1_000_000);

        assertTrue(bonkChoy.activatePlant(session));
        tick(12);
        int afterWindUp = ahead.getHP();
        assertTrue(afterWindUp < 1_000_000, "the first punch lands once the wind-up is over");

        int guard = 0;
        while (bonkChoy.isPlantFoodActive() && guard++ < 400) session.tick();

        assertTrue(1_000_000 - ahead.getHP() >= 10 * 45,
                "the flurry is many punches, not one; dealt "
                        + (1_000_000 - ahead.getHP()));
        assertTrue(diagonal.getHP() < 1_000_000, "diagonals are part of the eight tiles");
        assertEquals(1_000_000, outOfReach.getHP(), "two tiles away is out of reach");
    }

    @Test
    void phatBeetHoldsItsAreaStunnedForTheWholeSong() {
        Plant phatBeet = plant("Phat Beet", 2, 4);
        Zombie nearby = zombie(1, 6.0, 1_000_000);

        assertTrue(phatBeet.activatePlant(session));
        session.tick();

        assertTrue(nearby.getHP() < 1_000_000, "two tiles either side, three lanes tall");
        assertEquals(Zombie.Status.BUTTER, nearby.getStatus(), "the song pins them in place");
        assertTrue(phatBeet.getPlantFoodTimer() > 3.0, "the song runs for several seconds");
    }

    @Test
    void wasabiWhipLashesItsLaneAndSetsZombiesOnFire() {
        Plant wasabi = plant("Wasabi Whip", 2, 4);
        Zombie ahead = zombie(2, 7.0, 1_000_000);
        Zombie behind = zombie(2, 1.0, 1_000_000);
        Zombie otherRow = zombie(0, 5.0, 1_000_000);

        assertTrue(wasabi.activatePlant(session));
        tick(6);

        assertTrue(ahead.getHP() < 1_000_000, "the whip reaches three tiles ahead");
        assertTrue(behind.getHP() < 1_000_000, "and three tiles behind");
        assertEquals(Zombie.Status.FIRED, ahead.getStatus(), "wasabi burns");
        assertEquals(1_000_000, otherRow.getHP(), "it stays in its own lane");
    }

    @Test
    void kiwibeastSlamsSeveralTimesAndThrowsSurvivorsBack() {
        Plant kiwibeast = plant("Kiwibeast", 2, 4);
        Zombie target = zombie(2, 5.0, 1_000_000);

        assertTrue(kiwibeast.activatePlant(session));
        assertEquals(3, kiwibeast.getGrowthStage(),
                "the boost matures the kiwi so its slam animation is the right one");

        int afterFirstSlam = target.getHP();
        assertTrue(afterFirstSlam < 1_000_000, "the first slam lands with the leap");

        int guard = 0;
        while (kiwibeast.isPlantFoodActive() && guard++ < 400) session.tick();

        assertTrue(1_000_000 - target.getHP() >= 2 * 400,
                "the boost is a sequence of slams; dealt " + (1_000_000 - target.getHP()));
        assertTrue(target.getPosition().x() > 5.0, "survivors are thrown away from the crater");
    }

    @Test
    void icebergLettuceFreezesTheWholeLawnSolid() {
        Plant lettuce = plant("Iceberg Lettuce", 2, 0);
        Zombie a = zombie(0, 5.0, 1_000_000);
        Zombie b = zombie(4, 7.0, 1_000_000);

        assertTrue(lettuce.activatePlant(session));
        session.tick();

        assertEquals(Zombie.Status.FROZEN, a.getStatus());
        assertEquals(Zombie.Status.FROZEN, b.getStatus());
    }

    @Test
    void aFrozenZombieCannotKeepEating() {
        Plant wallNut = plant("Wall-nut", 2, 4);
        Zombie eater = zombie(2, 4.0, 1_000_000);

        eater.applyStatus(Zombie.Status.FROZEN, 10.0);
        int hp = wallNut.getHP();
        tick(30);

        assertEquals(hp, wallNut.getHP(), "a zombie frozen solid stops biting, not just walking");
    }

    @Test
    void catTailFiresALongHomingVolleySpreadOverEveryZombie() {
        Plant catTail = plant("Cat-tail", 2, 0);
        Zombie a = zombie(0, 5.0, 1_000_000);
        Zombie b = zombie(4, 7.0, 1_000_000);

        assertTrue(catTail.activatePlant(session));
        int fired = shotsFiredOverWholeBoost(catTail);
        assertTrue(fired >= 20, "the volley is a stream of spikes, got " + fired);

        tick(80);
        assertTrue(a.getHP() < 1_000_000, "spikes reach the top row");
        assertTrue(b.getHP() < 1_000_000, "and the bottom row");
    }

    @Test
    void aBoostedCatTailSpikeHitsThreeTimesAsHardAsANormalOne() {
        Plant catTail = plant("Cat-tail", 2, 0);
        zombie(2, 5.0, 1_000_000);

        int normalDamage = catTail.getDamage();
        assertTrue(catTail.activatePlant(session));
        session.tick();

        assertFalse(session.getProjectiles().isEmpty(), "the volley fired nothing");
        for (Projectile spike : session.getProjectiles()) {
            assertEquals(normalDamage * 3, spike.getDamage(),
                    "a boosted spike carries three times the plant's normal payload");
        }
    }


    @Test
    void aZombieThatBitASunBeanKeepsProducingSun() {
        Plant sunBean = plant("Sun Bean", 2, 4);
        Zombie biter = zombie(2, 4.0, 1_000_000);

        int guard = 0;
        while (!biter.isSunBeanCarrier() && guard++ < 200) session.tick();
        assertTrue(biter.isSunBeanCarrier(), "biting the bean marks the zombie");

        int sunsBefore = countSuns();
        tick(60);
        assertTrue(countSuns() > sunsBefore,
                "a sun-bean carrier drops sun every five seconds while it lives");
    }

    private int countSuns() {
        int suns = 0;
        for (Item item : session.getItems()) {
            if (item instanceof GroundSun) suns++;
        }
        return suns;
    }

    @Test
    void sunBeansBoostGivesItAShield() {
        Plant sunBean = plant("Sun Bean", 2, 4);

        assertTrue(sunBean.activatePlant(session));
        assertNotNull(sunBean.getArmor(), "the boost is the bean's shield");
        assertTrue(sunBean.getArmor().getHP() > 1000, "and it is a serious one");
        assertEquals("plantfood_on", sunBean.getVisualAnimationState(),
                "the shield uses the bean's own plantfood clip, not a bare idle");
    }

    @Test
    void tangleKelpOnlyGoesInWater() {
        Plant onLand = PlantFactory.createPlantByName("Tangle Kelp", 1, new Position(3, 2));
        assertFalse(session.plantAt(2, 3, onLand), "an aquatic trap cannot be planted on dry land");

        makeWater(2, 3);
        Plant inWater = PlantFactory.createPlantByName("Tangle Kelp", 1, new Position(3, 2));
        assertTrue(session.plantAt(2, 3, inWater), "and can be planted in water");
    }

    @Test
    void tangleKelpGrabsTheZombieOnItsTileBeforeItCanEatIt() {
        makeWater(2, 3);
        Plant kelp = plant("Tangle Kelp", 2, 3);
        Zombie swimmer = zombie(2, 3.4, 400);

        int kelpHp = kelp.getHP();
        int guard = 0;
        while (swimmer.getDragUnderWaterProgress() <= 0.0 && guard++ < 200) session.tick();
        assertTrue(swimmer.getDragUnderWaterProgress() > 0.0, "the kelp never started its grab");
        assertEquals(kelpHp, kelp.getHP(), "the kelp is never bitten while it is dragging");

        guard = 0;
        while (swimmer.isAlive() && guard++ < 200) session.tick();

        assertFalse(swimmer.isAlive(), "the zombie standing on the kelp is dragged under");
        assertTrue(swimmer.diedFromDragUnderWater(), "and dies the drowning death");
        assertFalse(session.getPlants().contains(kelp), "a used trap is consumed");
    }

    @Test
    void tangleKelpIgnoresAGargantuarItCannotPullUnder() {
        makeWater(2, 3);
        Plant kelp = plant("Tangle Kelp", 2, 3);
        Zombie garg = ZombieFactory.create("ZombieGargantuar", 2, 3);
        garg.setPosition(new Position(3.0, 2));
        garg.setSpeed(Position.ShowZero());
        session.getZombies().add(garg);

        tick(60);
        assertTrue(garg.isAlive(), "a Gargantuar is too big to drag under");
    }

    @Test
    void tangleKelpsBoostDragsSeveralSwimmersUnderAndIsThenSpent() {
        for (int col = 2; col <= 6; col++) makeWater(2, col);
        Plant kelp = plant("Tangle Kelp", 2, 3);
        Zombie onTile = zombie(2, 3.0, 400);
        Zombie remote = zombie(2, 6.0, 400);

        assertTrue(kelp.activatePlant(session));
        int guard = 0;
        while ((onTile.isAlive() || remote.isAlive()) && guard++ < 400) session.tick();

        assertFalse(onTile.isAlive(), "the zombie on the kelp's own tile is taken");
        assertFalse(remote.isAlive(), "and so are the extra swimmers the boost reaches");

        guard = 0;
        while (session.getPlants().contains(kelp) && guard++ < 200) session.tick();
        assertFalse(session.getPlants().contains(kelp), "the trap is spent afterwards");
    }


    @Test
    void bombardMintFeedsEveryExplosiveIncludingPotatoMinesWithoutBlowingUp() {
        Plant mine = plant("Potato Mine", 2, 4);
        Plant primal = plant("Primal Potato Mine", 3, 4);
        Plant mint = plant("Bombard-mint", 0, 0);

        int plantsBefore = session.getPlants().size();
        tick(40);

        assertFalse(session.getPlants().contains(mint), "the mint is used up");
        assertTrue(mine.isPotatoMineArmed(), "a fed Potato Mine arms instantly");
        assertTrue(primal.isPotatoMineArmed(), "and so does a Primal Potato Mine");
        assertTrue(session.getPlants().size() > plantsBefore - 1,
                "feeding the mines spawns their copies instead of crashing the tick");
    }

    @Test
    void enchantMintFeedsLilyPadsWithoutTrippingOverTheClonesTheySpawn() {
        for (int col = 2; col <= 6; col++) makeWater(2, col);
        plant("Lily Pad", 2, 3);
        Plant mint = plant("Enchant-mint", 0, 0);

        tick(40);
        assertFalse(session.getPlants().contains(mint), "the mint is used up");
        assertTrue(session.getPlants().size() >= 2, "the Lily Pads it fed are still there");
    }


    @Test
    void aBarrageIsOverBeforeThePlantGoesBackToItsNormalCadence() {
        Plant peashooter = plant("Peashooter", 2, 0);
        zombie(2, 8.0, 1_000_000);
        assertTrue(peashooter.activatePlant(session));

        TimedProjectileBurst burst =
                (TimedProjectileBurst) peashooter.getPlantFoodEffect();
        int guard = 0;
        while (peashooter.isPlantFoodActive() && guard++ < 2000) session.tick();

        assertTrue(burst.isBurstFinished(), "the whole barrage fires within the boost window");
        assertFalse(peashooter.isPlantFoodActive());
        assertTrue(peashooter.canUsePlantFood(), "and the plant can be fed again afterwards");
    }

    @Test
    void feedingOnePuffShroomFeedsThemAllWithoutDisturbingTheList() {
        Plant first = plant("Puff-shroom", 1, 0);
        Plant second = plant("Puff-shroom", 3, 0);
        zombie(1, 3.0, 1_000_000);
        zombie(3, 3.0, 1_000_000);

        assertTrue(first.activatePlant(session));
        assertTrue(second.isPlantFoodActive(), "the whole colony gets the boost");
        assertSame(first.getPlantFoodEffect().getClass(), second.getPlantFoodEffect().getClass());
    }
}
