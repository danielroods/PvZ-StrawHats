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
import com.badlogic.gdx.utils.Align;

import model.match.main.levels.Level;
import model.match.main.levels.special_levels.BossLevel;

import pvz.libpvz.pam.ClipRef;

import java.util.ArrayList;
import java.util.List;

public class PirateStagesScreen extends StagesScreen {

    private static final String CHAPTER_NAME = "Pirates";

    private static final String BACK_ICON = "images/ui/buttons_hud_back_normal.png";
    private static final String COLLECTION_ICON = "images/ui/collection.png";
    private static final String GREENHOUSE_ICON = "images/ui/greenhouse.png";
    private static final String LEADERBOARD_ICON = "images/ui/leaderboard.png";
    private static final String COIN_ICON = "images/ui/buttons_coin_buy_normal.png";
    private static final String GEM_ICON = "images/ui/buttons_premium_normal.png";

    private static final String CHAPTER_BACKGROUND = "images/backg/beaches_stages.png";
    private static final String COMIC_SHEET_PATH = null;
    private static final String SPLASH_PARTICLE_PATH = "assets/images/chapters/pirate/decs/anim3_74x75.png";

    private static final String[] STAGE_ISLAND_TEXTURES = {
            "assets/images/chapters/pirate/island8.png",
            "assets/images/chapters/pirate/island5.png",
            "assets/images/chapters/pirate/island3.png",
            "assets/images/chapters/pirate/island3.png"
    };
    private static final float NODE_WIDTH = 135f;
    private static final float NODE_HEIGHT = 90f;

    private static final float PATH_WIDTH = 1950f;
    private static final float PATH_HEIGHT = 700f;

    private static final float LAYOUT_SCALE_X = PATH_WIDTH / 1080f;
    private static final float LAYOUT_SCALE_Y = PATH_HEIGHT / 380f;

    private static final DecorTuning HOUSE_ISLAND_TUNING = new DecorTuning(520f, 400f, 0.35f, 0f, 50f);
    private static final DecorTuning LEVEL_NODE_TUNING = new DecorTuning(260f, 260f, 0.34f, 27f, 34f);
    private static final DecorTuning BOSS_LEVEL_NODE_TUNING = new DecorTuning(260f, 260f, 0.55f, -20f, -10f);
    private static final DecorTuning DANGER_NODE_TUNING = new DecorTuning(350f, 300f, 0.38f, 45f, 75f);
    private static final DecorTuning ZOMBOSS_TUNING = new DecorTuning(560f, 760f, 0.42f, 10f, 40f);
    private static final DecorTuning ZOMBOSS_HOLOGRAM_TUNING = new DecorTuning(560f, 760f, 0.42f, 10f, 120f);
    private static final DecorTuning WAVE_TUNING = new DecorTuning(90f, 180f, 0.30f, -120f, -60f);
    private static final DecorTuning SPLASH_TUNING = new DecorTuning(200f, 150f, 0.50f, 0f, 60f);
    private static final DecorTuning STAR_TUNING = new DecorTuning(25f, 25f, 0.30f, 0f, 0f);

    private static final DecorTuning WATER_DROP_TUNING = new DecorTuning(100f, 100f, 0.40f, 0f, -20f);
    private static final DecorTuning LARGE_ROCK_BEACH_TUNING = new DecorTuning(300f, 300f, 0.34f, -40f, -30f);
    private static final DecorTuning SMALL_ROCK_BEACH_TUNING = new DecorTuning(240f, 240f, 0.30f, -10f, 10f);

