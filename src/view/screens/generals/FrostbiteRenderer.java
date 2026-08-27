package view.screens.generals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;

import model.collections.animations.AnimationFactory;
import model.collections.zombie.Zombie;
import model.match.main.season.travellog.cave.IceWind;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.pitches.TileType;
import model.pitches.obstacles.IceBlock;

import java.util.HashMap;
import java.util.Map;

class FrostbiteRenderer {

    static final float PLANT_ICE_ALPHA_1 = 0.6f;
    static final float PLANT_ICE_ALPHA_2 = 0.55f;

    private static final float SLIDER_TILE_ART_SCALE = 0.43f;
    private static final float ICE_BLOCK_ART_SCALE = 1.8f;
    private static final float ICE_BLOCK_OFFSET_X = -50f;
    private static final float ICE_BLOCK_OFFSET_Y = 30f;

    // Tile slider PAM animations (replace the old static up/down arrow textures).
    // Scale/offset follow the same "anchor art inside its tile" pattern as the
    // scorched-earth tile effect (see EffectRenderer.SCORCHED_TILE_SCALE/OFFSET_*).
    private static final String TILESLIDER_DOWN_PAM =
            "768/FULL/EFFECTS/TILESLIDER_ICEAGE_DOWN/TILESLIDER_ICEAGE_DOWN.PAM";
    private static final String TILESLIDER_UP_PAM =
            "768/FULL/EFFECTS/TILESLIDER_ICEAGE_UP/TILESLIDER_ICEAGE_UP.PAM";
    private static final String TILESLIDER_STATE_IDLE = "idle";
    private static final String TILESLIDER_STATE_ACTIVE_START = "active_start";
    private static final String TILESLIDER_STATE_ACTIVE_END = "active_end";
    private static final float TILESLIDER_SCALE = 0.65f;
    private static final float TILESLIDER_OFFSET_X = 0.45f;
    private static final float TILESLIDER_OFFSET_Y = 0.50f;


    private static final float PLANT_ICE_SCALE_1 = 0.85f;
    private static final float PLANT_ICE_OFFSET_X_1 = -6f;
    private static final float PLANT_ICE_OFFSET_Y_1 = -22f;
    private static final float PLANT_ICE_SCALE_2 = 0.76f;
    private static final float PLANT_ICE_OFFSET_X_2 = -5f;
    private static final float PLANT_ICE_OFFSET_Y_2 = -7f;
    private static final float PLANT_ICE_SCALE_3 = 0.67f;
    private static final float PLANT_ICE_OFFSET_X_3 = -3f;
    private static final float PLANT_ICE_OFFSET_Y_3 = 5f;

    private final GameScreen screen;

    /** Tracks each slider tile's idle -> active_start -> active_end -> idle cycle,
     *  keyed by row*cols+col so it survives across frames. */
    private final Map<Integer, SliderTileEffect> sliderTileEffects = new HashMap<>();

    /** One slider tile's current PAM phase and how long it has been in that phase. */
    private static final class SliderTileEffect {
        String phase = TILESLIDER_STATE_IDLE;
        float phaseTime;
        boolean zombiePresentLastFrame;
    }

    FrostbiteRenderer(GameScreen screen) {
        this.screen = screen;
    }

    boolean isFrostbite() {
        return screen.session != null && screen.session.getLevel() != null
                && screen.session.getLevel().getSeason() != null
                && "Frostbite Caves".equalsIgnoreCase(screen.session.getLevel().getSeason().getName());
    }

    void drawFrostbiteTileArt(float delta) {
        if (!isFrostbite()) return;
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        int cols = Math.max(1, screen.session.getCols());
        GameScreenAssets assets = screen.assets();

        java.util.Set<Integer> stillSlippery = new java.util.HashSet<>();

        for (int r = 0; r < screen.session.getRows(); r++) {
            for (int c = 0; c < screen.session.getCols(); c++) {
                Cell cell = screen.session.getEnvironment().getCell(r, c);
                if (cell == null || cell.getTile() == null || cell.getTile().type() != TileType.Slippery) continue;
                float x = GameScreen.BOARD_X + c * boardTileWidth;
                float y = screen.cellY(r);
                boolean up = cell.getTile().slipperyDirection() == model.pitches.obstacles.SlipperyDirection.UP;

                int key = r * cols + c;
                stillSlippery.add(key);
                drawSliderTile(key, r, c, x, y, boardTileWidth, boardTileHeight, up, delta, assets);
            }
        }

        // A tile can stop being slippery mid-match (obstacle destroyed, etc.) - drop
        // its tracked phase so a stale entry doesn't linger in the map forever.
        sliderTileEffects.keySet().removeIf(key -> !stillSlippery.contains(key));
    }

