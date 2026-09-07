package model.projectile.targeting;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;

public final class TargetScan {

    private static final TargetScan EMPTY = new TargetScan(null, null);

    private final PlantTarget nearest;
    private final Zombie nearestZombie;

    TargetScan(PlantTarget nearest, Zombie nearestZombie) {
        this.nearest = nearest;
        this.nearestZombie = nearestZombie;
    }

    public static TargetScan empty() {
        return EMPTY;
    }

    public boolean hasTarget() {
        return nearest != null;
    }

    public PlantTarget nearest() {
        return nearest;
    }

    public Zombie zombie() {
        return nearestZombie;
    }

    public Position aimPosition() {
        if (nearestZombie != null && nearestZombie.getPosition() != null) {
            return nearestZombie.getPosition();
        }
        return nearest == null ? null : nearest.getPosition();
    }
}
