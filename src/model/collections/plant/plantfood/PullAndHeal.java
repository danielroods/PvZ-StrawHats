package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class PullAndHeal implements PlantFoodEffect {
    private final double range;

    public PullAndHeal(double range) {
        this.range = range;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        Position center = plant.getPosition();
        if (center == null) return;

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.isHypnotized()
                    || zombie.getPosition() == null) continue;
            if (Math.abs(zombie.getPosition().y() - center.y()) < 0.5) continue;
            if (Math.abs(zombie.getPosition().x() - center.x()) <= range) {
                zombie.setPosition(new Position(zombie.getPosition().x(), center.y()));
            }
        }

        plant.setHP(plant.getMaxHp());
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}
