package model.collections.zombie.zombie_effect;

import model.collections.Faction;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.projectile.zombie_projectile.BoneProjectile;
import model.utils.GameSession;
import service.GameClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraveErectorStatus implements ZombieEffectStatus {
    private final double tombGenerationDelay;
    private final int maxTombsPerAction;
    private double actionClock;

    public GraveErectorStatus(double cooldown, int spawnCount) {
        this.tombGenerationDelay = cooldown;
        this.maxTombsPerAction = spawnCount;
        this.actionClock = cooldown;
    }

    @Override
    public void applyTickEffect(Zombie target, GameSession session) {
        if (!target.isAlive() || target.getFaction() == Faction.PLANTS || target.getPosition() == null) {
            return;
        }

        actionClock += GameClock.SECONDS_PER_TICK;
        if (actionClock >= tombGenerationDelay) {
            actionClock = 0;
            launchNecroticSpire(target, session);
        }
    }

    private void launchNecroticSpire(Zombie spellcaster, GameSession session) {
        spellcaster.setActionAnimationState("power", 0.7, false);
        if (session.getEnvironment() == null) return;

        List<Cell> emptyGridSpots = new ArrayList<>();
        int gridRows = session.getEnvironment().getRows();
        int gridCols = session.getEnvironment().getCols();
        int currentX = (int) spellcaster.getPosition().x();

        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                if (c <= currentX) {
                    Cell checkedCell = session.getEnvironment().getCell(r, c);
                    if (checkedCell != null && checkedCell.getPlant() == null && checkedCell.getStructure() == null) {
                        emptyGridSpots.add(checkedCell);
                    }
                }
            }
        }

        if (emptyGridSpots.isEmpty()) return;
        Collections.shuffle(emptyGridSpots);

        int spawnedSpires = 0;
        for (Cell chosenCell : emptyGridSpots) {
            if (spawnedSpires >= maxTombsPerAction) break;

            Position origin = spellcaster.getPosition();
            Position targetLocation = new Position(chosenCell.getCol(), chosenCell.getRow());

            session.addZombieProjectile(new BoneProjectile(origin, targetLocation, 1.5, session));
            spawnedSpires++;
        }
    }
}
