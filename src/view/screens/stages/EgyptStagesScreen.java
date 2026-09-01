package view.screens.stages;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import model.match.main.levels.Level;
import model.match.main.levels.special_levels.BossLevel;

import pvz.libpvz.pam.ClipRef;

import java.util.ArrayList;
import java.util.List;

public class EgyptStagesScreen extends StagesScreen {

    private static final String CHAPTER_NAME = "Egypt";

    private static final String BACK_ICON = "images/ui/buttons_hud_back_normal.png";
    private static final String COLLECTION_ICON = "images/ui/collection.png";
    private static final String GREENHOUSE_ICON = "images/ui/greenhouse.png";
    private static final String LEADERBOARD_ICON = "images/ui/leaderboard.png";
    private static final String COIN_ICON = "images/ui/buttons_coin_buy_normal.png";
    private static final String GEM_ICON = "images/ui/buttons_premium_normal.png";

    private static final String CHAPTER_BACKGROUND = "images/backg/egyptbg.png";
    private static final String COMIC_SHEET_PATH = "assets/images/chapters/egypt/introcomic.jpg";
    private static final String HOURGLASS_PARTICLE_PATH = "assets/images/ui/gravebuster_dirt__rock_01.png";

    private static final String[] STAGE_ISLAND_TEXTURES = {
            "images/chapters/egypt/island5.png",
            "images/chapters/egypt/island4.png",
            "images/chapters/egypt/island5.png"
    };
    private static final String BOSS_STAGE_ISLAND_TEXTURE = "images/chapters/egypt/island3.png";

    private static final float NODE_WIDTH = 125f;
    private static final float NODE_HEIGHT = 95f;

    private static final float BOSS_NODE_WIDTH = 350f;
    private static final float BOSS_NODE_HEIGHT = 400f;

    private static final float PATH_WIDTH = 1700f;
    private static final float PATH_HEIGHT = 700f;

    private static final float LAYOUT_SCALE_X = PATH_WIDTH / 1080f;
    private static final float LAYOUT_SCALE_Y = PATH_HEIGHT / 380f;

    private static final DecorTuning LEVEL_NODE_TUNING = new DecorTuning(260f, 260f, 0.34f, 27f, 44f);
    private static final DecorTuning BOSS_LEVEL_NODE_TUNING = new DecorTuning(260f, 260f, 0.53f, 35f, 25f);
    private static final DecorTuning PYRAMID_TUNING = new DecorTuning(350f, 300f, 0.15f, 70f, -215f);
    private static final DecorTuning ZOMBOSS_TUNING = new DecorTuning(560f, 760f, 0.30f, 37f, 140f);
    private static final DecorTuning TORNADO_TUNING = new DecorTuning(250f, 300f, 0.30f, -83f, -24f);
    private static final DecorTuning ROCK_TUNING = new DecorTuning(100f, 100f, 0.22f, 0f, 0f);
    private static final DecorTuning DUST_TUNING = new DecorTuning(200f, 150f, 0.50f, 10f, 45f);
    private static final DecorTuning STAR_TUNING = new DecorTuning(25f, 25f, 0.30f, 0f, 0f);

    public enum MapObjectType {
        DECOR_HOUSE_ISLAND("images/chapters/egypt/island1.png", false),

        SMALL_ISLAND_1("images/chapters/egypt/island9.png", false),
        SMALL_ISLAND_2("images/chapters/egypt/anim9_347x204.png", false),
        SMALL_ISLAND_3("images/chapters/egypt/anim9_227x131.png", false),
        SMALL_ISLAND_4("images/chapters/egypt/anim5_132x90.png", false),
        SMALL_ISLAND_5("images/chapters/egypt/anim6_208x139.png", false),

        BIG_BOSS_DECOR_ISLAND("768/INITIAL/WORLDMAP/ZOMBOSS_NODE_EGYPT/ZOMBOSS_NODE_EGYPT.PAM", true),
        LEVEL_NODE("768/INITIAL/WORLDMAP/LEVEL_NODE/LEVEL_NODE.PAM", true),

        FLOATING_ROCK_ANIM_1("768/INITIAL/WORLDMAP/EGYPT/ANIM9/ANIM9.PAM", true),
        FLOATING_ROCK_ANIM_2("768/INITIAL/WORLDMAP/EGYPT/ANIM7/ANIM7.PAM", true),
        FLOATING_ROCK_ANIM_3("768/INITIAL/WORLDMAP/EGYPT/ANIM5/ANIM5.PAM", true),

