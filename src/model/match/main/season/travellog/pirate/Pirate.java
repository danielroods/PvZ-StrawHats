package model.match.main.season.travellog.pirate;

import model.match.main.season.Season;
import model.pitches.Cell;
import model.pitches.Environment;
import model.pitches.Tile;
import model.pitches.TileType;
import model.pitches.obstacles.Bridge;
import model.pitches.obstacles.Grave;
import model.utils.GameSession;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Pirate extends Season {
    private static final int GRAVE_COUNT = 3;
    private static final Random RANDOM = new Random();

    
    public static final int WATER_COLUMN_COUNT = 4;

    
    private Set<Integer> bridgeRows;

    public Pirate() {
        super("Pirates");
    }

    @Override
    public void placeSeasonObstacles(GameSession session) {
        if (session == null || session.getEnvironment() == null) return;
        Environment env = session.getEnvironment();

        setupSeaAndBridges(env);
        placeGraves(env);
    }

    
    private void setupSeaAndBridges(Environment env) {
        int waterStart = Math.max(0, env.getCols() - WATER_COLUMN_COUNT);
        bridgeRows = new HashSet<>();

        for (int row = 0; row < env.getRows(); row++) {
            boolean bridged = (row % 2 == 0);
            if (bridged) bridgeRows.add(row);

            for (int col = waterStart; col < env.getCols(); col++) {
                Cell cell = env.getCell(row, col);
                if (cell == null) continue;
                cell.setTile(new Tile(TileType.Water));
                if (bridged) {
                    cell.setObstacle(new Bridge());
                }
            }
        }
    }

    
    private void placeGraves(Environment env) {
        int waterStart = Math.max(0, env.getCols() - WATER_COLUMN_COUNT);

        int placed = 0;
        int attempts = 0;
        while (placed < GRAVE_COUNT && attempts < 100) {
            attempts++;
            int row = RANDOM.nextInt(env.getRows());
            int col = RANDOM.nextInt(waterStart == 0 ? env.getCols() : waterStart);
            Cell cell = env.getCell(row, col);
            if (cell != null && cell.getObstacle() == null && !cell.hasPlant()) {
                cell.setObstacle(new Grave());
                placed++;
            }
        }
    }

    
    public boolean rowHasBridge(int row) {
        return bridgeRows != null && bridgeRows.contains(row);
    }
}