package model.collections.zombie.zombie_effect;

import model.collections.Faction;
import model.collections.Item;
import model.collections.item.GroundItem;
import model.collections.item.GroundSun;
import model.collections.item.ItemType;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.utils.GameSession;
import service.GameClock;

public class SunThief implements ZombieEffectStatus {
    private static final double GRAB_PERIOD = 5.0;
    // How long Ra's one-shot "power_up" beat plays before the looping
    // "power" clip takes over for the rest of the grab window.
    private static final double POWER_UP_DURATION = 0.5;

    private final boolean directBankStealer;
    private final int lootLimit;
    private final double refundPercentage;
    private final double weaponChargeDuration;
    private final int beamAttackDamage;

    private int collectedSuns = 0;
    private boolean refundDispensedOnDeath = false;
    private double stateClock = 0;
    private double tickAccumulator = 0;
    private boolean engagingInTheft = false;
    private boolean beamDischarged = false;

    private GroundItem designatedTarget;
    private double lockOnTimer = 0;
    private Position groundedTargetOrigin;

    public SunThief(boolean isBankThief, int maxSunsToSteal, double dropRatioOnDeath, double chargingTime, int laserDamage) {
        this.directBankStealer = isBankThief;
        this.lootLimit = maxSunsToSteal;
        this.refundPercentage = dropRatioOnDeath;
        this.weaponChargeDuration = chargingTime;
        this.beamAttackDamage = laserDamage;
    }

    @Override
    public void applyTickEffect(Zombie target, GameSession session) {
        if (!target.isAlive()) return;

        if (target.getFaction() == Faction.PLANTS) {
            if (directBankStealer && !beamDischarged) {
                scorchHostilesWithBeam(target, session);
                beamDischarged = true;
            }
            return;
        }

        if (directBankStealer) {
            handleVaultBreaker(target, session);
        } else {
            handleScavengerBehavior(target, session);
        }
    }

    @Override
    public void onDeath(Zombie target, GameSession session) {
        if (refundDispensedOnDeath || session == null) return;
        int refundVal = (int) Math.round(collectedSuns * refundPercentage);
        if (refundVal > 0) session.addSun(refundVal);
        refundDispensedOnDeath = true;
    }

    private void handleScavengerBehavior(Zombie raider, GameSession session) {
        if (collectedSuns >= lootLimit) return;

        if (designatedTarget != null && (!designatedTarget.isAlive() || designatedTarget.getItemType() != ItemType.SUN)) {
            designatedTarget = null;
            groundedTargetOrigin = null;
            lockOnTimer = 0;
            raider.clearActionAnimationState();
        }

        if (designatedTarget == null) {
            designatedTarget = scanForFallenSun(session);
            lockOnTimer = 0;
            if (designatedTarget == null) return;
            // Just locked on: play the one-shot power-up beat, then the
            // looping power beat carries the rest of the GRAB_PERIOD window.
            groundedTargetOrigin = designatedTarget.getPosition();
            raider.setActionAnimationState("power_up", POWER_UP_DURATION, false);
        } else if (lockOnTimer >= POWER_UP_DURATION && !"power".equals(raider.getActionAnimationState())) {
            raider.setActionAnimationState("power", GRAB_PERIOD - POWER_UP_DURATION, true);
        }

        lockOnTimer += GameClock.SECONDS_PER_TICK;

        // Drag the sun across the lawn toward Ra for the rest of the grab
        // window, rather than leaving it sitting still until it vanishes.
        if (groundedTargetOrigin != null && raider.getPosition() != null) {
            double progress = Math.min(1.0, lockOnTimer / GRAB_PERIOD);
            Position pulled = groundedTargetOrigin.add(
                    raider.getPosition().sub(groundedTargetOrigin).scale(progress));
            designatedTarget.setPosition(pulled);
        }

        if (lockOnTimer >= GRAB_PERIOD) {
            consumeGroundSun(designatedTarget);
            designatedTarget = null;
            groundedTargetOrigin = null;
            lockOnTimer = 0;
            raider.clearActionAnimationState();
        }
    }

    private GroundItem scanForFallenSun(GameSession session) {
        for (Item entry : session.getItems()) {
            if (entry instanceof GroundItem groundLoot
                    && groundLoot.isAlive()
                    && !groundLoot.isCollected()
                    && groundLoot.getItemType() == ItemType.SUN
                    && !(groundLoot instanceof GroundSun fallingSun && fallingSun.isFalling())) {
                return groundLoot;
            }
        }
        return null;
    }

    private void consumeGroundSun(GroundItem sunItem) {
        if (sunItem instanceof GroundSun groundSun) {
            collectedSuns = Math.min(collectedSuns + groundSun.getSunValue(), lootLimit);
        }
        sunItem.setAlive(false);
    }

    private void handleVaultBreaker(Zombie raider, GameSession session) {
        if (beamDischarged) return;

        if (!engagingInTheft) {
            if (detectImminentVegetation(raider, session)) engagingInTheft = true;
        } else {
            stateClock += GameClock.SECONDS_PER_TICK;
            tickAccumulator += GameClock.SECONDS_PER_TICK;

            if (tickAccumulator >= 1.0 && stateClock <= weaponChargeDuration) {
                tickAccumulator = 0;
                int stealRate = Math.min(25, session.getSunCount());
                if (stealRate > 0) {
                    session.spendSun(stealRate);
                    collectedSuns += stealRate;
                }
            }

            if (stateClock >= weaponChargeDuration) {
                dischargeBeamWeapons(raider, session);
                beamDischarged = true;
                engagingInTheft = false;
            }
        }
    }

    private boolean detectImminentVegetation(Zombie raider, GameSession session) {
        if (session.getEnvironment() == null) return false;

        int r = (int) raider.getPosition().y();
        int c = (int) raider.getPosition().x();
        for (int step = 1; step <= 4; step++) {
            int scanCol = c - step;
            if (scanCol >= 0) {
                Cell cell = session.getEnvironment().getCell(r, scanCol);
                if (cell != null && cell.getPlant() != null && cell.getPlant().isAlive()) return true;
            }
        }
        return false;
    }

    private void dischargeBeamWeapons(Zombie raider, GameSession session) {
        if (session.getEnvironment() == null) return;

        int r = (int) raider.getPosition().y();
        int c = (int) raider.getPosition().x();
        for (int step = 1; step <= 4; step++) {
            int scanCol = c - step;
            if (scanCol >= 0) {
                Cell cell = session.getEnvironment().getCell(r, scanCol);
                if (cell != null && cell.getPlant() != null && cell.getPlant().isAlive()) {
                    cell.getPlant().takeDamage(beamAttackDamage, raider);
                }
            }
        }
    }

    private void scorchHostilesWithBeam(Zombie renegade, GameSession session) {
        int r = (int) renegade.getPosition().y();
        int c = (int) renegade.getPosition().x();
        session.getZombies().stream()
                .filter(z -> z.isAlive() && z.getFaction() == Faction.ZOMBIES && (int) z.getPosition().y() == r)
                .forEach(z -> {
                    double range = z.getPosition().x() - c;
                    if (range > 0 && range <= 4.0) z.takeDamage(beamAttackDamage);
                });
    }
}
