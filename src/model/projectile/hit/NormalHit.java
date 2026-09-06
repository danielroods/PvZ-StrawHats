package model.projectile.hit;

import model.collections.zombie.Zombie;

public class NormalHit implements HitEffectStrategy {
    private final int areaLength;
    private final int splashDamageBonus;

    public NormalHit(int areaLength) {
        this(areaLength, 0);
    }

    public NormalHit(int areaLength, int splashDamageBonus) {
        this.areaLength = Math.max(1, areaLength);
        this.splashDamageBonus = Math.max(0, splashDamageBonus);
    }

    @Override
    public void apply(Zombie zombie) {
    }

    @Override
    public int getAreaLength() { return areaLength; }

    @Override
    public int getSplashDamageBonus() { return splashDamageBonus; }
}
