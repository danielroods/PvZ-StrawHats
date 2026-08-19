package model.match.mini_games.wallnutbowlling.nut;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

import java.util.Random;

public class BowlingWallnut extends Nut {
    private static final int NORMAL_ZOMBIE_DAMAGE = 500;
    private static final double ROLL_SPEED = SPEED * 0.85;
    private static final double DEFLECT_ANGLE_DEGREES = 45.0;
    private static final Random RAND = new Random();

    private int hitsSoFar = 0;

    public BowlingWallnut(Position position, Position direction) {
        super(position, direction);
    }

    @Override
    public double getRollSpeed() {
        return ROLL_SPEED;
    }

    @Override
    public boolean onHitZombie(Zombie zombie, GameSession session) {
        zombie.takeDamage(NORMAL_ZOMBIE_DAMAGE, this);
        deflectAfterHit();
        return false;
    }

    @Override
    public String getKindName() {
        return "Bowling Wall-nut";
    }

    public int getHitsSoFar() {
        return hitsSoFar;
    }

    private void deflectAfterHit() {
        boolean angleDownwards = Math.abs(direction.y()) < 1e-6
                ? RAND.nextBoolean()
                : direction.y() < 0;
        hitsSoFar++;

        double radians = Math.toRadians(DEFLECT_ANGLE_DEGREES);
        double forward = Math.cos(radians);
        double vertical = Math.sin(radians);
        direction = new Position(forward, angleDownwards ? vertical : -vertical);
    }
}
