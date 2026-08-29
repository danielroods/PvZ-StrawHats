package model.projectile;

import model.match_mechanisms.vector.Position;
import service.GameClock;

import java.util.Random;

/**
 * Movement used by the Bowling Bulb's projectile so it rolls the same way
 * the Bowling Wall-nut does in the Wall-nut Bowling mini-game
 * ({@link model.match.mini_games.wallnutbowlling.nut.BowlingWallnut}):
 * it travels in a straight line at a constant speed (no gravity, no
 * height/arc) and, each time it hits a zombie, its path deflects by a
 * fixed angle instead of slowing down or bouncing off a floor.
 */
public class RollingBounceMove implements MoveStrategy {
    private static final double DEFLECT_ANGLE_DEGREES = 45.0;
    private static final Random RAND = new Random();

    private final double speed;
    private Boolean angleDownwards = null;

    public RollingBounceMove(double speed) {
        this.speed = speed;
    }

    @Override
    public void move(Projectile projectile) {
        Position pos = projectile.getPosition();
        Position velocity = projectile.getSpeed();
        if (pos == null || velocity == null) return;

        Position newPos = pos.add(velocity.scale(GameClock.SECONDS_PER_TICK));
        projectile.setPosition(newPos);
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
