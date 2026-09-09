package model.collections.zombie.zombie_move;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.TileType;
import model.pitches.WaterCrossing;
import model.pitches.obstacles.Bridge;
import model.utils.GameSession;

/**
 * Ground-bound movement for the Pirate Seas chapter.
 * <p>
 * Behaves exactly like {@link NormalWalk} on dry land. The moment a step
 * would carry the zombie onto a {@link TileType#Water} cell that has no
 * {@link Bridge} plank on it, the zombie is held at the water's edge instead
 * (it just stands there and renders on that row, unable to cross) - matching
 * every land zombie in this chapter except the flying seagull/pelican
 * zombies, which use {@link SeagullFlyMove}/{@link PelicanFlyMove} and never
 * call this.
 */
public class PirateGroundWalk implements MoveBehavior {

    @Override
    public void move(Zombie zombie, double deltaTime, GameSession session) {
        Position pos = zombie.getPosition();
        Position speed = zombie.getSpeed();
        if (pos == null || speed == null) return;

        Position nextPos = new Position(
                pos.x() + speed.x() * deltaTime,
                pos.y() + speed.y() * deltaTime
        );

        int oldCol = (int) pos.x();
        int newCol = (int) nextPos.x();

        if (newCol != oldCol) {
            if (WaterCrossing.isOpenWater(session, (int) Math.round(nextPos.y()), newCol)) {
                // Held at the edge - keep the old column, drop only the row/vertical drift.
                nextPos = new Position(pos.x(), nextPos.y());
            } else if (session != null) {
                nextPos = applySliderRedirect(zombie, pos, nextPos, session);
            }
        }

        zombie.setPosition(nextPos);
    }
}