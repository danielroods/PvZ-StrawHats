package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.LaneShiftMove;
import model.projectile.MoveStrategy;
import model.projectile.Projectile;
import model.projectile.StraightMove;
import model.projectile.hit.HitEffectStrategy;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PeaBarrage extends TimedProjectileBurst {
    private static final double GIANT_PEA_SPEED = 3.0;
    private static final double GIANT_PEA_KNOCKBACK = 0.35;

    private final int giantPeaDamage;
    private final String giantPeaPath;

    public PeaBarrage(int shots, double fireInterval, int giantPeaDamage, String giantPeaPath) {
        super(shots, fireInterval, 0.6);
        this.giantPeaDamage = Math.max(0, giantPeaDamage);
        this.giantPeaPath = giantPeaPath;
    }

    @Override
    protected void onBurstEnd(Plant plant, GameSession session) {
        if (giantPeaDamage <= 0 || session == null || plant.getPosition() == null) return;

        for (Position direction : distinctDirections(plant)) {
            spawnGiantPea(plant, session, direction);
        }
    }

    private void spawnGiantPea(Plant plant, GameSession session, Position direction) {
        double speed = session.projectileSpeed(GIANT_PEA_SPEED);
        Position velocity = direction.normalize().scale(speed);

        Projectile giant = new Projectile(plant,
                plant.getPosition(),
                velocity,
                null,
                giantPeaDamage,
                buildMoveStrategy(plant, direction, speed),
                new GiantPeaHit(elementalStatus(plant))
        );
        giant.setSourceDisplay(plant.getName(), true);
        if (giantPeaPath != null) {
            giant.setDisplay(giantPeaPath, "animation");
        }
        session.getProjectiles().add(giant);
    }

    private static Zombie.Status elementalStatus(Plant plant) {
        if (plant.getTags().contains(PlantTag.FIRE)) return Zombie.Status.FIRED;
        if (plant.getTags().contains(PlantTag.ICE)) return Zombie.Status.FREEZE;
        if (plant.getTags().contains(PlantTag.POISON)) return Zombie.Status.POISONED;
        return null;
    }

    private static MoveStrategy buildMoveStrategy(Plant plant, Position direction, double speed) {
        double dx = direction.x();
        double dy = direction.y();
        if (dx == 0 || Math.abs(Math.abs(dy) - 1.0) > 1.0e-6) return new StraightMove();
        double laneY = Math.round(plant.getPosition().y()) + dy;
        return new LaneShiftMove(laneY, Math.signum(dx) * speed);
    }

    private static List<Position> distinctDirections(Plant plant) {
        List<Position> vectors = plant.getShootingVectors();
        Set<String> seen = new LinkedHashSet<>();
        List<Position> result = new ArrayList<>();
        if (vectors == null || vectors.isEmpty()) {
            result.add(new Position(1, 0));
            return result;
        }
        for (Position vector : vectors) {
            if (vector == null) continue;
            Position normalized = vector.normalize();
            String key = Math.round(normalized.x() * 1000) + "," + Math.round(normalized.y() * 1000);
            if (seen.add(key)) result.add(vector);
        }
        if (result.isEmpty()) result.add(new Position(1, 0));
        return result;
    }

    private static final class GiantPeaHit implements HitEffectStrategy {
        private final Zombie.Status status;

        private GiantPeaHit(Zombie.Status status) {
            this.status = status;
        }

        @Override
        public void apply(Zombie zombie) {
            if (zombie == null || !zombie.isAlive() || status == null) return;
            zombie.applyStatus(status, status == Zombie.Status.FIRED ? 3.0 : 5.0);
        }

        @Override
        public int getPierceCount() { return -1; }

        @Override
        public double getKnockbackDistance() { return GIANT_PEA_KNOCKBACK; }

        @Override
        public boolean isFireDamage() { return status == Zombie.Status.FIRED; }
    }
}
