package model.projectile.hit;

import model.collections.zombie.Zombie;

public class IceHit implements HitEffectStrategy {
    private static final double DEFAULT_CHILL_SECONDS = 5.0;

    private final int areaLength;
    private final double chillSeconds;

    public IceHit(int areaLength) {
        this(areaLength, DEFAULT_CHILL_SECONDS);
    }

    public IceHit(int areaLength, double chillSeconds) {
        this.areaLength = Math.max(1, areaLength);
        this.chillSeconds = chillSeconds > 0 ? chillSeconds : DEFAULT_CHILL_SECONDS;
    }

    @Override
    public void apply(Zombie zombie) {
        if (zombie == null || !zombie.isAlive()) return;
        zombie.applyStatus(Zombie.Status.FREEZE, chillSeconds);
    }

    @Override
    public int getAreaLength() { return areaLength; }
}
