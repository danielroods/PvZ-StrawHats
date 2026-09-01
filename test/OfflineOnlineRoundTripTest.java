import com.google.gson.JsonObject;
import model.collections.plant.PlantFactory;
import model.collections.zombie.ZombieFactory;
import model.user_data.User;
import model.user_data.UserState;
import net.Envelope;
import net.JsonLine;
import net.Protocol;
import net.client.ServerConnection;
import net.dto.AccountDto;
import net.server.GameServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineOnlineRoundTripTest {

    private static final long TIMEOUT_MILLIS = 15_000L;

    private static GameServer server;
    private static Path dataDirectory;
    private static int port;

    @BeforeAll
    static void startServer() throws Exception {
        PlantFactory.autoInit();
        ZombieFactory.init();

        dataDirectory = Files.createTempDirectory("pvz-roundtrip-test");
        server = new GameServer(0, dataDirectory.toFile());
        Thread thread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception ignored) {
            }
        }, "roundtrip-server");
        thread.setDaemon(true);
        thread.start();

        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline && !server.isRunning()) {
            Thread.sleep(10);
        }
        assertTrue(server.isRunning());
        port = server.getPort();
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (server != null) server.stop();
        if (dataDirectory != null) {
            try (var paths = Files.walk(dataDirectory)) {
                paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> path.toFile().delete());
            }
        }
    }

    private static JsonObject syncPayload(User user) {
        JsonObject payload = new JsonObject();
        payload.add("account", JsonLine.toTree(AccountDto.of(user)));
        payload.add("userState", JsonLine.toTree(user.userState));
        return payload;
    }

    private static void playOffline(User user, int coinsEarned) {
        user.userState.coins += coinsEarned;
        user.userState.markSaved();
    }

    private static Envelope await(ServerConnection connection, Predicate<Envelope> test)
            throws Exception {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            Envelope envelope = connection.poll();
            if (envelope != null && test.test(envelope)) return envelope;
            if (envelope == null) Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for a reply.");
    }

    private static Envelope request(ServerConnection connection, String type, JsonObject payload)
            throws Exception {
        long id = connection.send(type, payload);
        Envelope reply = await(connection, envelope -> envelope.re != null && envelope.re == id);
        assertEquals(Protocol.OK, reply.t, "expected OK: " + reply.getString("message"));
        return reply;
    }

    private static ServerConnection connect() throws Exception {
        ServerConnection connection = new ServerConnection("127.0.0.1", port);
        request(connection, Protocol.HELLO, Envelope.obj("protocolVersion", Protocol.VERSION));
        return connection;
    }

    private static void adopt(User user, Envelope reply) {
        AccountDto dto = JsonLine.fromTree(reply.getObject("account"), AccountDto.class);
        UserState state = JsonLine.fromTree(reply.getObject("userState"), UserState.class);
        boolean stayLoggedIn = user.stayLoggedIn;
        dto.applyTo(user);
        user.userState = state;
        user.stayLoggedIn = stayLoggedIn;
        user.syncedPasswordHash = user.passwordHash;
    }

    @Test
    void oneAccountSurvivesGoingOnlineOfflineAndOnlineAgain() throws Exception {
        User user = new User("chopper", "Passw0rd!", "Chopper", "chopper@example.com", "male");
        user.stayLoggedIn = true;
        String accountId = user.accountId();

        playOffline(user, 500);

        ServerConnection first = connect();
        Envelope signIn = request(first, Protocol.SIGN_IN, syncPayload(user));
        adopt(user, signIn);
        assertEquals(500, user.userState.coins, "offline progress is uploaded, not discarded");
        assertEquals(accountId, user.accountId(), "still the same account");
        assertTrue(user.stayLoggedIn, "going online does not sign the player out of the game");
        assertEquals(500, server.accounts().stateOf("chopper").coins);

        playOffline(user, 250);
        adopt(user, request(first, Protocol.STATE_SYNC, syncPayload(user)));
        assertEquals(750, server.accounts().stateOf("chopper").coins,
                "saves made while online reach the server too");

        first.close();

        playOffline(user, 100);
        assertEquals(850, user.userState.coins,
                "losing the server leaves the game playing on the local copy");

        ServerConnection second = connect();
        adopt(user, request(second, Protocol.SIGN_IN, syncPayload(user)));
        assertEquals(850, user.userState.coins, "reconnecting uploads what happened offline");
        assertEquals(850, server.accounts().stateOf("chopper").coins);
        assertEquals(1, server.accounts().snapshot().stream()
                        .filter(stored -> accountId.equals(stored.accountId())).count(),
                "reconnecting must never create a second account");
        assertEquals(1, server.accounts().snapshot().stream()
                        .filter(stored -> "chopper".equalsIgnoreCase(stored.username)).count());
        second.close();
    }

    @Test
    void aPasswordChangedOfflineStillProvesTheAccountIsYours() throws Exception {
        User user = new User("jinbe", "Passw0rd!", "Jinbe", "jinbe@example.com", "male");
        user.userState.markSaved();

        ServerConnection first = connect();
        adopt(user, request(first, Protocol.SIGN_IN, syncPayload(user)));
        first.close();

        user.setPassword("Fishman1!");
        playOffline(user, 40);

        ServerConnection second = connect();
        Envelope reply = request(second, Protocol.SIGN_IN, syncPayload(user));
        adopt(user, reply);
        assertEquals(40, user.userState.coins);
        assertTrue(server.accounts().find("jinbe").checkPassword("Fishman1!"),
                "the new password is the one the server now holds");
        second.close();

        User impostor = new User("jinbe", "Guessing1!", "Nobody", "nobody@example.com", "male");
        impostor.userState.markSaved();
        impostor.userState.markSaved();
        impostor.userState.markSaved();
        ServerConnection third = connect();
        long id = third.send(Protocol.SIGN_IN, syncPayload(impostor));
        Envelope refused = await(third, envelope -> envelope.re != null && envelope.re == id);
        assertEquals(Protocol.ERR, refused.t);
        assertEquals(Protocol.ERR_BAD_CREDENTIALS, refused.getString("code"),
                "claiming a username you cannot authenticate never takes over the account");
        assertEquals(40, server.accounts().stateOf("jinbe").coins);
        third.close();
    }

    @Test
    void anAccountTheServerHasNeverSeenIsSimplyTakenOn() throws Exception {
        User user = new User("bepo", "Passw0rd!", "Bepo", "bepo@example.com", "male");
        user.userState = new UserState(new ArrayList<>(), 12, 3, 900);
        user.userState.markSaved();

        ServerConnection connection = connect();
        Envelope reply = request(connection, Protocol.SIGN_IN, syncPayload(user));
        assertNotNull(reply.getObject("account"),
                "no registration step - the offline account is the online account");

        UserState served = JsonLine.fromTree(reply.getObject("userState"), UserState.class);
        assertEquals(900, served.coins);
        assertEquals(12, served.lastLevel);
        assertEquals(3, served.diamonds);
        connection.close();
    }
}
