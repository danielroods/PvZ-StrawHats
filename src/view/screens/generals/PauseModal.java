package view.screens.generals;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;


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
                    hide();
                    controller.ScreenManager.forceResync();
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