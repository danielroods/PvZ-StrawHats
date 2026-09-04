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
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import model.match.main.levels.Level;
import model.match.main.levels.special_levels.BossLevel;

import pvz.libpvz.pam.ClipRef;

import java.util.ArrayList;
import java.util.List;

public class FutureStagesScreen extends StagesScreen {

    private static final String CHAPTER_NAME = "Future";

    private static final String BACK_ICON = "assets/images/ui/buttons_hud_back_normal.png";
    private static final String COLLECTION_ICON = "assets/images/ui/collection.png";
    private static final String GREENHOUSE_ICON = "assets/images/ui/greenhouse.png";
    private static final String LEADERBOARD_ICON = "assets/images/ui/leaderboard.png";
    private static final String COIN_ICON = "assets/images/ui/buttons_coin_buy_normal.png";
    private static final String GEM_ICON = "assets/images/ui/buttons_premium_normal.png";

    private static final String CHAPTER_BACKGROUND = "assets/images/backg/futurebg.png";
    private static final String COMIC_SHEET_PATH = "";
    private static final String FUTURE_PARTICLE_PATH = "assets/images/chapters/future/anim13_80x82.png";

    private static final String[] STAGE_ISLAND_TEXTURES = {
            "assets/images/chapters/future/island5.png",
            "assets/images/chapters/future/island6.png",
            "assets/images/chapters/future/island13.png"
    };

    private static final float DESIGN_PATH_HEIGHT = 700f;
    private static final float EXTRA_BOTTOM_SPACE = 90f;
    private static final float EXTRA_TOP_SPACE = 220f;

    private static final float GLOBAL_Y_OFFSET = 100f;

    private static final float NODE_WIDTH = 125f;
    private static final float NODE_HEIGHT = 95f;

    private static final float BOSS_NODE_WIDTH = 350f;
    private static final float BOSS_NODE_HEIGHT = 400f;

    private static final float PATH_WIDTH = 1700f;
    private static final float PATH_HEIGHT = DESIGN_PATH_HEIGHT + EXTRA_BOTTOM_SPACE + EXTRA_TOP_SPACE;

    private static final float LAYOUT_SCALE_X = PATH_WIDTH / 1080f;
    private static final float LAYOUT_SCALE_Y = DESIGN_PATH_HEIGHT / 380f;

    private static final Color TRAIL_COLOR = new Color(1f, 1f, 1f, 0.9f);

    private static final DecorTuning DANGER_NODE_TUNING = new DecorTuning(350f, 300f, 0.50f, 45f, 130f);
    private static final DecorTuning ZOMBOSS_TUNING = new DecorTuning(560f, 760f, 0.30f, 0f, 100f);
    private static final DecorTuning CLOUD_TUNING = new DecorTuning(300f, 200f, 0.30f, 0f, 0f);

    // Marker shown on each (non-boss) level island shifted (+25f right, +30f up)
    private static final DecorTuning LEVEL_NODE_TUNING = new DecorTuning(260f, 260f, 0.34f, 29f, 35f);
    // Boss node marker moved 80f left (25f - 80f = -55f) and 350f down (210f - 350f = -140f) relative to Zomboss anchor
    private static final DecorTuning BOSS_LEVEL_NODE_TUNING = new DecorTuning(220f, 220f, 0.40f, -25f, 60f);

    private static final DecorTuning TREE_TUNING = new DecorTuning(400f, 400f, 0.20f, 0f, 0f);
    private static final DecorTuning HOUSE_TUNING = new DecorTuning(500f, 400f, 0.28f, 0f, 0f);

    public enum MapObjectType {
        DECOR_HOUSE_ISLAND("768/FULL/WORLDMAP/FUTURE/ANIM7/ANIM7.PAM", true),

        SMALL_ISLAND_1("assets/images/chapters/future/island19.png", false),
        SMALL_ISLAND_2("768/FULL/WORLDMAP/FUTURE/ANIM13/ANIM13.PAM", true),
        SMALL_ISLAND_3("768/FULL/WORLDMAP/FUTURE/ANIM10/ANIM10.PAM", true),
        SMALL_ISLAND_4("768/FULL/WORLDMAP/FUTURE/ANIM12/ANIM12.PAM", true),

