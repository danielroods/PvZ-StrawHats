package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import controller.ScreenManager;
import controller.match.mini_games.MiniGameEndMenu;
import model.App;
import view.screens.generals.UiScreen;

public class MiniGameEndScreen extends UiScreen {

    private static final String BACKGROUND = "assets/images/backg/mainmenu_background.png";

    @Override
    public void show() {
        setBackground(BACKGROUND);
        super.show();
        build();
    }

    private void build() {
        rootTable.clear();

        MiniGameEndMenu menu = App.currentMenu instanceof MiniGameEndMenu m ? m : null;
        boolean won = menu != null && menu.isWon();

        Table panel = new Table();
        panel.setBackground(skin.getDrawable("modal-background"));
        panel.pad(34);

        Label title = new Label(won ? "YOU WIN!" : "GAME OVER", skin, "title");
        title.setColor(won ? Color.GREEN : Color.RED);
        panel.add(title).center().padBottom(10).row();

        if (menu != null) {
            panel.add(new Label(menu.getGameName(), skin, "main")).center().padBottom(16).row();
        }

        String details = menu == null ? "" : menu.getDetails();
        if (!details.isBlank()) {
            Label summary = new Label(details, skin, "main");
            summary.setWrap(true);
            summary.setAlignment(1);
            panel.add(summary).width(560).padBottom(22).row();
        }

        Table buttons = new Table();

        if (menu != null && menu.canRestart()) {
            TextButton playAgain = new TextButton("Play Again", skin);
            playAgain.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    menu.restart();
                    ScreenManager.syncWithCurrentMenu();
                }
            });
            buttons.add(playAgain).width(200).height(54).pad(6);
        }

        TextButton exit = new TextButton("Back to Travel Log", skin);
        exit.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                runCommand("menu exit");
            }
        });
        buttons.add(exit).width(240).height(54).pad(6);

        panel.add(buttons);
        rootTable.add(panel).center();
    }
}
