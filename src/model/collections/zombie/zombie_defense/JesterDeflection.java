package model.collections.zombie.zombie_defense;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.collections.zombie.zombie_effect.RotationalTurbulenceState;
import model.match.main.season.travellog.cave.FrostbiteFreezing;
import model.match_mechanisms.vector.Position;
import model.projectile.Projectile;
import model.projectile.StraightMove;
import model.utils.GameSession;


public class JesterDeflection implements DefenseBehavior {
    public static final double SPIN_PERIOD = 1.0;

    @Override
    public int handleDamage(Zombie zombie, int incomingDamage, Object damageSource, GameSession session) {
        if (damageSource instanceof Projectile projectile) {
            if (isDeflectable(projectile)) {
                activateSpinning(zombie);
                reflectTowardsPlant(zombie, projectile, session);
                return 0;
            }
        }
        return incomingDamage;
    }

    private boolean isDeflectable(Projectile projectile) {
        return projectile.getMoveStrategy() instanceof StraightMove;
    }

    public void activateSpinning(Zombie zombie) {
        if (zombie.getEffectStatus() instanceof RotationalTurbulenceState spinEffect) {
            spinEffect.triggerGyratingState(SPIN_PERIOD);
        }
    }

    private void reflectTowardsPlant(Zombie zombie, Projectile projectile, GameSession session) {
        if (projectile == null) return;

        Plant targetPlant = searchClosestPlantInRow(zombie, session);
        if (targetPlant != null) {
            targetPlant.takeDamage(projectile.getDamage(), zombie);
            if (projectile.getHitEffectStrategy() instanceof model.projectile.hit.IceHit) {
                FrostbiteFreezing.addChillLevel(session, targetPlant);
            }
        }

        Position speed = projectile.getSpeed();
        if (speed != null) {
            double magnitude = Math.abs(speed.x());
            double reflectedX = zombie.isFacingRight() ? magnitude : -magnitude;
            projectile.setSpeed(new Position(reflectedX, speed.y()));
        }
        projectile.deflectTowardsPlant(zombie);
    }

    public Plant searchClosestPlantInRow(Zombie zombie, GameSession session) {
        Position zombiePos = zombie.getPosition();
        if (zombiePos == null || session == null || session.getPlants() == null) {
            return null;
        }

        double zombieRow = zombiePos.y();
        double zombieCol = zombiePos.x();

        Plant closestPlant = null;
        double minSeparation = Double.MAX_VALUE;

        for (Plant plant : session.getPlants()) {
            if (plant == null || !plant.isAlive() || plant.getPosition() == null) continue;

            if (Math.abs(plant.getPosition().y() - zombieRow) >= 0.5) continue;

            if (plant.getPosition().x() >= zombieCol) continue;

            double separation = zombieCol - plant.getPosition().x();
            if (separation < minSeparation) {
                minSeparation = separation;
                closestPlant = plant;
            }
        }

        return closestPlant;
    }
}