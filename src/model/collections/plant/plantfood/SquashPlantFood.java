package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SquashPlantFood implements PlantFoodEffect {
    private static final double PLANTFOOD_DOWN_RIGHT_DURATION = 0.73;
    private static final double PLANTFOOD_DOWN_LEFT_DURATION = 0.70;
    private static final double TURN_DURATION = 1.20;
    private static final double LANDING_RADIUS = 0.52;

    private final int targetCount;
    private final Map<Plant, State> states = new IdentityHashMap<>();
    private final Random random = new Random();

    public SquashPlantFood(int targetCount) {
        this.targetCount = Math.max(1, targetCount);
    }

    private enum Phase { PLANTFOOD_JUMP, LANDING_HOLD, TURNING, COMPLETE }

    private static final class State {
        final Position origin;
        final List<Position> targets;
        int index;
        Phase phase = Phase.PLANTFOOD_JUMP;
        boolean facingLeft;

        State(Position origin, List<Position> targets) {
            this.origin = origin;
            this.targets = targets;
        }
    }

    @Override
    public double getDurationSeconds() {
        double longestTwoTargetSequence =
                PLANTFOOD_DOWN_RIGHT_DURATION
                        + TURN_DURATION
                        + PLANTFOOD_DOWN_LEFT_DURATION
                        + 0.10;
        double noTurnTwoTargetSequence =
                PLANTFOOD_DOWN_RIGHT_DURATION
                        + PLANTFOOD_DOWN_LEFT_DURATION
                        + 0.10;
        double oneTargetSequence =
                Math.max(PLANTFOOD_DOWN_RIGHT_DURATION, PLANTFOOD_DOWN_LEFT_DURATION)
                        + 0.10;
        double estimated = switch (Math.min(2, targetCount)) {
            case 2 -> Math.max(noTurnTwoTargetSequence, longestTwoTargetSequence);
            case 1 -> oneTargetSequence;
            default -> 0.0;
        };
        return Math.max(0.5, estimated);
    }

    @Override
    public boolean drivesActStrategy() {
        return true;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        Position origin = plant.getPosition();
        if (origin == null || session == null) return;

        List<Zombie> candidates = new ArrayList<>();
        for (Zombie zombie : session.getZombies()) {
            if (zombie != null && zombie.isAlive() && !zombie.isHypnotized() && zombie.getPosition() != null) {
                candidates.add(zombie);
            }
        }
        Collections.shuffle(candidates, random);

        List<Position> targets = new ArrayList<>();
        int count = Math.min(targetCount, candidates.size());
        for (int i = 0; i < count; i++) {
            targets.add(snapToBoardCell(candidates.get(i).getPosition()));
        }

        State state = new State(origin, targets);
        states.put(plant, state);
        plant.setSpecialInvulnerable(true);
        plant.setSquashActionState(true);

        if (targets.isEmpty()) {
            states.remove(plant);
            plant.clearSquashVisualPath();
            plant.setSquashActionState(false);
            plant.setSpecialInvulnerable(false);
            plant.clearVisualAnimationState();
            return;
        }

        state.facingLeft = targets.get(0).x() < origin.x();
        plant.setMeleeFacingLeft(state.facingLeft);
        startPlantFoodJump(plant, state);
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        State state = states.get(plant);
        if (state == null) return;

        double remaining = plant.getVisualAnimationRemaining();
        if (remaining > 0.0) return;

        if (state.phase == Phase.PLANTFOOD_JUMP) {
            smashAt(plant, GameSession.peekInstance(), state.targets.get(state.index));
            plant.holdVisualAnimationAtEnd();
            plant.setSquashVisualPosition(state.targets.get(state.index));
            state.index++;
            state.phase = Phase.LANDING_HOLD;
            return;
        }

        if (state.phase == Phase.LANDING_HOLD) {
            if (state.index >= state.targets.size()) {
                complete(plant, state);
                return;
            }

            boolean nextLeft = state.targets.get(state.index).x() < state.origin.x();
            if (nextLeft != state.facingLeft) {
                state.facingLeft = nextLeft;
                state.phase = Phase.TURNING;
                startTurn(plant);
            } else {
                startPlantFoodJump(plant, state);
            }
            return;
        }

        if (state.phase == Phase.TURNING) {
            startPlantFoodJump(plant, state);
        }
    }

    private void startPlantFoodJump(Plant plant, State state) {
        Position target = state.targets.get(state.index);
        plant.setSquashVisualPath(state.origin, target);
        plant.setMeleeFacingLeft(state.facingLeft);
        String animation = state.facingLeft
                ? "plantfood_jump_down_left"
                : "plantfood_jump_down_right";
        double duration = state.facingLeft ? PLANTFOOD_DOWN_LEFT_DURATION : PLANTFOOD_DOWN_RIGHT_DURATION;
        state.phase = Phase.PLANTFOOD_JUMP;
        plant.setState(Plant.PlantState.PREPPING);
        plant.setSpecialInvulnerable(true);
        plant.setSquashActionState(true);
        plant.setVisualAnimationState(animation, duration);
    }

    private void startTurn(Plant plant) {
        double duration = TURN_DURATION;
        plant.setState(Plant.PlantState.PREPPING);
        plant.setSpecialInvulnerable(true);
        plant.setSquashActionState(true);
        plant.setVisualAnimationState("turn", duration);
    }

    private void smashAt(Plant plant, GameSession session, Position landing) {
        if (session == null || landing == null) return;

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
            zombie.takeDamage(Math.max(1, plant.getDamage()), plant);
        }
    }

    private void complete(Plant plant, State state) {
        state.phase = Phase.COMPLETE;

        plant.clearSquashVisualPath();
        plant.setSquashActionState(false);
        plant.setSpecialInvulnerable(false);
        plant.setState(Plant.PlantState.ACTIVE);
        plant.setAlive(true);
        plant.clearVisualAnimationState();
        states.remove(plant);
    }

    private Position snapToBoardCell(Position position) {
        return new Position(Math.rint(position.x()), Math.rint(position.y()));
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }

    @Override
    public void reset() {
        states.clear();
    }
}