    private void drawSliderTile(int key, int row, int col, float x, float y,
                                float boardTileWidth, float boardTileHeight,
                                boolean up, float delta, GameScreenAssets assets) {
        String pamPath = up ? TILESLIDER_UP_PAM : TILESLIDER_DOWN_PAM;

        SliderTileEffect effect = sliderTileEffects.computeIfAbsent(key, k -> new SliderTileEffect());
        boolean zombiePresent = isZombieBeingThrown(row, col);
        advanceSliderPhase(effect, zombiePresent, pamPath, delta);

        float drawX = x + boardTileWidth * TILESLIDER_OFFSET_X;
        float drawY = y + boardTileHeight * TILESLIDER_OFFSET_Y;

        boolean drawn = screen.drawPam(pamPath, effect.phase, effect.phaseTime,
                drawX, drawY, TILESLIDER_SCALE, false);
        if (!drawn) {
            drawSliderTileFallback(assets, x, y, boardTileWidth, boardTileHeight, up);
        }
    }

    /**
     * Idle by default. When a zombie is currently riding/being thrown by this tile,
     * plays "active_start" once, then holds on "active_end" for as long as the
     * zombie is still there, then returns to "idle" the moment no zombie is present -
     * matching the requested intro/outro behaviour without a separate loop state.
     */
    private void advanceSliderPhase(SliderTileEffect effect, boolean zombiePresent, String pamPath, float delta) {
        effect.phaseTime += delta;

        if (!zombiePresent) {
            if (!TILESLIDER_STATE_IDLE.equals(effect.phase)) {
                effect.phase = TILESLIDER_STATE_IDLE;
                effect.phaseTime = 0f;
            } else {
                float idleDuration = AnimationFactory.clipDurationForPath(pamPath, TILESLIDER_STATE_IDLE);
                if (idleDuration > 0f) effect.phaseTime %= idleDuration;
            }
            effect.zombiePresentLastFrame = false;
            return;
        }

        boolean justArrived = !effect.zombiePresentLastFrame;
        if (justArrived) {
            effect.phase = TILESLIDER_STATE_ACTIVE_START;
            effect.phaseTime = 0f;
        } else if (TILESLIDER_STATE_ACTIVE_START.equals(effect.phase)) {
            float startDuration = AnimationFactory.clipDurationForPath(pamPath, TILESLIDER_STATE_ACTIVE_START);
            if (startDuration <= 0f) startDuration = 0.35f;
            if (effect.phaseTime >= startDuration) {
                effect.phase = TILESLIDER_STATE_ACTIVE_END;
                effect.phaseTime = 0f;
            }
        } else if (TILESLIDER_STATE_ACTIVE_END.equals(effect.phase)) {
            float endDuration = AnimationFactory.clipDurationForPath(pamPath, TILESLIDER_STATE_ACTIVE_END);
            if (endDuration > 0f) effect.phaseTime %= endDuration;
        }
        effect.zombiePresentLastFrame = true;
    }

    /** True while any zombie occupies (is being carried across) this slider tile's cell. */
    private boolean isZombieBeingThrown(int row, int col) {
        for (Zombie zombie : screen.session.getZombies()) {
            if (zombie == null || !zombie.isAlive()) continue;
            Position pos = zombie.getPosition();
            if (pos == null) continue;
            int zRow = (int) Math.round(pos.y());
            int zCol = (int) Math.floor(pos.x());
            if (zRow == row && zCol == col) return true;
        }
        return false;
    }

