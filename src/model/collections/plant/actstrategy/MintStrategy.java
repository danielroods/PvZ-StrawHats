package model.collections.plant.actstrategy;

import model.collections.plant.Plant;
import model.utils.GameSession;

public class MintStrategy implements ActStrategy {
    @Override
    public void act(Plant user, GameSession session) {
        if (user.getIntervalTimer() > 0) return;

        session.getPlants().stream()
                .filter(plant -> plant != null && plant.isAlive() && plant.getType() == user.getType())
                .forEach(plant -> {
                    if (plant != user && plant.getPlantFoodEffect() != null) plant.activatePlant(session);
                });

        user.setAlive(false);
    }
}
