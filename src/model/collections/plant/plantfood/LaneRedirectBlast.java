package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class LaneRedirectBlast implements PlantFoodEffect {

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        Position center = plant.getPosition();
        if (center == null) return;
        int rows = session.getRows();
        int plantRow = (int) Math.round(center.y());

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.isHypnotized()
                    || zombie.getPosition() == null) continue;
            if (Math.round(zombie.getPosition().y()) != plantRow) continue;

            int targetRow;
            if (plantRow <= 0) targetRow = 1;
            else if (plantRow >= rows - 1) targetRow = rows - 2;
            else targetRow = Math.random() < 0.5 ? plantRow - 1 : plantRow + 1;

            zombie.setPosition(new Position(zombie.getPosition().x(), targetRow));
        }
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}
