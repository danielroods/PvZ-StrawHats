package view.screens.stages_screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import controller.menus.match.MatchMenu;
import model.match.main.levels.Level;
import model.match.main.levels.special_levels.BossLevel;
import model.user_data.User;
import model.utils.LevelLoader;
import model.utils.LevelProgression;

import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.textures.TextureBank;

import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.general_screens.UiScreen;

import java.util.ArrayList;
import java.util.List;



public class BigWaveBeachStagesScreen extends StagesScreen {

    private static final String CHAPTER_NAME = "Big Wave Beach";

    private static final String BACK_ICON = "images/ui/buttons_hud_back_normal.png";
    private static final String COLLECTION_ICON = "images/ui/collection.png";
    private static final String GREENHOUSE_ICON = "images/ui/greenhouse.png";
    private static final String LEADERBOARD_ICON = "images/ui/leaderboard.png";
    private static final String COIN_ICON = "images/ui/buttons_coin_buy_normal.png";
    private static final String GEM_ICON = "images/ui/buttons_premium_normal.png";

    private static final String CHAPTER_BACKGROUND = "images/backg/beaches_stages.png";

    private static final String[] STAGE_ISLAND_TEXTURES = {
            "images/chapters/beach/anim12_335x420.png",
            "images/chapters/beach/anim13_397x399.png",
            "images/chapters/beach/anim17_321x255.png"
    };
    private static final String BOSS_STAGE_ISLAND_TEXTURE = "images/chapters/beach/boss.png";

    private static final float NODE_WIDTH = 125f;
    private static final float NODE_HEIGHT = 95f;

    private static final float BOSS_NODE_WIDTH = 350f;
    private static final float BOSS_NODE_HEIGHT = 400f;

    private static final float PATH_WIDTH = 1700f;
    private static final float PATH_HEIGHT = 700f;

    private static final float LAYOUT_SCALE_X = PATH_WIDTH / 1080f;
    private static final float LAYOUT_SCALE_Y = PATH_HEIGHT / 380f;

    private static final DecorTuning HOUSE_ISLAND_TUNING = new DecorTuning(20f, 400f, 0.10f, -60f, 0f);
    private static final DecorTuning LEVEL_NODE_TUNING = new DecorTuning(260f, 260f, 0.34f, -25f, 0f);
    private static final DecorTuning BOSS_LEVEL_NODE_TUNING = new DecorTuning(260f, 260f, 0.45f, -7f, -15f);
    private static final DecorTuning DANGER_NODE_TUNING = new DecorTuning(350f, 300f, 0.50f, 70f, -215f);
    private static final DecorTuning ZOMBOSS_TUNING = new DecorTuning(560f, 760f, 0.30f, 37f, 140f);
    private static final DecorTuning WAVE_TUNING = new DecorTuning(90f, 180f, 0.30f, -120f, -60f);
    private static final DecorTuning ROCK_TUNING = new DecorTuning(100f, 100f, 0.22f, 0f, 0f);
    private static final DecorTuning SPLASH_TUNING = new DecorTuning(200f, 150f, 0.50f, 0f, 60f);
    private static final DecorTuning STAR_TUNING = new DecorTuning(25f, 25f, 0.30f, 0f, 0f);

    private static final DecorTuning WATER_DROP_TUNING = new DecorTuning(100f, 100f, 0.40f, 0f, -20f);
    private static final DecorTuning WATERFALL_TUNING = new DecorTuning(200f, 250f, 0.22f, 20f, 0f);
    private static final DecorTuning LARGE_ROCK_BEACH_TUNING = new DecorTuning(180f, 180f, 0.25f, -40f, -30f);
    private static final DecorTuning SMALL_ROCK_BEACH_TUNING = new DecorTuning(120f, 120f, 0.20f, -10f, 10f);

    private List<Level> allLevels = new ArrayList<>();
    private List<Level> chapterLevels = new ArrayList<>();

    private Label selectionLabel;
    private TextButton playButton;

    private TextureBank textureBank;
    private PamPlayer pamPlayer;

