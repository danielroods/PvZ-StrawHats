package view.general_screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;

import model.match.main.season.travellog.cave.IceWind;
import model.pitches.Cell;
import model.pitches.TileType;
import model.pitches.obstacles.IceBlock;

class FrostbiteRenderer {

    static final float PLANT_ICE_ALPHA_1 = 0.6f;
    static final float PLANT_ICE_ALPHA_2 = 0.55f;

    private static final float SLIDER_TILE_ART_SCALE = 0.43f;
    private static final float ICE_BLOCK_ART_SCALE = 1.8f;
    private static final float ICE_BLOCK_OFFSET_X = -50f;
    private static final float ICE_BLOCK_OFFSET_Y = 30f;

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

    FrostbiteRenderer(GameScreen screen) {
        this.screen = screen;
    }

    boolean isFrostbite() {
        return screen.session != null && screen.session.getLevel() != null
                && screen.session.getLevel().getSeason() != null
                && "Frostbite Caves".equalsIgnoreCase(screen.session.getLevel().getSeason().getName());
    }

    void drawFrostbiteTileArt() {
        if (!isFrostbite()) return;
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        GameScreenAssets assets = screen.assets();
        for (int r = 0; r < screen.session.getRows(); r++) {
            for (int c = 0; c < screen.session.getCols(); c++) {
                Cell cell = screen.session.getEnvironment().getCell(r, c);
                if (cell == null || cell.getTile() == null || cell.getTile().type() != TileType.Slippery) continue;
                float x = GameScreen.BOARD_X + c * boardTileWidth;
                float y = screen.cellY(r);
                boolean up = cell.getTile().slipperyDirection() == model.pitches.obstacles.SlipperyDirection.UP;

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
