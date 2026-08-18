package model.projectile.hit;

import model.collections.zombie.Zombie;

public class FireHit implements HitEffectStrategy {
    private final int areaLength;
    private final double damageMultiplier;

    public FireHit(int areaLength) {
        this(areaLength, 2.0);
    }

    public FireHit(int areaLength, double damageMultiplier) {
        this.areaLength = Math.max(1, areaLength);
        this.damageMultiplier = damageMultiplier;
    }

    @Override
    public void apply(Zombie zombie) {
        if (zombie == null || !zombie.isAlive()) return;
        zombie.applyStatus(Zombie.Status.FIRED, 3.0);
    }

    @Override
    public int getAreaLength() { return areaLength; }

    @Override
    public double getDamageMultiplier() { return damageMultiplier; }

    @Override
    public boolean isFireDamage() { return true; }
}
