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

public class DarkAgesStagesScreen extends StagesScreen {

    private static final String CHAPTER_NAME = "Dark Ages";

    private static final String BACK_ICON = "assets/images/ui/buttons_hud_back_normal.png";
    private static final String COLLECTION_ICON = "assets/images/ui/collection.png";
    private static final String GREENHOUSE_ICON = "assets/images/ui/greenhouse.png";
    private static final String LEADERBOARD_ICON = "assets/images/ui/leaderboard.png";
    private static final String COIN_ICON = "assets/images/ui/buttons_coin_buy_normal.png";
    private static final String GEM_ICON = "assets/images/ui/buttons_premium_normal.png";

    private static final String CHAPTER_BACKGROUND = "assets/images/backg/darkage.png";
    private static final String COMIC_SHEET_PATH = "assets/images/chapters/darkage/introcomic.png";

    private static final String[] STAGE_ISLAND_TEXTURES = {
            "assets/images/chapters/darkage/island7.png",
            "assets/images/chapters/darkage/anim9_373x659.png",
            "assets/images/chapters/darkage/anim10_352x358.png"
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

    private static final Color TRAIL_COLOR = new Color(0.55f, 0.32f, 0.85f, 0.85f);

    private static final DecorTuning LEVEL_NODE_TUNING = new DecorTuning(260f, 260f, 0.34f, 27f, 34f);
    private static final DecorTuning BOSS_LEVEL_NODE_TUNING = new DecorTuning(260f, 260f, 0.45f, -14f, 70f);
    private static final DecorTuning DANGER_NODE_TUNING = new DecorTuning(350f, 300f, 0.50f, 45f, 130f);
    private static final DecorTuning ZOMBOSS_TUNING = new DecorTuning(560f, 760f, 0.30f, 0f, 100f);
    private static final DecorTuning ZOMBOSS_HOLOGRAM_TUNING = new DecorTuning(560f, 760f, 0.30f, 0f, 180f);
    private static final DecorTuning FIREFLY_TUNING = new DecorTuning(25f, 25f, 0.15f, 0f, 0f);
    private static final DecorTuning CLOUD_TUNING = new DecorTuning(300f, 200f, 0.30f, 0f, 0f);
    private static final DecorTuning LIGHTNING_TUNING = new DecorTuning(300f, 300f, 0.28f, 0f, 0f);

    public enum MapObjectType {
        DECOR_HOUSE_ISLAND("assets/images/chapters/darkage/anim1_1201x1413.png", false),

        SMALL_ISLAND_1("assets/images/chapters/darkage/island6.png", false),
        SMALL_ISLAND_2("assets/images/chapters/darkage/island8.png", false),
        SMALL_ISLAND_3("assets/images/chapters/darkage/island9.png", false),

        FLOATING_ROCK_1("assets/images/chapters/darkage/anim16_55x63.png", false),
        FLOATING_ROCK_2("assets/images/chapters/darkage/anim15_102x97.png", false),

        PARTICLE("assets/images/chapters/darkage/anim4_23x23.png", false),

        LEVEL_NODE("768/INITIAL/WORLDMAP/LEVEL_NODE/LEVEL_NODE.PAM", true),

        ZOMBOSS_BOSS_ISLAND("768/FULL/WORLDMAP/ZOMBOSS_NODE_DARK/ZOMBOSS_NODE_DARK.PAM", true),
        ZOMBOSS_HOLOGRAM("768/INITIAL/WORLDMAP/ZOMBOSS_NODE_HOLOGRAM/ZOMBOSS_NODE_HOLOGRAM.PAM", true),

        DANGER_NODE_ANIM("768/FULL/WORLDMAP/DANGER_NODE_DARK/DANGER_NODE_DARK.PAM", true),

        CLOUD_ANIM_1("768/FULL/WORLDMAP/DARK/ANIM6/ANIM6.PAM", true),
        CLOUD_ANIM_2("768/FULL/WORLDMAP/DARK/ANIM7/ANIM7.PAM", true),

        FIREFLY_ANIM("768/FULL/WORLDMAP/DARK/ANIM5/ANIM5.PAM", true),

        LIGHTNING_ANIM("768/FULL/EFFECTS/ZOMBIE_DARK_WIZARD_PROJECTILE_HIT/ZOMBIE_DARK_WIZARD_PROJECTILE_HIT.PAM", true);

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
        setupParticles(MapObjectType.PARTICLE.getPath(), 25, 14f, 26f, 1.1f);
    }

    
    @Override
    protected void build() {
        rootTable.clear();

        Table topBar = buildTopBar();
        Table pathContainer = buildPathContainer();
        Table selectionBar = buildSelectionBar();

        rootTable.add(topBar).fillX().padTop(5).padLeft(15).padRight(15).row();
        rootTable.add(new Label("Dark Ages", skin, "title")).padTop(SPACE_MD).row();
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

            addBackgroundDecorations();

            addActor(new TrailActor());

            addDangerNodeHitArea();

            addMapDecorations();

            for (int i = 0; i < count; i++) {
                buildNode(chapterLevels.get(i), i, i + 1);
            }

            addForegroundEffects();

            addClouds();

            if (count == 0) {
                Label empty = new Label("No Dark Ages stages found.", skin, "muted");
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
            float[][] fireflyCoords = {
                    {110f, 45f}, {320f, 330f}, {540f, 50f}, {760f, 310f}, {910f, 70f},
                    {210f, 270f}, {460f, 190f}, {650f, 35f}, {870f, 330f}, {140f, 170f},
                    {380f, 85f}, {590f, 320f}, {830f, 175f}, {260f, 345f}, {980f, 220f}
            };
            for (float[] coord : fireflyCoords) {
                addActor(createAnchoredAnimation(MapObjectType.FIREFLY_ANIM, FIREFLY_TUNING, "idle",
                        coord[0] * LAYOUT_SCALE_X, coord[1] * LAYOUT_SCALE_Y + EXTRA_BOTTOM_SPACE + GLOBAL_Y_OFFSET));
            }

            MapObjectType[] rockTypes = { MapObjectType.FLOATING_ROCK_1, MapObjectType.FLOATING_ROCK_2 };
            float[][] rockSizes = { {40f, 46f}, {46f, 44f} };
            float[][] rockCoords = {
                    {180f, 310f}, {480f, 320f}, {750f, 300f},
                    {120f, 150f}, {350f, 450f}, {600f, 120f},
                    {820f, 480f}, {1020f, 280f}, {400f, 250f},
                    {250f, 550f}, {680f, 500f}, {920f, 550f}
            };
            for (int i = 0; i < rockCoords.length; i++) {
                int typeIndex = i % rockTypes.length;
                MapDecorationActor rock = new MapDecorationActor(rockTypes[typeIndex], rockSizes[typeIndex][0], rockSizes[typeIndex][1], "idle");
                rock.setPosition(rockCoords[i][0] * LAYOUT_SCALE_X, rockCoords[i][1] * LAYOUT_SCALE_Y + EXTRA_BOTTOM_SPACE + GLOBAL_Y_OFFSET);
                rock.setTouchable(Touchable.disabled);
                addActor(rock);
            }

            for (int i = 0; i < 8; i++) {
                MapDecorationActor particle = new MapDecorationActor(MapObjectType.PARTICLE, 14f, 14f, "idle");
                particle.setPosition(rockCoords[i][0] * LAYOUT_SCALE_X + 20f, rockCoords[i][1] * LAYOUT_SCALE_Y + EXTRA_BOTTOM_SPACE + GLOBAL_Y_OFFSET - 20f);
                particle.setTouchable(Touchable.disabled);
                addActor(particle);
            }
        }

        private void addMapDecorations() {
            List<MapObjectPlacement> placements = new ArrayList<>();
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_2, 70, 260, 50, 38));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_2, 270, 150, 55, 40));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_3, 620, 280, 60, 45));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_1, 760, 110, 100, 70));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_2, 970, 240, 55, 40));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_3, 410, 170, 60, 45));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_1, 300, 200, 100, 70));

            for (MapObjectPlacement p : placements) {
                MapDecorationActor actor = new MapDecorationActor(p.type, p.width, p.height, "idle");
                actor.setPosition(p.x * LAYOUT_SCALE_X, p.y * LAYOUT_SCALE_Y + EXTRA_BOTTOM_SPACE + GLOBAL_Y_OFFSET);
                actor.setTouchable(Touchable.disabled);
                addActor(actor);
            }

            MapDecorationActor houseIsland = new MapDecorationActor(MapObjectType.DECOR_HOUSE_ISLAND, 300f, 220f, "idle");
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

            addActor(createAnchoredAnimation(MapObjectType.LIGHTNING_ANIM, LIGHTNING_TUNING, "idle",
                    dangerNodeAnchorX + 60f * LAYOUT_SCALE_X, dangerNodeAnchorY + 40f * LAYOUT_SCALE_Y));
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

            if (boss) {
                String zombossState = (status == StageStatus.COMPLETED) ? "defeated" : "active";
                addActor(createAnchoredAnimation(MapObjectType.ZOMBOSS_BOSS_ISLAND, ZOMBOSS_TUNING, zombossState,
                        centerX[index], centerY[index]));
                addActor(createAnchoredAnimation(MapObjectType.ZOMBOSS_HOLOGRAM, ZOMBOSS_HOLOGRAM_TUNING, "idle",
                        centerX[index], centerY[index] - 75f));
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
            }

            DecorTuning tuning = boss ? BOSS_LEVEL_NODE_TUNING : LEVEL_NODE_TUNING;
            addActor(createAnchoredAnimation(MapObjectType.LEVEL_NODE, tuning, nodeState.getPamState(),
                    centerX[index], centerY[index]));

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
        private static final String[] HOLOGRAM_STATES = {"idle", "idle3", "idle4", "laugh_broken", "laugh"};

        private final MapObjectType objectType;
        private Texture texture;
        private String pamState;
        private float stateTime = 0f;
        private float animDuration = 0f;

        public MapDecorationActor(MapObjectType objectType, float width, float height, String state) {
            this.objectType = objectType;
            if (objectType == MapObjectType.CLOUD_ANIM_1 || objectType == MapObjectType.CLOUD_ANIM_2) {
                String[] states = {"idle", "idle2", "idle3", "idle4"};
                this.pamState = states[(int) (Math.random() * states.length)];
                this.animDuration = 3f + (float) (Math.random() * 3f);
            } else if (objectType == MapObjectType.ZOMBOSS_HOLOGRAM) {
                this.pamState = HOLOGRAM_STATES[(int) (Math.random() * HOLOGRAM_STATES.length)];
                this.animDuration = 3f + (float) (Math.random() * 3f);
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
                    String[] states = {"idle", "idle2", "idle3", "idle4"};
                    this.pamState = states[(int) (Math.random() * states.length)];
                    this.animDuration = 3f + (float) (Math.random() * 3f);
                }
            } else if (objectType == MapObjectType.ZOMBOSS_HOLOGRAM) {
                if (stateTime >= animDuration) {
                    stateTime = 0f;
                    this.pamState = HOLOGRAM_STATES[(int) (Math.random() * HOLOGRAM_STATES.length)];
                    this.animDuration = 3f + (float) (Math.random() * 3f);
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