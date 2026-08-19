package model.projectile;

import model.match_mechanisms.vector.Position;
import service.GameClock;

public class StraightMove implements MoveStrategy {

    @Override
    public void move(Projectile projectile) {
        Position pos = projectile.getPosition();
        Position speed = projectile.getSpeed();

        if (pos != null && speed != null) {
            Position newPos = pos.add(speed.scale(GameClock.SECONDS_PER_TICK));
            projectile.setPosition(newPos);
        }
    }
}