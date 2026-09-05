package model.pitches;

import model.pitches.obstacles.Bridge;
import model.utils.GameSession;

/**
 * Shared check for whether a cell is water with nothing bridging it, used by
 * every ground-bound move behavior that needs to stop at the sea (Pirate
 * Seas' {@code PirateGroundWalk}/{@code PusherMove}, and any future one) so
 * the rule lives in exactly one place instead of being copy-pasted per class.
 */
public final class WaterCrossing {
    private WaterCrossing() {}

    /** True if (row, col) is a {@link TileType#Water} tile with no {@link Bridge} on it. */
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