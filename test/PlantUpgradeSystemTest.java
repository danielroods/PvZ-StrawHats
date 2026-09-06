import controller.CollectionManager;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.plant.PlantProgression;
import model.collections.plant.PlantStats;
import model.match_mechanisms.vector.Position;
import model.news.News;
import model.user_data.User;
import model.user_data.UserState;
import model.user_data.UserStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantUpgradeSystemTest {

    private static final class MemoryStore implements UserStore {
        private final List<User> saved = new ArrayList<>();
        int saveCount = 0;

        @Override public void load() { }

        @Override public void save() {
            saveCount++;
            saved.clear();
            saved.add(User.currentUser);
        }

        @Override public void addUser(User user) { }

        @Override public User findByUsername(String username) { return null; }

        @Override public boolean usernameExists(String username) { return false; }

        @Override public void setUser(User user) { User.currentUser = user; }

        UserState persistedState() {
            return saved.isEmpty() || saved.get(0) == null ? null : saved.get(0).userState;
        }
    }

    private MemoryStore store;
    private User previousUser;
    private CollectionManager manager;
    private UserState state;

    @BeforeEach
    void setUp() {
        PlantFactory.autoInit();
        previousUser = User.currentUser;
        store = new MemoryStore();
        User.useStore(store);

        User user = new User("upgrade-tester", "pw", "Tester", "t@example.com", "OTHER");
        user.userState = new UserState(new ArrayList<News>(), 0, 0, 0);
        User.currentUser = user;
        state = user.userState;
        manager = new CollectionManager();
    }

    @AfterEach
    void tearDown() {
        User.useLocalStore();
        User.currentUser = previousUser;
    }

    private PlantJsonParser.PlantConfig config(String name) {
        PlantJsonParser.PlantConfig config = manager.findPlant(name);
        assertNotNull(config, name + " must exist in the dataset");
        return config;
    }

    private void giveResources(PlantJsonParser.PlantConfig config, int coins, int packets) {
        state.coins = coins;
        state.seedPacketInventory.put(config.id, packets);
    }

    private void upgradeTo(PlantJsonParser.PlantConfig config, int targetLevel) {
        state.unlockPlant(config.id);
        while (PlantProgression.levelOf(state, config) < targetLevel) {
            int level = PlantProgression.levelOf(state, config);
            giveResources(config, PlantProgression.upgradeCoinCost(level),
                    PlantProgression.upgradePacketsRequired(level));
            assertTrue(manager.upgradePlant(state, config),
                    "upgrading " + config.name + " to level " + (level + 1) + " should succeed");
        }
    }

    @Test
    void everyPlantInTheDatasetDeclaresAMaxLevel() {
        for (PlantJsonParser.PlantConfig config : manager.getAllPlants()) {
            assertTrue(PlantProgression.maxLevel(config) >= 1,
                    config.name + " must have a usable max level");
        }
    }

    @Test
    void upgradingRaisesTheStoredLevelAndSpendsTheCost() {
        PlantJsonParser.PlantConfig peashooter = config("Peashooter");
        state.unlockPlant(peashooter.id);
        state.coins = 900;
        state.seedPacketInventory.put(peashooter.id, 4);

        assertTrue(manager.upgradePlant(state, peashooter));

        assertEquals(2, PlantProgression.levelOf(state, peashooter));
        assertEquals(400, state.coins);
        assertEquals(3, state.seedPacketInventory.get(peashooter.id));
    }

    @Test
    void upgradingIsRejectedWithoutEnoughCoinsOrPackets() {
        PlantJsonParser.PlantConfig peashooter = config("Peashooter");
        state.unlockPlant(peashooter.id);

        state.coins = 499;
        state.seedPacketInventory.put(peashooter.id, 5);
        assertFalse(manager.upgradePlant(state, peashooter), "not enough coins");

        state.coins = 5000;
        state.seedPacketInventory.put(peashooter.id, 0);
        assertFalse(manager.upgradePlant(state, peashooter), "not enough seed packets");

        assertEquals(1, PlantProgression.levelOf(state, peashooter));
        assertEquals(5000, state.coins);
    }

    @Test
    void aLockedPlantCannotBeUpgraded() {
        PlantJsonParser.PlantConfig snowPea = config("Snow Pea");
        state.coins = 99999;
        state.seedPacketInventory.put(snowPea.id, 99);

        assertFalse(manager.upgradePlant(state, snowPea));
        assertEquals(1, PlantProgression.levelOf(state, snowPea));
    }

    @Test
    void upgradingStopsAtTheMaxLevelAndSpendsNothingFurther() {
        PlantJsonParser.PlantConfig sunflower = config("Sunflower");
        int maxLevel = PlantProgression.maxLevel(sunflower);
        upgradeTo(sunflower, maxLevel);

        assertEquals(maxLevel, PlantProgression.levelOf(state, sunflower));

        state.coins = 99999;
        state.seedPacketInventory.put(sunflower.id, 99);
        assertFalse(manager.upgradePlant(state, sunflower), "a maxed plant refuses another upgrade");
        assertEquals(99999, state.coins, "a refused upgrade must not spend coins");
        assertEquals(99, state.seedPacketInventory.get(sunflower.id),
                "a refused upgrade must not spend packets");
        assertEquals(maxLevel, PlantProgression.levelOf(state, sunflower));
    }

    @Test
    void aLegacySaveStoredAboveTheMaxLevelReadsBackClamped() {
        PlantJsonParser.PlantConfig sunflower = config("Sunflower");
        state.unlockPlant(sunflower.id);
        state.setPlantLevel(sunflower.id, 7);

        int maxLevel = PlantProgression.maxLevel(sunflower);
        assertEquals(maxLevel, PlantProgression.levelOf(state, sunflower));
        assertEquals(PlantStats.of(sunflower, maxLevel).hp(),
                PlantProgression.statsFor(state, sunflower).hp());
    }

    @Test
    void everyUpgradeLevelChangesAtLeastOneStat() {
        for (PlantJsonParser.PlantConfig config : manager.getAllPlants()) {
            if ("Imitater".equalsIgnoreCase(config.name)) continue;
            int maxLevel = PlantProgression.maxLevel(config);
            for (int level = 2; level <= maxLevel; level++) {
                PlantStats previous = PlantStats.of(config, level - 1);
                PlantStats current = PlantStats.of(config, level);
                boolean changed = previous.hp() != current.hp()
                        || previous.cost() != current.cost()
                        || previous.damage() != current.damage()
                        || previous.recharge() != current.recharge()
                        || Math.abs(previous.actionInterval() - current.actionInterval()) > 1e-9
                        || Math.abs(previous.abilityValue() - current.abilityValue()) > 1e-9
                        || Math.abs(previous.attackRange() - current.attackRange()) > 1e-9
                        || Math.abs(previous.lifespan() - current.lifespan()) > 1e-9
                        || Math.abs(previous.plantFoodValue() - current.plantFoodValue()) > 1e-9
                        || !previous.specialTags().equals(current.specialTags());
                assertTrue(changed,
                        config.name + " level " + level + " must change something");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Peashooter", "Sunflower", "Wall-nut", "Repeater", "Snow Pea",
            "Cherry Bomb", "Twin Sunflower", "Melon-pult"})
    void plantedStatsMatchTheStoredLevelForEveryLevel(String plantName) {
        PlantJsonParser.PlantConfig config = config(plantName);
        int maxLevel = PlantProgression.maxLevel(config);

        for (int level = 1; level <= maxLevel; level++) {
            upgradeTo(config, level);

            PlantStats expected = PlantProgression.statsFor(state, config);
            assertEquals(level, expected.level(), plantName + " level bookkeeping");

            Plant planted = PlantFactory.createPlant(config.id,
                    PlantProgression.levelOf(state, config), new Position(1, 1));

            assertEquals(level, planted.getLevel(), plantName + " planted level");
            assertEquals(Math.max(1, expected.hp()), planted.getMaxHp(), plantName + " HP");
            assertEquals(expected.cost(), planted.getCost(), plantName + " sun cost");
            assertEquals(expected.damage(), planted.getDamage(), plantName + " damage");
            assertEquals(expected.recharge(), planted.getRecharge(), plantName + " recharge");
            assertEquals(expected.actionInterval(), planted.getActionInterval(), 1e-4,
                    plantName + " attack interval");
        }
    }

    @Test
    void aMaxedPeashooterIsStrictlyBetterThanAFreshOne() {
        PlantJsonParser.PlantConfig peashooter = config("Peashooter");
        Plant fresh = PlantFactory.createPlant(peashooter.id, 1, new Position(1, 1));

        upgradeTo(peashooter, PlantProgression.maxLevel(peashooter));
        Plant maxed = PlantFactory.createPlant(peashooter.id,
                PlantProgression.levelOf(state, peashooter), new Position(1, 1));

        assertTrue(maxed.getDamage() > fresh.getDamage(), "damage should grow");
        assertTrue(maxed.getMaxHp() > fresh.getMaxHp(), "HP should grow");
        assertTrue(maxed.getCost() < fresh.getCost(), "sun cost should drop");
    }

    @Test
    void wallNutGainsHpAndLosesRechargeAcrossItsLevels() {
        PlantJsonParser.PlantConfig wallNut = config("Wall-nut");

        assertEquals(4000, PlantStats.of(wallNut, 1).hp());
        assertEquals(5000, PlantStats.of(wallNut, 2).hp());
        assertEquals(20, PlantStats.of(wallNut, 2).recharge());
        assertEquals(15, PlantStats.of(wallNut, 3).recharge());
        assertEquals(6500, PlantStats.of(wallNut, 4).hp());
    }

    @Test
    void sunflowerProducesFasterAndGainsItsDoubleSunPerk() {
        PlantJsonParser.PlantConfig sunflower = config("Sunflower");

        assertEquals(24.0, PlantStats.of(sunflower, 1).actionInterval(), 1e-9);
        assertEquals(22.0, PlantStats.of(sunflower, 2).actionInterval(), 1e-9);
        assertFalse(PlantStats.of(sunflower, 3).hasSpecial("DOUBLE_SUN_CHANCE"));
        assertTrue(PlantStats.of(sunflower, 4).hasSpecial("DOUBLE_SUN_CHANCE"));

        Plant maxed = PlantFactory.createPlant(sunflower.id, 4, new Position(1, 1));
        assertTrue(maxed.hasSpecialUpgrade("DOUBLE_SUN_CHANCE"),
                "the planted sunflower must carry its level 4 perk");
    }

    @Test
    void potatoMineArmsFasterOnceUpgraded() {
        PlantJsonParser.PlantConfig mine = config("Potato Mine");

        Plant fresh = PlantFactory.createPlant(mine.id, 1, new Position(1, 1));
        Plant upgraded = PlantFactory.createPlant(mine.id, 2, new Position(1, 1));

        assertEquals(14.0, fresh.getIntervalTimer(), 1e-9);
        assertEquals(11.0, upgraded.getIntervalTimer(), 1e-9);
        assertTrue(upgraded.getIntervalTimer() < fresh.getIntervalTimer(),
                "the level 2 arm-time upgrade must shorten the fuse");
    }

    @Test
    void seaShroomKeepsItsExtendedLifespanAndRange() {
        PlantJsonParser.PlantConfig seaShroom = config("Sea-shroom");

        Plant fresh = PlantFactory.createPlant(seaShroom.id, 1, new Position(1, 1));
        Plant maxed = PlantFactory.createPlant(seaShroom.id,
                PlantProgression.maxLevel(seaShroom), new Position(1, 1));

        assertTrue(maxed.getLifespanSeconds() > fresh.getLifespanSeconds(),
                "LIFESPAN_EXT must extend the plant's life");
        assertTrue(maxed.getAttackRange() > fresh.getAttackRange(),
                "TILE_RANGE_EXT must extend the plant's range");
    }

    @Test
    void collectionAndBeforeMatchReadTheSameUpgradedStats() {
        PlantJsonParser.PlantConfig repeater = config("Repeater");
        upgradeTo(repeater, 3);

        PlantStats collectionView = PlantProgression.statsFor(state, repeater);
        PlantStats beforeMatchView = PlantStats.of(repeater,
                PlantProgression.levelOf(state, repeater));
        Plant inGame = PlantFactory.createPlant(repeater.id,
                PlantProgression.levelOf(state, repeater), new Position(1, 1));

        assertEquals(collectionView.hp(), beforeMatchView.hp());
        assertEquals(collectionView.damage(), beforeMatchView.damage());
        assertEquals(collectionView.cost(), beforeMatchView.cost());
        assertEquals(collectionView.recharge(), beforeMatchView.recharge());

        assertEquals(collectionView.hp(), inGame.getMaxHp());
        assertEquals(collectionView.damage(), inGame.getDamage());
        assertEquals(collectionView.cost(), inGame.getCost());
        assertEquals(collectionView.recharge(), inGame.getRecharge());
    }

    @Test
    void upgradesArePersistedImmediatelyAndSurviveReopeningAScreen() {
        PlantJsonParser.PlantConfig cabbage = config("Cabbage-pult");
        int savesBefore = store.saveCount;

        upgradeTo(cabbage, 3);

        assertTrue(store.saveCount > savesBefore, "each upgrade must persist the user state");
        UserState persisted = store.persistedState();
        assertNotNull(persisted);
        assertEquals(3, persisted.plantLevels.get(cabbage.id),
                "the saved state carries the new level");

        UserState reloaded = new UserState(new ArrayList<News>(), 0, 0, 0);
        reloaded.plantLevels.putAll(persisted.plantLevels);
        reloaded.unlockedPlantIds.addAll(persisted.unlockedPlantIds);
        reloaded.repair();

        assertEquals(3, PlantProgression.levelOf(reloaded, cabbage),
                "a state read back from disk keeps the upgrade");
        assertEquals(PlantStats.of(cabbage, 3).damage(),
                PlantProgression.statsFor(reloaded, cabbage).damage());
    }

    @Test
    void upgradingSeveralPlantsKeepsTheirLevelsIndependent() {
        PlantJsonParser.PlantConfig peashooter = config("Peashooter");
        PlantJsonParser.PlantConfig sunflower = config("Sunflower");
        PlantJsonParser.PlantConfig wallNut = config("Wall-nut");

        upgradeTo(peashooter, 4);
        upgradeTo(sunflower, 2);
        upgradeTo(wallNut, 3);

        assertEquals(4, PlantProgression.levelOf(state, peashooter));
        assertEquals(2, PlantProgression.levelOf(state, sunflower));
        assertEquals(3, PlantProgression.levelOf(state, wallNut));

        assertEquals(PlantStats.of(peashooter, 4).damage(),
                PlantFactory.createPlant(peashooter.id,
                        PlantProgression.levelOf(state, peashooter), new Position(1, 1)).getDamage());
        assertEquals(PlantStats.of(sunflower, 2).actionInterval(),
                PlantFactory.createPlant(sunflower.id,
                        PlantProgression.levelOf(state, sunflower),
                        new Position(1, 1)).getActionInterval(), 1e-9);
        assertEquals(PlantStats.of(wallNut, 3).hp(),
                PlantFactory.createPlant(wallNut.id,
                        PlantProgression.levelOf(state, wallNut), new Position(1, 1)).getMaxHp());
    }

    @Test
    void theImitaterHandsItsLevelToThePlantItCopies() {
        PlantJsonParser.PlantConfig imitater = config("Imitater");
        PlantJsonParser.PlantConfig peashooter = config("Peashooter");
        upgradeTo(imitater, PlantProgression.maxLevel(imitater));

        Plant planted = PlantFactory.createPlant(imitater.id,
                PlantProgression.levelOf(state, imitater), new Position(1, 1));
        assertEquals(PlantProgression.maxLevel(imitater), planted.getLevel(),
                "the Imitater carries the level it was upgraded to");

        Plant copy = PlantFactory.createPlant(peashooter.id, planted.getLevel(),
                new Position(1, 1));
        assertEquals(PlantStats.of(peashooter, planted.getLevel()).damage(), copy.getDamage(),
                "the copied plant is built at the Imitater's level");
    }

    @Test
    void theCollectionListingReportsTheUpgradedNumbers() {
        PlantJsonParser.PlantConfig peashooter = config("Peashooter");
        String atLevelOne = manager.formatPlant(peashooter, true, 1);
        upgradeTo(peashooter, 2);
        String atLevelTwo = manager.formatPlant(peashooter, true,
                PlantProgression.levelOf(state, peashooter));

        assertNotEquals(atLevelOne, atLevelTwo);
        assertTrue(atLevelTwo.contains("Damage: " + PlantStats.of(peashooter, 2).damage()));
        assertTrue(atLevelTwo.contains("Level: 2/" + PlantProgression.maxLevel(peashooter)));
    }
}
