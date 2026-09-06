package model.collections.plant;

import model.collections.zombie.Zombie;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.List;

public final class UpgradeEffects {

    public static final String DEATH_EXPLOSION_TAG = "DEATH_EXPLOSION_AOE";
    public static final String EXPLODE_ON_FINISH_TAG = "EXPLODE_ON_FINISH";
    public static final String MELT_AREA_TAG = "MELT_AREA_3X3";
    public static final String WARM_RADIUS_TAG = "WARM_RADIUS_EXT";
    public static final String AUTO_PLANT_FOOD_CHANCE_TAG = "AUTO_PLANT_FOOD_CHANCE";
    public static final String GROWTH_STAGE_MAX_UP_TAG = "GROWTH_STAGE_MAX_UP";

    public static final int DEFAULT_FAREWELL_DAMAGE = 100;
    private static final int FAREWELL_RADIUS = 1;

    private UpgradeEffects() {
    }

    public static boolean hasFarewellBlast(Plant plant) {
        return plant != null && (plant.hasSpecialUpgrade(DEATH_EXPLOSION_TAG)
                || plant.hasSpecialUpgrade(EXPLODE_ON_FINISH_TAG));
    }

    public static int farewellBlastDamage(Plant plant) {
        if (plant == null) return 0;
        double tagged = Math.max(plant.getSpecialUpgrade(DEATH_EXPLOSION_TAG, 0.0),
                plant.getSpecialUpgrade(EXPLODE_ON_FINISH_TAG, 0.0));
        int fromTag = tagged > 1.0 ? (int) Math.round(tagged) : DEFAULT_FAREWELL_DAMAGE;
        return Math.max(fromTag, 0);
    }

    public static void runFarewellBlasts(GameSession session) {
        if (session == null) return;
        List<Plant> dying = new ArrayList<>();
        for (Plant plant : session.getPlants()) {
            if (plant == null || plant.isAlive() || plant.hasFiredFarewellBlast()) continue;
            if (hasFarewellBlast(plant)) dying.add(plant);
        }
        for (Plant plant : dying) {
            plant.markFarewellBlastFired();
            detonate(session, plant);
        }
    }

    private static void detonate(GameSession session, Plant plant) {
        Position center = plant.getPosition();
        if (center == null) return;
        int damage = farewellBlastDamage(plant);
        if (damage <= 0) return;

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            if (Math.abs(zombie.getPosition().x() - center.x()) <= FAREWELL_RADIUS
                    && Math.abs(zombie.getPosition().y() - center.y()) <= FAREWELL_RADIUS) {
                zombie.takeDamage(damage, plant);
            }
        }

        for (PushableStructure structure : session.getPushableStructures()) {
            if (structure == null || !structure.isAlive() || structure.getPosition() == null) continue;
            if (Math.abs(structure.getPosition().x() - center.x()) <= FAREWELL_RADIUS
                    && Math.abs(structure.getPosition().y() - center.y()) <= FAREWELL_RADIUS) {
                structure.takeDamage(damage, plant, session);
            }
        }
    }

    public static int iceMeltMode(Plant plant, int baseMode) {
        if (plant != null && plant.hasSpecialUpgrade(MELT_AREA_TAG)) return Math.max(baseMode, 2);
        return baseMode;
    }

    public static int warmthRadius(Plant plant) {
        if (plant == null) return 0;
        return 1 + (int) Math.max(0, Math.round(plant.getSpecialUpgrade(WARM_RADIUS_TAG, 0.0)));
    }

    public static boolean rollsAutoPlantFood(Plant plant) {
        if (plant == null) return false;
        double chance = plant.getSpecialUpgrade(AUTO_PLANT_FOOD_CHANCE_TAG, 0.0);
        return chance > 0 && Math.random() < chance;
    }
}
