package model.match.boss;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
import model.pitches.TileType;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class ZombossLawn {

    private ZombossLawn() {
    }

    public static void destroyPlant(GameSession session, Plant plant) {
        if (plant == null || !plant.isAlive()) return;
        plant.setHP(0);
        plant.setAlive(false);
    }

    public static List<Plant> livingPlants(GameSession session) {
        List<Plant> result = new ArrayList<>();
        if (session == null) return result;
        for (Plant plant : session.getPlants()) {
            if (plant != null && plant.isAlive() && plant.getPosition() != null) result.add(plant);
        }
        return result;
    }

    public static List<Plant> plantsInRow(GameSession session, int row) {
        List<Plant> result = new ArrayList<>();
        for (Plant plant : livingPlants(session)) {
            if ((int) Math.round(plant.getPosition().y()) == row) result.add(plant);
        }
        return result;
    }

    public static Plant pickBombardTarget(GameSession session, Random random) {
        List<Plant> candidates = livingPlants(session);
        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparingDouble(p -> p.getPosition().x()));
        int pool = Math.max(1, (int) Math.ceil(candidates.size() * 0.6));
        return candidates.get(random.nextInt(pool));
    }

    public static Plant plantWithinReach(GameSession session, Position bossPosition, double reach) {
        if (session == null || bossPosition == null) return null;
        Plant best = null;
        double bestDistance = Double.MAX_VALUE;
        int bossRow = (int) Math.round(bossPosition.y());
        for (Plant plant : livingPlants(session)) {
            if ((int) Math.round(plant.getPosition().y()) != bossRow) continue;
            double dx = bossPosition.x() - plant.getPosition().x();
            if (dx < -0.5 || dx > reach) continue;
            if (dx < bestDistance) {
                bestDistance = dx;
                best = plant;
            }
        }
        return best;
    }

    public static boolean isWater(GameSession session, int row, int col) {
        Cell cell = cellAt(session, row, col);
        return cell != null && cell.getTile() != null && cell.getTile().type() == TileType.Water;
    }

    public static Cell cellAt(GameSession session, int row, int col) {
        if (session == null || session.getEnvironment() == null) return null;
        Environment environment = session.getEnvironment();
        if (row < 0 || row >= environment.getRows() || col < 0 || col >= environment.getCols()) {
            return null;
        }
        return environment.getCell(row, col);
    }

    public static List<Position> waterTiles(GameSession session, int minColumn) {
        List<Position> tiles = new ArrayList<>();
        if (session == null || session.getEnvironment() == null) return tiles;
        Environment environment = session.getEnvironment();
        for (int row = 0; row < environment.getRows(); row++) {
            for (int col = Math.max(0, minColumn); col < environment.getCols(); col++) {
                if (isWater(session, row, col)) tiles.add(new Position(col, row));
            }
        }
        return tiles;
    }

    public static boolean isFireProof(Plant plant) {
        return plant != null && plant.getTags() != null && plant.getTags().contains(PlantTag.FIRE);
    }

    public static boolean isTangleKelp(Plant plant) {
        return plant != null && plant.getName() != null
                && plant.getName().replace("-", "").replace(" ", "").equalsIgnoreCase("tanglekelp");
    }
}
