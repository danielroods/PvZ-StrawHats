package model.collections.zombie.zombie_effect;

import model.collections.Faction;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.pitches.Cell;
import model.utils.GameSession;

public class ThermiteExplosion implements ZombieEffectStatus {
    private static final int PLANT_DAMAGE = 99999;

    private boolean detonated = false;
    private int blastRow = -1;

    public ThermiteExplosion() {
    }

    @Override
    public void applyTickEffect(Zombie target, GameSession session) {
    }

    @Override
    public void onDeath(Zombie target, GameSession session) {
        if (detonated || session == null || target == null || target.getPosition() == null) return;
        detonated = true;
        blastRow = (int) Math.round(target.getPosition().y());
        burnLane(target, session, blastRow);
    }

    private void burnLane(Zombie source, GameSession session, int row) {
        if (source.getFaction() == Faction.ZOMBIES) {
            int totalCols = session.getEnvironment().getCols();
            for (int col = 0; col < totalCols; col++) {
                Cell cell = session.getEnvironment().getCell(row, col);
                if (cell == null) continue;
                Plant plant = cell.getPlant();
                if (plant != null && plant.isAlive()) plant.takeDamage(PLANT_DAMAGE, source);
            }
            view.GeneralPrinter.print("The Jalapeno Zombie went up in flames and torched lane "
                    + (row + 1) + ".");
            return;
        }

        for (Zombie other : new java.util.ArrayList<>(session.getZombies())) {
            if (other == source || !other.isAlive()) continue;
            if (other.getFaction() != Faction.ZOMBIES) continue;
            if (other.getPosition() == null) continue;
            if ((int) Math.round(other.getPosition().y()) != row) continue;
            other.takeDamage(other.getHp() + 1, source);
        }
    }

    public boolean hasDetonated() {
        return detonated;
    }

    public int getBlastRow() {
        return blastRow;
    }
}
