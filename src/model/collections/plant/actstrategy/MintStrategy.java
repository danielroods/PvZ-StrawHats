package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.List;

public class MintStrategy implements ActStrategy {
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

        for (Plant plant : family) {
            if (!plant.isAlive()) continue;
            plant.activatePlant(session);
        }

        user.setAlive(false);
    }
}
