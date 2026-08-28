package net.server;

import model.match.mini_games.izombie.IZombieMatch.Role;
import net.Envelope;
import net.Protocol;

public class MatchHandlers {

    private static final long REACTION_COOLDOWN_MILLIS = 1_500L;

    private final GameServer server;

    public MatchHandlers(GameServer server) {
        this.server = server;
    }

    public void handle(ClientSession session, Envelope envelope) {
        if (!session.isLoggedIn()) {
            session.sendError(envelope.id, Protocol.ERR_NOT_LOGGED_IN, "Sign in first.");
            return;
        }
        MatchSession match = server.matches().find(envelope.getString("matchId"));
        if (match == null || !match.involves(session)) {
            session.sendError(envelope.id, Protocol.ERR_NO_SUCH_MATCH, "That match is over.");
            return;
        }
        Role role = match.roleOf(session);
        switch (envelope.t) {
            case Protocol.MATCH_READY -> ready(session, envelope, match, role);
            case Protocol.MATCH_INTENT -> intent(session, envelope, match, role);
            case Protocol.MATCH_LEAVE -> leave(session, envelope, match, role);
            case Protocol.REACTION_SEND -> reaction(session, envelope, match, role);
            default -> session.sendError(envelope.id, Protocol.ERR_BAD_REQUEST, "Unhandled.");
        }
    }

    private void ready(ClientSession session, Envelope envelope, MatchSession match, Role role) {
        match.markReady(role);
        session.sendOk(envelope.id, Envelope.obj("matchId", match.getMatchId()));
        if (match.canStart() && !match.isStarted()) {
            match.markStarted();
        }
    }

    private void intent(ClientSession session, Envelope envelope, MatchSession match, Role role) {
        String action = envelope.getString("action");
        if (action == null) {
            session.sendError(envelope.id, Protocol.ERR_BAD_REQUEST, "Missing action.");
            return;
        }
        match.submit(new MatchSession.Intent(role, action, envelope.getString("target"),
                envelope.getInt("row", -1), envelope.getInt("col", -1), session));
    }

    private void leave(ClientSession session, Envelope envelope, MatchSession match, Role role) {
        match.getMatch().forfeit(role, "Your opponent left the match.");
        session.sendOk(envelope.id, Envelope.obj("left", true));
        if (!match.isStarted()) {
            match.markReady(Role.PLANTS);
            match.markReady(Role.ZOMBIES);
            match.markStarted();
        }
    }

    private void reaction(ClientSession session, Envelope envelope, MatchSession match, Role role) {
        long now = System.currentTimeMillis();
        if (now - session.getLastReactionMillis() < REACTION_COOLDOWN_MILLIS) {
            session.sendError(envelope.id, Protocol.ERR_RATE_LIMITED,
                    "Give your opponent a moment.");
            return;
        }
        String kind = envelope.getString("kind", Protocol.REACTION_TEXT);
        int index = envelope.getInt("index", -1);
        if (index < 0 || index > 2 || !isKnownKind(kind)) {
            session.sendError(envelope.id, Protocol.ERR_BAD_REQUEST, "Unknown reaction.");
            return;
        }
        session.markReactionSent();

        Role other = role == Role.PLANTS ? Role.ZOMBIES : Role.PLANTS;
        ClientSession opponent = match.clientFor(other);
        if (opponent != null && opponent.isRunning()) {
            opponent.push(Protocol.REACTION, Envelope.obj(
                    "matchId", match.getMatchId(),
                    "fromUsername", session.getUsername(),
                    "kind", kind,
                    "index", index));
        }
        session.sendOk(envelope.id, Envelope.obj("sent", true));
    }

    private boolean isKnownKind(String kind) {
        return Protocol.REACTION_TEXT.equals(kind)
                || Protocol.REACTION_EMOJI.equals(kind)
                || Protocol.REACTION_STICKER.equals(kind);
    }
}
