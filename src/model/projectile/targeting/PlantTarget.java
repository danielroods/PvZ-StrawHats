package model.projectile.targeting;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match.main.season.travellog.cave.FrostbiteFreezing;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.obstacles.Grave;
import model.pitches.obstacles.IceBlock;
import model.pitches.obstacles.Obstacle;
import model.pitches.obstacles.OctopusWrap;
import model.utils.GameSession;

public final class PlantTarget {

    private final Zombie zombie;
    private final PushableStructure structure;
    private final Cell cell;

    private PlantTarget(Zombie zombie, PushableStructure structure, Cell cell) {
        this.zombie = zombie;
        this.structure = structure;
        this.cell = cell;
    }

    public static PlantTarget ofZombie(Zombie zombie) {
        return zombie == null ? null : new PlantTarget(zombie, null, null);
    }

    public static PlantTarget ofStructure(PushableStructure structure) {
        return structure == null ? null : new PlantTarget(null, structure, null);
    }

    public static PlantTarget ofCell(Cell cell) {
        return cell == null ? null : new PlantTarget(null, null, cell);
    }

    public Zombie getZombie() {
        return zombie;
    }

    public PushableStructure getStructure() {
        return structure;
    }

    public Cell getCell() {
        return cell;
    }

    public boolean isZombie() {
        return zombie != null;
    }

    public Position getPosition() {
        if (zombie != null) return zombie.getPosition();
        if (structure != null) return structure.getPosition();
        if (cell != null) return new Position(cell.getCol(), cell.getRow());
        return null;
    }

    public boolean isAlive() {
        if (zombie != null) return zombie.isAlive();
        if (structure != null) return structure.isAlive();
        return cell != null && isDestructible(cell.getObstacle());
    }

    public void takeDamage(int amount, Plant source, GameSession session, boolean fireDamage) {
        if (amount <= 0 || session == null) return;
        if (zombie != null) {
            zombie.takeDamage(amount, source);
            return;
        }
        if (structure != null) {
            structure.takeDamage(amount, source, session);
            return;
        }
        damageObstacle(cell, amount, session, fireDamage);
    }

    public static void damageObstacle(Cell cell, int amount, GameSession session,
                                      boolean fireDamage) {
        if (cell == null || session == null || amount <= 0) return;
        Obstacle obstacle = cell.getObstacle();
        if (obstacle instanceof Grave) {
            session.damageGrave(cell, amount);
        } else if (obstacle instanceof IceBlock) {
            FrostbiteFreezing.damageIce(cell, amount, fireDamage);
        } else if (obstacle instanceof OctopusWrap wrap) {
            wrap.takeDamage(amount);
        }
    }

    public static boolean isDestructible(Obstacle obstacle) {
        if (obstacle instanceof Grave grave) return grave.getHp() > 0;
        if (obstacle instanceof IceBlock iceBlock) return iceBlock.getHp() > 0;
        if (obstacle instanceof OctopusWrap wrap) return !wrap.isDead();
        return false;
    }
}
