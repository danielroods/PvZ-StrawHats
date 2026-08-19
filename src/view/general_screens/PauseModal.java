package view.general_screens;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import model.utils.GameSession;

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
                if (screen.runCommand("restart")) {
                    screen.session = GameSession.getInstance();
                    screen.tickAccumulator = 0;
                    screen.paused = false;
                    screen.matchFinished = false;
                    hide();
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
