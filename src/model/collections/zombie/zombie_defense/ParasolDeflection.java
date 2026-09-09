package model.collections.zombie.zombie_defense;

import model.collections.zombie.Zombie;
import model.projectile.Projectile;
import model.utils.GameSession;

public class ParasolDeflection implements DefenseBehavior {

    @Override
    public int handleDamage(Zombie zombie, int incomingDamage, Object damageSource, GameSession session) {
        if (damageSource instanceof Projectile projectile) {
            if (projectile.isLobbed()) {
                // The parasol absorbs the hit entirely - no damage to this zombie -
                // and bats the shot onward: onto the next un-hit zombie in the lane
                // if there is one, otherwise a short hop further along the ground.
                projectile.bounceOverZombie(zombie, session);
                return 0;
            }
        }
        return incomingDamage;
    }
}