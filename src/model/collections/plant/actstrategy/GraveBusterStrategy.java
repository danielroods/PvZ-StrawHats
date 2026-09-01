package model.collections.plant.actstrategy;

import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.TileType;
import model.pitches.obstacles.Grave;
import model.utils.GameSession;

public class GraveBusterStrategy implements ActStrategy {

    public static final String CHEW_STATE = "attack";
    public static final String FINISH_STATE = "attack1";
    public static final String WATER_STATE = "water";

    private static final double MIN_BUST_SECONDS = 0.2;

    @Override
    public void act(Plant user, GameSession session) {
        if (user == null || session == null || !user.isAlive()) {
            return;
        }

        Position center = user.getPosition();
        if (center == null || session.getEnvironment() == null) {
            return;
        }

        int row = (int) Math.round(center.y());
        int col = (int) Math.round(center.x());
        Cell cell = session.getEnvironment().getCell(row, col);

        Grave grave = cell != null && cell.getObstacle() instanceof Grave found ? found : null;
        if (grave == null) {
            user.clearVisualAnimationState();
            user.setAlive(false);
            return;
        }

        double total = Math.max(MIN_BUST_SECONDS, user.getActionInterval());
        double remaining = Math.max(0.0, Math.min(total, user.getIntervalTimer()));

        if (remaining > 0.0) {
            showBustClip(user, cell, total, total - remaining);
            return;
        }

        showBustClip(user, cell, total, total);
        user.holdVisualAnimationAtEnd();
        session.damageGrave(cell, Math.max(1, user.getDamage()));
        user.setGraveBusterConsumedGrave(true);
        user.setAlive(false);
    }

    private void showBustClip(Plant user, Cell cell, double total, double elapsed) {
        boolean water = cell.getTile() != null && cell.getTile().type() == TileType.Water;

        String chewState = water ? WATER_STATE : CHEW_STATE;
        String finishState = water ? WATER_STATE : FINISH_STATE;

        double chewLength = 1.2;
        double finishLength = 1.3;

        double finishStart = Math.max(0.0, total - finishLength);

        String state;
        double inClip;
        double clipLength;
        if (elapsed < finishStart) {
            state = chewState;
            clipLength = chewLength;
            inClip = chewLength > 0 ? elapsed % chewLength : elapsed;
        } else {
            state = finishState;
            clipLength = finishLength;
            inClip = Math.min(elapsed - finishStart, Math.max(0.0, finishLength));
        }

        user.setVisualAnimationProgress(state, Math.max(clipLength - inClip, 0.0), inClip);
    }

    private double clipSeconds(Plant user, String state, double fallback) {
        float resolved = AnimationFactory.exactClipDurationForPath(
                AnimationFactory.pathForDisplayName(user.getName()), state);
        return resolved > 0f ? resolved : fallback;
    }
}
