package net.server;

import net.Envelope;
import net.Protocol;
import service.Log;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameServer {

    private final int port;
    private final AccountStore accounts;
    private final SessionRegistry sessions = new SessionRegistry();
    private final MatchRegistry matches;
    private final MatchmakingService matchmaking;
    private final LeaderboardService leaderboard;
    private final AccountHandlers accountHandlers;
    private final LobbyHandlers lobbyHandlers;
    private final MatchHandlers matchHandlers;
    private final List<ClientSession> allSessions = new CopyOnWriteArrayList<>();

    private ServerSocket serverSocket;
    private volatile boolean running;

    public GameServer(int port, File dataDirectory) {
        this.port = port;
        this.accounts = new AccountStore(dataDirectory);
        this.matches = new MatchRegistry(this);
        this.matchmaking = new MatchmakingService(this);
        this.leaderboard = new LeaderboardService(accounts);
        this.accountHandlers = new AccountHandlers(this);
        this.lobbyHandlers = new LobbyHandlers(this);
        this.matchHandlers = new MatchHandlers(this);
    }

    public AccountStore accounts() {
        return accounts;
    }

    public SessionRegistry sessions() {
        return sessions;
    }

    public MatchRegistry matches() {
        return matches;
    }

    public MatchmakingService matchmaking() {
        return matchmaking;
    }

    public LeaderboardService leaderboard() {
        return leaderboard;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        matches.start();
        startMaintenanceThread();
        Log.info("Server", "Listening on port " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "net-shutdown"));

        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                ClientSession session = new ClientSession(socket, this);
                allSessions.add(session);
                session.start();
            } catch (IOException e) {
                if (running) Log.error("Server", "Accept failed", e);
            }
        }
    }

    private void startMaintenanceThread() {
        Thread thread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(2_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                long now = System.currentTimeMillis();
                for (ClientSession session : allSessions) {
                    if (!session.isRunning()) {
                        allSessions.remove(session);
                        continue;
                    }
                    if (now - session.getLastSeenMillis() > Protocol.SILENCE_TIMEOUT_MILLIS) {
                        Log.info("Server", "Dropping silent session " + session.getSessionId());
                        session.close();
                    }
                }
                matchmaking.expireStaleInvites();
                accounts.flushIfDirty();
            }
        }, "net-maintenance");
        thread.setDaemon(true);
        thread.start();
    }

    public void route(ClientSession session, Envelope envelope) {
        switch (envelope.t) {
            case Protocol.HELLO -> handleHello(session, envelope);
            case Protocol.PING -> session.send(
                    new Envelope(Protocol.PONG, 0L, envelope.id, Envelope.obj()));
            case Protocol.PONG -> { }
            case Protocol.SIGN_IN, Protocol.LOGOUT, Protocol.STATE_SYNC,
                 Protocol.STATE_PULL, Protocol.BONUS_SCORE_SUBMIT ->
                    accountHandlers.handle(session, envelope);
            case Protocol.ONLINE_LIST, Protocol.CHALLENGE, Protocol.CHALLENGE_RESPOND,
                 Protocol.QUEUE_JOIN, Protocol.QUEUE_LEAVE, Protocol.LEADERBOARD ->
                    lobbyHandlers.handle(session, envelope);
            case Protocol.MATCH_READY, Protocol.MATCH_INTENT, Protocol.MATCH_LEAVE,
                 Protocol.REACTION_SEND ->
                    matchHandlers.handle(session, envelope);
            default -> session.sendError(envelope.id, Protocol.ERR_BAD_REQUEST,
                    "Unknown message type: " + envelope.t);
        }
    }

    private void handleHello(ClientSession session, Envelope envelope) {
        int clientVersion = envelope.getInt("protocolVersion", -1);
        if (clientVersion != Protocol.VERSION) {
            session.sendError(envelope.id, Protocol.ERR_VERSION,
                    "This client speaks protocol " + clientVersion + ", the server speaks "
                            + Protocol.VERSION + ". Update the game.");
            return;
        }
        session.sendOk(envelope.id, Envelope.obj("protocolVersion", Protocol.VERSION));
    }

    public void onSessionClosed(ClientSession session) {
        allSessions.remove(session);
        matchmaking.onSessionClosed(session);
        matches.onSessionClosed(session);
        sessions.release(session);
    }

    public void stop() {
        if (!running) return;
        running = false;
        matches.stop();
        accounts.flushIfDirty();
        for (ClientSession session : allSessions) {
            session.close();
        }
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
            // Closing a already-closed socket during shutdown is harmless.
        }
        Log.info("Server", "Stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return serverSocket == null ? port : serverSocket.getLocalPort();
    }
}