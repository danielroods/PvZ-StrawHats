package view.screens.generals;

import com.badlogic.gdx.graphics.Texture;

class BoardLayout {

    static final float FALLING_SUN_CLICK_HEIGHT = 80f;

    private static final float SOURCE_BOARD_WIDTH = 733f;

    
    private static final float PRE_MATCH_RIGHT_AREA_WIDTH = 240f;

    private final GameScreen screen;

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
        int rows = screen.session.getRows();
        return (float) (GameScreen.BOARD_Y + (rows - 1 - row) * boardTileHeight);
    }

    void updateBoardLayout() {
        float viewW = screen.stage.getViewport().getWorldWidth();
        float viewH = screen.stage.getViewport().getWorldHeight();

        Texture boardTexture = screen.boardTexture;

        if (boardTexture != null) {
            float texW = boardTexture.getWidth();
            float texH = boardTexture.getHeight();

            
            
            float fitScale = Math.max(viewH / texH, viewW / texW);
            bgW = texW * fitScale;
            bgH = texH * fitScale;
            float rightArea = screen.isBeforeMatchPreview()
                    ? PRE_MATCH_RIGHT_AREA_WIDTH : screen.reservedRightAreaWidth();
            bgX = viewW - bgW - rightArea;
            bgY = (viewH - bgH) * 0.5f;

            ChapterLawnArt.Insets insets = screen.boardInsets();
            float boardPixelW = bgW * (1f - insets.left() - insets.right());
            float boardPixelH = bgH * (1f - insets.top() - insets.bottom());
            GameScreen.BOARD_X = bgX + bgW * insets.left();
            GameScreen.BOARD_Y = bgY + bgH * insets.bottom();

            int cols = screen.session == null ? 9 : screen.session.getCols();
            int rows = screen.session == null ? 5 : screen.session.getRows();
            boardTileWidth = boardPixelW / cols;
            boardTileHeight = boardPixelH / rows;
            boardFitScale = boardPixelW / SOURCE_BOARD_WIDTH;
        } else {
            boardTileWidth = GameScreen.TILE_WIDTH;
            boardTileHeight = GameScreen.TILE_HEIGHT;
            GameScreen.BOARD_X = viewW - boardWidth();
            GameScreen.BOARD_Y = (viewH - boardHeight()) * 0.5f;
            bgX = GameScreen.BOARD_X;
            bgY = GameScreen.BOARD_Y;
            bgW = boardWidth();
            bgH = boardHeight();
            boardFitScale = 1f;
        }

        screen.interaction().resizeBoardInput();
    }
}