package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import controller.ScreenManager;
import model.App;
import model.collections.animations.AnimationFactory;
import model.match.mini_games.ZombieDashGame;
import model.match.mini_games.ZombieDashGame.SpawnEntity;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.BaseScreen;

import java.util.HashMap;
import java.util.Map;

/**
 * A self-contained 3-lane endless runner for the Console's Zombie Dash game. Like
 * ZombiePackmanGameScreen, it stays independent of GameScreen/GameSession: an
 * auto-scrolling runner with a fixed lane count is a different simulation from PvZ's
 * five-lane lawn-defense model, so it keeps its own tiny loop instead of reusing GameSession.
 */
public class ZombieDashGameScreen extends BaseScreen {
    private static final String PLAYER_PAM =
            "768/FULL/ZOMBIE/FOODFIGHT_ZOMBIE/FOODFIGHT_ZOMBIE.PAM";
    // Verified against assets/pvz-assets/animations.json - each path/clip pair below
    // is confirmed to exist there (see the "id/name" -> "path" -> clips entries).
    private static final String PLANT_PAM = "768/FULL/PLANT/SPIKEWEED/SPIKEWEED.PAM";
    private static final String GRAVE_PAM = "768/INITIAL/GRAVESTONES/TUTORIAL_GRAVESTONE/TUTORIAL_GRAVESTONE.PAM";
    private static final String PEASHOOTER_PAM = "768/INITIAL/PLANT/PEASHOOTER/PEASHOOTER.PAM";
    private static final String SNOWPEA_PAM = "768/INITIAL/PLANT/SNOWPEA/SNOWPEA.PAM";
    private static final String PEA_PROJECTILE_PAM = "768/INITIAL/EFFECTS/T_PEA_PROJECTILE/T_PEA_PROJECTILE.PAM";
    private static final String SUN_PAM = "768/INITIAL/EFFECTS/SUN/SUN.PAM";
    private static final String COIN_PAM = "768/INITIAL/EFFECTS/COIN_GOLD/COIN_GOLD.PAM";
    private static final String BRAIN_PAM = "768/FULL/ZOMBIE/POWER_BRAIN_PROJECTILE/POWER_BRAIN_PROJECTILE.PAM";
    private static final String LIFE_TEXTURE = "assets/images/ui/packman/timer_deco_bigbrainz.png";

    private static final float VIEW_W = SCREEN_WIDTH;
    private static final float VIEW_H = SCREEN_HEIGHT;
    private static final float PLAYER_SCALE = 0.55f;
    private static final float OBSTACLE_SCALE = 0.50f;
    private static final float PEA_SCALE = 0.42f;
    // Pickup PAM canvases vary a lot in native size (sun 200px, coin 45px, brain 390px),
    // so each needs its own scale to end up looking like a consistently-sized icon on screen.
    private static final float SUN_SCALE = 0.35f;
    private static final float COIN_SCALE = 0.40f;
    private static final float BRAIN_SCALE = 0.18f;
    private static final float LANE_WIDTH = 190f;
    private static final float PLAYER_SCREEN_X = 190f;
    private static final float PIXELS_PER_DISTANCE = 42f;

    // Scrolling checkerboard lawn (classic PvZ two-tone grass), tied to the same
    // distance/PIXELS_PER_DISTANCE math as the entities so it scrolls in lockstep with them.
    private static final float TILE_WORLD_WIDTH = 1.5f;
    private static final Color LANE_LIGHT = new Color(0.57f, 0.80f, 0.32f, 1f);
    private static final Color LANE_DARK = new Color(0.47f, 0.70f, 0.27f, 1f);
    private static final Color ICE_TINT = new Color(0.65f, 0.85f, 1f, 1f);

    private final Map<String, ClipRef> clipCache = new HashMap<>();
    private ZombieDashGame game;
    private OrthographicCamera camera;
    private TextureBank textureBank;
    private PamPlayer pamPlayer;
    private Texture lifeTexture;
    private TextureRegion whitePixel;
    private BitmapFont font;
    private InputAdapter keyboard;
    private float worldTime;
    private float laneY;
    private boolean started;

    @Override public void initParticles() { }

