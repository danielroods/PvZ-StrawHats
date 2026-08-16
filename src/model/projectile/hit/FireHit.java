package model.projectile.hit;

import model.collections.zombie.Zombie;

public class FireHit implements HitEffectStrategy {
    private final int areaLength;

    public FireHit(int areaLength) {
        this.areaLength = Math.max(1, areaLength);
    }

    @Override
    public void apply(Zombie zombie) {
        if (zombie == null || !zombie.isAlive()) return;
        zombie.applyStatus(Zombie.Status.FIRED, 3.0);
    }

    @Override
    public int getAreaLength() { return areaLength; }

    @Override
    public double getDamageMultiplier() { return 2.0; }

    @Override
    public boolean isFireDamage() { return true; }
}
