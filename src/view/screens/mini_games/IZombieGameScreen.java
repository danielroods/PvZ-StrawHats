package view.screens.mini_games;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import controller.mini_games.ImZombieController;
import model.App;
import model.match.mini_games.izombie.IZombie;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.general_screens.GameScreen;
import view.general_screens.Toast;

import java.util.Map;

/**
 * IZombieGameScreen mini-game gameplay entry point. The actual gameplay machinery
 * lives in GameScreen, exactly like the regular chapter stages; this class only
 * points it at the mini-game's own art folder (background, left/right border
 * textures, and grave icon all come from the same folder - see
 * GameScreen.getSeasonGameplayFolder()) and wires up I-Zombie-specific interaction:
 * clicking a lane right of the red line places the currently selected roster
 * zombie there, same as "place zombie -t <alias> -l (x,y)" in the terminal engine.
 * The defending plants and sun zombies are real Plant/Zombie entities the same as
 * any other level, so GameScreen already draws them; the only thing missing is a
 * way to choose which zombie to place, since MatchHud's plant tray doesn't apply
 * here, so this screen draws its own roster picker.
 */
public class IZombieGameScreen extends GameScreen {

    {
        // Mini-games have no Level/Season, so this doubles as the lawn mower art
        // key - see GameScreen.getLawnMowerSeasonKey() and SEASON_LAWN_MOWER_PAM_PATHS.
        seasonFolder = "izombie";
    }

    private Table rosterPanel;
    private String selectedZombieAlias;

    @Override
    protected String getSeasonGameplayFolder() {
        return "assets/images/backg/mini_games/izombie/";
    }

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
        selectedZombieAlias = null;
        buildRosterPanel();
    }

    private IZombie currentGame() {
        return App.currentMenu instanceof ImZombieController controller ? controller.getGame() : null;
    }

    @Override
    protected void onCellClicked(int row, int col) {
        IZombie game = currentGame();
        if (game == null) {
            super.onCellClicked(row, col);
            return;
        }

        if (col <= game.getRedLineColumn()) {
            Toast.show(stage, "Zombies are placed right of the red line, past your own defenses.");
            return;
        }
        if (selectedZombieAlias == null) {
            Toast.show(stage, "Pick a zombie from the roster first.");
            return;
        }
        runCommand("place zombie -t " + selectedZombieAlias + " -l (" + (col + 1) + ", " + (row + 1) + ")");
        buildRosterPanel();
    }

    @Override
    protected void drawSeasonGameplayEffects(float delta, float bw, float bh) {
        IZombie game = currentGame();
        if (game == null) return;

        float redLineX = getCellX(game.getRedLineColumn() + 1);
        drawFallback(redLineX - 2f, getBoardBottom(), 4f, bh, new Color(0.85f, 0.2f, 0.2f, 0.85f));
    }

    /**
     * Roster picker pinned to the bottom-left of the stage, since MatchHud's plant
     * tray is loadout-based and has no concept of I Zombie's purchasable roster.
     * Added directly to the stage (like Toast) rather than into rootStack, which
     * force-stretches any actor added to it - see GameScreen board-layout notes.
     */
    private void buildRosterPanel() {
        IZombie game = currentGame();
        if (rosterPanel != null) {
            rosterPanel.remove();
            rosterPanel = null;
        }
        if (game == null) return;

        Table panel = new Table();
        panel.setBackground(skin.getDrawable("card-background"));
        panel.pad(8f);

        Label title = new Label("ROSTER (Sun: " + game.getSession().getSunCount() + ")", skin, "main");
        panel.add(title).left().padBottom(4f).row();

        Table row = new Table();
        row.left();
        for (Map.Entry<String, Integer> entry : game.getRoster().entrySet()) {
            String alias = entry.getKey();
            int cost = entry.getValue();
            TextButton button = new TextButton(alias + " (" + cost + ")", skin,
                    alias.equalsIgnoreCase(selectedZombieAlias) ? "default" : "secondary");
            button.setDisabled(cost > game.getSession().getSunCount());
            button.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    selectedZombieAlias = alias;
                    buildRosterPanel();
                }
            });
            row.add(button).padRight(6f);
        }

        ScrollPane scroll = new ScrollPane(row);
        scroll.setScrollingDisabled(false, true);
        panel.add(scroll).width(420f).height(48f);
        panel.pack();
        panel.setPosition(24f, 24f);

        rosterPanel = panel;
        stage.addActor(rosterPanel);
    }
}
