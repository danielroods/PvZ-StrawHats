package model.collections.zombie.zombie_move;

import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
import model.pitches.Tile;
import model.pitches.TileType;
import model.pitches.obstacles.SlipperyDirection;
import model.utils.GameSession;

public interface MoveBehavior {
    void move(Zombie zombie, double deltaTime, GameSession session);

    default Position applySlipperyShift(Position pos, GameSession session) {
        Environment lawn = session == null ? null : session.getLawn();
        if (lawn == null || pos == null) return pos;

        int row = (int) Math.round(pos.y());
        int col = (int) Math.floor(pos.x());
        Cell cell = lawn.getCell(row, col);
        if (cell == null || cell.getTile() == null) return pos;

        Tile tile = cell.getTile();
        if (tile.type() != TileType.Slippery || tile.slipperyDirection() == null) return pos;
        double rowDelta = tile.slipperyDirection() == SlipperyDirection.UP ? -1.0 : 1.0;
        double newRow = row + rowDelta;
        if (newRow < 0 || newRow >= lawn.getRows()) return pos;
        return new Position(pos.x(), newRow);
    }

    default Position applySliderRedirect(Zombie zombie, Position previous, Position next, GameSession session) {
        if (zombie == null || next == null || session == null || session.getLawn() == null) return next;
        if (isFlyingOverSliders(zombie)) return next;

        Environment lawn = session.getLawn();
        int oldRow = (int) Math.round(previous == null ? next.y() : previous.y());
        int oldCol = (int) Math.floor(previous == null ? next.x() : previous.x());
        int newCol = (int) Math.floor(next.x());

        if (newCol == oldCol) return next;

        int row = Math.max(0, Math.min(lawn.getRows() - 1, (int) Math.round(next.y())));
        Cell cell = lawn.getCell(row, newCol);
        if (cell == null || cell.getTile() == null) return next;
        Tile tile = cell.getTile();
        if (tile.type() != TileType.Slippery || tile.slipperyDirection() == null) return next;

        int redirectedRow = row + (tile.slipperyDirection() == SlipperyDirection.UP ? -1 : 1);
        if (redirectedRow < 0 || redirectedRow >= lawn.getRows()) return next;
        return new Position(next.x(), redirectedRow);
    }

    default boolean isFlyingOverSliders(Zombie zombie) {
        if (zombie == null || zombie.getAlias() == null) return false;
        String alias = zombie.getAlias().toLowerCase().replace("_", "").replace("-", "").replace(" ", "");
        return alias.contains("dodorider") || alias.contains("iceagedodo");
    }
}
