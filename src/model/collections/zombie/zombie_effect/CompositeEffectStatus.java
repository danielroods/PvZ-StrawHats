package model.collections.zombie.zombie_effect;

import model.collections.zombie.Zombie;
import model.utils.GameSession;

import java.util.List;

public class CompositeEffectStatus implements ZombieEffectStatus {

    private final List<ZombieEffectStatus> effects;

    public CompositeEffectStatus(List<ZombieEffectStatus> effects) {
        this.effects = List.copyOf(effects);
    }

    @Override
    public void applyTickEffect(Zombie target, GameSession session) {
        for (ZombieEffectStatus effect : effects) {
            effect.applyTickEffect(target, session);
        }
    }

    @Override
    public void onDeath(Zombie target, GameSession session) {
        for (ZombieEffectStatus effect : effects) {
            effect.onDeath(target, session);
        }
    }
}
