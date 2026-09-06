import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import controller.CollectionManager;
import controller.match.BeforeMenu;
import controller.ui_menus.CollectionMenu;
import model.App;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.plant.PlantProgression;
import model.collections.plant.PlantStats;
import model.game_exceptions.GameException;
import model.match.main.levels.normal_levels.NormalLevel;
import model.match_mechanisms.vector.Position;
import model.news.News;
import model.user_data.User;
import model.user_data.UserState;
import model.user_data.UserStore;
import model.utils.GameSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantUpgradeScreenFlowTest {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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

        User user = new User("flow-tester", "pw", "Flow", "flow@example.com", "OTHER");
        user.userState = new UserState(new ArrayList<News>(), 0, 0, 0);
        User.currentUser = user;
        state = user.userState;
        manager = new CollectionManager();

        GameSession.setCurrent(new GameSession(5, 9));
        GameSession.getInstance().setLevel(new NormalLevel());
        BeforeMenu.selectedPlants = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        User.useLocalStore();
        User.currentUser = previousUser;
        App.currentMenu = null;
        BeforeMenu.selectedPlants = new ArrayList<>();
    }

    private PlantJsonParser.PlantConfig config(String name) {
        PlantJsonParser.PlantConfig config = manager.findPlant(name);
        assertNotNull(config, name + " must exist");
        return config;
    }

    private void fundNextUpgrade(PlantJsonParser.PlantConfig config) {
        int level = PlantProgression.levelOf(state, config);
        state.coins = PlantProgression.upgradeCoinCost(level) + 10;
        state.seedPacketInventory.put(config.id,
                PlantProgression.upgradePacketsRequired(level) + 2);
    }

    private void collectionUpgrade(PlantJsonParser.PlantConfig config) {
        CollectionMenu menu = new CollectionMenu();
        App.currentMenu = menu;
        menu.handleCommand("menu collection upgrade-plant -p " + config.name);
    }

    private void beforeMatchUpgrade(PlantJsonParser.PlantConfig config) {
        BeforeMenu menu = new BeforeMenu();
        App.currentMenu = menu;
        menu.handleCommand("upgrade plant -t " + config.name);
    }

    @Test
    void theCollectionScreenCommandRaisesTheLevelUsedInGameplay() {
        PlantJsonParser.PlantConfig peashooter = config("Peashooter");
        state.unlockPlant(peashooter.id);
        fundNextUpgrade(peashooter);

        collectionUpgrade(peashooter);

        assertEquals(2, PlantProgression.levelOf(state, peashooter));
        Plant planted = PlantFactory.createPlant(peashooter.id,
                PlantProgression.levelOf(state, peashooter), new Position(1, 1));
        assertEquals(PlantStats.of(peashooter, 2).damage(), planted.getDamage());
    }

    @Test
    void theBeforeMatchScreenCommandRaisesTheSameLevel() {
        PlantJsonParser.PlantConfig sunflower = config("Sunflower");
        state.unlockPlant(sunflower.id);
        fundNextUpgrade(sunflower);

        beforeMatchUpgrade(sunflower);

        assertEquals(2, PlantProgression.levelOf(state, sunflower));
        assertEquals(PlantStats.of(sunflower, 2).actionInterval(),
                PlantProgression.statsFor(state, sunflower).actionInterval(), 1e-9);
    }

    @Test
    void bothScreensAdvanceOneSharedLevelCounter() {
        PlantJsonParser.PlantConfig wallNut = config("Wall-nut");
        state.unlockPlant(wallNut.id);

        fundNextUpgrade(wallNut);
        collectionUpgrade(wallNut);
        assertEquals(2, PlantProgression.levelOf(state, wallNut));

        fundNextUpgrade(wallNut);
        beforeMatchUpgrade(wallNut);
        assertEquals(3, PlantProgression.levelOf(state, wallNut),
                "the Before-Match upgrade continues from where Collection left off");

        fundNextUpgrade(wallNut);
        collectionUpgrade(wallNut);
        assertEquals(4, PlantProgression.levelOf(state, wallNut));
    }

    @Test
    void reopeningEitherScreenStillSeesTheUpgrade() {
        PlantJsonParser.PlantConfig repeater = config("Repeater");
        state.unlockPlant(repeater.id);
        fundNextUpgrade(repeater);
        collectionUpgrade(repeater);

        CollectionManager reopenedCollection = new CollectionManager();
        PlantJsonParser.PlantConfig reread = reopenedCollection.findPlant("Repeater");
        assertEquals(2, PlantProgression.levelOf(state, reread));

        BeforeMenu reopenedBeforeMatch = new BeforeMenu();
        App.currentMenu = reopenedBeforeMatch;
        assertEquals(2, PlantProgression.levelOf(state, config("Repeater")));
        assertEquals(PlantStats.of(repeater, 2).hp(),
                PlantProgression.statsFor(state, repeater).hp());
        assertNotNull(reopenedBeforeMatch);
    }

    @Test
    void aRestartedGameReloadsTheUpgradedLevels() {
        PlantJsonParser.PlantConfig peashooter = config("Peashooter");
        PlantJsonParser.PlantConfig sunflower = config("Sunflower");
        state.unlockPlant(peashooter.id);
        state.unlockPlant(sunflower.id);

        for (int i = 0; i < 3; i++) {
            fundNextUpgrade(peashooter);
            collectionUpgrade(peashooter);
        }
        fundNextUpgrade(sunflower);
        beforeMatchUpgrade(sunflower);

        String onDisk = GSON.toJson(User.currentUser);
        User reloaded = GSON.fromJson(onDisk, User.class);
        assertNotNull(reloaded);
        reloaded.userState.repair();

        assertEquals(4, PlantProgression.levelOf(reloaded.userState, peashooter));
        assertEquals(2, PlantProgression.levelOf(reloaded.userState, sunflower));

        Plant plantedAfterRestart = PlantFactory.createPlant(peashooter.id,
                PlantProgression.levelOf(reloaded.userState, peashooter), new Position(1, 1));
        assertEquals(PlantStats.of(peashooter, 4).damage(), plantedAfterRestart.getDamage());
        assertEquals(PlantStats.of(peashooter, 4).cost(), plantedAfterRestart.getCost());
    }

    @Test
    void bothScreensRefuseToUpgradePastTheMaxLevel() {
        PlantJsonParser.PlantConfig cherry = config("Cherry Bomb");
        state.unlockPlant(cherry.id);
        int maxLevel = PlantProgression.maxLevel(cherry);
        while (PlantProgression.levelOf(state, cherry) < maxLevel) {
            fundNextUpgrade(cherry);
            collectionUpgrade(cherry);
        }

        state.coins = 100000;
        state.seedPacketInventory.put(cherry.id, 100);

        assertThrows(GameException.class, () -> collectionUpgrade(cherry),
                "the Collection screen must refuse a max-level upgrade");
        assertThrows(GameException.class, () -> beforeMatchUpgrade(cherry),
                "the Before-Match screen must refuse a max-level upgrade");

        assertEquals(100000, state.coins);
        assertEquals(100, state.seedPacketInventory.get(cherry.id));
        assertEquals(maxLevel, PlantProgression.levelOf(state, cherry));
    }

    @Test
    void aMatchPlantsTheUpgradedPlantAndChargesItsUpgradedCost() {
        PlantJsonParser.PlantConfig peashooter = config("Peashooter");
        state.unlockPlant(peashooter.id);
        int maxLevel = PlantProgression.maxLevel(peashooter);
        while (PlantProgression.levelOf(state, peashooter) < maxLevel) {
            fundNextUpgrade(peashooter);
            collectionUpgrade(peashooter);
        }

        BeforeMenu.selectedPlants.add(peashooter.name);
        GameSession session = GameSession.getInstance();
        session.addSun(1000);
        int sunBefore = session.getSunCount();

        controller.match.GameplayMenu gameplay = new controller.match.GameplayMenu();
        App.currentMenu = gameplay;
        gameplay.handleCommand("plant plant -t Peashooter -l (2, 3)");

        Plant onBoard = session.getEnvironment().getCell(2, 1).getPlant();
        assertNotNull(onBoard, "the plant should be on the lawn");
        assertEquals(maxLevel, onBoard.getLevel(), "the planted copy uses the upgraded level");
        assertEquals(PlantStats.of(peashooter, maxLevel).damage(), onBoard.getDamage());
        assertEquals(PlantStats.of(peashooter, maxLevel).hp(), onBoard.getMaxHp());

        int upgradedCost = PlantStats.of(peashooter, maxLevel).cost();
        assertEquals(sunBefore - upgradedCost, session.getSunCount(),
                "the match must charge the upgraded sun cost, not the base one");
        assertTrue(upgradedCost < PlantStats.of(peashooter, 1).cost());
    }

    @Test
    void aLegacyOverMaxSaveIsNotChargedForAnotherUpgrade() {
        PlantJsonParser.PlantConfig sunflower = config("Sunflower");
        state.unlockPlant(sunflower.id);
        state.setPlantLevel(sunflower.id, 9);
        state.coins = 50000;
        state.seedPacketInventory.put(sunflower.id, 50);

        assertThrows(GameException.class, () -> collectionUpgrade(sunflower));
        assertEquals(50000, state.coins);
        assertEquals(PlantProgression.maxLevel(sunflower),
                PlantProgression.levelOf(state, sunflower));
        assertFalse(manager.upgradePlant(state, sunflower));
    }
}
