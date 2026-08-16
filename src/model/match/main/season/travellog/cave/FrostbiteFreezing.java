package model.match.main.season.travellog.cave;

import model.collections.plant.Plant;
import model.collections.plant.PlantTag;
import model.projectile.Projectile;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
import model.pitches.obstacles.IceBlock;
import model.utils.GameSession;

public final class FrostbiteFreezing {
    public static final int MAX_CHILL_LEVEL = 3;
    public static final int ICE_BLOCK_HP = 600;
    public static final double ADJACENT_FIRE_MELT_DPS = 60.0;

    private FrostbiteFreezing() { }


    public static boolean isFrozenInIce(GameSession session, Plant plant) {
        if (session == null || session.getEnvironment() == null || plant == null || plant.getPosition() == null) return false;
        Cell cell = findPlantCell(session.getEnvironment(), plant);
        return cell != null && cell.getObstacle() instanceof IceBlock ice && ice.getFrozenPlant() == plant;
    }

    public static boolean isFrozenInIce(GameSession session, Zombie zombie) {
        if (session == null || session.getEnvironment() == null || zombie == null || zombie.getPosition() == null) return false;
        int row = (int) Math.round(zombie.getPosition().y());
        int col = (int) Math.round(zombie.getPosition().x());
        Cell cell = session.getEnvironment().getCell(row, col);
        return cell != null && cell.getObstacle() instanceof IceBlock ice && ice.getFrozenZombie() == zombie;
    }

    public static boolean damageFrozenPlantIfInIce(GameSession session, Plant plant, int damage, boolean fireDamage) {
        if (!isFrozenInIce(session, plant)) return false;
        Cell cell = findPlantCell(session.getEnvironment(), plant);
        return damageIce(cell, damage, fireDamage);
    }

    public static boolean damageFrozenZombieIfInIce(GameSession session, Zombie zombie, int damage, boolean fireDamage) {
        if (!isFrozenInIce(session, zombie)) return false;
        int row = (int) Math.round(zombie.getPosition().y());
        int col = (int) Math.round(zombie.getPosition().x());
        return damageIce(session.getEnvironment().getCell(row, col), damage, fireDamage);
    }

    public static boolean isFireDamageSource(Object source) {
        if (source instanceof Plant plant) return plant.getTags().contains(PlantTag.FIRE);
        if (source instanceof Projectile projectile) {
            Plant plant = projectile.getSourcePlant();
            return plant != null && plant.getTags().contains(PlantTag.FIRE);
        }
        return false;
    }

    public static boolean canAddChill(Plant plant) {
        return plant != null
                && plant.isAlive()
                && !plant.getTags().contains(PlantTag.FIRE)
                && plant.getChillLevel() < MAX_CHILL_LEVEL
                && plant.getPlantState() != Plant.PlantState.DYING;
    }

    public static boolean addChillLevel(GameSession session, Plant plant) {
        if (session == null || session.getEnvironment() == null || !canAddChill(plant)) return false;
        Cell cell = findPlantCell(session.getEnvironment(), plant);
        if (cell == null) return false;

        int newLevel = Math.min(MAX_CHILL_LEVEL, plant.getChillLevel() + 1);
        plant.setChillLevel(newLevel);
        if (newLevel >= MAX_CHILL_LEVEL) {
            if (!(cell.getObstacle() instanceof IceBlock iceBlock) || iceBlock.getFrozenPlant() != plant) {
                cell.setObstacle(new IceBlock(plant, ICE_BLOCK_HP));
            }
            plant.setState(Plant.PlantState.INCAPACITATED);
        }
        return true;
    }

    public static boolean freezeZombieInIce(GameSession session, Zombie zombie, int row, int col) {
        if (session == null || session.getEnvironment() == null || zombie == null || !zombie.isAlive()) return false;
        Cell cell = session.getEnvironment().getCell(row, col);
        if (cell == null || cell.getObstacle() != null || cell.getPlant() != null) return false;
        zombie.setPosition(new Position(col, row));
        zombie.setStatus(Zombie.Status.FREEZE);
        cell.setObstacle(new IceBlock(zombie, ICE_BLOCK_HP));
        return true;
    }

    public static boolean damageIce(Cell cell, int damage, boolean fireDamage) {
        if (cell == null || !(cell.getObstacle() instanceof IceBlock iceBlock)) return false;
        boolean destroyed = fireDamage
                ? iceBlock.takeDamage(IceBlock.BASE_HP)
                : iceBlock.takeDamage(Math.max(0, damage));
        if (destroyed) cell.setObstacle(null);
        return destroyed;
    }

    public static void damageAdjacentIceBlocks(GameSession session, Position center, int mode, int damage, boolean fireDamage) {
        if (session == null || session.getEnvironment() == null || center == null) return;
        Environment env = session.getEnvironment();
        int row = (int) Math.round(center.y());
        int col = (int) Math.round(center.x());
        for (int r = 0; r < env.getRows(); r++) {
            for (int c = 0; c < env.getCols(); c++) {
                Cell cell = env.getCell(r, c);
                if (!(cell.getObstacle() instanceof IceBlock)) continue;
                boolean inRange = switch (mode) {
                    case 1 -> r == row && c == col;
                    case 2 -> Math.abs(r - row) <= 1 && Math.abs(c - col) <= 1;
                    case 3 -> r == row;
                    case 4 -> true;
                    default -> false;
                };
                if (inRange) damageIce(cell, damage, fireDamage);
            }
        }
    }

    public static void meltFromAdjacentFirePlants(GameSession session, double deltaSeconds) {
        if (session == null || session.getEnvironment() == null || deltaSeconds <= 0) return;
        Environment environment = session.getEnvironment();
        int damage = Math.max(1, (int) Math.round(ADJACENT_FIRE_MELT_DPS * deltaSeconds));

        for (int row = 0; row < environment.getRows(); row++) {
            for (int col = 0; col < environment.getCols(); col++) {
                Cell cell = environment.getCell(row, col);
                if (!(cell.getObstacle() instanceof IceBlock iceBlock)) continue;

                if (iceBlock.getFrozenPlant() != null && !iceBlock.getFrozenPlant().isAlive()) {
                    cell.setObstacle(null);
                    continue;
                }
                if (iceBlock.getFrozenZombie() != null && !iceBlock.getFrozenZombie().isAlive()) {
                    cell.setObstacle(null);
                    continue;
                }

                if (hasAdjacentFirePlant(environment, row, col)) {
                    damageIce(cell, damage, false);
                }
            }
        }
    }

    private static boolean hasAdjacentFirePlant(Environment environment, int row, int col) {
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int colOffset = -1; colOffset <= 1; colOffset++) {
                if (rowOffset == 0 && colOffset == 0) continue;
                Cell neighbour = environment.getCell(row + rowOffset, col + colOffset);
                Plant plant = neighbour == null ? null : neighbour.getPlant();
                if (plant != null && plant.isAlive() && plant.getTags().contains(PlantTag.FIRE)) return true;
            }
        }
        return false;
    }

    private static Cell findPlantCell(Environment environment, Plant plant) {
        Position position = plant.getLocation();
        if (position == null) return null;
        int row = (int) Math.round(position.y());
        int col = (int) Math.round(position.x());
        Cell cell = environment.getCell(row, col);
        return cell != null && cell.getPlant() == plant ? cell : null;
    }
}
