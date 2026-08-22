package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class SquashStrategy implements ActStrategy {
    private static final int DETECTION_CELL_RANGE = 1;
    private static final double LANDING_RADIUS = 0.52;
    private static final double JUMP_UP_DURATION = 0.80;
    private static final double JUMP_DOWN_DURATION = 0.80;
    private static final double JUMP_DOWN_FALL_DURATION = 0.20;
    private static final double TURN_DURATION = 1.20;

    private final Map<Plant, Action> actions = new IdentityHashMap<>();

    private enum Phase { JUMP_UP, JUMP_DOWN, LANDED_STEADY, TURNING }

    private static final class Action {
        final Position origin;
        int remainingSmashes;
        Position target;
        Phase phase;
        boolean facingLeft;

        Action(Position origin, Position target, int remainingSmashes, boolean facingLeft) {
            this.origin = origin;
            this.target = target;
            this.remainingSmashes = remainingSmashes;
            this.facingLeft = facingLeft;
            this.phase = Phase.JUMP_UP;
        }
    }

    @Override
    public void act(Plant user, GameSession session) {
        if (session == null || user.getPosition() == null || !user.isAlive()) return;

        Action action = actions.get(user);
        if (action != null) {
            if (user.getVisualAnimationRemaining() > 0.0) return;
            advanceAction(user, session, action);
            return;
        }

        if (user.getIntervalTimer() > 0.0) return;

        Zombie target = findNearestTarget(user, session, user.getPosition());
        if (target == null) return;

        Position origin = user.getPosition();
        Position targetPosition = snapToBoardCell(target.getPosition());
        boolean left = targetPosition.x() < origin.x();
        int smashCount = 1 + Math.max(0, (int) Math.round(
                user.getSpecialUpgrade("BONUS_SMASH_CHARGES", 0)));

        Action newAction = new Action(origin, targetPosition, smashCount, left);
        actions.put(user, newAction);
        startJumpUp(user, newAction);
    }

    private void advanceAction(Plant user, GameSession session, Action action) {
        if (action.phase == Phase.JUMP_UP) {
            action.phase = Phase.JUMP_DOWN;
            startJumpDown(user, action);
            return;
        }

        if (action.phase == Phase.JUMP_DOWN) {
            double elapsed = user.getVisualAnimationElapsed();
            if (elapsed >= JUMP_DOWN_FALL_DURATION - 0.0001) {
                smashLandingTile(user, session, action.target);
                action.remainingSmashes--;
                user.setSquashVisualPosition(action.target);
                action.phase = Phase.LANDED_STEADY;
            }
            return;
        }

        if (action.phase == Phase.LANDED_STEADY) {
            if (user.getVisualAnimationRemaining() > 0.0) return;

            if (action.remainingSmashes <= 0) {
                finish(user);
                actions.remove(user);
                return;
            }


            Zombie next = findNearestTarget(user, session, action.origin);
            if (next == null) {
                finish(user);
                actions.remove(user);
                return;
            }

            Position nextTarget = snapToBoardCell(next.getPosition());
            boolean nextLeft = nextTarget.x() < action.origin.x();
            if (nextLeft != action.facingLeft) {
                action.facingLeft = nextLeft;
                action.phase = Phase.TURNING;
                action.target = nextTarget;
                startTurn(user);
            } else {
                action.target = nextTarget;
                action.phase = Phase.JUMP_UP;
                startJumpUp(user, action);
            }
            return;
        }

        action.phase = Phase.JUMP_UP;
        user.setMeleeFacingLeft(action.facingLeft);
        startJumpUp(user, action);
    }

    private void startJumpUp(Plant user, Action action) {
        user.setSquashVisualPath(action.origin, action.target);
        String state = action.facingLeft ? "jump_up_left" : "jump_up_right";
        double duration = JUMP_UP_DURATION;
        user.setInternalTimer(0.0);
        user.setState(Plant.PlantState.PREPPING);
        user.setSpecialInvulnerable(true);
        user.setSquashActionState(true);
        user.setMeleeFacingLeft(action.facingLeft);
        user.setVisualAnimationState(state, duration);
    }

    private void startJumpDown(Plant user, Action action) {
        String state = action.facingLeft ? "jump_down_left" : "jump_down_right";
        double duration = JUMP_DOWN_DURATION;
        user.setInternalTimer(0.0);
        user.setState(Plant.PlantState.PREPPING);
        user.setSpecialInvulnerable(true);
        user.setSquashActionState(true);
        user.setMeleeFacingLeft(action.facingLeft);
        user.setVisualAnimationState(state, duration);
    }

    private void startTurn(Plant user) {
        double duration = TURN_DURATION;
        user.setInternalTimer(0.0);
        user.setState(Plant.PlantState.PREPPING);
        user.setSpecialInvulnerable(true);
        user.setSquashActionState(true);
        user.setVisualAnimationState("turn", duration);
    }

    private void smashLandingTile(Plant squash, GameSession session, Position landing) {
        if (landing == null) return;

        List<Zombie> victims = new ArrayList<>();
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position p = zombie.getPosition();
            if (Math.abs(p.x() - landing.x()) <= LANDING_RADIUS
                    && Math.abs(p.y() - landing.y()) <= LANDING_RADIUS) {
                victims.add(zombie);
            }
        }

        for (Zombie zombie : victims) {
            zombie.takeDamage(Math.max(1, squash.getDamage()), squash);
        }
    }

    private Zombie findNearestTarget(Plant user, GameSession session, Position center) {
        if (center == null || session.getZombies() == null) return null;

        final int plantCol = (int) Math.round(center.x());
        final int plantRow = (int) Math.round(center.y());

        return session.getZombies().stream()
                .filter(z -> z != null && z.isAlive() && !z.isHypnotized() && z.getPosition() != null)
                .filter(z -> (int) Math.round(z.getPosition().y()) == plantRow)
                .filter(z -> Math.abs((int) Math.round(z.getPosition().x()) - plantCol)
                        <= DETECTION_CELL_RANGE)
                .min(Comparator
                        .comparingInt((Zombie z) -> Math.abs((int) Math.round(z.getPosition().x()) - plantCol))
                        .thenComparingDouble(z -> Math.abs(z.getPosition().x() - center.x())))
                .orElse(null);
    }

    private Position snapToBoardCell(Position position) {
        return new Position(Math.rint(position.x()), Math.rint(position.y()));
    }

    private void finish(Plant user) {
        user.clearSquashVisualPath();
        user.setSquashActionState(false);
        user.clearVisualAnimationState();
        user.setSpecialInvulnerable(false);
        user.setAlive(false);
    }
}
