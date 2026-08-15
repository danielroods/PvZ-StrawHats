package view.general_screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import controller.assets.GameAssetManager;
import controller.menus.match.AfterMenu;
import controller.menus.match.BeforeMenu;
import controller.menus.match.MatchMenu;
import controller.menus.match.MeanwhileMenu;
import model.App;
import model.collections.animations.AnimationFactory;
import model.collections.animations.AnimationJsonParser;
import model.collections.animations.ZombieAnimationRegistry;
import model.collections.item.GroundItem;
import model.collections.item.GroundSun;
import model.collections.plant.Plant;
import model.collections.plant.PlantFactory;
import model.collections.plant.PlantJsonParser;
import model.collections.zombie.Zombie;
import model.collections.zombie.ZombieState;
import model.game_exceptions.GameException;
import model.match.main.levels.special_levels.BossLevel;
import model.match.main.levels.special_levels.ConveyorBeltLevel;
import model.match.main.levels.special_levels.DeadLineLevel;
import model.match.main.levels.special_levels.IntroductionLevel;
import model.match.main.levels.special_levels.LockedPlantsLevel;
import model.match.main.levels.special_levels.LoveYourPlantsLevel;
import model.match.main.levels.special_levels.NightOpsLevel;
import model.match.main.levels.special_levels.PlantWhatYouGetLevel;
import model.match.main.levels.special_levels.SaveOurSeedsLevel;
import model.match.main.levels.special_levels.TimedWarLevel;
import model.match.main.season.travellog.egypt.SandStorm;
import model.match_mechanisms.vector.Position;
import model.pitches.Cell;
import model.user_data.User;
import model.user_data.UserState;
import model.projectile.Projectile;
import model.projectile.zombie_projectile.ZombieProjectile;
import model.utils.GameSession;
import model.utils.GameSettings;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import service.GameClock;
import service.resource_manager.AudioManager;
import view.hud.MatchHud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Real-time match renderer/input layer. The model remains authoritative:
 * planting, digging, feeding, collecting and wave scheduling are delegated to
 * MeanwhileMenu/GameSession exactly as the command gameplay does.
 */
public class GameScreen extends UiScreen {
    protected static final float TILE_WIDTH = 96f;
    protected static final float TILE_HEIGHT = 96f;
    protected static float BOARD_X = 190f;
    protected static float BOARD_Y = 170f;

    private static final String ASSET_ROOT = "assets/images/chapters/";

    /**
     * Season folder key, e.g. "egypt", "frostbite_caves", "big_wave_beach", "dark_ages".
     * Every season's gameplay art lives at the same relative layout -
     * chapters/<seasonFolder>/gameplay/{map,texture_left,texture_right}.png -
     * so a subclass only needs to set this field (in its constructor) instead of
     * overriding three separate path-getter methods with full paths each.
     */
    protected String seasonFolder = "egypt";

    private static final float BOARD_INSET_LEFT_FRAC = 260f / 1024f;
    private static final float BOARD_INSET_RIGHT_FRAC = (1024f - 993f) / 1024f;
    private static final float BOARD_INSET_TOP_FRAC = 192f / 768f;
    private static final float BOARD_INSET_BOTTOM_FRAC = (768f - 686f) / 768f;
    private static final float FALLING_SUN_CLICK_HEIGHT = 80f;

    protected GameSession session;
    protected MatchHud hud;
    protected Texture boardTexture;
    protected TextureRegion whitePixel;

    private Texture sideTextureLeft;
    private Texture sideTextureRight;
    private float sideLeftX, sideLeftW, sideLeftH;
    private float sideRightX, sideRightW, sideRightH;

    private Texture graveTexture;
    private TextureRegion graveRegion;

    protected TextureBank textureBank;
    protected PamPlayer pamPlayer;

    private float boardTileWidth = TILE_WIDTH;
    private float boardTileHeight = TILE_HEIGHT;
    private float bgX, bgY, bgW, bgH;

    private static final String SHOVEL_ICON_PATH = "assets/images/chapters/egypt/gameplay/shovel_icon.png";
    private Texture shovelIconTexture;


    private static final float DEATH_ANIM_DURATION = 1.0f;

    private static final class DyingZombie {
        final String alias;
        final Position position;
        final boolean facingRight;
        final float duration;
        float time;

        DyingZombie(String alias, Position position, boolean facingRight, float duration) {
            this.alias = alias;
            this.position = position;
            this.facingRight = facingRight;
            this.duration = duration;
        }
    }

    private final List<DyingZombie> dyingZombies = new ArrayList<>();

