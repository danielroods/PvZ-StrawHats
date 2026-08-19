package model.projectile.hit;

import model.collections.zombie.Zombie;

public class HypnotizeHit implements HitEffectStrategy {
    private final int pierceNumber;

    public HypnotizeHit(int pierceNumber) {
        this.pierceNumber = pierceNumber == 0 ? 1 : pierceNumber;
    }

    @Override
    public void apply(Zombie zombie) {
        if (zombie == null || !zombie.isAlive() || zombie.isHypnotized()) return;
        zombie.hypnotize();
    }

    @Override
    public int getPierceCount() { return pierceNumber; }
}
