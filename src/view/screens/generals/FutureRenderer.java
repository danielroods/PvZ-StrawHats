package view.screens.generals;

import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.match.main.season.travellog.future.Future;
import model.pitches.Cell;
import model.pitches.TileType;

import java.util.IdentityHashMap;
import java.util.Map;

class FutureRenderer {

    private static final String[] LINKTILE_01_PAMS = {
            "768/INITIAL/BACKGROUNDS/LINKTILE_01/LINKTILE_01.PAM",
            "assets/pvz-assets/768/INITIAL/BACKGROUNDS/LINKTILE_01/LINKTILE_01.PAM",
            "768/FULL/BACKGROUNDS/LINKTILE_01/LINKTILE_01.PAM"
    };
    private static final String[] LINKTILE_02_PAMS = {
            "768/INITIAL/BACKGROUNDS/LINKTILE_02/LINKTILE_02.PAM",
            "assets/pvz-assets/768/INITIAL/BACKGROUNDS/LINKTILE_02/LINKTILE_02.PAM",
            "768/FULL/BACKGROUNDS/LINKTILE_02/LINKTILE_02.PAM"
    };
    private static final String[] LINKTILE_03_PAMS = {
            "768/INITIAL/BACKGROUNDS/LINKTILE_03/LINKTILE_03.PAM",
            "assets/pvz-assets/768/INITIAL/BACKGROUNDS/LINKTILE_03/LINKTILE_03.PAM",
            "768/FULL/BACKGROUNDS/LINKTILE_03/LINKTILE_03.PAM"
    };


    private static final String LINKTILE_STATE = "animation";

    private static final String LINKTILE_PLANTFOOD_STATE = "animation2";

    private static final String LINKTILE_DEATH_STATE = "animation3";

    private static final float DEATH_ANIM_FALLBACK_DURATION = 1.2f;

    // PAM artwork uses its own pixel space. These tiles are 768-era assets, so
    // board pixel sizes must never be passed directly as PAM scale values.
    private static final float LINK_TILE_SCALE = 0.65f;
    private static final float LINK_TILE_OFFSET_X = 0.45f;
    private static final float LINK_TILE_OFFSET_Y = 0.50f;

    private final GameScreen screen;
    private float animTime;


    private final Map<Cell, Boolean> linkTileHadPlant = new IdentityHashMap<>();

    private final Map<Cell, Float> linkTileDeathAnimTime = new IdentityHashMap<>();

    FutureRenderer(GameScreen screen) {
        this.screen = screen;
    }

    boolean isFuture() {
        return screen.session != null && screen.session.getLevel() != null
                && screen.session.getLevel().getSeason() != null
                && "Future".equalsIgnoreCase(screen.session.getLevel().getSeason().getName());
    }

    void drawLinkTileArt(float delta) {
        if (!isFuture()) return;
        animTime += delta;

        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();

        for (int r = 0; r < screen.session.getRows(); r++) {
            for (int c = 0; c < screen.session.getCols(); c++) {
                Cell cell = screen.session.getEnvironment().getCell(r, c);
                if (cell == null || cell.getTile() == null) continue;

                String[] pamPaths = pamForTileType(cell.getTile().type());
                if (pamPaths == null) continue;

                String state = resolveLinkTileState(cell, pamPaths[0], delta);

                float x = GameScreen.BOARD_X + c * boardTileWidth
                        + boardTileWidth * LINK_TILE_OFFSET_X;
                float y = screen.cellY(r)
                        + boardTileHeight * LINK_TILE_OFFSET_Y;
                int row = r;
                String finalState = state;
                float finalTime = animTime;
                for (String pamPath : pamPaths) {
                    String finalPamPath = pamPath;
                    screen.queueRowDraw(row, () -> screen.drawPam(finalPamPath, finalState, finalTime,
                            x, y, LINK_TILE_SCALE, true));
                    break;
                }
            }
        }

        linkTileHadPlant.keySet().removeIf(cell -> cell.getTile() == null || !Future.isLinkTile(cell.getTile().type()));
        linkTileDeathAnimTime.keySet().removeIf(cell -> cell.getTile() == null || !Future.isLinkTile(cell.getTile().type()));
    }

    /**
     * Picks which clip a link tile should play this frame: the idle loop by
     * default, {@link #LINKTILE_DEATH_STATE} for a short beat right after a
     * plant standing on the tile dies, and {@link #LINKTILE_PLANTFOOD_STATE}
     * on both tiles of a pair while either plant in the pair has plant food
     * active.
     */
    private String resolveLinkTileState(Cell cell, String pamPath, float delta) {
        boolean hasPlantNow = cell.hasPlant();
        Boolean hadPlant = linkTileHadPlant.put(cell, hasPlantNow);
        if (Boolean.TRUE.equals(hadPlant) && !hasPlantNow) {
            linkTileDeathAnimTime.put(cell, 0f);
        }

        Float deathTime = linkTileDeathAnimTime.get(cell);
        if (deathTime != null) {
            float duration = AnimationFactory.clipDurationForPath(pamPath, LINKTILE_DEATH_STATE);
            if (duration <= 0f) duration = DEATH_ANIM_FALLBACK_DURATION;

            deathTime += delta;
            if (deathTime >= duration) {
                linkTileDeathAnimTime.remove(cell);
            } else {
                linkTileDeathAnimTime.put(cell, deathTime);
                return LINKTILE_DEATH_STATE;
            }
        }

        if (isPlantFoodActive(cell.getPlant())) {
            return LINKTILE_PLANTFOOD_STATE;
        }

        Cell partnerCell = Future.findLinkPartnerCell(screen.session, cell.getRow(), cell.getCol(),
                cell.getTile().type());
        if (partnerCell != null && isPlantFoodActive(partnerCell.getPlant())) {
            return LINKTILE_PLANTFOOD_STATE;
        }

        return LINKTILE_STATE;
    }

    private static boolean isPlantFoodActive(Plant plant) {
        return plant != null && plant.isAlive() && plant.isPlantFoodActive();
    }

    private static String[] pamForTileType(TileType type) {
        if (type == TileType.LinkTile01) return LINKTILE_01_PAMS;
        if (type == TileType.LinkTile02) return LINKTILE_02_PAMS;
        if (type == TileType.LinkTile03) return LINKTILE_03_PAMS;
        return null;
    }
}