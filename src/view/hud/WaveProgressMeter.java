package view.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

public final class WaveProgressMeter extends Widget implements Disposable {

    public enum Mode {
        WAVES,
        PLAIN,
        BOSS,
        TIMER
    }

    private static final float FRAME_W = 273f;
    private static final float FRAME_H = 33f;
    private static final float FRAME_CAP = 12f;
    private static final float INSET_X = 10f;
    private static final float INSET_TOP = 8f;
    private static final float INSET_BOTTOM = 9f;

    private static final float MAX_BAR_WIDTH = 560f;
    private static final float FILL_EASE_PER_SECOND = 7.0f;
    private static final float SNAP_BACK_THRESHOLD = 0.2f;
    private static final float FLAG_RAISE_SECONDS = 0.45f;
    private static final float HEAD_POP_SECONDS = 0.55f;
    private static final float SHIMMER_PERIOD = 2.6f;

    private final Texture frameTexture;
    private final Texture headTexture;
    private final Texture poleTexture;
    private final Texture flagTexture;
    private final Texture pixelTexture;

    private final TextureRegion frameLeft;
    private final TextureRegion frameMiddle;
    private final TextureRegion frameRight;
    private final TextureRegion head;
    private final TextureRegion pole;
    private final TextureRegion flag;
    private final TextureRegion pixel;

    private final List<Integer> flagWaves = new ArrayList<>();
    private final List<Float> flagRaise = new ArrayList<>();

    private Mode mode = Mode.WAVES;
    private float target;
    private float displayed;
    private int totalWaves = 1;
    private boolean hugeWaveIncoming;
    private boolean snapNextValue = true;
    private float time;
    private float headPop;

    public WaveProgressMeter() {
        setTouchable(Touchable.disabled);

        frameTexture = loadTexture("assets/images/ui/progress_meter.png");
        headTexture = loadTexture("assets/images/ui/progress_meter_zombiehead.png");
        poleTexture = loadTexture("assets/images/ui/progress_meter_flag_pole.png");
        flagTexture = loadTexture("assets/images/ui/progress_meter_flag_default.png");

        Pixmap white = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        white.setColor(Color.WHITE);
        white.fill();
        pixelTexture = new Texture(white);
        white.dispose();
        pixel = new TextureRegion(pixelTexture);

        boolean framed = frameTexture.getWidth() == (int) FRAME_W
                && frameTexture.getHeight() == (int) FRAME_H;
        if (framed) {
            frameLeft = new TextureRegion(frameTexture, 0, 0, (int) FRAME_CAP, (int) FRAME_H);
            frameMiddle = new TextureRegion(frameTexture, (int) FRAME_CAP, 0,
                    (int) (FRAME_W - 2 * FRAME_CAP), (int) FRAME_H);
            frameRight = new TextureRegion(frameTexture, (int) (FRAME_W - FRAME_CAP), 0,
                    (int) FRAME_CAP, (int) FRAME_H);
        } else {
            frameLeft = new TextureRegion(frameTexture);
            frameMiddle = new TextureRegion(frameTexture);
            frameRight = new TextureRegion(frameTexture);
        }

        head = new TextureRegion(headTexture);
       pole = poleTexture.getWidth() >= 10
                ? new TextureRegion(poleTexture, 0, 0, 10, poleTexture.getHeight())
                : new TextureRegion(poleTexture);
        flag = new TextureRegion(flagTexture);
    }

    @Override
    public float getPrefHeight() {
        return FRAME_H;
    }

    @Override
    public float getPrefWidth() {
        return FRAME_W;
    }

    public void setMode(Mode mode) {
        if (this.mode != mode) {
            this.mode = mode;
            snapNextValue = true;
        }
    }

    public void setValue(float value) {
        target = MathUtils.clamp(value, 0f, 1f);
        if (snapNextValue || (mode == Mode.WAVES && target < displayed - SNAP_BACK_THRESHOLD)) {
            reset();
        }
    }

    public void setTotalWaves(int totalWaves) {
        this.totalWaves = Math.max(1, totalWaves);
    }

    public void setFlagWaves(List<Integer> waves) {
        if (waves == null) {
            if (!flagWaves.isEmpty()) {
                flagWaves.clear();
                flagRaise.clear();
            }
            return;
        }
        if (flagWaves.equals(waves)) return;
        flagWaves.clear();
        flagWaves.addAll(waves);
        flagRaise.clear();
        for (int i = 0; i < flagWaves.size(); i++) {
            flagRaise.add(flagAt(i) <= displayed + 1e-4f ? 1f : 0f);
        }
    }

    public void setHugeWaveIncoming(boolean incoming) {
        this.hugeWaveIncoming = incoming;
    }

    public void pulse() {
        headPop = 1f;
    }

