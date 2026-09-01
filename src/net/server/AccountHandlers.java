package net.server;

import com.google.gson.JsonObject;
import model.user_data.User;
import model.user_data.UserState;
import net.Envelope;
import net.JsonLine;
import net.Protocol;
import net.dto.AccountDto;

public class AccountHandlers {

    private final GameServer server;

    public AccountHandlers(GameServer server) {
        this.server = server;
    }

    public void handle(ClientSession session, Envelope envelope) {
        switch (envelope.t) {
            case Protocol.SIGN_IN -> signIn(session, envelope);
            case Protocol.LOGOUT -> logout(session, envelope);
            case Protocol.STATE_SYNC -> stateSync(session, envelope);
            case Protocol.STATE_PULL -> statePull(session, envelope);
            case Protocol.BONUS_SCORE_SUBMIT -> bonusScore(session, envelope);
            default -> session.sendError(envelope.id, Protocol.ERR_BAD_REQUEST, "Unhandled.");
        }
    }

    private void signIn(ClientSession session, Envelope envelope) {
        AccountDto dto = accountOf(envelope);
        UserState incoming = stateOf(envelope);
        if (dto == null || dto.username == null || dto.username.isEmpty()) {
            session.sendError(envelope.id, Protocol.ERR_VALIDATION, "Missing account.");
            return;
        }

        AccountStore accounts = server.accounts();
        User known = accounts.locate(dto.accountId, dto.username);
        if (known != null && !accounts.credentialsMatch(known, dto)) {
            session.sendError(envelope.id, Protocol.ERR_BAD_CREDENTIALS,
                    "Another account on this server already uses that username.");
            return;
        }
        if (known != null && server.sessions().isOnline(known.username)) {
            session.sendError(envelope.id, Protocol.ERR_ALREADY_ONLINE,
                    "That account is already signed in somewhere else.");
            return;
        }

        AccountStore.SyncOutcome outcome = server.accounts().sync(dto, incoming);
        if (outcome.isError()) {
            String code = outcome.error().startsWith("Username already")
                    ? Protocol.ERR_USERNAME_TAKEN : Protocol.ERR_VALIDATION;
            session.sendError(envelope.id, code, outcome.error());
            return;
        }

        User user = outcome.user();
        server.sessions().release(session);
        server.sessions().bind(user.username, session);
        session.setUsername(user.username);
        session.setAccountId(user.accountId());
        server.accounts().saveNow();
        session.sendOk(envelope.id, accountPayload(user, outcome.usernameTaken()));
    }

    private void logout(ClientSession session, Envelope envelope) {
        server.matchmaking().onSessionClosed(session);
        server.matches().onSessionClosed(session);
        server.sessions().release(session);
        session.setUsername(null);
        session.setAccountId(null);
        session.sendOk(envelope.id, Envelope.obj());
    }

    private void stateSync(ClientSession session, Envelope envelope) {
        if (!requireLogin(session, envelope)) return;
        AccountDto dto = accountOf(envelope);
        UserState incoming = stateOf(envelope);
        if (dto == null || incoming == null) {
            session.sendError(envelope.id, Protocol.ERR_BAD_REQUEST, "Missing account state.");
            return;
        }

        User bound = server.accounts().findById(session.getAccountId());
        if (bound == null) bound = server.accounts().find(session.getUsername());
        if (bound == null) {
            session.sendError(envelope.id, Protocol.ERR_NO_SUCH_USER, "Account is gone.");
            return;
        }
        if (!bound.accountId().equals(dto.accountId)) {
            session.sendError(envelope.id, Protocol.ERR_WRONG_ACCOUNT,
                    "That state belongs to a different account.");
            return;
        }

        AccountStore.SyncOutcome outcome = server.accounts().sync(dto, incoming);
        if (outcome.isError()) {
            session.sendError(envelope.id, Protocol.ERR_VALIDATION, outcome.error());
            return;
        }

        User user = outcome.user();
        if (!user.username.equalsIgnoreCase(session.getUsername())) {
            server.sessions().release(session);
            server.sessions().bind(user.username, session);
            session.setUsername(user.username);
        }
        server.accounts().saveNow();
        session.sendOk(envelope.id, accountPayload(user, outcome.usernameTaken()));
    }

    private void statePull(ClientSession session, Envelope envelope) {
        if (!requireLogin(session, envelope)) return;
        User user = server.accounts().find(session.getUsername());
        if (user == null) {
            session.sendError(envelope.id, Protocol.ERR_NO_SUCH_USER, "Account is gone.");
            return;
        }
        session.sendOk(envelope.id, accountPayload(user, false));
    }

    private void bonusScore(ClientSession session, Envelope envelope) {
        if (!requireLogin(session, envelope)) return;
        int score = envelope.getInt("score", 0);
        Integer before = server.accounts().bonusScoreOf(session.getUsername());
        server.accounts().submitBonusScore(session.getUsername(), score);
        Integer best = server.accounts().bonusScoreOf(session.getUsername());
        boolean improved = before == null || (best != null && best > before);
        server.accounts().saveNow();
        session.sendOk(envelope.id, Envelope.obj("bestScore", best, "improved", improved));
    }

    private boolean requireLogin(ClientSession session, Envelope envelope) {
        if (session.isLoggedIn()) return true;
        session.sendError(envelope.id, Protocol.ERR_NOT_LOGGED_IN, "Sign in first.");
        return false;
    }

    private AccountDto accountOf(Envelope envelope) {
        JsonObject json = envelope.getObject("account");
        return json == null ? null : JsonLine.fromTree(json, AccountDto.class);
    }

    private UserState stateOf(Envelope envelope) {
        JsonObject json = envelope.getObject("userState");
        return json == null ? null : JsonLine.fromTree(json, UserState.class);
    }

    private JsonObject accountPayload(User user, boolean usernameTaken) {
        JsonObject payload = new JsonObject();
        payload.add("account", JsonLine.toTree(AccountDto.of(user)));
        payload.add("userState", JsonLine.toTree(user.userState));
        payload.addProperty("usernameTaken", usernameTaken);
        return payload;
    }
}
