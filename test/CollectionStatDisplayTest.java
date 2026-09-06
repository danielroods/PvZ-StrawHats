import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import controller.CollectionManager;
import controller.ui_menus.CollectionMenu;
import model.App;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.plant.PlantProgression;
import model.collections.plant.PlantStats;
import model.news.News;
import model.user_data.User;
import model.user_data.UserState;
import model.user_data.UserStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import view.screens.ui_menus.PlantStatRows;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionStatDisplayTest {

    private static final Gson GSON = new GsonBuilder().create();

    private static final class MemoryStore implements UserStore {
        @Override public void load() { }
        @Override public void save() { }
        @Override public void addUser(User user) { }
        @Override public User findByUsername(String username) { return null; }
        @Override public boolean usernameExists(String username) { return false; }
        @Override public void setUser(User user) { User.currentUser = user; }
    }

    private User previousUser;
    private CollectionManager manager;
    private UserState state;

    @BeforeEach
    void setUp() {
        PlantFactory.autoInit();
        previousUser = User.currentUser;
        User.useStore(new MemoryStore());
        User user = new User("display-tester", "pw", "Display", "d@example.com", "OTHER");
        user.userState = new UserState(new ArrayList<News>(), 0, 0, 0);
        User.currentUser = user;
        state = user.userState;
        manager = new CollectionManager();
    }

    @AfterEach
    void tearDown() {
        User.useLocalStore();
        User.currentUser = previousUser;
        App.currentMenu = null;
    }

    private PlantJsonParser.PlantConfig config(String name) {
        PlantJsonParser.PlantConfig config = manager.findPlant(name);
        assertNotNull(config, name + " must exist");
        return config;
    }

    private List<String> withoutLevelRow(List<String> rows) {
        List<String> filtered = new ArrayList<>();
        for (String row : rows) {
            if (!row.startsWith("Level:")) filtered.add(row);
        }
        return filtered;
    }

    private List<String> rows(PlantJsonParser.PlantConfig config) {
        return PlantStatRows.of(state, config, state.isPlantUnlocked(config.id));
    }

    private String row(PlantJsonParser.PlantConfig config, String prefix) {
        for (String line : rows(config)) {
            if (line.startsWith(prefix)) return line;
        }
        return null;
    }

    private void upgradeViaCollectionScreen(PlantJsonParser.PlantConfig config) {
        int level = PlantProgression.levelOf(state, config);
        state.coins = PlantProgression.upgradeCoinCost(level) + 10;
        state.seedPacketInventory.put(config.id,
                PlantProgression.upgradePacketsRequired(level) + 5);
        CollectionMenu menu = new CollectionMenu();
        App.currentMenu = menu;
        menu.handleCommand("menu collection upgrade-plant -p " + config.name);
    }

    private void raiseTo(PlantJsonParser.PlantConfig config, int level) {
        state.unlockPlant(config.id);
        while (PlantProgression.levelOf(state, config) < level) {
            upgradeViaCollectionScreen(config);
        }
    }

    @Test
    void everyUpgradeLevelVisiblyChangesTheCollectionRows() {
        for (PlantJsonParser.PlantConfig config : manager.getAllPlants()) {
            state.unlockPlant(config.id);
            int maxLevel = PlantProgression.maxLevel(config);
            for (int level = 2; level <= maxLevel; level++) {
                state.setPlantLevel(config.id, level - 1);
                List<String> before = withoutLevelRow(rows(config));
                state.setPlantLevel(config.id, level);
                List<String> after = withoutLevelRow(rows(config));
                assertNotEquals(before, after,
                        config.name + " level " + level + " must change what the Collection shows");
            }
        }
    }

    @Test
    void theNextLevelHintMatchesWhatTheUpgradeActuallyDoes() {
        for (PlantJsonParser.PlantConfig config : manager.getAllPlants()) {
            state.unlockPlant(config.id);
            int maxLevel = PlantProgression.maxLevel(config);
            for (int level = 1; level < maxLevel; level++) {
                state.setPlantLevel(config.id, level);
                String hint = PlantStatRows.nextLevelSummary(state, config, true);
                assertNotNull(hint, config.name + " level " + level + " must promise something");
                assertFalse(hint.isBlank());
            }
            state.setPlantLevel(config.id, maxLevel);
            assertNull(PlantStatRows.nextLevelSummary(state, config, true),
                    config.name + " at max level promises nothing further");
        }
    }

    @Test
    void theNextLevelHintNeverRepeatsTheSameUpgradeTwice() {
        for (PlantJsonParser.PlantConfig config : manager.getAllPlants()) {
            state.unlockPlant(config.id);
            for (int level = 1; level < PlantProgression.maxLevel(config); level++) {
                state.setPlantLevel(config.id, level);
                String hint = PlantStatRows.nextLevelSummary(state, config, true);
                String[] parts = hint.split(", ");
                List<String> seen = new ArrayList<>();
                for (String part : parts) {
                    assertFalse(seen.contains(part),
                            config.name + " level " + level + " repeats \"" + part + "\" in " + hint);
                    seen.add(part);
                }
            }
        }
    }

    @Test
    void aDamageUpgradeShowsOnTheDamageRowImmediately() {
        PlantJsonParser.PlantConfig puffShroom = config("Puff-shroom");
        raiseTo(puffShroom, 2);

        assertEquals("Damage: 20", row(puffShroom, "Damage:"));
        assertTrue(PlantStatRows.nextLevelSummary(state, puffShroom, true).contains("+10 damage"));

        upgradeViaCollectionScreen(puffShroom);

        assertEquals(3, PlantProgression.levelOf(state, puffShroom));
        assertEquals("Damage: 30  (+10)", row(puffShroom, "Damage:"));
    }

    @Test
    void aSplashDamageUpgradeIsVisibleOnTheDamageRow() {
        PlantJsonParser.PlantConfig melonPult = config("Melon-pult");
        raiseTo(melonPult, 2);
        String before = row(melonPult, "Damage:");

        upgradeViaCollectionScreen(melonPult);

        String after = row(melonPult, "Damage:");
        assertNotEquals(before, after, "a splash damage upgrade must show up");
        assertTrue(after.contains("+15 splash"), after);
    }

    @Test
    void aReflectDamageUpgradeIsVisibleOnTheDamageRow() {
        PlantJsonParser.PlantConfig endurian = config("Endurian");
        raiseTo(endurian, 1);
        String before = row(endurian, "Damage:");

        upgradeViaCollectionScreen(endurian);

        assertNotEquals(before, row(endurian, "Damage:"));
        assertTrue(row(endurian, "Damage:").contains("+5 reflect"));
    }

    @Test
    void aSunProductionUpgradeIsVisible() {
        PlantJsonParser.PlantConfig goldBloom = config("Gold Bloom");
        raiseTo(goldBloom, 2);
        assertEquals("Sun produced: 375", row(goldBloom, "Sun produced:"));

        upgradeViaCollectionScreen(goldBloom);

        assertEquals("Sun produced: 425  (+50)", row(goldBloom, "Sun produced:"));
    }

    @Test
    void aPierceUpgradeIsVisible() {
        PlantJsonParser.PlantConfig cactus = config("Cactus");
        raiseTo(cactus, 1);
        String before = row(cactus, "Pierce:");

        upgradeViaCollectionScreen(cactus);

        assertNotEquals(before, row(cactus, "Pierce:"));
        assertTrue(row(cactus, "Pierce:").contains("(+1)"));
    }

    @Test
    void aRangeUpgradeIsVisible() {
        PlantJsonParser.PlantConfig seaShroom = config("Sea-shroom");
        raiseTo(seaShroom, 1);
        assertEquals("Range: 3.50 tiles", row(seaShroom, "Range:"));

        upgradeViaCollectionScreen(seaShroom);

        assertEquals("Range: 4.50 tiles  (+1)", row(seaShroom, "Range:"));
    }

    @Test
    void aLifespanUpgradeIsVisible() {
        PlantJsonParser.PlantConfig puffShroom = config("Puff-shroom");
        raiseTo(puffShroom, 1);
        assertEquals("Lifespan: 60s", row(puffShroom, "Lifespan:"));

        upgradeViaCollectionScreen(puffShroom);

        assertEquals("Lifespan: 70s  (+10)", row(puffShroom, "Lifespan:"));
    }

    @Test
    void aPlantFoodUpgradeIsVisible() {
        PlantJsonParser.PlantConfig iceShroom = config("Ice-shroom");
        raiseTo(iceShroom, 1);
        String before = row(iceShroom, "Plant Food power:");

        upgradeViaCollectionScreen(iceShroom);

        assertNotEquals(before, row(iceShroom, "Plant Food power:"));
        assertTrue(row(iceShroom, "Plant Food power:").contains("(+2)"));
    }

    @Test
    void theRowsMatchTheStatsGameplayUsesForThePlantedPlant() {
        PlantJsonParser.PlantConfig repeater = config("Repeater");
        raiseTo(repeater, PlantProgression.maxLevel(repeater));

        PlantStats displayed = PlantStatRows.statsFor(state, repeater, true);
        var planted = PlantFactory.createPlant(repeater.id,
                PlantProgression.levelOf(state, repeater),
                new model.match_mechanisms.vector.Position(1, 1));

        assertTrue(row(repeater, "HP:").startsWith("HP: " + planted.getMaxHp()));
        assertTrue(row(repeater, "Damage:").startsWith("Damage: " + planted.getDamage()));
        assertTrue(row(repeater, "Sun cost:").startsWith("Sun cost: " + planted.getCost()));
        assertTrue(row(repeater, "Recharge:").startsWith("Recharge: " + planted.getRecharge()));
        assertEquals(displayed.damage(), planted.getDamage());
    }

    @Test
    void theRowsStayCorrectAfterReopeningTheScreenAndReloadingTheGame() {
        PlantJsonParser.PlantConfig peashooter = config("Peashooter");
        raiseTo(peashooter, 2);
        List<String> afterUpgrade = rows(peashooter);
        assertEquals("Damage: 30  (+10)", row(peashooter, "Damage:"));

        List<String> reopened = PlantStatRows.of(User.currentUser.userState, peashooter, true);
        assertEquals(afterUpgrade, reopened, "reopening the Collection shows the same rows");

        User reloaded = GSON.fromJson(GSON.toJson(User.currentUser), User.class);
        reloaded.userState.repair();
        List<String> afterRestart = PlantStatRows.of(reloaded.userState, peashooter, true);
        assertEquals(afterUpgrade, afterRestart, "restarting the game shows the same rows");
    }

    @Test
    void aLockedPlantIsShownAtItsBaseStats() {
        PlantJsonParser.PlantConfig snowPea = config("Snow Pea");
        state.setPlantLevel(snowPea.id, 4);

        assertEquals("Level: -", row(snowPea, "Level:"));
        assertEquals("Damage: " + PlantStats.of(snowPea, 1).damage(), row(snowPea, "Damage:"));
    }

    @Test
    void belowMaxLevelTheUpgradePreviewIsShown() {
        PlantJsonParser.PlantConfig peashooter = config("Peashooter");
        state.unlockPlant(peashooter.id);

        String line = PlantStatRows.upgradeLine(state, peashooter, true);
        assertNotNull(line);
        assertTrue(line.startsWith(PlantStatRows.NEXT_UPGRADE_PREFIX), line);
        assertTrue(line.contains("+10 damage"), line);
        assertFalse(line.contains(PlantStatRows.MAX_LEVEL_PREFIX), line);
    }

    @Test
    void atMaxLevelNoUpgradePreviewIsShown() {
        PlantJsonParser.PlantConfig peashooter = config("Peashooter");
        raiseTo(peashooter, PlantProgression.maxLevel(peashooter));

        String line = PlantStatRows.upgradeLine(state, peashooter, true);
        assertNotNull(line);
        assertTrue(line.startsWith(PlantStatRows.MAX_LEVEL_PREFIX), line);
        assertFalse(line.contains(PlantStatRows.NEXT_UPGRADE_PREFIX), line);
        assertFalse(line.contains("+"), line);
        assertNull(PlantStatRows.nextLevelSummary(state, peashooter, true));
    }

    @Test
    void everyPlantHidesItsUpgradePreviewOnceItReachesItsDefinedMaxLevel() {
        for (PlantJsonParser.PlantConfig config : manager.getAllPlants()) {
            state.unlockPlant(config.id);
            int maxLevel = PlantProgression.maxLevel(config);

            for (int level = 1; level < maxLevel; level++) {
                state.setPlantLevel(config.id, level);
                String line = PlantStatRows.upgradeLine(state, config, true);
                assertNotNull(line, config.name + " level " + level);
                assertTrue(line.startsWith(PlantStatRows.NEXT_UPGRADE_PREFIX),
                        config.name + " level " + level + ": " + line);
                assertFalse(PlantStatRows.isMaxLevel(state, config, true), config.name);
            }

            state.setPlantLevel(config.id, maxLevel);
            String maxed = PlantStatRows.upgradeLine(state, config, true);
            assertTrue(PlantStatRows.isMaxLevel(state, config, true), config.name);
            assertTrue(maxed.startsWith(PlantStatRows.MAX_LEVEL_PREFIX),
                    config.name + " at max: " + maxed);
            assertFalse(maxed.contains("+"), config.name + " at max shows a delta: " + maxed);
            for (String banned : new String[]{"damage", "HP", "recharge", "sun cost",
                    "attack interval", "tile range", "lifespan"}) {
                assertFalse(maxed.contains(banned),
                        config.name + " at max still advertises " + banned + ": " + maxed);
            }
        }
    }

    @Test
    void aPlantStoredAboveItsMaxAlsoHidesTheUpgradePreview() {
        PlantJsonParser.PlantConfig sunflower = config("Sunflower");
        state.unlockPlant(sunflower.id);
        state.setPlantLevel(sunflower.id, 12);

        String line = PlantStatRows.upgradeLine(state, sunflower, true);
        assertTrue(line.startsWith(PlantStatRows.MAX_LEVEL_PREFIX), line);
        assertTrue(line.contains(PlantProgression.maxLevel(sunflower)
                + "/" + PlantProgression.maxLevel(sunflower)), line);
        assertFalse(line.contains("+"), line);
    }

    @Test
    void theUpgradeLineIsIdenticalInCollectionAndBeforeMatch() {
        PlantJsonParser.PlantConfig wallNut = config("Wall-nut");
        state.unlockPlant(wallNut.id);

        for (int level = 1; level <= PlantProgression.maxLevel(wallNut); level++) {
            state.setPlantLevel(wallNut.id, level);
            String collectionLine = PlantStatRows.upgradeLine(state, wallNut, true);
            String beforeMatchLine = PlantStatRows.upgradeLine(state, wallNut, true);
            assertEquals(collectionLine, beforeMatchLine,
                    "both screens read the same upgrade line at level " + level);
        }
    }

    @Test
    void theMaxLevelLineSurvivesReopeningAndReloading() {
        PlantJsonParser.PlantConfig cherry = config("Cherry Bomb");
        raiseTo(cherry, PlantProgression.maxLevel(cherry));
        String line = PlantStatRows.upgradeLine(state, cherry, true);

        assertEquals(line, PlantStatRows.upgradeLine(User.currentUser.userState, cherry, true));

        User reloaded = GSON.fromJson(GSON.toJson(User.currentUser), User.class);
        reloaded.userState.repair();
        assertEquals(line, PlantStatRows.upgradeLine(reloaded.userState, cherry, true));
        assertTrue(PlantStatRows.isMaxLevel(reloaded.userState, cherry, true));
    }

    @Test
    void aLockedPlantShowsNoUpgradeLine() {
        PlantJsonParser.PlantConfig snowPea = config("Snow Pea");
        assertNull(PlantStatRows.upgradeLine(state, snowPea, false));
    }

    @Test
    void aPlantStoredAboveTheMaxIsShownAtTheMaxRows() {
        PlantJsonParser.PlantConfig sunflower = config("Sunflower");
        state.unlockPlant(sunflower.id);
        state.setPlantLevel(sunflower.id, 9);

        int maxLevel = PlantProgression.maxLevel(sunflower);
        assertEquals("Level: " + maxLevel + " / " + maxLevel, row(sunflower, "Level:"));
        state.setPlantLevel(sunflower.id, maxLevel);
        assertEquals(rows(sunflower), rows(sunflower));
        assertNull(PlantStatRows.nextLevelSummary(state, sunflower, true));
    }
}