    public enum DangerNodeState {
        LOCKED_IDLE("locked_idle"),
        UNLOCKED_ANIMATION("unlocked_animation"),
        UNLOCKED_IDLE("unlocked_idle");

        private final String pamState;

        DangerNodeState(String pamState) {
            this.pamState = pamState;
        }

        public String getPamState() {
            return pamState;
        }
    }

    public enum LevelNodeState {
        LOCKED_IDLE("locked_idle"),
        LOCKED_ANIMATION("locked_animation"),
        UNLOCKED("unlocked"),
        UNLOCKED_ANIMATION("unlocked_animation"),
        FINISHED("finished");

        private final String pamState;

        LevelNodeState(String pamState) {
            this.pamState = pamState;
        }

        public String getPamState() {
            return pamState;
        }
    }

    public enum MapObjectType {
        DECOR_HOUSE_ISLAND("768/FULL/WORLDMAP/BEACH/ANIM27/ANIM27.PAM", true),

        SMALL_ISLAND_1("768/FULL/WORLDMAP/DINO/ANIM16/ANIM16.PAM", true),
        SMALL_ISLAND_2("768/FULL/WORLDMAP/BEACH/ANIM6/ANIM6.PAM", true),
        SMALL_ISLAND_3("images/chapters/beach/island42.png", false),
        SMALL_ISLAND_4("images/chapters/beach/island41.png", false),
        SMALL_ISLAND_5("images/chapters/beach/img_1.png", false),

        ZOMBOSS_NODE("768/FULL/WORLDMAP/BEACH/ANIM15/ANIM15.PAM", true),
        LEVEL_NODE("768/INITIAL/WORLDMAP/LEVEL_NODE/LEVEL_NODE.PAM", true),

        FLOATING_ROCK_ANIM_1("768/FULL/WORLDMAP/BEACH/ANIM19/ANIM19.PAM", true),
        FLOATING_ROCK_ANIM_2("768/FULL/WORLDMAP/BEACH/ANIM20/ANIM20.PAM", true),
        FLOATING_ROCK_ANIM_3("768/FULL/WORLDMAP/BEACH/ANIM18/ANIM18.PAM", true),

        DANGER_NODE_ANIM("768/FULL/WORLDMAP/DANGER_NODE_BEACH/DANGER_NODE_BEACH.PAM", true),

        WAVE_ANIM("768/FULL/WORLDMAP/FUTURE/ANIM4/ANIM4.PAM", true),
        TWINKLING_STAR_ANIM("768/FULL/UI/JOUST/SPINNING_GOLD_STAR/SPINNING_GOLD_STAR.PAM", true),
        SPLASH_EFFECT_ANIM("768/FULL/EFFECTS/WATER_SPLASH/WATER_SPLASH.PAM", true),

        WATER_DROP_ANIM("768/FULL/WORLDMAP/BEACH/ANIM35/ANIM35.PAM", true),
        WATERFALL_ANIM("768/FULL/WORLDMAP/BEACH/ANIM32/ANIM32.PAM", true),
        FLOATING_ROCK_BEACH_LARGE_1("768/FULL/WORLDMAP/BEACH/ANIM16/ANIM16.PAM", true),
        FLOATING_ROCK_BEACH_LARGE_2("768/FULL/WORLDMAP/BEACH/ANIM10/ANIM10.PAM", true),
        SMALL_ROCK_BEACH_1("768/FULL/WORLDMAP/BEACH/ANIM4/ANIM4.PAM", true),
        SMALL_ROCK_BEACH_2("768/FULL/WORLDMAP/BEACH/ANIM5/ANIM5.PAM", true);

        private final String path;
        private final boolean isPamAnimation;

        MapObjectType(String path, boolean isPamAnimation) {
            this.path = path;
            this.isPamAnimation = isPamAnimation;
        }

        public String getPath() { return path; }
        public boolean isPamAnimation() { return isPamAnimation; }
    }

    public static class MapObjectPlacement {
        MapObjectType type;
        float x, y;
        float width, height;

