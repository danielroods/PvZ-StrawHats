import controller.CollectionManager;
import controller.match.BeforeMenu;
import controller.match.CoopBeforeMenu;
import controller.match.NetBeforeMenu;
import model.collections.plant.PlantFactory;
import model.collections.zombie.ZombieFactory;
import model.game_exceptions.GameException;
import model.match.main.levels.normal_levels.NormalLevel;
import model.user_data.User;
import model.user_data.UserState;
import model.utils.GameSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedMatchLoadoutTest {

    private static final String LOCKED_PLANT = "Peashooter";

    @BeforeAll
    static void initFactories() {
        PlantFactory.autoInit();
        ZombieFactory.init();
    }

    @BeforeEach
    void freshBrandNewAccount() {
        User user = new User("rookie", "", "Rookie", "rookie@example.com", "male");
        user.userState = new UserState(new ArrayList<>(), 0, 0, 0);
        User.currentUser = user;

        NormalLevel level = new NormalLevel();
        level.setName("Shared");
        level.setZombiePool(new ArrayList<>());
        new GameSession().setLevel(level);

        BeforeMenu.selectedPlants.clear();
        BeforeMenu.selectedZombies.clear();
    }

    @Test
    void aSoloLoadoutStillRespectsTheAccountUnlocks() {
        assertTrue(!User.currentUser.userState.isPlantUnlocked(
                        PlantFactory.findPlantIdByName(LOCKED_PLANT)),
                "the test relies on this plant being locked for a brand new account");

        BeforeMenu solo = new BeforeMenu();
        assertThrows(GameException.class,
                () -> solo.handleCommand("add plant -t " + LOCKED_PLANT),
                "single player must still buy its plants");
    }

    @Test
    void coopOpensEveryPlantRegardlessOfTheAccount() {
        new CoopBeforeMenu().handleCommand("add plant -t " + LOCKED_PLANT);
        assertTrue(BeforeMenu.selectedPlants.contains(LOCKED_PLANT));
    }

    @Test
    void onlineOpensEveryPlantRegardlessOfTheAccount() {
        new NetBeforeMenu(null).handleCommand("add plant -t " + LOCKED_PLANT);
        assertTrue(BeforeMenu.selectedPlants.contains(LOCKED_PLANT));
    }

    @Test
    void bothSharedModesOfferEveryZombieRegardlessOfTheAccount() {
        CollectionManager manager = new CollectionManager();
        assertTrue(manager.getSeenZombieAliases(User.currentUser.userState).isEmpty(),
                "a brand new account has met no zombies yet");

        CoopBeforeMenu coop = new CoopBeforeMenu();
        for (String alias : manager.getAllZombieAliases()) {
            coop.handleCommand("add zombie -t " + alias);
            assertTrue(BeforeMenu.selectedZombies.contains(alias));
            coop.handleCommand("remove zombie -t " + alias);
        }
    }
}
