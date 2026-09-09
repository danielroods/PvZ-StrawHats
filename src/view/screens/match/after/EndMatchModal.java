package view.screens.match.after;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import view.screens.generals.GameScreen;
import view.screens.generals.Modal;


class EndMatchModal extends Modal {

    EndMatchModal(GameScreen screen, boolean won) {
        pad(24);
        add(new Label(won ? "LEVEL COMPLETE!" : "LEVEL FAILED", skin, "title")).padBottom(15).row();
        if (!won) {
            TextButton retry = new TextButton("Retry", skin);
            retry.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { screen.runCommand("restart"); hide(); } });
            add(retry).size(220, 50).pad(5).row();
        }
        TextButton exit = new TextButton("Exit", skin);
        exit.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { screen.runCommand("menu exit"); hide(); } });
        add(exit).size(220, 50).pad(5);
    }
}
