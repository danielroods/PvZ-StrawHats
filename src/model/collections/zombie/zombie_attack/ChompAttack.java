package model.collections.zombie.zombie_attack;

import model.collections.Item;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.utils.GameSession;
import service.GameClock;

public class ChompAttack implements AttackBehavior {

    @Override
    public void attack(Zombie zombie, GameSession session) {
        Item target = ZombieTargeting.findTarget(zombie, session);
        if (target == null || !target.isAlive()) return;

        int damage = (int) (zombie.getEatDps() * GameClock.SECONDS_PER_TICK);

        if (target instanceof Plant p) {
            p.takeDamage(damage, zombie);
            if (p.isPotatoMine() && !p.isAlive()) {
                p.markPotatoMineEatenByZombie();
            }
            if (p.isGarlic()) {
                p.handleGarlicBite(zombie, session);
            }
        } else {
            target.takeDamage(damage);
        }
    }
}