package model.collections.plant.plantfood;

import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.collections.plant.PlantFoodEffect;
import model.collections.item.GroundSun;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

import java.util.concurrent.ThreadLocalRandom;

public class SpawnSun implements PlantFoodEffect {
    private static final int SUN_VALUE_PER_DROP = 25;
    private static final double DROP_SPREAD_X = 0.22;
    private static final double DROP_SPREAD_Y = 0.08;

    private final int amount;
    private boolean pendingDrop;
    private double elapsed;
    private double durationSeconds = 0.6;

    public SpawnSun(int amount) {
        this.amount = Math.max(0, amount);
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        
        
        pendingDrop = true;
        elapsed = 0.0;

        durationSeconds = getClipDuration(plant);
        plant.setVisualAnimationState(plantFoodClip(plant), durationSeconds);
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
        if (!pendingDrop) return;

        elapsed += Math.max(0.0, deltaTimeSeconds);
        if (elapsed < durationSeconds) return;

        pendingDrop = false;
        dropSuns(plant);
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }

    @Override
    public double getDurationSeconds() {
        return durationSeconds;
    }

    private double getClipDuration(Plant plant) {
        if (plant == null) return 0.6;
        String clip = plantFoodClip(plant);
        String path = AnimationFactory.pathForDisplayName(plant.getName());
        float duration = AnimationFactory.exactClipDurationForPath(path, clip);
        return duration > 0f ? duration : 0.6;
    }

    private String plantFoodClip(Plant plant) {
        if (plant != null && "Sun-shroom".equalsIgnoreCase(plant.getName())) {
            return switch (plant.getGrowthStage()) {
                case 2 -> "plantfood_stage2";
                case 3 -> "plantfood_stage3";
                default -> "plantfood_stage1";
            };
        }
        return "plantfood";
    }

    private void dropSuns(Plant plant) {
        if (plant == null || !plant.isAlive()) return;
        GameSession session = GameSession.peekInstance();
        if (session == null || amount <= 0) return;

        Position origin = plant.getPosition();
        if (origin == null) return;

        int count = Math.max(1, (amount + SUN_VALUE_PER_DROP - 1) / SUN_VALUE_PER_DROP);
        int remaining = amount;

        for (int i = 0; i < count; i++) {
            int dropsLeft = count - i;
            int value = Math.max(1, remaining / dropsLeft);
            remaining -= value;

            double x = origin.x() + (ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * DROP_SPREAD_X;
            double y = origin.y() + (ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * DROP_SPREAD_Y;
            session.getItems().add(new GroundSun(new Position(x, y), value, true));
        }
    }

    @Override
    public void reset() {
        pendingDrop = false;
        elapsed = 0.0;
        durationSeconds = 0.6;
    }
}
