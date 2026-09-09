package model.collections.plant.plantfood;

import model.collections.armour.ZombieArmour;
import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Magnet-shroom's Plant Food: instantly rips the metal armor (bucket/crown) off up to
 * {@code maxTargets} nearby zombies - showing the "plantfood" clip and the caught
 * Magnet_Item element for the whole boosted window (see PlantRenderer) - then, after a
 * short delay so the throw reads as its own beat, flings each collected item at the
 * nearest zombies for damage and hides the item again.
 */
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

    /** Throws every item that was collected in {@link #triggerSuperpower} at the nearest
     * live zombies, dealing damage, then hides the Magnet_Item element again. */
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
        // Long enough for the "plantfood" clip to hold through the throw delay above,
        // plus a little tail so the throw itself is still visible before it ends.
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