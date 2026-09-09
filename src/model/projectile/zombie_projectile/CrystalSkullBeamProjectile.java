package model.projectile.zombie_projectile;

import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

/**
 * Purely visual: doesn't travel like a normal shot. Damage is applied by
 * {@link model.collections.zombie.zombie_effect.CrystalSkullBeamEffect} the
 * instant the beam fires; this projectile just sticks around for a short
 * beat so the renderer can draw the CRYSTALSKULL_BEAM art scratched (stretched)
 * from the zombie's position to the targeted plant's position, then vanish.
 */
public class CrystalSkullBeamProjectile extends ZombieProjectile {

    public CrystalSkullBeamProjectile(Position sourcePosition, Position targetPosition,
                                      double visibleDuration, GameSession session) {
        super(sourcePosition, targetPosition, Math.max(0.05, visibleDuration),
                "ZombieCrystalSkull", session);
    }

    
    public Position getBeamTargetPosition() {
        return targetPosition;
    }

    @Override
    protected void updateFlightPath(double progress) {
        
        setPosition(startPosition);
    }

    @Override
    protected void onDestinationReached(GameSession session) {
        
    }
}
