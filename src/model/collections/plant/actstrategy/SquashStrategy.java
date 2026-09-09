package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;

public class SquashStrategy implements ActStrategy {
    private static final int DETECTION_CELL_RANGE = 1;
    private static final double LANDING_RADIUS = 0.52;
    private static final double JUMP_UP_DURATION = 0.80;
    private static final double JUMP_DOWN_DURATION = 0.80;
    private static final double TURN_DURATION = 1.20;

    private final Map<Plant, Action> actions = new IdentityHashMap<>();

    private enum Phase { JUMP_UP, JUMP_DOWN, LANDED_STEADY, TURNING }

    private static final class Action {
        final Position origin;
        final Zombie targetZombie;
        Position target;
        int remainingSmashes;
        Phase phase;
        boolean facingLeft;

        Action(Position origin, Position target, Zombie targetZombie,
               int remainingSmashes, boolean facingLeft) {
            this.origin = origin;
            this.target = target;
            this.targetZombie = targetZombie;
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

        int smashCount = 1 + Math.max(0,
                (int) Math.round(user.getSpecialUpgrade("BONUS_SMASH_CHARGES", 0)));

        Action newAction = new Action(
                origin,
                targetPosition,
                target,
                smashCount,
                left
        );

        actions.put(user, newAction);
        startJumpUp(user, newAction);
    }

    private void advanceAction(Plant user, GameSession session, Action action) {
        switch (action.phase) {
            case JUMP_UP -> {
                action.phase = Phase.JUMP_DOWN;
                startJumpDown(user, action);
            }

            case JUMP_DOWN -> {
                
                smashLanding(user, session, action);
                action.remainingSmashes--;

                
                
                user.setSquashVisualPosition(action.target);
                user.clearVisualAnimationState();

                if (action.remainingSmashes <= 0) {
                    finish(user);
                    actions.remove(user);
                    return;
                }

                action.phase = Phase.LANDED_STEADY;
                user.setInternalTimer(0.0);
            }

            case LANDED_STEADY -> {
                Zombie next = findNearestTarget(user, session, action.origin);

                if (next == null) {
                    finish(user);
                    actions.remove(user);
                    return;
                }

                Position nextTarget = snapToBoardCell(next.getPosition());
                boolean nextLeft = nextTarget.x() < action.origin.x();

                action.target = nextTarget;

                if (nextLeft != action.facingLeft) {
                    action.facingLeft = nextLeft;
                    action.phase = Phase.TURNING;
                    startTurn(user);
                } else {
                    action.phase = Phase.JUMP_UP;
                    startJumpUp(user, action);
                }
            }

            case TURNING -> {
                action.phase = Phase.JUMP_UP;
                startJumpUp(user, action);
            }
        }
    }

    private void startJumpUp(Plant user, Action action) {
        user.setSquashVisualPath(action.origin, action.target);
        user.setMeleeFacingLeft(action.facingLeft);
        user.setState(Plant.PlantState.PREPPING);
        user.setSpecialInvulnerable(true);
        user.setSquashActionState(true);
        user.setInternalTimer(0.0);

        String state = action.facingLeft ? "jump_up_left" : "jump_up_right";
        user.setVisualAnimationState(state, JUMP_UP_DURATION);
    }

    private void startJumpDown(Plant user, Action action) {
        user.setMeleeFacingLeft(action.facingLeft);
        user.setState(Plant.PlantState.PREPPING);
        user.setSpecialInvulnerable(true);
        user.setSquashActionState(true);
        user.setInternalTimer(0.0);

        String state = action.facingLeft ? "jump_down_left" : "jump_down_right";
        user.setVisualAnimationState(state, JUMP_DOWN_DURATION);
    }

    private void startTurn(Plant user) {
        user.setState(Plant.PlantState.PREPPING);
        user.setSpecialInvulnerable(true);
        user.setSquashActionState(true);
        user.setInternalTimer(0.0);
        user.setVisualAnimationState("turn", TURN_DURATION);
    }

    private void smashLanding(Plant squash, GameSession session, Action action) {
        if (session == null || action == null || action.target == null) return;

        
        
        Zombie primary = action.targetZombie;
        if (primary != null && primary.isAlive()) {
            
            
            primary.takeDamage(Math.max(primary.getHP() + 1, squash.getDamage()), squash);
        }

        
        
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || zombie == primary || !zombie.isAlive()
                    || zombie.getPosition() == null) {
                continue;
            }

            Position p = zombie.getPosition();

            if (Math.abs(p.x() - action.target.x()) <= LANDING_RADIUS
                    && Math.abs(p.y() - action.target.y()) <= LANDING_RADIUS) {
                zombie.takeDamage(
                        Math.max(zombie.getHP() + 1, squash.getDamage()),
                        squash
                );
            }
        }
    }

    private Zombie findNearestTarget(Plant user, GameSession session, Position center) {
        if (center == null || session.getZombies() == null) return null;

        final int plantCol = (int) Math.round(center.x());
        final int plantRow = (int) Math.round(center.y());

        return session.getZombies().stream()
                .filter(z -> z != null
                        && z.isAlive()
                        && !z.isHypnotized()
                        && z.getPosition() != null)
                .filter(z -> (int) Math.round(z.getPosition().y()) == plantRow)
                .filter(z -> Math.abs(
                        (int) Math.round(z.getPosition().x()) - plantCol
                ) <= DETECTION_CELL_RANGE)
                .min(Comparator
                        .comparingInt((Zombie z) ->
                                Math.abs((int) Math.round(z.getPosition().x()) - plantCol))
                        .thenComparingDouble(z ->
                                Math.abs(z.getPosition().x() - center.x())))
                .orElse(null);
    }

    private Position snapToBoardCell(Position position) {
        return new Position(
                Math.rint(position.x()),
                Math.rint(position.y())
        );
    }

    private void finish(Plant user) {
        user.clearSquashVisualPath();
        user.setSquashActionState(false);
        user.setSpecialInvulnerable(false);
        user.clearVisualAnimationState();
        user.setState(Plant.PlantState.DYING);
        user.setAlive(false);
    }
}