package model.collections.zombie.zombie_move;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

/**
 * Flight movement for the Pirate Seas seagull zombie.
 * <p>
 * Unlike {@link PirateGroundWalk}, a flyer never checks for water or bridges -
 * it simply keeps walking straight across every row, including the open-water
 * columns, which is exactly what {@link NormalWalk} already does. This class
 * exists as its own named behavior (rather than reusing NormalWalk directly)
 * so the move type in Zombie.json documents the intent and gives
 * {@link PelicanFlyMove} something dedicated to extend.
 */
public class SeagullFlyMove implements MoveBehavior {

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
        if (newCol != oldCol && session != null) {
            nextPos = applySliderRedirect(zombie, pos, nextPos, session);
        }

        zombie.setPosition(nextPos);
    }
}