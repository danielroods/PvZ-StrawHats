package view.screens.generals;

import com.badlogic.gdx.graphics.Color;

import model.collections.animations.AnimationFactory;
import model.match.boss.ZombossChapter;
import model.match.boss.ZombossFight;
import model.match.boss.ZombossPhase;
import model.match.boss.ZombossRowEffect;
import model.match.boss.ZombossShark;
import model.match.boss.ZombossSkyStrike;
import model.match.boss.behavior.BeachZombossBehavior;
import model.match.boss.behavior.IceAgeZombossBehavior;
import model.match_mechanisms.vector.Position;

class ZombossRenderer {

    private static final float BOSS_SCALE = 0.52f;
    private static final float WORLD_SCALE = 0.52f;
    private static final float TILE_FX_SCALE = 0.35f;
    private static final float ZOMBIE_ANCHOR_X = -10f;
    private static final float ZOMBIE_ANCHOR_Y = 40f;
    private static final float SKY_FALL_HEIGHT_TILES = 5.5f;

    private static final float NPC_SCALE = 0.42f;
    private static final float NPC_MARGIN_X = 190f;
    private static final float NPC_MARGIN_Y = 40f;

    private static final String GLACIER_TOP =
            "768/FULL/EFFECTS/ZOMBOSS_GLACIER_TOP/ZOMBOSS_GLACIER_TOP.PAM";
    private static final String GLACIER_MIDDLE =
            "768/FULL/EFFECTS/ZOMBOSS_GLACIER_MIDDLE/ZOMBOSS_GLACIER_MIDDLE.PAM";
    private static final String GLACIER_BOTTOM =
            "768/FULL/EFFECTS/ZOMBOSS_GLACIER_BOTTOM/ZOMBOSS_GLACIER_BOTTOM.PAM";
    private static final String GLACIER_FOG =
            "768/FULL/EFFECTS/ZOMBOSS_GLACIER_FOGGING/ZOMBOSS_GLACIER_FOGGING.PAM";
    private static final String GLACIER_CLIP = "animation";

    private final GameScreen screen;

    private float effectTime;

    ZombossRenderer(GameScreen screen) {
        this.screen = screen;
    }

    private ZombossFight fight() {
        return screen.session == null ? null : screen.session.getZombossFight();
    }

    void advance(float delta) {
        effectTime += delta;
    }

    void preload() {
        ZombossFight fight = fight();
        if (fight == null) return;
        screen.preloadPam(fight.getChapter().getBossPam(), ZombossChapter.NPC_PAM);
        switch (fight.getChapter()) {
            case ICE_AGE -> screen.preloadPam(GLACIER_TOP, GLACIER_MIDDLE, GLACIER_BOTTOM,
                    GLACIER_FOG);
            case BEACH -> screen.preloadPam(ZombossShark.PAM,
                    BeachZombossBehavior.TURBINE_WIND_PAM, BeachZombossBehavior.PLANT_PULLED_PAM);
            default -> { }
        }
    }

    void drawBackdrop() {
        if (screen.isBeforeMatchPreview()) return;
        ZombossFight fight = fight();
        if (fight == null || fight.getPhase() == ZombossPhase.SILENCE) return;
        if (fight.getBehavior() instanceof IceAgeZombossBehavior ice) {
            drawGlacier(ice.getBlockedColumnStart());
        }
    }

    void drawBoss() {
        if (screen.isBeforeMatchPreview()) return;
        ZombossFight fight = fight();
        if (fight == null) return;
        String clip = fight.getBossClip();
        if (clip == null) return;

        Position position = fight.getBossPosition();
        float x = bossX(position);
        float y = bossY(position);
        screen.drawPam(fight.getChapter().getBossPam(), clip,
                (float) fight.getBossClipTime(), x, y, BOSS_SCALE, false);
    }

    void drawEffects(float delta) {
        if (screen.isBeforeMatchPreview()) return;
        ZombossFight fight = fight();
        if (fight == null) return;
        advance(delta);
        drawRowEffects(fight);
        if (fight.getBehavior() instanceof BeachZombossBehavior beach) {
            drawTurbine(beach);
            drawPulledPlants(beach);
            drawSharks(beach);
        }
        drawSkyStrikes(fight);
    }

    void drawNpc() {
        if (screen.isBeforeMatchPreview()) return;
        ZombossFight fight = fight();
        if (fight == null) return;
        String clip = fight.getNpcClip();
        if (clip == null) return;
        float viewW = screen.stage.getViewport().getWorldWidth();
        screen.drawPam(ZombossChapter.NPC_PAM, clip, (float) fight.getNpcClipTime(),
                viewW - NPC_MARGIN_X, NPC_MARGIN_Y + 90f, NPC_SCALE, false);
    }

