package model.collections.zombie.zombie_move;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.TileType;
import model.pitches.WaterCrossing;
import model.pitches.obstacles.Bridge;
import model.utils.GameSession;


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
                
                nextPos = new Position(pos.x(), nextPos.y());
            } else if (session != null) {
                nextPos = applySliderRedirect(zombie, pos, nextPos, session);
            }
        }

        zombie.setPosition(nextPos);
    }
}