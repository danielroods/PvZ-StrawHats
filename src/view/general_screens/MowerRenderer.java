package view.general_screens;

import com.badlogic.gdx.graphics.Color;

/**
 * Draws the row of lawn mowers, picking the season's mower art and the idle/transition/
 * attack clip that matches each mower's current state, with a procedural mower as the
 * fallback when no PAM is available.
 */
class MowerRenderer {

    private static final java.util.Map<String, String[]> SEASON_LAWN_MOWER_PAM_PATHS = new java.util.HashMap<>();
    static {
        String[] egypt = {
                "768/INITIAL/MOWERS/MOWER_EGYPT/MOWER_EGYPT.PAM",
        };
        String[] cave = {
                "768/FULL/MOWERS/MOWER_ICEAGE/MOWER_ICEAGE.PAM",
        };
        String[] beach = {
                "768/FULL/MOWERS/MOWER_BEACH/MOWER_BEACH.PAM",

        };
        String[] dark = {
                "768/FULL/MOWERS/MOWER_DARK/MOWER_DARK.PAM",
        };
        String[] carnival = {
                "768/FULL/MOWERS/MOWER_CARNIVAL/MOWER_CARNIVAL.PAM",
        };
        String[] future = {
                "768/FULL/MOWERS/MOWER_FUTURE/MOWER_FUTURE.PAM",
        };
        String[] tutorial = {
                "768/INITIAL/MOWERS/MOWER_TUTORIAL/MOWER_TUTORIAL.PAM",
        };

        SEASON_LAWN_MOWER_PAM_PATHS.put("egypt", egypt);
        SEASON_LAWN_MOWER_PAM_PATHS.put("cave", cave);
        SEASON_LAWN_MOWER_PAM_PATHS.put("frostbite caves", cave);
        SEASON_LAWN_MOWER_PAM_PATHS.put("frostbite_caves", cave);
        SEASON_LAWN_MOWER_PAM_PATHS.put("beach", beach);
        SEASON_LAWN_MOWER_PAM_PATHS.put("big wave beach", beach);
        SEASON_LAWN_MOWER_PAM_PATHS.put("big_wave_beach", beach);
        SEASON_LAWN_MOWER_PAM_PATHS.put("darkage", dark);
        SEASON_LAWN_MOWER_PAM_PATHS.put("dark ages", dark);
        SEASON_LAWN_MOWER_PAM_PATHS.put("dark_ages", dark);

        SEASON_LAWN_MOWER_PAM_PATHS.put("beghouled", carnival);
        SEASON_LAWN_MOWER_PAM_PATHS.put("wallnutbowlling", future);
        SEASON_LAWN_MOWER_PAM_PATHS.put("wallnut bowling", future);
        SEASON_LAWN_MOWER_PAM_PATHS.put("wallnut_bowling", future);
        SEASON_LAWN_MOWER_PAM_PATHS.put("zombotany", tutorial);
        SEASON_LAWN_MOWER_PAM_PATHS.put("vase breaker", tutorial);
        SEASON_LAWN_MOWER_PAM_PATHS.put("vasebreaker", tutorial);
        SEASON_LAWN_MOWER_PAM_PATHS.put("izombie", tutorial);
        SEASON_LAWN_MOWER_PAM_PATHS.put("i zombie", tutorial);
    }

    private final GameScreen screen;

    MowerRenderer(GameScreen screen) {
        this.screen = screen;
    }

    void drawMowers(float bw, float bh) {
        if (!screen.areLawnMowersVisible()) return;
        final float time = screen.getRenderTime();
        float boardTileWidth = screen.getBoardTileWidth();
        String seasonKey = screen.getLawnMowerSeasonKey();
        String[] mowerPaths = SEASON_LAWN_MOWER_PAM_PATHS.get(seasonKey);
        model.pitches.LawnMower[] mowers = screen.session.getLawnMowers();

        float manualXOffset = -35f;
        float manualYOffset = 35f;
        float mowerScale = 0.60f;

        for (int r = 0; r < screen.session.getRows(); r++) {
            if (mowers == null || r >= mowers.length) continue;
            model.pitches.LawnMower mower = mowers[r];

            if (mower == null || mower.getState() == model.pitches.LawnMower.MowerState.DEAD) continue;

            float baseX = GameScreen.BOARD_X + (float) mower.getXPosition() * boardTileWidth + manualXOffset;
            float baseY = screen.cellY(r) + 8f + manualYOffset;

            String clipName = getMowerClipName(mower, seasonKey);

            float animTime = (mower.getState() == model.pitches.LawnMower.MowerState.IDLE)
                    ? time
                    : (float) mower.getStateTimer();

            boolean pamDrawn = false;
            if (screen.pamPlayer != null && mowerPaths != null) {
                for (String pamPath : mowerPaths) {
                    if (screen.drawPam(pamPath, clipName, animTime, baseX + 27f, baseY + 10f, mowerScale, false)) {
                        pamDrawn = true;
                        break;
                    }
                }
            }

            if (!pamDrawn) {
                float bob = (mower.getState() == model.pitches.LawnMower.MowerState.IDLE) ? 0f : (float) Math.sin(time * 15f) * 2f;
                float wheelTurn = (mower.getState() == model.pitches.LawnMower.MowerState.IDLE) ? 0f : time * 4.0f;
                drawProceduralLawnMower(baseX, baseY + bob, wheelTurn);
            }
        }
    }

    private String getMowerClipName(model.pitches.LawnMower mower, String seasonKey) {
        if (mower.getState() == model.pitches.LawnMower.MowerState.TRANSITION) {
            return "transition";
        }

        if (mower.getState() == model.pitches.LawnMower.MowerState.ATTACK) {

            if ("bigwavebeach".equalsIgnoreCase(seasonKey) || "beach".equalsIgnoreCase(seasonKey)) {

                double waterStartX = 7.0;


                if (mower.getXPosition() >= waterStartX) {
                    return "attack2";
                }
            }

            return "attack";
        }

        return "idle";
    }

    private void drawProceduralLawnMower(float x, float y, float wheelTime) {
        screen.batch.setColor(Color.valueOf("5E9B3F"));
        screen.batch.draw(screen.whitePixel, x + 8f, y + 20f, 43f, 27f);

        screen.batch.setColor(Color.valueOf("D6D0B4"));
        screen.batch.draw(screen.whitePixel, x + 14f, y + 47f, 31f, 9f);

        screen.batch.setColor(Color.valueOf("4B4B43"));
        screen.batch.draw(screen.whitePixel, x + 2f, y + 11f, 10f, 22f);

        float wheelPulse = 1f + 0.05f * (float) Math.sin(wheelTime * 6f);
        screen.batch.setColor(Color.valueOf("20221E"));
        screen.batch.draw(screen.whitePixel, x + 11f, y + 9f, 11f * wheelPulse, 7f);
        screen.batch.draw(screen.whitePixel, x + 38f, y + 9f, 11f * wheelPulse, 7f);

        screen.batch.setColor(Color.valueOf("C47B2C"));
        screen.batch.draw(screen.whitePixel, x + 25f, y + 26f, 8f, 8f);

        screen.batch.setColor(Color.valueOf("E9E5CE"));
        screen.batch.draw(screen.whitePixel, x + 48f, y + 34f, 16f, 5f);
        screen.batch.setColor(Color.WHITE);
    }
}
