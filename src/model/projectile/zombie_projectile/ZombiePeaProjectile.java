package model.projectile.zombie_projectile;

import model.collections.plant.Plant;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class ZombiePeaProjectile extends ZombieProjectile {

    public static final double SPEED = 3.6;
    private static final double HIT_RADIUS = 0.45;
    private static final double DESPAWN_X = -1.0;

    private final int damage;
    private final int lane;
    private double previousX;
    private boolean splatted;

    public ZombiePeaProjectile(Position startPosition, int damage, GameSession session) {
        super(startPosition, new Position(DESPAWN_X, startPosition.y()),
                Math.max(0.1, (startPosition.x() - DESPAWN_X) / SPEED),
                "PeashooterZombie", session);
        this.damage = damage;
        this.lane = (int) Math.round(startPosition.y());
        this.previousX = startPosition.x();
    }

    public int getDamage() {
        return damage;
    }

    public int getLane() {
        return lane;
    }

    public boolean hasSplatted() {
        return splatted;
    }

    @Override
    protected void updateFlightPath(double progress) {
        double currentX = startPosition.x()
                + (targetPosition.x() - startPosition.x()) * progress;
        setPosition(new Position(currentX, startPosition.y()));

        if (session == null || session.getEnvironment() == null) {
            previousX = currentX;
            return;
        }

        Plant hit = firstPlantInSweep(currentX);
        previousX = currentX;
        if (hit == null) return;

        hit.takeDamage(damage, null);
        splatted = true;
        setPosition(new Position(hit.getPosition().x(), startPosition.y()));
        setAlive(false);
    }

    private Plant firstPlantInSweep(double currentX) {
        double from = currentX - HIT_RADIUS;
        double to = previousX + HIT_RADIUS;
        Plant best = null;
        for (Plant plant : session.getPlants()) {
            if (plant == null || !plant.isAlive() || plant.getPosition() == null) continue;
            if ((int) Math.round(plant.getPosition().y()) != lane) continue;
            double plantX = plant.getPosition().x();
            if (plantX < from || plantX > to) continue;
            if (best == null || plantX > best.getPosition().x()) best = plant;
        }
        return best;
    }

    @Override
    protected void onDestinationReached(GameSession session) {
    }
}
