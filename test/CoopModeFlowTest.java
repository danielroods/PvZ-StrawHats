import controller.CollectionManager;
import controller.match.BeforeMenu;
import controller.match.CoopBeforeMenu;
import controller.match.mini_games.CouchIZombieController;
import controller.ui_menus.MainMenu;
import model.App;
import model.collections.plant.PlantFactory;
import model.collections.zombie.ZombieFactory;
import model.match.mini_games.izombie.IZombieMatch;
import model.match.mini_games.izombie.IZombieMatch.Role;
import model.match.mini_games.izombie.ZombiePacket;
import model.user_data.User;
import model.user_data.UserState;
import model.utils.GameSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CoopModeFlowTest {

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

        App.currentMenu = new MainMenu();
        BeforeMenu.selectedPlants.clear();
        BeforeMenu.selectedZombies.clear();
    }

    @Test
    void theMainMenuCoopBannerReachesTheCoopLoadout() {
        
        App.currentMenu.handleCommand("menu enter coop");

        assertInstanceOf(CoopBeforeMenu.class, App.currentMenu,
                "the Co-op banner must land on the co-op loadout, not blow up with \"no such menu\"");
    }

    @Test
    void enteringCoopLeavesALevelLoadedSoTheLoadoutScreenCanDraw() {
        App.currentMenu.handleCommand("menu enter coop");

        assertNotNull(GameSession.peekInstance(), "co-op needs a session");
        assertNotNull(GameSession.peekInstance().getLevel(),
                "CoopBeforeMatchScreen bails out with \"No level loaded.\" without one");
        assertTrue(BeforeMenu.selectedPlants.isEmpty(), "a fresh co-op run starts with an empty loadout");
        assertTrue(BeforeMenu.selectedZombies.isEmpty(), "a fresh co-op run starts with an empty roster");
    }

    @Test
    void everyZombieThePickerAcceptsSurvivesIntoTheMatchRoster() {
        App.currentMenu.handleCommand("menu enter coop");
        CoopBeforeMenu loadout = (CoopBeforeMenu) App.currentMenu;
        loadout.handleCommand("add plant -t Peashooter");

        List<String> picked = new ArrayList<>(new CollectionManager().getAllZombieAliases())
                .subList(0, CoopBeforeMenu.ZOMBIE_SLOTS);
        for (String alias : picked) {
            loadout.handleCommand("add zombie -t " + alias);
        }
        assertEquals(CoopBeforeMenu.ZOMBIE_SLOTS, BeforeMenu.selectedZombies.size(),
                "the picker filled every slot it offered");

        loadout.handleCommand("start game");
        assertInstanceOf(CouchIZombieController.class, App.currentMenu,
                "co-op hands off to the Couch I, Zombie match");

        IZombieMatch match = ((CouchIZombieController) App.currentMenu).getMatch();
        assertEquals(picked.size(), match.getRoster().size(),
                "a slot the loadout accepts must reach the tray - a pick that is silently "
                        + "dropped here can never be placed in the match");
        for (String alias : picked) {
            assertNotNull(match.findPacket(alias), alias + " made it into the tray");
        }
    }

    @Test
    void theZombiePlayerCanDropEveryZombieTheCollectionOffers() {
        
        
        
        for (String alias : new CollectionManager().getAllZombieAliases()) {
            IZombieMatch match = new IZombieMatch(IZombieMatch.COUCH_MATCH_SECONDS,
                    List.of("Peashooter"), List.of(alias));
            GameSession.setCurrent(match.getSession());

            ZombiePacket packet = match.findPacket(alias);
            assertNotNull(packet, alias + " is offered by the picker, so it needs a packet");
            assertTrue(packet.isReady(), alias + " starts off cooldown");

            String rejection = match.applyIntent(Role.ZOMBIES, "PLACE_ZOMBIE",
                    alias, 2, IZombieMatch.COLS - 1);
            assertNull(rejection, "P2 must be able to drop " + alias);
            assertTrue(match.getSession().getZombies().size() >= 1,
                    alias + " reached the lawn");
        }
    }

    @Test
    void theLoadoutNeverOffersMoreSlotsThanTheMatchCanField() {
        assertEquals(IZombieMatch.MAX_ROSTER_SIZE, CoopBeforeMenu.ZOMBIE_SLOTS,
                "an extra loadout slot is a zombie the player picks and then never sees");
    }
}
