package model.collections.zombie.zombie_move;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

/**
 * Flight movement for the Pirate Seas pelican zombie.
 * <p>
 * Flies over water exactly like {@link SeagullFlyMove}, but can also glide
 * smoothly from its current row to a new one instead of snapping instantly,
 * which is the one behavioral difference the design calls out between the
 * two bird zombies. Call {@link #switchToRow(int, double)} to start a glide;
 * until it finishes, {@link #move} eases the row (y) toward the target while
 * the horizontal flight continues unaffected.
 */
public class PelicanFlyMove implements MoveBehavior {

    private final double rowGlideSpeed;
    private Double targetRow;

    public PelicanFlyMove() {
        this(3.0);
    }

    
    public PelicanFlyMove(double rowGlideSpeed) {
        this.rowGlideSpeed = rowGlideSpeed <= 0 ? 3.0 : rowGlideSpeed;
    }

    
    public void switchToRow(int row) {
        this.targetRow = (double) row;
    }

    public boolean isSwitchingRows() {
        return targetRow != null;
    }

    @Override
    public void move(Zombie zombie, double deltaTime, GameSession session) {
        Position pos = zombie.getPosition();
        Position speed = zombie.getSpeed();
        if (pos == null || speed == null) return;

        double nextX = pos.x() + speed.x() * deltaTime;
        double nextY = pos.y();

        if (targetRow != null) {
            double diff = targetRow - nextY;
            double step = rowGlideSpeed * deltaTime;
            if (Math.abs(diff) <= step) {
                nextY = targetRow;
                targetRow = null;
            } else {
                nextY += Math.signum(diff) * step;
            }
        }

        Position nextPos = new Position(nextX, nextY);

        int oldCol = (int) pos.x();
        int newCol = (int) nextX;
        if (newCol != oldCol && session != null) {
            nextPos = applySliderRedirect(zombie, pos, nextPos, session);
        }

        zombie.setPosition(nextPos);
    }
}