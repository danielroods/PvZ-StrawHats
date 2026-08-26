package model.collections.zombie.zombie_effect;

import model.collections.Faction;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.projectile.zombie_projectile.SnowballProjectile;
import model.utils.GameSession;
import service.GameClock;

public class IceAgeHunterEffect implements ZombieEffectStatus {
    private final double snowballDelay;
    private double actionTimer;

    public IceAgeHunterEffect(double snowballDelay) {
        this.snowballDelay = snowballDelay;
        this.actionTimer = snowballDelay;
    }

    @Override
    public void applyTickEffect(Zombie target, GameSession session) {
        if (!target.isAlive() || target.getPosition() == null) return;

        actionTimer += GameClock.SECONDS_PER_TICK;
        if (actionTimer >= snowballDelay) {
            if (dischargeFrostProjectiles(target, session)) {
                actionTimer = 0;
            }
        }
    }

    private boolean dischargeFrostProjectiles(Zombie actor, GameSession session) {
        if (session.getEnvironment() == null) return false;

        int r = (int) actor.getPosition().y();
        double currentX = actor.getPosition().x();
        int maxCols = session.getEnvironment().getCols();
        Position spawnOrigin = actor.getPosition();

        if (actor.getFaction() == Faction.ZOMBIES) {
            for (int colCursor = (int) currentX; colCursor >= 0; colCursor--) {
                if (colCursor >= maxCols) continue;
                Cell gridCell = session.getEnvironment().getCell(r, colCursor);
                if (gridCell != null && gridCell.getPlant() != null && gridCell.getPlant().isAlive()) {
                    Position destination = new Position(gridCell.getPlant().getLocation().x(), gridCell.getPlant().getLocation().y());
                    session.addZombieProjectile(new SnowballProjectile(spawnOrigin, destination, 0.8, session));
                    actor.setActionAnimationState("throw", 0.6, false);
                    return true;
                }
            }
        } else {
            for (Zombie foe : session.getZombies()) {
                if (foe.isAlive() && foe.getFaction() == Faction.ZOMBIES
                        && (int) foe.getPosition().y() == r && foe.getPosition().x() > currentX) {
                    session.addZombieProjectile(new SnowballProjectile(spawnOrigin, foe.getPosition(), 0.8, session));
                    actor.setActionAnimationState("throw", 0.6, false);
                    return true;
                }
            }
        }
        return false;
    }
}
