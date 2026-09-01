package net.client;

import model.match.mini_games.izombie.IZombieMatch;
import model.match.mini_games.izombie.IZombieMatch.Role;
import model.utils.GameSession;
import net.dto.MatchSnapshot;
import service.GameClock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class NetMatchState {

    public record Reaction(String fromUsername, String kind, int index, float age) { }

    private static final float MIN_SNAPSHOT_WINDOW = (float) GameClock.SECONDS_PER_TICK;
    private static final float MAX_SNAPSHOT_WINDOW = 0.5f;

    private final String matchId;
    private final Role role;
    private final String opponentUsername;
    private final String opponentNickname;
    private final GameSession shadow;
    private final SnapshotApplier applier;
    private final Deque<String> rejections = new ArrayDeque<>();
    private final List<String> loadout = new ArrayList<>();

    private double matchSeconds = IZombieMatch.ONLINE_MATCH_SECONDS;
    private MatchSnapshot previous;
    private MatchSnapshot current;
    private float sinceSnapshot;
    private float snapshotWindow = (float) GameClock.SECONDS_PER_TICK;
    private boolean plantsReady;
    private boolean zombiesReady;
    private boolean started;
    private boolean ended;
    private boolean won;
    private String endReason = "";
    private Reaction incomingReaction;
    private float reactionAge;
    private int lastBrainEatenRow = -1;

    public NetMatchState(String matchId, Role role, String opponentUsername,
                         String opponentNickname) {
        this.matchId = matchId;
        this.role = role;
        this.opponentUsername = opponentUsername;
        this.opponentNickname = opponentNickname;
        this.shadow = new GameSession(IZombieMatch.ROWS, IZombieMatch.COLS);
        this.shadow.setLawnMowersEnabled(false);
        this.shadow.setSkySunEnabled(false);
        this.shadow.setZombieBreachesEnabled(false);
        this.applier = new SnapshotApplier(shadow);
    }

    public String getMatchId() {
        return matchId;
    }

    public Role getRole() {
        return role;
    }

    public boolean isPlantSide() {
        return role == Role.PLANTS;
    }

    public String getOpponentUsername() {
        return opponentUsername;
    }

    public String getOpponentNickname() {
        return opponentNickname == null ? opponentUsername : opponentNickname;
    }

    public GameSession getShadowSession() {
        return shadow;
    }

    public MatchSnapshot getSnapshot() {
        return current;
    }

    public boolean hasSnapshot() {
        return current != null;
    }

    public double getMatchSeconds() {
        return matchSeconds;
    }

    public void setMatchSeconds(double seconds) {
        if (seconds > 0) this.matchSeconds = seconds;
    }

    public List<String> getLoadout() {
        return loadout;
    }

    public void setLoadout(List<String> picks) {
        loadout.clear();
        if (picks != null) loadout.addAll(picks);
    }

    public void setReadyFlags(boolean plants, boolean zombies) {
        this.plantsReady = plants;
        this.zombiesReady = zombies;
    }

    public boolean isMyReady() {
        return isPlantSide() ? plantsReady : zombiesReady;
    }

    public boolean isOpponentReady() {
        return isPlantSide() ? zombiesReady : plantsReady;
    }

    public boolean isStarted() {
        return started;
    }

    public void markStarted() {
        started = true;
    }

    public void applySnapshot(MatchSnapshot snapshot) {
        if (snapshot != null && current != null) {
            snapshotWindow = Math.max(MIN_SNAPSHOT_WINDOW,
                    Math.min(MAX_SNAPSHOT_WINDOW, sinceSnapshot));
        }
        previous = current;
        current = snapshot;
        sinceSnapshot = 0f;
        GameSession.setCurrent(shadow);
        applier.apply(snapshot);
        if (snapshot != null) {
            setMatchSeconds(snapshot.matchSeconds);
            shadow.setSunCount(isPlantSide() ? snapshot.plantSun : snapshot.zombieSun);
            shadow.setPlantFoodCount(snapshot.plantFood);
        }
    }

    public java.util.List<model.collections.zombie.Zombie> drainRemovedZombies() {
        return applier.drainRemovedZombies();
    }

    public java.util.List<model.collections.plant.Plant> drainRemovedPlants() {
        return applier.drainRemovedPlants();
    }

    public void advance(float delta) {
        sinceSnapshot += delta;
        applier.advanceProjectiles(delta);
        applier.advancePlantVisuals(delta);
        if (incomingReaction != null) {
            reactionAge += delta;
            if (reactionAge > 2.5f) incomingReaction = null;
        }
        if (previous == null || current == null) return;
        float alpha = Math.min(1f, sinceSnapshot / snapshotWindow);
        applier.interpolate(previous, current, alpha);
    }

    public void pushRejection(String reason) {
        if (reason != null) rejections.addLast(reason);
    }

    public String pollRejection() {
        return rejections.pollFirst();
    }

    public void setReaction(String fromUsername, String kind, int index) {
        incomingReaction = new Reaction(fromUsername, kind, index, 0f);
        reactionAge = 0f;
    }

    public Reaction getIncomingReaction() {
        return incomingReaction;
    }

    public float getReactionAge() {
        return reactionAge;
    }

    public void markBrainEaten(int row) {
        lastBrainEatenRow = row;
    }

    public int consumeBrainEatenRow() {
        int row = lastBrainEatenRow;
        lastBrainEatenRow = -1;
        return row;
    }

    public void end(boolean youWon, String reason) {
        ended = true;
        won = youWon;
        endReason = reason == null ? "" : reason;
    }

    public boolean isEnded() {
        return ended;
    }

    public boolean isWon() {
        return won;
    }

    public String getEndReason() {
        return endReason;
    }

    public int getPlantSun() {
        return current == null ? 0 : current.plantSun;
    }

    public int getZombieSun() {
        return current == null ? 0 : current.zombieSun;
    }

    public int getMySun() {
        return isPlantSide() ? getPlantSun() : getZombieSun();
    }

    public double getRemainingSeconds() {
        return current == null ? matchSeconds : current.remaining;
    }

    public int getBrainsEaten() {
        return current == null ? 0 : current.brainsEaten;
    }

    public int getBrainCount() {
        return model.match.mini_games.izombie.IZombieMatch.ROWS;
    }

    public void dispose() {
        applier.clear();
    }
}
