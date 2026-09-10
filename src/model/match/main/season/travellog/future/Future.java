package model.match.main.season.travellog.future;

import model.match.main.levels.special_levels.BossLevel;
import model.match.main.season.Season;
import model.pitches.Cell;
import model.pitches.Environment;
import model.pitches.Tile;
import model.pitches.TileType;
import model.utils.GameSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Future extends Season {
    private static final TileType[] LINK_TILE_PAIRS = {
            TileType.LinkTile01, TileType.LinkTile02, TileType.LinkTile03
    };

    private static final Random RANDOM = new Random();

    public Future() {
        super("Future");
    }

    @Override
    public boolean isNight() { return true; }

    @Override
    public void placeSeasonObstacles(GameSession session) {
        if (session == null || session.getEnvironment() == null) return;
        Environment env = session.getEnvironment();
        boolean isBoss = session.getLevel() instanceof BossLevel;

        List<Cell> candidates = new ArrayList<>();
        for (int row = 0; row < env.getRows(); row++) {
            for (int col = 0; col < env.getCols(); col++) {
                Cell cell = env.getCell(row, col);
                if (cell != null && cell.getTile() == null && cell.getObstacle() == null && !cell.hasPlant()) {
                    candidates.add(cell);
                }
            }
        }
        Collections.shuffle(candidates, RANDOM);

        if (isBoss) {
            for (TileType pairType : LINK_TILE_PAIRS) {
                placePair(candidates, pairType);
            }
            return;
        }

        for (TileType pairType : LINK_TILE_PAIRS) {
            if (RANDOM.nextBoolean()) {
                placePair(candidates, pairType);
            }
        }
    }

    private static void placePair(List<Cell> candidates, TileType pairType) {
        int placed = 0;
        while (placed < 2 && !candidates.isEmpty()) {
            Cell cell = candidates.remove(candidates.size() - 1);
            cell.setTile(new Tile(pairType));
            placed++;
        }
    }

    public static boolean isLinkTile(TileType type) {
        return type == TileType.LinkTile01 || type == TileType.LinkTile02 || type == TileType.LinkTile03;
    }

    public static Cell findLinkPartnerCell(GameSession session, int row, int col, TileType pairType) {
        if (session == null || session.getEnvironment() == null || !isLinkTile(pairType)) return null;
        Environment env = session.getEnvironment();
        for (int r = 0; r < env.getRows(); r++) {
            for (int c = 0; c < env.getCols(); c++) {
                if (r == row && c == col) continue;
                Cell cell = env.getCell(r, c);
                if (cell != null && cell.getTile() != null && cell.getTile().type() == pairType) {
                    return cell;
                }
            }
        }
        return null;
    }
}