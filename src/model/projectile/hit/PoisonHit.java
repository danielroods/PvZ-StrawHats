package model.projectile.hit;

import model.collections.zombie.Zombie;

public class PoisonHit implements HitEffectStrategy {
    public static final double BASE_POISON_SECONDS = 6.0;

    private final int areaLength;
    private final double poisonSeconds;

    public PoisonHit(int areaLength) {
        this(areaLength, BASE_POISON_SECONDS);
    }

    public PoisonHit(int areaLength, double poisonSeconds) {
        this.areaLength = Math.max(1, areaLength);
        this.poisonSeconds = Math.max(0.5, poisonSeconds);
    }

    @Override
    public void apply(Zombie zombie) {
        if (zombie == null || !zombie.isAlive()) return;
        zombie.applyStatus(Zombie.Status.POISONED, poisonSeconds);
    }

    @Override
    public int getAreaLength() { return areaLength; }

    @Override
    public boolean bypassesArmor() { return true; }
}
