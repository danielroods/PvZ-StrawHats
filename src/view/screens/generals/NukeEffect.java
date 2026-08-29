package view.screens.generals;

import com.badlogic.gdx.math.MathUtils;

import controller.cheat.NukeCheatController;
import model.collections.animations.AnimationFactory;

import java.util.Random;

/**
 * Drives the "release the nuke" cheat's on-screen sequence for a GameScreen:
 *
 *  1. FALL     - the Egypt Zomboss missile PAM drops from the sky toward the
 *                center of the screen (reuses the same missile art already
 *                used by the real Egypt Zomboss fight).
 *  2. IMPACT   - the missile explosion PAM plays and every non-boss zombie is
 *                killed (via {@link NukeCheatController}) at that moment.
 *  3. SHAKE    - the whole screen (board + HUD alike, since both share the
 *                same stage/camera) shakes while a white flash smoothly fades
 *                in, holds, then smoothly fades back out.
 *
 * This is intentionally decoupled from the boss-fight's ZombossSkyStrike:
 * that class targets a specific lawn tile and destroys a plant there, which
 * is boss-fight-only behavior. The cheat instead always drops in the middle
 * of the screen and never touches plants, so it works on any level.
 */
final class NukeEffect {

    private static final String MISSILE_PAM =
            "768/INITIAL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_EGYPT/ZOMBOSS_MISSILE_EXPLOSION_EGYPT.PAM";
    private static final String FALL_CLIP = "missile";
    private static final String IMPACT_CLIP = "missile_explosion";

    private static final float FALL_SECONDS = 1f; // "drop from the sky" delay requested by the user
    private static final float FALL_HEIGHT = 900f; // px above screen center the missile starts at
    private static final float MISSILE_SCALE = 0.65f;

    private static final float SHAKE_SECONDS = 0.9f;
    private static final float SHAKE_MAGNITUDE = 14f;

    private static final float FLASH_IN_SECONDS = 0.18f;
    private static final float FLASH_HOLD_SECONDS = 0.35f;
    private static final float FLASH_OUT_SECONDS = 0.55f;
    private static final float FLASH_MAX_ALPHA = 0.92f;

    private enum Stage { IDLE, FALL, IMPACT }

    private final GameScreen screen;
    private final NukeCheatController cheatController = new NukeCheatController();
    private final Random random = new Random();

    private Stage stage = Stage.IDLE;
    private float stageElapsed;
    private float impactSeconds = 1f;
    private boolean zombiesCleared;

    NukeEffect(GameScreen screen) {
        this.screen = screen;
    }

    boolean isActive() {
        return stage != Stage.IDLE;
    }

    void preload() {
        screen.preloadPam(MISSILE_PAM);
    }

    /** Starts the sequence. Safe to call again once a previous run has finished. */
    void trigger() {
        if (isActive()) return;
        stage = Stage.FALL;
        stageElapsed = 0f;
        zombiesCleared = false;
        float exact = AnimationFactory.exactClipDurationForPath(MISSILE_PAM, IMPACT_CLIP);
        impactSeconds = exact > 0 ? exact : 1f;
    }

    void tick(float delta) {
        if (stage == Stage.IDLE) return;
        stageElapsed += delta;

        switch (stage) {
            case FALL -> {
                if (stageElapsed >= FALL_SECONDS) {
                    // Zombies die at the moment of impact, exactly one second
                    // after the button was clicked.
                    if (!zombiesCleared) {
                        cheatController.detonate();
                        zombiesCleared = true;
                    }
                    stage = Stage.IMPACT;
                    stageElapsed = 0f;
                }
            }
            case IMPACT -> {
                float total = Math.max(impactSeconds, FLASH_IN_SECONDS + FLASH_HOLD_SECONDS + FLASH_OUT_SECONDS);
                if (stageElapsed >= total) {
                    // Back to IDLE (not just DONE) so isActive() drops and the
                    // button can trigger another run immediately.
                    stage = Stage.IDLE;
                    stageElapsed = 0f;
                }
            }
            default -> { }
        }
    }

    /** Draws the falling/exploding missile. Must be called inside the board's batch.begin()/end(). */
    void drawMissile() {
        if (stage != Stage.FALL && stage != Stage.IMPACT) return;

        float centerX = screen.getBoardCenterX();
        float centerY = screen.getBoardCenterY();

        if (stage == Stage.FALL) {
            float progress = MathUtils.clamp(stageElapsed / FALL_SECONDS, 0f, 1f);
            // Ease-in so the missile accelerates downward, like it's dropping from orbit.
            float eased = progress * progress;
            float y = centerY + FALL_HEIGHT * (1f - eased);
            screen.drawPam(MISSILE_PAM, FALL_CLIP, stageElapsed, centerX, y, MISSILE_SCALE, false);
        } else {
            screen.drawPam(MISSILE_PAM, IMPACT_CLIP, stageElapsed, centerX, centerY, MISSILE_SCALE, false);
        }
    }

    /** Camera shake offset for this frame; add to the camera position, then restore it after drawing. */
    float shakeOffsetX() {
        return shakeActive() ? (random.nextFloat() * 2f - 1f) * currentShakeMagnitude() : 0f;
    }

    float shakeOffsetY() {
        return shakeActive() ? (random.nextFloat() * 2f - 1f) * currentShakeMagnitude() : 0f;
    }

    private boolean shakeActive() {
        return stage == Stage.IMPACT && stageElapsed <= SHAKE_SECONDS;
    }

    private float currentShakeMagnitude() {
        // Shake dies down smoothly rather than cutting off abruptly.
        float fade = 1f - MathUtils.clamp(stageElapsed / SHAKE_SECONDS, 0f, 1f);
        return SHAKE_MAGNITUDE * fade;
    }

    /** Current alpha [0,1] for the full-screen white flash overlay. */
    float flashAlpha() {
        if (stage != Stage.IMPACT) return 0f;
        float t = stageElapsed;
        if (t < FLASH_IN_SECONDS) {
            return FLASH_MAX_ALPHA * (t / FLASH_IN_SECONDS);
        }
        t -= FLASH_IN_SECONDS;
        if (t < FLASH_HOLD_SECONDS) {
            return FLASH_MAX_ALPHA;
        }
        t -= FLASH_HOLD_SECONDS;
        if (t < FLASH_OUT_SECONDS) {
            return FLASH_MAX_ALPHA * (1f - t / FLASH_OUT_SECONDS);
        }
        return 0f;
    }
}