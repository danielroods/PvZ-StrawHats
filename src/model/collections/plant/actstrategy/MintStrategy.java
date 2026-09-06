package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MintStrategy implements ActStrategy {

    public static final String DURATION_TAG = "DURATION_EXT";
    public static final String RESET_COOLDOWNS_TAG = "RESET_FAMILY_COOLDOWNS";

    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        List<Plant> family = new ArrayList<>();
        for (Plant plant : new ArrayList<>(session.getPlants())) {
            if (plant == null || plant == user || !plant.isAlive()) continue;
            if (plant.getType() != user.getType()) continue;
            if (plant.getPlantFoodEffect() == null || !plant.canUsePlantFood()) continue;
            family.add(plant);
        }

        double bonusDuration = Math.max(0.0, user.getSpecialUpgrade(DURATION_TAG, 0.0));

        for (Plant plant : family) {
            if (!plant.isAlive()) continue;
            if (!plant.activatePlant(session)) continue;
            if (bonusDuration > 0 && plant.getPlantFoodTimer() > 0
                    && !Double.isInfinite(plant.getPlantFoodTimer())) {
                plant.setPlantFoodTimer(plant.getPlantFoodTimer() + bonusDuration);
            }
        }

        if (user.hasSpecialUpgrade(RESET_COOLDOWNS_TAG)) {
            resetFamilyCooldowns(user, session);
        }

        user.setAlive(false);
    }

    private void resetFamilyCooldowns(Plant user, GameSession session) {
        Set<Integer> familyIds = new HashSet<>();
        for (Plant plant : new ArrayList<>(session.getPlants())) {
            if (plant == null || plant == user) continue;
            if (plant.getType() == user.getType()) familyIds.add(plant.getId());
        }
        for (model.collections.plant.PlantJsonParser.PlantConfig config
                : model.collections.plant.PlantFactory.getBlueprints().values()) {
            if (config != null && config.category == user.getType()) familyIds.add(config.id);
        }
        for (int plantId : familyIds) session.clearPlantCooldown(plantId);
    }
}
