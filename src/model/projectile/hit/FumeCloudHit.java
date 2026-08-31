package model.projectile.hit;

import model.collections.armour.Armour;
import model.collections.zombie.Zombie;

public class FumeCloudHit implements HitEffectStrategy {
    private final int areaLength;

    public FumeCloudHit(int areaLength) {
        this.areaLength = Math.max(1, areaLength);
    }

    @Override
    public void beforeDamage(Zombie zombie) {
        if (zombie == null || !zombie.isAlive()) return;
        Armour armour = zombie.getArmour();
        if (armour == null || armour.getHP() <= 0) return;
        zombie.setArmour(null);
    }

    @Override
    public void apply(Zombie zombie) {
    }

    @Override
    public int getAreaLength() { return areaLength; }

    @Override
    public int getPierceCount() { return -1; }
}
