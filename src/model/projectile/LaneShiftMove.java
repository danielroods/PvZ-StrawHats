package model.projectile;

import model.match_mechanisms.vector.Position;

public class LaneShiftMove implements MoveStrategy {

    private static final double SHIFT_SPAN_ROWS = 1.0;
    private static final double LAUNCH_VERTICAL_RATIO = Math.sqrt(0.5);
    private static final double SETTLE_EPSILON = 1.0e-6;

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
        if (pos == null || deltaSeconds <= 0) return;

        double travelSpeed = Math.abs(forwardSpeed);
        double remaining = targetY - pos.y();

        if (travelSpeed <= SETTLE_EPSILON || Math.abs(remaining) <= SETTLE_EPSILON) {
            projectile.setSpeed(Position.of(forwardSpeed, 0));
            projectile.setPosition(Position.of(pos.x() + forwardSpeed * deltaSeconds, targetY));
            return;
        }

        double distance = Math.abs(remaining);
        double progress = Math.min(1.0, distance / SHIFT_SPAN_ROWS);
        double verticalSpeed = Math.min(travelSpeed * LAUNCH_VERTICAL_RATIO * Math.sqrt(progress),
                distance / deltaSeconds);
        double horizontalSpeed = Math.signum(forwardSpeed)
                * Math.sqrt(Math.max(0, travelSpeed * travelSpeed - verticalSpeed * verticalSpeed));
        double climbSpeed = Math.signum(remaining) * verticalSpeed;

        double newX = pos.x() + horizontalSpeed * deltaSeconds;
        double newY = pos.y() + climbSpeed * deltaSeconds;

        if (Math.abs(targetY - newY) <= SETTLE_EPSILON) {
            newY = targetY;
            projectile.setSpeed(Position.of(forwardSpeed, 0));
        } else {
            projectile.setSpeed(Position.of(horizontalSpeed, climbSpeed));
        }
        projectile.setPosition(Position.of(newX, newY));
    }
}
