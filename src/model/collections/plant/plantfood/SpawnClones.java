package model.collections.plant.plantfood;

import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantFoodEffect;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;

public class SpawnClones implements PlantFoodEffect {
    private final int count;

    public SpawnClones(int count) {
        this.count = Math.max(1, count);
    }

    @Override
    public void triggerSuperpower(Plant plant, GameSession session) {
        Position position = plant.getPosition();
        if (position == null) return;

        if (plant.isPotatoMine()) {
            plant.armPotatoMine();
            spawnPotatoMineCopies(plant, session);
            return;
        }

        int row = (int) position.y();
        int col = (int) position.x();
        int spawned = 0;

        for (int offset = 1; offset <= session.getCols() && spawned < count; offset++) {
            spawned += trySpawnAt(session, plant, row, col + offset);
            if (spawned >= count) break;
            spawned += trySpawnAt(session, plant, row, col - offset);
        }
    }

    private void spawnPotatoMineCopies(Plant plant, GameSession session) {
        int target = Math.min(2, count);
        java.util.List<Position> candidates = new java.util.ArrayList<>();
        for (int row = 0; row < session.getRows(); row++) {
            for (int col = 0; col < session.getCols(); col++) {
                if (session.getEnvironment().getCell(row, col) == null) continue;
                if (session.getEnvironment().getCell(row, col).hasPlant()) continue;
                candidates.add(new Position(col, row));
            }
        }
        java.util.Collections.shuffle(candidates, java.util.concurrent.ThreadLocalRandom.current());
        int spawned = 0;
        for (Position candidate : candidates) {
            if (spawned >= target) break;
            Plant clone = PlantFactory.createPlant(plant.getId(), plant.getLevel(), candidate);
            if (session.plantAt((int) candidate.y(), (int) candidate.x(), clone)) {
                clone.armPotatoMine();
                spawned++;
            } else {
                clone.setAlive(false);
            }
        }
    }

    private int trySpawnAt(GameSession session, Plant plant, int row, int col) {
        if (row < 0 || row >= session.getRows() || col < 0 || col >= session.getCols()) return 0;
        if (session.getEnvironment().getCell(row, col) == null
                || session.getEnvironment().getCell(row, col).hasPlant()) return 0;

        Plant clone = PlantFactory.createPlant(plant.getId(), plant.getLevel(), new Position(col, row));
        return session.plantAt(row, col, clone) ? 1 : 0;
    }

    @Override
    public void tickDurationEffect(Plant plant, double deltaTimeSeconds) {
    }

    @Override
    public void applyStatusModifiers(Plant plant) {
    }
}