    private void drawSliderTileFallback(GameScreenAssets assets, float x, float y,
                                        float boardTileWidth, float boardTileHeight, boolean up) {
        Texture background = up ? assets.sliderUpBackgroundTexture() : assets.sliderDownBackgroundTexture();
        if (background == null) background = assets.sliderBackgroundTexture();

        if (background != null) {
            screen.batch.setColor(Color.WHITE);
            screen.batch.draw(background, x, y, boardTileWidth, boardTileHeight);
        } else {
            screen.batch.setColor(0.72f, 0.86f, 0.96f, 0.20f);
            screen.batch.draw(screen.whitePixel, x, y, boardTileWidth, boardTileHeight);
        }

        Texture arrow = up ? assets.sliderUpTexture() : assets.sliderDownTexture();
        if (arrow != null) {
            screen.batch.setColor(Color.WHITE);
            float drawW = boardTileWidth * SLIDER_TILE_ART_SCALE - 10;
            float drawH = boardTileHeight * SLIDER_TILE_ART_SCALE + 10;
            float drawX = x + (boardTileWidth - drawW) * 0.5f;
            float drawY = y + (boardTileHeight - drawH) * 0.5f;
            screen.batch.draw(arrow, drawX, drawY, drawW, drawH);
        } else {
            drawSlipperyArrowFallback(x, y, up);
        }
    }

    private void drawSlipperyArrowFallback(float x, float y, boolean up) {
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        screen.batch.setColor(0.76f, 0.90f, 1f, 0.85f);
        float centerX = x + boardTileWidth * 0.5f;
        float centerY = y + boardTileHeight * 0.5f;
        float arrow = boardTileWidth * 0.22f;
        screen.batch.draw(screen.whitePixel, centerX - 3f, centerY - arrow, 6f, arrow * 2f);
        screen.batch.draw(screen.whitePixel, centerX - 12f, centerY + (up ? arrow : -arrow), 24f, 6f);
        screen.batch.setColor(Color.WHITE);
    }

    void drawFrostbiteIceBlocks(float delta) {
        if (!isFrostbite()) return;
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        float boardFitScale = screen.boardFitScale();
        float pulse = 0.94f + 0.03f * (float) Math.sin((screen.getRenderTime() + delta) * 2.0f);
        for (int r = 0; r < screen.session.getRows(); r++) {
            for (int c = 0; c < screen.session.getCols(); c++) {
                Cell cell = screen.session.getEnvironment().getCell(r, c);
                if (cell == null || !(cell.getObstacle() instanceof IceBlock iceBlock)) continue;
                float x = GameScreen.BOARD_X + c * boardTileWidth;
                float y = screen.cellY(r);
                boolean isZombie = iceBlock.getFrozenZombie() != null;
                Texture iceBlockTexture = isZombie ? screen.assets().zombieIceBlockTexture() : plantIceBlockTextureFor(iceBlock);
                int plantLevel = isZombie ? 3 : plantIceLevelFor(iceBlock);
                if (iceBlockTexture != null) {
                    float drawW = isZombie ? boardTileWidth * ICE_BLOCK_ART_SCALE
                            : iceBlockTexture.getWidth() * boardFitScale * plantIceScaleFor(plantLevel);
                    float drawH = isZombie ? boardTileHeight * ICE_BLOCK_ART_SCALE
                            : iceBlockTexture.getHeight() * boardFitScale * plantIceScaleFor(plantLevel);
                    float drawX = isZombie ? x + (boardTileWidth - drawW) * 0.5f + ICE_BLOCK_OFFSET_X
                            : x + (boardTileWidth - drawW) * 0.5f + plantIceOffsetXFor(plantLevel);
                    float drawY = isZombie ? y + (boardTileHeight - drawH) * 0.5f + ICE_BLOCK_OFFSET_Y
                            : y + (boardTileHeight - drawH) * 0.5f + plantIceOffsetYFor(plantLevel);
                    screen.batch.setColor(1f, 1f, 1f, pulse);
                    screen.batch.draw(iceBlockTexture, drawX, drawY, drawW, drawH);
                    screen.batch.setColor(Color.WHITE);
                } else {
                    screen.batch.setColor(0.70f, 0.90f, 1f, 0.34f);
                    screen.batch.draw(screen.whitePixel, x + 4f, y + 4f, boardTileWidth - 8f, boardTileHeight - 8f);
                    screen.batch.setColor(0.88f, 0.98f, 1f, 0.34f);
                    screen.batch.draw(screen.whitePixel, x + boardTileWidth * 0.16f, y + boardTileHeight * 0.16f, 5f, boardTileHeight * 0.65f);
                    screen.batch.draw(screen.whitePixel, x + boardTileWidth * 0.58f, y + boardTileHeight * 0.25f, 4f, boardTileHeight * 0.48f);
                    screen.batch.setColor(Color.WHITE);
                }
            }
        }
    }