    @Override
    public void show() {
        super.show();
        camera = new OrthographicCamera(VIEW_W, VIEW_H);
        camera.position.set(VIEW_W / 2f, VIEW_H / 2f, 0f);
        camera.update();

        game = new ZombieDashGame();
        laneY = laneCenterY(game.getLane());

        font = new BitmapFont();
        font.getData().setScale(1.05f);
        lifeTexture = loadTexture(LIFE_TEXTURE);
        whitePixel = createWhitePixel();

        try {
            textureBank = new TextureBank("atlases", Gdx.files.internal("assets/pvz-assets"));
            pamPlayer = new PamPlayer(textureBank, Gdx.files.internal("assets/pvz-assets"));
            preload();
        } catch (Throwable t) {
            textureBank = null;
            pamPlayer = null;
            if (model.utils.GameSettings.get().isDebugMode())
                Gdx.app.error("ZOMBIE_DASH_PAM", "PAM initialization failed", t);
        }

        keyboard = new InputAdapter() {
            @Override public boolean keyDown(int keycode) {
                if (!started) {
                    if (keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER) { started = true; return true; }
                    if (keycode == Input.Keys.ESCAPE) { exitToConsole(); return true; }
                    return false;
                }
                if (keycode == Input.Keys.UP) game.moveUp();
                else if (keycode == Input.Keys.DOWN) game.moveDown();
                else if (keycode == Input.Keys.P) togglePause();
                else if (keycode == Input.Keys.R) restart();
                else if (keycode == Input.Keys.ESCAPE) exitToConsole();
                else return false;
                return true;
            }
        };
        multiplexer.addProcessor(keyboard);
        AudioManager.get().playMusic(AudioEnum.MINI_GAME_MUSIC, true);
    }

