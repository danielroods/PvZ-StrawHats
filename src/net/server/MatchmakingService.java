package net.server;

import model.match.mini_games.izombie.IZombieMatch.Role;
import net.Envelope;
import net.Protocol;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MatchmakingService {

    private static final long INVITE_TTL_MILLIS = 30_000L;

    public static class Invite {
        public final long id;
        public final ClientSession from;
        public final ClientSession to;
        public final long createdAt = System.currentTimeMillis();

        Invite(long id, ClientSession from, ClientSession to) {
            this.id = id;
            this.from = from;
            this.to = to;
        }
    }

    private final GameServer server;
    private final Deque<ClientSession> queue = new ArrayDeque<>();
    private final Map<Long, Invite> invites = new ConcurrentHashMap<>();
    private final AtomicLong inviteIds = new AtomicLong();
    private final Random random = new Random();
    private final Object queueLock = new Object();

    public MatchmakingService(GameServer server) {
        this.server = server;
    }

    public String challenge(ClientSession from, String targetUsername) {
        if (targetUsername == null || targetUsername.isBlank()) {
            return "Type the username you want to challenge.";
        }
        if (targetUsername.equalsIgnoreCase(from.getUsername())) {
            return Protocol.ERR_SELF_CHALLENGE;
        }
        if (!server.accounts().exists(targetUsername)) {
            return Protocol.ERR_NO_SUCH_USER;
        }
        ClientSession target = server.sessions().find(targetUsername);
        if (target == null || !target.isRunning()) {
            return Protocol.ERR_USER_OFFLINE;
        }
        if (server.matches().isBusy(target) || server.matches().isBusy(from)) {
            return Protocol.ERR_USER_BUSY;
        }

        long id = inviteIds.incrementAndGet();
        invites.put(id, new Invite(id, from, target));
        target.push(Protocol.CHALLENGE_INCOMING, Envelope.obj(
                "inviteId", id,
                "fromUsername", from.getUsername(),
                "fromNickname", nicknameOf(from.getUsername()),
                "yourRole", Protocol.ROLE_PLANTS,
                "ttlSeconds", INVITE_TTL_MILLIS / 1000L));
        return null;
    }

    public String respond(ClientSession responder, long inviteId, boolean accept) {
        Invite invite = invites.remove(inviteId);
        if (invite == null) return "That invite has expired.";
        if (invite.to != responder) return "That invite is not yours.";

        if (!accept) {
            if (invite.from.isRunning()) {
                invite.from.push(Protocol.CHALLENGE_DECLINED, Envelope.obj(
                        "inviteId", inviteId,
                        "byUsername", responder.getUsername()));
            }
            return null;
        }

        if (!invite.from.isRunning()) return "That player went offline.";
        if (server.matches().isBusy(invite.from) || server.matches().isBusy(responder)) {
            return "One of you is already in a match.";
        }
        startMatch(responder, invite.from);
        return null;
    }

    public String joinQueue(ClientSession session) {
        if (server.matches().isBusy(session)) return "You are already in a match.";
        ClientSession opponent = null;
        synchronized (queueLock) {
            queue.remove(session);
            while (!queue.isEmpty()) {
                ClientSession head = queue.pollFirst();
                if (head != null && head.isRunning() && head != session
                        && !server.matches().isBusy(head)) {
                    opponent = head;
                    break;
                }
            }
            if (opponent == null) queue.addLast(session);
        }

        if (opponent == null) {
            return null;
        }

        boolean coinFlip = random.nextBoolean();
        if (coinFlip) {
            startMatch(session, opponent);
        } else {
            startMatch(opponent, session);
        }
        return null;
    }

    public void leaveQueue(ClientSession session) {
        synchronized (queueLock) {
            queue.remove(session);
        }
    }

    private void startMatch(ClientSession plants, ClientSession zombies) {
        synchronized (queueLock) {
            queue.remove(plants);
            queue.remove(zombies);
        }
        invites.entrySet().removeIf(entry ->
                entry.getValue().from == plants || entry.getValue().to == plants
                        || entry.getValue().from == zombies || entry.getValue().to == zombies);

        MatchSession match = server.matches().create(plants, zombies);
        announce(match, Role.PLANTS, plants, zombies);
        announce(match, Role.ZOMBIES, zombies, plants);
    }

    private void announce(MatchSession match, Role role, ClientSession self, ClientSession other) {
        self.push(Protocol.MATCH_FOUND, Envelope.obj(
                "matchId", match.getMatchId(),
                "role", role.name(),
                "opponentUsername", other.getUsername(),
                "opponentNickname", nicknameOf(other.getUsername()),
                "matchSeconds", model.match.mini_games.izombie.IZombieMatch.MATCH_SECONDS));
    }

    private String nicknameOf(String username) {
        var user = server.accounts().find(username);
        return user == null || user.nickname == null ? username : user.nickname;
    }

    public void expireStaleInvites() {
        long now = System.currentTimeMillis();
        List<Invite> expired = new ArrayList<>();
        for (Invite invite : invites.values()) {
            if (now - invite.createdAt > INVITE_TTL_MILLIS) expired.add(invite);
        }
        for (Invite invite : expired) {
            invites.remove(invite.id);
            if (invite.from.isRunning()) {
                invite.from.push(Protocol.CHALLENGE_EXPIRED, Envelope.obj(
                        "inviteId", invite.id, "byUsername", invite.to.getUsername()));
            }
            if (invite.to.isRunning()) {
                invite.to.push(Protocol.CHALLENGE_EXPIRED, Envelope.obj(
                        "inviteId", invite.id, "byUsername", invite.from.getUsername()));
            }
        }
        synchronized (queueLock) {
            queue.removeIf(session -> !session.isRunning());
        }
    }

    public void onSessionClosed(ClientSession session) {
        leaveQueue(session);
        invites.entrySet().removeIf(entry -> {
            Invite invite = entry.getValue();
            if (invite.from != session && invite.to != session) return false;
            ClientSession other = invite.from == session ? invite.to : invite.from;
            if (other.isRunning()) {
                other.push(Protocol.CHALLENGE_EXPIRED, Envelope.obj(
                        "inviteId", invite.id, "byUsername", session.getUsername()));
            }
            return true;
        });
    }

    public int queueSize() {
        synchronized (queueLock) {
            return queue.size();
        }
    }
}
