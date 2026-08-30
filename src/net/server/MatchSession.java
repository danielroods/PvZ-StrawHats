package net.server;

import model.collections.plant.Plant;
import model.match.mini_games.izombie.IZombieMatch;
import model.match.mini_games.izombie.IZombieMatch.Role;
import model.projectile.zombie_projectile.ZombieProjectile;
import net.dto.MatchSnapshot;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MatchSession {

    public record Intent(Role role, String action, String target, int row, int col,
                         ClientSession from) { }

    private final String matchId;
    private final ClientSession plantsClient;
    private final ClientSession zombiesClient;
    private final String plantsUsername;
    private final String zombiesUsername;
    private final Queue<Intent> inbox = new ConcurrentLinkedQueue<>();
    private final MatchSnapshotBuilder snapshots = new MatchSnapshotBuilder();

    private volatile List<String> plantLoadout = List.of();
    private volatile List<String> zombieLoadout = List.of();
    private volatile boolean plantsReady;
    private volatile boolean zombiesReady;
    private volatile Role leaver;
    private volatile String leaveReason = "Your opponent left the match.";

    private IZombieMatch match;
    private boolean started;
    private int tickCount;

    public MatchSession(String matchId, ClientSession plantsClient, ClientSession zombiesClient) {
        this.matchId = matchId;
        this.plantsClient = plantsClient;
        this.zombiesClient = zombiesClient;
        this.plantsUsername = plantsClient.getUsername();
        this.zombiesUsername = zombiesClient.getUsername();
    }

    public String getMatchId() {
        return matchId;
    }

    public IZombieMatch getMatch() {
        return match;
    }

    public ClientSession clientFor(Role role) {
        return role == Role.PLANTS ? plantsClient : zombiesClient;
    }

    public String usernameFor(Role role) {
        return role == Role.PLANTS ? plantsUsername : zombiesUsername;
    }

    public Role roleOf(ClientSession session) {
        if (session == plantsClient) return Role.PLANTS;
        if (session == zombiesClient) return Role.ZOMBIES;
        return null;
    }

    public boolean involves(ClientSession session) {
        return session == plantsClient || session == zombiesClient;
    }

    public void markReady(Role role, List<String> loadout) {
        List<String> picks = loadout == null ? List.of() : List.copyOf(loadout);
        if (role == Role.PLANTS) {
            plantLoadout = picks;
            plantsReady = true;
        } else if (role == Role.ZOMBIES) {
            zombieLoadout = picks;
            zombiesReady = true;
        }
    }

    public boolean isReady(Role role) {
        return role == Role.PLANTS ? plantsReady : zombiesReady;
    }

    public List<String> loadoutFor(Role role) {
        return role == Role.PLANTS ? plantLoadout : zombieLoadout;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean canStart() {
        return plantsReady && zombiesReady && leaver == null;
    }

    public boolean startIfReady() {
        if (started || !canStart()) return false;
        match = new IZombieMatch(IZombieMatch.ONLINE_MATCH_SECONDS, plantLoadout, zombieLoadout);
        started = true;
        return true;
    }

    public void requestLeave(Role role, String reason) {
        if (role == null || leaver != null) return;
        if (reason != null && !reason.isBlank()) leaveReason = reason;
        leaver = role;
    }

    public Role pendingLeaver() {
        return leaver;
    }

    public String leaveReason() {
        return leaveReason;
    }

    public void submit(Intent intent) {
        inbox.offer(intent);
    }

    public Queue<Intent> inbox() {
        return inbox;
    }

    public int tickCount() {
        return tickCount;
    }

    public void countTick() {
        tickCount++;
    }

    public void captureRemovals(List<Plant> plantsBefore, List<ZombieProjectile> shotsBefore) {
        snapshots.captureRemovals(match, plantsBefore, shotsBefore);
    }

    public void clearRemovals() {
        snapshots.clearRemovals();
    }

    public void forgetDeadEntities() {
        snapshots.forgetDeadEntities(match);
    }

    public MatchSnapshot snapshot(Role viewer) {
        return snapshots.build(match, viewer, tickCount);
    }

    public List<Role> roles() {
        return List.of(Role.PLANTS, Role.ZOMBIES);
    }
}
