package view.screens.match.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import model.match.main.levels.Level;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;

/**
 * Big Wave Beach gameplay visuals. The tide is authoritative in GameSession;
 * this screen renders its animated waterline, rocks, huge wave, and boardwalk.
 */
public class BigWaveBeachGameScreen extends GameScreen {
    private static final String WATER_UPPER_PAM =
            "768/FULL/BACKGROUNDS/WAVE_UPPERLAYER/WAVE_UPPERLAYER.PAM";
    private static final String BIG_WAVE_PAM =
            "768/FULL/BACKGROUNDS/WAVE_BIG/WAVE_BIG.PAM";

    private static final String ROCKS_PATH = "assets/images/chapters/beach/gameplay/Beach_rocks.png";
    private static final String BOARDWALK_PATH = "assets/images/chapters/beach/gameplay/Beach_boardwalk.png";

    private static final float WATER_SCALE = 0.50f;
    private static final float BIG_WAVE_SCALE = 0.50f;
    private static final float TIDE_MOVE_SECONDS = 0.75f;

    private static final float WATER_TILE_OFFSET = -2.5f;
    private static final float WATER_OFFSET_X = 500f;
    private static final float WATER_OFFSET_Y = -30f;

    private static final float BIG_WAVE_OFFSET_X = 0f;
    private static final float BIG_WAVE_OFFSET_Y = 0f;

    private static final float ROCKS_SCALE = 0.98f;
    private static final float ROCKS_OFFSET_X = 640f;
    private static final float ROCKS_OFFSET_Y = 265f;

    private static final float BOARDWALK_SCALE = 0.935f;
    private static final float BOARDWALK_OFFSET_X = 527f;
    private static final float BOARDWALK_OFFSET_Y = -192f;

    private float visualTideColumns = -1f;
    private float waterAnimationTime = 0f;
    private float bigWaveAnimationTime = 0f;
    private int lastTideColumn = -1;
    private int lastBigWaveIndex = -1;

    private Texture rocksTexture;
    private Texture boardwalkTexture;

    public BigWaveBeachGameScreen() {
        seasonFolder = "beach";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.BEACH_MUSIC, true);
        visualTideColumns = -1f;
        lastTideColumn = -1;
        lastBigWaveIndex = -1;
        waterAnimationTime = 0f;
        bigWaveAnimationTime = 0f;