        PYRAMID_ANIM("768/INITIAL/WORLDMAP/DANGER_NODE_EGYPT/DANGER_NODE_EGYPT.PAM", true),

        TORNADO_ANIM("768/INITIAL/WORLDMAP/EGYPT/ANIM4/ANIM4.PAM", true),
        TWINKLING_STAR_ANIM("768/INITIAL/WORLDMAP/EGYPT/ANIM3/ANIM3.PAM", true),
        DUST_EFFECT_ANIM("768/INITIAL/WORLDMAP/EGYPT/ANIM10/ANIM10.PAM", true);

        private final String path;
        private final boolean isPamAnimation;

        MapObjectType(String path, boolean isPamAnimation) {
            this.path = path;
            this.isPamAnimation = isPamAnimation;
        }

        public String getPath() { return path; }
        public boolean isPamAnimation() { return isPamAnimation; }
    }

    private static class MapObjectPlacement {
        final MapObjectType type;
        final float x, y;
        final float width, height;

        MapObjectPlacement(MapObjectType type, float x, float y, float width, float height) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    @Override
    protected String getChapterName() { return CHAPTER_NAME; }

    @Override
    protected String getChapterBackground() { return CHAPTER_BACKGROUND; }

    @Override
    protected String getBackIcon() { return BACK_ICON; }

    @Override
    protected String getCollectionIcon() { return COLLECTION_ICON; }

    @Override
    protected String getGreenhouseIcon() { return GREENHOUSE_ICON; }

    @Override
    protected String getLeaderboardIcon() { return LEADERBOARD_ICON; }

    @Override
    protected String getCoinIcon() { return COIN_ICON; }

    @Override
    protected String getGemIcon() { return GEM_ICON; }

    @Override
    protected String getComicSheetPath() { return COMIC_SHEET_PATH; }

    @Override
    public void initParticles() {
        setupParticles(HOURGLASS_PARTICLE_PATH, 20, 20f, 35f, 1.2f);
    }

    @Override
    public void hide() {
        super.hide();
        super.initParticles();
    }

    @Override
    protected Table buildPathContainer() {
        Table wrap = new Table();

        StagePath stagePath = new StagePath();
        stagePath.setSize(PATH_WIDTH, PATH_HEIGHT);

        ScrollPane scrollPane = new ScrollPane(stagePath);
        scrollPane.setScrollingDisabled(false, false);
        scrollPane.setFadeScrollBars(true);
        scrollPane.setOverscroll(false, false);

        wrap.add(scrollPane).expand().fill().padLeft(-50).padRight(-50).padTop(0).padBottom(-100);
        return wrap;
    }

    private class StagePath extends Group {

        private final float[] centerX;
        private final float[] centerY;
        private final float houseX;
        private final float houseY;
        private final float zombossNodeX;
        private final float zombossNodeY;
        private final float pyramidAnchorX;
        private final float pyramidAnchorY;
        private final float bridgeX;
        private final float bridgeY;

        private float zombossRenderHeight() {
            return ZOMBOSS_TUNING.nativeH() * ZOMBOSS_TUNING.scale();
        }

