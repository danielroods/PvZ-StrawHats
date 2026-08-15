package view.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import controller.menus.match.AfterMenu;
import controller.menus.match.MatchMenu;
import model.App;
import model.match.main.levels.Level;
import model.utils.GameSession;
import view.general_screens.UiScreen;

/**
 * Dedicated graphical end-of-match screen. AfterMenu still owns rewards and progression;
 * this class is presentation only.
 */
public class AfterMatchScreen extends UiScreen {

    private static final String BACKGROUND = "assets/images/backg/mainmenu_background.png";
    private boolean shown;

    @Override
    public void show() {
        super.show();
        build();
    }

    private void build() {
        rootTable.clear();
        rootTable.setBackground(new TextureRegionDrawable(loadTextureSafe(BACKGROUND)));

        AfterMenu menu = App.currentMenu instanceof AfterMenu ? (AfterMenu) App.currentMenu : null;
        String text = menu == null ? "Match finished." : menu.showMenu();
        boolean won = text.startsWith("YOU WIN");

        Table panel = new Table();
        panel.setBackground(skin.getDrawable("modal-background"));
        panel.pad(34);

        Label title = new Label(won ? "LEVEL COMPLETE!" : "LEVEL FAILED", skin, "title");
        title.setColor(won ? Color.GREEN : Color.RED);
        panel.add(title).center().padBottom(16).row();

        Label summary = new Label(text, skin, "main");
        summary.setWrap(true);
        summary.setAlignment(1);
        panel.add(summary).width(650).padBottom(22).row();

        Table buttons = new Table();

        if (!won) {
            TextButton retry = new TextButton("Retry", skin);
            retry.addListener(new ClickListener() {
                @Override public void clicked(InputEvent event, float x, float y) {
                    MatchMenu menu = new MatchMenu();
                    App.currentMenu = menu;
                    runCommand("start game");
                }
            });
            buttons.add(retry).width(190).height(54).pad(6);
        }

        TextButton exit = new TextButton("Back to " + currentChapterMapLabel(), skin);
        exit.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                App.currentMenu = new MatchMenu();
                controller.ScreenManager.syncWithCurrentMenu();
            }
        });
        buttons.add(exit).width(240).height(54).pad(6);

        panel.add(buttons);
        rootTable.add(panel).center();
    }


    private String currentChapterMapLabel() {
        GameSession session = GameSession.peekInstance();
        Level level = session == null ? null : session.getLevel();
        String seasonName = level == null || level.getSeason() == null ? null : level.getSeason().getName();
        if (seasonName == null) return "Map";
        return seasonName + " Map";
    }

    @Override public void render(float delta) {
        super.render(delta);
    }
}