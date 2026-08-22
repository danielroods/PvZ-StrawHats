package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

/**
 * Plant Food implementation for the two custom melee AOE behaviors:
 * Bonk Choy: all eight neighboring tiles.
 * Phat Beet: two-tile radius on the same row and five-second stun.
 */
public class MeleeAreaPlantFood implements PlantFoodEffect {
    private final boolean phatBeet;
    private final boolean kiwibeast;
    private final int damage;
    private final double duration;
    private double elapsed = 0.0;

    public MeleeAreaPlantFood(boolean phatBeet, int damage) {
        this(phatBeet, damage, false);
    }

    public MeleeAreaPlantFood(boolean phatBeet, int damage, boolean kiwibeast) {
        this.phatBeet = phatBeet;
        this.kiwibeast = kiwibeast;
        this.damage = damage;
        this.duration = phatBeet ? 5.0 : (kiwibeast ? 1.2 : 2.0);
    }

    @Override
    public double getDurationSeconds() {
        return duration;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        elapsed = 0.0;
        if (kiwibeast) {
            plant.setVisualAnimationState("plantfood_stage3", duration);
        } else if (phatBeet) {
            plant.setVisualAnimationState("plantfood", duration);
        } else {
            plant.setVisualAnimationState("plantfood_on", 0.35);
        }
        Position center = plant.getPosition();
        if (center == null || session == null) return;

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            Position p = zombie.getPosition();

            if (phatBeet) {
                if (Math.abs(p.y() - center.y()) < 0.5
                        && Math.abs(p.x() - center.x()) <= 2.0
                        && Math.abs(p.x() - center.x()) > 0.01) {
                    zombie.takeDamage(damage, plant);
                    zombie.applyStatus(Zombie.Status.BUTTER, 5.0);
                }
            } else if (kiwibeast) {
                if (Math.abs(p.y() - center.y()) < 0.5
                        && Math.abs(p.x() - center.x()) <= 1.0
                        && Math.abs(p.x() - center.x()) > 0.01) {
                    zombie.takeDamage(damage, plant);
                    if (zombie.isAlive()) {
                        int hitNumber = plant.incrementKiwibeastHitCounter();
                        if (hitNumber % 2 == 0) {
                            double dir = Math.signum(p.x() - center.x());
                            if (dir == 0) dir = 1;
                            zombie.setPosition(new Position(p.x() + dir, p.y()));
                        }
                    }
                }
            } else {
                // Exactly the 8 neighboring board cells, excluding the plant's own cell.
                int dx = (int) Math.round(p.x() - center.x());
                int dy = (int) Math.round(p.y() - center.y());
                if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && (dx != 0 || dy != 0)) {
                    zombie.takeDamage(damage, plant);
                }
            }
        }
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        elapsed += deltaTimeSeconds;
        if (!kiwibeast && !phatBeet) {
            if (elapsed < 0.35) {
                if (!"plantfood_on".equals(plant.getVisualAnimationState()))
                    plant.setVisualAnimationState("plantfood_on", 0.35 - elapsed);
            } else if (elapsed < duration - 0.35) {
                if (!"plantfood".equals(plant.getVisualAnimationState()))
                    plant.setVisualAnimationState("plantfood", duration - 0.35 - elapsed);
            } else if (!"plantfood_off".equals(plant.getVisualAnimationState())) {
                plant.setVisualAnimationState("plantfood_off", Math.max(0.05, duration - elapsed));
            }
        }
        // Phat Beet's five-second stun is applied directly above.
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}