        public MapObjectPlacement(MapObjectType type, float x, float y, float width, float height) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    @Override
    public void show() {
        if (CHAPTER_BACKGROUND != null && !CHAPTER_BACKGROUND.isEmpty() && Gdx.files.internal(CHAPTER_BACKGROUND).exists()) {
            setBackground(CHAPTER_BACKGROUND);
        }
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);

        if (textureBank == null) {
            try {
                FileHandle rootHandle = Gdx.files.internal("assets/pvz-assets");
                textureBank = new TextureBank("atlases", rootHandle);
                pamPlayer = new PamPlayer(textureBank, rootHandle);

                Gdx.app.log("PAM_INIT", "PAM System and TextureBank initialized successfully!");
            } catch (Throwable t) {
                Gdx.app.error("PAM_INIT", "Failed to initialize PAM System", t);
            }
        }

        super.show();
        loadLevels();
        build();
    }
    private static final String SPLASH_PARTICLE_PATH = "assets/images/chapters/beach/seashooter_projectile_28x28.png";

    @Override
    public void initParticles() {
        if (particles != null) {
            particles.dispose();
        }
        particlePaths = new String[]{ SPLASH_PARTICLE_PATH };

        particles = new view.general_screens.ParticleCreator(particlePaths, 20, 20f, 35f, 1.2f, true);

        com.badlogic.gdx.scenes.scene2d.Actor particleActor = particles.createActor();
        particleActor.setTouchable(Touchable.disabled);
        rootStack.addActorAt(1, particleActor);
    }

    @Override
    public void hide() {
        super.hide();
        super.initParticles();
    }

    @Override
    public void render(float delta) {
        if (textureBank != null) {
            try {
                textureBank.update();
            } catch (Throwable t) {
            }
        }
        super.render(delta);
    }

    private void loadLevels() {
        try {
            allLevels = LevelProgression.sorted(LevelLoader.loadLevels());
        } catch (Exception e) {
            allLevels = new ArrayList<>();
        }
        if (allLevels != null) {
            chapterLevels = allLevels.stream()
                    .filter(level -> level != null && level.getSeason() != null && level.getSeason().getName() != null)
                    .filter(level -> level.getSeason().getName().equalsIgnoreCase(CHAPTER_NAME))
                    .toList();
        } else {
            chapterLevels = new ArrayList<>();
        }
    }

    /**
     * The danger node is a hidden, unlisted level of this chapter: it reuses the
     * chapter's own season (so it plays through the exact same gameplay screen as
     * every other stage here) plus the first stage's basic settings as a starting
     * point. No special gameplay logic yet.
     */
    private Level buildDangerLevel() {
        model.match.main.levels.normal_levels.NormalLevel level = new model.match.main.levels.normal_levels.NormalLevel();
        Level base = chapterLevels.isEmpty() ? null : chapterLevels.get(0);
        level.setId(base != null ? -(1_000_000 + base.getId()) : -1_000_000);
        level.setName(CHAPTER_NAME + " Lottery");
        level.setGameMode("Lottery");
        if (base != null) {
            level.setSeason(base.getSeason());
            level.setRows(base.getRows());
            level.setCols(base.getCols());
            level.setInitialSun(base.getInitialSun());
            level.setAvailablePlants(base.getAvailablePlants());
            level.setForcedPlants(base.getForcedPlants());
            level.setZombiePool(base.getZombiePool());
            level.setWaves(base.getWaves());
        }
        return level;
    }

    private void playDangerNode() {
        MatchMenu.selectedLevel = buildDangerLevel();
        runCommand("start game");
    }

    private void build() {
        rootTable.clear();

        Table topBar = buildTopBar();
        Table pathContainer = buildPathContainer();
        Table selectionBar = buildSelectionBar();

        rootTable.add(topBar).fillX().padTop(5).padLeft(15).padRight(15).row();
        rootTable.add(pathContainer).expand().padTop(SPACE_SM).row();
        rootTable.add(selectionBar).fillX().padBottom(SPACE_SM);

        topBar.toFront();
        selectionBar.toFront();
    }