    public enum MapObjectType {
        DECOR_HOUSE_ISLAND("768/FULL/WORLDMAP/PIRATE/ANIM6/ANIM6.PAM", true),
        SMALL_ISLAND_1("assets/images/chapters/pirate/decs/island13.png", false),
        SMALL_ISLAND_2("assets/images/chapters/pirate/decs/island14.png", false),
        SMALL_ISLAND_3("assets/images/chapters/pirate/decs/island15.png", false),
        SMALL_ISLAND_4("assets/images/chapters/pirate/decs/island17.png", false),
        SMALL_ISLAND_5("assets/images/chapters/pirate/decs/island18.png", false),
        ZOMBOSS_NODE("768/FULL/WORLDMAP/ZOMBOSS_NODE_PIRATE/ZOMBOSS_NODE_PIRATE.PAM", true, null, null),
        ZOMBOSS_HOLOGRAM("768/INITIAL/WORLDMAP/ZOMBOSS_NODE_HOLOGRAM/ZOMBOSS_NODE_HOLOGRAM.PAM", true, null, null),
        LEVEL_NODE("768/INITIAL/WORLDMAP/LEVEL_NODE/LEVEL_NODE.PAM", true),
        DANGER_NODE_ANIM("768/FULL/WORLDMAP/DANGER_NODE_PIRATE/DANGER_NODE_PIRATE.PAM", true,
                "assets/images/chapters/pirate/island5.png", new Color(0.85f, 0.25f, 0.22f, 1f)),
        FLOATING_ROCK_ANIM_1("768/FULL/WORLDMAP/PIRATE/ANIM9/ANIM9.PAM", true),
        FLOATING_ROCK_ANIM_2("assets/images/chapters/pirate/decs/island24.png", false),
        FLOATING_ROCK_ANIM_3("assets/images/chapters/pirate/decs/island25.png", false),
        WAVE_ANIM("768/FULL/WORLDMAP/PIRATE/ANIM9/ANIM9.PAM", true),
        TWINKLING_STAR_ANIM("assets/images/chapters/pirate/decs/anim3_74x75.png", false),
        SPLASH_EFFECT_ANIM("assets/images/chapters/pirate/decs/anim3_74x75.png", false),
        WATER_DROP_ANIM("assets/images/chapters/pirate/decs/island24.png", false),
        FLOATING_ROCK_BEACH_LARGE_1("assets/images/chapters/pirate/decs/island13.png", false),
        FLOATING_ROCK_BEACH_LARGE_2("assets/images/chapters/pirate/decs/island14.png", false),
        SMALL_ROCK_BEACH_1("assets/images/chapters/pirate/decs/island15.png", false),
        SMALL_ROCK_BEACH_2("assets/images/chapters/pirate/decs/island17.png", false),
        DANGER_BACKGROUND_ISLAND("assets/images/chapters/pirate/island19.png", false);

        private final String path;
        private final boolean isPamAnimation;
        private final String fallbackPath;
        private final Color fallbackTint;

        MapObjectType(String path, boolean isPamAnimation) {
            this(path, isPamAnimation, null, null);
        }

        MapObjectType(String path, boolean isPamAnimation, String fallbackPath, Color fallbackTint) {
            this.path = path;
            this.isPamAnimation = isPamAnimation;
            this.fallbackPath = fallbackPath;
            this.fallbackTint = fallbackTint;
        }

        public String getPath() { return path; }
        public boolean isPamAnimation() { return isPamAnimation; }
        public String getFallbackPath() { return fallbackPath; }
        public Color getFallbackTint() { return fallbackTint; }
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
        setupParticles(SPLASH_PARTICLE_PATH, 20, 20f, 35f, 1.2f);
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
        private final float dangerNodeAnchorX;
        private final float dangerNodeAnchorY;

        StagePath() {
            setSize(PATH_WIDTH, PATH_HEIGHT);

            int count = chapterLevels.size();
            centerX = new float[Math.max(count, 1)];
            centerY = new float[Math.max(count, 1)];

            for (int i = 0; i < count; i++) {
                float progress = count <= 1 ? 0.5f : (float) i / (count - 1);
                float xVal = 400f + 1450f * progress;
                float yVal = 360f + 110f * (float) Math.sin(progress * Math.PI);
                centerX[i] = xVal;
                centerY[i] = yVal;
            }

            if (count > 0) {
                houseX = centerX[0] - 140f;
                houseY = centerY[0] + 130f;
            } else {
                houseX = 260f;
                houseY = 490f;
            }

            if (count >= 3) {
                dangerNodeAnchorX = (centerX[1] + centerX[2]) / 2f;
                dangerNodeAnchorY = (centerY[1] + centerY[2]) / 2f - 15f;
            } else if (count > 1) {
                dangerNodeAnchorX = centerX[1] + 120f;
                dangerNodeAnchorY = centerY[1] - 15f;
            } else {
                dangerNodeAnchorX = PATH_WIDTH / 2f;
                dangerNodeAnchorY = PATH_HEIGHT / 2f;
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
                Label empty = new Label("No Pirates stages found.", skin, "muted");
                empty.setPosition(PATH_WIDTH / 2f - 100f, PATH_HEIGHT / 2f);
                addActor(empty);
            }
        }