        ZOMBOSS_BOSS_ISLAND("768/FULL/WORLDMAP/ZOMBOSS_NODE_FUTURE/ZOMBOSS_NODE_FUTURE.PAM", true),
        DANGER_NODE_ANIM("768/FULL/WORLDMAP/DANGER_NODE_FUTURE/DANGER_NODE_FUTURE.PAM", true),
        LEVEL_NODE("768/INITIAL/WORLDMAP/LEVEL_NODE/LEVEL_NODE.PAM", true),

        CLOUD_ANIM_1("768/FULL/WORLDMAP/FUTURE/ANIM4/ANIM4.PAM", true),
        CLOUD_ANIM_2("768/FULL/WORLDMAP/FUTURE/ANIM4/ANIM4.PAM", true);

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
        setupParticles(FUTURE_PARTICLE_PATH, 20, 5f, 15f, 1.2f);
    }

    @Override
    public void hide() {
        super.hide();
        super.initParticles();
    }

    @Override
    protected void build() {
        rootTable.clear();

        Table topBar = buildTopBar();
        Table pathContainer = buildPathContainer();
        Table selectionBar = buildSelectionBar();

        rootTable.add(topBar).fillX().padTop(5).padLeft(15).padRight(15).row();
        rootTable.add(new Label("Future", skin, "title")).padTop(SPACE_MD).row();
        rootTable.add(pathContainer).expand().padTop(SPACE_SM).row();
        rootTable.add(selectionBar).fillX().padBottom(SPACE_SM);

        topBar.toFront();
        selectionBar.toFront();
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

        scrollPane.layout();
        scrollPane.setScrollY(0);

        wrap.add(scrollPane).expand().fill().padLeft(-50).padRight(-50).padTop(0).padBottom(0);
        return wrap;
    }

    private class StagePath extends Group {

        private final float[] centerX;
        private final float[] centerY;
        private final float houseX;
        private final float houseY;
        private final float dangerNodeAnchorX;
        private final float dangerNodeAnchorY;
        private final float bridgeX;
        private final float bridgeY;
        private final float bossVisualX;
        private final float bossVisualY;

        private float dangerRenderHeight() {
            return DANGER_NODE_TUNING.nativeH() * DANGER_NODE_TUNING.scale();
        }

