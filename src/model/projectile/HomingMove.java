package model.projectile;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.targeting.PlantTarget;

public class HomingMove implements MoveStrategy {

    private static final double EPSILON = 1.0e-6;

    private final PlantTarget target;
    private final double speed;
    private final double turnRatePerSecond;

    public HomingMove(Zombie target, double speed, double turnRatePerSecond) {
        this(PlantTarget.ofZombie(target), speed, turnRatePerSecond);
    }

    public HomingMove(PlantTarget target, double speed, double turnRatePerSecond) {
        this.target = target;
        this.speed = speed;
        this.turnRatePerSecond = turnRatePerSecond;
    }

    @Override
    public void move(Projectile projectile, double deltaSeconds) {
        Position position = projectile.getPosition();
        Position velocity = projectile.getSpeed();
        if (position == null || velocity == null) return;

        Position targetPosition = target != null && target.isAlive() ? target.getPosition() : null;
        if (targetPosition != null) {
            Position desired = targetPosition.sub(position);
            if (desired.length() > EPSILON) {
                Position heading = velocity.length() > EPSILON
                        ? velocity.normalize() : desired.normalize();
                double blend = Math.min(1.0, turnRatePerSecond * deltaSeconds);
                Position steered = heading.add(desired.normalize().sub(heading).scale(blend));
                if (steered.length() > EPSILON) {
                    velocity = steered.normalize().scale(speed);
                    projectile.setSpeed(velocity);
                }
            }
        }

        projectile.setPosition(position.add(velocity.scale(deltaSeconds)));
    }
}
