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

    private static final String EGYPT_MAP = "assets/images/chapters/egypt/egypt_gameplay/map.png";
    private static final String GRAVE_ITEM_ICON = "assets/images/chapters/egypt/egypt_gameplay/grave.png";

    private static final float BOARD_INSET_LEFT_FRAC = 260f / 1024f;
    private static final float BOARD_INSET_RIGHT_FRAC = (1024f - 993f) / 1024f;
    private static final float BOARD_INSET_TOP_FRAC = 192f / 768f;
    private static final float BOARD_INSET_BOTTOM_FRAC = (768f - 686f) / 768f;
    private static final float FALLING_SUN_CLICK_HEIGHT = 80f;

    protected GameSession session;
    protected MatchHud hud;
    protected Texture boardTexture;
    protected TextureRegion whitePixel;

    private Texture graveTexture;
    private TextureRegion graveRegion;

    protected TextureBank textureBank;
    protected PamPlayer pamPlayer;

    private float boardTileWidth = TILE_WIDTH;
    private float boardTileHeight = TILE_HEIGHT;
    private float bgX, bgY, bgW, bgH;

    private static final String SHOVEL_ICON_PATH = "assets/images/chapters/egypt/egypt_gameplay/shovel_icon.png";
    private Texture shovelIconTexture;

    private final Map<Plant, Float> plantAnimTimes = new IdentityHashMap<>();
    private final Map<Zombie, Float> zombieAnimTimes = new IdentityHashMap<>();
    private final Map<GroundItem, Float> itemAnimTimes = new IdentityHashMap<>();
    private final Map<String, Float> clipTimes = new java.util.HashMap<>();
    private static final Map<String, String[]> SEASON_LAWN_MOWER_PAM_PATHS = new java.util.HashMap<>();
    static {
        String[] egypt = {
                "768/INITIAL/MOWERS/MOWER_EGYPT/MOWER_EGYPT.PAM",

        };
        String[] cave = {
                "768/INITIAL/MOWERS/MOWER_ICEAGE/MOWER_ICEAGE.PAM"
        };
        String[] beach = {
                "768/INITIAL/MOWERS/MOWER_BEACH/MOWER_BEACH.PAM"
        };
        String[] dark = {
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
    }

    protected String getGameplayBackgroundPath() { return EGYPT_MAP; }

    private void initGraveTexture() {
        String path = resolveExistingAssetPath(GRAVE_ITEM_ICON);
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
        rootStack.add(hud);
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
            try { textureBank.update(); } catch (Throwable ignored) {}
        }

        if (!paused && !matchFinished) {
            tickAccumulator += delta * GameSettings.get().getGameSpeed();
            while (tickAccumulator >= GameClock.SECONDS_PER_TICK) {
                session.tick();
                tickAccumulator -= GameClock.SECONDS_PER_TICK;
                checkMatchEnd();
                if (matchFinished) break;
            }
        }

        if (matchFinished) return;

        refreshHud(delta);
        drawBoard(delta);
        stage.act(delta);
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
        drawSpecialEffects(bw, bh);
        drawGroundItems(delta, bw, bh);
        drawPlants(delta, bw, bh);
        drawZombies(delta, bw, bh);
        drawProjectiles(bw, bh);
        drawMowers(bw, bh);
        drawHover(bw, bh);
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
                if (cell.getTile() != null && cell.getTile().type().name().equalsIgnoreCase("WATER")) {
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
        if (isEgypt()) drawEgyptGraves();
    }

    private boolean isEgypt() {
        return session.getLevel() != null && session.getLevel().getSeason() != null
                && "Egypt".equalsIgnoreCase(session.getLevel().getSeason().getName());
    }

    private void drawEgyptGraves() {
        for (int r = 0; r < session.getRows(); r++) {
            for (int c = 0; c < session.getCols(); c++) {
                Cell cell = session.getEnvironment().getCell(r, c);
                if (cell == null || cell.getObstacle() == null) continue;
                if (!"Grave".equalsIgnoreCase(cell.getObstacle().getName())) continue;

                TextureRegion grave = GameAssetManager.get().getUiRegion("grave");
                if (grave == null) {
                    grave = graveRegion;
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
            plantAnimTimes.put(plant, t);
            Position p = plant.getPosition();
            float x = BOARD_X + (float) p.x() * boardTileWidth;
            float y = cellY((int) p.y());

            float plantOffsetX = x + 30f;
            float plantOffsetY = y + 40f;

            String path = AnimationFactory.pathForDisplayName(plant.getName());
            if (!drawPam(path, "idle", t, plantOffsetX , plantOffsetY, 0.55f, false)) {
                TextureRegion region = GameAssetManager.get().getPlantRegion(plant.getName());
                drawEntity(region, plantOffsetX, plantOffsetY, boardTileWidth, boardTileHeight, new Color(0.2f, 0.65f, 0.22f, 1f), initials(plant.getName()));
            }
        }
        plantAnimTimes.keySet().removeIf(p -> !session.getPlants().contains(p));
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
            float y = cellY((int) p.y());
            float zombieOffsetY = y + 40f;

            String preferred = switch (zombie.getZombieState()) {
                case EATING -> "eat";
                case DEAD -> "die";
                default -> "walk";
            };
            String path = ZombieAnimationRegistry.pathFor(zombie.getAlias());
            float animationTime = t;
            if ("walk".equals(preferred) && path != null) {
                AnimationJsonParser.AnimationConfig config = ZombieAnimationRegistry.resolve(zombie.getAlias());
                if (config != null) {
                    String clipName = AnimationFactory.resolveClipName(config, "walk");
                    Double duration = clipName == null || config.clips == null ? null : config.clips.get(clipName);
                    if (duration != null && duration > 0.0) {
                        animationTime = (float) (t % duration);
                    }
                }
            }
            if (!drawPam(path, preferred, animationTime, x - 10f, zombieOffsetY, 0.52f, zombie.isFacingRight())) {
                TextureRegion region = GameAssetManager.get().getZombieRegion(zombie.getAlias());
                drawEntity(region, x, zombieOffsetY, boardTileWidth, boardTileHeight, new Color(0.55f, 0.5f, 0.45f, 1f), initials(zombie.getAlias()));
            }
        }
        zombieAnimTimes.keySet().removeIf(z -> !session.getZombies().contains(z));
    }

    private boolean drawPam(String path, String preferred, float time, float x, float y, float scale, boolean flip) {
        if (pamPlayer == null || path == null) return false;
        try {
            String clipName = AnimationFactory.resolveClipNameForPath(path, preferred);
            if (clipName == null) clipName = preferred;
            if (clipName == null || clipName.isBlank()) return false;
            ClipRef clip = pamPlayer.getClip(path, clipName);
            if (clip == null) return false;

            batch.flush();
            com.badlogic.gdx.math.Matrix4 old = batch.getTransformMatrix().cpy();

            batch.getTransformMatrix().translate(x, y, 0f).scale(scale, scale, 1f);
            batch.setTransformMatrix(batch.getTransformMatrix());

            pamPlayer.draw(batch, clip, time, 0f, 0f, flip);

            batch.flush();
            batch.setTransformMatrix(old);
            return true;
        } catch (Throwable ignored) {
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

    private float cellY(int row) { return BOARD_Y + (session.getRows() - 1 - row) * boardTileHeight; }

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
        if (graveTexture != null) graveTexture.dispose();
        if (shovelIconTexture != null) shovelIconTexture.dispose();
        super.dispose();
    }

    private class PauseModal extends Modal {
        PauseModal() {
            pad(24);
            add(new Label("PAUSED", skin, "title")).padBottom(15).row();
            TextButton resume = new TextButton("Resume", skin);
            resume.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { paused = false; hide(); } });
            add(resume).size(220, 50).pad(5).row();
            TextButton restart = new TextButton("Restart", skin);
            restart.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { if (runCommand("restart")) { paused = false; matchFinished = false; hide(); } } });
            add(restart).size(220, 50).pad(5).row();
            TextButton exit = new TextButton("Save & Exit", skin);
            exit.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { runCommand("menu exit"); hide(); } });
            add(exit).size(220, 50).pad(5);
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
}