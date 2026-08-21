package view.screens.ui_menus;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import view.screens.generals.Modal;

class ConfirmQuitModal extends Modal {

    private static final float BUTTON_WIDTH = 170f;

    ConfirmQuitModal(Runnable onConfirm) {
        content.add(new Label("PvZ", skin, "title")).colspan(2).padBottom(8).row();
        content.add(new Label("Quit Plants vs. Zombies?", skin, "title")).colspan(2).padBottom(12).row();
        content.add(new Label("Any unsaved progress will be lost.", skin, "muted")).colspan(2).padBottom(16).row();

        TextButton cancel = new TextButton("Cancel", skin, "secondary");
        TextButton quit = new TextButton("Quit", skin, "default");

        cancel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
            }
        });
        quit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onConfirm.run();
            }
        });

        content.add(cancel).width(BUTTON_WIDTH).padRight(8);
        content.add(quit).width(BUTTON_WIDTH);
    }
}