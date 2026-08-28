package view.screens.generals;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import controller.assets.GameAssetManager;
import model.collections.animations.AnimationFactory;
import model.collections.item.GroundItem;
import model.collections.item.GroundSun;
import model.collections.plant.Plant;
import model.match.main.levels.special_levels.ConveyorBeltLevel;
import model.match_mechanisms.vector.Position;

class BoardInteraction {

    enum Tool { NONE, SHOVEL, FOOD }

    private final GameScreen screen;

    private String selectedPlant;
    private Plant selectedConveyorPlant;
    private Tool activeTool = Tool.NONE;
    private Actor boardInput;
    private float dragPreviewTime;

    BoardInteraction(GameScreen screen) {
        this.screen = screen;
    }

    String selectedPlant() {
        return selectedPlant;
    }

    void clearSelectedPlant() {
        selectedPlant = null;
        selectedConveyorPlant = null;
    }

    Tool activeTool() {
        return activeTool;
    }

    private boolean isCutscene() {
        return screen.session != null && screen.session.isCutsceneActive();
    }

    void createBoardInput() {
        boardInput = new Actor();
        boardInput.setTouchable(Touchable.enabled);
        boardInput.setBounds(GameScreen.BOARD_X, GameScreen.BOARD_Y, screen.boardWidth(),
                screen.boardHeight() + BoardLayout.FALLING_SUN_CLICK_HEIGHT);
        boardInput.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Vector2 click = new Vector2(event.getStageX(), event.getStageY());
                if (!screen.collectUnderMouse(click)) {
                    handleBoardClick(click.x, click.y);
                }
            }
        });
        screen.rootStack.addActorBefore(screen.hud, boardInput);
    }

    void resizeBoardInput() {
        if (boardInput != null) {
            boardInput.setBounds(GameScreen.BOARD_X, GameScreen.BOARD_Y, screen.boardWidth(),
                    screen.boardHeight() + BoardLayout.FALLING_SUN_CLICK_HEIGHT);
        }
    }

    void handlePlantDragRelease(Vector2 stagePosition) {
        if (screen.paused || screen.matchFinished || stagePosition == null) return;
        if (isCutscene()) return;

        float x = stagePosition.x;
        float y = stagePosition.y;
        if (x < GameScreen.BOARD_X || y < GameScreen.BOARD_Y
                || x >= GameScreen.BOARD_X + screen.boardWidth()
                || y >= GameScreen.BOARD_Y + screen.boardHeight()) {
            return;
        }

        int col = (int) ((x - GameScreen.BOARD_X) / screen.getBoardTileWidth());
        int row = screen.session.getRows() - 1 - (int) ((y - GameScreen.BOARD_Y) / screen.getBoardTileHeight());
        if (row < 0 || row >= screen.session.getRows() || col < 0 || col >= screen.session.getCols()) return;

        if (selectedConveyorPlant == null && screen.session.getLevel() instanceof ConveyorBeltLevel conveyor
                && conveyor.getCurrentPlant() != null) {
            selectConveyorPlant(conveyor.getCurrentPlant());
        }
        if (selectedPlant == null && selectedConveyorPlant == null) return;

        plantAtCell(row, col);
    }

    void selectPlant(String plantName) {
        if (screen.paused || screen.matchFinished) return;
        if (screen.session.getLevel() instanceof ConveyorBeltLevel conveyor) {
            selectConveyorPlant(conveyor.getCurrentPlant());
            activeTool = Tool.NONE;
            return;
        }
        selectedPlant = plantName;
        activeTool = Tool.NONE;
    }

    void selectConveyorPlant(Plant plant) {
        if (screen.paused || screen.matchFinished) return;
        selectedConveyorPlant = plant;
        selectedPlant = plant == null ? null : plant.getName();
        activeTool = Tool.NONE;
    }

    void armTool(Tool tool) {
        if (screen.paused || screen.matchFinished) return;
        activeTool = activeTool == tool ? Tool.NONE : tool;
        selectedPlant = null;
        selectedConveyorPlant = null;
    }

    private void handleBoardClick(float x, float y) {
        if (screen.paused || screen.matchFinished || isCutscene()) return;
        if (x < GameScreen.BOARD_X || y < GameScreen.BOARD_Y
                || x >= GameScreen.BOARD_X + screen.boardWidth()
                || y >= GameScreen.BOARD_Y + screen.boardHeight()) return;

        int col = (int) ((x - GameScreen.BOARD_X) / screen.getBoardTileWidth());
        int row = screen.session.getRows() - 1 - (int) ((y - GameScreen.BOARD_Y) / screen.getBoardTileHeight());
        if (row < 0 || row >= screen.session.getRows() || col < 0 || col >= screen.session.getCols()) return;
        screen.onCellClicked(row, col);
    }

    void plantAtCell(int row, int col) {
        int commandX = col + 1;
        int commandY = row + 1;
        String command;

        if (activeTool == Tool.SHOVEL) {
            boolean removed = screen.session.removePlantAt(row, col);
            if (!removed) {
                Toast.show(screen.stage, "There is no plant to remove here.");
                return;
            }
            activeTool = Tool.NONE;
            selectedPlant = null;
            return;
        } else if (activeTool == Tool.FOOD) {
            command = "feed plant -l (" + commandX + ", " + commandY + ")";
        } else if (screen.session.getLevel() instanceof ConveyorBeltLevel conveyor) {
            Plant offered = selectedConveyorPlant != null ? selectedConveyorPlant : conveyor.getCurrentPlant();
            if (offered == null) return;

            if (!screen.session.plantAt(row, col, offered)) {
                Toast.show(screen.stage, "That tile is occupied, blocked, or out of bounds.");
                return;
            }

            conveyor.takeConveyorPlant(offered);
            selectedConveyorPlant = null;
            selectedPlant = null;
            activeTool = Tool.NONE;
            return;
        } else if (selectedPlant != null) {
            command = "plant plant -t " + selectedPlant + " -l (" + commandX + ", " + commandY + ")";
        } else {
            return;
        }

        if (screen.runCommand(command)) {
            if (activeTool != Tool.SHOVEL && activeTool != Tool.FOOD) selectedPlant = null;
            if (activeTool != Tool.NONE) activeTool = Tool.NONE;
        }
    }

    boolean collectUnderMouse(Vector2 click) {
        if (screen.paused || screen.matchFinished || click == null) return false;
        if (isCutscene()) return false;

        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();

        Vector2 world = click;

        for (model.collections.Item raw : screen.session.getItems()) {
            if (!(raw instanceof GroundItem item) || !item.isAlive() || item.isCollected()
                    || item.getPosition() == null) continue;

            Position p = item.getPosition();
            // Use the item's own (possibly off-tile-center) position for both the hit
            // test and the collect target, so a sun that renders nudged away from its
            // producer's tile - e.g. a sunflower's second sun, or a sun-shroom's drop -
            // is only picked up by clicking where it's actually drawn, matching the
            // original game instead of the plant's tile underneath it.
            float itemX = GameScreen.BOARD_X + (float) p.x() * boardTileWidth + boardTileWidth * 0.28f;
            float itemY = screen.cellY((int) Math.round(p.y())) + boardTileHeight * 0.25f;

            if (item instanceof GroundSun sun && sun.isFalling()) {
                float progress = sun.getFallProgress();
                itemY = GameScreen.BOARD_Y + screen.boardHeight() + 35f
                        + (itemY - (GameScreen.BOARD_Y + screen.boardHeight() + 35f)) * progress;

                float age = screen.groundItems().animTimeFor(item);
                itemY += (float) Math.sin(age * 3.0f) * 3f;
            }

            // Hitbox is centered on the sprite but deliberately more generous than the
            // sprite itself (fixed size, not tied to the pulse animation) so collecting
            // doesn't require pixel-precise clicks - matching how forgiving the original
            // game's sun/coin/food clicking feels.
            float spriteSize = boardTileWidth * 0.45f;
            float centerX = itemX + spriteSize * 0.5f;
            float centerY = itemY + boardTileHeight * 0.45f * 0.5f;
            float radius = Math.max(boardTileWidth, boardTileHeight) * 0.55f;

            if (Math.abs(world.x - centerX) <= radius && Math.abs(world.y - centerY) <= radius) {
                screen.session.collectItemsNear(p);
                return true;
            }
        }

        return false;
    }

    Vector2 mouseWorld() {
        Vector3 screenCoordinates = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        screen.stage.getViewport().unproject(screenCoordinates);
        return new Vector2(screenCoordinates.x, screenCoordinates.y);
    }

    void drawDragPreview(float delta) {
        Vector2 mouse = mouseWorld();
        if (mouse == null) return;

        float boardTileWidth = screen.getBoardTileWidth();

        if (activeTool == Tool.SHOVEL) {
            Texture shovelIconTexture = screen.assets().shovelIconTexture();
            if (shovelIconTexture != null) {
                float size = boardTileWidth * 0.7f;
                float drawX = mouse.x - size * 0.5f;
                float drawY = mouse.y - size * 0.5f;
                screen.batch.setColor(Color.WHITE);
                screen.batch.draw(shovelIconTexture, drawX, drawY, size, size);
            }
            return;
        }

        if (activeTool != Tool.NONE || selectedPlant == null) return;

        dragPreviewTime += delta;
        float size = boardTileWidth * 0.8f;
        float drawX = mouse.x - size * 0.5f;
        float drawY = mouse.y - size * 0.5f;

        screen.batch.setColor(1f, 1f, 1f, 0.85f);
        String path = AnimationFactory.pathForDisplayName(selectedPlant);
        boolean drawn = screen.drawPam(path, "idle", dragPreviewTime, drawX + size * 0.15f, drawY, 0.5f, false);
        screen.batch.setColor(Color.WHITE);
        if (!drawn) {
            TextureRegion region = GameAssetManager.get().getPlantRegion(selectedPlant);
            screen.drawEntity(region, drawX, drawY, size, size, new Color(0.2f, 0.65f, 0.22f, 0.85f),
                    GameScreenGraphics.initials(selectedPlant));
        }
    }

    void drawHover(float bw, float bh) {
        Vector2 mouse = mouseWorld();
        if (mouse == null) return;
        float boardTileWidth = screen.getBoardTileWidth();
        float boardTileHeight = screen.getBoardTileHeight();
        int col = (int) ((mouse.x - GameScreen.BOARD_X) / boardTileWidth);
        int row = screen.session.getRows() - 1 - (int) ((mouse.y - GameScreen.BOARD_Y) / boardTileHeight);
        if (row < 0 || row >= screen.session.getRows() || col < 0 || col >= screen.session.getCols()) return;
        boolean active = selectedPlant != null || activeTool != Tool.NONE
                || screen.session.getLevel() instanceof ConveyorBeltLevel;
        screen.batch.setColor(active ? new Color(0.55f, 1f, 0.55f, 0.22f) : new Color(1f, 1f, 1f, 0.12f));
        screen.batch.draw(screen.whitePixel, GameScreen.BOARD_X + col * boardTileWidth, screen.cellY(row),
                boardTileWidth, boardTileHeight);
        screen.batch.setColor(Color.WHITE);
    }
}