        private Group createScaledAnimation(MapObjectType type, float nativeWidth, float nativeHeight, String state, float scale, float x, float y, boolean flipX) {
            Group group = new Group();
            group.setTransform(true);
            group.setSize(nativeWidth, nativeHeight);
            group.setOrigin(nativeWidth / 2f, nativeHeight / 2f);
            group.setScale(flipX ? -scale : scale, scale);
            group.setPosition(x, y);

            group.setTouchable(Touchable.disabled);

            MapDecorationActor actor = new MapDecorationActor(type, nativeWidth, nativeHeight, state);
            actor.setSize(nativeWidth, nativeHeight);
            actor.setTouchable(Touchable.disabled);
            group.addActor(actor);
            return group;
        }

        private Group createScaledAnimation(MapObjectType type, float nativeWidth, float nativeHeight, String state, float scale, float x, float y) {
            return createScaledAnimation(type, nativeWidth, nativeHeight, state, scale, x, y, false);
        }

        private Group createAnchoredAnimation(MapObjectType type, DecorTuning tuning, String state, float anchorX, float anchorY, boolean flipX) {
            float nativeW = tuning.nativeW();
            float nativeH = tuning.nativeH();
            float x = anchorX - nativeW / 2f + tuning.offsetX() * LAYOUT_SCALE_X;
            float y = anchorY - nativeH / 2f + tuning.offsetY() * LAYOUT_SCALE_Y;
            return createScaledAnimation(type, nativeW, nativeH, state, tuning.scale(), x, y, flipX);
        }

        private Group createAnchoredAnimation(MapObjectType type, DecorTuning tuning, String state, float anchorX, float anchorY) {
            return createAnchoredAnimation(type, tuning, state, anchorX, anchorY, false);
        }

