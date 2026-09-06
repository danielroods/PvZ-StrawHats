package model.collections.zombie.zombie_move;

import model.collections.animations.AnimationFactory;
import model.collections.animations.ZombieAnimationRegistry;
import model.collections.zombie.VulnerabilityType;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
import model.utils.GameSession;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

public final class JetpackFlyMove implements MoveBehavior {

    public static final String CLIP_ENTER = "enter";
    public static final String CLIP_FLY_UP = "fly_up";
    public static final String CLIP_UP_IDLE = "up_idle";
    public static final String CLIP_FLY_DOWN = "fly_down";

    private static final double FALLBACK_ENTER_SECONDS = 1.0;
    private static final double FALLBACK_CLIMB_SECONDS = 0.7;
    private static final double CRUISE_HEIGHT_TILES = 0.95;
    private static final double AIRBORNE_SPEED_SCALE = 1.35;
    private static final double LANDING_DEADLINE_COLUMN = 1.2;

    private static final Random RANDOM = new Random();

    private enum Phase { ENTERING, GROUND, CLIMBING, CRUISING, DESCENDING }

    private static final class State {
        Phase phase = Phase.ENTERING;
        double phaseElapsed;
        double groundCooldown;
    }

    private final double takeoffCooldownSeconds;
    private final double takeoffChancePerSecond;
    private final double flightSeconds;
    private final Map<Zombie, State> states = new IdentityHashMap<>();

    public JetpackFlyMove(double takeoffCooldownSeconds, double takeoffChancePerSecond,
                          double flightSeconds) {
        this.takeoffCooldownSeconds = takeoffCooldownSeconds <= 0 ? 4.0 : takeoffCooldownSeconds;
        this.takeoffChancePerSecond = takeoffChancePerSecond <= 0 ? 0.35 : takeoffChancePerSecond;
        this.flightSeconds = flightSeconds <= 0 ? 5.0 : flightSeconds;
    }

    @Override
    public void move(Zombie zombie, double deltaTime, GameSession session) {
        Position pos = zombie.getPosition();
        Position speed = zombie.getSpeed();
        if (pos == null || speed == null) return;

        State state = states.computeIfAbsent(zombie, z -> new State());
        state.phaseElapsed += deltaTime;

        switch (state.phase) {
            case ENTERING -> {
                zombie.setActionAnimationState(CLIP_ENTER, 0, false);
                if (state.phaseElapsed >= clipSeconds(zombie, CLIP_ENTER, FALLBACK_ENTER_SECONDS)) {
                    beginGround(zombie, state);
                }
            }
            case GROUND -> {
                advance(zombie, speed, deltaTime, 1.0, session);
                state.groundCooldown = Math.max(0, state.groundCooldown - deltaTime);
                if (canTakeOff(zombie, session, state, deltaTime)) beginPhase(state, Phase.CLIMBING);
            }
            case CLIMBING -> {
                double climb = clipSeconds(zombie, CLIP_FLY_UP, FALLBACK_CLIMB_SECONDS);
                zombie.setActionAnimationState(CLIP_FLY_UP, 0, false);
                zombie.setHoverHeight(CRUISE_HEIGHT_TILES
                        * Math.min(1.0, climb <= 0 ? 1.0 : state.phaseElapsed / climb));
                if (state.phaseElapsed >= climb) {
                    zombie.setHoverHeight(CRUISE_HEIGHT_TILES);
                    zombie.setVulnerabilityState(VulnerabilityType.AIRBORNE);
                    zombie.setIgnoreTargetAcquisition(true);
                    beginPhase(state, Phase.CRUISING);
                }
            }
            case CRUISING -> {
                zombie.setActionAnimationState(CLIP_UP_IDLE, 0, true);
                advance(zombie, speed, deltaTime, AIRBORNE_SPEED_SCALE, session);
                boolean atHouse = zombie.getPosition() != null
                        && zombie.getPosition().x() <= LANDING_DEADLINE_COLUMN;
                if (state.phaseElapsed >= flightSeconds || atHouse) {
                    beginPhase(state, Phase.DESCENDING);
                }
            }
            case DESCENDING -> {
                double fall = clipSeconds(zombie, CLIP_FLY_DOWN, FALLBACK_CLIMB_SECONDS);
                zombie.setActionAnimationState(CLIP_FLY_DOWN, 0, false);
                zombie.setHoverHeight(CRUISE_HEIGHT_TILES
                        * Math.max(0.0, 1.0 - (fall <= 0 ? 1.0 : state.phaseElapsed / fall)));
                if (state.phaseElapsed >= fall) beginGround(zombie, state);
            }
            default -> beginGround(zombie, state);
        }
    }

    private void beginGround(Zombie zombie, State state) {
        zombie.setHoverHeight(0);
        zombie.setVulnerabilityState(VulnerabilityType.FULLY_VULNERABLE);
        zombie.setIgnoreTargetAcquisition(false);
        zombie.clearActionAnimationState();
        state.groundCooldown = takeoffCooldownSeconds;
        beginPhase(state, Phase.GROUND);
    }

    private static void beginPhase(State state, Phase phase) {
        state.phase = phase;
        state.phaseElapsed = 0;
    }

    private boolean canTakeOff(Zombie zombie, GameSession session, State state, double deltaTime) {
        if (state.groundCooldown > 0) return false;
        Position pos = zombie.getPosition();
        if (pos == null || pos.x() <= LANDING_DEADLINE_COLUMN) return false;
        if (hasPlantAhead(zombie, session)) return false;
        return RANDOM.nextDouble() < takeoffChancePerSecond * deltaTime;
    }

    private static boolean hasPlantAhead(Zombie zombie, GameSession session) {
        if (session == null || session.getEnvironment() == null || zombie.getPosition() == null) {
            return false;
        }
        Environment lawn = session.getEnvironment();
        int row = (int) Math.round(zombie.getPosition().y());
        int col = (int) Math.floor(zombie.getPosition().x());
        for (int scan = col; scan >= col - 1; scan--) {
            if (scan < 0 || scan >= lawn.getCols()) continue;
            Cell cell = lawn.getCell(row, scan);
            if (cell != null && cell.getPlant() != null && cell.getPlant().isAlive()) return true;
        }
        return false;
    }

    private void advance(Zombie zombie, Position speed, double deltaTime,
                         double speedScale, GameSession session) {
        Position current = zombie.getPosition();
        if (current == null) return;
        Position next = new Position(
                current.x() + speed.x() * deltaTime * speedScale,
                current.y() + speed.y() * deltaTime * speedScale);

        if (!zombie.isAirborne() && (int) current.x() != (int) next.x() && session != null) {
            next = applySliderRedirect(zombie, current, next, session);
        }
        zombie.setPosition(next);
    }

    private static double clipSeconds(Zombie zombie, String clip, double fallback) {
        float duration = AnimationFactory.exactClipDurationForPath(
                ZombieAnimationRegistry.pathFor(zombie.getAlias()), clip);
        return duration > 0f ? duration : fallback;
    }
}
