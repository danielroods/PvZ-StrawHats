package view.screens.generals;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

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
import view.hud.MatchHud;
import view.screens.match.after.MatchEndSequence;

import java.util.ArrayList;
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
    protected Texture boardTexture;
    public TextureRegion whitePixel;

    protected TextureBank textureBank;
    protected PamPlayer pamPlayer;

    private final BoardLayout layout = new BoardLayout(this);
    private final GameScreenAssets assets = new GameScreenAssets(this);
    private final PamRenderer pam = new PamRenderer(this);
    private final BoardInteraction interaction = new BoardInteraction(this);
    private final SeasonOverlayRenderer overlays = new SeasonOverlayRenderer(this);
    private final FrostbiteRenderer frostbite = new FrostbiteRenderer(this);
    private final PlantRenderer plants = new PlantRenderer(this);
    private final ZombieRenderer zombies = new ZombieRenderer(this);
    private final EffectRenderer effects = new EffectRenderer(this);
    private final GroundItemRenderer groundItems = new GroundItemRenderer(this);
    private final MowerRenderer mowers = new MowerRenderer(this);
    private final MatchEndSequence matchEnd = new MatchEndSequence(this);
    private final ZombossRenderer zomboss = new ZombossRenderer(this);

    private view.hud.ZombossDialogueBox zombossDialogue;

    double tickAccumulator;
    boolean paused;
    public boolean matchFinished;
    private float sandStormAnimTime;
    private final Map<String, Float> clipTimes = new java.util.HashMap<>();

    @Override
    public void initParticles() {
        // Gameplay effects are rendered by the board layers below.
    }

    @Override
    public void show() {
        super.show();
        session = GameSession.getInstance();
        AnimationFactory.autoInit();
        pam.initPam();
        assets.initBoardTexture();
        assets.initGraveTexture();
        createHud();
        createBoardInput();
        initParticles();
        assets.initShovelTexture();
        assets.initFrostbiteTextures();
        zomboss.reset();
        zomboss.preload();
        createZombossDialogue();
    }

    private void createZombossDialogue() {
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

    protected void createHud() {
        hud = new MatchHud(skin);
        hud.setPamPlayer(pamPlayer);
        hud.setPlantSelection(this::selectPlant);
        hud.setConveyorPlantSelection(interaction::selectConveyorPlant);
        hud.setPlantDragRelease(interaction::handlePlantDragRelease);
        hud.setShovelAction(() -> interaction.armTool(BoardInteraction.Tool.SHOVEL));
        hud.setFoodAction(() -> interaction.armTool(BoardInteraction.Tool.FOOD));
        hud.setPauseAction(this::togglePause);
        hud.setStartWavesAction(() -> runCommand("start zombie waves"));
        hud.setDebugAddSunAction(() -> runCommand("cheat add -n 25 suns"));
        hud.setDebugAddFoodAction(() -> runCommand("cheat add-plant-food"));
        addBeforeModal(hud);
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

    @Override
    public void render(float delta) {
        adoptRestartedBossSession();
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

        refreshHud(delta);
        drawBoard(delta);
        stage.act(delta);
        if (controller.ScreenManager.getScreen() != this) return;
        stage.draw();
    }

    private void adoptRestartedBossSession() {
        GameSession current = GameSession.peekInstance();
        if (current != null && current != session && current.getZombossFight() != null) {
            session = current;
        }
    }

    /**
     * Advances the simulation by one fixed tick (see GameClock.SECONDS_PER_TICK).
     * Default: ticks the shared GameSession directly, exactly as before. A screen
     * whose model wraps the session in something with its own extra bookkeeping
     * (e.g. a minigame with its own win/loss condition and its own scheduler) can
     * override this instead of duplicating render()'s whole tick loop.
     */
    protected void tickSession() {
        session.tick();
    }

    protected List<String> loadoutPlants() {
        return new ArrayList<>(BeforeMenu.selectedPlants);
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
        refreshZomboss();
        hud.update(session, loadout);
    }

    private void refreshZomboss() {
        model.match.boss.ZombossFight fight = session.getZombossFight();
        if (fight == null) return;
        hud.setObjectiveOverride("DEFEAT DR. ZOMBOSS");
        if (fight.getPhase().isBeforeBattle()) {
            hud.setProgressOverride("ZOMBOSS INCOMING", 0f);
        } else {
            float health = (float) fight.getBossHealthFraction();
            hud.setProgressOverride("ZOMBOSS " + Math.round(health * 100f) + "%", health);
        }
        if (zombossDialogue != null) {
            zombossDialogue.showLine(fight.getDialogueLine(), fight.getDialogueIndex(),
                    fight.getDialogueCount());
        }
    }

    protected void selectPlant(String plantName) {
        interaction.selectPlant(plantName);
    }

    /**
     * What happens when a board cell is tapped/clicked. Default is the normal
     * plant/shovel/food flow below. Mini-games with a different interaction model
     * (Vasebreaker's break-vase-then-plant, Wallnut Bowling's launch-a-nut,
     * I Zombie's place-a-zombie, Beghouled's swap-two-gems) override this instead
     * of touching handleBoardClick/handlePlantDragRelease directly.
     */
    protected void onCellClicked(int row, int col) {
        interaction.plantAtCell(row, col);
    }

    protected boolean collectUnderMouse(Vector2 click) {
        return interaction.collectUnderMouse(click);
    }

    public boolean runCommand(String command) {
        try {
            App.currentMenu.handleCommand(command);
            return true;
        } catch (GameException e) {
            Toast.show(stage, e.getMessage());
            return false;
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Command failed: " + command, e);
            Toast.show(stage, e.getMessage() == null ? "Action failed." : e.getMessage());
            return false;
        }
    }

    protected void checkMatchEnd() {
        matchEnd.checkMatchEnd();
    }

    protected void onZombieDied(Zombie zombie) {
    }

    protected void startMatchEndSequence(boolean won) {
        matchEnd.startMatchEndSequence(won);
    }

    protected boolean isMatchEndSequenceIdle() {
        return matchEnd.isIdle();
    }

    public void onMatchEndSequenceFinished(boolean won) {
        runCommand(won ? "end game -r win" : "end game -r lose");
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
        frostbite.drawFrostbiteTileArt();
        drawSeasonGameplayEffects(delta, bw, bh);
        overlays.drawSpecialEffects(delta, bw, bh);
        zomboss.drawBackdrop();
        plants.drawPlants(delta, bw, bh);
        effects.drawExplodingPlantEffects(delta);
        zomboss.drawBoss();
        zombies.drawZombies(delta, bw, bh);
        zombies.drawDyingZombies(delta);
        groundItems.drawGroundItems(delta, bw, bh);
        effects.drawProjectiles(delta, bw, bh);
        zomboss.drawEffects(delta);
        mowers.drawMowers(bw, bh);
        frostbite.drawFrostbiteIceBlocks(delta);
        interaction.drawHover(bw, bh);
        drawSeasonForegroundEffects(delta, bw, bh);
        interaction.drawDragPreview(delta);
        zomboss.drawNpc();
        matchEnd.drawMatchEndOverlay();

        batch.end();
    }

    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {
        // Default seasons have no extra gameplay overlay.
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

    protected boolean drawPam(String path, String preferred, float time, float x, float y, float scale, boolean flip) {
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
        // Mini-games have no Level/Season on their session, so fall back to seasonFolder,
        // which mini-game screens set to their own SEASON_LAWN_MOWER_PAM_PATHS key
        // (see BeghouledGameScreen, VasebreakerGameScreen, WallnutBowlingGameScreen,
        // IZombieGameScreen, ZombotanyGameScreen).
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
        if (zombossDialogue != null) zombossDialogue.dispose();
        if (textureBank != null) {
            try { textureBank.dispose(); } catch (Throwable ignored) {}
        }
        if (whitePixel != null) whitePixel.getTexture().dispose();
        if (boardTexture != null) boardTexture.dispose();
        assets.dispose();
        super.dispose();
    }
}
