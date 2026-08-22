package view.screens.generals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import controller.assets.GameAssetManager;
import model.collections.animations.AnimationFactory;
import model.match.main.levels.special_levels.BossLevel;
import model.match.main.levels.special_levels.DeadLineLevel;
import model.match.main.levels.special_levels.IntroductionLevel;
import model.match.main.levels.special_levels.NightOpsLevel;
import model.match.main.levels.special_levels.PlantWhatYouGetLevel;
import model.match.main.levels.special_levels.SaveOurSeedsLevel;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.utils.GameSettings;

/**
 * The layers under the entities: the season background, water/grid tiles,
 * the per-level tints and markers (deadline, boss, night, Save Our Seeds), graves, and
 * the sandstorm haze over the lawn.
 */
class SeasonOverlayRenderer {

    private static final String BEACH_PROTECT_TILE_PATH =
            "assets/images/chapters/beach/gameplay/protect_tile_112x125.png";

    private static final String DEADLINE_FLOWER_PAM =
            "768/INITIAL/EFFECTS/STAR_OBJECTIVE_FLOWER/STAR_OBJECTIVE_FLOWER.PAM";
    private static final float DEADLINE_FLOWER_SCALE = 0.52f;

    private enum DeadlineFlowerPhase { IDLE, FAIL, FAIL_IDLE, WIN, WIN_IDLE }

    private DeadlineFlowerPhase deadlineFlowerPhase = DeadlineFlowerPhase.IDLE;
    private float deadlineFlowerPhaseElapsed = 0f;

    private final GameScreen screen;

    SeasonOverlayRenderer(GameScreen screen) {
        this.screen = screen;
    }

    void drawBackground(float bw, float bh) {
        BoardLayout layout = screen.layout();
        if (screen.boardTexture != null) {
            screen.batch.setColor(Color.WHITE);
            screen.batch.draw(screen.boardTexture, layout.bgX, layout.bgY, layout.bgW, layout.bgH);
        } else {
            screen.batch.setColor(new Color(0.46f, 0.35f, 0.18f, 1f));
            screen.batch.draw(screen.whitePixel, GameScreen.BOARD_X, GameScreen.BOARD_Y, bw, bh);
        }
        screen.batch.setColor(Color.WHITE);
    }

    void drawTiles(float bw, float bh) {
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        for (int r = 0; r < screen.session.getRows(); r++) {
            for (int c = 0; c < screen.session.getCols(); c++) {
                Cell cell = screen.session.getEnvironment().getCell(r, c);
                if (cell == null) continue;
                if (cell.getTile() != null && cell.getTile().type().name().equalsIgnoreCase("WATER")
                        && !screen.isBeach()) {
                    screen.batch.setColor(0.22f, 0.52f, 0.72f, 0.45f);
                    screen.batch.draw(screen.whitePixel, GameScreen.BOARD_X + c * boardTileWidth,
                            screen.cellY(r), boardTileWidth, boardTileHeight);
                    screen.batch.setColor(Color.WHITE);
                }
            }
        }
        if (GameSettings.get().isShowGrid()) {
            screen.batch.setColor(1f, 1f, 1f, 0.18f);
            for (int c = 0; c <= screen.session.getCols(); c++) screen.batch.draw(screen.whitePixel, GameScreen.BOARD_X + c * boardTileWidth, GameScreen.BOARD_Y, 1f, bh);
            for (int r = 0; r <= screen.session.getRows(); r++) screen.batch.draw(screen.whitePixel, GameScreen.BOARD_X, GameScreen.BOARD_Y + r * boardTileHeight, bw, 1f);
            screen.batch.setColor(Color.WHITE);
        }
    }

