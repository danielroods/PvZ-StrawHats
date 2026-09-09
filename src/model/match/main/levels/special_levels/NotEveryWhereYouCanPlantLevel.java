package model.match.main.levels.special_levels;

import model.match.main.levels.Level;
import model.pitches.Cell;
import model.pitches.obstacles.MoldBlock;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.List;

/// "Not Every Where You Can Plant!" - mold covers one or more whole columns of the
/// lawn (by default just the leftmost column) and those tiles can never be planted
/// on for the whole level. The blocked tiles are rendered with the
/// STAR_OBJECTIVE_MOLD "idle" animation (see MoldBlock).
public class NotEveryWhereYouCanPlantLevel extends Level {

    /// Columns (0-indexed from the left) that are covered in mold and unplantable.
    /// Defaults to just the first column on the left side of the grid.
    private List<Integer> blockedColumns = new ArrayList<>(List.of(0));

    @Override
    public void initSpecial(GameSession session) {
        if (session == null || session.getEnvironment() == null || blockedColumns == null) return;

        for (int col : blockedColumns) {
            if (col < 0 || col >= session.getEnvironment().getCols()) continue;
            for (int row = 0; row < session.getEnvironment().getRows(); row++) {
                Cell cell = session.getEnvironment().getCell(row, col);
                if (cell != null && cell.getObstacle() == null) {
                    cell.setObstacle(new MoldBlock());
                }
            }
        }
    }

    public List<Integer> getBlockedColumns() { return blockedColumns; }
    public void setBlockedColumns(List<Integer> blockedColumns) {
        this.blockedColumns = (blockedColumns == null || blockedColumns.isEmpty())
                ? new ArrayList<>(List.of(0))
                : blockedColumns;
    }
}