        private void addBackgroundDecorations() {
            MapObjectType[] islandTypes = {
                    MapObjectType.SMALL_ISLAND_1, MapObjectType.SMALL_ISLAND_2,
                    MapObjectType.SMALL_ISLAND_3, MapObjectType.SMALL_ISLAND_4,
                    MapObjectType.SMALL_ISLAND_5, MapObjectType.FLOATING_ROCK_ANIM_2,
                    MapObjectType.FLOATING_ROCK_ANIM_3
            };
            float[][] coords = {
                    {90f, 560f}, {360f, 610f}, {700f, 590f}, {980f, 615f},
                    {1330f, 540f}, {250f, 90f}, {1120f, 100f}
            };
            for (int i = 0; i < islandTypes.length; i++) {
                addActor(createAnchoredAnimation(islandTypes[i], LARGE_ROCK_BEACH_TUNING, "idle",
                        coords[i][0] * LAYOUT_SCALE_X, coords[i][1] * LAYOUT_SCALE_Y));
            }

            float[][] largeRocksBeach1 = { {180f, 330f}, {560f, 130f}, {930f, 340f} };
            for (float[] coord : largeRocksBeach1) {
                addActor(createAnchoredAnimation(MapObjectType.FLOATING_ROCK_BEACH_LARGE_1, LARGE_ROCK_BEACH_TUNING, "idle",
                        coord[0] * LAYOUT_SCALE_X, coord[1] * LAYOUT_SCALE_Y));
            }
            float[][] largeRocksBeach2 = { {60f, 130f}, {430f, 350f}, {1220f, 300f} };
            for (float[] coord : largeRocksBeach2) {
                addActor(createAnchoredAnimation(MapObjectType.FLOATING_ROCK_BEACH_LARGE_2, LARGE_ROCK_BEACH_TUNING, "idle",
                        coord[0] * LAYOUT_SCALE_X, coord[1] * LAYOUT_SCALE_Y));
            }

            float[][] smallRocksBeach = {
                    {230f, 230f}, {500f, 470f}, {780f, 200f},
                    {1080f, 420f}, {1420f, 260f}
            };
            for (int i = 0; i < smallRocksBeach.length; i++) {
                MapObjectType type = (i % 2 == 0) ? MapObjectType.SMALL_ROCK_BEACH_1 : MapObjectType.SMALL_ROCK_BEACH_2;
                addActor(createAnchoredAnimation(type, SMALL_ROCK_BEACH_TUNING, "idle",
                        smallRocksBeach[i][0] * LAYOUT_SCALE_X, smallRocksBeach[i][1] * LAYOUT_SCALE_Y));
            }

            float[][] waterDropCoords = {
                    {130f, 260f}, {330f, 440f}, {520f, 200f},
                    {690f, 380f}, {880f, 160f}, {1060f, 300f},
                    {1250f, 440f}, {1450f, 180f}
            };
            for (float[] coord : waterDropCoords) {
                addActor(createAnchoredAnimation(MapObjectType.WATER_DROP_ANIM, WATER_DROP_TUNING, "idle",
                        coord[0] * LAYOUT_SCALE_X, coord[1] * LAYOUT_SCALE_Y));
            }

            float[][] starCoords = {
                    {60f, 620f}, {300f, 660f}, {540f, 630f},
                    {760f, 660f}, {1000f, 640f}, {1220f, 660f},
                    {1450f, 630f}, {200f, 40f}, {950f, 50f}
            };
            for (float[] coord : starCoords) {
                addActor(createAnchoredAnimation(MapObjectType.TWINKLING_STAR_ANIM, STAR_TUNING, "idle",
                        coord[0] * LAYOUT_SCALE_X, coord[1] * LAYOUT_SCALE_Y));
            }
        }

