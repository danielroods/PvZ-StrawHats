package model.collections.zombie.zombie_effect;

import model.collections.Faction;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.projectile.zombie_projectile.OctopusProjectile;
import model.utils.GameSession;
import service.GameClock;

public class OctopusThrow implements ZombieEffectStatus {
    
    
    private static final double TOSS_ANIMATION_DURATION = 0.8;

    private final double snareCooldown;
    private double intervalTracker;

    public OctopusThrow(double snareCooldown) {
        this.snareCooldown = snareCooldown;
        this.intervalTracker = snareCooldown;
    }

    @Override
    public void applyTickEffect(Zombie target, GameSession session) {
        if (!target.isAlive() || target.getPosition() == null) return;

        intervalTracker += GameClock.SECONDS_PER_TICK;
        if (intervalTracker >= snareCooldown) {
            if (launchEntanglingOctopus(target, session)) {
                intervalTracker = 0;
            }
        }
    }

    private boolean launchEntanglingOctopus(Zombie activeOctopus, GameSession session) {
        if (session.getEnvironment() == null) return false;

        int r = (int) activeOctopus.getPosition().y();
        double colX = activeOctopus.getPosition().x();
        int maxCols = session.getEnvironment().getCols();
        Position originPos = activeOctopus.getPosition();

        if (activeOctopus.getFaction() == Faction.ZOMBIES) {
            for (int colIndex = (int) colX; colIndex >= 0; colIndex--) {
                if (colIndex >= maxCols) continue;
                Cell gridCell = session.getEnvironment().getCell(r, colIndex);
                if (gridCell != null && gridCell.getPlant() != null && gridCell.getPlant().isAlive()) {
                    Position targetPosition = new Position(gridCell.getPlant().getLocation().x(), gridCell.getPlant().getLocation().y());
                    session.addZombieProjectile(new OctopusProjectile(originPos, targetPosition, 1.5, session));
                    activeOctopus.setActionAnimationState("toss", TOSS_ANIMATION_DURATION, false);
                    return true;
                }
            }
        } else {
            for (Zombie threat : session.getZombies()) {
                if (threat.isAlive() && threat.getFaction() == Faction.ZOMBIES
                        && (int) threat.getPosition().y() == r && threat.getPosition().x() > colX) {
                    session.addZombieProjectile(new OctopusProjectile(originPos, threat.getPosition(), 1.5, session));
                    activeOctopus.setActionAnimationState("toss", TOSS_ANIMATION_DURATION, false);
                    return true;
                }
            }
        }
        return false;
    }
}