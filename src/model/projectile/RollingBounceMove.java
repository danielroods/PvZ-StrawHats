package model.projectile;

import model.match_mechanisms.vector.Position;

import java.util.Random;


public class RollingBounceMove implements MoveStrategy {
    private static final double DEFLECT_ANGLE_DEGREES = 45.0;
    private static final Random RAND = new Random();

    private final double speed;
    private Boolean angleDownwards = null;

    public RollingBounceMove(double speed) {
        this.speed = speed;
    }

    @Override
    public void move(Projectile projectile, double deltaSeconds) {
        Position pos = projectile.getPosition();
        Position velocity = projectile.getSpeed();
        if (pos == null || velocity == null) return;

        projectile.setPosition(pos.add(velocity.scale(deltaSeconds)));
    }

    @Override
    public void onHit(Projectile projectile) {
        Position velocity = projectile.getSpeed();
        if (velocity == null) return;

        angleDownwards = Math.abs(velocity.y()) < 1e-6
                ? RAND.nextBoolean()
                : velocity.y() < 0;

        double radians = Math.toRadians(DEFLECT_ANGLE_DEGREES);
        double horizontalSign = velocity.x() < 0 ? -1 : 1;

        double newVx = horizontalSign * speed * Math.cos(radians);
        double newVy = (angleDownwards ? 1 : -1) * speed * Math.sin(radians);

        projectile.setSpeed(new Position(newVx, newVy));
    }
}
