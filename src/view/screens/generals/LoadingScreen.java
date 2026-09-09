package view.screens.generals;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Actor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import controller.ScreenManager;
import model.utils.GameSettings;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

/**
 * Generic "please wait" screen shown between two real screens: stages -> before match,
 * end of match -> after match screen, and whenever a mini-game/co-op screen is entered.
 * <p>
 * Picks one of four background images at random, fills a green progress bar (with a
 * little rising-bubble effect) inside the box baked into those backgrounds, rides the
 * PAM LOAD_ICON_BACK/LOAD_ICON_FRONT "animation" clips along the bar as a spinner/cursor,
 * and then swaps itself out for the real destination screen once the bar finishes.
 */
public class LoadingScreen extends BaseScreen {

    private static final String[] BACKGROUNDS = {
            "assets/images/backg/loadscreen/ChatGPT Image Aug 30, 2026, 04_00_01 PM.png",
            "assets/images/backg/loadscreen/ChatGPT Image Aug 30, 2026, 11_05_53 PM.png",
            "assets/images/backg/loadscreen/ChatGPT Image Aug 30, 2026, 11_10_49 PM.png",
            "assets/images/backg/loadscreen/ChatGPT Image Aug 30, 2026, 11_17_04 PM.png"
    };

    private static final String PAM_ROOT = "assets/pvz-assets";
    private static final String LOAD_ICON_BACK_PATH = "768/INITIAL/EFFECTS/LOAD_ICON_BACK/LOAD_ICON_BACK.PAM";
    private static final String LOAD_ICON_FRONT_PATH = "768/INITIAL/EFFECTS/LOAD_ICON_FRONT/LOAD_ICON_FRONT.PAM";
    private static final String PAM_STATE = "animation";

    // --- Geometry of the empty box baked into the loadscreen backgrounds -------------
    // These four numbers are the only thing you should need to touch to line the bar up
    // pixel-perfectly with the box drawn into your PNGs (screen is SCREEN_WIDTH x
    // SCREEN_HEIGHT = 1280x720). As given they roughly match the box shown in the
    // reference mock-up (centered, in the lower third of the screen).
    // Made noticeably smaller / more inset than before so the green fill stays safely
    // inside the drawn box on every one of the 4 backgrounds. Shrink BAR_BOX_WIDTH /
    // BAR_BOX_HEIGHT further (or raise BAR_PADDING) if it still pokes out on a given
    // image, and nudge BAR_BOX_X / BAR_BOX_Y to re-center it on that image's box.
    private static final float BAR_BOX_WIDTH = 420f;
    private static final float BAR_BOX_HEIGHT = 30f;
    private static final float BAR_BOX_X = (BaseScreen.SCREEN_WIDTH - BAR_BOX_WIDTH) / 2f;
    private static final float BAR_BOX_Y = 50f;
    private static final float BAR_PADDING = 5f;

    private static final float MIN_DURATION = 1.6f;
    private static final float MAX_DURATION = 2.6f;

    private static final int BUBBLE_COUNT = 14;

    private final Supplier<BaseScreen> nextScreenSupplier;
    private final float duration;
    private float elapsed = 0f;
    private boolean switching = false;

    private TextureBank textureBank;
    private PamPlayer pamPlayer;

    private Texture whitePixel;
    private Texture bubbleTexture;
    private BitmapFont font;
    private final List<Bubble> bubbles = new ArrayList<>();

    private static class Bubble {
        float x, yOffset, speed, size, alpha;
    }

    public LoadingScreen(Supplier<BaseScreen> nextScreenSupplier) {
        this(nextScreenSupplier, MathUtils.random(MIN_DURATION, MAX_DURATION));
    }

    public LoadingScreen(Supplier<BaseScreen> nextScreenSupplier, float duration) {
        this.nextScreenSupplier = nextScreenSupplier;
        this.duration = Math.max(0.2f, duration);
    }

    @Override
    public void render(float delta) {
        // TextureBank streams its atlas pages in lazily; without calling update() every
        // frame the PAM textures never finish loading and the clips silently draw
        // nothing. (Same call GreenhouseScreen / GameScreen make each frame.)
        if (textureBank != null) {
            try {
                textureBank.update();
            } catch (Throwable t) {
                if (GameSettings.get().isDebugMode()) {
                    Gdx.app.error("LoadingScreen", "textureBank.update() failed", t);
                }
            }
        }
        super.render(delta);
    }

