package model.collections.zombie;

import model.collections.animations.AnimationFactory;
import model.collections.animations.ZombieAnimationRegistry;

public final class ZombieStunProfile {

    public static final String CLIP_START = "stun_start";
    public static final String CLIP_IDLE = "stun_idle";
    public static final String CLIP_END = "stun_end";

    private static final double FALLBACK_START_SECONDS = 0.6;
    private static final double FALLBACK_END_SECONDS = 0.6;

    private final double healthFraction;
    private final double stunSeconds;
    private final boolean immobilizeAfterwards;

    private boolean triggered;
    private boolean completed;

    public ZombieStunProfile(double healthFraction, double stunSeconds, boolean immobilizeAfterwards) {
        this.healthFraction = Math.max(0.0, Math.min(1.0, healthFraction));
        this.stunSeconds = Math.max(0.0, stunSeconds);
        this.immobilizeAfterwards = immobilizeAfterwards;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void update(Zombie zombie) {
        if (triggered || zombie == null || !zombie.isAlive() || zombie.getMaxHp() <= 0) return;
        if (zombie.getHp() > zombie.getMaxHp() * healthFraction) return;

        triggered = true;
        String pam = ZombieAnimationRegistry.pathFor(zombie.getAlias());
        zombie.playSequence(new ZombieSequence()
                .add(CLIP_START, clipSeconds(pam, CLIP_START, FALLBACK_START_SECONDS))
                .add(CLIP_IDLE, stunSeconds, true)
                .add(CLIP_END, clipSeconds(pam, CLIP_END, FALLBACK_END_SECONDS))
                .onComplete(() -> {
                    completed = true;
                    if (immobilizeAfterwards) zombie.setImmobilized(true);
                }));
    }

    static double clipSeconds(String pamPath, String clip, double fallback) {
        float duration = AnimationFactory.exactClipDurationForPath(pamPath, clip);
        return duration > 0f ? duration : fallback;
    }
}