    private Table buildTopBar() {
        ImageButton backBtn = createIconButton(BACK_ICON, 54, 54, () -> runCommand("menu exit"));

        Table topLeft = new Table();
        topLeft.add(backBtn).padRight(16);
        topLeft.add(createIconButtonWithLabel(COLLECTION_ICON, 54, 54,
                "Collection", () -> runCommand("menu enter collection"))).padRight(16);
        topLeft.add(createIconButtonWithLabel(GREENHOUSE_ICON, 54, 54,
                "Greenhouse", () -> runCommand("menu greenhouse"))).padRight(16);
        topLeft.add(createIconButtonWithLabel(LEADERBOARD_ICON, 54, 54,
                "Leaderboard", () -> runCommand("menu leaderboard")));

        User user = User.currentUser;
        int coins = (user != null && user.userState != null) ? user.userState.coins : 0;
        int diamonds = (user != null && user.userState != null) ? user.userState.diamonds : 0;

        Table topRight = new Table();
        topRight.add(createResourceWidget(COIN_ICON, String.valueOf(coins))).padRight(15);
        topRight.add(createResourceWidget(GEM_ICON, String.valueOf(diamonds)));

        Table topBar = new Table();
        topBar.add(topLeft).left().expandX();
        topBar.add(topRight).right();
        return topBar;
    }









