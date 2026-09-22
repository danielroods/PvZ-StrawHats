package model.match.main.season.travellog.pirate;

import model.match.main.season.Season;
import model.pitches.Cell;
import model.pitches.Environment;
import model.pitches.Tile;
import model.pitches.TileType;
import model.pitches.obstacles.Bridge;
import model.utils.GameSession;

import java.util.HashSet;
import java.util.Set;

public class Pirate extends Season {

    
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

    public boolean rowHasBridge(int row) {
        return bridgeRows != null && bridgeRows.contains(row);
    }
}