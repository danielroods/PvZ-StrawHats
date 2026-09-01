package model.collections.zombie.zombie_move;

import model.collections.Faction;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.collections.zombie.zombie_pushing_item.PushableStructure;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.Environment;
import model.utils.GameSession;

public class PusherMove implements MoveBehavior {
    private static final double PUSH_GAP = 0.6;

    @Override
    public void move(Zombie zombie, double deltaTime, GameSession session) {
        Position pos = zombie.getPosition();
        Environment lawn = session.getLawn();
        if (pos == null || lawn == null || zombie.getSpeed() == null) return;

        double deltaX = zombie.getSpeed().x() * deltaTime;
        double targetZombieX = pos.x() + deltaX;
        PushableStructure structure = zombie.getPushedStructure();

        if (structure != null && structure.isFalling()) {
            // Still dropping in from the sky (see ZombieFactory.spawnFallingIceBlockOnRelease) -
            // let it land in place before the zombie starts shoving it forward.
            structure.updateFall(deltaTime);
        }

        if (structure != null && structure.isAlive() && !structure.isFalling()) {
            // Continuous "push" beat for as long as the zombie is actively
            // shoving a structure (arcade cabinet / barrel); duration 0 means
            // it persists until cleared below rather than auto-expiring.
            zombie.setActionAnimationState("push", 0, true);

            int row = (int) Math.round(pos.y());
            Position oldPosition = structure.getPosition();
            if (oldPosition == null || Math.abs(oldPosition.y() - row) > 0.5
                    || Math.abs((pos.x() - oldPosition.x()) - PUSH_GAP) > 0.75) {
                oldPosition = new Position(pos.x() - PUSH_GAP, row);
                structure.setPosition(oldPosition);
            }

            Position nextPosition = new Position(oldPosition.x() + deltaX, row);
            int oldCol = (int) Math.round(oldPosition.x());
            int newCol = (int) Math.round(nextPosition.x());

            Cell nextCell = lawn.getCell(row, newCol);
            if (nextCell != null) {
                Plant plant = nextCell.getPlant();
                if (plant != null && plant.isAlive()) plant.takeDamage(plant.getHP(), zombie);
                for (Zombie other : nextCell.getZombies()) {
                    if (other != null && other.isAlive() && other != zombie && other.getFaction() == Faction.PLANTS) {
                        other.takeDamage(other.getHp(), zombie);
                    }
                }
            }

            structure.setPosition(nextPosition);
            if (oldCol != newCol) {
                Cell oldCell = lawn.getCell(row, oldCol);
                if (oldCell != null && oldCell.getStructure() == structure) oldCell.setStructure(null);
            }
            session.registerStructure(structure);
        } else if ("push".equals(zombie.getActionAnimationState())) {
            // No structure left to push (destroyed or never assigned this tick);
            // fall back to the zombie's normal walk/eat animation.
            zombie.clearActionAnimationState();
        }

        Position nextZombiePosition = new Position(targetZombieX, pos.y());
        nextZombiePosition = applySliderRedirect(zombie, pos, nextZombiePosition, session);
        zombie.setPosition(nextZombiePosition);
    }
}