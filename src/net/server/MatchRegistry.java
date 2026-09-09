package net.server;

import com.google.gson.JsonObject;
import model.collections.plant.Plant;
import model.match.mini_games.izombie.IZombieMatch;
import model.match.mini_games.izombie.IZombieMatch.MatchEvent;
import model.match.mini_games.izombie.IZombieMatch.Role;
import model.projectile.zombie_projectile.ZombieProjectile;
import model.user_data.User;
import net.Envelope;
import net.JsonLine;
import net.Protocol;
import service.GameClock;
import service.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class MatchRegistry {

    private static final long TICK_NANOS = (long) (GameClock.SECONDS_PER_TICK * 1_000_000_000L);
    private static final int SNAPSHOT_EVERY_TICKS = 1;
    private static final int MAX_CATCH_UP_TICKS = 5;

    private final GameServer server;
    private final Map<String, MatchSession> matches = new ConcurrentHashMap<>();
    private final AtomicLong matchIds = new AtomicLong();

    private Thread runner;
    private volatile boolean running;

    public MatchRegistry(GameServer server) {
        this.server = server;
    }

    public void start() {
        running = true;
        runner = new Thread(this::runLoop, "match-runner");
        runner.setDaemon(true);
        runner.start();
    }

    public void stop() {
        running = false;
        if (runner != null) runner.interrupt();
    }

    public MatchSession create(ClientSession plants, ClientSession zombies) {
        String id = "m" + matchIds.incrementAndGet();
        MatchSession session = new MatchSession(id, plants, zombies);
        matches.put(id, session);
        return session;
    }

    public MatchSession find(String matchId) {
        return matchId == null ? null : matches.get(matchId);
    }

    public MatchSession findByClient(ClientSession client) {
        for (MatchSession session : matches.values()) {
            if (session.involves(client)) return session;
        }
        return null;
    }

    public boolean isBusy(ClientSession client) {
        return findByClient(client) != null;
    }

    public boolean isBusy(String username) {
        if (username == null) return false;
        for (MatchSession session : matches.values()) {
            if (username.equalsIgnoreCase(session.usernameFor(Role.PLANTS))
                    || username.equalsIgnoreCase(session.usernameFor(Role.ZOMBIES))) {
                return true;
            }
        }
        return false;
    }

    private void runLoop() {
        long nextTick = System.nanoTime();
        while (running) {
            int steps = 0;
            while (System.nanoTime() - nextTick >= 0 && steps < MAX_CATCH_UP_TICKS) {
                try {
                    stepAllMatches();
                } catch (Exception e) {
                    Log.error("MatchRunner", "Tick failed", e);
                }
                nextTick += TICK_NANOS;
                steps++;
            }
            if (steps >= MAX_CATCH_UP_TICKS) {
                nextTick = System.nanoTime();
            }
            long sleepNanos = nextTick - System.nanoTime();
            if (sleepNanos > 0) {
                LockSupport.parkNanos(Math.min(sleepNanos, TICK_NANOS));
            }
            if (Thread.currentThread().isInterrupted()) return;
        }
    }

    private void stepAllMatches() {
        for (MatchSession session : matches.values()) {
            Role leaver = session.pendingLeaver();
            if (leaver != null && !session.isStarted()) {
                abandon(session, leaver);
                continue;
            }
            if (!session.isStarted()) {
                if (session.startIfReady()) announceStart(session);
                continue;
            }

            IZombieMatch match = session.getMatch();
            model.utils.GameSession.setCurrent(match.getSession());

            if (leaver != null && !match.isFinished()) {
                match.forfeit(leaver, session.leaveReason());
            }

            drainIntents(session, match);
            List<Plant> plantsBeforeTick = new ArrayList<>(match.getSession().getPlants());
            List<ZombieProjectile> shotsBeforeTick =
                    new ArrayList<>(match.getSession().getZombieProjectiles());
            match.tick();
            session.countTick();
            session.captureRemovals(plantsBeforeTick, shotsBeforeTick);
            session.forgetDeadEntities();

            for (MatchEvent event : match.drainEvents()) {
                broadcast(session, Protocol.MATCH_EVENT,
                        Envelope.obj("matchId", session.getMatchId(),
                                "kind", event.kind(), "row", event.row()));
            }

            if (session.tickCount() % SNAPSHOT_EVERY_TICKS == 0) {
                broadcastSnapshot(session);
                session.clearRemovals();
            }

            if (match.isFinished()) {
                finish(session);
            }
        }
    }

    private void announceStart(MatchSession session) {
        IZombieMatch match = session.getMatch();
        for (Role role : session.roles()) {
            ClientSession client = session.clientFor(role);
            if (client == null || !client.isRunning()) continue;
            client.push(Protocol.MATCH_START, Envelope.obj(
                    "matchId", session.getMatchId(),
                    "role", role.name(),
                    "matchSeconds", match.getMatchSeconds()));
        }
        broadcastSnapshot(session);
    }

    public void broadcastLobby(MatchSession session) {
        broadcast(session, Protocol.MATCH_LOBBY, Envelope.obj(
                "matchId", session.getMatchId(),
                "plantsReady", session.isReady(Role.PLANTS),
                "zombiesReady", session.isReady(Role.ZOMBIES)));
    }

    private void drainIntents(MatchSession session, IZombieMatch match) {
        MatchSession.Intent intent;
        while ((intent = session.inbox().poll()) != null) {
            String rejection = match.applyIntent(intent.role(), intent.action(),
                    intent.target(), intent.row(), intent.col());
            if (rejection != null && intent.from() != null) {
                intent.from().push(Protocol.MATCH_EVENT,
                        Envelope.obj("matchId", session.getMatchId(),
                                "kind", Protocol.EVENT_INTENT_REJECTED,
                                "reason", rejection));
            }
        }
    }

    private void broadcastSnapshot(MatchSession session) {
        for (Role role : session.roles()) {
            ClientSession client = session.clientFor(role);
            if (client == null || !client.isRunning()) continue;
            JsonObject payload = new JsonObject();
            payload.addProperty("matchId", session.getMatchId());
            payload.add("snapshot", JsonLine.compactTree(session.snapshot(role)));
            client.push(Protocol.MATCH_SNAPSHOT, payload);
        }
    }

    private void broadcast(MatchSession session, String type, JsonObject payload) {
        for (Role role : session.roles()) {
            ClientSession client = session.clientFor(role);
            if (client != null && client.isRunning()) client.push(type, payload);
        }
    }

    private void finish(MatchSession session) {
        IZombieMatch match = session.getMatch();
        Role winner = match.getWinner();
        matches.remove(session.getMatchId());

        int winnerMiniGamesWon = -1;
        if (winner != null) {
            String winnerName = session.usernameFor(winner);
            User user = server.accounts().find(winnerName);
            if (user != null && user.userState != null) {
                winnerMiniGamesWon = ++user.userState.miniGamesWon;
                server.accounts().touch();
                server.accounts().saveNow();
            }
        }

        for (Role role : session.roles()) {
            ClientSession client = session.clientFor(role);
            if (client == null || !client.isRunning()) continue;
            JsonObject payload = Envelope.obj(
                    "matchId", session.getMatchId(),
                    "winnerRole", winner == null ? null : winner.name(),
                    "youWon", winner == role,
                    "reason", match.getEndReason(),
                    "brainsEaten", match.getBrainsEaten(),
                    "brainCount", match.getBrainCount(),
                    "elapsed", match.getElapsedSeconds());
            // The winner is told the total the server just wrote rather than being left to
            // add one of its own, so the two copies of the account converge on one number
            // however the state sync happens to be ordered around the match ending.
            if (winner == role && winnerMiniGamesWon >= 0) {
                payload.addProperty("miniGamesWon", winnerMiniGamesWon);
            }
            client.push(Protocol.MATCH_END, payload);
        }
    }

    private void abandon(MatchSession session, Role leaver) {
        matches.remove(session.getMatchId());
        Role other = leaver == Role.PLANTS ? Role.ZOMBIES : Role.PLANTS;
        ClientSession client = session.clientFor(other);
        if (client == null || !client.isRunning()) return;
        client.push(Protocol.MATCH_END, Envelope.obj(
                "matchId", session.getMatchId(),
                "winnerRole", other.name(),
                "youWon", true,
                "reason", session.leaveReason(),
                "brainsEaten", 0,
                "brainCount", IZombieMatch.ROWS,
                "elapsed", 0.0));
    }

    public void onSessionClosed(ClientSession client) {
        List<MatchSession> live = List.copyOf(matches.values());
        for (MatchSession session : live) {
            if (!session.involves(client)) continue;
            Role role = session.roleOf(client);
            if (role == null) continue;
            session.requestLeave(role, "Your opponent left the match.");
        }
    }
}
