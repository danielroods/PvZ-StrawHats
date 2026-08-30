import com.google.gson.JsonObject;
import model.collections.Item;
import model.collections.item.GroundItem;
import model.collections.item.GroundPlantFood;
import model.collections.item.GroundSun;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.zombie.ZombieFactory;
import model.match.mini_games.izombie.IZombieMatch;
import model.match.mini_games.izombie.IZombieMatch.Role;
import model.match_mechanisms.vector.Position;
import model.projectile.Projectile;
import model.utils.GameSession;
import net.JsonLine;
import net.client.SnapshotApplier;
import net.dto.MatchSnapshot;
import net.server.MatchSnapshotBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnlinePlantParityTest {

    private static final double TICK = 0.1;
    private static final int PLANT_ROW = 2;
    private static final int PLANT_COL = 5;
    private static final int ZOMBIE_COL = 8;

    @BeforeAll
    static void initFactories() {
        PlantFactory.autoInit();
        ZombieFactory.init();
    }

    private static final class Mirror {
        final IZombieMatch match;
        final MatchSnapshotBuilder builder = new MatchSnapshotBuilder();
        final GameSession shadow = new GameSession(IZombieMatch.ROWS, IZombieMatch.COLS);
        final SnapshotApplier applier = new SnapshotApplier(shadow);
        final List<Plant> uprooted = new ArrayList<>();
        MatchSnapshot last;
        int tick;

        Mirror(List<String> plantLoadout) {
            match = new IZombieMatch(100_000.0, plantLoadout, null);
            shadow.setLawnMowersEnabled(false);
            shadow.setSkySunEnabled(false);
            shadow.setZombieBreachesEnabled(false);
        }

        GameSession server() {
            return match.getSession();
        }

        String intent(Role role, String action, String target, int row, int col) {
            GameSession.setCurrent(server());
            return match.applyIntent(role, action, target, row, col);
        }

        void step() {
            applier.advanceProjectiles((float) TICK);
            applier.advancePlantVisuals((float) TICK);

            GameSession.setCurrent(server());
            List<Plant> plantsBefore = new ArrayList<>(server().getPlants());
            var shotsBefore = new ArrayList<>(server().getZombieProjectiles());
            match.tick();
            tick++;
            builder.captureRemovals(match, plantsBefore, shotsBefore);
            builder.forgetDeadEntities(match);

            MatchSnapshot built = builder.build(match, Role.PLANTS, tick);
            builder.clearRemovals();

            JsonObject wire = JsonLine.compactTree(built);
            last = JsonLine.fromTree(wire, MatchSnapshot.class);

            GameSession.setCurrent(shadow);
            applier.apply(last);
            uprooted.addAll(applier.drainRemovedPlants());
        }

        void run(int ticks) {
            for (int i = 0; i < ticks; i++) {
                step();
                assertMirrored();
            }
        }

        void earnSun(int target) {
            for (int i = 0; i < 4000 && match.getPlantSun() < target; i++) {
                step();
                collectEverySun();
            }
            assertTrue(match.getPlantSun() >= target,
                    "the plant bank never reached " + target + " sun");
        }

        void collectEverySun() {
            for (Item item : new ArrayList<>(server().getItems())) {
                if (!(item instanceof GroundSun sun) || !sun.isAlive()) continue;
                Position at = sun.getPosition();
                if (at == null) continue;
                intent(Role.PLANTS, "COLLECT_SUN", null,
                        (int) Math.round(at.y()), (int) Math.round(at.x()));
            }
        }

        void clearLawnKeepingSunflowers() {
            for (int row = 0; row < IZombieMatch.ROWS; row++) {
                for (int col = IZombieMatch.BRAIN_COLUMN + 2;
                     col <= IZombieMatch.REDLINE_COLUMN; col++) {
                    intent(Role.PLANTS, "DIG", null, row, col);
                }
            }
        }

        void assertMirrored() {
            Map<String, Plant> mirrored = new HashMap<>();
            for (Plant plant : shadow.getPlants()) {
                mirrored.put(key(plant), plant);
            }
            for (Plant authoritative : server().getPlants()) {
                if (!authoritative.isAlive() || authoritative.getPosition() == null) continue;
                Plant copy = mirrored.get(key(authoritative));
                assertNotNull(copy, "the client is missing " + authoritative.getName()
                        + " at " + key(authoritative) + " on tick " + tick);
                assertPlantMatches(authoritative, copy);
            }
            assertEquals(countAlive(server().getPlants()), shadow.getPlants().size(),
                    "the client shows a different number of plants on tick " + tick);
            assertProjectilesMirrored();
        }

        private void assertPlantMatches(Plant authoritative, Plant copy) {
            String where = authoritative.getName() + " at " + key(authoritative)
                    + " on tick " + tick;
            assertEquals(authoritative.getName(), copy.getName(), where);
            assertEquals(authoritative.getHP(), copy.getHP(), where + " - hp");
            assertEquals(authoritative.getMaxHp(), copy.getMaxHp(), where + " - max hp");
            assertEquals(authoritative.getPlantState(), copy.getPlantState(), where + " - state");
            assertEquals(authoritative.isPlantFoodActive(), copy.isPlantFoodActive(),
                    where + " - plant food");
            assertEquals(authoritative.getVisualAnimationState(), copy.getVisualAnimationState(),
                    where + " - visual clip");
            assertEquals(authoritative.getGrowthStage(), copy.getGrowthStage(),
                    where + " - growth stage");
            assertEquals(authoritative.getStackNumber(), copy.getStackNumber(),
                    where + " - stack");
            assertEquals(authoritative.getChillLevel(), copy.getChillLevel(), where + " - chill");
            assertEquals(authoritative.isMeleeFacingLeft(), copy.isMeleeFacingLeft(),
                    where + " - melee facing");
            assertEquals(authoritative.isPotatoMineArmed(), copy.isPotatoMineArmed(),
                    where + " - mine armed");
            assertEquals(authoritative.isPotatoMineDetonationPending(),
                    copy.isPotatoMineDetonationPending(), where + " - mine fuse");
            assertEquals(authoritative.isCactusUnderground(), copy.isCactusUnderground(),
                    where + " - cactus posture");
            assertEquals(authoritative.isMagnetItemVisible(), copy.isMagnetItemVisible(),
                    where + " - magnet item");
            assertEquals(authoritative.isEndurianUnderAttack(), copy.isEndurianUnderAttack(),
                    where + " - endurian hit flash");
            assertEquals(armourHp(authoritative), armourHp(copy), where + " - armour");
            assertEquals(authoritative.getIntervalTimer(), copy.getIntervalTimer(), 1.0e-6,
                    where + " - shot timer");
            assertEquals(clampedRemaining(authoritative), copy.getVisualAnimationRemaining(),
                    1.0e-6, where + " - clip remaining");
        }

        private void assertProjectilesMirrored() {
            int authoritative = 0;
            for (Projectile projectile : server().getProjectiles()) {
                if (projectile.isAlive() && projectile.isVisible()) authoritative++;
            }
            assertEquals(authoritative, shadow.getProjectiles().size(),
                    "the client shows a different number of projectiles on tick " + tick);
            for (Projectile projectile : shadow.getProjectiles()) {
                assertTrue(projectile.isVisible(),
                        "a mirrored projectile must never sit in its spawn delay");
            }
        }

        private static double clampedRemaining(Plant plant) {
            double remaining = plant.getVisualAnimationRemaining();
            return Double.isInfinite(remaining) ? 1.0e9 : remaining;
        }

        private static int armourHp(Plant plant) {
            return plant.getArmor() == null ? 0 : plant.getArmor().getHP();
        }

        private static int countAlive(List<Plant> plants) {
            int alive = 0;
            for (Plant plant : plants) {
                if (plant.isAlive() && plant.getPosition() != null) alive++;
            }
            return alive;
        }

        private static String key(Plant plant) {
            Position at = plant.getPosition();
            return Math.round(at.y()) + "," + Math.round(at.x());
        }
    }

    private static final List<String> SHOOTERS = List.of(
            "Peashooter", "Repeater", "Threepeater", "Snow Pea", "Split Pea", "Fume-shroom",
            "Puff-shroom", "Cactus", "Fire Peashooter", "Starfruit");

    private static final List<String> LOBBERS_AND_MELEE = List.of(
            "Cabbage-pult", "Kernel-pult", "Melon-pult", "Bonk Choy", "Chomper", "Kiwibeast",
            "Phat Beet", "Iceberg Lettuce", "Wasabi Whip");

    private static final List<String> DEFENDERS_AND_BOMBS = List.of(
            "Wall-nut", "Tall-nut", "Garlic", "Endurian", "Explode-o-nut", "Sweet Potato",
            "Potato Mine", "Cherry Bomb", "Squash", "Jalapeno");

    private static final List<String> SUPPORT = List.of(
            "Sunflower", "Twin Sunflower", "Sun-shroom", "Torchwood", "Magnet-shroom",
            "Hypno-shroom", "Ice-shroom", "Sun Bean", "Grapeshot", "Bowling Bulb");

    @Test
    void everyShooterMirrorsOntoTheClientWhileItFights() {
        assertLoadoutMirrors(SHOOTERS, false);
    }

    @Test
    void everyLobberAndMeleePlantMirrorsOntoTheClientWhileItFights() {
        assertLoadoutMirrors(LOBBERS_AND_MELEE, false);
    }

    @Test
    void everyDefenderAndBombMirrorsOntoTheClientWhileItFights() {
        assertLoadoutMirrors(DEFENDERS_AND_BOMBS, false);
    }

    @Test
    void everySupportPlantMirrorsOntoTheClientWhileItFights() {
        assertLoadoutMirrors(SUPPORT, false);
    }

    @Test
    void everyShooterMirrorsOntoTheClientWhileBoostedByPlantFood() {
        assertLoadoutMirrors(SHOOTERS, true);
    }

    @Test
    void everyLobberAndMeleePlantMirrorsOntoTheClientWhileBoostedByPlantFood() {
        assertLoadoutMirrors(LOBBERS_AND_MELEE, true);
    }

    @Test
    void everyDefenderAndBombMirrorsOntoTheClientWhileBoostedByPlantFood() {
        assertLoadoutMirrors(DEFENDERS_AND_BOMBS, true);
    }

    @Test
    void everySupportPlantMirrorsOntoTheClientWhileBoostedByPlantFood() {
        assertLoadoutMirrors(SUPPORT, true);
    }

    private void assertLoadoutMirrors(List<String> plantNames, boolean feedPlantFood) {
        List<String> unplantable = new ArrayList<>();
        for (String name : plantNames) {
            Mirror mirror = new Mirror(List.of(name));
            mirror.clearLawnKeepingSunflowers();
            mirror.run(2);

            IZombieMatch.SeedCard card = mirror.match.findSeed(name);
            assertNotNull(card, name + " never made it into the online seed bank");
            mirror.earnSun(card.cost());

            String rejection = mirror.intent(Role.PLANTS, "PLANT", name, PLANT_ROW, PLANT_COL);
            if (rejection != null) {
                unplantable.add(name + ": " + rejection);
                continue;
            }
            mirror.intent(Role.ZOMBIES, "PLACE_ZOMBIE", "ZombieDefault", PLANT_ROW, ZOMBIE_COL);
            mirror.run(5);

            if (feedPlantFood) {
                Plant planted = mirror.server().getPlantAt(PLANT_ROW, PLANT_COL);
                if (planted != null && planted.getPlantFoodEffect() != null) {
                    mirror.server().setPlantFoodCount(5);
                    mirror.intent(Role.PLANTS, "USE_PLANT_FOOD", null, PLANT_ROW, PLANT_COL);
                }
            }

            mirror.run(150);
        }
        assertTrue(unplantable.isEmpty(), "some plants could not be placed: " + unplantable);
    }

    @Test
    void aPeaKeepsItsSourcePlantAndArtworkAcrossTheWire() {
        Mirror mirror = new Mirror(List.of("Snow Pea"));
        mirror.clearLawnKeepingSunflowers();
        mirror.earnSun(mirror.match.findSeed("Snow Pea").cost());
        assertNull(mirror.intent(Role.PLANTS, "PLANT", "Snow Pea", PLANT_ROW, PLANT_COL));
        assertNull(mirror.intent(Role.ZOMBIES, "PLACE_ZOMBIE", "ZombieDefault",
                PLANT_ROW, ZOMBIE_COL));

        boolean sawPea = false;
        for (int i = 0; i < 200 && !sawPea; i++) {
            mirror.step();
            mirror.assertMirrored();
            if (mirror.shadow.getProjectiles().isEmpty()) continue;
            sawPea = true;

            Projectile authoritative = mirror.server().getProjectiles().get(0);
            Projectile copy = mirror.shadow.getProjectiles().get(0);
            assertNotNull(copy.getSourcePlant(),
                    "the client must know which plant fired, or it cannot pick the pea art");
            assertEquals("Snow Pea", copy.getSourcePlant().getName());
            assertEquals(authoritative.getSourcePlant().isPlantFoodActive(),
                    copy.getSourcePlant().isPlantFoodActive(),
                    "the boosted-pea artwork is chosen off this flag");
            assertEquals(authoritative.getAssetVariant(), copy.getAssetVariant());
            assertEquals(authoritative.getSpeed().x(), copy.getSpeed().x(), 1.0e-6,
                    "a mirrored pea flies at the server's own speed");
            assertEquals(authoritative.getPosition().x(), copy.getPosition().x(), 0.4,
                    "a mirrored pea tracks the server's own position");
        }
        assertTrue(sawPea, "the Snow Pea never fired");
    }

    @Test
    void aBoostedPeashooterVolleyReachesTheClientIntact() {
        int plain = peasSeenByTheClient(false);
        int boosted = peasSeenByTheClient(true);
        assertTrue(boosted > plain,
                "the boosted plant must reach the client firing faster than the plain one: "
                        + plain + " -> " + boosted);
    }

    private int peasSeenByTheClient(boolean feedPlantFood) {
        Mirror mirror = new Mirror(List.of("Peashooter"));
        mirror.clearLawnKeepingSunflowers();
        mirror.earnSun(mirror.match.findSeed("Peashooter").cost());
        assertNull(mirror.intent(Role.PLANTS, "PLANT", "Peashooter", PLANT_ROW, PLANT_COL));
        assertNull(mirror.intent(Role.ZOMBIES, "PLACE_ZOMBIE", "ZombieArmor2",
                PLANT_ROW, ZOMBIE_COL));
        mirror.run(3);

        if (feedPlantFood) {
            mirror.server().setPlantFoodCount(1);
            assertNull(mirror.intent(Role.PLANTS, "USE_PLANT_FOOD", null, PLANT_ROW, PLANT_COL));
            Plant boostedPlant = mirror.server().getPlantAt(PLANT_ROW, PLANT_COL);
            assertTrue(boostedPlant.isPlantFoodActive(), "the peashooter refused the plant food");
        }

        Set<Projectile> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean sawBoost = false;
        for (int i = 0; i < 40; i++) {
            mirror.step();
            mirror.assertMirrored();
            seen.addAll(mirror.shadow.getProjectiles());
            Plant copy = mirror.shadow.getPlantAt(PLANT_ROW, PLANT_COL);
            if (copy != null && copy.isPlantFoodActive()) sawBoost = true;
        }
        assertEquals(feedPlantFood, sawBoost, "the client must see the boost exactly when it is on");
        return seen.size();
    }

    @Test
    void aThreepeaterVolleyArrivesAsThreeSeparatePeas() {
        Mirror mirror = new Mirror(List.of("Threepeater"));
        mirror.clearLawnKeepingSunflowers();
        mirror.earnSun(mirror.match.findSeed("Threepeater").cost());
        assertNull(mirror.intent(Role.PLANTS, "PLANT", "Threepeater", PLANT_ROW, PLANT_COL));

        String[] lanes = {"ZombieDefault", "ZombieArmor1", "ZombieArmor2"};
        for (int lane = 0; lane < lanes.length; lane++) {
            assertNull(mirror.intent(Role.ZOMBIES, "PLACE_ZOMBIE", lanes[lane],
                    PLANT_ROW - 1 + lane, ZOMBIE_COL));
        }

        int mostOnScreen = 0;
        for (int i = 0; i < 120; i++) {
            mirror.step();
            mirror.assertMirrored();
            mostOnScreen = Math.max(mostOnScreen, mirror.shadow.getProjectiles().size());
        }
        assertEquals(3, mostOnScreen,
                "all three lanes of the volley must reach the client at once");
    }

    @Test
    void anExplodingPotatoMineReachesTheClientWithItsFinalState() {
        Mirror mirror = new Mirror(List.of("Potato Mine"));
        mirror.clearLawnKeepingSunflowers();
        mirror.earnSun(mirror.match.findSeed("Potato Mine").cost());
        assertNull(mirror.intent(Role.PLANTS, "PLANT", "Potato Mine", PLANT_ROW, PLANT_COL));
        mirror.uprooted.clear();

        for (int i = 0; i < 600 && mirror.uprooted.isEmpty(); i++) {
            mirror.step();
            mirror.assertMirrored();
            if (i == 200) {
                mirror.intent(Role.ZOMBIES, "PLACE_ZOMBIE", "ZombieDefault",
                        PLANT_ROW, ZOMBIE_COL);
            }
        }

        assertFalse(mirror.uprooted.isEmpty(),
                "the client must be told the mine is gone so it can play the explosion");
        Plant gone = mirror.uprooted.get(0);
        assertEquals("Potato Mine", gone.getName());
        assertFalse(gone.wasPotatoMineEatenByZombie(),
                "a mine that detonated must not look like one that was eaten");
        assertFalse(mirror.shadow.getPlants().contains(gone),
                "the removed plant is off the lawn - the effect plays on its own");
    }

    @Test
    void aWallNutEatenByAZombieReachesTheClientAsAPlainDeath() {
        Mirror mirror = new Mirror(List.of("Wall-nut"));
        mirror.clearLawnKeepingSunflowers();
        mirror.earnSun(mirror.match.findSeed("Wall-nut").cost());
        assertNull(mirror.intent(Role.PLANTS, "PLANT", "Wall-nut", PLANT_ROW, PLANT_COL));
        assertNull(mirror.intent(Role.ZOMBIES, "PLACE_ZOMBIE", "ZombieArmor2",
                PLANT_ROW, ZOMBIE_COL));
        mirror.uprooted.clear();

        for (int i = 0; i < 4000 && mirror.uprooted.isEmpty(); i++) {
            mirror.step();
            mirror.assertMirrored();
        }
        assertFalse(mirror.uprooted.isEmpty(), "the wall-nut was never eaten");
        Plant gone = mirror.uprooted.get(0);
        assertEquals("Wall-nut", gone.getName());
        assertEquals(0, gone.getHP(), "an eaten plant reaches the client with its hp spent");
    }

    @Test
    void sunAndPlantFoodDropsAreVisibleToThePlantPlayer() {
        Mirror mirror = new Mirror(List.of("Peashooter"));
        boolean sawSun = false;
        boolean sawPlantFood = false;

        for (int i = 0; i < 400 && !(sawSun && sawPlantFood); i++) {
            if (i == 20) {
                GameSession.setCurrent(mirror.server());
                mirror.server().getItems().add(
                        new GroundPlantFood(new Position(PLANT_COL, PLANT_ROW)));
            }
            mirror.step();
            mirror.assertMirrored();
            for (Item item : mirror.shadow.getItems()) {
                if (item instanceof GroundSun) sawSun = true;
                if (item instanceof GroundPlantFood) sawPlantFood = true;
            }
        }

        assertTrue(sawSun, "sun produced by the plants never reached the client");
        assertTrue(sawPlantFood, "a plant food drop never reached the client");
        assertEquals(countCollectables(mirror.server()), mirror.shadow.getItems().size(),
                "the client mirrors exactly the collectables the server has");
    }

    @Test
    void anEndlessPlantFoodTimerSurvivesTheJsonWire() {
        Mirror mirror = new Mirror(List.of("Torchwood"));
        mirror.clearLawnKeepingSunflowers();
        mirror.earnSun(mirror.match.findSeed("Torchwood").cost());
        assertNull(mirror.intent(Role.PLANTS, "PLANT", "Torchwood", PLANT_ROW, PLANT_COL));
        mirror.run(2);

        mirror.server().setPlantFoodCount(1);
        assertNull(mirror.intent(Role.PLANTS, "USE_PLANT_FOOD", null, PLANT_ROW, PLANT_COL));
        Plant authoritative = mirror.server().getPlantAt(PLANT_ROW, PLANT_COL);
        assertTrue(Double.isInfinite(authoritative.getPlantFoodTimer()),
                "this test only means something while Torchwood's boost is endless");

        mirror.run(40);
        Plant copy = mirror.shadow.getPlantAt(PLANT_ROW, PLANT_COL);
        assertNotNull(copy, "an endless Plant Food timer must not break the snapshot");
        assertTrue(copy.isPlantFoodActive(), "the client keeps showing the boost");
    }

    private static int countCollectables(GameSession session) {
        int found = 0;
        for (Item item : session.getItems()) {
            if (item instanceof GroundItem ground && ground.isAlive() && !ground.isCollected()) {
                found++;
            }
        }
        return found;
    }
}
