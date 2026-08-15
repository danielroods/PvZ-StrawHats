package view.general_screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;

import java.util.ArrayList;
import java.util.List;

import static view.general_screens.BaseScreen.SCREEN_HEIGHT;
import static view.general_screens.BaseScreen.SCREEN_WIDTH;

public class ParticleCreator {

    // ۱. مقدار آلفا روی ۱ ثابت شد
    private static final float FIXED_ALPHA = 1.0f;
    private final int particleCount;
    private final float minSpeed, maxSpeed, fastness, minSize, maxSize;
    private final boolean isRotating;
    private final boolean enabled;

    private static class Particle {
        float x, y, speedX, speedY, size, alpha, rotation, rotationSpeed;
        TextureRegion region;
    }

    private final Texture[] textures;
    private final Particle[] particles;

    private boolean worldMode = false;
    private float worldX, worldY, worldW, worldH;

    public ParticleCreator(String[] particleImages, int count, float minSize, float maxSize,
                           float fastness, boolean isRotating) {
        this.fastness = fastness;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.isRotating = isRotating;
        this.minSpeed = -100f * fastness;
        this.maxSpeed = -30f * fastness;

        List<Texture> loaded = new ArrayList<>();
        if (particleImages != null) {
            for (String path : particleImages) {
                if (path != null && Gdx.files.internal(path).exists()) {
                    Texture tex = new Texture(Gdx.files.internal(path));
                    tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    loaded.add(tex);
                }
            }
        }
        this.enabled = !loaded.isEmpty();
        this.textures = loaded.toArray(new Texture[0]);
        this.particleCount = enabled ? count : 0;

        particles = new Particle[particleCount];
        for (int i = 0; i < particleCount; i++) {
            particles[i] = new Particle();
            spawn(particles[i], true);
        }
    }

    public ParticleCreator(String[] particleImages, int count, float minSize, float maxSize, float fastness,
                           boolean isRotating, float worldX, float worldY, float worldW, float worldH) {
        this(particleImages, count, minSize, maxSize, fastness, isRotating);
        this.worldMode = true;
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldW = worldW;
        this.worldH = worldH;
        for (Particle p : particles) spawn(p, true);
    }

    public Actor createActor() {
        return new Actor() {
            @Override
            public void act(float delta) {
                update(delta);
            }

            @Override
            public void draw(Batch batch, float parentAlpha) {
                ParticleCreator.this.draw((SpriteBatch) batch);
            }
        };
    }

    private void spawn(Particle p, boolean randomY) {
        if (!enabled) return;
        p.region = new TextureRegion(textures[MathUtils.random(textures.length - 1)]);
        float minX = worldMode ? worldX : 0f, maxX = worldMode ? worldX + worldW : SCREEN_WIDTH;
        float minY = worldMode ? worldY : 0f, maxY = worldMode ? worldY + worldH : SCREEN_HEIGHT;
        p.x = MathUtils.random(minX, maxX);
        p.y = randomY ? MathUtils.random(minY, maxY) : maxY + 20f;
        p.speedX = MathUtils.random(-30f, 30f);
        p.speedY = MathUtils.random(minSpeed, maxSpeed);
        p.size = MathUtils.random(minSize, maxSize);

        // ۲. شفافیت یکسان و کاملاً کامل برای همه پارتیکل‌ها
        p.alpha = FIXED_ALPHA;

        p.rotation = MathUtils.random(0f, 360f);
        p.rotationSpeed = isRotating ? fastness * MathUtils.random(20f, 80f) * (MathUtils.randomBoolean() ? 1 : -1) : 0;
    }

    public void update(float delta) {
        if (!enabled) return;
        float maxX = worldMode ? worldX + worldW : SCREEN_WIDTH;
        float minY = worldMode ? worldY : 0f;
        float minX = worldMode ? worldX : 0f;
        for (Particle p : particles) {
            p.y += p.speedY * delta;
            p.x += p.speedX * delta;
            p.rotation += p.rotationSpeed * delta;

            // ۳. بخش تغییر مقادیر آلفا حذف شد تا همیشه ثابت بماند

            if (p.y < minY - 20 || p.x < minX - 30 || p.x > maxX + 30) {
                spawn(p, false);
            }
        }
    }

    public void draw(SpriteBatch batch) {
        if (!enabled) return;
        for (Particle p : particles) {
            batch.setColor(1f, 1f, 1f, p.alpha);
            batch.draw(p.region, p.x - p.size / 2f, p.y - p.size / 2f, p.size / 2f, p.size / 2f,
                    p.size, p.size, 1f, 1f, p.rotation);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void dispose() {
        for (Texture t : textures) t.dispose();
    }
}