        StagePath() {
            setSize(PATH_WIDTH, PATH_HEIGHT);

            int count = chapterLevels.size();
            centerX = new float[Math.max(count, 1)];
            centerY = new float[Math.max(count, 1)];

            for (int i = 0; i < count; i++) {
                float progress = count <= 1 ? 0.5f : (float) i / (count - 1);
                float xVal = PATH_WIDTH * (0.12f + 0.76f * progress);
                float yVal = DESIGN_PATH_HEIGHT * (0.52f + 0.26f * (float) Math.sin(progress * Math.PI * 1.3f)) + EXTRA_BOTTOM_SPACE + GLOBAL_Y_OFFSET;

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
                houseY = 200f * LAYOUT_SCALE_Y + GLOBAL_Y_OFFSET;
            }

            if (count >= 3) {
                dangerNodeAnchorX = (centerX[1] + centerX[2]) / 2f;
                dangerNodeAnchorY = Math.max(centerY[1], centerY[2]) - 200f * LAYOUT_SCALE_Y;
            } else {
                dangerNodeAnchorX = PATH_WIDTH / 2f;
                dangerNodeAnchorY = PATH_HEIGHT / 2f + GLOBAL_Y_OFFSET;
            }

            bridgeX = dangerNodeAnchorX;
            bridgeY = dangerNodeAnchorY + dangerRenderHeight() / 2f;

            if (count > 0) {
                bossVisualX = centerX[count - 1] + ZOMBOSS_TUNING.offsetX() * LAYOUT_SCALE_X;
                bossVisualY = centerY[count - 1] + ZOMBOSS_TUNING.offsetY() * LAYOUT_SCALE_Y;
            } else {
                bossVisualX = 0f;
                bossVisualY = 0f;
            }

            addBackgroundDecorations();

            addMapDecorations();

            // Render path trail before home island and level nodes
            addActor(new TrailActor());

            // Render home island on top of trail line
            addHomeIsland();

            addDangerNodeHitArea();

            for (int i = 0; i < count; i++) {
                buildNode(chapterLevels.get(i), i, i + 1);
            }

            addForegroundEffects();

            addClouds();

            if (count == 0) {
                Label empty = new Label("No Future stages found.", skin, "muted");
                empty.setPosition(PATH_WIDTH / 2f - 130f, PATH_HEIGHT / 2f + GLOBAL_Y_OFFSET);
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
            float[][] islandCoords = {
                    {110f, 45f}, {320f, 330f}, {540f, 50f}, {760f, 310f}, {910f, 70f},
                    {210f, 270f}, {460f, 190f}, {650f, 35f}, {870f, 330f}, {140f, 170f},
                    {380f, 85f}, {590f, 320f}, {830f, 175f}, {260f, 345f}, {980f, 220f}
            };
            MapObjectType[] types = {
                    MapObjectType.SMALL_ISLAND_1, MapObjectType.SMALL_ISLAND_2,
                    MapObjectType.SMALL_ISLAND_3, MapObjectType.SMALL_ISLAND_4
            };
            float[][] sizes = { {105f, 80f}, {110f, 85f}, {100f, 90f}, {115f, 90f} };
            for (int i = 0; i < islandCoords.length; i++) {
                int t = i % types.length;
                MapObjectType type = types[t];
                float x = islandCoords[i][0] * LAYOUT_SCALE_X;
                float y = islandCoords[i][1] * LAYOUT_SCALE_Y + EXTRA_BOTTOM_SPACE + GLOBAL_Y_OFFSET;
                if (type.isPamAnimation()) {
                    addActor(createAnchoredAnimation(type, TREE_TUNING, "idle", x, y));
                } else {
                    MapDecorationActor actor = new MapDecorationActor(type, sizes[t][0], sizes[t][1], "idle");
                    actor.setPosition(x, y);
                    actor.setTouchable(Touchable.disabled);
                    addActor(actor);
                }
            }
        }

        private void addMapDecorations() {
            List<MapObjectPlacement> placements = new ArrayList<>();

            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_1,
                    dangerNodeAnchorX / LAYOUT_SCALE_X - 180f,
                    dangerNodeAnchorY / LAYOUT_SCALE_Y - 125f,
                    360f, 270f));

            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_2, 70, 260, 120, 90));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_3, 270, 150, 125, 95));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_4, 620, 280, 125, 100));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_2, 880, 25, 120, 90));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_3, 970, 240, 125, 95));

            for (MapObjectPlacement p : placements) {
                float x = p.x * LAYOUT_SCALE_X;
                float y = p.y * LAYOUT_SCALE_Y + EXTRA_BOTTOM_SPACE + GLOBAL_Y_OFFSET;
                if (p.type.isPamAnimation()) {
                    addActor(createAnchoredAnimation(p.type, TREE_TUNING, "idle", x, y));
                } else {
                    MapDecorationActor actor = new MapDecorationActor(p.type, p.width, p.height, "idle");
                    actor.setPosition(x, y);
                    actor.setTouchable(Touchable.disabled);
                    addActor(actor);
                }
            }
        }

        private void addHomeIsland() {
            Group houseIslandGroup = createAnchoredAnimation(MapObjectType.DECOR_HOUSE_ISLAND,
                    HOUSE_TUNING, "idle", houseX + 150f, houseY + 110f);
            houseIslandGroup.setTouchable(Touchable.enabled);
            houseIslandGroup.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    runCommand("menu enter greenhouse");
                }
            });
            addActor(houseIslandGroup);
        }

        private void addDangerNodeHitArea() {
            if (calculateDangerNodeState() == DangerNodeState.LOCKED_IDLE) return;
            addActor(createDangerNodeHitArea(
                    dangerNodeAnchorX + DANGER_NODE_TUNING.offsetX() * LAYOUT_SCALE_X,
                    dangerNodeAnchorY + DANGER_NODE_TUNING.offsetY() * LAYOUT_SCALE_Y,
                    DANGER_NODE_TUNING.nativeW() * DANGER_NODE_TUNING.scale(),
                    DANGER_NODE_TUNING.nativeH() * DANGER_NODE_TUNING.scale()));
        }

        private void addForegroundEffects() {
            DangerNodeState dState = calculateDangerNodeState();
            Group dangerNode = createAnchoredAnimation(MapObjectType.DANGER_NODE_ANIM, DANGER_NODE_TUNING, dState.getPamState(),
                    dangerNodeAnchorX, dangerNodeAnchorY);
            if (dState != DangerNodeState.LOCKED_IDLE) {
                dangerNode.setTouchable(Touchable.enabled);
                dangerNode.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        playDangerNode();
                    }
                });
            } else {
                dangerNode.setTouchable(Touchable.disabled);
            }
            addActor(dangerNode);
        }

        private void addClouds() {
            float travel = PATH_WIDTH + CLOUD_TUNING.nativeW() * CLOUD_TUNING.scale() * 2f;
            for (int i = 0; i < 4; i++) {
                float startOffset = travel * i / 4f;
                float y = DESIGN_PATH_HEIGHT * (0.65f + 0.08f * i) + EXTRA_BOTTOM_SPACE + GLOBAL_Y_OFFSET;
                float speed = 40f + i * 6f;
                addActor(new DriftingCloud(MapObjectType.CLOUD_ANIM_1, CLOUD_TUNING, startOffset, y, speed));
            }
            for (int i = 0; i < 4; i++) {
                float startOffset = travel * (i + 0.5f) / 4f;
                float y = DESIGN_PATH_HEIGHT * (0.20f + 0.08f * i) + EXTRA_BOTTOM_SPACE + GLOBAL_Y_OFFSET;
                float speed = 30f + i * 5f;
                addActor(new DriftingCloud(MapObjectType.CLOUD_ANIM_2, CLOUD_TUNING, startOffset, y, speed));
            }
        }

        private void buildNode(Level level, int index, int stageNumber) {
            boolean boss = (level instanceof BossLevel) || (index == chapterLevels.size() - 1);
            float width = boss ? BOSS_NODE_WIDTH : NODE_WIDTH;
            float height = boss ? BOSS_NODE_HEIGHT : NODE_HEIGHT;
            StageStatus status = statusOf(level);
            LevelNodeState nodeState = levelNodeStateOf(index, status);

            DecorTuning tuning = boss ? BOSS_LEVEL_NODE_TUNING : LEVEL_NODE_TUNING;

            if (boss) {
                String zombossState = (status == StageStatus.COMPLETED) ? "defeated" : "active";
                addActor(createAnchoredAnimation(MapObjectType.ZOMBOSS_BOSS_ISLAND, ZOMBOSS_TUNING, zombossState,
                        centerX[index], centerY[index]));

                Group bossNodeGroup = createAnchoredAnimation(MapObjectType.LEVEL_NODE, BOSS_LEVEL_NODE_TUNING, nodeState.getPamState(),
                        centerX[index], centerY[index]);
                addActor(bossNodeGroup);
                bossNodeGroup.toFront();
            } else {
                String islandPath = STAGE_ISLAND_TEXTURES[index % STAGE_ISLAND_TEXTURES.length];

                float islandWidth = width;
                float islandHeight = (index == 1) ? height * 1.5f : height;

                Image islandImage = new Image(getTextureDrawable(islandPath, (int) islandWidth, (int) islandHeight, new Color(0.45f, 0.30f, 0.6f, 1f)));
                islandImage.setSize(islandWidth, islandHeight);

                float islandX = centerX[index] - islandWidth / 2f;
                float islandY;

                if (index == 1) {
                    float topY = centerY[index] + height / 2f;
                    islandY = topY - islandHeight + 16f;
                } else {
                    islandY = centerY[index] - islandHeight / 2f;
                }

                islandImage.setPosition(islandX, islandY);
                islandImage.setTouchable(Touchable.disabled);
                addActor(islandImage);

                Group levelNodeGroup = createAnchoredAnimation(MapObjectType.LEVEL_NODE, LEVEL_NODE_TUNING, nodeState.getPamState(),
                        centerX[index], centerY[index]);
                addActor(levelNodeGroup);
                levelNodeGroup.toFront();
            }

            Stack stack = new Stack();
            stack.setSize(width, height);

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

            float nodeCenterX = centerX[index] + tuning.offsetX() * LAYOUT_SCALE_X;
            float nodeCenterY = centerY[index] + tuning.offsetY() * LAYOUT_SCALE_Y;

            column.setPosition(nodeCenterX - column.getWidth() / 2f,
                    nodeCenterY - column.getHeight() / 2f);

            if (status != StageStatus.LOCKED) {
                column.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        runCommand("select stage -s " + level.getId());
                    }
                });
            }

            addActor(column);
            column.toFront();
        }

        private class DriftingCloud extends Group {
            private final float renderWidth;
            private final float baseY;
            private final float startOffset;
            private final float speed;
            private final float travel;
            private float elapsed = 0f;

            DriftingCloud(MapObjectType type, DecorTuning tuning, float startOffset, float y, float speed) {
                setTransform(true);
                setScale(tuning.scale());
                setSize(tuning.nativeW(), tuning.nativeH());
                setTouchable(Touchable.disabled);

                this.renderWidth = tuning.nativeW() * tuning.scale();
                this.baseY = y;
                this.startOffset = startOffset;
                this.speed = speed;
                this.travel = PATH_WIDTH + renderWidth * 2f;

                MapDecorationActor actor = new MapDecorationActor(type, tuning.nativeW(), tuning.nativeH(), "idle");
                actor.setSize(tuning.nativeW(), tuning.nativeH());
                actor.setTouchable(Touchable.disabled);
                addActor(actor);

                setPosition(currentX(), baseY);
            }

            private float currentX() {
                float x = (startOffset + speed * elapsed) % travel;
                return x - renderWidth;
            }

            @Override
            public void act(float delta) {
                super.act(delta);
                elapsed += delta;
                setPosition(currentX(), baseY);
            }
        }

        private class TrailActor extends Actor {
            private final TextureRegion pixel = whitePixelRegion();

            TrailActor() {
                setPosition(0f, 0f);
                setSize(PATH_WIDTH, PATH_HEIGHT);
            }

            @Override
            public void draw(Batch batch, float parentAlpha) {
                batch.setColor(TRAIL_COLOR);

                if (centerX.length > 0) {
                    drawSegment(batch, houseX + 150f, houseY + 110f, centerX[0], centerY[0]);
                }

                boolean bridgeExists = centerX.length >= 3;
                for (int i = 0; i < centerX.length - 1; i++) {
                    boolean isBridgedSegment = bridgeExists && i == 1;
                    if (isBridgedSegment) {
                        continue;
                    }
                    boolean isFinalSegment = i == centerX.length - 2;
                    float endX = isFinalSegment ? bossVisualX : centerX[i + 1];
                    float endY = isFinalSegment ? bossVisualY : centerY[i + 1];
                    drawSegment(batch, centerX[i], centerY[i], endX, endY);
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
        private String pamState;
        private float stateTime = 0f;
        private float animDuration = 0f;

        public MapDecorationActor(MapObjectType objectType, float width, float height, String state) {
            this.objectType = objectType;
            if (objectType == MapObjectType.CLOUD_ANIM_1 || objectType == MapObjectType.CLOUD_ANIM_2) {
                this.pamState = "idle";
                this.animDuration = Float.MAX_VALUE;
            } else {
                this.pamState = state;
            }
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
            if (objectType == MapObjectType.CLOUD_ANIM_1 || objectType == MapObjectType.CLOUD_ANIM_2) {
                if (stateTime >= animDuration) {
                    stateTime = 0f;
                    this.pamState = "idle";
                    this.animDuration = Float.MAX_VALUE;
                }
            }
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
                        clip = pamPlayer.getClip(objectType.getPath(), "active");
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