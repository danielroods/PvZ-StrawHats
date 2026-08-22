package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class LocalAttack implements PlantFoodEffect {
    private double elapsed;
    private final double radius;
    private final int damage;

    public LocalAttack(double radius, int damage) {
        this.radius = radius;
        this.damage = damage;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        elapsed = 0.0;
        if ("Wasabi Whip".equalsIgnoreCase(plant.getName())) {
            plant.setVisualAnimationState("plantfood_on", 0.35);
        }
        Position center = plant.getPosition();
        if (center == null) return;
        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            if (zombie.getPosition().distanceTo(center) <= radius) {
                zombie.takeDamage(damage, plant);
            }
        }
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        elapsed += deltaTimeSeconds;
        if (!"Wasabi Whip".equalsIgnoreCase(plant.getName())) return;
        double total = 2.0;
        if (elapsed < 0.35) {
            if (!"plantfood_on".equals(plant.getVisualAnimationState()))
                plant.setVisualAnimationState("plantfood_on", 0.35 - elapsed);
        } else if (elapsed < total - 0.35) {
            if (!"plantfood".equals(plant.getVisualAnimationState()))
                plant.setVisualAnimationState("plantfood", total - 0.35 - elapsed);
        } else if (!"plantfood_off".equals(plant.getVisualAnimationState())) {
            plant.setVisualAnimationState("plantfood_off", Math.max(0.05, total - elapsed));
        }
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}