    public void reset() {
        displayed = target;
        headPop = 0f;
        snapNextValue = false;
        for (int i = 0; i < flagRaise.size(); i++) {
            flagRaise.set(i, flagAt(i) <= displayed + 1e-4f ? 1f : 0f);
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;

        if (mode == Mode.WAVES) target = Math.max(target, displayed);
        displayed += (target - displayed) * Math.min(1f, delta * FILL_EASE_PER_SECOND);
        if (Math.abs(target - displayed) < 0.0005f) displayed = target;

        if (headPop > 0f) headPop = Math.max(0f, headPop - delta / HEAD_POP_SECONDS);
        for (int i = 0; i < flagRaise.size(); i++) {
            float raise = flagRaise.get(i);
            boolean reached = flagAt(i) <= displayed + 1e-4f;
            if (reached && raise < 1f) {
                flagRaise.set(i, Math.min(1f, raise + delta / FLAG_RAISE_SECONDS));
            } else if (!reached && raise > 0f) {
                flagRaise.set(i, 0f);
            }
        }
    }

    private float flagAt(int index) {
        if (index < 0 || index >= flagWaves.size()) return 1f;
        return MathUtils.clamp(flagWaves.get(index) / (float) totalWaves, 0f, 1f);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        float alpha = parentAlpha * getColor().a;
        if (alpha <= 0f) return;

        float height = getHeight();
        if (height <= 0f || getWidth() <= 0f) return;

        float scale = height / FRAME_H;
        float barWidth = Math.min(getWidth(), MAX_BAR_WIDTH);
        float barX = getX() + (getWidth() - barWidth) * 0.5f;
        float barY = getY();

        float innerX = barX + INSET_X * scale;
        float innerY = barY + INSET_BOTTOM * scale;
        float innerW = Math.max(1f, barWidth - 2f * INSET_X * scale);
        float innerH = Math.max(1f, height - (INSET_TOP + INSET_BOTTOM) * scale);

        drawTrack(batch, alpha, innerX, innerY, innerW, innerH);
        drawFill(batch, alpha, innerX, innerY, innerW, innerH);
        drawWaveTicks(batch, alpha, innerX, innerY, innerW, innerH);
        drawFrame(batch, alpha, barX, barY, barWidth, height, scale);
        if (mode == Mode.WAVES) {
            drawFlags(batch, alpha, innerX, innerY, innerW, innerH, scale);
        }
        drawHead(batch, alpha, innerX, innerY, innerW, innerH, height);

        batch.setColor(Color.WHITE);
    }

    private void drawTrack(Batch batch, float alpha, float x, float y, float w, float h) {
        batch.setColor(0.06f, 0.07f, 0.05f, 0.62f * alpha);
        batch.draw(pixel, x, y, w, h);
        batch.setColor(0f, 0f, 0f, 0.30f * alpha);
        batch.draw(pixel, x, y + h * 0.62f, w, h * 0.38f);
    }

    private void drawFill(Batch batch, float alpha, float x, float y, float w, float h) {
        float filled = w * MathUtils.clamp(displayed, 0f, 1f);
        if (filled <= 0.5f) return;

        float pulse = 1f;
        if (hugeWaveIncoming && mode == Mode.WAVES) {
            pulse = 1f + 0.22f * (0.5f + 0.5f * MathUtils.sin(time * 7.5f));
        }

        Color base = baseColor();
        Color dark = darkColor();
        Color light = lightColor();

        batch.setColor(dark.r * pulse, dark.g * pulse, dark.b * pulse, alpha);
        batch.draw(pixel, x, y, filled, h);
        batch.setColor(base.r * pulse, base.g * pulse, base.b * pulse, alpha);
        batch.draw(pixel, x, y + h * 0.26f, filled, h * 0.58f);
        batch.setColor(light.r * pulse, light.g * pulse, light.b * pulse, alpha);
        batch.draw(pixel, x, y + h * 0.60f, filled, h * 0.28f);
        batch.setColor(1f, 1f, 1f, 0.38f * alpha);
        batch.draw(pixel, x, y + h - Math.max(1f, h * 0.16f), filled, Math.max(1f, h * 0.16f));
        batch.setColor(0f, 0f, 0f, 0.22f * alpha);
        batch.draw(pixel, x, y, filled, Math.max(1f, h * 0.14f));

        drawShimmer(batch, alpha, x, y, filled, h);

        float lip = Math.max(1.5f, h * 0.22f);
        batch.setColor(light.r, light.g, light.b, 0.85f * alpha);
        batch.draw(pixel, x + filled - lip, y, lip, h);
    }

    private void drawShimmer(Batch batch, float alpha, float x, float y, float filled, float h) {
        float cycle = (time % SHIMMER_PERIOD) / SHIMMER_PERIOD;
        float band = Math.max(10f, h * 1.8f);
        float centre = -band + cycle * (filled + 2f * band);
        int slices = 6;
        float slice = band / slices;
        for (int i = 0; i < slices; i++) {
            float falloff = 1f - Math.abs((i + 0.5f) / slices * 2f - 1f);
            float left = Math.max(x, x + centre + i * slice);
            float right = Math.min(x + filled, x + centre + (i + 1) * slice);
            if (right <= left) continue;
            batch.setColor(1f, 1f, 1f, 0.16f * falloff * alpha);
            batch.draw(pixel, left, y, right - left, h);
        }
    }

    private void drawWaveTicks(Batch batch, float alpha, float x, float y, float w, float h) {
        if (mode != Mode.WAVES || totalWaves <= 1 || totalWaves > 12) return;
        float tick = Math.max(1f, h * 0.10f);
        for (int wave = 1; wave < totalWaves; wave++) {
            float at = x + w * (wave / (float) totalWaves);
            batch.setColor(0f, 0f, 0f, 0.28f * alpha);
            batch.draw(pixel, at - tick * 0.5f, y, tick, h);
        }
    }

    private void drawFrame(Batch batch, float alpha, float x, float y, float w, float h,
                           float scale) {
        float cap = FRAME_CAP * scale;
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(frameLeft, x, y, cap, h);
        batch.draw(frameMiddle, x + cap, y, Math.max(0f, w - 2f * cap), h);
        batch.draw(frameRight, x + w - cap, y, cap, h);
    }

    private void drawFlags(Batch batch, float alpha, float x, float y, float w, float h,
                           float scale) {
        if (flagWaves.isEmpty()) return;

        float barHeight = h + (INSET_TOP + INSET_BOTTOM) * scale;
        float poleH = Math.max(22f, barHeight * 1.38f);
        float poleW = Math.max(3f, poleH * pole.getRegionWidth() / (float) pole.getRegionHeight());
        float flagH = Math.max(10f, poleH * 0.42f);
        float flagW = Math.max(12f, flagH * flag.getRegionWidth() / (float) flag.getRegionHeight());
        float poleY = y - INSET_BOTTOM * scale;

        for (int i = 0; i < flagWaves.size(); i++) {
            float at = x + w * flagAt(i);
            float poleX = MathUtils.clamp(at - poleW * 0.5f, x - poleW * 0.5f,
                    x + w - poleW * 0.5f);

            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(pole, poleX, poleY, poleW, poleH);

            float raise = i < flagRaise.size() ? flagRaise.get(i) : 0f;
            float eased = raise * raise * (3f - 2f * raise);
            float ripple = raise >= 1f ? 1f + 0.07f * MathUtils.sin(time * 5.5f + i) : 1f;
            float pop = eased < 1f ? 1f + 0.25f * MathUtils.sin(eased * MathUtils.PI) : 1f;
            float width = flagW * (0.55f + 0.45f * eased) * ripple * pop;
            float height = flagH * (0.80f + 0.20f * eased);
            float flagY = poleY + poleH - height - poleH * 0.06f;

            if (raise <= 0f) {
                batch.setColor(0.62f, 0.62f, 0.62f, 0.85f * alpha);
            } else {
                batch.setColor(1f, 1f, 1f, alpha);
            }
            batch.draw(flag, poleX + poleW * 0.5f, flagY, width, height);
        }
    }

    private void drawHead(Batch batch, float alpha, float x, float y, float w, float h,
                          float barHeight) {
        float pop = headPop * headPop;
        float headH = barHeight * (1.42f + 0.28f * pop);
        float headW = headH * head.getRegionWidth() / (float) head.getRegionHeight();

        float at = x + w * MathUtils.clamp(displayed, 0f, 1f);
        float centreX = MathUtils.clamp(at, x, x + w);
        float bob = MathUtils.sin(time * 5.2f) * barHeight * 0.05f;
        float tilt = MathUtils.sin(time * 2.6f) * 4f + pop * 12f;

        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(head, centreX - headW * 0.5f, y + (h - headH) * 0.5f + bob,
                headW * 0.5f, headH * 0.5f, headW, headH, 1f, 1f, tilt);
    }

    private Color baseColor() {
        return switch (mode) {
            case BOSS -> new Color(0.78f, 0.14f, 0.17f, 1f);
            case TIMER -> new Color(0.98f, 0.72f, 0.20f, 1f);
            default -> new Color(0.38f, 0.78f, 0.24f, 1f);
        };
    }

    private Color darkColor() {
        return switch (mode) {
            case BOSS -> new Color(0.40f, 0.04f, 0.07f, 1f);
            case TIMER -> new Color(0.66f, 0.40f, 0.04f, 1f);
            default -> new Color(0.16f, 0.46f, 0.11f, 1f);
        };
    }

    private Color lightColor() {
        return switch (mode) {
            case BOSS -> new Color(0.96f, 0.44f, 0.38f, 1f);
            case TIMER -> new Color(1f, 0.90f, 0.52f, 1f);
            default -> new Color(0.66f, 0.94f, 0.44f, 1f);
        };
    }

    private static Texture loadTexture(String path) {
        String resolved = path;
        if (!Gdx.files.internal(resolved).exists() && resolved.startsWith("assets/")) {
            resolved = resolved.substring("assets/".length());
        }
        if (Gdx.files.internal(resolved).exists()) {
            Texture texture = new Texture(Gdx.files.internal(resolved));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return texture;
        }
        Pixmap fallback = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        fallback.setColor(Color.WHITE);
        fallback.fill();
        Texture texture = new Texture(fallback);
        fallback.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        frameTexture.dispose();
        headTexture.dispose();
        poleTexture.dispose();
        flagTexture.dispose();
        pixelTexture.dispose();
    }
}