        StagePath() {
            setSize(PATH_WIDTH, PATH_HEIGHT);

            int count = chapterLevels.size();
            centerX = new float[Math.max(count, 1)];
            centerY = new float[Math.max(count, 1)];

            for (int i = 0; i < count; i++) {
                float progress = count <= 1 ? 0.5f : (float) i / (count - 1);
                float xVal = PATH_WIDTH * (0.12f + 0.76f * progress);
                float yVal = PATH_HEIGHT * (0.52f + 0.26f * (float) Math.sin(progress * Math.PI * 1.3f));
                centerX[i] = xVal;
                centerY[i] = yVal;
            }
            if (count > 0) {
                centerX[0] += 60f * LAYOUT_SCALE_X;
            }

            if (count > 0) {
                houseX = centerX[0] - 170f * LAYOUT_SCALE_X;
                houseY = centerY[0] + 70f * LAYOUT_SCALE_Y;
            } else {
                houseX = 50f * LAYOUT_SCALE_X;
                houseY = 200f * LAYOUT_SCALE_Y;
            }

            if (count >= 3) {
                zombossNodeX = (centerX[1] + centerX[2]) / 2f;
                zombossNodeY = Math.max(centerY[1], centerY[2]) - 200f * LAYOUT_SCALE_Y;
            } else {
                zombossNodeX = 450f * LAYOUT_SCALE_X;
                zombossNodeY = 300f * LAYOUT_SCALE_Y;
            }

            bridgeX = zombossNodeX;
            bridgeY = zombossNodeY + zombossRenderHeight() / 2f;

            if (count > 1) {
                pyramidAnchorX = centerX[1] + PYRAMID_TUNING.offsetX() * LAYOUT_SCALE_X;
                pyramidAnchorY = centerY[1] + 120f * LAYOUT_SCALE_Y;
            } else {
                pyramidAnchorX = PATH_WIDTH / 2f;
                pyramidAnchorY = PATH_HEIGHT / 2f;
            }
            addBackgroundDecorations();

            addActor(new TrailActor());

            addDangerNodeHitArea();

            addMapDecorations();

            for (int i = 0; i < count; i++) {
                buildNode(chapterLevels.get(i), i, i + 1);
            }

            addForegroundEffects();

            if (count == 0) {
                Label empty = new Label("No Egypt stages found.", skin, "muted");
                empty.setPosition(PATH_WIDTH / 2f - 100f, PATH_HEIGHT / 2f);
                addActor(empty);
            }
        }

        private Group createScaledAnimation(MapObjectType type, float nativeWidth, float nativeHeight, String state, float scale, float x, float y) {
            Group group = new Group();
            group.setTransform(true);
            group.setScale(scale);
            group.setSize(nativeWidth, nativeHeight);
            group.setPosition(x, y);

            group.setTouchable(Touchable.disabled);

            MapDecorationActor actor = new MapDecorationActor(type, nativeWidth, nativeHeight, state);
            actor.setSize(nativeWidth, nativeHeight);
            actor.setTouchable(Touchable.disabled);
            group.addActor(actor);
            return group;
        }

        private Group createAnchoredAnimation(MapObjectType type, DecorTuning tuning, String state, float anchorX, float anchorY) {
            float renderW = tuning.nativeW() * tuning.scale();
            float renderH = tuning.nativeH() * tuning.scale();
            float x = anchorX - renderW / 2f + tuning.offsetX() * LAYOUT_SCALE_X;
            float y = anchorY - renderH / 2f + tuning.offsetY() * LAYOUT_SCALE_Y;
            return createScaledAnimation(type, tuning.nativeW(), tuning.nativeH(), state, tuning.scale(), x, y);
        }

        private void addBackgroundDecorations() {
            float[][] starCoords = {
                    {110f, 45f}, {320f, 330f}, {540f, 50f}, {760f, 310f}, {910f, 70f},
                    {210f, 270f}, {460f, 190f}, {650f, 35f}, {870f, 330f}, {140f, 170f},
                    {380f, 85f}, {590f, 320f}, {830f, 175f}, {260f, 345f}, {980f, 220f}
            };
            for (float[] coord : starCoords) {
                addActor(createAnchoredAnimation(MapObjectType.TWINKLING_STAR_ANIM, STAR_TUNING, "idle",
                        coord[0] * LAYOUT_SCALE_X, coord[1] * LAYOUT_SCALE_Y));
            }

            MapObjectType[] rockTypes = {
                    MapObjectType.FLOATING_ROCK_ANIM_1,
                    MapObjectType.FLOATING_ROCK_ANIM_2,
                    MapObjectType.FLOATING_ROCK_ANIM_3
            };
            float[][] rockCoords = {
                    {180f, 310f}, {480f, 320f}, {750f, 300f},
                    {120f, 150f}, {350f, 450f}, {600f, 120f},
                    {820f, 480f}, {1020f, 280f}, {400f, 250f},
                    {250f, 550f}, {680f, 500f}, {920f, 550f}
            };
            for (int i = 0; i < rockCoords.length; i++) {
                MapObjectType selectedRock = rockTypes[i % rockTypes.length];
                addActor(createAnchoredAnimation(selectedRock, ROCK_TUNING, "idle",
                        rockCoords[i][0] * LAYOUT_SCALE_X, rockCoords[i][1] * LAYOUT_SCALE_Y));
            }
        }

