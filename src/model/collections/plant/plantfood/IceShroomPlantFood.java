package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

/**
 * Ice-shroom's Plant Food: an instant hit on the same 3x3 (9-tile) footprint as its normal
 * periodic melee attack (MeleeStrategy's areaDetect for abilityValue 2), centered on its own
 * tile. Every zombie caught in it takes damage and gets iced, same as a normal attack via
 * PlantTag.ICE (see MeleeStrategy.userAct). The falling-icicle projectile visuals are handled
 * separately by EffectRenderer.triggerIceShroomPlantFood, keyed off isPlantFoodActive()
 * flipping true, and the "plantfood" clip plays for the plant itself via setVisualAnimationState.
 */
public class IceShroomPlantFood implements PlantFoodEffect {
    private static final double PLANT_FOOD_DURATION_SECONDS = 2.5;
    private static final double FREEZE_SECONDS = 5.0;

    private final int damage;

    public IceShroomPlantFood(int damage) {
        this.damage = Math.max(0, damage);
    }

    @Override
    public double getDurationSeconds() {
        return PLANT_FOOD_DURATION_SECONDS;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        plant.setVisualAnimationState("plantfood", PLANT_FOOD_DURATION_SECONDS);

        Position center = plant.getPosition();
        if (center == null || session == null) return;

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position p = zombie.getPosition();
            if (Math.abs(p.x() - center.x()) <= 1 && Math.abs(p.y() - center.y()) <= 1) {
                zombie.takeDamage(damage, plant);
                if (zombie.isAlive()) {
                    zombie.applyStatus(Zombie.Status.FREEZE, FREEZE_SECONDS);
                }
            }
        }
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}