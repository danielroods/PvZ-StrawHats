package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DisarmBlast implements PlantFoodEffect {
    private final int maxTargets;

    public DisarmBlast(int maxTargets) {
        this.maxTargets = Math.max(1, maxTargets);
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        if (plant.getPosition() == null) return;
        List<Zombie> candidates = new ArrayList<>();
        for (Zombie zombie : session.getZombies()) {
            if (zombie != null && zombie.isAlive() && !zombie.isHypnotized()
                    && zombie.getPosition() != null
                    && zombie.getArmour() != null && zombie.getArmour().getHP() > 0) {
                candidates.add(zombie);
            }
        }
        candidates.sort(Comparator.comparingDouble(
                zombie -> zombie.getPosition().distanceTo(plant.getPosition())));
        for (int i = 0; i < Math.min(maxTargets, candidates.size()); i++) {
            candidates.get(i).setArmour(null);
        }
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}