    @Override
    public void initParticles() {
        // Intentionally empty: this screen builds its own lightweight bubble effect
        // instead of using the shared ParticleCreator image-particle system.
    }

    @Override
    public void show() {
        setBackground(BACKGROUNDS[MathUtils.random(BACKGROUNDS.length - 1)]);
        super.show();

        initPam();
        createBarTextures();
        spawnBubbles();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        rootStack.add(buildLoadingBarActor());
    }

    private void initPam() {
        try {
            FileHandle root = Gdx.files.internal(PAM_ROOT);
            textureBank = new TextureBank("atlases", root);
            pamPlayer = new PamPlayer(textureBank, root);

            // getClip() only returns clip *metadata* - the actual textures for a PAM
            // aren't queued in until loadAsync() is called, and textureBank.update()
            // (called every frame in render()) is what actually streams them in after
            // that. Without this call the clip exists but has nothing to draw.
            pamPlayer.loadAsync(LOAD_ICON_BACK_PATH, null);
            pamPlayer.loadAsync(LOAD_ICON_FRONT_PATH, null);
        } catch (Throwable t) {
            textureBank = null;
            pamPlayer = null;
            if (GameSettings.get().isDebugMode()) {
                Gdx.app.error("LoadingScreen", "PAM icon initialization failed; spinner will be skipped.", t);
            }
        }
    }

