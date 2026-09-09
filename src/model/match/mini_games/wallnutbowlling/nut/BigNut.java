package model.match.mini_games.wallnutbowlling.nut;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class BigNut extends Nut {
    private static final double ROLL_SPEED = 1.05;

    public BigNut(Position position, Position direction) {
        super(position, direction);
    }

    @Override
    public double getRollSpeed() {
        return ROLL_SPEED;
    }

    @Override
    public boolean onHitZombie(Zombie zombie, GameSession session) {
        int armorHp = zombie.getArmor() == null ? 0 : zombie.getArmor().getHP();
        zombie.takeDamage(zombie.getHp() + armorHp + 1, this);
        return false; // keeps rolling straight through
    }

    @Override
    public String getKindName() {
        return "Big Wall-nut";
    }
}
