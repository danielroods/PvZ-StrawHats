package model.projectile.hit;

import model.collections.zombie.Zombie;

public interface HitEffectStrategy {
    void apply(Zombie zombie);

    default int getAreaLength() { return 1; }
    default int getPierceCount() { return 1; }
    default double getKnockbackDistance() { return 0; }
    default double getDamageMultiplier() { return 1.0; }
    default boolean bypassesArmor() { return false; }
    default boolean isFireDamage() { return false; }
}
