package net.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.Envelope;
import net.JsonLine;
import net.Protocol;
import net.dto.LeaderboardRowDto;

public class LobbyHandlers {

    private final GameServer server;

    public LobbyHandlers(GameServer server) {
        this.server = server;
    }

    public void handle(ClientSession session, Envelope envelope) {
        if (!session.isLoggedIn()) {
            session.sendError(envelope.id, Protocol.ERR_NOT_LOGGED_IN, "Sign in first.");
            return;
        }
        switch (envelope.t) {
            case Protocol.ONLINE_LIST -> onlineList(session, envelope);
            case Protocol.CHALLENGE -> challenge(session, envelope);
            case Protocol.CHALLENGE_RESPOND -> respond(session, envelope);
            case Protocol.QUEUE_JOIN -> queueJoin(session, envelope);
            case Protocol.QUEUE_LEAVE -> queueLeave(session, envelope);
            case Protocol.LEADERBOARD -> leaderboard(session, envelope);
            default -> session.sendError(envelope.id, Protocol.ERR_BAD_REQUEST, "Unhandled.");
        }
    }

    private void onlineList(ClientSession session, Envelope envelope) {
        JsonArray players = new JsonArray();
        for (String username : server.sessions().onlineUsernames()) {
            if (username.equalsIgnoreCase(session.getUsername())) continue;
            var user = server.accounts().find(username);
            JsonObject entry = new JsonObject();
            entry.addProperty("username", username);
            entry.addProperty("nickname", user == null || user.nickname == null
                    ? username : user.nickname);
            entry.addProperty("inMatch", server.matches().isBusy(username));
            players.add(entry);
        }
        JsonObject payload = new JsonObject();
        payload.add("players", players);
        session.sendOk(envelope.id, payload);
    }

    private void challenge(ClientSession session, Envelope envelope) {
        String target = envelope.getString("targetUsername");
        String error = server.matchmaking().challenge(session, target);
        if (error == null) {
            session.sendOk(envelope.id, Envelope.obj("sent", true, "targetUsername", target));
            return;
        }
        session.sendError(envelope.id, codeFor(error), messageFor(error, target));
    }

    private String codeFor(String error) {
        return switch (error) {
            case Protocol.ERR_NO_SUCH_USER, Protocol.ERR_USER_OFFLINE,
                 Protocol.ERR_USER_BUSY, Protocol.ERR_SELF_CHALLENGE -> error;
            default -> Protocol.ERR_VALIDATION;
        };
    }

    private String messageFor(String error, String target) {
        return switch (error) {
            case Protocol.ERR_NO_SUCH_USER -> "No player called \"" + target + "\".";
            case Protocol.ERR_USER_OFFLINE -> target + " is not online right now.";
            case Protocol.ERR_USER_BUSY -> target + " is already in a match.";
            case Protocol.ERR_SELF_CHALLENGE -> "You cannot challenge yourself.";
            default -> error;
        };
    }

    private void respond(ClientSession session, Envelope envelope) {
        long inviteId = envelope.getLong("inviteId", -1);
        boolean accept = envelope.getBoolean("accept", false);
        String error = server.matchmaking().respond(session, inviteId, accept);
        if (error != null) {
            session.sendError(envelope.id, Protocol.ERR_VALIDATION, error);
            return;
        }
        session.sendOk(envelope.id, Envelope.obj("accepted", accept));
    }

    private void queueJoin(ClientSession session, Envelope envelope) {
        String error = server.matchmaking().joinQueue(session);
        if (error != null) {
            session.sendError(envelope.id, Protocol.ERR_VALIDATION, error);
            return;
        }
        session.sendOk(envelope.id, Envelope.obj("queued", true));
    }

    private void queueLeave(ClientSession session, Envelope envelope) {
        server.matchmaking().leaveQueue(session);
        session.sendOk(envelope.id, Envelope.obj("queued", false));
    }

    private void leaderboard(ClientSession session, Envelope envelope) {
        JsonArray rows = new JsonArray();
        for (LeaderboardRowDto row : server.leaderboard().rows()) {
            rows.add(JsonLine.toTree(row));
        }
        JsonObject payload = new JsonObject();
        payload.add("rows", rows);
        session.sendOk(envelope.id, payload);
    }
}