    private void drawGlacier(int sealedColumnStart) {
        int cols = screen.session.getCols();
        int rows = screen.session.getRows();
        float tileW = screen.getBoardTileWidth();
        float tileH = screen.getBoardTileHeight();
        float wallX = GameScreen.BOARD_X + (cols - 0.4f) * tileW;

        screen.drawPam(GLACIER_MIDDLE, GLACIER_CLIP, effectTime,
                wallX, screen.cellY(rows / 2.0) + ZOMBIE_ANCHOR_Y, WORLD_SCALE, false);
        screen.drawPam(GLACIER_TOP, GLACIER_CLIP, effectTime,
                wallX, screen.cellY(0) + tileH * 0.9f, WORLD_SCALE, false);
        screen.drawPam(GLACIER_BOTTOM, GLACIER_CLIP, effectTime,
                wallX, screen.cellY(rows - 1) + tileH * 0.1f, WORLD_SCALE, false);

        for (int row = 0; row < rows; row++) {
            for (int col = sealedColumnStart; col < cols; col++) {
                screen.drawPam(GLACIER_FOG, GLACIER_CLIP, effectTime + row * 0.3f,
                        GameScreen.BOARD_X + (col + 0.5f) * tileW,
                        screen.cellY(row) + tileH * 0.35f, TILE_FX_SCALE, false);
            }
        }
    }

    private void drawRowEffects(ZombossFight fight) {
        float tileW = screen.getBoardTileWidth();
        float tileH = screen.getBoardTileHeight();
        int cols = screen.session.getCols();
        for (ZombossRowEffect effect : fight.getRowEffects()) {
            float time = loopedTime(effect.getPamPath(), effect.getClip(),
                    (float) effect.getElapsed());
            for (int col = 0; col < cols; col++) {
                screen.drawPam(effect.getPamPath(), effect.getClip(), time + col * 0.05f,
                        GameScreen.BOARD_X + (col + 0.45f) * tileW,
                        screen.cellY(effect.getRow()) + tileH * 0.3f, TILE_FX_SCALE, false);
            }
        }
    }

    private void drawTurbine(BeachZombossBehavior beach) {
        int row = beach.getSuctionRow();
        if (row < 0) return;
        float tileW = screen.getBoardTileWidth();
        float tileH = screen.getBoardTileHeight();
        int bossColumn = (int) Math.round(fight().getBossPosition().x());
        float time = loopedTime(BeachZombossBehavior.TURBINE_WIND_PAM, "animation", effectTime);
        for (int col = 0; col < bossColumn; col++) {
            screen.drawPam(BeachZombossBehavior.TURBINE_WIND_PAM, "animation",
                    time + col * 0.07f,
                    GameScreen.BOARD_X + (col + 0.5f) * tileW,
                    screen.cellY(row) + tileH * 0.35f, WORLD_SCALE, false);
        }
    }

    private void drawPulledPlants(BeachZombossBehavior beach) {
        float tileW = screen.getBoardTileWidth();
        float tileH = screen.getBoardTileHeight();
        for (BeachZombossBehavior.PulledPlant pulled : beach.getPulledPlants()) {
            if (!pulled.isOverWater()) continue;
            String clip = pulled.hasArrived() ? "animation2" : "animation";
            float time = pulled.hasArrived()
                    ? (float) pulled.getArrivedElapsed()
                    : loopedTime(BeachZombossBehavior.PLANT_PULLED_PAM, clip, effectTime);
            screen.drawPam(BeachZombossBehavior.PLANT_PULLED_PAM, clip, time,
                    GameScreen.BOARD_X + ((float) pulled.getColumn() + 0.5f) * tileW,
                    screen.cellY(pulled.getRow()) + tileH * 0.35f, WORLD_SCALE, false);
        }
    }

    private void drawSharks(BeachZombossBehavior beach) {
        float tileW = screen.getBoardTileWidth();
        float tileH = screen.getBoardTileHeight();
        for (ZombossShark shark : beach.getSharks()) {
            String clip = shark.getClip();
            if (clip == null) continue;
            screen.drawPam(ZombossShark.PAM, clip, (float) shark.getClipTime(),
                    GameScreen.BOARD_X + ((float) shark.getColumn() + 0.5f) * tileW,
                    screen.cellY(shark.getRow()) + tileH * 0.3f, WORLD_SCALE, false);
        }
    }

    private void drawSkyStrikes(ZombossFight fight) {
        float tileW = screen.getBoardTileWidth();
        float tileH = screen.getBoardTileHeight();
        for (ZombossSkyStrike strike : fight.getSkyStrikes()) {
            String path = strike.getCurrentPath();
            String clip = strike.getCurrentClip();
            if (path == null || clip == null) continue;

            float x = GameScreen.BOARD_X + (strike.getCol() + 0.5f) * tileW;
            float y = screen.cellY(strike.getRow()) + tileH * 0.3f;
            if (strike.getStage() == ZombossSkyStrike.Stage.FALL) {
                float remaining = 1f - (float) strike.getFallProgress();
                y += remaining * SKY_FALL_HEIGHT_TILES * tileH;
            }
            screen.drawPam(path, clip, (float) strike.getCurrentClipTime(), x, y,
                    TILE_FX_SCALE, false);
        }
    }

    private float bossX(Position position) {
        return GameScreen.BOARD_X + (float) position.x() * screen.getBoardTileWidth()
                + ZOMBIE_ANCHOR_X;
    }

    private float bossY(Position position) {
        return screen.cellY(position.y()) + ZOMBIE_ANCHOR_Y;
    }

    private float loopedTime(String path, String clip, float time) {
        float length = AnimationFactory.exactClipDurationForPath(path, clip);
        return length > 0 ? time % length : time;
    }

    void reset() {
        effectTime = 0f;
        screen.batch.setColor(Color.WHITE);
    }
}