    void drawSpecialEffects(float delta, float bw, float bh) {
        float boardTileWidth = screen.getBoardTileWidth();
        var level = screen.session.getLevel();
        if (level == null) return;

        if (level instanceof SaveOurSeedsLevel save && save.getSeedPositions() != null) {
            if (screen.isBeach()) {
                for (Position p : save.getSeedPositions().keySet()) drawBeachProtectTile((int) p.y(), (int) p.x());
            } else {
                for (Position p : save.getSeedPositions().keySet()) drawCellBorder((int) p.y(), (int) p.x(), new Color(0.2f, 1f, 0.35f, 0.55f), 4f);
            }
        }
        if (level instanceof DeadLineLevel deadline && deadline.getDeadLine() != null) {
            if (screen.isIceAge()) {
                drawIceAgeDeadlineFlowers(delta, deadline);
            } else {
                int c = (int) deadline.getDeadLine().x();
                screen.batch.setColor(1f, 0.12f, 0.08f, 0.75f);
                screen.batch.draw(screen.whitePixel, GameScreen.BOARD_X + c * boardTileWidth, GameScreen.BOARD_Y, 5f, bh);
                screen.batch.setColor(Color.WHITE);
            }
        }
        if (level instanceof IntroductionLevel) {
            screen.batch.setColor(1f, 0.95f, 0.65f, 0.10f);
            screen.batch.draw(screen.whitePixel, GameScreen.BOARD_X, GameScreen.BOARD_Y, bw, bh);
            screen.batch.setColor(Color.WHITE);
        }
        if (level instanceof PlantWhatYouGetLevel) {
            screen.batch.setColor(0.85f, 0.65f, 0.18f, 0.10f);
            screen.batch.draw(screen.whitePixel, GameScreen.BOARD_X, GameScreen.BOARD_Y, bw, bh);
            screen.batch.setColor(Color.WHITE);
        }
        if (level instanceof BossLevel) {
            screen.batch.setColor(0.35f, 0.05f, 0.05f, 0.12f);
            screen.batch.draw(screen.whitePixel, GameScreen.BOARD_X, GameScreen.BOARD_Y, bw, bh);
            screen.batch.setColor(Color.WHITE);
        }
        if (level instanceof NightOpsLevel || (level.getSeason() != null && level.getSeason().isNight())) {
            screen.batch.setColor(0.04f, 0.06f, 0.14f, 0.26f);
            screen.batch.draw(screen.whitePixel, GameScreen.BOARD_X, GameScreen.BOARD_Y, bw, bh);
            screen.batch.setColor(Color.WHITE);
        }
        if (isEgypt() || isDarkAge()) drawEgyptGraves();
        drawSandStorm(bw, bh);
    }

    private void drawSandStorm(float bw, float bh) {
        if (screen.session == null || !screen.session.isSandStormActive()) return;

        float progress = (float) screen.session.getSandStormProgress();
        float strength = 0.045f + 0.065f * (float) Math.sin(progress * Math.PI);

        screen.batch.setColor(0.78f, 0.61f, 0.34f, strength);
        screen.batch.draw(screen.whitePixel, GameScreen.BOARD_X, GameScreen.BOARD_Y, bw, bh);
        screen.batch.setColor(Color.WHITE);
    }

    private boolean isEgypt() {
        return screen.session.getLevel() != null && screen.session.getLevel().getSeason() != null
                && "Egypt".equalsIgnoreCase(screen.session.getLevel().getSeason().getName());
    }

    private boolean isDarkAge() {
        return screen.session.getLevel() != null && screen.session.getLevel().getSeason() != null
                && "Dark Ages".equalsIgnoreCase(screen.session.getLevel().getSeason().getName());
    }

    private void drawEgyptGraves() {
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        for (int r = 0; r < screen.session.getRows(); r++) {
            for (int c = 0; c < screen.session.getCols(); c++) {
                Cell cell = screen.session.getEnvironment().getCell(r, c);
                if (cell == null || cell.getObstacle() == null) continue;
                if (!"Grave".equalsIgnoreCase(cell.getObstacle().getName())) continue;

                TextureRegion grave = screen.assets().graveRegion();
                if (grave == null) {
                    grave = GameAssetManager.get().getUiRegion("grave");
                }

                float drawX = GameScreen.BOARD_X + c * boardTileWidth + (boardTileWidth - 60f) / 2f;
                float drawY = screen.cellY(r) + (boardTileHeight - 78f) / 2f;

                if (grave != null) {
                    screen.batch.draw(grave, drawX, drawY, 60, 78);
                } else {
                    screen.drawFallback(drawX, drawY, 60, 78, new Color(0.55f, 0.52f, 0.48f, 1f));
                }
            }
        }
    }

