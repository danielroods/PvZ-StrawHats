package model.projectile.hit;

import model.collections.zombie.Zombie;

public class ButterHit implements HitEffectStrategy {
    private final int areaLength;

    public ButterHit(int areaLength) {
        this.areaLength = Math.max(1, areaLength);
    }

    @Override
    public void apply(Zombie zombie) {
        if (zombie == null || !zombie.isAlive()) return;
        zombie.applyStatus(Zombie.Status.BUTTER, 5.0);
    }

    @Override
    public int getAreaLength() { return areaLength; }
}