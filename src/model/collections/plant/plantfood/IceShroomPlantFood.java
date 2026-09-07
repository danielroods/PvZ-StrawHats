package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.projectile.targeting.PlantTarget;
import model.projectile.targeting.TargetFinder;
import model.utils.GameSession;

public class IceShroomPlantFood implements PlantFoodEffect {
    private static final double PLANT_FOOD_DURATION_SECONDS = 2.5;
    private static final double FREEZE_SECONDS = 5.0;
    private static final double AREA_REACH = 1.0;

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

        for (PlantTarget target : TargetFinder.allWithin(plant, session,
                TargetFinder.box(center, AREA_REACH, AREA_REACH), false)) {
            Zombie zombie = target.getZombie();
            if (zombie == null) {
                target.takeDamage(damage, plant, session, false);
                continue;
            }
            zombie.takeDamage(damage, plant);
            if (zombie.isAlive()) {
                zombie.applyStatus(Zombie.Status.FREEZE, FREEZE_SECONDS);
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