        // بارگذاری ایمن تصویر صخره‌ها
        if (Gdx.files.internal(ROCKS_PATH).exists()) {
            rocksTexture = new Texture(Gdx.files.internal(ROCKS_PATH));
            rocksTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        // بارگذاری ایمن تصویر پل چوبی
        if (Gdx.files.internal(BOARDWALK_PATH).exists()) {
            boardwalkTexture = new Texture(Gdx.files.internal(BOARDWALK_PATH));
            boardwalkTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    @Override
    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {
        if (session == null || session.getLevel() == null) return;
        Level level = session.getLevel();
        if (level.getSeason() == null || !"Big Wave Beach".equalsIgnoreCase(level.getSeason().getName())) return;

        waterAnimationTime += Math.max(0f, delta);
        updateVisualTide(delta, level.getCurrentTideColumn());

        // ترتیب رندر لایه‌های پس‌زمینه/میان‌زمینه: water < beach rocks < wave
        drawWaterUpperLayer();
        drawBeachRocks();
        drawBigWave(delta);
    }

    @Override
    protected void drawSeasonForegroundEffects(float delta, float bw, float bh) {
        if (session == null || session.getLevel() == null) return;
        Level level = session.getLevel();
        if (level.getSeason() == null || !"Big Wave Beach".equalsIgnoreCase(level.getSeason().getName())) return;

        drawBoardwalk();
    }

    private void updateVisualTide(float delta, int targetColumns) {
        if (visualTideColumns < 0f) {
            visualTideColumns = targetColumns;
            lastTideColumn = targetColumns;
            return;
        }

        if (targetColumns != lastTideColumn) {
            lastTideColumn = targetColumns;
        }

        float alpha = Math.min(1f, Math.max(0f, delta) / TIDE_MOVE_SECONDS);
        visualTideColumns += (targetColumns - visualTideColumns) * alpha;
    }

    /**
     * The blue rectangle used by GameScreen is intentionally disabled for Beach.
     * This PAM is the moving upper water edge and follows the interpolated tide
     * boundary, so its center moves whenever the waterline moves.
     */
    private void drawWaterUpperLayer() {
        if (pamPlayer == null || visualTideColumns <= 0.01f) return;

        float waterEdgeX = BOARD_X + (session.getCols() - visualTideColumns + WATER_TILE_OFFSET) * getBoardTileWidth() + WATER_OFFSET_X;
        float centerY = getBoardCenterY() + WATER_OFFSET_Y;
        float bob = (float) Math.sin(waterAnimationTime * 2.4f) * 3f;

        batch.setColor(Color.WHITE);
        drawPam(WATER_UPPER_PAM, "water", waterAnimationTime,
                waterEdgeX, centerY + bob, WATER_SCALE, true);
        batch.setColor(Color.WHITE);
    }

    /** Render the beach rocks overlay. */
    private void drawBeachRocks() {
        if (rocksTexture == null) return;

        float x = BOARD_X + ROCKS_OFFSET_X;
        float y = getBoardCenterY() + ROCKS_OFFSET_Y;
        float width = rocksTexture.getWidth() * ROCKS_SCALE;
        float height = rocksTexture.getHeight() * ROCKS_SCALE;

        batch.setColor(Color.WHITE);
        batch.draw(rocksTexture, x - width / 2f, y - height / 2f, width, height);
    }

    /** Render the large entrance wave; its clip switches from wave to wave_crash at landfall. */
    private void drawBigWave(float delta) {
        if (pamPlayer == null || !session.isBeachBigWaveActive()) return;

        float progress = (float) session.getBeachBigWaveProgress();
        boolean crash = session.isBeachBigWaveCrash();
        float phaseProgress = crash
                ? Math.min(1f, (progress - 0.72f) / 0.28f)
                : Math.min(1f, progress / 0.72f);

        float waterEdgeX = BOARD_X + (session.getCols() - Math.max(0f, visualTideColumns) + WATER_TILE_OFFSET) * getBoardTileWidth() + WATER_OFFSET_X;
        float startX = getBoardRight() + getBoardTileWidth() * 1.25f;
        float targetX = waterEdgeX;
        float x = startX + (targetX - startX) * smoothStep(phaseProgress);
        if (crash) x = targetX;

        float y = getBoardCenterY() + BIG_WAVE_OFFSET_Y;
        String state = crash ? "wave_crash" : "wave";
        if (session.getBeachBigWaveIndex() != lastBigWaveIndex) {
            lastBigWaveIndex = session.getBeachBigWaveIndex();
            bigWaveAnimationTime = 0f;
        }
        float time = bigWaveAnimationTime += Math.max(0f, delta);

        batch.setColor(Color.WHITE);
        drawPam(BIG_WAVE_PAM, state, time, x + BIG_WAVE_OFFSET_X, y, BIG_WAVE_SCALE, true);
        batch.setColor(Color.WHITE);
    }

    /** Render the boardwalk overlay. */
    private void drawBoardwalk() {
        if (boardwalkTexture == null) return;

        float x = BOARD_X + BOARDWALK_OFFSET_X;
        float y = getBoardCenterY() + BOARDWALK_OFFSET_Y;
        float width = boardwalkTexture.getWidth() * BOARDWALK_SCALE;
        float height = boardwalkTexture.getHeight() * BOARDWALK_SCALE;

        batch.setColor(Color.WHITE);
        batch.draw(boardwalkTexture, x - width / 2f, y - height / 2f, width, height);
    }

    private float smoothStep(float x) {
        x = Math.max(0f, Math.min(1f, x));
        return x * x * (3f - 2f * x);
    }

    @Override
    public void dispose() {
        if (rocksTexture != null) {
            rocksTexture.dispose();
            rocksTexture = null;
        }
        if (boardwalkTexture != null) {
            boardwalkTexture.dispose();
            boardwalkTexture = null;
        }
        super.dispose();
    }
}