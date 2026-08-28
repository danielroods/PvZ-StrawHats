import model.collections.plant.PlantFactory;
import model.collections.zombie.ZombieFactory;
import model.match.mini_games.izombie.IZombie;
import model.user_data.LocalUserStore;
import model.user_data.User;
import model.user_data.UserState;
import model.user_data.UserStore;
import net.client.NetworkClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineModePreservedTest {

    @BeforeEach
    void setUp() {
        PlantFactory.autoInit();
        ZombieFactory.init();
        User.useLocalStore();
    }

    @AfterEach
    void tearDown() {
        User.useLocalStore();
    }

    @Test
    void theGameDefaultsToTheLocalStore() {
        assertFalse(User.isRemote(), "a fresh client plays offline against Data.json");
        assertTrue(User.store() instanceof LocalUserStore);
    }

    @Test
    void swappingStoresIsReversible() {
        UserStore local = User.store();
        UserStore fake = new LocalUserStore();

        User.useStore(fake);
        assertSame(fake, User.store());
        assertTrue(User.isRemote(), "any non-default store counts as remote");

        User.useLocalStore();
        assertSame(local, User.store(), "signing out restores the original local store");
        assertFalse(User.isRemote());
    }

    @Test
    void anUnconnectedClientNeverBlocksOrThrows() {
        NetworkClient client = NetworkClient.get();
        assertFalse(client.isConnected());
        assertFalse(client.isSignedIn());

        client.pump();
        client.fireAndForget("ANYTHING", net.Envelope.obj());
        client.markStateDirty();
        client.pushStateNow();
        client.leaveQueue();
        client.clearMatchState();

        assertFalse(client.isConnected(), "still offline, still fine");
        assertNull(client.getMatchState());
    }

    @Test
    void singlePlayerIZombieStillRunsUnchanged() {
        User previous = User.currentUser;
        try {
            User user = new User("offline-tester", "Passw0rd!", "Tester",
                    "t@example.com", "male");
            user.userState = new UserState(new ArrayList<>(), 0, 0, 0);
            User.currentUser = user;

            IZombie game = new IZombie(1);
            assertEquals(3000, game.getStartingSun(), "level 1 keeps its sun budget");
            assertEquals(5, game.getBrainCount());
            assertEquals(5, game.getRedLineColumn());
            assertFalse(game.getRoster().isEmpty());
            assertNotNull(game.getSession());

            assertTrue(game.placeZombie("ZombieDefault", 2),
                    "the single-player game still accepts a placement");

            for (int tick = 0; tick < 200 && !game.isFinished(); tick++) {
                game.tick(service.GameClock.SECONDS_PER_TICK);
            }
            assertTrue(game.getElapsedSeconds() > 0);
        } finally {
            User.currentUser = previous;
        }
    }

    @Test
    void bonusScoreStartsUnsetAndOnlyRisesWithBetterRuns() {
        UserState state = new UserState(new ArrayList<>(), 0, 0, 0);
        assertNull(state.bonusHighScore, "a player who never played has no score at all");
        assertFalse(state.hasBonusScore());

        assertTrue(state.recordBonusScore(300));
        assertEquals(300, state.bonusHighScore);

        assertFalse(state.recordBonusScore(120), "a worse run never lowers the record");
        assertEquals(300, state.bonusHighScore);

        assertTrue(state.recordBonusScore(750));
        assertEquals(750, state.bonusHighScore);
    }

    @Test
    void winningAnAdventureMatchNoLongerFakesAScore() {
        UserState state = new UserState(new ArrayList<>(), 0, 0, 0);
        state.recordGameResult(4);
        state.recordGameResult(7);

        assertEquals(2, state.gamesPlayed);
        assertEquals(7, state.lastLevel);
        assertNull(state.bonusHighScore,
                "adventure rewards must not populate the My Point column");
    }
}