    private Texture plantIceBlockTextureFor(IceBlock iceBlock) {
        GameScreenAssets assets = screen.assets();
        double fraction = IceBlock.BASE_HP > 0 ? iceBlock.getHp() / (double) IceBlock.BASE_HP : 1.0;
        Texture preferred;
        if (fraction > 2.0 / 3.0) {
            preferred = assets.plantIceBlockTexture3();
        } else if (fraction > 1.0 / 3.0) {
            preferred = assets.plantIceBlockTexture2();
        } else {
            preferred = assets.plantIceBlockTexture1();
        }
        if (preferred != null) return preferred;
        if (assets.plantIceBlockTexture3() != null) return assets.plantIceBlockTexture3();
        if (assets.plantIceBlockTexture2() != null) return assets.plantIceBlockTexture2();
        return assets.plantIceBlockTexture1();
    }

    private int plantIceLevelFor(IceBlock iceBlock) {
        double fraction = IceBlock.BASE_HP > 0 ? iceBlock.getHp() / (double) IceBlock.BASE_HP : 1.0;
        if (fraction > 2.0 / 3.0) return 3;
        if (fraction > 1.0 / 3.0) return 2;
        return 1;
    }

    static float plantIceScaleFor(int level) {
        return switch (level) {
            case 1 -> PLANT_ICE_SCALE_1;
            case 2 -> PLANT_ICE_SCALE_2;
            default -> PLANT_ICE_SCALE_3;
        };
    }

    static float plantIceOffsetXFor(int level) {
        return switch (level) {
            case 1 -> PLANT_ICE_OFFSET_X_1;
            case 2 -> PLANT_ICE_OFFSET_X_2;
            default -> PLANT_ICE_OFFSET_X_3;
        };
    }

    static float plantIceOffsetYFor(int level) {
        return switch (level) {
            case 1 -> PLANT_ICE_OFFSET_Y_1;
            case 2 -> PLANT_ICE_OFFSET_Y_2;
            default -> PLANT_ICE_OFFSET_Y_3;
        };
    }

    void drawSeasonForegroundEffects(float delta, float bw, float bh) {
        if (!isFrostbite() || !IceWind.isActive(screen.session)) return;
        drawIceWindOverlay((float) IceWind.animationTime(screen.session));
    }

    private void drawIceWindOverlay(float elapsed) {
        if (elapsed < 0) return;

        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();

        for (Integer row : IceWind.activeRows(screen.session)) {
            if (row == null || row < 0 || row >= screen.session.getRows()) continue;

            float y = screen.cellY(row);
            float x = GameScreen.BOARD_X;
            float width = screen.boardWidth();
            float centerX = x + width * 0.5f - boardTileWidth * 0.35f - 130f;
            float centerY = y + boardTileHeight * 0.5f + 10f;
            float scale = Math.max(0.45f, boardTileWidth / 118f) * 0.55f;

            if (!screen.drawPam(IceWind.PAM_PATH_PLACEHOLDER, IceWind.PAM_CLIP,
                    (float)(elapsed % IceWind.EVENT_DURATION_SECONDS),
                    centerX, centerY, scale, false)) {
                float strength = 0.18f + 0.10f * (float)Math.sin(elapsed * 2.0f);
                screen.batch.setColor(0.78f, 0.92f, 1f, strength);
                screen.batch.draw(screen.whitePixel, GameScreen.BOARD_X, y, screen.boardWidth(), boardTileHeight);
                for (int streak = 0; streak < 16; streak++) {
                    float travel = ((float)(elapsed * 0.55f + streak * 0.137f) % 1.0f) * screen.boardWidth();
                    float streakY = y + boardTileHeight * (0.12f + (streak % 6) * 0.15f);
                    screen.batch.setColor(0.90f, 0.98f, 1f, 0.12f + 0.018f * (streak % 4));
                    screen.batch.draw(screen.whitePixel, GameScreen.BOARD_X + travel, streakY, boardTileWidth * 0.9f, 3f);
                }
                screen.batch.setColor(Color.WHITE);
            }
        }
    }
}