    private final Map<Plant, Float> plantAnimTimes = new IdentityHashMap<>();
    private final Map<Zombie, Float> zombieAnimTimes = new IdentityHashMap<>();
    private final Map<GroundItem, Float> itemAnimTimes = new IdentityHashMap<>();
    // Fire-event detection + one-shot "attack" clip playback for plants (see drawPlants).
    private final Map<Plant, Double> plantLastCooldown = new IdentityHashMap<>();
    private final Map<Plant, Float> plantAttackAnimTimes = new IdentityHashMap<>();
    private final Map<Plant, Float> plantAttackWindow = new IdentityHashMap<>();
    private static final float DEFAULT_PLANT_ATTACK_DURATION = 0.4f;
    // Tracks whether the fire event currently playing out in plantAttackAnimTimes was
    // triggered while the plant was plant-food-boosted, so it plays "plantfood" instead
    // of the normal "attack" clip - see drawPlants.
    private final Map<Plant, Boolean> plantAttackIsBoosted = new IdentityHashMap<>();
    private final Map<String, Float> clipTimes = new java.util.HashMap<>();
    private static final Map<String, String[]> SEASON_LAWN_MOWER_PAM_PATHS = new java.util.HashMap<>();
    static {
        String[] egypt = {
                "768/INITIAL/MOWERS/MOWER_EGYPT/MOWER_EGYPT.PAM",

        };
        String[] cave = {
                "768/FULL/MOWERS/MOWER_ICEAGE/MOWER_ICEAGE.PAM",

        };
        String[] beach = {
                "768/FULL/MOWERS/MOWER_BEACH/MOWER_BEACH.PAM",
                "768/INITIAL/MOWERS/MOWER_BEACH/MOWER_BEACH.PAM"
        };
        String[] dark = {
                "768/FULL/MOWERS/MOWER_DARK/MOWER_DARK.PAM",
                "768/INITIAL/MOWERS/MOWER_DARK/MOWER_DARK.PAM"
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
    };

    private double tickAccumulator;
    private boolean paused;
    private boolean matchFinished;
    private String selectedPlant;
    private Tool activeTool = Tool.NONE;
    private Actor boardInput;
    private float dragPreviewTime;
    private float sandStormAnimTime;

    private enum Tool { NONE, SHOVEL, FOOD }

    @Override
    public void initParticles() {
        // Gameplay effects are rendered by the board layers below.
    }
    private void initShovelTexture() {
        String path = resolveExistingAssetPath(SHOVEL_ICON_PATH);
        if (path != null && Gdx.files.internal(path).exists()) {
            shovelIconTexture = new Texture(Gdx.files.internal(path));
            shovelIconTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    @Override
    public void show() {
        super.show();
        session = GameSession.getInstance();
        AnimationFactory.autoInit();
        initPam();
        initBoardTexture();
        initGraveTexture();
        createHud();
        createBoardInput();
        initParticles();
        initShovelTexture();
    }

    private void initPam() {
        try {
            com.badlogic.gdx.files.FileHandle root = Gdx.files.internal("assets/pvz-assets");
            textureBank = new TextureBank("atlases", root);
            pamPlayer = new PamPlayer(textureBank, root);
        } catch (Throwable t) {
            textureBank = null;
            pamPlayer = null;
            Gdx.app.error("GameScreen", "PAM initialization failed; atlas fallback will be used.", t);
        }
    }

    private void initBoardTexture() {
        whitePixel = makeWhitePixel();
        String path = resolveExistingAssetPath(getGameplayBackgroundPath());
        if (Gdx.files.internal(path).exists()) {
            boardTexture = new Texture(Gdx.files.internal(path));
            boardTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        initSideTextures();
    }

    private void initSideTextures() {
        String leftPath = resolveExistingAssetPath(getSideTextureLeftPath());
        if (leftPath != null && !leftPath.isEmpty() && Gdx.files.internal(leftPath).exists()) {
            sideTextureLeft = new Texture(Gdx.files.internal(leftPath));
            sideTextureLeft.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        String rightPath = resolveExistingAssetPath(getSideTextureRightPath());
        if (rightPath != null && !rightPath.isEmpty() && Gdx.files.internal(rightPath).exists()) {
            sideTextureRight = new Texture(Gdx.files.internal(rightPath));
            sideTextureRight.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    /**
     * Base folder for this season's gameplay art: chapters/<seasonFolder>/gameplay/.
     * Subclasses set {@link #seasonFolder} instead of overriding this.
     */
    protected String getSeasonGameplayFolder() {
        return ASSET_ROOT + seasonFolder + "/gameplay/";
    }

    protected String getGameplayBackgroundPath() { return getSeasonGameplayFolder() + "map.png"; }
    protected String getSideTextureLeftPath() { return getSeasonGameplayFolder() + "texture_left.png"; }
    protected String getSideTextureRightPath() { return getSeasonGameplayFolder() + "texture_right.png"; }

    protected String getGraveIconPath() {
        return getSeasonGameplayFolder() + "grave.png";
    }

    private void initGraveTexture() {
        String path = resolveExistingAssetPath(getGraveIconPath());
        if (path != null && !path.isEmpty() && Gdx.files.internal(path).exists()) {
            graveTexture = new Texture(Gdx.files.internal(path));
            graveTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            graveRegion = new TextureRegion(graveTexture);
        } else {
            graveTexture = null;
            graveRegion = null;
        }
    }

    private String resolveExistingAssetPath(String path) {
        if (path == null) return "";
        if (Gdx.files.internal(path).exists()) return path;
        if (path.startsWith("assets/")) {
            String noPrefix = path.substring("assets/".length());
            if (Gdx.files.internal(noPrefix).exists()) return noPrefix;
        } else {
            String withPrefix = "assets/" + path;
            if (Gdx.files.internal(withPrefix).exists()) return withPrefix;
        }
        return path;
    }

    private void createHud() {
        hud = new MatchHud(skin);
        hud.setPlantSelection(this::selectPlant);
        hud.setPlantDragRelease(this::handlePlantDragRelease);
        hud.setShovelAction(() -> armTool(Tool.SHOVEL));
        hud.setFoodAction(() -> armTool(Tool.FOOD));
        hud.setPauseAction(this::togglePause);
        hud.setStartWavesAction(() -> runCommand("start zombie waves"));
        hud.setDebugAddSunAction(() -> runCommand("cheat add -n 25 suns"));
        hud.setDebugAddFoodAction(() -> runCommand("cheat add-plant-food"));
        addBeforeModal(hud);
    }

    private void createBoardInput() {
        boardInput = new Actor();
        boardInput.setTouchable(Touchable.enabled);
        boardInput.setBounds(BOARD_X, BOARD_Y, boardWidth(), boardHeight() + FALLING_SUN_CLICK_HEIGHT);
        boardInput.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Vector2 click = new Vector2(event.getStageX(), event.getStageY());
                if (!collectUnderMouse(click)) {
                    handleBoardClick(click.x, click.y);
                }
            }
        });
        rootStack.addActorBefore(hud, boardInput);
    }

    private float boardWidth() { return (session == null ? 9 : session.getCols()) * boardTileWidth; }
    private float boardHeight() { return (session == null ? 5 : session.getRows()) * boardTileHeight; }

    private void updateBoardLayout() {
        float viewW = stage.getViewport().getWorldWidth();
        float viewH = stage.getViewport().getWorldHeight();

        if (boardTexture != null) {
            float texW = boardTexture.getWidth();
            float texH = boardTexture.getHeight();
            float fitScale = Math.min(viewW / texW, viewH / texH);
            bgW = texW * fitScale;
            bgH = texH * fitScale;
            bgX = (viewW - bgW) / 2f;
            bgY = (viewH - bgH) / 2f;

            BOARD_X = bgX + bgW * BOARD_INSET_LEFT_FRAC;
            BOARD_Y = bgY + bgH * BOARD_INSET_BOTTOM_FRAC;
            float boardPixelW = bgW * (1f - BOARD_INSET_LEFT_FRAC - BOARD_INSET_RIGHT_FRAC);
            float boardPixelH = bgH * (1f - BOARD_INSET_TOP_FRAC - BOARD_INSET_BOTTOM_FRAC);
            int cols = session == null ? 9 : session.getCols();
            int rows = session == null ? 5 : session.getRows();
            boardTileWidth = boardPixelW / cols;
            boardTileHeight = boardPixelH / rows;

            // Side filler art shares the map's fit scale and height so its top/bottom
            // edges line up with the map exactly; each panel grows outward from the
            // map's left/right edge to cover the FitViewport letterbox gutter.
            if (sideTextureLeft != null) {
                sideLeftW = sideTextureLeft.getWidth() * fitScale;
                sideLeftH = sideTextureLeft.getHeight() * fitScale;
                sideLeftX = bgX - sideLeftW;
            }
            if (sideTextureRight != null) {
                sideRightW = sideTextureRight.getWidth() * fitScale;
                sideRightH = sideTextureRight.getHeight() * fitScale;
                sideRightX = bgX + bgW;
            }
        } else {
            bgX = BOARD_X;
            bgY = BOARD_Y;
            bgW = boardWidth();
            bgH = boardHeight();
            boardTileWidth = TILE_WIDTH;
            boardTileHeight = TILE_HEIGHT;
        }

        if (boardInput != null) {
            boardInput.setBounds(BOARD_X, BOARD_Y, boardWidth(), boardHeight() + FALLING_SUN_CLICK_HEIGHT);
        }
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

        if (!paused && !matchFinished) {
            List<Zombie> aliveBeforeTick = session == null ? java.util.Collections.emptyList() : new ArrayList<>(session.getZombies());
            tickAccumulator += delta * GameSettings.get().getGameSpeed();
            while (tickAccumulator >= GameClock.SECONDS_PER_TICK) {
                session.tick();
                tickAccumulator -= GameClock.SECONDS_PER_TICK;
                checkMatchEnd();
                if (matchFinished) break;
            }
            if (session != null) trackZombieDeaths(aliveBeforeTick);
        }

        if (matchFinished) return;

        refreshHud(delta);
        drawBoard(delta);
        stage.act(delta);
        if (controller.ScreenManager.getScreen() != this) return;
        stage.draw();
    }

    private void refreshHud(float delta) {
        if (hud == null || session == null) return;
        if (selectedPlant != null && !BeforeMenu.selectedPlants.contains(selectedPlant)
                && !(session.getLevel() instanceof ConveyorBeltLevel)) {
            selectedPlant = null;
        }
        hud.setSelectedPlant(selectedPlant);
        hud.setTools(activeTool == Tool.SHOVEL, activeTool == Tool.FOOD);
        hud.update(session, new ArrayList<>(BeforeMenu.selectedPlants));
    }

    private void handlePlantDragRelease(Vector2 stagePosition) {
        if (paused || matchFinished || stagePosition == null) return;

        float x = stagePosition.x;
        float y = stagePosition.y;
        if (x < BOARD_X || y < BOARD_Y
                || x >= BOARD_X + boardWidth()
                || y >= BOARD_Y + boardHeight()) {
            return;
        }

        int col = (int) ((x - BOARD_X) / boardTileWidth);
        int row = session.getRows() - 1 - (int) ((y - BOARD_Y) / boardTileHeight);
        if (row < 0 || row >= session.getRows() || col < 0 || col >= session.getCols()) return;

        if (selectedPlant == null && session.getLevel() instanceof ConveyorBeltLevel conveyor
                && conveyor.getCurrentPlant() != null) {
            selectedPlant = conveyor.getCurrentPlant().getName();
        }
        if (selectedPlant == null) return;

        plantAtCell(row, col);
    }

    private void selectPlant(String plantName) {
        if (paused || matchFinished) return;
        if (session.getLevel() instanceof ConveyorBeltLevel conveyor) {
            if (conveyor.getCurrentPlant() != null) selectedPlant = conveyor.getCurrentPlant().getName();
            activeTool = Tool.NONE;
            return;
        }
        selectedPlant = plantName;
        activeTool = Tool.NONE;
    }

    private void armTool(Tool tool) {
        if (paused || matchFinished) return;
        activeTool = activeTool == tool ? Tool.NONE : tool;
        selectedPlant = null;
    }

    private void handleBoardClick(float x, float y) {
        if (paused || matchFinished) return;
        if (x < BOARD_X || y < BOARD_Y || x >= BOARD_X + boardWidth() || y >= BOARD_Y + boardHeight()) return;

        int col = (int) ((x - BOARD_X) / boardTileWidth);
        int row = session.getRows() - 1 - (int) ((y - BOARD_Y) / boardTileHeight);
        if (row < 0 || row >= session.getRows() || col < 0 || col >= session.getCols()) return;
        plantAtCell(row, col);
    }

    private void plantAtCell(int row, int col) {
        int commandX = col + 1;
        int commandY = row + 1;
        String command;

        if (activeTool == Tool.SHOVEL) {
            boolean removed = session.removePlantAt(row, col);
            if (!removed) {
                Toast.show(stage, "There is no plant to remove here.");
                return;
            }
            activeTool = Tool.NONE;
            selectedPlant = null;
            return;
        } else if (activeTool == Tool.FOOD) {
            command = "feed plant -l (" + commandX + ", " + commandY + ")";
        } else if (session.getLevel() instanceof ConveyorBeltLevel conveyor) {
            Plant offered = conveyor.getCurrentPlant();
            if (offered == null) return;
            command = "plant plant -t " + offered.getName() + " -l (" + commandX + ", " + commandY + ")";
        } else if (selectedPlant != null) {
            command = "plant plant -t " + selectedPlant + " -l (" + commandX + ", " + commandY + ")";
        } else {
            return;
        }

        if (runCommand(command)) {
            if (activeTool != Tool.SHOVEL && activeTool != Tool.FOOD) selectedPlant = null;
            if (activeTool != Tool.NONE) activeTool = Tool.NONE;
        }
    }

    private boolean collectUnderMouse(Vector2 click) {
        if (paused || matchFinished || click == null) return false;

        Vector2 world = click;
        int col = (int) ((world.x - BOARD_X) / boardTileWidth);

        for (model.collections.Item raw : session.getItems()) {
            if (!(raw instanceof GroundItem item) || !item.isAlive() || item.isCollected()
                    || item.getPosition() == null) continue;

            Position p = item.getPosition();
            int itemCol = (int) p.x();
            float itemX = BOARD_X + (float) p.x() * boardTileWidth + boardTileWidth * 0.28f;
            float itemY = cellY((int) p.y()) + boardTileHeight * 0.25f;

            if (item instanceof GroundSun sun && sun.isFalling()) {
                float progress = sun.getFallProgress();
                itemY = BOARD_Y + boardHeight() + 35f
                        + (itemY - (BOARD_Y + boardHeight() + 35f)) * progress;

                float age = itemAnimTimes.getOrDefault(item, 0f);
                itemY += (float) Math.sin(age * 3.0f) * 3f;

                float size = boardTileWidth * 0.45f;
                float drawX = itemX + (boardTileWidth * 0.45f - size) * 0.5f;
                float drawY = itemY + (boardTileHeight * 0.45f - size) * 0.5f;
                float radius = Math.max(size, boardTileHeight * 0.45f) * 0.5f;
                float centerX = drawX + size * 0.5f;
                float centerY = drawY + size * 0.5f;

                if (Math.abs(world.x - centerX) <= radius
                        && Math.abs(world.y - centerY) <= radius) {
                    session.collectItemsNear(new Position(itemCol, (int) p.y()));
                    return true;
                }
            } else {
                int row = session.getRows() - 1 - (int) ((world.y - BOARD_Y) / boardTileHeight);
                if (row >= 0 && row < session.getRows()
                        && Math.abs(p.x() - col) <= item.getCollectRadius()
                        && Math.abs(p.y() - row) <= item.getCollectRadius()) {
                    session.collectItemsNear(new Position(col, row));
                    return true;
                }
            }
        }

        return false;
    }

    private Vector2 mouseWorld() {
        Vector3 screen = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        stage.getViewport().unproject(screen);
        return new Vector2(screen.x, screen.y);
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

    private void checkMatchEnd() {
        if (matchFinished) return;
        if (session.isGameOver()) {
            matchFinished = true;
            runCommand("end game -r lose");
            controller.ScreenManager.syncWithCurrentMenu();
        } else if (session.isGameWon()) {
            matchFinished = true;
            runCommand("end game -r win");
            controller.ScreenManager.syncWithCurrentMenu();
        }
    }

    private void togglePause() {
        if (matchFinished) return;
        if (paused) return;
        paused = true;
        new PauseModal().show();
    }

    private void drawBoard(float delta) {
        Gdx.gl.glClearColor(0.03f, 0.03f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        batch.begin();

        updateBoardLayout();
        float bw = boardWidth();
        float bh = boardHeight();
        drawBackground(bw, bh);
        drawTiles(bw, bh);
        drawSeasonGameplayEffects(delta, bw, bh);
        drawSpecialEffects(bw, bh);
        drawGroundItems(delta, bw, bh);
        drawPlants(delta, bw, bh);
        drawZombies(delta, bw, bh);
        drawDyingZombies(delta);
        drawProjectiles(bw, bh);
        drawMowers(bw, bh);
        drawHover(bw, bh);
        drawSeasonForegroundEffects(delta, bw, bh);
        drawDragPreview(delta);

        batch.end();
    }

    /**
     * Ghost preview of the armed/dragged plant: follows the cursor with its idle
     * animation from the moment a loadout card is picked up until it is planted
     * or deselected.
     */
    private void drawDragPreview(float delta) {
        Vector2 mouse = mouseWorld();
        if (mouse == null) return;

        // رندر کردن عکس بیل همراه با موس وقتی شاول فعال است
        if (activeTool == Tool.SHOVEL) {
            if (shovelIconTexture != null) {
                float size = boardTileWidth * 0.7f;
                float drawX = mouse.x - size * 0.5f;
                float drawY = mouse.y - size * 0.5f;
                batch.setColor(Color.WHITE);
                batch.draw(shovelIconTexture, drawX, drawY, size, size);
            }
            return;
        }

        if (activeTool != Tool.NONE || selectedPlant == null) return;

        dragPreviewTime += delta;
        float size = boardTileWidth * 0.8f;
        float drawX = mouse.x - size * 0.5f;
        float drawY = mouse.y - size * 0.5f;

        batch.setColor(1f, 1f, 1f, 0.85f);
        String path = AnimationFactory.pathForDisplayName(selectedPlant);
        boolean drawn = drawPam(path, "idle", dragPreviewTime, drawX + size * 0.15f, drawY, 0.5f, false);
        batch.setColor(Color.WHITE);
        if (!drawn) {
            TextureRegion region = GameAssetManager.get().getPlantRegion(selectedPlant);
            drawEntity(region, drawX, drawY, size, size, new Color(0.2f, 0.65f, 0.22f, 0.85f), initials(selectedPlant));
        }
    }

    private void drawBackground(float bw, float bh) {
        if (boardTexture != null) {
            batch.setColor(Color.WHITE);
            if (sideTextureLeft != null) batch.draw(sideTextureLeft, sideLeftX, bgY, sideLeftW, sideLeftH);
            if (sideTextureRight != null) batch.draw(sideTextureRight, sideRightX, bgY, sideRightW, sideRightH);
            batch.draw(boardTexture, bgX, bgY, bgW, bgH);
        } else {
            batch.setColor(new Color(0.46f, 0.35f, 0.18f, 1f));
            batch.draw(whitePixel, BOARD_X, BOARD_Y, bw, bh);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawTiles(float bw, float bh) {
        for (int r = 0; r < session.getRows(); r++) {
            for (int c = 0; c < session.getCols(); c++) {
                Cell cell = session.getEnvironment().getCell(r, c);
                if (cell == null) continue;
                if (cell.getTile() != null && cell.getTile().type().name().equalsIgnoreCase("WATER")
                        && !isBeach()) {
                    batch.setColor(0.22f, 0.52f, 0.72f, 0.45f);
                    batch.draw(whitePixel, BOARD_X + c * boardTileWidth, cellY(r), boardTileWidth, boardTileHeight);
                    batch.setColor(Color.WHITE);
                }
            }
        }
        if (GameSettings.get().isShowGrid()) {
            batch.setColor(1f, 1f, 1f, 0.18f);
            for (int c = 0; c <= session.getCols(); c++) batch.draw(whitePixel, BOARD_X + c * boardTileWidth, BOARD_Y, 1f, bh);
            for (int r = 0; r <= session.getRows(); r++) batch.draw(whitePixel, BOARD_X, BOARD_Y + r * boardTileHeight, bw, 1f);
            batch.setColor(Color.WHITE);
        }
    }

    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {
        // Default seasons have no extra gameplay overlay.
    }

    protected boolean isBeach() {
        return session != null && session.getLevel() != null
                && session.getLevel().getSeason() != null
                && "Big Wave Beach".equalsIgnoreCase(session.getLevel().getSeason().getName());
    }

    protected float getBoardTileWidth() { return boardTileWidth; }
    protected float getBoardTileHeight() { return boardTileHeight; }
    protected float getBoardCenterX() { return BOARD_X + boardWidth() * 0.5f; }
    protected float getBoardCenterY() { return BOARD_Y + boardHeight() * 0.5f; }
    protected float getBoardRight() { return BOARD_X + boardWidth(); }
    protected float getBoardBottom() { return BOARD_Y; }
    protected float getCellCenterY(int row) { return cellY(row) + boardTileHeight * 0.5f; }

    private void drawSpecialEffects(float bw, float bh) {
        var level = session.getLevel();
        if (level == null) return;

        if (level instanceof SaveOurSeedsLevel save && save.getSeedPositions() != null) {
            for (Position p : save.getSeedPositions().keySet()) drawCellBorder((int) p.y(), (int) p.x(), new Color(0.2f, 1f, 0.35f, 0.55f), 4f);
        }
        if (level instanceof DeadLineLevel deadline && deadline.getDeadLine() != null) {
            int c = (int) deadline.getDeadLine().x();
            batch.setColor(1f, 0.12f, 0.08f, 0.75f);
            batch.draw(whitePixel, BOARD_X + c * boardTileWidth, BOARD_Y, 5f, bh);
            batch.setColor(Color.WHITE);
        }
        if (level instanceof IntroductionLevel) {
            batch.setColor(1f, 0.95f, 0.65f, 0.10f);
            batch.draw(whitePixel, BOARD_X, BOARD_Y, bw, bh);
            batch.setColor(Color.WHITE);
        }
        if (level instanceof PlantWhatYouGetLevel) {
            batch.setColor(0.85f, 0.65f, 0.18f, 0.10f);
            batch.draw(whitePixel, BOARD_X, BOARD_Y, bw, bh);
            batch.setColor(Color.WHITE);
        }
        if (level instanceof BossLevel) {
            batch.setColor(0.35f, 0.05f, 0.05f, 0.12f);
            batch.draw(whitePixel, BOARD_X, BOARD_Y, bw, bh);
            batch.setColor(Color.WHITE);
        }
        if (level instanceof NightOpsLevel || (level.getSeason() != null && level.getSeason().isNight())) {
            batch.setColor(0.04f, 0.06f, 0.14f, 0.26f);
            batch.draw(whitePixel, BOARD_X, BOARD_Y, bw, bh);
            batch.setColor(Color.WHITE);
        }
        if (isEgypt() || isDarkAge()) drawEgyptGraves();
        drawSandStorm(bw, bh);
    }

    private void drawSandStorm(float bw, float bh) {
        if (session == null || !session.isSandStormActive()) return;

        float progress = (float) session.getSandStormProgress();
        float strength = 0.045f + 0.065f * (float) Math.sin(progress * Math.PI);

        batch.setColor(0.78f, 0.61f, 0.34f, strength);
        batch.draw(whitePixel, BOARD_X, BOARD_Y, bw, bh);
        batch.setColor(Color.WHITE);
    }

    private boolean isEgypt() {
        return session.getLevel() != null && session.getLevel().getSeason() != null
                && "Egypt".equalsIgnoreCase(session.getLevel().getSeason().getName());
    }

    private boolean isDarkAge() {
        return session.getLevel() != null && session.getLevel().getSeason() != null
                && "Dark Ages".equalsIgnoreCase(session.getLevel().getSeason().getName());
    }

    private void drawEgyptGraves() {
        for (int r = 0; r < session.getRows(); r++) {
            for (int c = 0; c < session.getCols(); c++) {
                Cell cell = session.getEnvironment().getCell(r, c);
                if (cell == null || cell.getObstacle() == null) continue;
                if (!"Grave".equalsIgnoreCase(cell.getObstacle().getName())) continue;

                TextureRegion grave = graveRegion;
                if (grave == null) {
                    grave = GameAssetManager.get().getUiRegion("grave");
                }

                float drawX = BOARD_X + c * boardTileWidth + (boardTileWidth - 60f) / 2f;
                float drawY = cellY(r) + (boardTileHeight - 78f) / 2f;

                if (grave != null) {
                    batch.draw(grave, drawX, drawY, 60, 78);
                } else {
                    drawFallback(drawX, drawY, 60, 78, new Color(0.55f, 0.52f, 0.48f, 1f));
                }
            }
        }
    }

    private static final float SUN_PAM_SCALE_MULTIPLIER = 1.35f;
    private static final float SUN_PAM_LOOP_SECONDS = 1.0f;

    private void drawGroundItems(float delta, float bw, float bh) {
        for (model.collections.Item raw : session.getItems()) {
            if (!(raw instanceof GroundItem item)) continue;
            if (item == null || !item.isAlive() || item.isCollected() || item.getPosition() == null) continue;
            float age = itemAnimTimes.getOrDefault(item, 0f) + delta;
            itemAnimTimes.put(item, age);
            Position p = item.getPosition();
            float x = BOARD_X + (float) p.x() * boardTileWidth + boardTileWidth * 0.28f;
            float y = cellY((int) p.y()) + boardTileHeight * 0.25f;

            if (item instanceof GroundSun sun) {
                float progress = sun.getFallProgress();
                y = BOARD_Y + bh + 35f + (y - (BOARD_Y + bh + 35f)) * progress;
            }
            float pulse = 1f;
            if (item instanceof GroundSun) {
                pulse = 0.92f + 0.08f * (float) Math.sin(age * 5.5f);
                y += (float) Math.sin(age * 3.0f) * 3f;
            }
            float size = boardTileWidth * 0.45f * pulse;
            float drawX = x + (boardTileWidth * 0.45f - size) * 0.5f;
            float drawY = y + (boardTileHeight * 0.45f - size) * 0.5f;
            if (item instanceof GroundSun sun) {
                String pamPath = GroundSun.getPamAnimationPath(sun.getDropType());
                String clipName = GroundSun.getPamAnimationClip(sun.getDropType());
                float loopingPamTime = age % SUN_PAM_LOOP_SECONDS;
                float pamScale = (size / 100f) * SUN_PAM_SCALE_MULTIPLIER;

                if (!drawPam(pamPath, clipName, loopingPamTime,
                        drawX + size * 0.5f,
                        drawY + size * 0.5f,
                        pamScale,
                        false)) {
                    drawFallback(drawX, drawY, size, size, itemColor("SUN"));
                }
            } else {
                TextureRegion region = GameAssetManager.get().getItemRegion(item.getItemType().name());
                if (region != null) {
                    batch.draw(region, drawX, drawY, size, size);
                } else {
                    drawFallback(drawX, drawY, size, size, itemColor(item.getItemType().name()));
                }
            }
        }
        itemAnimTimes.keySet().removeIf(i -> !session.getItems().contains(i));
    }

    private void drawPlants(float delta, float bw, float bh) {
        for (Plant plant : new ArrayList<>(session.getPlants())) {
            if (plant == null || plant.getPosition() == null) continue;
            float t = plantAnimTimes.getOrDefault(plant, 0f) + delta;
            // Idle time otherwise grows unbounded for the whole match; PamPlayer/ClipRef
            // eventually chokes on a stateTime far past the clip's own length (this is
            // why animations "work at first then stop" - it only shows up once a plant
            // has been sitting idle long enough). Wrap it the same way the zombie
            // walk/eat path already does below via resolveClipDuration.
            float idleDuration = resolvePlantClipDuration(plant.getName(), "idle");
            if (idleDuration > 0f) t %= idleDuration;
            plantAnimTimes.put(plant, t);
            Position p = plant.getPosition();
            float x = BOARD_X + (float) p.x() * boardTileWidth;
            float y = cellY((int) p.y());

            float plantOffsetX = x + 30f;
            float plantOffsetY = y + 40f;

            String path = AnimationFactory.pathForDisplayName(plant.getName());

            // The model has no "attacking" state - ActStrategy.act() fires a shot the
            // instant internalTimer hits 0 and immediately resets it to actionInterval.
            // So a fire event is detected here by watching that reset happen (cooldown
            // was <= 0 last frame, is > 0 now), and a short "attack" window is opened
            // for plantAttackAnimTimes/plantAttackWindow to ride out.
            double cooldown = plant.getIntervalTimer();
            Double lastCooldown = plantLastCooldown.put(plant, cooldown);
            if (lastCooldown != null && cooldown > lastCooldown + 0.05) {
                boolean boosted = plant.isPlantFoodActive();
                String durationState = boosted ? "plantfood" : "attack";
                float attackDuration = resolvePlantClipDuration(plant.getName(), durationState);
                if (attackDuration <= 0f) attackDuration = DEFAULT_PLANT_ATTACK_DURATION;
                plantAttackAnimTimes.put(plant, 0f);
                plantAttackWindow.put(plant, attackDuration);
                plantAttackIsBoosted.put(plant, boosted);
            }

            Float attackTime = plantAttackAnimTimes.get(plant);
            if (attackTime != null) {
                attackTime += delta;
                float window = plantAttackWindow.getOrDefault(plant, DEFAULT_PLANT_ATTACK_DURATION);
                if (attackTime >= window) {
                    plantAttackAnimTimes.remove(plant);
                    plantAttackWindow.remove(plant);
                    plantAttackIsBoosted.remove(plant);
                } else {
                    plantAttackAnimTimes.put(plant, attackTime);
                }
            }

            boolean attacking = plantAttackAnimTimes.containsKey(plant);
            boolean attackIsBoosted = attacking && Boolean.TRUE.equals(plantAttackIsBoosted.get(plant));
            String preferredState = attacking ? (attackIsBoosted ? "plantfood" : "attack") : "idle";
            float animTime = attacking ? plantAttackAnimTimes.get(plant) : t;

            if (!drawPam(path, preferredState, animTime, plantOffsetX , plantOffsetY, 0.55f, false)) {
                TextureRegion region = GameAssetManager.get().getPlantRegion(plant.getName());
                drawEntity(region, plantOffsetX, plantOffsetY, boardTileWidth, boardTileHeight, new Color(0.2f, 0.65f, 0.22f, 1f), initials(plant.getName()));
            }
        }
        plantAnimTimes.keySet().removeIf(p -> !session.getPlants().contains(p));
        plantLastCooldown.keySet().removeIf(p -> !session.getPlants().contains(p));
        plantAttackAnimTimes.keySet().removeIf(p -> !session.getPlants().contains(p));
        plantAttackWindow.keySet().removeIf(p -> !session.getPlants().contains(p));
        plantAttackIsBoosted.keySet().removeIf(p -> !session.getPlants().contains(p));
    }
    /** Looks up how long a zombie's clip for the given state actually plays, in seconds. Returns -1 if unknown. */
    private float resolveClipDuration(String alias, String preferredState) {
        AnimationJsonParser.AnimationConfig config = ZombieAnimationRegistry.resolve(alias);
        if (config == null || config.clips == null) return -1f;
        String clipName = AnimationFactory.resolveClipName(config, preferredState);
        if (clipName == null) return -1f;
        Double duration = config.clips.get(clipName);
        return (duration != null && duration > 0.0) ? duration.floatValue() : -1f;
    }

    /** Same as {@link #resolveClipDuration} but for a plant display name, e.g. "Peashooter". Returns -1 if unknown. */
    private float resolvePlantClipDuration(String displayName, String preferredState) {
        AnimationJsonParser.AnimationConfig config = AnimationFactory.resolveByDisplayName(displayName);
        if (config == null || config.clips == null) return -1f;
        String clipName = AnimationFactory.resolveClipName(config, preferredState);
        if (clipName == null) return -1f;
        Double duration = config.clips.get(clipName);
        return (duration != null && duration > 0.0) ? duration.floatValue() : -1f;
    }

    private void trackZombieDeaths(List<Zombie> aliveBeforeTick) {
        List<Zombie> stillAlive = session.getZombies();
        for (Zombie zombie : aliveBeforeTick) {
            if (stillAlive.contains(zombie)) continue;
            if (zombie.getPosition() == null) continue;
            if (zombie.getZombieState() != ZombieState.DEAD) continue;
            float dieDuration = resolveClipDuration(zombie.getAlias(), "die");
            if (dieDuration <= 0f) dieDuration = DEATH_ANIM_DURATION;
            dyingZombies.add(new DyingZombie(zombie.getAlias(), zombie.getPosition(), zombie.isFacingRight(), dieDuration));
            zombieAnimTimes.remove(zombie);
        }
    }

    private void drawZombies(float delta, float bw, float bh) {
        List<Zombie> zombies = new ArrayList<>(session.getZombies());
        zombies.sort(Comparator.comparingDouble(z -> z.getPosition() == null ? 0 : z.getPosition().y()));
        for (Zombie zombie : zombies) {
            if (zombie == null || zombie.getPosition() == null) continue;
            float t = zombieAnimTimes.getOrDefault(zombie, 0f) + delta;
            zombieAnimTimes.put(zombie, t);
            Position p = zombie.getPosition();
            float x = BOARD_X + (float) p.x() * boardTileWidth;
            float y = cellY(p.y());
            float zombieOffsetY = y + 40f;

            String preferred = switch (zombie.getZombieState()) {
                case EATING -> "eat";
                case DEAD -> "die";
                default -> "walk";
            };
            String path = ZombieAnimationRegistry.pathFor(zombie.getAlias(), seasonFolder);
            float animationTime = t;
            if (("walk".equals(preferred) || "eat".equals(preferred)) && path != null) {
                float duration = resolveClipDuration(zombie.getAlias(), preferred);
                if (duration > 0f) {
                    animationTime = t % duration;
                }
            }
            if (!drawPam(path, preferred, animationTime, x - 10f, zombieOffsetY, 0.52f, zombie.isFacingRight())) {
                TextureRegion region = GameAssetManager.get().getZombieRegion(zombie.getAlias());
                drawEntity(region, x, zombieOffsetY, boardTileWidth, boardTileHeight, new Color(0.55f, 0.5f, 0.45f, 1f), initials(zombie.getAlias()));
            }
            if (session.isZombieInSandStorm(zombie)) {
                double stormTime = session.getSandStormAnimationTime(zombie);
                if (stormTime >= 0.0) {
                    float stormX = x + boardTileWidth * 0.5f - 60f;
                    float stormY = zombieOffsetY + boardTileHeight * 0.78f - 40f;
                    drawZombieSandStorm(stormX, stormY,
                            boardTileWidth, boardTileHeight, (float) stormTime, System.identityHashCode(zombie), true, zombie.isFacingRight());
                }
            }
        }
        zombieAnimTimes.keySet().removeIf(z -> !session.getZombies().contains(z));
    }
    private void drawDyingZombies(float delta) {
        if (dyingZombies.isEmpty()) return;
        for (DyingZombie dz : dyingZombies) {
            dz.time += delta;
            float x = BOARD_X + (float) dz.position.x() * boardTileWidth;
            float y = cellY((int) dz.position.y());
            float zombieOffsetY = y + 40f;

            String path = ZombieAnimationRegistry.pathFor(dz.alias, seasonFolder);

            // Particles (head + hand) drop off and settle onto the row's ground
            // over roughly the first half of the death animation.
            float fallProgress = Math.min(1f, dz.time / (dz.duration * 0.5f));
            float fallEase = 1f - (1f - fallProgress) * (1f - fallProgress);
            float particleDrop = 24f * fallEase;

            drawPam(path, "particles", dz.time, x - 10f, zombieOffsetY - particleDrop, 0.52f, dz.facingRight);
            // Clamp to the clip's own last frame instead of letting time run past
            // it, so the death animation holds on its final pose instead of
            // looping/glitching once dz.time exceeds the clip's real length.
            drawPam(path, "die", Math.min(dz.time, dz.duration), x - 10f, zombieOffsetY, 0.52f, dz.facingRight);
        }
        dyingZombies.removeIf(dz -> dz.time > dz.duration);
    }


    private void drawZombieSandStorm(float centerX, float centerY, float tileW, float tileH,
                                     float time, int seed, boolean renderPam, boolean flip) {
        boolean pamDrawn = false;
        if (renderPam) {
            String pamPath = SandStorm.PAM_PATH_PLACEHOLDER;
            float elapsed = time;
            String phase;
            float phaseTime;
            float intro = (float) SandStorm.INTRO_DURATION_SECONDS;
            float outroStart = (float) (SandStorm.EVENT_DURATION_SECONDS - SandStorm.OUTRO_DURATION_SECONDS);
            if (elapsed < intro) {
                phase = "intro";
                phaseTime = Math.max(0f, elapsed);
            } else if (elapsed < outroStart) {
                phase = "loop";
                phaseTime = (elapsed - intro) % (float) SandStorm.LOOP_DURATION_SECONDS;
            } else {
                phase = "outro";
                phaseTime = Math.max(0f, elapsed - outroStart);
            }

            float stormScale = tileW / 118f;
            pamDrawn = drawPam(pamPath, phase, phaseTime, centerX, centerY, stormScale, flip);
        }

        if (pamDrawn) return;

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        int particleCount = 16;
        float baseRadius = tileW * 0.72f;
        for (int i = 0; i < particleCount; i++) {
            float particleSeed = (seed % 360) + i * 137.5f;
            float angularSpeed = 52f + (i % 3) * 18f;
            float angle = (particleSeed + time * angularSpeed) % 360f;
            float rad = (float) Math.toRadians(angle);
            float orbit = baseRadius * (0.35f + 0.65f * ((i % 5) / 4f));
            float px = centerX + (float) Math.cos(rad) * orbit;
            float py = centerY + (float) Math.sin(rad) * orbit * 0.46f;
            float size = tileW * (0.095f + 0.045f * (i % 4));
            float alpha = 0.22f + 0.18f * (float) Math.sin(time * 4.0f + i * 1.31f);
            batch.setColor(0.97f, 0.82f, 0.54f, Math.max(0.10f, alpha));
            batch.draw(whitePixel, px - size * 0.5f, py - size * 0.15f, size * 0.5f, size * 0.15f,
                    size, size * 0.3f, 1f, 1f, angle * 1.5f);
        }
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    protected boolean drawPam(String path, String preferred, float time, float x, float y, float scale, boolean flip) {
        if (pamPlayer == null || path == null) return false;
        try {
            String pamPath = path;
            if (pamPath.startsWith("assets/pvz-assets/")) {
                pamPath = pamPath.substring("assets/pvz-assets/".length());
            }
            String clipName = AnimationFactory.resolveClipNameForPath(pamPath, preferred);
            if (clipName == null) clipName = preferred;
            if (clipName == null || clipName.isBlank()) return false;
            ClipRef clip = pamPlayer.getClip(pamPath, clipName);
            if (clip == null) {
                if (GameSettings.get().isDebugMode()) {
                    Gdx.app.log("DRAWPAM_NULLCLIP", "getClip returned null for path=" + path + " clip=" + clipName);
                }
                return false;
            }

            batch.flush();
            com.badlogic.gdx.math.Matrix4 old = batch.getTransformMatrix().cpy();

            batch.getTransformMatrix().translate(x, y, 0f).scale(scale, scale, 1f);
            batch.setTransformMatrix(batch.getTransformMatrix());

            pamPlayer.draw(batch, clip, time, 0f, 0f, flip);

            batch.flush();
            batch.setTransformMatrix(old);
            return true;
        } catch (Throwable t) {
            if (GameSettings.get().isDebugMode()) {
                Gdx.app.error("DRAWPAM_FAIL", "drawPam threw for path=" + path + " preferred=" + preferred, t);
            }
            return false;
        }
    }

    private void drawProjectiles(float bw, float bh) {
        for (Projectile projectile : session.getProjectiles()) drawSmallDot(projectile.getPosition(), new Color(0.95f, 0.9f, 0.18f, 1f));
        for (ZombieProjectile projectile : session.getZombieProjectiles()) drawSmallDot(projectile.getPosition(), new Color(0.8f, 0.18f, 0.18f, 1f));
    }

    private void drawSmallDot(Position p, Color color) {
        if (p == null) return;
        float x = BOARD_X + (float) p.x() * boardTileWidth + boardTileWidth * 0.41f;
        float y = cellY((int) p.y()) + boardTileHeight * 0.42f;
        drawFallback(x, y, 18f, 18f, color);
    }

    private void drawMowers(float bw, float bh) {
        final float time = getRenderTime();
        String seasonKey = getLawnMowerSeasonKey();
        String[] mowerPaths = SEASON_LAWN_MOWER_PAM_PATHS.get(seasonKey);
        model.pitches.LawnMower[] mowers = session.getLawnMowers();

        float manualXOffset = -35f;
        float manualYOffset = 35f;
        float mowerScale = 0.60f;

        for (int r = 0; r < session.getRows(); r++) {
            if (mowers == null || r >= mowers.length) continue;
            model.pitches.LawnMower mower = mowers[r];

            if (mower == null || mower.getState() == model.pitches.LawnMower.MowerState.DEAD) continue;

            float baseX = BOARD_X + (float) mower.getXPosition() * boardTileWidth + manualXOffset;
            float baseY = cellY(r) + 8f + manualYOffset;

            String clipName = getMowerClipName(mower, seasonKey);

            float animTime = (mower.getState() == model.pitches.LawnMower.MowerState.IDLE)
                    ? time
                    : (float) mower.getStateTimer();

            boolean pamDrawn = false;
            if (pamPlayer != null && mowerPaths != null) {
                for (String pamPath : mowerPaths) {
                    if (drawPam(pamPath, clipName, animTime, baseX + 27f, baseY + 10f, mowerScale, false)) {
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

    private String getLawnMowerSeasonKey() {
        if (session == null || session.getLevel() == null || session.getLevel().getSeason() == null) return "";
        String name = session.getLevel().getSeason().getName();
        if (name == null) return "";
        return name.trim().toLowerCase().replace('-', ' ');
    }

    private float getRenderTime() {
        return (Gdx.graphics != null ? Gdx.graphics.getDeltaTime() : 0f) +
                (com.badlogic.gdx.utils.TimeUtils.millis() % 100000L) / 1000f;
    }

    private void drawProceduralLawnMower(float x, float y, float wheelTime) {
        batch.setColor(Color.valueOf("5E9B3F"));
        batch.draw(whitePixel, x + 8f, y + 20f, 43f, 27f);

        batch.setColor(Color.valueOf("D6D0B4"));
        batch.draw(whitePixel, x + 14f, y + 47f, 31f, 9f);

        batch.setColor(Color.valueOf("4B4B43"));
        batch.draw(whitePixel, x + 2f, y + 11f, 10f, 22f);

        float wheelPulse = 1f + 0.05f * (float) Math.sin(wheelTime * 6f);
        batch.setColor(Color.valueOf("20221E"));
        batch.draw(whitePixel, x + 11f, y + 9f, 11f * wheelPulse, 7f);
        batch.draw(whitePixel, x + 38f, y + 9f, 11f * wheelPulse, 7f);

        batch.setColor(Color.valueOf("C47B2C"));
        batch.draw(whitePixel, x + 25f, y + 26f, 8f, 8f);

        batch.setColor(Color.valueOf("E9E5CE"));
        batch.draw(whitePixel, x + 48f, y + 34f, 16f, 5f);
        batch.setColor(Color.WHITE);
    }

    private void drawHover(float bw, float bh) {
        Vector2 mouse = mouseWorld();
        if (mouse == null) return;
        int col = (int) ((mouse.x - BOARD_X) / boardTileWidth);
        int row = session.getRows() - 1 - (int) ((mouse.y - BOARD_Y) / boardTileHeight);
        if (row < 0 || row >= session.getRows() || col < 0 || col >= session.getCols()) return;
        boolean active = selectedPlant != null || activeTool != Tool.NONE || session.getLevel() instanceof ConveyorBeltLevel;
        batch.setColor(active ? new Color(0.55f, 1f, 0.55f, 0.22f) : new Color(1f, 1f, 1f, 0.12f));
        batch.draw(whitePixel, BOARD_X + col * boardTileWidth, cellY(row), boardTileWidth, boardTileHeight);
        batch.setColor(Color.WHITE);
    }

    private float cellY(double row) { return (float) (BOARD_Y + (session.getRows() - 1 - row) * boardTileHeight); }

    private void drawCellBorder(int row, int col, Color color, float thickness) {
        if (row < 0 || col < 0 || row >= session.getRows() || col >= session.getCols()) return;
        float x = BOARD_X + col * boardTileWidth;
        float y = cellY(row);
        batch.setColor(color);
        batch.draw(whitePixel, x, y, boardTileWidth, thickness);
        batch.draw(whitePixel, x, y + boardTileHeight - thickness, boardTileWidth, thickness);
        batch.draw(whitePixel, x, y, thickness, boardTileHeight);
        batch.draw(whitePixel, x + boardTileWidth - thickness, y, thickness, boardTileHeight);
        batch.setColor(Color.WHITE);
    }

    private void drawEntity(TextureRegion region, float x, float y, float w, float h, Color fallback, String label) {
        if (region != null) batch.draw(region, x, y, w, h);
        else {
            drawFallback(x, y, w, h, fallback);
            if (label != null) {
                BitmapFontAccess.draw(batch, skin, label, x + 8, y + h * 0.62f);
            }
        }
    }

    private void drawFallback(float x, float y, float w, float h, Color color) {
        batch.setColor(color);
        batch.draw(whitePixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    private static Color itemColor(String type) {
        if (type == null) return Color.LIGHT_GRAY;
        return switch (type) {
            case "SUN" -> new Color(1f, 0.84f, 0.1f, 1f);
            case "PLANT_FOOD" -> new Color(0.2f, 0.9f, 0.35f, 1f);
            case "COIN" -> new Color(0.95f, 0.7f, 0.15f, 1f);
            case "DIAMOND" -> new Color(0.25f, 0.8f, 1f, 1f);
            default -> Color.LIGHT_GRAY;
        };
    }

    private static String initials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] words = name.split("\\s+");
        StringBuilder b = new StringBuilder();
        for (String w : words) if (!w.isEmpty()) b.append(Character.toUpperCase(w.charAt(0)));
        return b.substring(0, Math.min(2, b.length()));
    }

    private static TextureRegion makeWhitePixel() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return new TextureRegion(t);
    }

    @Override public void dispose() {
        if (hud != null) hud.dispose();
        if (textureBank != null) {
            try { textureBank.dispose(); } catch (Throwable ignored) {}
        }
        if (whitePixel != null) whitePixel.getTexture().dispose();
        if (boardTexture != null) boardTexture.dispose();
        if (sideTextureLeft != null) sideTextureLeft.dispose();
        if (sideTextureRight != null) sideTextureRight.dispose();
        if (graveTexture != null) graveTexture.dispose();
        if (shovelIconTexture != null) shovelIconTexture.dispose();
        super.dispose();
    }

    private class PauseModal extends Modal {
        PauseModal() {
            content.add(new Label("PAUSED", skin, "title")).colspan(2).padBottom(15).row();
            TextButton resume = new TextButton("Resume", skin);
            resume.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { paused = false; hide(); } });
            content.add(resume).size(220, 50).pad(5).row();
            TextButton restart = new TextButton("Restart", skin);
            restart.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    if (runCommand("restart")) {
                        session = GameSession.getInstance();
                        tickAccumulator = 0;
                        paused = false;
                        matchFinished = false;
                        hide();
                    }
                }
            });
            content.add(restart).size(220, 50).pad(5).row();
            TextButton exit = new TextButton("Save & Exit", skin);
            exit.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    runCommand("menu exit");
                    hide();
                    controller.ScreenManager.syncWithCurrentMenu();
                }
            });
            content.add(exit).size(220, 50).pad(5);
        }
    }
    private class EndMatchModal extends Modal {
        EndMatchModal(boolean won) {
            pad(24);
            add(new Label(won ? "LEVEL COMPLETE!" : "LEVEL FAILED", skin, "title")).padBottom(15).row();
            if (!won) {
                TextButton retry = new TextButton("Retry", skin);
                retry.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { runCommand("restart"); hide(); } });
                add(retry).size(220, 50).pad(5).row();
            }
            TextButton exit = new TextButton("Exit", skin);
            exit.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { runCommand("menu exit"); hide(); } });
            add(exit).size(220, 50).pad(5);
        }
    }

    /** Tiny font helper keeps fallback rendering out of the gameplay logic. */
    private static final class BitmapFontAccess {
        static void draw(Batch batch, Skin skin, String text, float x, float y) {
            if (skin != null && skin.has("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont.class)) {
                skin.getFont("default-font").draw(batch, text, x, y);
            }
        }
    }
    protected void drawSeasonForegroundEffects(float delta, float bw, float bh) {
        // Default seasons have no extra foreground overlay.
    }
}