package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.graphics.Color;

import model.collections.animations.AnimationFactory;
import view.screens.generals.GameScreen;

/**
 * Full-screen "VS" splash played right as a networked match begins: darkens the board,
 * plays the VS_ICON PAM's "intro" clip once, then its "outro" clip once, then gets out
 * of the way so the match can proceed as normal. Mirrors
 * {@link view.screens.match.after.MatchEndSequence}'s shading + clip-sequencing style,
 * but is driven from {@link GameScreen#drawMatchStartOverlay()} instead of the board's
 * own match-end hook.
 */
public class MatchStartOverlay {

    private enum Phase { IDLE, INTRO, OUTRO }

    private static final String VS_ICON_PAM =
            "768/FULL/UI/JOUST/MATCHLOADING/VS_ICON/VS_ICON.PAM";
    private static final float VS_ICON_SCALE = 0.6f;
    private static final float SHADE_ALPHA = 0.72f;
    
    private static final float FALLBACK_CLIP_DURATION = 1.2f;

    private final GameScreen screen;
    private Phase phase = Phase.IDLE;
    private float clipTime;

    public MatchStartOverlay(GameScreen screen) {
        this.screen = screen;
    }

    public boolean isActive() {
        return phase != Phase.IDLE;
    }

    
    public void start() {
        phase = Phase.INTRO;
        clipTime = 0f;
    }

    public void advance(float delta) {
        if (phase == Phase.IDLE) return;
        clipTime += delta;

        String state = phase == Phase.INTRO ? "intro" : "outro";
        float duration = AnimationFactory.clipDurationForPath(VS_ICON_PAM, state);
        if (duration <= 0f) duration = FALLBACK_CLIP_DURATION;
        if (clipTime < duration) return;

        if (phase == Phase.INTRO) {
            phase = Phase.OUTRO;
            clipTime = 0f;
        } else {
            phase = Phase.IDLE;
            clipTime = 0f;
        }
    }

    
    public void draw() {
        if (phase == Phase.IDLE) return;

        float viewW = screen.stage.getViewport().getWorldWidth();
        float viewH = screen.stage.getViewport().getWorldHeight();

        screen.batch.setColor(0f, 0f, 0f, SHADE_ALPHA);
        screen.batch.draw(screen.whitePixel, 0f, 0f, viewW, viewH);
        screen.batch.setColor(Color.WHITE);

        String state = phase == Phase.INTRO ? "intro" : "outro";
        screen.drawPam(VS_ICON_PAM, state, clipTime, viewW * 0.5f, viewH * 0.5f, VS_ICON_SCALE, false);
    }
}