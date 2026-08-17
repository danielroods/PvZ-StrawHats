package view.screens.mini_games;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import controller.ScreenManager;
import controller.mini_games.BeghouledController;
import model.App;
import model.match.mini_games.Beghouled;
import service.GameClock;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.general_screens.GameScreen;

/**
 * Beghouled (match-3) mini-game gameplay screen. The board itself (plants as
 * "gems", zombies wandering the lanes, sun/items falling) is drawn by the
 * inherited GameScreen exactly like a normal chapter - that machinery doesn't
 * need to change. What's genuinely different about Beghouled, and is
 * therefore fully overridden here rather than in GameScreen:
 * <ul>
 *     <li>ticking: Beghouled runs its own model (endless zombie waves + the
 *     match-goal win condition) on top of the GameSession, so the
 *     BeghouledController - not the raw session - has to be what's ticked
 *     each frame (see {@link #tickSession()}/{@link #checkMatchEnd()}).</li>
 *     <li>input: there's no seed tray to drag from - tapping a tile selects
 *     it, tapping an adjacent tile swaps them (see
 *     {@link #createBoardInput()}).</li>
 *     <li>HUD: sun + match progress + one upgrade button per plant in the
 *     model's upgrade table, instead of the tower-defense seed-packet tray
 *     (see {@link #createHud()}).</li>
 * </ul>
 */
public class BeghouledGameScreen extends GameScreen {

    private static final String FOLDER = "assets/images/backg/mini_games/begh/";

    private Label sunLabel;
    private Label matchesLabel;
    private Table hudTable;

    private int selectedRow = -1;
    private int selectedCol = -1;
    private Actor boardInputActor;

    public BeghouledGameScreen() {
        // Used by GameScreen.getLawnMowerSeasonKey() (mini-game sessions have no
        // Level/Season to read a name from) to pick the right mower art.
        seasonFolder = "beghouled";
    }

    @Override
    protected String getSeasonGameplayFolder() {
        return FOLDER;
    }

    @Override
    protected String getGameplayBackgroundPath() {
        return getSeasonGameplayFolder() + "texture.png";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
    }

    // ------------------------------------------------------------------
    // Ticking / win-loss
    // ------------------------------------------------------------------

    @Override
    protected void tickSession() {
        if (App.currentMenu instanceof BeghouledController controller) {
            controller.tick(GameClock.SECONDS_PER_TICK);
        }
    }

    @Override
    protected void checkMatchEnd() {
        if (matchFinished) return;
        // BeghouledController.tick()/handleCommand() already run the win/loss check
        // (reportOutcome()) and swap App.currentMenu to MiniGameEndMenu themselves -
        // this just needs to notice that happened and hand off to the new screen.
        if (!(App.currentMenu instanceof BeghouledController)) {
            matchFinished = true;
            ScreenManager.syncWithCurrentMenu();
        }
    }

    // ------------------------------------------------------------------
    // Input: tap a tile to select it, tap an adjacent tile to attempt a swap.
    // The swap command itself already reverts if it wouldn't create a match.
    // ------------------------------------------------------------------

    @Override
    protected void createBoardInput() {
        Actor input = new Actor();
        input.setTouchable(Touchable.enabled);
        input.setBounds(BOARD_X, BOARD_Y, session.getCols() * getBoardTileWidth(),
                session.getRows() * getBoardTileHeight());
        input.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleTileClick(event.getStageX(), event.getStageY());
            }
        });
        addBeforeModal(input);
    }

    private void handleTileClick(float stageX, float stageY) {
        if (matchFinished || session == null) return;

        float tileW = getBoardTileWidth();
        float tileH = getBoardTileHeight();
        int col = (int) ((stageX - BOARD_X) / tileW);
        int row = session.getRows() - 1 - (int) ((stageY - BOARD_Y) / tileH);
        if (row < 0 || row >= session.getRows() || col < 0 || col >= session.getCols()) return;

        if (selectedRow < 0) {
            selectedRow = row;
            selectedCol = col;
            return;
        }
        if (selectedRow == row && selectedCol == col) {
            selectedRow = -1;
            selectedCol = -1;
            return;
        }

        boolean adjacent = Math.abs(selectedRow - row) + Math.abs(selectedCol - col) == 1;
        if (!adjacent) {
            // Not next to the first pick - start a new selection from here instead.
            selectedRow = row;
            selectedCol = col;
            return;
        }

        runCommand("swap -l (" + (selectedCol + 1) + ", " + (selectedRow + 1) + ") -l ("
                + (col + 1) + ", " + (row + 1) + ")");
        selectedRow = -1;
        selectedCol = -1;
    }

    @Override
    protected void drawSeasonForegroundEffects(float delta, float bw, float bh) {
        if (selectedRow < 0 || selectedCol < 0 || whitePixel == null) return;

        float tileW = getBoardTileWidth();
        float tileH = getBoardTileHeight();
        float x = BOARD_X + selectedCol * tileW;
        float y = getCellCenterY(selectedRow) - tileH / 2f;
        float thickness = 4f;

        batch.setColor(1f, 0.92f, 0.2f, 0.9f);
        batch.draw(whitePixel, x, y, tileW, thickness);
        batch.draw(whitePixel, x, y + tileH - thickness, tileW, thickness);
        batch.draw(whitePixel, x, y, thickness, tileH);
        batch.draw(whitePixel, x + tileW - thickness, y, thickness, tileH);
        batch.setColor(Color.WHITE);
    }

    // ------------------------------------------------------------------
    // HUD: sun + match progress + one upgrade button per upgradeable plant.
    // ------------------------------------------------------------------

    @Override
    protected void createHud() {
        Table root = new Table();
        root.setFillParent(true);
        root.top();
        root.pad(16f);

        Table topBar = new Table();
        topBar.add(secondaryButton("Back", () -> runCommand("menu exit"))).size(110, 46).padRight(20);
        sunLabel = createLabel("Sun: 0", "title");
        topBar.add(sunLabel).padRight(24);
        matchesLabel = createLabel("Matches: 0/0", "title");
        topBar.add(matchesLabel).expandX().left();
        root.add(topBar).fillX().row();

        Table upgradeRow = new Table();
        if (App.currentMenu instanceof BeghouledController controller) {
            Beghouled game = controller.getGame();
            for (String plantName : game.getUpgradeablePlantNames()) {
                int cost = game.getUpgradeCost(plantName);
                upgradeRow.add(primaryButton(plantName + " (" + cost + ")",
                        () -> runCommand("upgrade -t " + plantName))).size(190, 52).padRight(8);
            }
        }
        root.add(upgradeRow).left().padTop(10).row();

        hudTable = root;
        addBeforeModal(hudTable);
    }

    @Override
    protected void refreshHud(float delta) {
        if (sunLabel == null || matchesLabel == null) return;
        if (!(App.currentMenu instanceof BeghouledController controller)) return;

        Beghouled game = controller.getGame();
        sunLabel.setText("Sun: " + game.getSession().getSunCount());
        matchesLabel.setText("Matches: " + game.getMatchesMade() + "/" + game.getMatchesNeeded());
    }
}