package model.collections.zombie.zombie_defense;

import model.collections.zombie.Zombie;
import model.utils.GameSession;

public class ArmorBasedDefense implements DefenseBehavior {

    @Override
    public int handleDamage(Zombie zombie, int incomingDamage, Object damageSource, GameSession session) {
        return incomingDamage;
    }
}