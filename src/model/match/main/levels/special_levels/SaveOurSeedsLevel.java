package model.match.main.levels.special_levels;

import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantProgression;
import model.match.main.levels.Level;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.utils.GameSession;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SaveOurSeedsLevel extends Level {
    private Map<Position, String> seedPositions; 
    private final Set<Position> confirmedPlanted = new HashSet<>();

    @Override
    public void initSpecial(GameSession session) {
        confirmedPlanted.clear();
        if (session == null || seedPositions == null) return;

        for (Map.Entry<Position, String> entry : seedPositions.entrySet()) {
            Position position = entry.getKey();
            Integer plantId = PlantFactory.getBlueprints().values().stream()
                    .filter(config -> config.name.equalsIgnoreCase(entry.getValue()))
                    .map(config -> config.id)
                    .findFirst()
                    .orElse(null);
            if (plantId == null) {
                throw new IllegalStateException("Unknown guarded plant: " + entry.getValue());
            }

            Plant plant = PlantFactory.createPlant(plantId,
                    PlantProgression.currentLevelOf(plantId), position);
            if (!session.plantAt((int) position.y(), (int) position.x(), plant)) {
                throw new IllegalStateException("Could not place guarded plant at " + position);
            }
            confirmedPlanted.add(position);
        }
    }


    @Override
    public boolean checkLossCondition(GameSession session) {
        if (seedPositions == null) return false;

        for (Position pos : seedPositions.keySet()) {
            Cell cell = session.getEnvironment().getCell((int) pos.y(), (int) pos.x());
            boolean hasPlant = cell != null && cell.hasPlant();

            if (hasPlant)
                confirmedPlanted.add(pos);
            else if (confirmedPlanted.contains(pos))
                return true;

        }
        return false;
    }

    public Map<Position, String> getSeedPositions() { return seedPositions; }
    public void setSeedPositions(Map<Position, String> seedPositions) { this.seedPositions = seedPositions; }
}
