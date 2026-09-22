package view.screens.generals;

import com.badlogic.gdx.math.MathUtils;

import controller.cheat.NukeCheatController;
import model.collections.animations.AnimationFactory;

import java.util.Random;


final class NukeEffect {

    private static final String MISSILE_PAM =
            "768/INITIAL/EFFECTS/ZOMBOSS_MISSILE_EXPLOSION_EGYPT/ZOMBOSS_MISSILE_EXPLOSION_EGYPT.PAM";
    private static final String FALL_CLIP = "missile";
    private static final String IMPACT_CLIP = "missile_explosion";

    private static final float FALL_SECONDS = 1f; 
    private static final float FALL_HEIGHT = 900f; 
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
                    
                    
                    stage = Stage.IDLE;
                    stageElapsed = 0f;
                }
            }
            default -> { }
        }
    }

    
    void drawMissile() {
        if (stage != Stage.FALL && stage != Stage.IMPACT) return;

        float centerX = screen.getBoardCenterX();
        float centerY = screen.getBoardCenterY();

        if (stage == Stage.FALL) {
            float progress = MathUtils.clamp(stageElapsed / FALL_SECONDS, 0f, 1f);
            
            float eased = progress * progress;
            float y = centerY + FALL_HEIGHT * (1f - eased);
            screen.drawPam(MISSILE_PAM, FALL_CLIP, stageElapsed, centerX, y, MISSILE_SCALE, false);
        } else {
            screen.drawPam(MISSILE_PAM, IMPACT_CLIP, stageElapsed, centerX, centerY, MISSILE_SCALE, false);
        }
    }

    
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
        
        float fade = 1f - MathUtils.clamp(stageElapsed / SHAKE_SECONDS, 0f, 1f);
        return SHAKE_MAGNITUDE * fade;
    }

    
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