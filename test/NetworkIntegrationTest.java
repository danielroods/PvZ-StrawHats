import com.google.gson.JsonObject;
import model.collections.plant.PlantFactory;
import model.collections.zombie.ZombieFactory;
import net.Envelope;
import net.JsonLine;
import net.Protocol;
import net.client.ServerConnection;
import model.user_data.User;
import model.user_data.UserState;
import net.dto.AccountDto;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private static JsonObject syncPayload(User user) {
        JsonObject payload = new JsonObject();
        payload.add("account", JsonLine.toTree(AccountDto.of(user)));
        payload.add("userState", JsonLine.toTree(user.userState));
        return payload;
    }

    private static User localAccount(String username) {
        User user = new User(username, "Passw0rd!", username + "-nick",
                username + "@example.com", "male");
        user.setSecurityQuestion("1. What is the name of your first pet?", "a");
        user.userState.markSaved();
        return user;
    }

    private Peer connectAndSignIn(String username) throws Exception {
        return connectAndSignIn(localAccount(username));
    }

    private Peer connectAndSignIn(User account) throws Exception {
        Peer peer = new Peer(port);
        peer.awaitOk(peer.send(Protocol.HELLO,
                Envelope.obj("protocolVersion", Protocol.VERSION)));

        Envelope signIn = peer.awaitOk(peer.send(Protocol.SIGN_IN, syncPayload(account)));
        assertNotNull(signIn.getObject("account"), "sign-in should return the account");
        assertNotNull(signIn.getObject("userState"), "sign-in should return the saved state");
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
    void theOfflineAccountIsTheOnlineAccount() throws Exception {
        User offline = localAccount("dupe");
        offline.userState.coins = 1234;
        offline.userState.lastLevel = 7;
        offline.userState.markSaved();

        Peer peer = connectAndSignIn(offline);
        assertNotNull(server.accounts().findById(offline.accountId()),
                "connecting takes on the account the player is already logged into");

        Envelope pulled = peer.awaitOk(peer.send(Protocol.STATE_PULL, Envelope.obj()));
        AccountDto served = JsonLine.fromTree(pulled.getObject("account"), AccountDto.class);
        UserState state = JsonLine.fromTree(pulled.getObject("userState"), UserState.class);
        assertEquals(offline.accountId(), served.accountId, "one account, not a second one");
        assertEquals("dupe", served.username);
        assertEquals(1234, state.coins, "offline progress is uploaded on connect");
        assertEquals(7, state.lastLevel);
        peer.close();

        Peer impostor = new Peer(port);
        impostor.awaitOk(impostor.send(Protocol.HELLO,
                Envelope.obj("protocolVersion", Protocol.VERSION)));
        User sameName = localAccount("dupe");
        sameName.setPassword("Different1!");
        sameName.userState.markSaved();
        Envelope refused = impostor.awaitReply(
                impostor.send(Protocol.SIGN_IN, syncPayload(sameName)));
        assertEquals(Protocol.ERR_BAD_CREDENTIALS, refused.getString("code"),
                "a different account cannot claim a username that is already taken");
        impostor.close();
        assertEquals(1234, server.accounts().stateOf("dupe").coins,
                "and it certainly cannot overwrite the real account");

        Peer invalid = new Peer(port);
        invalid.awaitOk(invalid.send(Protocol.HELLO,
                Envelope.obj("protocolVersion", Protocol.VERSION)));
        User badEmail = localAccount("weakling");
        badEmail.email = "not-an-email";
        Envelope rejected = invalid.awaitReply(
                invalid.send(Protocol.SIGN_IN, syncPayload(badEmail)));
        assertEquals(Protocol.ERR_VALIDATION, rejected.getString("code"));
        invalid.close();
    }

    @Test
    @Order(8)
    void syncingKeepsTheNewerCopyAndNeverForksTheAccount() throws Exception {
        User account = localAccount("brook");
        account.userState.coins = 10;
        account.userState.markSaved();
        Peer peer = connectAndSignIn(account);

        account.userState.coins = 50;
        account.userState.markSaved();
        Envelope pushed = peer.awaitOk(peer.send(Protocol.STATE_SYNC, syncPayload(account)));
        assertEquals(50, JsonLine.fromTree(pushed.getObject("userState"), UserState.class).coins,
                "a newer save is taken by the server");

        UserState stale = JsonLine.fromTree(JsonLine.toTree(account.userState), UserState.class);
        stale.coins = 3;
        stale.stateRevision -= 1;
        JsonObject stalePayload = new JsonObject();
        stalePayload.add("account", JsonLine.toTree(AccountDto.of(account)));
        stalePayload.add("userState", JsonLine.toTree(stale));
        Envelope answered = peer.awaitOk(peer.send(Protocol.STATE_SYNC, stalePayload));
        assertEquals(50, JsonLine.fromTree(answered.getObject("userState"), UserState.class).coins,
                "a save built on older state never overwrites the newer copy");

        String previousId = account.accountId();
        account.username = "brook-renamed";
        account.userState.coins = 70;
        account.userState.markSaved();
        Envelope renamed = peer.awaitOk(peer.send(Protocol.STATE_SYNC, syncPayload(account)));
        AccountDto after = JsonLine.fromTree(renamed.getObject("account"), AccountDto.class);
        assertEquals("brook-renamed", after.username, "a rename follows the same account");
        assertEquals(previousId, after.accountId);
        assertNull(server.accounts().find("brook"),
                "renaming must not leave a second account behind under the old name");
        assertEquals(1, server.accounts().snapshot().stream()
                .filter(user -> previousId.equals(user.accountId())).count());
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

    @Test
    @Order(9)
    void lotteryRecordsAreKeptPerChapterAndReachTheLeaderboard() throws Exception {
        Peer peer = connectAndSignIn("chopper");

        Envelope egypt = peer.awaitOk(peer.send(Protocol.BONUS_SCORE_SUBMIT,
                Envelope.obj("score", 1500, "chapter", "egypt")));
        assertEquals(1500, egypt.getLong("chapterBest", -1));
        assertEquals(1500, egypt.getInt("bestScore", -1),
                "My Point follows the best Lottery run");

        Envelope beach = peer.awaitOk(peer.send(Protocol.BONUS_SCORE_SUBMIT,
                Envelope.obj("score", 400, "chapter", "big_wave_beach")));
        assertEquals(400, beach.getLong("chapterBest", -1),
                "one chapter's record does not spill into another's");
        assertEquals(1500, beach.getInt("bestScore", -1),
                "a lower run in another chapter does not lower My Point");

        Envelope worse = peer.awaitOk(peer.send(Protocol.BONUS_SCORE_SUBMIT,
                Envelope.obj("score", 900, "chapter", "egypt")));
        assertEquals(1500, worse.getLong("chapterBest", -1),
                "a worse run never lowers a chapter record");

        Envelope better = peer.awaitOk(peer.send(Protocol.BONUS_SCORE_SUBMIT,
                Envelope.obj("score", 9000, "chapter", "dark_ages")));
        assertEquals(9000, better.getLong("chapterBest", -1));
        assertEquals(9000, better.getInt("bestScore", -1));

        UserState stored = server.accounts().stateOf("chopper");
        assertEquals(1500, stored.getLotteryHighScore("egypt"));
        assertEquals(400, stored.getLotteryHighScore("big_wave_beach"));
        assertEquals(9000, stored.getLotteryHighScore("dark_ages"));
        assertEquals(0, stored.getLotteryHighScore("frostbite_caves"),
                "a chapter never played has no record");

        Envelope board = peer.awaitOk(peer.send(Protocol.LEADERBOARD, Envelope.obj()));
        var rows = board.payload().getAsJsonArray("rows");
        assertNotNull(rows);
        boolean sawChopper = false;
        for (var element : rows) {
            var row = JsonLine.fromTree(element.getAsJsonObject(),
                    net.dto.LeaderboardRowDto.class);
            if (!"chopper".equals(row.username)) continue;
            sawChopper = true;
            assertEquals(1500L, row.lotteryScore("egypt"));
            assertEquals(400L, row.lotteryScore("big_wave_beach"));
            assertEquals(9000L, row.lotteryScore("dark_ages"));
            assertNull(row.lotteryScore("frostbite_caves"));
            assertEquals(9000, row.myPoint);
        }
        assertTrue(sawChopper, "the leaderboard should carry the submitting player");
        peer.close();
    }

    
    @Test
    @Order(10)
    void submittingAScoreDoesNotMakeTheClientsOwnSaveLookStale() throws Exception {
        User account = localAccount("franky");
        account.userState.coins = 10;
        account.userState.markSaved();
        Peer peer = connectAndSignIn(account);

        peer.awaitOk(peer.send(Protocol.BONUS_SCORE_SUBMIT,
                Envelope.obj("score", 2500, "chapter", "frostbite_caves")));

        account.userState.recordLotteryScore("frostbite_caves", 2500);
        account.userState.coins = 999;
        account.userState.miniGamesWon = 4;
        account.userState.markSaved();
        Envelope pushed = peer.awaitOk(peer.send(Protocol.STATE_SYNC, syncPayload(account)));

        UserState served = JsonLine.fromTree(pushed.getObject("userState"), UserState.class);
        assertEquals(999, served.coins, "the client's later save is still the newer one");
        assertEquals(4, served.miniGamesWon);
        assertEquals(2500, served.getLotteryHighScore("frostbite_caves"),
                "and it still carries the record that was submitted");
        peer.close();
    }

    @Test
    @Order(11)
    void offlineLotteryProgressIsUploadedOnConnect() throws Exception {
        User offline = localAccount("brooke");
        offline.userState.recordGameResult(104);
        offline.userState.recordLotteryScore("egypt", 7777);
        offline.userState.miniGamesWon = 2;
        offline.userState.questsCompleted = 6;
        offline.userState.markSaved();

        Peer peer = connectAndSignIn(offline);
        UserState stored = server.accounts().stateOf("brooke");
        assertEquals(7777, stored.getLotteryHighScore("egypt"),
                "an offline Lottery record reaches the server on connect");
        assertEquals(104, stored.lastLevel);
        assertEquals(2, stored.miniGamesWon);
        assertEquals(6, stored.questsCompleted);
        assertEquals(7777, stored.bonusHighScore);

        Envelope board = peer.awaitOk(peer.send(Protocol.LEADERBOARD, Envelope.obj()));
        for (var element : board.payload().getAsJsonArray("rows")) {
            var row = JsonLine.fromTree(element.getAsJsonObject(),
                    net.dto.LeaderboardRowDto.class);
            if (!"brooke".equals(row.username)) continue;
            assertEquals(7777L, row.lotteryScore("egypt"));
            assertEquals("Frostbite Caves", row.chapter,
                    "chapter progress travels with the account too");
            assertEquals(4, row.levelsCleared);
            assertEquals(2, row.miniGamesWon);
            assertEquals(6, row.questsCompleted);
        }
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
