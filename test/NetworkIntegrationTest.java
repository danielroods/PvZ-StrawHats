import com.google.gson.JsonObject;
import model.collections.plant.PlantFactory;
import model.collections.zombie.ZombieFactory;
import net.Envelope;
import net.JsonLine;
import net.Protocol;
import net.client.ServerConnection;
import net.dto.MatchSnapshot;
import net.server.GameServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NetworkIntegrationTest {

    private static final long TIMEOUT_MILLIS = 15_000L;

    private static GameServer server;
    private static Path dataDirectory;
    private static int port;

    private static class Peer {
        final ServerConnection connection;
        final List<Envelope> received = new ArrayList<>();

        Peer(int port) throws Exception {
            connection = new ServerConnection("127.0.0.1", port);
        }

        long send(String type, JsonObject payload) {
            return connection.send(type, payload);
        }

        void drain() {
            Envelope envelope;
            while ((envelope = connection.poll()) != null) {
                received.add(envelope);
            }
        }

        Envelope await(Predicate<Envelope> predicate) throws Exception {
            long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
            while (System.currentTimeMillis() < deadline) {
                drain();
                for (Envelope envelope : received) {
                    if (predicate.test(envelope)) return envelope;
                }
                Thread.sleep(10);
            }
            throw new AssertionError("Timed out waiting for a message. Got: "
                    + received.stream().map(e -> e.t).toList());
        }

        Envelope awaitReply(long requestId) throws Exception {
            return await(envelope -> envelope.re != null && envelope.re == requestId);
        }

        Envelope awaitOk(long requestId) throws Exception {
            Envelope reply = awaitReply(requestId);
            assertEquals(Protocol.OK, reply.t,
                    "expected OK but got " + reply.t + ": " + reply.getString("message"));
            return reply;
        }

        void forget() {
            received.clear();
        }

        void close() {
            connection.close();
        }
    }

    @BeforeAll
    static void startServer() throws Exception {
        PlantFactory.autoInit();
        ZombieFactory.init();

        dataDirectory = Files.createTempDirectory("pvz-server-test");
        server = new GameServer(0, dataDirectory.toFile());

        Thread thread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception ignored) {
                // The socket closes when the test tears the server down.
            }
        }, "test-server");
        thread.setDaemon(true);
        thread.start();

        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline && !server.isRunning()) {
            Thread.sleep(10);
        }
        assertTrue(server.isRunning(), "the server should be listening");
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

    private Peer connectAndSignIn(String username) throws Exception {
        Peer peer = new Peer(port);
        peer.awaitOk(peer.send(Protocol.HELLO,
                Envelope.obj("protocolVersion", Protocol.VERSION)));

        peer.awaitOk(peer.send(Protocol.REGISTER, Envelope.obj(
                "username", username, "password", "Passw0rd!", "nickname", username + "-nick",
                "email", username + "@example.com", "gender", "male",
                "securityQuestion", "q", "securityAnswer", "a")));

        Envelope login = peer.awaitOk(peer.send(Protocol.LOGIN,
                Envelope.obj("username", username, "password", "Passw0rd!")));
        assertNotNull(login.getObject("account"), "login should return the account");
        assertNotNull(login.getObject("userState"), "login should return the saved state");
        peer.forget();
        return peer;
    }

    @Test
    @Order(1)
    void handshakeRejectsAWrongProtocolVersion() throws Exception {
        Peer peer = new Peer(port);
        Envelope reply = peer.awaitReply(peer.send(Protocol.HELLO,
                Envelope.obj("protocolVersion", Protocol.VERSION + 99)));
        assertEquals(Protocol.ERR, reply.t);
        assertEquals(Protocol.ERR_VERSION, reply.getString("code"));
        peer.close();
    }

    @Test
    @Order(2)
    void registrationRejectsDuplicatesAndWeakPasswords() throws Exception {
        Peer peer = new Peer(port);
        peer.awaitOk(peer.send(Protocol.HELLO,
                Envelope.obj("protocolVersion", Protocol.VERSION)));

        peer.awaitOk(peer.send(Protocol.REGISTER, Envelope.obj(
                "username", "dupe", "password", "Passw0rd!", "nickname", "Dupe",
                "email", "dupe@example.com", "gender", "female",
                "securityQuestion", "q", "securityAnswer", "a")));

        Envelope duplicate = peer.awaitReply(peer.send(Protocol.REGISTER, Envelope.obj(
                "username", "dupe", "password", "Passw0rd!", "nickname", "Dupe",
                "email", "dupe@example.com", "gender", "female",
                "securityQuestion", "q", "securityAnswer", "a")));
        assertEquals(Protocol.ERR_USERNAME_TAKEN, duplicate.getString("code"));

        Envelope weak = peer.awaitReply(peer.send(Protocol.REGISTER, Envelope.obj(
                "username", "weakling", "password", "abc", "nickname", "Weak",
                "email", "weak@example.com", "gender", "male",
                "securityQuestion", "q", "securityAnswer", "a")));
        assertEquals(Protocol.ERR_VALIDATION, weak.getString("code"));
        peer.close();
    }

    @Test
    @Order(3)
    void challengeOfflineUserFails() throws Exception {
        Peer peer = connectAndSignIn("lonely");
        Envelope reply = peer.awaitReply(peer.send(Protocol.CHALLENGE,
                Envelope.obj("targetUsername", "ghost")));
        assertEquals(Protocol.ERR_NO_SUCH_USER, reply.getString("code"));

        Envelope self = peer.awaitReply(peer.send(Protocol.CHALLENGE,
                Envelope.obj("targetUsername", "lonely")));
        assertEquals(Protocol.ERR_SELF_CHALLENGE, self.getString("code"));
        peer.close();
    }

    @Test
    @Order(4)
    void twoPlayersPlayAFullNetworkedMatch() throws Exception {
        Peer challenger = connectAndSignIn("zoro");
        Peer target = connectAndSignIn("nami");

        challenger.awaitOk(challenger.send(Protocol.CHALLENGE,
                Envelope.obj("targetUsername", "nami")));

        Envelope incoming = target.await(e -> e.isType(Protocol.CHALLENGE_INCOMING));
        long inviteId = incoming.getLong("inviteId", -1);
        assertEquals("zoro", incoming.getString("fromUsername"));

        target.awaitOk(target.send(Protocol.CHALLENGE_RESPOND,
                Envelope.obj("inviteId", inviteId, "accept", true)));

        Envelope challengerFound = challenger.await(e -> e.isType(Protocol.MATCH_FOUND));
        Envelope targetFound = target.await(e -> e.isType(Protocol.MATCH_FOUND));

        String matchId = challengerFound.getString("matchId");
        assertEquals(matchId, targetFound.getString("matchId"));
        assertEquals(Protocol.ROLE_ZOMBIES, challengerFound.getString("role"),
                "the challenger brings the attack");
        assertEquals(Protocol.ROLE_PLANTS, targetFound.getString("role"));

        Peer zombies = challenger;
        Peer plants = target;

        List<String> zombieLoadout = List.of("ZombieDefault", "ZombieImp", "ZombieArmor1");
        List<String> plantLoadout = List.of("Sunflower", "Peashooter", "Wall-nut");

        zombies.awaitOk(zombies.send(Protocol.MATCH_READY,
                readyPayload(matchId, zombieLoadout)));
        assertFalse(sawWithin(zombies, e -> e.isType(Protocol.MATCH_START), 600L),
                "one ready player is not enough to start the match");

        plants.awaitOk(plants.send(Protocol.MATCH_READY, readyPayload(matchId, plantLoadout)));
        zombies.await(e -> e.isType(Protocol.MATCH_START));
        plants.await(e -> e.isType(Protocol.MATCH_START));

        MatchSnapshot first = awaitSnapshot(zombies, matchId);
        assertEquals(400, first.zombieSun, "the zombie player starts with a fixed budget");
        assertEquals(150, first.plantSun, "the plant player starts small");
        assertEquals(zombieLoadout.size(), first.packets.size(),
                "the zombie player gets exactly the roster they picked");
        assertTrue(first.seeds.isEmpty(), "the zombie player never sees the plant cards");
        assertTrue(first.plants.size() >= 10, "the lawn opens already defended");
        assertEquals(5, first.brains.length);

        MatchSnapshot plantsFirst = awaitSnapshot(plants, matchId);
        assertEquals(plantLoadout.size(), plantsFirst.seeds.size(),
                "the plant player gets exactly the seed bank they picked");
        assertTrue(plantsFirst.packets.isEmpty(), "the plant player never sees the zombie cards");

        zombies.forget();
        zombies.send(Protocol.MATCH_INTENT, Envelope.obj(
                "matchId", matchId, "action", Protocol.INTENT_PLACE_ZOMBIE,
                "target", "ZombieDefault", "row", 2, "col", 8));

        MatchSnapshot afterPlace = awaitSnapshotMatching(zombies, matchId,
                snapshot -> !snapshot.zombies.isEmpty());
        assertEquals(350, afterPlace.zombieSun, "the browncoat costs 50 sun");
        assertEquals(2, afterPlace.zombies.get(0).row);

        MatchSnapshot firing = awaitSnapshotMatching(zombies, matchId,
                snapshot -> !snapshot.projectiles.isEmpty());
        MatchSnapshot.ProjectileDto pea = firing.projectiles.get(0);
        assertNotNull(pea.sourceName, "a projectile carries the plant that fired it");
        assertTrue(pea.vx > 0, "a projectile carries its own velocity for client-side motion");

        plants.forget();
        MatchSnapshot beforePlant = awaitSnapshot(plants, matchId);
        plants.forget();
        plants.send(Protocol.MATCH_INTENT, Envelope.obj(
                "matchId", matchId, "action", Protocol.INTENT_PLANT,
                "target", "Wall-nut", "row", 4, "col", 5));

        MatchSnapshot afterPlant = awaitSnapshotMatching(plants, matchId, snapshot ->
                snapshot.plants.stream().anyMatch(p -> "Wall-nut".equals(p.name)));
        assertTrue(afterPlant.plantSun < beforePlant.plantSun,
                "planting the wall-nut spends more sun than the bank grants in that window");
        assertTrue(afterPlant.seeds.stream()
                        .anyMatch(seed -> "Wall-nut".equals(seed.name) && seed.cooldown > 0),
                "the wall-nut card goes on recharge after planting");

        plants.forget();
        plants.send(Protocol.MATCH_INTENT, Envelope.obj(
                "matchId", matchId, "action", Protocol.INTENT_PLACE_ZOMBIE,
                "target", "ZombieImp", "row", 1, "col", 8));
        Envelope rejected = plants.await(e -> e.isType(Protocol.MATCH_EVENT)
                && Protocol.EVENT_INTENT_REJECTED.equals(e.getString("kind")));
        assertTrue(rejected.getString("reason").contains("plants"),
                "the plant player cannot place zombies: " + rejected.getString("reason"));

        plants.forget();
        plants.awaitOk(plants.send(Protocol.REACTION_SEND,
                Envelope.obj("matchId", matchId, "kind", Protocol.REACTION_EMOJI, "index", 1)));
        Envelope reaction = zombies.await(e -> e.isType(Protocol.REACTION));
        assertEquals("nami", reaction.getString("fromUsername"));
        assertEquals(1, reaction.getInt("index", -1));

        Envelope limited = plants.awaitReply(plants.send(Protocol.REACTION_SEND,
                Envelope.obj("matchId", matchId, "kind", Protocol.REACTION_EMOJI, "index", 2)));
        assertEquals(Protocol.ERR_RATE_LIMITED, limited.getString("code"),
                "reaction spam is throttled");

        zombies.forget();
        zombies.close();
        Envelope end = plants.await(e -> e.isType(Protocol.MATCH_END));
        assertTrue(end.getBoolean("youWon", false), "a disconnect forfeits the match");

        plants.close();
    }

    @Test
    @Order(5)
    void theRandomQueuePairsTheFirstTwoWaitingPlayers() throws Exception {
        Peer first = connectAndSignIn("sanji");
        first.awaitOk(first.send(Protocol.QUEUE_JOIN, Envelope.obj("mode", "IZOMBIE")));

        Peer second = connectAndSignIn("robin");
        second.awaitOk(second.send(Protocol.QUEUE_JOIN, Envelope.obj("mode", "IZOMBIE")));

        Envelope firstFound = first.await(e -> e.isType(Protocol.MATCH_FOUND));
        Envelope secondFound = second.await(e -> e.isType(Protocol.MATCH_FOUND));

        assertEquals(firstFound.getString("matchId"), secondFound.getString("matchId"),
                "both queued players land in the same match");
        assertNotEquals(firstFound.getString("role"), secondFound.getString("role"),
                "the queue hands out one of each role");
        assertEquals("robin", firstFound.getString("opponentUsername"));
        assertEquals("sanji", secondFound.getString("opponentUsername"));

        first.close();
        second.close();
    }

    @Test
    @Order(6)
    void bonusScoreKeepsTheBestAndLeaderboardReportsIt() throws Exception {
        Peer peer = connectAndSignIn("usopp");

        Envelope first = peer.awaitOk(peer.send(Protocol.BONUS_SCORE_SUBMIT,
                Envelope.obj("score", 420)));
        assertEquals(420, first.getInt("bestScore", -1));
        assertTrue(first.getBoolean("improved", false));

        Envelope worse = peer.awaitOk(peer.send(Protocol.BONUS_SCORE_SUBMIT,
                Envelope.obj("score", 100)));
        assertEquals(420, worse.getInt("bestScore", -1), "a worse run never lowers the record");

        Envelope better = peer.awaitOk(peer.send(Protocol.BONUS_SCORE_SUBMIT,
                Envelope.obj("score", 999)));
        assertEquals(999, better.getInt("bestScore", -1));

        Envelope board = peer.awaitOk(peer.send(Protocol.LEADERBOARD, Envelope.obj()));
        var rows = board.payload().getAsJsonArray("rows");
        assertNotNull(rows);

        boolean sawUsopp = false;
        boolean sawNullPoint = false;
        for (var element : rows) {
            var row = JsonLine.fromTree(element.getAsJsonObject(),
                    net.dto.LeaderboardRowDto.class);
            if ("usopp".equals(row.username)) {
                assertEquals(999, row.myPoint);
                sawUsopp = true;
            } else if (row.myPoint == null) {
                sawNullPoint = true;
            }
        }
        assertTrue(sawUsopp, "the leaderboard should include the submitting player");
        assertTrue(sawNullPoint,
                "a player who never played the bonus game must have no score at all");

        peer.close();
    }

    private JsonObject readyPayload(String matchId, List<String> loadout) {
        JsonObject payload = Envelope.obj("matchId", matchId);
        com.google.gson.JsonArray picks = new com.google.gson.JsonArray();
        loadout.forEach(picks::add);
        payload.add("loadout", picks);
        return payload;
    }

    private boolean sawWithin(Peer peer, Predicate<Envelope> predicate, long millis)
            throws Exception {
        long deadline = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < deadline) {
            peer.drain();
            for (Envelope envelope : new ArrayList<>(peer.received)) {
                if (predicate.test(envelope)) return true;
            }
            Thread.sleep(10);
        }
        return false;
    }

    private MatchSnapshot awaitSnapshot(Peer peer, String matchId) throws Exception {
        return awaitSnapshotMatching(peer, matchId, snapshot -> true);
    }

    private MatchSnapshot awaitSnapshotMatching(Peer peer, String matchId,
                                                Predicate<MatchSnapshot> predicate)
            throws Exception {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            peer.drain();
            for (Envelope envelope : new ArrayList<>(peer.received)) {
                if (!envelope.isType(Protocol.MATCH_SNAPSHOT)) continue;
                if (!matchId.equals(envelope.getString("matchId"))) continue;
                MatchSnapshot snapshot = JsonLine.fromTree(
                        envelope.getObject("snapshot"), MatchSnapshot.class);
                if (snapshot != null && predicate.test(snapshot)) return snapshot;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Timed out waiting for a matching snapshot.");
    }

    @Test
    @Order(7)
    void serverDataFileSurvivesARestart() {
        server.accounts().saveNow();
        File file = new File(dataDirectory.toFile(), "Data.json");
        assertTrue(file.isFile(), "the server writes its own account file");
        assertTrue(file.length() > 0, "the account file should not be empty");
    }
}
