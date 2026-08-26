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

        if (plant.isPumpkin() || plant.isWallNut() || plant.isTallNut() || plant.isEndurian()) {
            String initialState = resolveInitialState(plant);
            float clipDuration = AnimationFactory.clipDurationForDisplayName(
                    plant.getName(), initialState);
            if (clipDuration > 0f) runtimeDuration = Math.max(2.5, clipDuration);
            double visualDuration = plant.isEndurian() && clipDuration > 0f
                    ? clipDuration : runtimeDuration;
            plant.setVisualAnimationState(initialState, visualDuration);

            Plant bottom = plant.getBottom();
            if (bottom != null && bottom.isAlive() && bottom.getPlantFoodEffect() != null
                    && bottom.canUsePlantFood()) {
                bottom.activatePlantFoodFromPumpkin(session);
            }
        }
    }

    private static String resolveInitialState(Plant plant) {
        if (plant.isPumpkin()) return "idle_plantfood";
        if (plant.isWallNut()) return "plantfood";
        if (plant.isEndurian()) return "plantfood_on";
        return "idle";
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
        if (plant == null) return;
        if (plant.isPumpkin() || plant.isWallNut() || plant.isTallNut() || plant.isEndurian()) {
            plant.setHP(plant.getMaxHp());
        }
        plant.setArmor((PlantArmour) ArmourFactory.createArmour(ArmourType.PLANT_SHIELD, hp, 0, false));
    }

    @Override
    public double getDurationSeconds() {
        return runtimeDuration;
    }
}
