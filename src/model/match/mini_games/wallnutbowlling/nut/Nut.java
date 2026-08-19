package model.match.mini_games.wallnutbowlling.nut;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public abstract class Nut {
    protected static final double SPEED = 3.0; // columns per second

    protected Position position;
    protected Position direction;
    protected boolean alive = true;
    private double rolledDistance;

    protected Nut(Position position, Position direction) {
        this.position = position;
        this.direction = direction;
    }

    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }
    public Position getDirection() { return direction; }
    public boolean isAlive() { return alive; }
    public void kill() { alive = false; }

    public double getRolledDistance() { return rolledDistance; }

    public double getRollSpeed() { return SPEED; }

    public String getKindName() {
        return getClass().getSimpleName();
    }

    public void move(double deltaSeconds) {
        if (!alive) return;
        Position step = direction.scale(getRollSpeed() * deltaSeconds);
        position = position.add(step);
        rolledDistance += step.length();
    }

    public void bounceOffLaneEdge() {
        direction = new Position(direction.x(), -direction.y());
    }

    public abstract boolean onHitZombie(Zombie zombie, GameSession session);
}
