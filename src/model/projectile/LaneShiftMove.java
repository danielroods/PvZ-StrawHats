package model.projectile;

import model.match_mechanisms.vector.Position;

public class LaneShiftMove implements MoveStrategy {

    private final double targetY;
    private final double forwardSpeed;

    public LaneShiftMove(double targetY, double forwardSpeed) {
        this.targetY = targetY;
        this.forwardSpeed = forwardSpeed;
    }

    public double getTargetY() {
        return targetY;
    }

    public double getForwardSpeed() {
        return forwardSpeed;
    }

    @Override
    public void move(Projectile projectile, double deltaSeconds) {
        Position pos = projectile.getPosition();
        Position speed = projectile.getSpeed();
        if (pos == null || speed == null) return;

        double newX = pos.x() + speed.x() * deltaSeconds;
        double newY = pos.y() + speed.y() * deltaSeconds;

        boolean reached = (speed.y() > 0 && newY >= targetY)
                || (speed.y() < 0 && newY <= targetY)
                || speed.y() == 0;
        if (reached) {
            newY = targetY;
            projectile.setSpeed(Position.of(forwardSpeed, 0));
        }
        projectile.setPosition(Position.of(newX, newY));
    }
}
