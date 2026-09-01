package view.screens.generals;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/** The in-match pause dialog: resume, restart the level, or save and leave. */
class PauseModal extends Modal {

    PauseModal(GameScreen screen) {
        content.add(new Label("PAUSED", skin, "title")).colspan(2).padBottom(15).row();
        TextButton resume = new TextButton("Resume", skin);
        resume.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { screen.paused = false; hide(); } });
        content.add(resume).size(220, 50).pad(5).row();
        TextButton restart = new TextButton("Restart", skin);
        restart.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                // "restart" (see GameplayMenu.restartMatch()) now sets App.currentMenu
                // to BeforeMenu/GameplayMenu itself rather than mutating this screen's
                // session in place, so the old approach of patching
                // screen.session/tickAccumulator/paused/matchFinished on this
                // soon-to-be-discarded GameScreen instance no longer applies - the
                // screen swap (loading screen -> before-match/gameplay, matching
                // whatever level this is, lottery/danger nodes included) needs to go
                // through ScreenManager instead, same as "menu exit" already does
                // below. Without this call the new menu wouldn't take effect until
                // some other code path happened to poll ScreenManager next.
                if (screen.runCommand("restart")) {
                    hide();
                    controller.ScreenManager.syncWithCurrentMenu();
                }
            }
        });
        content.add(restart).size(220, 50).pad(5).row();
        TextButton exit = new TextButton("Save & Exit", skin);
        exit.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                screen.runCommand("menu exit");
                hide();
                controller.ScreenManager.syncWithCurrentMenu();
            }
        });
        content.add(exit).size(220, 50).pad(5);
    }
}