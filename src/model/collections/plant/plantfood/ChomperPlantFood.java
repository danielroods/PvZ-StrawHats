package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

import java.util.IdentityHashMap;
import java.util.Map;


public class ChomperPlantFood implements PlantFoodEffect {
    private static final double TOTAL_DURATION = 3.0;
    private static final double PULL_DURATION = 1.0;
    private static final double PULL_SPEED = 4.0;
    private static final double HIT_RADIUS = 0.55;
    private static final int MAX_EAT = 3;
    private static final int LETHAL_DAMAGE = 999999;

    private final Map<Zombie, Position> originalPositions = new IdentityHashMap<>();
    private double elapsed;
    private boolean burpStarted;
    private boolean burpEndStarted;
    private int eaten;

    @Override
    public double getDurationSeconds() {
        return TOTAL_DURATION;
    }

    @Override
    public boolean drivesActStrategy() {
        return true;
    }

    @Override
    public void reset() {
        originalPositions.clear();
        elapsed = 0;
        burpStarted = false;
        burpEndStarted = false;
        eaten = 0;
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        reset();
        plant.setVisualAnimationState("plantfood_on", 0.35);

        Position center = plant.getPosition();
        if (center == null || session == null) return;

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            if (Math.abs(zombie.getPosition().y() - center.y()) < 0.5) {
                originalPositions.put(zombie,
                        new Position(zombie.getPosition().x(), zombie.getPosition().y()));
            }
        }
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        elapsed += deltaTimeSeconds;
        GameSession session = GameSession.peekInstance();
        Position center = plant.getPosition();
        if (session == null || center == null) return;

        if (elapsed <= PULL_DURATION) {
            if (!"plantfood".equals(plant.getVisualAnimationState())) {
                plant.setVisualAnimationState("plantfood", PULL_DURATION);
            }

            for (Zombie zombie : originalPositions.keySet()) {
                if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;

                Position pos = zombie.getPosition();
                double dx = center.x() - pos.x();
                double dy = center.y() - pos.y();
                double distance = Math.sqrt(dx * dx + dy * dy);

                
                zombie.applyStatus(Zombie.Status.BUTTER, Math.max(0.1, deltaTimeSeconds * 2.0));

                if (distance > HIT_RADIUS && distance > 0.0001) {
                    double step = Math.min(PULL_SPEED * deltaTimeSeconds, distance);
                    zombie.setPosition(new Position(
                            pos.x() + dx / distance * step,
                            pos.y() + dy / distance * step
                    ));
                }

                if (distance <= HIT_RADIUS && eaten < MAX_EAT && !isGargantuar(zombie)) {
                    zombie.takeDamage(LETHAL_DAMAGE, plant);
                    eaten++;
                }
            }
            return;
        }

        if (!burpStarted) {
            burpStarted = true;
            plant.setVisualAnimationState("plantfood_burp", 0.65);

            
            for (Map.Entry<Zombie, Position> entry : originalPositions.entrySet()) {
                Zombie zombie = entry.getKey();
                Position original = entry.getValue();
                if (zombie == null || !zombie.isAlive() || original == null) continue;

                double distance = original.x() - zombie.getPosition().x();
                if (Math.abs(distance) > 0.0001) {
                    zombie.startKnockback(distance, 0.35);
                }
                zombie.applyStatus(Zombie.Status.BUTTER, 0.35);
            }
            return;
        }

        
        
        if (!burpEndStarted && elapsed >= TOTAL_DURATION - 0.35) {
            burpEndStarted = true;
            plant.setVisualAnimationState("plantfood_burp_end", 0.35);
        } else if (!burpEndStarted && !"plantfood_burp".equals(plant.getVisualAnimationState())) {
            plant.setVisualAnimationState("plantfood_burp", 0.65);
        }
    }

    private boolean isGargantuar(Zombie zombie) {
        String name = zombie.getName();
        return name != null && name.toLowerCase().contains("gargantuar");
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}