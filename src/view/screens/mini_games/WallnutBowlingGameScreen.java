package view.screens.mini_games;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import controller.mini_games.WallnutBowlingController;
import model.App;
import model.match.mini_games.wallnutbowlling.WallnutBowling;
import model.match.mini_games.wallnutbowlling.nut.Nut;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.general_screens.GameScreen;
import view.general_screens.Toast;

/**
 * WallnutBowlingGameScreen mini-game gameplay entry point. The actual gameplay machinery
 * lives in GameScreen, exactly like the regular chapter stages; this class only
 * points it at the mini-game's own art folder (background, left/right border
 * textures, and grave icon all come from the same folder - see
 * GameScreen.getSeasonGameplayFolder()) and wires up Wallnut-Bowling-specific
 * interaction: clicking a launch lane (left of the red line) fires the next nut
 * on the conveyor down that row, same as "plant nut -l (x,y)" in the terminal
 * engine. Rolling nuts and the conveyor queue are not part of GameScreen's normal
 * rendering, so this screen draws its own on top.
 */
public class WallnutBowlingGameScreen extends GameScreen {

    {
        // Mini-games have no Level/Season, so this doubles as the lawn mower art
        // key - see GameScreen.getLawnMowerSeasonKey() and SEASON_LAWN_MOWER_PAM_PATHS.
        seasonFolder = "wallnutbowlling";
    }

    private Table conveyorPanel;

    @Override
    protected String getSeasonGameplayFolder() {
        return "assets/images/backg/mini_games/wallnut/";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
        buildConveyorPanel();
    }

    private WallnutBowling currentGame() {
        return App.currentMenu instanceof WallnutBowlingController controller ? controller.getGame() : null;
    }

    /**
     * Clicking a lane left of the red line launches the next conveyor nut down
     * that row. Clicking right of the red line (where the zombies roam) does
     * nothing - nuts aren't planted there, they roll into it.
     */
    @Override
    protected void onCellClicked(int row, int col) {
        WallnutBowling game = currentGame();
        if (game == null) {
            super.onCellClicked(row, col);
            return;
        }

        if (col > game.getRedLineColumn()) {
            Toast.show(stage, "Nuts launch from behind the red line and roll the rest of the way.");
            return;
        }
        if (game.getNextNutKind() == null) {
            Toast.show(stage, "The conveyor belt is empty.");
            return;
        }
        runCommand("plant nut -t " + game.getNextNutKind() + " -l (" + (col + 1) + ", " + (row + 1) + ")");
        buildConveyorPanel();
    }

    @Override
    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {
        WallnutBowling game = currentGame();
        if (game == null) return;

        float redLineX = getCellX(game.getRedLineColumn() + 1);
        drawFallback(redLineX - 2f, getBoardBottom(), 4f, bh, new Color(0.85f, 0.2f, 0.2f, 0.85f));

        for (Nut nut : game.getActiveNuts()) {
            if (!nut.isAlive()) continue;
            float x = getCellX(0) + (float) nut.getPosition().x() * getBoardTileWidth();
            float y = getCellY(0) + (float) nut.getPosition().y() * getBoardTileHeight();
            drawFallback(x + getBoardTileWidth() * 0.32f, y + getBoardTileHeight() * 0.28f,
                    getBoardTileWidth() * 0.36f, getBoardTileHeight() * 0.44f, nutColor(nut));
        }
    }

    private Color nutColor(Nut nut) {
        return switch (nut.getKindName()) {
            case "ExplodeONut" -> new Color(0.9f, 0.35f, 0.1f, 1f);
            case "BigNut" -> new Color(0.6f, 0.42f, 0.2f, 1f);
            default -> new Color(0.82f, 0.65f, 0.35f, 1f);
        };
    }

    /**
     * Small conveyor-queue readout pinned to the bottom-left of the stage, since
     * MatchHud has no concept of Wallnut Bowling's nut conveyor. Added directly to
     * the stage (like Toast) rather than into rootStack, which force-stretches any
     * actor added to it - see GameScreen board-layout notes.
     */
    private void buildConveyorPanel() {
        WallnutBowling game = currentGame();
        if (conveyorPanel != null) {
            conveyorPanel.remove();
            conveyorPanel = null;
        }
        if (game == null) return;

        Table panel = new Table();
        panel.setBackground(skin.getDrawable("card-background"));
        panel.pad(8f);

        Label title = new Label("CONVEYOR", skin, "main");
        panel.add(title).left().padBottom(4f).row();

        StringBuilder belt = new StringBuilder();
        for (WallnutBowling.NutKind kind : game.getConveyorBelt()) {
            if (belt.length() > 0) belt.append("  ->  ");
            belt.append(kind.name());
        }
        if (belt.length() == 0) belt.append("(empty)");
        panel.add(new Label(belt.toString(), skin, "main")).left();
        panel.pack();
        panel.setPosition(24f, 24f);

        conveyorPanel = panel;
        stage.addActor(conveyorPanel);
    }
}
