package net.client;

import com.google.gson.JsonObject;
import model.App;
import model.match.mini_games.izombie.IZombieMatch.Role;
import model.user_data.User;
import net.Envelope;
import net.JsonLine;
import net.Protocol;
import net.dto.LeaderboardRowDto;
import net.dto.MatchSnapshot;
import service.Log;
import view.GeneralPrinter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class NetworkClient {

    public record Invite(long inviteId, String fromUsername, String fromNickname) { }

    public record OnlinePlayer(String username, String nickname, boolean inMatch) { }

    private static final NetworkClient INSTANCE = new NetworkClient();
    private static final long STATE_PUSH_INTERVAL_MILLIS = 2000L;

    private final Map<Long, Consumer<Envelope>> pending = new HashMap<>();

    private ServerConnection connection;
    private String host = Protocol.DEFAULT_HOST;
    private int port = Protocol.DEFAULT_PORT;
    private String signedInUsername;
    private String statusMessage = "";
    private Invite pendingInvite;
    private NetMatchState matchState;
    private List<OnlinePlayer> onlinePlayers = new ArrayList<>();
    private List<LeaderboardRowDto> leaderboardRows;
    private boolean queued;
    private boolean stateDirty;
    private long lastStatePushMillis;
    private Runnable onMatchFound;
    private Consumer<String> onDisconnected;

    private NetworkClient() {
    }

    public static NetworkClient get() {
        return INSTANCE;
    }

    public boolean isConnected() {
        return connection != null && connection.isRunning();
    }

    public boolean isSignedIn() {
        return isConnected() && signedInUsername != null;
    }

    public String getSignedInUsername() {
        return signedInUsername;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isQueued() {
        return queued;
    }

    public Invite getPendingInvite() {
        return pendingInvite;
    }

    public void clearPendingInvite() {
        pendingInvite = null;
    }

    public List<OnlinePlayer> getOnlinePlayers() {
        return onlinePlayers;
    }

    public List<LeaderboardRowDto> getLeaderboardRows() {
        return leaderboardRows;
    }

    public NetMatchState getMatchState() {
        return matchState;
    }

    public void setOnMatchFound(Runnable onMatchFound) {
        this.onMatchFound = onMatchFound;
    }

    public void setOnDisconnected(Consumer<String> onDisconnected) {
        this.onDisconnected = onDisconnected;
    }

    public boolean connect(String host, int port) {
        disconnect();
        this.host = host;
        this.port = port;
        try {
            connection = new ServerConnection(host, port);
            statusMessage = "Connected to " + host + ":" + port;
            send(Protocol.HELLO, Envelope.obj("protocolVersion", Protocol.VERSION), envelope -> {
                if (envelope.isType(Protocol.ERR)) {
                    statusMessage = envelope.getString("message", "Handshake refused.");
                    disconnect();
                }
            });
            return true;
        } catch (IOException e) {
            statusMessage = "Could not reach " + host + ":" + port
                    + ". Is the server running?";
            connection = null;
            return false;
        }
    }

    public void disconnect() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
        signedInUsername = null;
        pendingInvite = null;
        queued = false;
        matchState = null;
        pending.clear();
        onlinePlayers = new ArrayList<>();
        User.useLocalStore();
    }

    private long send(String type, JsonObject payload, Consumer<Envelope> onReply) {
        if (!isConnected()) {
            if (onReply != null) {
                onReply.accept(new Envelope(Protocol.ERR, 0, null,
                        Envelope.obj("code", "OFFLINE", "message", "You are not connected.")));
            }
            return -1;
        }
        long id = connection.send(type, payload);
        if (onReply != null) pending.put(id, onReply);
        return id;
    }

    public void fireAndForget(String type, JsonObject payload) {
        send(type, payload, null);
    }

    public void register(String username, String password, String nickname, String email,
                         String gender, String question, String answer, Consumer<Envelope> reply) {
        send(Protocol.REGISTER, Envelope.obj(
                "username", username, "password", password, "nickname", nickname,
                "email", email, "gender", gender,
                "securityQuestion", question, "securityAnswer", answer), reply);
    }

    public void login(String username, String password, Consumer<Envelope> reply) {
        send(Protocol.LOGIN, Envelope.obj("username", username, "password", password),
                envelope -> {
                    if (envelope.isType(Protocol.OK)) {
                        adoptAccount(envelope);
                    }
                    if (reply != null) reply.accept(envelope);
                });
    }

    private void adoptAccount(Envelope envelope) {
        JsonObject account = envelope.getObject("account");
        JsonObject stateJson = envelope.getObject("userState");
        if (account == null) return;

        net.dto.AccountDto dto = JsonLine.fromTree(account, net.dto.AccountDto.class);
        model.user_data.UserState state = stateJson == null
                ? null : JsonLine.fromTree(stateJson, model.user_data.UserState.class);

        User user = new User(dto.username, "", dto.nickname, dto.email, dto.gender);
        dto.applyTo(user);
        if (state != null) user.userState = state;

        signedInUsername = dto.username;
        User.users.clear();
        User.users.add(user);
        User.useStore(new RemoteUserStore(this));
        User.setUser(user);
        statusMessage = "Signed in as " + dto.username;
    }

    public void logout() {
        if (isConnected()) fireAndForget(Protocol.LOGOUT, Envelope.obj());
        signedInUsername = null;
        queued = false;
        matchState = null;
        User.useLocalStore();
    }

    public void forgotPasswordStart(String username, String email, Consumer<Envelope> reply) {
        send(Protocol.FORGOT_PASSWORD_START,
                Envelope.obj("username", username, "email", email), reply);
    }

    public void forgotPasswordAnswer(String answer, String newPassword, Consumer<Envelope> reply) {
        send(Protocol.FORGOT_PASSWORD_ANSWER,
                Envelope.obj("answer", answer, "newPassword", newPassword), reply);
    }

    public void updateProfile(String field, String value, String oldPassword,
                              Consumer<Envelope> reply) {
        send(Protocol.PROFILE_UPDATE,
                Envelope.obj("field", field, "value", value, "oldPassword", oldPassword),
                envelope -> {
                    if (envelope.isType(Protocol.OK)) adoptAccount(envelope);
                    if (reply != null) reply.accept(envelope);
                });
    }

    public void markStateDirty() {
        stateDirty = true;
    }

    public void pushStateNow() {
        if (!isSignedIn() || User.currentUser == null) return;
        stateDirty = false;
        lastStatePushMillis = System.currentTimeMillis();
        JsonObject payload = new JsonObject();
        payload.add("userState", JsonLine.toTree(User.currentUser.userState));
        fireAndForget(Protocol.STATE_PUSH, payload);
    }

    public void refreshOnlinePlayers() {
        send(Protocol.ONLINE_LIST, Envelope.obj(), envelope -> {
            if (!envelope.isType(Protocol.OK)) return;
            List<OnlinePlayer> players = new ArrayList<>();
            var array = envelope.payload().getAsJsonArray("players");
            if (array != null) {
                for (var element : array) {
                    JsonObject entry = element.getAsJsonObject();
                    players.add(new OnlinePlayer(
                            entry.get("username").getAsString(),
                            entry.get("nickname").getAsString(),
                            entry.get("inMatch").getAsBoolean()));
                }
            }
            onlinePlayers = players;
        });
    }

    public void challenge(String targetUsername, Consumer<Envelope> reply) {
        send(Protocol.CHALLENGE, Envelope.obj("targetUsername", targetUsername), reply);
    }

    public void respondToInvite(long inviteId, boolean accept, Consumer<Envelope> reply) {
        pendingInvite = null;
        send(Protocol.CHALLENGE_RESPOND,
                Envelope.obj("inviteId", inviteId, "accept", accept), reply);
    }

    public void joinQueue(Consumer<Envelope> reply) {
        queued = true;
        send(Protocol.QUEUE_JOIN, Envelope.obj("mode", "IZOMBIE"), envelope -> {
            if (!envelope.isType(Protocol.OK)) queued = false;
            if (reply != null) reply.accept(envelope);
        });
    }

    public void leaveQueue() {
        queued = false;
        fireAndForget(Protocol.QUEUE_LEAVE, Envelope.obj());
    }

    public void requestLeaderboard(Consumer<Envelope> reply) {
        send(Protocol.LEADERBOARD, Envelope.obj(), envelope -> {
            if (envelope.isType(Protocol.OK)) {
                List<LeaderboardRowDto> rows = new ArrayList<>();
                var array = envelope.payload().getAsJsonArray("rows");
                if (array != null) {
                    for (var element : array) {
                        rows.add(JsonLine.fromTree(element.getAsJsonObject(),
                                LeaderboardRowDto.class));
                    }
                }
                leaderboardRows = rows;
            }
            if (reply != null) reply.accept(envelope);
        });
    }

    public void submitBonusScore(int score, Consumer<Envelope> reply) {
        send(Protocol.BONUS_SCORE_SUBMIT, Envelope.obj("score", score), reply);
    }

    public void sendMatchReady(List<String> loadout) {
        if (matchState == null) return;
        com.google.gson.JsonArray picks = new com.google.gson.JsonArray();
        if (loadout != null) {
            for (String pick : loadout) {
                if (pick != null && !pick.isBlank()) picks.add(pick);
            }
        }
        JsonObject payload = Envelope.obj("matchId", matchState.getMatchId());
        payload.add("loadout", picks);
        fireAndForget(Protocol.MATCH_READY, payload);
    }

    public void sendIntent(String action, String target, int row, int col) {
        if (matchState == null) return;
        fireAndForget(Protocol.MATCH_INTENT, Envelope.obj(
                "matchId", matchState.getMatchId(),
                "action", action, "target", target, "row", row, "col", col));
    }

    public void sendReaction(String kind, int index) {
        if (matchState == null) return;
        fireAndForget(Protocol.REACTION_SEND, Envelope.obj(
                "matchId", matchState.getMatchId(), "kind", kind, "index", index));
    }

    public void leaveMatch() {
        if (matchState == null) return;
        fireAndForget(Protocol.MATCH_LEAVE, Envelope.obj("matchId", matchState.getMatchId()));
        matchState = null;
    }

    public void clearMatchState() {
        if (matchState != null) matchState.dispose();
        matchState = null;
    }

    public void pump() {
        if (connection == null) return;

        if (!connection.isRunning()) {
            String failure = connection.getFailure();
            statusMessage = failure == null ? "Disconnected." : failure;
            connection = null;
            signedInUsername = null;
            matchState = null;
            queued = false;
            User.useLocalStore();
            if (onDisconnected != null) onDisconnected.accept(statusMessage);
            return;
        }

        connection.maybePing();
        if (stateDirty
                && System.currentTimeMillis() - lastStatePushMillis > STATE_PUSH_INTERVAL_MILLIS) {
            pushStateNow();
        }

        Envelope envelope;
        while ((envelope = connection.poll()) != null) {
            dispatch(envelope);
        }
    }

    private void dispatch(Envelope envelope) {
        if (envelope.re != null) {
            Consumer<Envelope> callback = pending.remove(envelope.re);
            if (callback != null) {
                try {
                    callback.accept(envelope);
                } catch (Exception e) {
                    Log.error("NetworkClient", "Reply handler failed for " + envelope.t, e);
                }
                return;
            }
        }

        switch (envelope.t) {
            case Protocol.PONG, Protocol.OK, Protocol.ERR -> { }
            case Protocol.CHALLENGE_INCOMING -> pendingInvite = new Invite(
                    envelope.getLong("inviteId", -1),
                    envelope.getString("fromUsername", "?"),
                    envelope.getString("fromNickname", "?"));
            case Protocol.CHALLENGE_DECLINED -> GeneralPrinter.print(
                    envelope.getString("byUsername", "Your opponent") + " declined the match.");
            case Protocol.CHALLENGE_EXPIRED -> {
                pendingInvite = null;
                GeneralPrinter.print("The match invite expired.");
            }
            case Protocol.MATCH_FOUND -> onMatchFound(envelope);
            case Protocol.MATCH_LOBBY -> onMatchLobby(envelope);
            case Protocol.MATCH_START -> onMatchStart(envelope);
            case Protocol.MATCH_SNAPSHOT -> onSnapshot(envelope);
            case Protocol.MATCH_EVENT -> onMatchEvent(envelope);
            case Protocol.MATCH_END -> onMatchEnd(envelope);
            case Protocol.REACTION -> {
                if (matchState != null) {
                    matchState.setReaction(envelope.getString("fromUsername", "?"),
                            envelope.getString("kind", Protocol.REACTION_TEXT),
                            envelope.getInt("index", 0));
                }
            }
            default -> Log.info("NetworkClient", "Unhandled push: " + envelope.t);
        }
    }

    private void onMatchFound(Envelope envelope) {
        queued = false;
        pendingInvite = null;
        Role role = Protocol.ROLE_PLANTS.equals(envelope.getString("role"))
                ? Role.PLANTS : Role.ZOMBIES;
        matchState = new NetMatchState(
                envelope.getString("matchId"),
                role,
                envelope.getString("opponentUsername"),
                envelope.getString("opponentNickname"));
        matchState.setMatchSeconds(envelope.getDouble("matchSeconds", 0));
        if (onMatchFound != null) onMatchFound.run();
        App.currentMenu = controller.match.NetBeforeMenu.open(matchState);
    }

    private void onMatchLobby(Envelope envelope) {
        if (matchState == null) return;
        if (!matchState.getMatchId().equals(envelope.getString("matchId"))) return;
        matchState.setReadyFlags(envelope.getBoolean("plantsReady", false),
                envelope.getBoolean("zombiesReady", false));
    }

    private void onMatchStart(Envelope envelope) {
        if (matchState == null) return;
        if (!matchState.getMatchId().equals(envelope.getString("matchId"))) return;
        if (matchState.isStarted()) return;
        matchState.setMatchSeconds(envelope.getDouble("matchSeconds", 0));
        matchState.markStarted();
        controller.match.BeforeMenu.selectedPlants.clear();
        controller.match.BeforeMenu.selectedZombies.clear();
        App.currentMenu = new controller.match.mini_games.NetIZombieController(matchState);
    }

    private void onSnapshot(Envelope envelope) {
        if (matchState == null) return;
        if (!matchState.getMatchId().equals(envelope.getString("matchId"))) return;
        JsonObject json = envelope.getObject("snapshot");
        MatchSnapshot snapshot = JsonLine.fromTree(json, MatchSnapshot.class);
        matchState.applySnapshot(snapshot);
    }

    private void onMatchEvent(Envelope envelope) {
        if (matchState == null) return;
        String kind = envelope.getString("kind", "");
        if (Protocol.EVENT_INTENT_REJECTED.equals(kind)) {
            matchState.pushRejection(envelope.getString("reason", "That move was rejected."));
        } else if (Protocol.EVENT_BRAIN_EATEN.equals(kind)) {
            matchState.markBrainEaten(envelope.getInt("row", -1));
        }
    }

    private void onMatchEnd(Envelope envelope) {
        if (matchState == null) return;
        matchState.end(envelope.getBoolean("youWon", false),
                envelope.getString("reason", ""));
        if (envelope.getBoolean("youWon", false) && User.currentUser != null) {
            User.currentUser.userState.miniGamesWon++;
        }
    }
}
