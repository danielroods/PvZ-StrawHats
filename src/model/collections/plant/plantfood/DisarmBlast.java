package model.collections.plant.plantfood;

import model.collections.armour.ZombieArmour;
import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class DisarmBlast implements PlantFoodEffect {
    private static final double THROW_DELAY_SECONDS = 1.0;
    private static final int DAMAGE_PER_ITEM = 40;

    private final int maxTargets;
    private double elapsed = 0;
    private int collectedItems = 0;
    private boolean thrown = false;

    public DisarmBlast(int maxTargets) {
        this.maxTargets = Math.max(1, maxTargets);
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        if (plant.getPosition() == null) return;
        List<Zombie> candidates = new ArrayList<>();
        for (Zombie zombie : session.getZombies()) {
            if (zombie != null && zombie.isAlive() && !zombie.isHypnotized()
                    && zombie.getPosition() != null && hasMetalArmour(zombie)) {
                candidates.add(zombie);
            }
        }
        candidates.sort(Comparator.comparingDouble(
                zombie -> zombie.getPosition().distanceTo(plant.getPosition())));

        collectedItems = Math.min(maxTargets, candidates.size());
        for (int i = 0; i < collectedItems; i++) {
            candidates.get(i).setArmour(null);
        }
        if (collectedItems > 0) {
            plant.setMagnetItemVisible(true);
        }
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        if (thrown || collectedItems <= 0) return;
        elapsed += deltaTimeSeconds;
        if (elapsed >= THROW_DELAY_SECONDS) {
            throwCollectedItems(plant, GameSession.getInstance());
        }
    }

    
    private void throwCollectedItems(Plant plant, GameSession session) {
        thrown = true;
        if (session == null || plant.getPosition() == null) return;

        List<Zombie> targets = new ArrayList<>();
        for (Zombie zombie : session.getZombies()) {
            if (zombie != null && zombie.isAlive() && !zombie.isHypnotized() && zombie.getPosition() != null) {
                targets.add(zombie);
            }
        }
        targets.sort(Comparator.comparingDouble(
                zombie -> zombie.getPosition().distanceTo(plant.getPosition())));

        for (int i = 0; i < Math.min(collectedItems, targets.size()); i++) {
            targets.get(i).takeDamage(DAMAGE_PER_ITEM, plant);
        }
        plant.setMagnetItemVisible(false);
    }

    private boolean hasMetalArmour(Zombie zombie) {
        return zombie.getArmour() instanceof ZombieArmour armour
                && armour.getHP() > 0 && armour.isMetal();
    }

    @Override
    public double getDurationSeconds() {
        
        
        return THROW_DELAY_SECONDS + 1.5;
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }

    @Override
    public void reset() {
        elapsed = 0;
        collectedItems = 0;
        thrown = false;
    }
}