        private void addMapDecorations() {
            List<MapObjectPlacement> placements = new ArrayList<>();
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_1, 70, 260, 50, 38));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_2, 310, 15, 55, 40));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_3, 620, 280, 60, 45));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_4, 880, 25, 50, 35));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_5, 970, 240, 55, 40));

            for (MapObjectPlacement p : placements) {
                MapDecorationActor actor = new MapDecorationActor(p.type, p.width, p.height, "idle");
                actor.setPosition(p.x * LAYOUT_SCALE_X, p.y * LAYOUT_SCALE_Y);
                actor.setTouchable(Touchable.disabled);
                addActor(actor);
            }

            MapDecorationActor houseIsland = new MapDecorationActor(MapObjectType.DECOR_HOUSE_ISLAND, 150f, 110f, "idle");
            houseIsland.setPosition(houseX, houseY);
            houseIsland.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    runCommand("menu enter greenhouse");
                }
            });
            addActor(houseIsland);
        }

        private void addDangerNodeHitArea() {
            if (calculateDangerNodeState() == DangerNodeState.LOCKED_IDLE) return;
            addActor(createDangerNodeHitArea(
                    pyramidAnchorX + PYRAMID_TUNING.offsetX() * LAYOUT_SCALE_X,
                    pyramidAnchorY + PYRAMID_TUNING.offsetY() * LAYOUT_SCALE_Y,
                    PYRAMID_TUNING.nativeW() * PYRAMID_TUNING.scale(),
                    PYRAMID_TUNING.nativeH() * PYRAMID_TUNING.scale()));
        }

        private void addForegroundEffects() {
            DangerNodeState pState = calculateDangerNodeState();
            String zombossState = (pState == DangerNodeState.UNLOCKED_IDLE) ? "defeated" : "active";
            addActor(createAnchoredAnimation(MapObjectType.BIG_BOSS_DECOR_ISLAND, ZOMBOSS_TUNING, zombossState, zombossNodeX, zombossNodeY));
            Group pyramid = createAnchoredAnimation(MapObjectType.PYRAMID_ANIM, PYRAMID_TUNING, pState.getPamState(), pyramidAnchorX, pyramidAnchorY);
            if (pState != DangerNodeState.LOCKED_IDLE) {
                pyramid.setTouchable(Touchable.enabled);
                pyramid.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        playDangerNode();
                    }
                });
            } else {
                pyramid.setTouchable(Touchable.disabled);
            }
            addActor(pyramid);

            if (chapterLevels.size() > 0) {
                addActor(createAnchoredAnimation(MapObjectType.DUST_EFFECT_ANIM, DUST_TUNING, "idle", houseX + 45f, houseY + 20f));
                addActor(createAnchoredAnimation(MapObjectType.DUST_EFFECT_ANIM, DUST_TUNING, "idle", centerX[0], centerY[0] - 20f));
            }
            if (chapterLevels.size() >= 3) {
                addActor(createAnchoredAnimation(MapObjectType.DUST_EFFECT_ANIM, DUST_TUNING, "idle", centerX[2], centerY[2] - 20f));
            }

            if (chapterLevels.size() >= 2) {
                float tornadoOffsetX = 130f * LAYOUT_SCALE_X;
                float tornadoOffsetY = 70f * LAYOUT_SCALE_Y;
                addActor(createAnchoredAnimation(MapObjectType.TORNADO_ANIM, TORNADO_TUNING, "idle",
                        centerX[1] + tornadoOffsetX, centerY[1] + tornadoOffsetY));
            }
        }

        private void buildNode(Level level, int index, int stageNumber) {
            boolean boss = level instanceof BossLevel;
            float width = boss ? BOSS_NODE_WIDTH : NODE_WIDTH;
            float height = boss ? BOSS_NODE_HEIGHT : NODE_HEIGHT;
            StageStatus status = statusOf(level);
            LevelNodeState nodeState = levelNodeStateOf(index, status);

            Stack stack = new Stack();
            stack.setSize(width, height);

            String islandPath = boss ? BOSS_STAGE_ISLAND_TEXTURE : STAGE_ISLAND_TEXTURES[index % STAGE_ISLAND_TEXTURES.length];
            Image islandImage = new Image(getTextureDrawable(islandPath, (int) width, (int) height, new Color(0.8f, 0.6f, 0.2f, 1f)));
            stack.add(islandImage);

            Label numberLabel = new Label(boss ? "BOSS" : String.valueOf(stageNumber), skin, "title");
            numberLabel.setAlignment(Align.center);
            stack.add(numberLabel);

            Table column = new Table();
            column.add(stack).size(width, height).row();

            Label nameLabel = new Label(level.getName(), skin, status == StageStatus.LOCKED ? "muted" : "main");
            nameLabel.setAlignment(Align.center);
            nameLabel.setFontScale(0.85f);
            nameLabel.setWrap(true);
            column.add(nameLabel).width(width + 40f).padTop(2);

            column.pack();
            column.setPosition(centerX[index] - column.getWidth() / 2f,
                    centerY[index] - column.getHeight() / 2f);

            if (status != StageStatus.LOCKED) {
                column.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        runCommand("select stage -s " + level.getId());
                    }
                });
            }

            addActor(column);

            DecorTuning tuning = boss ? BOSS_LEVEL_NODE_TUNING : LEVEL_NODE_TUNING;
            addActor(createAnchoredAnimation(MapObjectType.LEVEL_NODE, tuning, nodeState.getPamState(),
                    centerX[index], centerY[index]));
        }

        private class TrailActor extends Actor {
            private final TextureRegion pixel = whitePixelRegion();

            TrailActor() {
                setPosition(0f, 0f);
                setSize(PATH_WIDTH, PATH_HEIGHT);
            }

            @Override
            public void draw(Batch batch, float parentAlpha) {
                batch.setColor(0.85f, 0.70f, 0.35f, 0.8f);

                if (centerX.length > 0) {
                    drawSegment(batch, houseX + 75f, houseY + 20f, centerX[0], centerY[0]);
                }

                boolean bridgeExists = centerX.length >= 3;
                for (int i = 0; i < centerX.length - 1; i++) {
                    boolean isBridgedSegment = bridgeExists && i == 1;
                    if (isBridgedSegment) {
                        continue;
                    }
                    drawSegment(batch, centerX[i], centerY[i], centerX[i + 1], centerY[i + 1]);
                }

                if (bridgeExists) {
                    drawSegment(batch, centerX[1], centerY[1], bridgeX, bridgeY);
                    drawSegment(batch, bridgeX, bridgeY, centerX[2], centerY[2]);
                }

                batch.setColor(Color.WHITE);
            }

            private void drawSegment(Batch batch, float x1, float y1, float x2, float y2) {
                float dx = x2 - x1;
                float dy = y2 - y1;
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
                float thickness = 10f;
                batch.draw(pixel, x1, y1 - thickness / 2f, 0f, thickness / 2f,
                        length, thickness, 1f, 1f, angle);
            }
        }
    }

    private class MapDecorationActor extends Actor {
        private final MapObjectType objectType;
        private Texture texture;
        private final String pamState;
        private float stateTime = 0f;

        public MapDecorationActor(MapObjectType objectType, float width, float height, String state) {
            this.objectType = objectType;
            this.pamState = state;
            setSize(width, height);

            if (!objectType.isPamAnimation()) {
                if (Gdx.files.internal(objectType.getPath()).exists()) {
                    texture = new Texture(Gdx.files.internal(objectType.getPath()));
                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                }
            }
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            stateTime += delta;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (!objectType.isPamAnimation() && texture != null) {
                batch.draw(texture, getX(), getY(), getWidth(), getHeight());
            } else if (objectType.isPamAnimation() && pamPlayer != null) {
                try {
                    ClipRef clip = null;
                    if (pamState != null) {
                        clip = pamPlayer.getClip(objectType.getPath(), pamState);
                    }
                    if (clip == null) {
                        clip = pamPlayer.getClip(objectType.getPath(), "idle");
                    }
                    if (clip == null) {
                        clip = pamPlayer.getClip(objectType.getPath(), "default");
                    }
                    if (clip == null) {
                        clip = pamPlayer.getClip(objectType.getPath(), "");
                    }

                    if (clip != null) {
                        pamPlayer.draw(batch, clip, stateTime, getX(), getY(), true);
                    }
                } catch (Throwable t) {
                }
            }
        }
    }
}