    private void drawBeachProtectTile(int row, int col) {
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        if (row < 0 || col < 0 || row >= screen.session.getRows() || col >= screen.session.getCols()) return;
        float x = GameScreen.BOARD_X + col * boardTileWidth;
        float y = screen.cellY(row);
        screen.assets().drawStaticEffectStretched(BEACH_PROTECT_TILE_PATH, x, y, boardTileWidth, boardTileHeight);
    }

    /**
     * Ice Age (Frostbite Caves) dead line: instead of a plain red bar, each
     * tile in the dead-line column shows a STAR_OBJECTIVE_FLOWER effect.
     * They idle normally, then latch to a one-shot "fail"/"win" beat
     * followed by a held "fail_idle"/"win_idle" once the match resolves,
     * and stay that way for the rest of the screen.
     */
    private void drawIceAgeDeadlineFlowers(float delta, DeadLineLevel deadline) {
        if (deadlineFlowerPhase == DeadlineFlowerPhase.IDLE) {
            if (screen.session.isGameOver()) {
                deadlineFlowerPhase = DeadlineFlowerPhase.FAIL;
                deadlineFlowerPhaseElapsed = 0f;
            } else if (screen.session.isGameWon()) {
                deadlineFlowerPhase = DeadlineFlowerPhase.WIN;
                deadlineFlowerPhaseElapsed = 0f;
            }
        }
        deadlineFlowerPhaseElapsed += delta;

        String state;
        switch (deadlineFlowerPhase) {
            case FAIL -> {
                float failDuration = AnimationFactory.clipDurationForPath(DEADLINE_FLOWER_PAM, "fail");
                if (failDuration > 0f && deadlineFlowerPhaseElapsed >= failDuration) {
                    deadlineFlowerPhase = DeadlineFlowerPhase.FAIL_IDLE;
                    deadlineFlowerPhaseElapsed = 0f;
                }
                state = deadlineFlowerPhase == DeadlineFlowerPhase.FAIL ? "fail" : "fail_idle";
            }
            case WIN -> {
                float winDuration = AnimationFactory.clipDurationForPath(DEADLINE_FLOWER_PAM, "win");
                if (winDuration > 0f && deadlineFlowerPhaseElapsed >= winDuration) {
                    deadlineFlowerPhase = DeadlineFlowerPhase.WIN_IDLE;
                    deadlineFlowerPhaseElapsed = 0f;
                }
                state = deadlineFlowerPhase == DeadlineFlowerPhase.WIN ? "win" : "win_idle";
            }
            case FAIL_IDLE -> state = "fail_idle";
            case WIN_IDLE -> state = "win_idle";
            default -> state = "idle";
        }

        float clipTime = deadlineFlowerPhaseElapsed;
        boolean looping = state.equals("idle") || state.equals("fail_idle") || state.equals("win_idle");
        if (looping) {
            float loopDuration = AnimationFactory.clipDurationForPath(DEADLINE_FLOWER_PAM, state);
            if (loopDuration > 0f) clipTime = clipTime % loopDuration;
        }

        float boardTileWidth = screen.getBoardTileWidth();
        int col = (int) deadline.getDeadLine().x();
        for (int row = 0; row < screen.session.getRows(); row++) {
            float x = GameScreen.BOARD_X + col * boardTileWidth - 10f;
            float y = screen.cellY(row) + 40f;
            screen.drawPam(DEADLINE_FLOWER_PAM, state, clipTime, x, y, DEADLINE_FLOWER_SCALE, false);
        }
    }

    private void drawCellBorder(int row, int col, Color color, float thickness) {
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        if (row < 0 || col < 0 || row >= screen.session.getRows() || col >= screen.session.getCols()) return;
        float x = GameScreen.BOARD_X + col * boardTileWidth;
        float y = screen.cellY(row);
        screen.batch.setColor(color);
        screen.batch.draw(screen.whitePixel, x, y, boardTileWidth, thickness);
        screen.batch.draw(screen.whitePixel, x, y + boardTileHeight - thickness, boardTileWidth, thickness);
        screen.batch.draw(screen.whitePixel, x, y, thickness, boardTileHeight);
        screen.batch.draw(screen.whitePixel, x + boardTileWidth - thickness, y, thickness, boardTileHeight);
        screen.batch.setColor(Color.WHITE);
    }
}
