package model.collections.zombie.zombie_attack;

import model.collections.Item;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import service.GameClock;

public class SmashAttack implements AttackBehavior {
    private static final double DEFAULT_ACTION_STATE_DURATION = 0.6;

    private final int smashDamage;
    private final double windupDuration;
    private final boolean isOneTime;
    private final double speedScaleAfter;
    private final String actionState;
    private final double actionStateDuration;

    private double timer = 0.0;

    public SmashAttack(int smashDamage, double windupDuration, boolean isOneTime, double speedScaleAfter) {
        this(smashDamage, windupDuration, isOneTime, speedScaleAfter, null, DEFAULT_ACTION_STATE_DURATION);
    }

    public SmashAttack(int smashDamage, double windupDuration, boolean isOneTime, double speedScaleAfter,
                        String actionState, double actionStateDuration) {
        this.smashDamage = smashDamage;
        this.windupDuration = windupDuration;
        this.isOneTime = isOneTime;
        this.speedScaleAfter = speedScaleAfter;
        this.actionState = actionState;
        this.actionStateDuration = actionStateDuration;
    }

    @Override
    public void attack(Zombie zombie, GameSession session) {
        Item target = ZombieTargeting.findTarget(zombie, session);

        if (target == null || !target.isAlive()) {
            timer = 0;
            return;
        }

        timer += GameClock.SECONDS_PER_TICK;

        if (timer >= windupDuration) {
            if (actionState != null) {
                zombie.setActionAnimationState(actionState, actionStateDuration, false);
            }
            if (target instanceof Plant p) {
                p.takeDamage(smashDamage, zombie);
            } else {
                target.takeDamage(smashDamage);
            }

            timer = 0;
            if (isOneTime) {
                Position currentSpeed = zombie.getSpeed();
                if (currentSpeed != null) {
                    zombie.setSpeed(new Position(
                            currentSpeed.x() * speedScaleAfter,
                            currentSpeed.y() * speedScaleAfter
                    ));
                }

                zombie.setAttackBehavior(new ChompAttack());
            }
        }
    }
}