    private void preload() {
        for (String path : new String[]{PLAYER_PAM, PLANT_PAM, GRAVE_PAM, PEASHOOTER_PAM, SNOWPEA_PAM,
                PEA_PROJECTILE_PAM, SUN_PAM, COIN_PAM, BRAIN_PAM}) {
            try { pamPlayer.loadAsync(normalize(path), null); } catch (Throwable ignored) { }
        }
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 0.05f);
        worldTime += delta;
        if (textureBank != null) try { textureBank.update(); } catch (Throwable ignored) { }
        if (started && !game.isPaused() && !game.isGameOver()) game.update(delta);
        updateLaneY(delta);
        drawWorld(delta);
        drawHud();
        stage.act(delta);
        stage.draw();
    }

    private void updateLaneY(float delta) {
        float target = laneCenterY(game.getLane());
        float smoothing = Math.min(1f, delta * 14f);
        laneY += (target - laneY) * smoothing;
    }

    private float laneCenterY(int lane) {
        float middle = VIEW_H / 2f;
        return middle + (1 - lane) * LANE_WIDTH;
    }

    private void drawWorld(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.06f, 1f);
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawLanes();
        if (started) {
            drawEntities();
            drawPlayer();
        } else {
            drawPlayerIdle();
        }
        batch.end();
    }

    /** Scrolling checkerboard lawn: tile positions use the exact same
     *  (worldX - distanceTraveled) * PIXELS_PER_DISTANCE projection as drawEntities(), so the
     *  ground visibly scrolls in lockstep with the obstacles/pickups instead of sitting still. */
    private void drawLanes() {
        float playerWorldX = game.getDistanceTraveled();
        float tilePx = TILE_WORLD_WIDTH * PIXELS_PER_DISTANCE;
        int startCol = (int) Math.floor((playerWorldX - PLAYER_SCREEN_X / PIXELS_PER_DISTANCE) / TILE_WORLD_WIDTH) - 1;
        int endCol = (int) Math.ceil((playerWorldX + (VIEW_W - PLAYER_SCREEN_X) / PIXELS_PER_DISTANCE) / TILE_WORLD_WIDTH) + 1;
        for (int lane = 0; lane < ZombieDashGame.LANE_COUNT; lane++) {
            float laneBottom = laneCenterY(lane) - LANE_WIDTH / 2f;
            for (int col = startCol; col <= endCol; col++) {
                float worldX = col * TILE_WORLD_WIDTH;
                float screenX = PLAYER_SCREEN_X + (worldX - playerWorldX) * PIXELS_PER_DISTANCE;
                batch.setColor(((col + lane) & 1) == 0 ? LANE_LIGHT : LANE_DARK);
                batch.draw(whitePixel, screenX, laneBottom, tilePx + 1f, LANE_WIDTH);
            }
        }
        batch.setColor(0f, 0f, 0f, 0.35f);
        for (int lane = 0; lane <= ZombieDashGame.LANE_COUNT; lane++) {
            float y = laneCenterY(0) + LANE_WIDTH / 2f - lane * LANE_WIDTH;
            batch.draw(whitePixel, 0, y - 1.5f, VIEW_W, 3f);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawEntities() {
        float playerWorldX = game.getDistanceTraveled();
        for (SpawnEntity e : game.getEntities()) {
            if (e.collected || e.hit) continue;
            float screenX = PLAYER_SCREEN_X + (e.distance - playerWorldX) * PIXELS_PER_DISTANCE;
            if (screenX < -80 || screenX > VIEW_W + 80) continue;
            float screenY = laneCenterY(e.lane);
            drawEntity(e, screenX, screenY);
        }
    }

    private void drawEntity(SpawnEntity e, float screenX, float screenY) {
        switch (e.type) {
            case PLANT -> drawPam(PLANT_PAM, "idle", worldTime, screenX - 28, screenY - 30,
                    OBSTACLE_SCALE, false, null);
            case GRAVE -> drawPam(GRAVE_PAM, "undamaged", worldTime, screenX - 28, screenY - 30,
                    OBSTACLE_SCALE, false, null);
            case PEASHOOTER -> drawShooter(PEASHOOTER_PAM, e, screenX, screenY);
            case SNOWPEA -> drawShooter(SNOWPEA_PAM, e, screenX, screenY);
            case PEA -> {
                if (e.icy) batch.setColor(ICE_TINT);
                drawPam(PEA_PROJECTILE_PAM, "animation", worldTime, screenX - 16, screenY - 16, PEA_SCALE, true, null);
                if (e.icy) batch.setColor(Color.WHITE);
            }
            case SUN -> drawPam(SUN_PAM, "animation", worldTime, screenX - 20, screenY - 20,
                    SUN_SCALE, false, null);
            case COIN -> drawPam(COIN_PAM, "animation", worldTime, screenX - 20, screenY - 20,
                    COIN_SCALE, false, null);
            case BRAIN -> drawPam(BRAIN_PAM, "animation", worldTime, screenX - 20, screenY - 20,
                    BRAIN_SCALE, false, null);
        }
    }

    /** Idle-loops on worldTime like everything else, but its "attack" clip plays a proper
     *  windup-to-shot sequence timed from the moment it actually fired, instead of sampling
     *  whatever random phase worldTime happens to be at. */
    private void drawShooter(String path, SpawnEntity e, float screenX, float screenY) {
        if (e.fireFlashTimer > 0) {
            float sinceFired = ZombieDashGame.SHOOTER_FLASH_TIME - e.fireFlashTimer;
            drawPam(path, "attack", sinceFired, screenX - 28, screenY - 30, OBSTACLE_SCALE, true, null);
        } else {
            drawPam(path, "idle", worldTime, screenX - 28, screenY - 30, OBSTACLE_SCALE, true, null);
        }
    }

    private void drawPlayer() {
        if (game.isGameOver()) {
            float dur = AnimationFactory.clipDurationForPath(normalize(PLAYER_PAM), "die");
            float t = dur > 0 ? Math.min(worldTime % (dur + 2f), dur - 0.03f) : worldTime;
            drawPam(PLAYER_PAM, "die", t, PLAYER_SCREEN_X - 28, laneY - 24, PLAYER_SCALE, true, null);
            return;
        }
        boolean flashHit = game.isInvulnerable() && ((worldTime % 0.2f) < 0.1f);
        if (flashHit) batch.setColor(1f, 0.55f, 0.55f, 1f);
        drawPam(PLAYER_PAM, "walk", worldTime, PLAYER_SCREEN_X - 28, laneY - 24, PLAYER_SCALE, true, null);
        batch.setColor(Color.WHITE);
    }

    private void drawPlayerIdle() {
        drawPam(PLAYER_PAM, "idle", worldTime, PLAYER_SCREEN_X - 28, laneY - 24, PLAYER_SCALE, true, null);
    }

    private void drawHud() {
        batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        batch.begin();
        if (!started) {
            drawStartScreen();
            batch.end();
            return;
        }
        if (lifeTexture != null) {
            for (int i = 0; i < game.getLives(); i++) batch.draw(lifeTexture, 25 + i * 42, SCREEN_HEIGHT - 62, 34, 34);
        }
        font.draw(batch, "LIVES", 25, SCREEN_HEIGHT - 12);
        font.draw(batch, "SCORE  " + game.getScore(), 25, SCREEN_HEIGHT - 82);
        font.draw(batch, "COINS " + game.getCoinsCollected() + "   SUNS " + game.getSunsCollected()
                + "   BRAINS " + game.getBrainsCollected(), 25, SCREEN_HEIGHT - 108);
        font.draw(batch, "UP/DOWN SWITCH LANE   P PAUSE   R RESTART   ESC CONSOLE", 25, 24);

        if (game.isGameOver()) {
            drawCenteredBanner("GAME OVER", "Press R to restart or ESC to return", 2.2f);
        } else if (game.isPaused()) {
            drawCenteredBanner("PAUSED", "Press P to resume, R to restart, ESC to leave", 2.0f);
        }
        batch.end();
    }

    private void drawStartScreen() {
        font.getData().setScale(2.4f);
        GlyphLayout title = new GlyphLayout(font, "ZOMBIE DASH");
        font.draw(batch, title, SCREEN_WIDTH / 2f - title.width / 2f, SCREEN_HEIGHT / 2f + 60);
        font.getData().setScale(1.15f);
        GlyphLayout sub = new GlyphLayout(font, "Run, dodge, collect, survive.");
        font.draw(batch, sub, SCREEN_WIDTH / 2f - sub.width / 2f, SCREEN_HEIGHT / 2f + 15);
        GlyphLayout hint = new GlyphLayout(font, "UP / DOWN to switch lanes  -  Press SPACE to start");
        font.draw(batch, hint, SCREEN_WIDTH / 2f - hint.width / 2f, SCREEN_HEIGHT / 2f - 25);
        GlyphLayout esc = new GlyphLayout(font, "ESC to return to console");
        font.draw(batch, esc, SCREEN_WIDTH / 2f - esc.width / 2f, SCREEN_HEIGHT / 2f - 55);
        font.getData().setScale(1.05f);
    }

    private void drawCenteredBanner(String title, String subtitle, float titleScale) {
        font.getData().setScale(titleScale);
        GlyphLayout l = new GlyphLayout(font, title);
        font.draw(batch, l, SCREEN_WIDTH / 2f - l.width / 2f, SCREEN_HEIGHT / 2f + 25);
        font.getData().setScale(1.05f);
        GlyphLayout sub = new GlyphLayout(font, subtitle);
        font.draw(batch, sub, SCREEN_WIDTH / 2f - sub.width / 2f, SCREEN_HEIGHT / 2f - 20);
    }

    private boolean drawPam(String path, String state, float time, float x, float y,
            float scale, boolean flip, Map<String, Boolean> visibility) {
        if (pamPlayer == null || path == null) return false;
        String p = normalize(path);
        try {
            String clipName = AnimationFactory.resolveClipNameForPath(p, state);
            if (clipName == null) clipName = state;
            ClipRef clip = clipCache.get(p + "#" + clipName);
            if (clip == null) {
                clip = pamPlayer.getClip(p, clipName);
                if (clip != null) clipCache.put(p + "#" + clipName, clip);
            }
            if (clip == null) return false;

            // Same convention the main GameScreen's PlantRenderer uses: PAM clips don't loop on
            // their own, so a state meant to keep animating (idle, walk, attack...) has to have
            // its play time wrapped by the clip's real duration, or it just freezes after one pass.
            float clipDuration = AnimationFactory.clipDurationForPath(p, state);
            float playTime = clipDuration > 0f ? time % clipDuration : time;

            batch.flush();
            com.badlogic.gdx.math.Matrix4 old = batch.getTransformMatrix().cpy();
            float scaleX = flip ? -scale : scale;
            batch.getTransformMatrix().translate(x, y, 0).scale(scaleX, scale, 1);
            batch.setTransformMatrix(batch.getTransformMatrix());
            if (visibility == null) pamPlayer.draw(batch, clip, playTime, 0, 0, false);
            else pamPlayer.draw(batch, clip, playTime, 0, 0, false, visibility);
            batch.flush();
            batch.setTransformMatrix(old);
            return true;
        } catch (Throwable ignored) { return false; }
    }

    private void togglePause() { game.togglePaused(); }

    private String normalize(String path) {
        return path.startsWith("assets/pvz-assets/") ? path.substring("assets/pvz-assets/".length()) : path;
    }

    private Texture loadTexture(String path) {
        if (path != null && Gdx.files.internal(path).exists()) {
            Texture t = new Texture(Gdx.files.internal(path));
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return t;
        }
        return null;
    }

    private TextureRegion createWhitePixel() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return new TextureRegion(t);
    }

    private void restart() {
        game = new ZombieDashGame();
        laneY = laneCenterY(game.getLane());
        clipCache.clear();
        preload();
        started = true;
    }

    private void exitToConsole() {
        App.currentMenu = new controller.ui_menus.ConsoleMenu();
        ScreenManager.forceResync();
    }

    @Override public void dispose() {
        if (keyboard != null) multiplexer.removeProcessor(keyboard);
        if (font != null) font.dispose();
        if (lifeTexture != null) lifeTexture.dispose();
        if (whitePixel != null) whitePixel.getTexture().dispose();
        if (textureBank != null) try { textureBank.dispose(); } catch (Throwable ignored) { }
        super.dispose();
    }
}
