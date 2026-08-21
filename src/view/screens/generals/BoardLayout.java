package view.screens.generals;

import com.badlogic.gdx.graphics.Texture;

class BoardLayout {

    static final float FALLING_SUN_CLICK_HEIGHT = 80f;

    private static final float BOARD_INSET_LEFT_FRAC = 260f / 1024f;
    private static final float BOARD_INSET_RIGHT_FRAC = (1024f - 993f) / 1024f;
    private static final float BOARD_INSET_TOP_FRAC = 192f / 768f;
    private static final float BOARD_INSET_BOTTOM_FRAC = (768f - 686f) / 768f;

    private final GameScreen screen;

    float sideLeftX;
    float sideLeftW;
    float sideLeftH;
    float sideRightX;
    float sideRightW;
    float sideRightH;

    private float boardTileWidth = GameScreen.TILE_WIDTH;
    private float boardTileHeight = GameScreen.TILE_HEIGHT;
    float bgX;
    float bgY;
    float bgW;
    float bgH;
    private float boardFitScale = 1f;

    BoardLayout(GameScreen screen) {
        this.screen = screen;
    }

    float boardTileWidth() {
        return boardTileWidth;
    }

    float boardTileHeight() {
        return boardTileHeight;
    }

    float boardFitScale() {
        return boardFitScale;
    }

    float boardWidth() {
        return (screen.session == null ? 9 : screen.session.getCols()) * boardTileWidth;
    }

    float boardHeight() {
        return (screen.session == null ? 5 : screen.session.getRows()) * boardTileHeight;
    }

    float cellY(double row) {
        return (float) (GameScreen.BOARD_Y + (screen.session.getRows() - 1 - row) * boardTileHeight);
    }

    void updateBoardLayout() {
        float viewW = screen.stage.getViewport().getWorldWidth();
        float viewH = screen.stage.getViewport().getWorldHeight();

        Texture boardTexture = screen.boardTexture;
        Texture sideTextureLeft = screen.assets().sideTextureLeft();
        Texture sideTextureRight = screen.assets().sideTextureRight();

        if (boardTexture != null) {
            float texW = boardTexture.getWidth();
            float texH = boardTexture.getHeight();
            float fitScale = Math.min(viewW / texW, viewH / texH);
            boardFitScale = fitScale;
            bgW = texW * fitScale;
            bgH = texH * fitScale;
            bgX = (viewW - bgW) / 2f;
            bgY = (viewH - bgH) / 2f;

            GameScreen.BOARD_X = bgX + bgW * BOARD_INSET_LEFT_FRAC;
            GameScreen.BOARD_Y = bgY + bgH * BOARD_INSET_BOTTOM_FRAC;
            float boardPixelW = bgW * (1f - BOARD_INSET_LEFT_FRAC - BOARD_INSET_RIGHT_FRAC);
            float boardPixelH = bgH * (1f - BOARD_INSET_TOP_FRAC - BOARD_INSET_BOTTOM_FRAC);
            int cols = screen.session == null ? 9 : screen.session.getCols();
            int rows = screen.session == null ? 5 : screen.session.getRows();
            boardTileWidth = boardPixelW / cols;
            boardTileHeight = boardPixelH / rows;

            if (sideTextureLeft != null) {
                sideLeftW = sideTextureLeft.getWidth() * fitScale;
                sideLeftH = sideTextureLeft.getHeight() * fitScale;
                sideLeftX = bgX - sideLeftW;
            }
            if (sideTextureRight != null) {
                sideRightW = sideTextureRight.getWidth() * fitScale;
                sideRightH = sideTextureRight.getHeight() * fitScale;
                sideRightX = bgX + bgW;
            }
        } else {
            bgX = GameScreen.BOARD_X;
            bgY = GameScreen.BOARD_Y;
            bgW = boardWidth();
            bgH = boardHeight();
            boardTileWidth = GameScreen.TILE_WIDTH;
            boardTileHeight = GameScreen.TILE_HEIGHT;
            boardFitScale = 1f;
        }

        screen.interaction().resizeBoardInput();
    }
}
