package model.collections.plant.plantfood;

import model.collections.armour.ArmourFactory;
import model.collections.armour.ArmourType;
import model.collections.armour.PlantArmour;
import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.animations.AnimationFactory;
import model.utils.GameSession;

public class GrantArmor implements PlantFoodEffect {
    private final int hp;
    private double runtimeDuration = 2.5;

    public GrantArmor(int hp) {
        this.hp = Math.max(1, hp);
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        if (plant == null) return;

        if (plant.isPumpkin() || plant.isWallNut() || plant.isTallNut()) {
            String initialState = plant.isPumpkin() ? "idle_plantfood"
                    : (plant.isWallNut() ? "plantfood" : "idle");
            float clipDuration = AnimationFactory.clipDurationForDisplayName(
                    plant.getName(), initialState);
            if (clipDuration > 0f) runtimeDuration = Math.max(2.5, clipDuration);
            plant.setVisualAnimationState(initialState, runtimeDuration);

            Plant bottom = plant.getBottom();
            if (bottom != null && bottom.isAlive() && bottom.getPlantFoodEffect() != null
                    && bottom.canUsePlantFood()) {
                bottom.activatePlantFoodFromPumpkin(session);
            }
        }
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
        if (plant == null) return;
        if (plant.isPumpkin() || plant.isWallNut() || plant.isTallNut()) {
            plant.setHP(plant.getMaxHp());
        }
        plant.setArmor((PlantArmour) ArmourFactory.createArmour(ArmourType.PLANT_SHIELD, hp, 0, false));
    }

    @Override
    public double getDurationSeconds() {
        return runtimeDuration;
    }
}