        private void addMapDecorations() {
            List<MapObjectPlacement> placements = new ArrayList<>();
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_1, 20, 350, 110, 90));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_2, 310, 15, 110, 90));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_3, 620, 280, 120, 95));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_4, 880, 25, 110, 85));
            for (MapObjectPlacement p : placements) {
                MapDecorationActor actor = new MapDecorationActor(p.type, p.width, p.height, "idle");
                actor.setPosition(p.x * LAYOUT_SCALE_X, p.y * LAYOUT_SCALE_Y);
                actor.setTouchable(Touchable.disabled);
                addActor(actor);
            }

            float dangerIslandW = 220f;
            float dangerIslandH = 150f;
            Image dangerIsland = new Image(getTextureDrawable(MapObjectType.DANGER_BACKGROUND_ISLAND.getPath(),
                    (int) dangerIslandW, (int) dangerIslandH, Color.WHITE));
            dangerIsland.setSize(dangerIslandW, dangerIslandH);
            dangerIsland.setPosition(dangerNodeAnchorX - dangerIslandW / 2f, dangerNodeAnchorY - dangerIslandH / 2f);
            dangerIsland.setTouchable(Touchable.disabled);
            addActor(dangerIsland);

            Group houseIslandGroup = createAnchoredAnimation(MapObjectType.DECOR_HOUSE_ISLAND,
                    HOUSE_ISLAND_TUNING, "idle", houseX, houseY);
            houseIslandGroup.setTouchable(Touchable.enabled);
            houseIslandGroup.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    runCommand("menu enter greenhouse");
                }
            });
            addActor(houseIslandGroup);

            addActor(createAnchoredAnimation(MapObjectType.SPLASH_EFFECT_ANIM, SPLASH_TUNING, "idle",
                    houseX, houseY - 30f));
            if (chapterLevels.size() > 0) {
                addActor(createAnchoredAnimation(MapObjectType.SPLASH_EFFECT_ANIM, SPLASH_TUNING, "idle",
                        centerX[0], centerY[0] - 30f));
            }
        }

        private void addDangerNodeHitArea() {
            if (calculateDangerNodeState() == DangerNodeState.LOCKED_IDLE) return;
            float renderW = DANGER_NODE_TUNING.nativeW() * DANGER_NODE_TUNING.scale();
            float renderH = DANGER_NODE_TUNING.nativeH() * DANGER_NODE_TUNING.scale();
            addActor(createDangerNodeHitArea(
                    dangerNodeAnchorX - renderW / 2f + DANGER_NODE_TUNING.offsetX() * LAYOUT_SCALE_X,
                    dangerNodeAnchorY - renderH / 2f + DANGER_NODE_TUNING.offsetY() * LAYOUT_SCALE_Y,
                    renderW, renderH));
        }

        private void addForegroundEffects() {
            DangerNodeState dState = calculateDangerNodeState();
            Group dangerNode = createAnchoredAnimation(MapObjectType.DANGER_NODE_ANIM, DANGER_NODE_TUNING, dState.getPamState(), dangerNodeAnchorX, dangerNodeAnchorY);
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

            if (chapterLevels.size() >= 2) {
                addActor(createAnchoredAnimation(MapObjectType.WAVE_ANIM, WAVE_TUNING, "idle",
                        centerX[1] + 130f * LAYOUT_SCALE_X,
                        centerY[1] + 70f * LAYOUT_SCALE_Y));
            }
        }

        private void buildNode(Level level, int index, int stageNumber) {
            boolean boss = (level instanceof BossLevel) || (index == chapterLevels.size() - 1);
            StageStatus status = statusOf(level);
            LevelNodeState nodeState = levelNodeStateOf(index, status);

            if (boss) {
                String zombossState = (status == StageStatus.COMPLETED) ? "defeated" : "defeated";
                addActor(createAnchoredAnimation(MapObjectType.ZOMBOSS_NODE, ZOMBOSS_TUNING, zombossState,
                        centerX[index], centerY[index]));
                addActor(createAnchoredAnimation(MapObjectType.ZOMBOSS_HOLOGRAM, ZOMBOSS_HOLOGRAM_TUNING, "idle",
                        centerX[index] - 10f, centerY[index] - 50f));
            } else {
                float width = NODE_WIDTH;
                float height = NODE_HEIGHT;
                String islandPath = STAGE_ISLAND_TEXTURES[index % STAGE_ISLAND_TEXTURES.length];
                Image islandImage = new Image(getTextureDrawable(islandPath, (int) width, (int) height, new Color(0.2f, 0.55f, 0.75f, 1f)));
                islandImage.setSize(width, height);
                islandImage.setPosition(centerX[index] - width / 2f, centerY[index] - height / 2f);
                islandImage.setTouchable(Touchable.disabled);
                addActor(islandImage);
            }

            float labelWidth = boss ? 200f : NODE_WIDTH;
            float labelHeight = boss ? 60f : NODE_HEIGHT;

            Stack stack = new Stack();
            stack.setSize(labelWidth, labelHeight);

            Label numberLabel = new Label(boss ? "BOSS" : String.valueOf(stageNumber), skin, "title");
            numberLabel.setAlignment(Align.center);
            stack.add(numberLabel);

            Table column = new Table();
            column.add(stack).size(labelWidth, labelHeight).row();

            Label nameLabel = new Label(level.getName(), skin, status == StageStatus.LOCKED ? "muted" : "main");
            nameLabel.setAlignment(Align.center);
            nameLabel.setFontScale(0.85f);
            nameLabel.setWrap(true);
            column.add(nameLabel).width(labelWidth + 40f).padTop(2);

            column.pack();

            float offsetX = boss ? -20f : 0f;
            float offsetY = boss ? -10f : 0f;

            column.setPosition(centerX[index] + offsetX - column.getWidth() / 2f,
                    centerY[index] + offsetY - column.getHeight() / 2f);

            if (status != StageStatus.LOCKED) {
                column.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        runCommand("select stage -s " + level.getId());
                    }
                });
            }

            addActor(column);

            DecorTuning nodeTuning = boss ? BOSS_LEVEL_NODE_TUNING : LEVEL_NODE_TUNING;
            addActor(createAnchoredAnimation(MapObjectType.LEVEL_NODE, nodeTuning, nodeState.getPamState(),
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
                Color oldColor = batch.getColor().cpy();

                batch.setColor(
                        0.35f * oldColor.r,
                        0.65f * oldColor.g,
                        0.80f * oldColor.b,
                        0.8f * oldColor.a * parentAlpha
                );

                if (centerX.length > 0) {
                    drawSegment(batch, houseX, houseY, centerX[0], centerY[0]);
                }

                for (int i = 0; i < centerX.length - 1; i++) {
                    if (i == 1 && centerX.length >= 3) {
                        drawSegment(batch, centerX[1], centerY[1], dangerNodeAnchorX, dangerNodeAnchorY);
                        drawSegment(batch, dangerNodeAnchorX, dangerNodeAnchorY, centerX[2], centerY[2]);
                    } else if (i != 1 || centerX.length < 3) {
                        drawSegment(batch, centerX[i], centerY[i], centerX[i + 1], centerY[i + 1]);
                    }
                }

                batch.setColor(oldColor);
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
        private Texture fallbackTexture;
        private String pamState;
        private float stateTime = 0f;
        private float animDuration = 0f;

        public MapDecorationActor(MapObjectType objectType, float width, float height, String state) {
            this.objectType = objectType;
            if (objectType == MapObjectType.ZOMBOSS_HOLOGRAM) {
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
            } else if (objectType.getFallbackPath() != null
                    && Gdx.files.internal(objectType.getFallbackPath()).exists()) {
                fallbackTexture = new Texture(Gdx.files.internal(objectType.getFallbackPath()));
                fallbackTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            stateTime += delta;
            if (objectType == MapObjectType.ZOMBOSS_HOLOGRAM && stateTime >= animDuration) {
                stateTime = 0f;
                this.pamState = HOLOGRAM_STATES[(int) (Math.random() * HOLOGRAM_STATES.length)];
                this.animDuration = 3f + (float) (Math.random() * 3f);
            }
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            Color oldColor = batch.getColor().cpy();

            if (!objectType.isPamAnimation() && texture != null) {
                batch.setColor(oldColor.r, oldColor.g, oldColor.b, oldColor.a * parentAlpha);
                batch.draw(texture, getX(), getY(), getWidth(), getHeight());
            } else {
                boolean drewPam = false;
                if (objectType.isPamAnimation() && pamPlayer != null) {
                    try {
                        ClipRef clip = null;
                        if (pamState != null && !pamState.isEmpty()) {
                            clip = pamPlayer.getClip(objectType.getPath(), pamState);
                        }
                        if (clip == null) {
                            clip = pamPlayer.getClip(objectType.getPath(), "idle");
                        }
                        if (clip == null) {
                            clip = pamPlayer.getClip(objectType.getPath(), "defeated");
                        }
                        if (clip == null) {
                            clip = pamPlayer.getClip(objectType.getPath(), "default");
                        }
                        if (clip == null) {
                            clip = pamPlayer.getClip(objectType.getPath(), "");
                        }

                        if (clip != null) {
                            pamPlayer.draw(batch, clip, stateTime, getX(), getY(), true);
                            drewPam = true;
                        }
                    } catch (Throwable ignored) {
                    }
                }

                if (!drewPam && fallbackTexture != null) {
                    if (objectType.getFallbackTint() != null) {
                        Color t = objectType.getFallbackTint();
                        batch.setColor(t.r * oldColor.r, t.g * oldColor.g, t.b * oldColor.b, t.a * oldColor.a * parentAlpha);
                    } else {
                        batch.setColor(oldColor.r, oldColor.g, oldColor.b, oldColor.a * parentAlpha);
                    }
                    batch.draw(fallbackTexture, getX(), getY(), getWidth(), getHeight());
                }
            }

            batch.setColor(oldColor);
        }
    }
}