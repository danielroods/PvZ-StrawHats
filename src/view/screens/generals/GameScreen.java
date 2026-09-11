package view.screens.generals;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import controller.cheat.CheatAccess;
import controller.match.BeforeMenu;
import model.App;
import model.collections.animations.AnimationFactory;
import model.collections.plant.Plant;
import model.collections.zombie.Zombie;
import model.game_exceptions.GameException;
import model.match.main.levels.special_levels.ConveyorBeltLevel;
import model.match_mechanisms.vector.Position;
import model.utils.GameSession;
import model.utils.GameSettings;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import service.GameClock;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.hud.LotteryMatchHud;
import view.hud.MatchHud;
import view.screens.match.after.MatchEndSequence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GameScreen extends UiScreen {
    protected static final float TILE_WIDTH = 96f;
    protected static final float TILE_HEIGHT = 96f;
    protected static float BOARD_X = 190f;
    protected static float BOARD_Y = 170f;

    protected  static final String ASSET_ROOT = "assets/images/chapters/";

    protected String seasonFolder = "egypt";

    public GameSession session;
    protected MatchHud hud;
    private LotteryMatchHud endlessHud;
    protected Texture boardTexture;
    public TextureRegion whitePixel;
    public TextureRegion bubbleTexture;

    protected TextureBank textureBank;
    protected PamPlayer pamPlayer;

    private final BoardLayout layout = new BoardLayout(this);
    private final GameScreenAssets assets = new GameScreenAssets(this);
    private final PamRenderer pam = new PamRenderer(this);
    private final BoardInteraction interaction = new BoardInteraction(this);
    private final SeasonOverlayRenderer overlays = new SeasonOverlayRenderer(this);
    private final FrostbiteRenderer frostbite = new FrostbiteRenderer(this);
    private final FutureRenderer future = new FutureRenderer(this);
    private final PlantRenderer plants = new PlantRenderer(this);
    private final ZombieRenderer zombies = new ZombieRenderer(this);
    private final EffectRenderer effects = new EffectRenderer(this);
    private final GroundItemRenderer groundItems = new GroundItemRenderer(this);
    private final MowerRenderer mowers = new MowerRenderer(this);
    private final MatchEndSequence matchEnd = new MatchEndSequence(this);
    private final ZombossRenderer zomboss = new ZombossRenderer(this);
    private final NukeEffect nukeEffect = new NukeEffect(this);
    private Image nukeFlashOverlay;



    private float screenShakeTime = 0f;
    private float screenShakeDuration = 0f;
    private float screenShakeStrength = 0f;


    private int lastDisplayedWaveCount = 0;
    private float waveBannerTime = 0f;
    private String waveBannerText = null;
    private final GlyphLayout waveBannerLayout = new GlyphLayout();

    private view.hud.ZombossDialogueBox zombossDialogue;

    private boolean zombossNpcVoicePlayed = false;


    private boolean lastSandStormActive = false;

    double tickAccumulator;
    boolean paused;
    public boolean matchFinished;
    private float sandStormAnimTime;
    private final Map<String, Float> clipTimes = new java.util.HashMap<>();

    static final int ROW_LAYER_BEHIND_PLANTS = -1;
    static final int ROW_LAYER_DEFAULT = 0;

    private static final class QueuedRowDraw {
        final int row;
        final int layer;
        final Runnable draw;

        QueuedRowDraw(int row, int layer, Runnable draw) {
            this.row = row;
            this.layer = layer;
            this.draw = draw;
        }
    }

    private final List<QueuedRowDraw> rowDrawQueue = new ArrayList<>();

    void queueRowDraw(int row, Runnable draw) {
        queueRowDraw(row, ROW_LAYER_DEFAULT, draw);
    }

    void queueRowDraw(int row, int layer, Runnable draw) {
        rowDrawQueue.add(new QueuedRowDraw(row, layer, draw));
    }

    private void flushRowDrawQueue() {
        if (rowDrawQueue.isEmpty()) return;

        rowDrawQueue.sort(Comparator.<QueuedRowDraw>comparingInt(q -> q.row)
                .thenComparingInt(q -> q.layer));
        for (QueuedRowDraw queued : rowDrawQueue) {
            queued.draw.run();
        }
        rowDrawQueue.clear();
    }

    @Override
    public void initParticles() {

    }

    @Override
    public void show() {
        super.show();
        session = GameSession.getInstance();
        AnimationFactory.autoInit();
        pam.initPam();
        assets.initBoardTexture();
        createHud();
        createBoardInput();
        initParticles();
        assets.initShovelTexture();
        assets.initFrostbiteTextures();
        zomboss.reset();
        zomboss.preload();
        createZombossDialogue();
        nukeEffect.preload();
        createNukeFlashOverlay();
        lastDisplayedWaveCount = session == null ? 0 : session.getWavesSpawnedCount();
        waveBannerTime = 0f;
        waveBannerText = null;
        screenShakeTime = 0f;
    }
    private void createNukeFlashOverlay() {
        nukeFlashOverlay = new Image(new TextureRegionDrawable(whitePixel));
        nukeFlashOverlay.setFillParent(true);
        nukeFlashOverlay.setTouchable(Touchable.disabled);
        nukeFlashOverlay.setColor(1f, 1f, 1f, 0f);
        stage.addActor(nukeFlashOverlay);
    }


    protected void triggerNukeCheat() {
        if (!CheatAccess.isEnabled()) return;
        nukeEffect.trigger();
    }

    protected void createZombossDialogue() {
        if (session == null || session.getZombossFight() == null) return;
        zombossDialogue = new view.hud.ZombossDialogueBox(skin);
        zombossDialogue.setAdvanceAction(() -> {
            model.match.boss.ZombossFight fight = session.getZombossFight();
            if (fight != null) fight.advanceDialogue();
        });
        zombossDialogue.showLine(null, 0, 0);
        addBeforeModal(zombossDialogue);
    }

    protected String getSeasonGameplayFolder() {
        return ASSET_ROOT + seasonFolder + "/gameplay/";
    }

    protected String getGameplayBackgroundPath() { return getSeasonGameplayFolder() + "map.png"; }

    protected String getGraveIconPath() {
        return getSeasonGameplayFolder() + "grave.png";
    }

    protected String[] getGameplayBackgroundLayers() {
        return ChapterLawnArt.backgroundLayers(seasonFolder, getSeasonGameplayFolder());
    }

    ChapterLawnArt.Insets boardInsets() {
        return ChapterLawnArt.insetsFor(seasonFolder);
    }

    protected boolean hasPaintedWater() {
        return isBeach() || isPirate();
    }

    protected void createHud() {
        hud = new MatchHud(skin);
        hud.setPamPlayer(pamPlayer);
        hud.setPlantSelection(this::selectPlant);
        hud.setConveyorPlantSelection(interaction::selectConveyorPlant);
        hud.setPlantDragRelease(interaction::handlePlantDragRelease);
        hud.setShovelAction(() -> interaction.armTool(BoardInteraction.Tool.SHOVEL));
        hud.setFoodAction(() -> interaction.armTool(BoardInteraction.Tool.FOOD));
        hud.setNukeAction(this::triggerNukeCheat);
        hud.setPauseAction(this::togglePause);
        hud.setSpeedAction(this::cycleGameSpeed);
        hud.setStartWavesAction(() -> runCommand("start zombie waves"));
        hud.setDebugAddSunAction(() -> runCommand("cheat add -n 25 suns"));
        hud.setDebugAddFoodAction(() -> runCommand("cheat add-plant-food"));
        addBeforeModal(hud);
        createEndlessHud();
    }

    private void createEndlessHud() {
        if (session == null || !session.isEndless()) return;
        endlessHud = new LotteryMatchHud(skin);
        addBeforeModal(endlessHud);
    }

    protected void createBoardInput() {
        interaction.createBoardInput();
    }

    BoardLayout layout() { return layout; }

    GameScreenAssets assets() { return assets; }

    PamRenderer pam() { return pam; }

    BoardInteraction interaction() { return interaction; }

    PlantRenderer plants() { return plants; }

    EffectRenderer effects() { return effects; }

    GroundItemRenderer groundItems() { return groundItems; }

    float boardWidth() { return layout.boardWidth(); }

    float boardHeight() { return layout.boardHeight(); }

    float boardFitScale() { return layout.boardFitScale(); }

    float cellY(double row) { return layout.cellY(row); }

    float tickAlpha() {
        double alpha = tickAccumulator / GameClock.SECONDS_PER_TICK;
        return (float) Math.max(0.0, Math.min(1.0, alpha));
    }

    @Override
    public void render(float delta) {
        if (textureBank != null) {
            try {
                textureBank.update();
            } catch (Throwable t) {
                if (GameSettings.get().isDebugMode()) {
                    Gdx.app.error("TEXTUREBANK_UPDATE_FAIL", "textureBank.update() threw", t);
                }
            }
        }

        if (!paused && !matchFinished && matchEnd.isIdle()) {
            List<Zombie> aliveBeforeTick = session == null ? java.util.Collections.emptyList() : new ArrayList<>(session.getZombies());
            List<Plant> alivePlantsBeforeTick = session == null ? java.util.Collections.emptyList() : new ArrayList<>(session.getPlants());
            tickAccumulator += delta * GameSettings.get().getGameSpeed();
            while (tickAccumulator >= GameClock.SECONDS_PER_TICK) {
                tickSession();
                tickAccumulator -= GameClock.SECONDS_PER_TICK;
                checkMatchEnd();
                if (matchFinished || !matchEnd.isIdle()) break;
            }
            if (session != null) {
                zombies.trackZombieDeaths(aliveBeforeTick);
                plants.trackExplodedPlants(alivePlantsBeforeTick);
            }
        } else if (!paused && !matchEnd.isIdle()) {
            matchEnd.advanceMatchEndSequence(delta);
        }

        if (matchFinished) return;

        if (!paused) nukeEffect.tick(delta);
        if (nukeFlashOverlay != null) {
            float a = nukeEffect.flashAlpha();
            nukeFlashOverlay.setColor(1f, 1f, 1f, a);
            nukeFlashOverlay.setVisible(a > 0f);
        }

        updateGameplayPresentation(delta);
        refreshHud(delta);
        refreshZombossDialogue();
        refreshSandStormAudio();

        Object camera = stage.getViewport().getCamera();
        OrthographicCamera orthoCamera = camera instanceof OrthographicCamera ? (OrthographicCamera) camera : null;
        float baseCameraX = 0f;
        float baseCameraY = 0f;
        float baseCameraZ = 0f;
        if (orthoCamera != null) {
            baseCameraX = orthoCamera.position.x;
            baseCameraY = orthoCamera.position.y;
            baseCameraZ = orthoCamera.position.z;
            applyScreenShake(orthoCamera);
            if (nukeEffect.isActive()) {
                orthoCamera.position.x += nukeEffect.shakeOffsetX();
                orthoCamera.position.y += nukeEffect.shakeOffsetY();
                orthoCamera.update();
            }
        }

        drawBoard(delta);
        stage.act(delta);

        if (controller.ScreenManager.getScreen() != this) {
            if (orthoCamera != null) {
                orthoCamera.position.set(baseCameraX, baseCameraY, baseCameraZ);
                orthoCamera.update();
            }
            return;
        }
        stage.draw();

        if (orthoCamera != null) {
            orthoCamera.position.set(baseCameraX, baseCameraY, baseCameraZ);
            orthoCamera.update();
        }
    }

    protected void tickSession() {
        session.tick();
    }

    protected List<String> loadoutPlants() {
        return new ArrayList<>(BeforeMenu.selectedPlants);
    }


    protected boolean isBeforeMatchPreview() {
        return false;
    }

    protected float reservedRightAreaWidth() {
        return 0f;
    }

    private void refreshZombossDialogue() {
        if (zombossDialogue == null || session == null) return;

        model.match.boss.ZombossFight fight = session.getZombossFight();
        if (fight == null || fight.getPhase() != model.match.boss.ZombossPhase.NPC_TALK) {
            zombossDialogue.showLine(null, 0, 0);
            zombossNpcVoicePlayed = false;
            return;
        }

        if (!zombossNpcVoicePlayed) {
            zombossNpcVoicePlayed = true;
            AudioManager.get().playSound(AudioEnum.SFX_ZOMBOSS_NPC);
        }

        zombossDialogue.showLine(
                fight.getDialogueLine(),
                fight.getDialogueIndex(),
                fight.getDialogueCount());
    }





    private void refreshSandStormAudio() {
        if (session == null) return;
        boolean active = session.isSandStormActive();
        if (active && !lastSandStormActive) {
            AudioManager.get().playSound(AudioEnum.SFX_SANDSTORM);
        }
        lastSandStormActive = active;
    }


    void triggerScreenShake(float strength, float duration) {
        if (strength <= 0f || duration <= 0f) return;
        screenShakeStrength = Math.max(screenShakeStrength, strength);
        screenShakeDuration = Math.max(screenShakeDuration, duration);
        screenShakeTime = Math.max(screenShakeTime, duration);
    }

    private void updateGameplayPresentation(float delta) {
        if (screenShakeTime > 0f) {
            screenShakeTime = Math.max(0f, screenShakeTime - delta);
            if (screenShakeTime <= 0f) {
                screenShakeDuration = 0f;
                screenShakeStrength = 0f;
            }
        }

        int waveCount = session == null ? 0 : session.getWavesSpawnedCount();
        if (waveCount > lastDisplayedWaveCount) {
            lastDisplayedWaveCount = waveCount;
            waveBannerText = "Wave " + waveCount + " Started!";
            waveBannerTime = 1.45f;
        }
        if (waveBannerTime > 0f) {
            waveBannerTime = Math.max(0f, waveBannerTime - delta);
        }
    }

    private void applyScreenShake(OrthographicCamera camera) {
        if (screenShakeTime <= 0f || screenShakeDuration <= 0f) return;
        float progress = Math.max(0f, Math.min(1f, screenShakeTime / screenShakeDuration));
        float fade = progress * progress;
        float x = (float) (Math.random() * 2.0 - 1.0) * screenShakeStrength * fade;
        float y = (float) (Math.random() * 2.0 - 1.0) * screenShakeStrength * fade;
        camera.position.x += x;
        camera.position.y += y;
        camera.update();
    }

    private void drawWaveBanner() {
        if (waveBannerText == null || waveBannerTime <= 0f || skin == null) return;
        BitmapFont font = skin.has("default-font", BitmapFont.class)
                ? skin.getFont("default-font") : null;
        if (font == null) return;

        float alpha = Math.min(1f, waveBannerTime / 0.22f);
        if (waveBannerTime > 1.15f) alpha = 1f;
        float oldScaleX = font.getData().scaleX;
        float oldScaleY = font.getData().scaleY;
        font.getData().setScale(1.65f);
        font.getColor().set(1f, 0f, 0f, alpha);
        waveBannerLayout.setText(font, waveBannerText);

        float cx = stage.getViewport().getWorldWidth() * 0.5f;
        float cy = stage.getViewport().getWorldHeight() * 0.72f;
        float x = cx - waveBannerLayout.width * 0.5f;
        float y = cy + waveBannerLayout.height * 0.5f;



        font.getColor().set(0f, 0f, 0f, alpha);
        float d = 2.5f;
        font.draw(batch, waveBannerText, x - d, y);
        font.draw(batch, waveBannerText, x + d, y);
        font.draw(batch, waveBannerText, x, y - d);
        font.draw(batch, waveBannerText, x, y + d);
        font.draw(batch, waveBannerText, x - d, y - d);
        font.draw(batch, waveBannerText, x + d, y + d);
        font.draw(batch, waveBannerText, x - d, y + d);
        font.draw(batch, waveBannerText, x + d, y - d);

        font.getColor().set(1f, 0f, 0f, alpha);
        font.draw(batch, waveBannerText, x, y);
        font.getColor().set(Color.WHITE);
        font.getData().setScale(oldScaleX, oldScaleY);
    }

    protected void refreshHud(float delta) {
        if (hud == null || session == null) return;
        List<String> loadout = loadoutPlants();
        if (interaction.selectedPlant() != null && !loadout.contains(interaction.selectedPlant())
                && !(session.getLevel() instanceof ConveyorBeltLevel)) {
            interaction.clearSelectedPlant();
        }
        hud.setSelectedPlant(interaction.selectedPlant());
        hud.setTools(interaction.activeTool() == BoardInteraction.Tool.SHOVEL,
                interaction.activeTool() == BoardInteraction.Tool.FOOD);
        refreshEndlessHud();
        hud.update(session, loadout);
    }

    private void refreshEndlessHud() {
        if (endlessHud == null) return;
        model.match.endless.EndlessRun run = session.getEndlessRun();
        int waveNumber = session.getWavesSpawnedCount();
        endlessHud.update(run, waveNumber, endlessRecord());
        hud.setProgressOverride("WAVE " + Math.max(1, waveNumber),
                (float) session.getWaveProgress());
    }

    private long endlessRecord() {
        if (!(session.getLevel() instanceof model.match.endless.EndlessLevel level)) return -1L;
        if (model.user_data.User.currentUser == null
                || model.user_data.User.currentUser.userState == null) {
            return -1L;
        }
        String key = level.getChapter().key();
        model.user_data.UserState state = model.user_data.User.currentUser.userState;
        return state.hasLotteryScore(key) ? state.getLotteryHighScore(key) : -1L;
    }

    protected void selectPlant(String plantName) {
        interaction.selectPlant(plantName);
    }

    protected void onCellClicked(int row, int col) {
        interaction.plantAtCell(row, col);
    }

    protected boolean collectUnderMouse(Vector2 click) {
        return interaction.collectUnderMouse(click);
    }

    protected model.collections.item.GroundItem itemUnderMouse(Vector2 click) {
        return interaction.itemUnderMouse(click);
    }

    @Override
    protected boolean notificationsEnabled() {
        return false;
    }

    public boolean runCommand(String command) {
        try {
            App.currentMenu.handleCommand(command);
            return true;
        } catch (GameException e) {
            return false;
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Command failed: " + command, e);
            return false;
        }
    }

    protected void checkMatchEnd() {
        matchEnd.checkMatchEnd();
    }

    protected void onZombieDied(Zombie zombie) {
    }

    protected void trackZombieDeaths(List<Zombie> aliveBefore) {
        zombies.trackZombieDeaths(aliveBefore);
    }

    protected void trackPlantDeaths(List<Plant> alivePlantsBefore) {
        plants.trackExplodedPlants(alivePlantsBefore);
    }

    protected void startMatchEndSequence(boolean won) {
        matchEnd.startMatchEndSequence(won);
    }

    protected boolean isMatchEndSequenceIdle() {
        return matchEnd.isIdle();
    }

    public boolean isMatchEndSequenceActive() {
        return !matchEnd.isIdle();
    }

    public void onMatchEndSequenceFinished(boolean won) {
        runCommand(won ? "end game -r win" : "end game -r lose");
    }

    private void cycleGameSpeed() {
        GameSettings settings = GameSettings.get();
        int next = settings.getGameSpeed() >= 3 ? 1 : settings.getGameSpeed() + 1;
        settings.setGameSpeed(next);
        settings.save();
    }

    private void togglePause() {
        if (matchFinished) return;
        if (paused) return;
        paused = true;
        new PauseModal(this).show();
    }

    private void drawBoard(float delta) {
        Gdx.gl.glClearColor(0.03f, 0.03f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        batch.begin();

        layout.updateBoardLayout();
        float bw = boardWidth();
        float bh = boardHeight();
        overlays.drawBackground(bw, bh);
        overlays.drawTiles(bw, bh);
        frostbite.drawFrostbiteTileArt(delta);
        future.drawLinkTileArt(delta);
        drawSeasonGameplayEffects(delta, bw, bh);
        overlays.drawSpecialEffects(delta, bw, bh);
        zomboss.drawBackdrop();
        overlays.drawGraves();
        effects.drawScorchedTileEffects(delta);
        effects.drawHotPotatoMeltEffects(delta);
        plants.drawPlants(delta, bw, bh);
        effects.drawExplodingPlantEffects(delta);
        zombies.drawZombies(delta, bw, bh);
        zombies.drawDyingZombies(delta);
        effects.drawForegroundEffects(delta);
        groundItems.drawGroundItems(delta, bw, bh);
        effects.drawProjectiles(delta, bw, bh);
        mowers.drawMowers(bw, bh);
        frostbite.drawFrostbiteIceBlocks(delta);
        flushRowDrawQueue();

        zomboss.drawBoss();
        zomboss.drawEffects(delta);
        nukeEffect.drawMissile();
        overlays.drawLawnGrid(bw, bh);
        interaction.drawHover(bw, bh);
        drawSeasonForegroundEffects(delta, bw, bh);
        zomboss.drawNpc();
        zomboss.drawNarrationIcon();
        interaction.drawDragPreview(delta);
        matchEnd.drawMatchEndOverlay();
        drawMatchStartOverlay();
        drawWaveBanner();

        batch.end();
    }

    /**
     * Hook for a pre-match splash (e.g. the "VS" icon shown right as a networked match
     * begins). No-op by default; drawn last, on top of everything else in the board
     * batch, right after {@link MatchEndSequence#drawMatchEndOverlay()}. Override and
     * pair with a helper that draws via {@link #batch}/{@link #whitePixel} the same way
     * {@code MatchEndSequence} does.
     */
    protected void drawMatchStartOverlay() {
    }

    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {

    }

    protected boolean areLawnMowersVisible() {
        return true;
    }

    protected void drawSeasonForegroundEffects(float delta, float bw, float bh) {
        frostbite.drawSeasonForegroundEffects(delta, bw, bh);
    }

    protected boolean isBeach() {
        return session != null && session.getLevel() != null
                && session.getLevel().getSeason() != null
                && "Big Wave Beach".equalsIgnoreCase(session.getLevel().getSeason().getName());
    }

    protected boolean isPirate() {
        return session != null && session.getLevel() != null
                && session.getLevel().getSeason() != null
                && "Pirates".equalsIgnoreCase(session.getLevel().getSeason().getName());
    }

    protected boolean isIceAge() {
        return session != null && session.getLevel() != null
                && session.getLevel().getSeason() != null
                && "Frostbite Caves".equalsIgnoreCase(session.getLevel().getSeason().getName());
    }

    protected float getBoardTileWidth() { return layout.boardTileWidth(); }
    protected float getBoardTileHeight() { return layout.boardTileHeight(); }
    protected float getBoardCenterX() { return BOARD_X + boardWidth() * 0.5f; }
    protected float getBoardCenterY() { return BOARD_Y + boardHeight() * 0.5f; }
    protected float getBoardRight() { return BOARD_X + boardWidth(); }
    protected float getBoardBottom() { return BOARD_Y; }
    protected float getCellCenterY(int row) { return cellY(row) + getBoardTileHeight() * 0.5f; }
    protected float getCellX(int col) { return BOARD_X + col * getBoardTileWidth(); }
    protected float getCellY(int row) { return cellY(row); }

    protected Position visualPositionFor(Plant plant) {
        return plant.getPosition();
    }

    private static final String DEADLINE_FLOWER_PAM =
            "768/INITIAL/EFFECTS/STAR_OBJECTIVE_FLOWER/STAR_OBJECTIVE_FLOWER.PAM";
    private static final float DEADLINE_FLOWER_SCALE = 0.52f;
    private float deadlineFlowerLineClock = 0f;

    /**
     * Draws the Ice Age (Frostbite Caves) dead-line marker - a STAR_OBJECTIVE_FLOWER
     * effect on every row of the given column - instead of a plain colored bar.
     * Used everywhere a "line you can't cross" needs to be shown: PvP/campaign dead
     * lines, Wall-nut Bowling's red line, and I, Zombie's red line.
     */
    protected void drawDeadlineFlowerLine(int col, int rows) {
        deadlineFlowerLineClock += Gdx.graphics.getDeltaTime();
        float loopDuration = AnimationFactory.clipDurationForPath(DEADLINE_FLOWER_PAM, "idle");
        float clipTime = loopDuration > 0f ? deadlineFlowerLineClock % loopDuration : deadlineFlowerLineClock;
        float boardTileWidth = getBoardTileWidth();
        for (int row = 0; row < rows; row++) {
            float x = BOARD_X + col * boardTileWidth - 10f;
            float y = cellY(row) + 40f;
            drawPam(DEADLINE_FLOWER_PAM, "idle", clipTime, x, y, DEADLINE_FLOWER_SCALE, false);
        }
    }

    public boolean drawPam(String path, String preferred, float time, float x, float y, float scale, boolean flip) {
        return drawPam(path, preferred, time, x, y, scale, flip, null);
    }

    /**
     * Same as the 7-arg drawPam, but with a per-element visibility mask. Pass a
     * Map<String, Boolean> where each key is one of the PAM clip's named elements
     * and the value is whether that element should currently be drawn - e.g. the
     * armor pieces on a basic zombie, switched off one at a time as armor health
     * drops. Set the actual element name strings where noted in drawZombies()
     * below; null (or omitting the map via the 7-arg overload) draws every
     * element, same as before.
     */
    public boolean drawPam(String path, String preferred, float time, float x, float y, float scale, boolean flip,
                           Map<String, Boolean> elementVisibility) {
        return pam.drawPam(path, preferred, time, x, y, scale, flip, elementVisibility);
    }

    public boolean drawPamMirrored(String path, String preferred, float time, float x, float y, float scale) {
        return pam.drawPamMirrored(path, preferred, time, x, y, scale);
    }

    /**
     * Rotates the clip to face an arbitrary travel direction instead of only mirroring
     * left/right - see {@link PamRenderer#drawPamRotated}.
     */
    public boolean drawPamRotated(String path, String preferred, float time, float x, float y, float scale,
                                  float rotationDegrees) {
        return pam.drawPamRotated(path, preferred, time, x, y, scale, rotationDegrees);
    }

    public boolean drawPamStretched(String path, String preferred, float time, float x, float y,
                                    float scaleX, float scaleY, boolean flip) {
        return pam.drawPamStretched(path, preferred, time, x, y, scaleX, scaleY, flip);
    }

    protected void preloadPam(String... paths) {
        if (pamPlayer == null || paths == null) return;
        for (String path : paths) {
            if (path == null || path.isBlank()) continue;
            try {
                pamPlayer.loadAsync(path, null);
            } catch (Throwable t) {
                Gdx.app.error("GameScreen", "Could not preload PAM " + path, t);
            }
        }
    }

    protected String getLawnMowerSeasonKey() {
        if (session != null && session.getLevel() != null && session.getLevel().getSeason() != null) {
            String name = session.getLevel().getSeason().getName();
            if (name != null && !name.isBlank()) return name.trim().toLowerCase().replace('-', ' ');
        }




        return seasonFolder == null ? "" : seasonFolder.trim().toLowerCase().replace('-', ' ');
    }

    float getRenderTime() {
        return (Gdx.graphics != null ? Gdx.graphics.getDeltaTime() : 0f) +
                (com.badlogic.gdx.utils.TimeUtils.millis() % 100000L) / 1000f;
    }

    protected void drawEntity(TextureRegion region, float x, float y, float w, float h, Color fallback, String label) {
        if (region != null) batch.draw(region, x, y, w, h);
        else {
            drawFallback(x, y, w, h, fallback);
            if (label != null) {
                GameScreenGraphics.BitmapFontAccess.draw(batch, skin, label, x + 8, y + h * 0.62f);
            }
        }
    }

    protected void drawFallback(float x, float y, float w, float h, Color color) {
        batch.setColor(color);
        batch.draw(whitePixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    @Override public void dispose() {
        if (hud != null) hud.dispose();
        if (endlessHud != null) endlessHud.dispose();
        if (zombossDialogue != null) zombossDialogue.dispose();
        if (textureBank != null) {
            try { textureBank.dispose(); } catch (Throwable ignored) {}
        }
        if (whitePixel != null) whitePixel.getTexture().dispose();
        if (bubbleTexture != null) bubbleTexture.getTexture().dispose();
        if (boardTexture != null) boardTexture.dispose();
        assets.dispose();
        super.dispose();
    }
}