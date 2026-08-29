package model.collections.zombie.zombie_effect;

import model.collections.armour.Armour;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieFactory;
import model.utils.GameSession;
import service.GameClock;

public class KingBuffEffect implements ZombieEffectStatus {

    public static final int MAX_CORONATIONS = 2;

    private final double coronationCooldown;
    private double rechargeTimer;
    private int coronations;

    public KingBuffEffect(double coronationCooldown) {
        this.coronationCooldown = coronationCooldown;
        this.rechargeTimer = coronationCooldown;
    }

    @Override
    public void applyTickEffect(Zombie king, GameSession session) {
        if (!king.isAlive() || king.getPosition() == null) return;
        if (coronations >= MAX_CORONATIONS) return;

        rechargeTimer += GameClock.SECONDS_PER_TICK;
        if (rechargeTimer >= coronationCooldown) {
            if (bestowKnightArmor(king, session)) {
                rechargeTimer = 0;
                coronations++;
            }
        }
    }

    private boolean bestowKnightArmor(Zombie monarch, GameSession session) {
        int sovereignRow = (int) monarch.getPosition().y();
        double sovereignCol = monarch.getPosition().x();

        return session.getZombies().stream()
                .filter(Zombie::isAlive)
                .filter(z -> z != monarch)
                .filter(z -> z.getFaction() == monarch.getFaction())
                .filter(z -> !z.getName().contains("King"))
                .filter(z -> (int) z.getPosition().y() == sovereignRow)
                .filter(z -> Math.abs(z.getPosition().x() - sovereignCol) <= 4.0)
                .filter(z -> z.getArmor() == null || z.getArmor().getHP() <= 0)
                .findFirst()
                .map(targetPeasant -> {
                    Armour crownArmor = ZombieFactory.createKnightArmor();
                    targetPeasant.setArmor(crownArmor);
                    return true;
                }).orElse(false);
    }
}
