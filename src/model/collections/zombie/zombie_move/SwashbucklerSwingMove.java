package model.collections.zombie.zombie_move;

import model.collections.zombie.Zombie;
import model.match.main.season.travellog.pirate.Pirate;
import model.match_mechanisms.vector.Position;
import model.pitches.Environment;
import model.pitches.WaterCrossing;
import model.utils.GameSession;

import java.util.Random;


public final class SwashbucklerSwingMove implements MoveBehavior {
    private static final Random RANDOM = new Random();

    private static final int SWING_COUNT = 3;
    private static final double SWING_SECONDS = 1.0;
    private static final double SUCCESS_CHANCE = 0.80;

    private final java.util.IdentityHashMap<Zombie, State> states = new java.util.IdentityHashMap<>();

    private static final class State {
        double elapsed;
        boolean resolved;
        boolean success;
    }

    @Override
    public void move(Zombie zombie, double deltaTime, GameSession session) {
        if (zombie == null || session == null || session.getLevel() == null
                || !(session.getLevel().getSeason() instanceof Pirate)
                || zombie.getPosition() == null) {
            return;
        }

        Environment lawn = session.getLawn();
        if (lawn == null) return;

        Position pos = zombie.getPosition();
        int row = (int) Math.round(pos.y());
        int waterStart = Math.max(0, lawn.getCols() - Pirate.WATER_COLUMN_COUNT);

        
        if (!WaterCrossing.isOpenWater(session, row, waterStart)) {
            zombie.clearActionAnimationState();
            zombie.setMoveBehavior(new PirateGroundWalk());
            new PirateGroundWalk().move(zombie, deltaTime, session);
            return;
        }

        State state = states.computeIfAbsent(zombie, z -> new State());

        if (!state.resolved) {
            state.elapsed += deltaTime;

            
            
            zombie.setActionAnimationState("swing back", 0, true);

            if (state.elapsed >= SWING_COUNT * SWING_SECONDS) {
                state.resolved = true;
                state.success = RANDOM.nextDouble() < SUCCESS_CHANCE;

                if (state.success) {
                    zombie.setPosition(new Position(waterStart, row));
                    zombie.setActionAnimationState("swing success", SWING_SECONDS, false);
                } else {
                    zombie.markSwashbucklerWaterDeath();
                    zombie.setActionAnimationState("swing failure", SWING_SECONDS, false);
                    zombie.setHp(0);
                }
            }
            return;
        }

        if (state.success) {
            
            if (zombie.getActionAnimationState() == null) {
                states.remove(zombie);
                zombie.setMoveBehavior(new PirateGroundWalk());
                new PirateGroundWalk().move(zombie, deltaTime, session);
            }
        }
    }
}
