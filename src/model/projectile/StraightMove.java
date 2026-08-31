package model.projectile;

import model.match_mechanisms.vector.Position;

public class StraightMove implements MoveStrategy {

    @Override
    public void move(Projectile projectile, double deltaSeconds) {
        Position pos = projectile.getPosition();
        Position speed = projectile.getSpeed();

        if (pos != null && speed != null) {
            projectile.setPosition(pos.add(speed.scale(deltaSeconds)));
        }
    }
}