    /** Same "animation" state, but falls back to a few common alternates if the exact
     *  clip name in the PAM doesn't match, so the icon degrades gracefully instead of
     *  just not drawing at all. Looked up fresh each frame (cheap map lookups) rather
     *  than cached once, since the clip may still be mid-load the first few frames. */
    private ClipRef getSafeClip(String pamPath) {
        if (pamPlayer == null) return null;
        String[] candidates = {PAM_STATE, "idle", "default", "loop", "main", ""};
        for (String name : candidates) {
            try {
                ClipRef clip = pamPlayer.getClip(pamPath, name);
                if (clip != null) return clip;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private void createBarTextures() {
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        whitePixel = new Texture(pixel);
        pixel.dispose();

        int d = 16;
        Pixmap dot = new Pixmap(d, d, Pixmap.Format.RGBA8888);
        dot.setColor(1f, 1f, 1f, 1f);
        dot.fillCircle(d / 2, d / 2, d / 2 - 1);
        bubbleTexture = new Texture(dot);
        bubbleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        dot.dispose();
    }

    private void spawnBubbles() {
        bubbles.clear();
        for (int i = 0; i < BUBBLE_COUNT; i++) {
            Bubble b = new Bubble();
            resetBubble(b, true);
            bubbles.add(b);
        }
    }

    private void resetBubble(Bubble b, boolean randomStart) {
        b.size = MathUtils.random(3f, 7f);
        b.speed = MathUtils.random(18f, 42f);
        b.alpha = MathUtils.random(0.35f, 0.75f);
        b.x = MathUtils.random(0f, 1f);
        b.yOffset = randomStart ? MathUtils.random(0f, BAR_BOX_HEIGHT - 2 * BAR_PADDING) : 0f;
    }

    private Actor buildLoadingBarActor() {
        return new Actor() {
            @Override
            public void act(float delta) {
                super.act(delta);
                elapsed += delta;

                float innerHeight = BAR_BOX_HEIGHT - 2 * BAR_PADDING;
                for (Bubble b : bubbles) {
                    b.yOffset += b.speed * delta;
                    if (b.yOffset > innerHeight) {
                        resetBubble(b, false);
                    }
                }

                if (!switching && elapsed >= duration) {
                    switching = true;
                    BaseScreen next = nextScreenSupplier.get();
                    ScreenManager.setScreen(next);
                }
            }

            @Override
            public void draw(Batch batch, float parentAlpha) {
                float progress = MathUtils.clamp(elapsed / duration, 0f, 1f);
                drawBoxFrame(batch);
                drawGreenFill(batch, progress);
                drawBubbles(batch, progress);
                drawSpinnerIcon(batch);
                drawLabel(batch, progress);
            }
        };
    }

    private void drawBoxFrame(Batch batch) {
        // Recessed slot behind the green fill.
        batch.setColor(0f, 0f, 0f, 0.45f);
        batch.draw(whitePixel, BAR_BOX_X, BAR_BOX_Y, BAR_BOX_WIDTH, BAR_BOX_HEIGHT);

        // Thin frame around the slot.
        batch.setColor(0.05f, 0.05f, 0.05f, 0.85f);
        float t = 2f;
        batch.draw(whitePixel, BAR_BOX_X, BAR_BOX_Y, BAR_BOX_WIDTH, t); // bottom
        batch.draw(whitePixel, BAR_BOX_X, BAR_BOX_Y + BAR_BOX_HEIGHT - t, BAR_BOX_WIDTH, t); // top
        batch.draw(whitePixel, BAR_BOX_X, BAR_BOX_Y, t, BAR_BOX_HEIGHT); // left
        batch.draw(whitePixel, BAR_BOX_X + BAR_BOX_WIDTH - t, BAR_BOX_Y, t, BAR_BOX_HEIGHT); // right

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private float fillWidth(float progress) {
        return (BAR_BOX_WIDTH - 2 * BAR_PADDING) * progress;
    }

    private void drawGreenFill(Batch batch, float progress) {
        float fillW = fillWidth(progress);
        if (fillW <= 0f) return;
        float x = BAR_BOX_X + BAR_PADDING;
        float y = BAR_BOX_Y + BAR_PADDING;
        float h = BAR_BOX_HEIGHT - 2 * BAR_PADDING;

        // Base green fill (PvZ-ish grass green).
        batch.setColor(0.20f, 0.62f, 0.15f, 1f);
        batch.draw(whitePixel, x, y, fillW, h);

        // Lighter glossy strip along the top for a bit of depth.
        batch.setColor(0.47f, 0.86f, 0.30f, 0.55f);
        batch.draw(whitePixel, x, y + h * 0.55f, fillW, h * 0.35f);

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawBubbles(Batch batch, float progress) {
        float fillW = fillWidth(progress);
        if (fillW <= 1f) return;
        float baseX = BAR_BOX_X + BAR_PADDING;
        float baseY = BAR_BOX_Y + BAR_PADDING;

        for (Bubble b : bubbles) {
            float bx = baseX + b.x * fillW;
            float by = baseY + b.yOffset;
            float fade = 1f - (b.yOffset / (BAR_BOX_HEIGHT - 2 * BAR_PADDING));
            batch.setColor(0.85f, 1f, 0.85f, b.alpha * MathUtils.clamp(fade, 0f, 1f));
            batch.draw(bubbleTexture, bx - b.size / 2f, by - b.size / 2f, b.size, b.size);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawSpinnerIcon(Batch batch) {
        if (pamPlayer == null) return;
        float iconScale = 0.6f;
        float iconX = BAR_BOX_X + BAR_BOX_WIDTH + 26f;
        float iconY = BAR_BOX_Y + BAR_BOX_HEIGHT / 2f;

        drawPamClip(batch, getSafeClip(LOAD_ICON_BACK_PATH), iconX, iconY, iconScale * 0.50f, false);
        drawPamClip(batch, getSafeClip(LOAD_ICON_FRONT_PATH), iconX, iconY, iconScale * 0.50f, false);
    }

    private void drawPamClip(Batch batch, ClipRef clip, float x, float y, float scale, boolean flip) {
        if (clip == null) return;
        try {
            batch.flush();
            Matrix4 old = batch.getTransformMatrix().cpy();
            batch.getTransformMatrix().translate(x, y, 0f).scale(scale, scale, 1f);
            batch.setTransformMatrix(batch.getTransformMatrix());
            pamPlayer.draw(batch, clip, elapsed, 0f, 0f, flip);
            batch.flush();
            batch.setTransformMatrix(old);
        } catch (Throwable t) {
            if (GameSettings.get().isDebugMode()) {
                Gdx.app.error("LoadingScreen", "Failed to draw loading icon clip.", t);
            }
        }
    }

    private final GlyphLayout layout = new GlyphLayout();

    private void drawLabel(Batch batch, float progress) {
        if (font == null) return;
        String text = "Loading... " + (int) (progress * 100f) + "%";
        layout.setText(font, text);
        float x = BAR_BOX_X + BAR_BOX_WIDTH / 2f - layout.width / 2f;
        float y = BAR_BOX_Y + BAR_BOX_HEIGHT + layout.height + 10f;
        font.draw(batch, layout, x, y);
    }

    @Override
    public void dispose() {
        if (whitePixel != null) whitePixel.dispose();
        if (bubbleTexture != null) bubbleTexture.dispose();
        if (font != null) font.dispose();
        if (textureBank != null) {
            try {
                textureBank.dispose();
            } catch (Throwable ignored) {
            }
        }
        super.dispose();
    }
}