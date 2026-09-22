package model.pitches;

import model.pitches.obstacles.Bridge;
import model.utils.GameSession;


public final class WaterCrossing {
    private WaterCrossing() {}

    
    public static boolean isOpenWater(GameSession session, int row, int col) {
        if (session == null) return false;
        Environment lawn = session.getLawn();
        if (lawn == null) return false;

        Cell cell = lawn.getCell(row, col);
        if (cell == null) return false;

        Tile tile = cell.getTile();
        if (tile == null || tile.type() != TileType.Water) return false;

        return !(cell.getObstacle() instanceof Bridge);
    }
}