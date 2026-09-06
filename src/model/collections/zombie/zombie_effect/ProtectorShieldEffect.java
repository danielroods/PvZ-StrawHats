package model.collections.zombie.zombie_effect;

import model.collections.animations.AnimationFactory;
import model.collections.animations.ZombieAnimationRegistry;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieSequence;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import service.GameClock;

public class ProtectorShieldEffect implements ZombieEffectStatus {

    public static final String CLIP_SHIELD_START = "shield_start";
    public static final String CLIP_SHIELD_IDLE = "shield_idle";
    public static final String CLIP_SHIELD_END = "shield_end";

    private static final double FALLBACK_START_SECONDS = 0.6;
    private static final double FALLBACK_IDLE_SECONDS = 1.2;
    private static final double FALLBACK_END_SECONDS = 0.6;

    private final double castCooldownSeconds;
    private final int shieldAmount;
    private final double shieldRadiusColumns;

    private double cooldownTimer;

    public ProtectorShieldEffect(double castCooldownSeconds, int shieldAmount,
                                 double shieldRadiusColumns) {
        this.castCooldownSeconds = castCooldownSeconds <= 0 ? 6.0 : castCooldownSeconds;
        this.shieldAmount = shieldAmount <= 0 ? 300 : shieldAmount;
        this.shieldRadiusColumns = shieldRadiusColumns <= 0 ? 4.0 : shieldRadiusColumns;
        this.cooldownTimer = this.castCooldownSeconds;
    }

    @Override
    public void applyTickEffect(Zombie protector, GameSession session) {
        if (protector == null || !protector.isAlive() || session == null) return;
        if (!protector.isStunCompleted() || protector.isSequenceActive()) return;

        cooldownTimer += GameClock.SECONDS_PER_TICK;
        if (cooldownTimer < castCooldownSeconds) return;

        Zombie target = findShieldTarget(protector, session);
        if (target == null) return;

        cooldownTimer = 0;
        String pam = ZombieAnimationRegistry.pathFor(protector.getAlias());
        protector.playSequence(new ZombieSequence()
                .add(CLIP_SHIELD_START, clipSeconds(pam, CLIP_SHIELD_START, FALLBACK_START_SECONDS))
                .add(CLIP_SHIELD_IDLE, clipSeconds(pam, CLIP_SHIELD_IDLE, FALLBACK_IDLE_SECONDS), true,
                        (self, ignored) -> grantShield(self, target))
                .add(CLIP_SHIELD_END, clipSeconds(pam, CLIP_SHIELD_END, FALLBACK_END_SECONDS)));
    }

    private void grantShield(Zombie protector, Zombie target) {
        if (target == null || !target.isAlive() || target.hasShield()) return;
        target.applyShield(shieldAmount);
        view.GeneralPrinter.print(protector.getName() + " shielded " + target.getName()
                + " for " + shieldAmount + " hit points.");
    }

    private Zombie findShieldTarget(Zombie protector, GameSession session) {
        Position origin = protector.getPosition();
        if (origin == null || session.getZombies() == null) return null;

        Zombie best = null;
        double bestX = Double.MAX_VALUE;
        for (Zombie other : session.getZombies()) {
            if (other == null || other == protector || !other.isAlive()) continue;
            if (other.isBoss() || other.isHypnotized() || other.hasShield()) continue;
            Position position = other.getPosition();
            if (position == null) continue;
            if (Math.abs(position.x() - origin.x()) > shieldRadiusColumns) continue;
            if (Math.abs(position.y() - origin.y()) > 1.5) continue;
            if (position.x() < bestX) {
                bestX = position.x();
                best = other;
            }
        }
        return best;
    }

    private static double clipSeconds(String pamPath, String clip, double fallback) {
        float duration = AnimationFactory.exactClipDurationForPath(pamPath, clip);
        return duration > 0f ? duration : fallback;
    }
}
