package model.collections.plant;

import model.utils.GameSession;

public interface PlantFoodEffect {
    void triggerSuperpower(Plant plant, GameSession session);
    void tickDurationEffect(Plant plant, double deltaTimeSeconds);
    void applyStatusModifiers(Plant plant);
    // Effects that don't override this (SpawnSun, GrantArmor, InstantKill, etc.) get a real
    // 2.5s boosted window instead of an instant 0s one, so the plant-food state (and its
    // "plantfood" animation) is actually visible/felt instead of resolving in a single tick.
    default double getDurationSeconds() { return 2.5; }
    default void reset() {}
}