package model.collections.zombie.zombie_move;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;


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