    private Table buildPathContainer() {
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

    private Table buildSelectionBar() {
        Table bar = new Table();
        bar.setBackground(skin.getDrawable("card-background"));
        bar.pad(14);

        selectionLabel = new Label("", skin, "main");
        selectionLabel.setWrap(true);

        playButton = primaryButton("Play", () -> runCommand("start game"));

        bar.add(selectionLabel).width(760).left().expandX();
        bar.add(playButton).width(180).height(56).padLeft(SPACE_LG);

        refreshSelectionBar();
        return bar;
    }

    private void refreshSelectionBar() {
        Level selected = MatchMenu.selectedLevel;
        boolean isBigWaveBeachSelection = selected != null && selected.getSeason() != null && selected.getSeason().getName().equalsIgnoreCase(CHAPTER_NAME);
        if (isBigWaveBeachSelection) {
            selectionLabel.setText(selected.getName() + "\n" + selected.getGameMode());
            playButton.setDisabled(false);
            playButton.setTouchable(Touchable.enabled);
        } else {
            selectionLabel.setText("Tap an unlocked stage to select it.");
            playButton.setDisabled(true);
            playButton.setTouchable(Touchable.disabled);
        }
    }

    @Override
    protected void onAfterCommand() {
        build();
    }

    private enum StageStatus { LOCKED, UNLOCKED, COMPLETED, CURRENT }

    private StageStatus statusOf(Level level) {
        int lastLevel = (User.currentUser != null && User.currentUser.userState != null)
                ? User.currentUser.userState.lastLevel : 0;
        Level selected = MatchMenu.selectedLevel;
        if (selected != null && selected.getId() == level.getId()) {
            return StageStatus.CURRENT;
        }
        if (LevelProgression.isCompleted(allLevels, lastLevel, level)) {
            return StageStatus.COMPLETED;
        }
        if (LevelProgression.isUnlocked(allLevels, lastLevel, level)) {
            return StageStatus.UNLOCKED;
        }
        return StageStatus.LOCKED;
    }

    private class StagePath extends Group {

        private final float[] centerX;
        private final float[] centerY;
        private final float houseX;
        private final float houseY;
        private final float zombossNodeX;
        private final float zombossNodeY;
        private final float dangerNodeAnchorX;
        private final float dangerNodeAnchorY;
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
                dangerNodeAnchorX = centerX[1] + DANGER_NODE_TUNING.offsetX() * LAYOUT_SCALE_X;
                dangerNodeAnchorY = centerY[1] + 120f * LAYOUT_SCALE_Y;
            } else {
                dangerNodeAnchorX = PATH_WIDTH / 2f;
                dangerNodeAnchorY = PATH_HEIGHT / 2f;
            }
            addBackgroundDecorations();

            addActor(new TrailActor());

            addMapDecorations();

            for (int i = 0; i < count; i++) {
                buildNode(chapterLevels.get(i), i, i + 1);
            }

            addForegroundEffects();

            if (count == 0) {
                Label empty = new Label("No Big Wave Beach stages found.", skin, "muted");
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

            MapDecorationActor actor = new MapDecorationActor(type, nativeWidth, nativeHeight, state);
            actor.setSize(nativeWidth, nativeHeight);
            group.addActor(actor);
            return group;
        }

        private Group createScaledAnimation(MapObjectType type, float nativeWidth, float nativeHeight, String state, float scale, float x, float y) {
            return createScaledAnimation(type, nativeWidth, nativeHeight, state, scale, x, y, false);
        }

        private Group createAnchoredAnimation(MapObjectType type, DecorTuning tuning, String state, float anchorX, float anchorY, boolean flipX) {
            float renderW = tuning.nativeW() * tuning.scale();
            float renderH = tuning.nativeH() * tuning.scale();
            float x = anchorX - renderW / 2f + tuning.offsetX() * LAYOUT_SCALE_X;
            float y = anchorY - renderH / 2f + tuning.offsetY() * LAYOUT_SCALE_Y;
            return createScaledAnimation(type, tuning.nativeW(), tuning.nativeH(), state, tuning.scale(), x, y, flipX);
        }

        private Group createAnchoredAnimation(MapObjectType type, DecorTuning tuning, String state, float anchorX, float anchorY) {
            return createAnchoredAnimation(type, tuning, state, anchorX, anchorY, false);
        }

        private void addBackgroundDecorations() {
            if (chapterLevels.size() > 0) {
                addActor(createAnchoredAnimation(MapObjectType.WATERFALL_ANIM, WATERFALL_TUNING, "idle",
                        centerX[0] - 40f * LAYOUT_SCALE_X, centerY[0] - 50f * LAYOUT_SCALE_Y, false));
            }

            float[][] floatingRocksBeach1 = {
                    {280f, 320f}, {670f, 110f}, {930f, 280f}
            };
            for (float[] coord : floatingRocksBeach1) {
                addActor(createAnchoredAnimation(MapObjectType.FLOATING_ROCK_BEACH_LARGE_1, LARGE_ROCK_BEACH_TUNING, "idle",
                        coord[0] * LAYOUT_SCALE_X, coord[1] * LAYOUT_SCALE_Y));
            }

            float[][] floatingRocksBeach2 = {
                    {190f, 90f}, {510f, 330f}, {820f, 130f}
            };
            for (float[] coord : floatingRocksBeach2) {
                addActor(createAnchoredAnimation(MapObjectType.FLOATING_ROCK_BEACH_LARGE_2, LARGE_ROCK_BEACH_TUNING, "idle",
                        coord[0] * LAYOUT_SCALE_X, coord[1] * LAYOUT_SCALE_Y));
            }

            float[][] starCoords = {
                    {110f, 45f}, {320f, 330f}, {540f, 50f},
                    {210f, 270f}, {460f, 190f}, {650f, 35f},
                    {380f, 85f}, {590f, 320f}, {830f, 175f}
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
                    {120f, 150f}, {350f, 450f},
                    {820f, 480f}, {1020f, 280f},
                    {250f, 550f}, {680f, 500f},
            };
            for (int i = 0; i < rockCoords.length; i++) {
                MapObjectType selectedRock = rockTypes[i % rockTypes.length];
                addActor(createAnchoredAnimation(selectedRock, ROCK_TUNING, "idle",
                        rockCoords[i][0] * LAYOUT_SCALE_X, rockCoords[i][1] * LAYOUT_SCALE_Y));
            }
        }

        private void addMapDecorations() {
            List<MapObjectPlacement> placements = new ArrayList<>();
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_1, 20, 350, 50, 38));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_2, 310, 15, 55, 40));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_3, 620, 280, 60, 45));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_4, 880, 25, 50, 35));
            placements.add(new MapObjectPlacement(MapObjectType.SMALL_ISLAND_5, 970, 240, 55, 40));

            for (MapObjectPlacement p : placements) {
                MapDecorationActor actor = new MapDecorationActor(p.type, p.width, p.height, "idle");
                actor.setPosition(p.x * LAYOUT_SCALE_X, p.y * LAYOUT_SCALE_Y);
                addActor(actor);
            }

            if (chapterLevels.size() > 0) {
                float[][] smallRocks1 = {
                        {centerX[0] - 65f * LAYOUT_SCALE_X, centerY[0] - 35f * LAYOUT_SCALE_Y},
                        {centerX[0] + 75f * LAYOUT_SCALE_X, centerY[0] + 40f * LAYOUT_SCALE_Y},
                        {400f * LAYOUT_SCALE_X, 160f * LAYOUT_SCALE_Y}
                };
                for (float[] coord : smallRocks1) {
                    addActor(createAnchoredAnimation(MapObjectType.SMALL_ROCK_BEACH_1, SMALL_ROCK_BEACH_TUNING, "idle",
                            coord[0], coord[1]));
                }
            }

            if (chapterLevels.size() > 1) {
                float[][] smallRocks2 = {
                        {centerX[1] - 70f * LAYOUT_SCALE_X, centerY[1] - 40f * LAYOUT_SCALE_Y},
                        {centerX[1] + 70f * LAYOUT_SCALE_X, centerY[1] + 35f * LAYOUT_SCALE_Y},
                        {zombossNodeX - 80f * LAYOUT_SCALE_X, zombossNodeY + 40f * LAYOUT_SCALE_Y}
                };
                for (float[] coord : smallRocks2) {
                    addActor(createAnchoredAnimation(MapObjectType.SMALL_ROCK_BEACH_2, SMALL_ROCK_BEACH_TUNING, "idle",
                            coord[0], coord[1]));
                }
            }

            if (chapterLevels.size() > 2) {
                addActor(createAnchoredAnimation(MapObjectType.SMALL_ROCK_BEACH_1, SMALL_ROCK_BEACH_TUNING, "idle",
                        centerX[2] + 80f * LAYOUT_SCALE_X, centerY[2] - 35f * LAYOUT_SCALE_Y));
                addActor(createAnchoredAnimation(MapObjectType.SMALL_ROCK_BEACH_2, SMALL_ROCK_BEACH_TUNING, "idle",
                        centerX[2] - 65f * LAYOUT_SCALE_X, centerY[2] + 45f * LAYOUT_SCALE_Y));
            }

            float[][] waterDropCoords = {
                    {80f, 120f}, {160f, 340f}, {240f, 80f},
                    {380f, 310f}, {440f, 130f}, {510f, 260f},
                    {640f, 330f}, {710f, 180f}, {780f, 60f},
                    {900f, 140f}

            };
            for (float[] coord : waterDropCoords) {
                addActor(createAnchoredAnimation(MapObjectType.WATER_DROP_ANIM, WATER_DROP_TUNING, "idle",
                        coord[0] * LAYOUT_SCALE_X, coord[1] * LAYOUT_SCALE_Y));
            }

            Group houseIslandGroup = createAnchoredAnimation(MapObjectType.DECOR_HOUSE_ISLAND, HOUSE_ISLAND_TUNING, "idle", houseX + 50f, houseY + 20f);
            houseIslandGroup.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    runCommand("menu enter greenhouse");
                }
            });
            addActor(houseIslandGroup);
        }

        private void addForegroundEffects() {
            DangerNodeState dState = calculateDangerNodeState();
            String zombossState = (dState == DangerNodeState.UNLOCKED_IDLE) ? "defeated" : "idle";
            addActor(createAnchoredAnimation(MapObjectType.ZOMBOSS_NODE, ZOMBOSS_TUNING, zombossState, zombossNodeX, zombossNodeY));
            Group dangerNode = createAnchoredAnimation(MapObjectType.DANGER_NODE_ANIM, DANGER_NODE_TUNING, dState.getPamState(), dangerNodeAnchorX, dangerNodeAnchorY);
            if (dState != DangerNodeState.LOCKED_IDLE) {
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

            if (chapterLevels.size() > 0) {
                addActor(createAnchoredAnimation(MapObjectType.SPLASH_EFFECT_ANIM, SPLASH_TUNING, "idle", houseX + 45f, houseY + 20f));
                addActor(createAnchoredAnimation(MapObjectType.SPLASH_EFFECT_ANIM, SPLASH_TUNING, "idle", centerX[0], centerY[0] - 20f));
            }
            if (chapterLevels.size() >= 3) {
                addActor(createAnchoredAnimation(MapObjectType.SPLASH_EFFECT_ANIM, SPLASH_TUNING, "idle", centerX[2], centerY[2] - 20f));
            }

            if (chapterLevels.size() >= 2) {
                float waveOffsetX = 130f * LAYOUT_SCALE_X;
                float waveOffsetY = 70f * LAYOUT_SCALE_Y;
                addActor(createAnchoredAnimation(MapObjectType.WAVE_ANIM, WAVE_TUNING, "idle",
                        centerX[1] + waveOffsetX, centerY[1] + waveOffsetY));
            }
        }

        private DangerNodeState calculateDangerNodeState() {
            if (chapterLevels.size() >= 3) {
                StageStatus s2 = statusOf(chapterLevels.get(1));
                StageStatus s3 = statusOf(chapterLevels.get(2));
                if (s3 != StageStatus.LOCKED) {
                    return DangerNodeState.UNLOCKED_IDLE;
                } else if (s2 == StageStatus.COMPLETED) {
                    return DangerNodeState.UNLOCKED_ANIMATION;
                }
            }
            return DangerNodeState.LOCKED_IDLE;
        }

        private LevelNodeState levelNodeStateOf(int index, StageStatus status) {
            switch (status) {
                case COMPLETED:
                    return LevelNodeState.FINISHED;
                case CURRENT:
                    return LevelNodeState.UNLOCKED_ANIMATION;
                case UNLOCKED:
                    return LevelNodeState.UNLOCKED;
                case LOCKED:
                default:
                    boolean nextUp = index > 0 && statusOf(chapterLevels.get(index - 1)) != StageStatus.LOCKED;
                    return nextUp ? LevelNodeState.LOCKED_ANIMATION : LevelNodeState.LOCKED_IDLE;
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
            Image islandImage = new Image(getTextureDrawable(islandPath, (int) width, (int) height));
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

        private com.badlogic.gdx.scenes.scene2d.utils.Drawable getTextureDrawable(String path, int w, int h) {
            if (Gdx.files.internal(path).exists()) {
                return new TextureRegionDrawable(loadTextureSafe(path));
            }
            return circleDrawable(Math.min(w, h), new Color(0.2f, 0.55f, 0.75f, 1f), Color.WHITE, 2);
        }

        private com.badlogic.gdx.scenes.scene2d.utils.Drawable circleDrawable(int diameter, Color fill, Color border, int borderWidth) {
            Pixmap pixmap = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
            pixmap.setColor(border);
            pixmap.fillCircle(diameter / 2, diameter / 2, diameter / 2);
            pixmap.setColor(fill);
            pixmap.fillCircle(diameter / 2, diameter / 2, diameter / 2 - borderWidth);
            Texture texture = new Texture(pixmap);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            pixmap.dispose();
            return new TextureRegionDrawable(texture);
        }

        private class TrailActor extends Actor {
            private final TextureRegion pixel = whitePixelRegion();

            TrailActor() {
                setPosition(0f, 0f);
                setSize(PATH_WIDTH, PATH_HEIGHT);
            }

            @Override
            public void draw(Batch batch, float parentAlpha) {
                batch.setColor(0.35f, 0.65f, 0.80f, 0.8f);

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