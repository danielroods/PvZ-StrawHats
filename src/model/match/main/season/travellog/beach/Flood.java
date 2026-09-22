package model.match.main.season.travellog.beach;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.collections.zombie.VulnerabilityType;
import model.collections.zombie.Zombie;
import model.collections.zombie.zombie_move.SnorkelMove;
import model.match.main.levels.Level;
import model.pitches.Cell;
import model.pitches.Tile;
import model.pitches.TileType;
import model.utils.GameSession;


public final class Flood {
    private Flood() {}

    public static void initialize(Level level, GameSession session) {
        if (level == null || session == null) return;
        level.setMaxTideColumn(Math.min(Beach.DEFAULT_MAX_TIDE_COLUMNS, Math.max(0, level.getCols() - 1)));
        if (level.getCurrentTideColumn() <= 0) {
            level.setCurrentTideColumn(level.getMaxTideColumn());
        }
        apply(level, session);
    }

    public static void riselevel(Level level, GameSession session) {
        if (level == null) return;
        int old = level.getCurrentTideColumn();
        level.setCurrentTideColumn(Math.min(old + 1, level.getMaxTideColumn()));
        if (session != null) apply(level, session);
    }

    public static void falllevel(Level level, GameSession session) {
        if (level == null) return;
        int old = level.getCurrentTideColumn();
        level.setCurrentTideColumn(Math.max(old - 1, 0));
        if (session != null) apply(level, session);
    }

    public static void riselevel(Level level) { riselevel(level, GameSession.peekInstance()); }
    public static void falllevel(Level level) { falllevel(level, GameSession.peekInstance()); }

    public static void apply(Level level, GameSession session) {
        if (level == null || session == null || session.getEnvironment() == null) return;

        int waterStart = Math.max(0, session.getCols() - level.getCurrentTideColumn());

        for (int row = 0; row < session.getRows(); row++) {
            for (int col = 0; col < session.getCols(); col++) {
                Cell cell = session.getEnvironment().getCell(row, col);
                if (cell == null) continue;

                boolean flooded = col >= waterStart;
                Tile tile = cell.getTile();
                boolean wasWater = tile != null && tile.type() == TileType.Water;

                if (flooded) {
                    if (!wasWater) {
                        cell.setTile(new Tile(TileType.Water));
                        washLandPlants(cell);
                    }
                } else {
                    if (wasWater) {
                        cell.setTile(new Tile(TileType.Normal));
                        
                        
                        removeAquaticPlant(cell);
                    }
                }
            }
        }

        for (Zombie zombie : session.getZombies()) {
            if (zombie == null || !zombie.isAlive() || zombie.getPosition() == null) continue;
            boolean flooded = zombie.getPosition().x() >= waterStart;
            if (zombie.getMoveBehavior() instanceof SnorkelMove && flooded
                    && zombie.getZombieState() != model.collections.zombie.ZombieState.EATING) {
                zombie.setVulnerabilityState(VulnerabilityType.SUBMERGED);
            } else if (zombie.getVulnerabilityState() == VulnerabilityType.SUBMERGED) {
                zombie.setVulnerabilityState(VulnerabilityType.FULLY_VULNERABLE);
            }
        }
    }

    
    private static void washLandPlants(Cell cell) {
        Plant top = cell.getPlant();
        if (top == null || !top.isAlive()) return;

        Plant bottom = top.getBottom();
        if (bottom != null && bottom.isAlive() && bottom.getTags().contains(PlantTag.WATER)) {
            
            return;
        }

        if (!top.getTags().contains(PlantTag.WATER)) {
            top.setAlive(false);
            cell.setPlant(null);
        }
    }

    private static void removeAquaticPlant(Cell cell) {
        Plant top = cell.getPlant();
        if (top == null || !top.isAlive()) return;

        if (top.getTags().contains(PlantTag.WATER)) {
            
            
            if (top.getTags().contains(PlantTag.STACK)) return;
            
            
            top.setHP(0);
            top.setAlive(false);
            cell.setPlant(null);
            return;
        }

        Plant bottom = top.getBottom();
        if (bottom != null && bottom.getTags().contains(PlantTag.WATER)) {
            
            
            if (top.isAlive()) return;
        }
    }
    
    public static void applyBigWaveWash(Level level, GameSession session) {
        if (level == null || session == null || session.getEnvironment() == null) return;
        int waterStart = Math.max(0, session.getCols() - level.getCurrentTideColumn());
        for (int row = 0; row < session.getRows(); row++) {
            for (int col = waterStart; col < session.getCols(); col++) {
                Cell cell = session.getEnvironment().getCell(row, col);
                if (cell != null) washLandPlants(cell);
            }
        }
    }

}