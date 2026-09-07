package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.plant.PlantTag;
import model.match_mechanisms.vector.Position;
import model.projectile.targeting.PlantTarget;
import model.projectile.targeting.TargetFinder;
import model.utils.GameSession;

public class LocalAttack implements PlantFoodEffect {
    private final double radius;
    private final int damage;

    public LocalAttack(double radius, int damage) {
        this.radius = radius;
        this.damage = damage;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        Position center = plant.getPosition();
        if (center == null || session == null) return;
        boolean fire = plant.getTags().contains(PlantTag.FIRE);
        for (PlantTarget target : TargetFinder.allWithin(plant, session,
                TargetFinder.within(center, radius), false)) {
            target.takeDamage(damage, plant, session, fire);
        }
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}
