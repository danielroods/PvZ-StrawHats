package model.collections.zombie.zombie_effect;

import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.projectile.zombie_projectile.CrystalSkullBeamProjectile;
import model.utils.GameSession;
import service.GameClock;


public class CrystalSkullBeamEffect implements ZombieEffectStatus {

    private enum Phase { IDLE, POWER_UP, POWER, POWER_DOWN, ATTACK, COOLDOWN }

    private static final double POWER_UP_DURATION = 0.5;
    private static final double POWER_DOWN_DURATION = 0.5;
    private static final double ATTACK_DURATION = 0.5;
    
    private static final double BEAM_VISUAL_DURATION = 0.35;
    private static final int MAX_RANGE_COLS = 9;

    private final int laserDamage;
    private final double chargingTime;
    private final double cooldownTime;

    private Phase phase = Phase.IDLE;
    private double phaseClock = 0;
    private Plant lockedTarget;

    public CrystalSkullBeamEffect(int laserDamage, double chargingTime, double cooldownTime) {
        this.laserDamage = laserDamage;
        this.chargingTime = Math.max(POWER_UP_DURATION + POWER_DOWN_DURATION + ATTACK_DURATION + 0.1, chargingTime);
        this.cooldownTime = Math.max(0, cooldownTime);
    }

    @Override
    public void applyTickEffect(Zombie zombie, GameSession session) {
        if (!zombie.isAlive() || zombie.getPosition() == null || session == null) return;

        double delta = GameClock.SECONDS_PER_TICK;

        switch (phase) {
            case IDLE -> {
                Plant target = findTargetAhead(zombie, session);
                if (target == null) return;
                lockedTarget = target;
                phase = Phase.POWER_UP;
                phaseClock = 0;
                zombie.setActionAnimationState("power_up", POWER_UP_DURATION, false);
            }

            case POWER_UP -> {
                phaseClock += delta;
                if (phaseClock >= POWER_UP_DURATION) {
                    phase = Phase.POWER;
                    phaseClock = 0;
                    double powerDuration = chargingTime - POWER_UP_DURATION - POWER_DOWN_DURATION - ATTACK_DURATION;
                    zombie.setActionAnimationState("power", powerDuration, true);
                }
            }

            case POWER -> {
                phaseClock += delta;
                double powerDuration = chargingTime - POWER_UP_DURATION - POWER_DOWN_DURATION - ATTACK_DURATION;
                if (phaseClock >= powerDuration || !isStillValidTarget(zombie, session)) {
                    phase = Phase.POWER_DOWN;
                    phaseClock = 0;
                    zombie.setActionAnimationState("power_down", POWER_DOWN_DURATION, false);
                }
            }

            case POWER_DOWN -> {
                phaseClock += delta;
                if (phaseClock >= POWER_DOWN_DURATION) {
                    phase = Phase.ATTACK;
                    phaseClock = 0;
                    zombie.setActionAnimationState("attack", ATTACK_DURATION, false);
                }
            }

            case ATTACK -> {
                phaseClock += delta;
                if (phaseClock >= ATTACK_DURATION) {
                    fireBeam(zombie, session);
                    zombie.clearActionAnimationState();
                    phase = Phase.COOLDOWN;
                    phaseClock = 0;
                    lockedTarget = null;
                }
            }

            case COOLDOWN -> {
                phaseClock += delta;
                if (phaseClock >= cooldownTime) {
                    phase = Phase.IDLE;
                    phaseClock = 0;
                }
            }
        }
    }

    private boolean isStillValidTarget(Zombie zombie, GameSession session) {
        return lockedTarget != null && lockedTarget.isAlive()
                && findTargetAhead(zombie, session) != null;
    }

    private void fireBeam(Zombie zombie, GameSession session) {
        if (lockedTarget == null || !lockedTarget.isAlive()) {
            lockedTarget = findTargetAhead(zombie, session);
        }
        if (lockedTarget == null || zombie.getPosition() == null) return;

        lockedTarget.takeDamage(laserDamage, zombie);

        Position muzzle = zombie.getPosition();
        Position beamTarget = lockedTarget.getPosition();
        session.addZombieProjectile(
                new CrystalSkullBeamProjectile(muzzle, beamTarget, BEAM_VISUAL_DURATION, session));
    }

    
    private Plant findTargetAhead(Zombie zombie, GameSession session) {
        if (session.getEnvironment() == null || zombie.getPosition() == null) return null;

        int row = (int) Math.round(zombie.getPosition().y());
        int col = (int) Math.floor(zombie.getPosition().x());
        for (int step = 1; step <= MAX_RANGE_COLS; step++) {
            int scanCol = col - step;
            if (scanCol < 0) break;
            Cell cell = session.getEnvironment().getCell(row, scanCol);
            if (cell != null && cell.getPlant() != null && cell.getPlant().isAlive()) {
                return cell.getPlant();
            }
        }
        return null;
    }
}
