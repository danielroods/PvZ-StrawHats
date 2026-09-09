package model.match.main.levels.special_levels;

import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.match.main.levels.Level;
import model.utils.GameSession;
import service.GameClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ConveyorBeltLevel extends Level {
    private static final double CONVEYOR_INTERVAL_SECONDS = 12.0;
    private static final Random RAND = new Random();

    
    private List<Plant> conveyorPlants;
    private int maxConveyorSize = 8;

    
    private final List<Plant> activeConveyorPlants = new ArrayList<>();
    private double conveyorTimer = 0;

    @Override
    public void initSpecial(GameSession session) {
        conveyorTimer = 0;
        activeConveyorPlants.clear();
        offerNextPlant();
    }

    public void tickConveyor(double deltaSeconds) {
        if (activeConveyorPlants.size() >= maxConveyorSize) return;

        conveyorTimer += deltaSeconds;
        if (GameClock.hasReached(conveyorTimer, CONVEYOR_INTERVAL_SECONDS)) {
            conveyorTimer = 0;
            offerNextPlant();
        }
    }

    private void offerNextPlant() {
        if (conveyorPlants == null || conveyorPlants.isEmpty()) return;
        if (activeConveyorPlants.size() >= maxConveyorSize) return;

        Plant template = conveyorPlants.get(RAND.nextInt(conveyorPlants.size()));
        Plant offered = PlantFactory.createPlant(template.getId(), template.getLevel(), template.getPosition());
        if (offered != null) activeConveyorPlants.add(offered);
    }

    
    public Plant getCurrentPlant() {
        return activeConveyorPlants.isEmpty() ? null : activeConveyorPlants.get(0);
    }

    
    public Plant takeCurrentPlant() {
        if (activeConveyorPlants.isEmpty()) return null;
        Plant taken = activeConveyorPlants.remove(0);
        conveyorTimer = 0;
        return taken;
    }

    
    public boolean takeConveyorPlant(Plant plant) {
        if (plant == null) return false;
        boolean removed = activeConveyorPlants.remove(plant);
        if (removed) conveyorTimer = 0;
        return removed;
    }

    
    public List<Plant> getActiveConveyorPlants() {
        return Collections.unmodifiableList(activeConveyorPlants);
    }

    public List<Plant> getConveyorPlants() { return conveyorPlants; }
    public void setConveyorPlants(List<Plant> conveyorPlants) { this.conveyorPlants = conveyorPlants; }
    public int getMaxConveyorSize() { return maxConveyorSize; }
    public void setMaxConveyorSize(int maxConveyorSize) { this.maxConveyorSize = Math.max(1, maxConveyorSize); }
}
