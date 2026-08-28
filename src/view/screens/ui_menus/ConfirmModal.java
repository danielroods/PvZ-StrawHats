package view.screens.ui_menus;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import view.screens.generals.Modal;

class ConfirmModal extends Modal {

    private static final float MESSAGE_WIDTH = 380f;
    private static final float BUTTON_WIDTH = 170f;

    ConfirmModal(String title, String message, String confirmLabel, Runnable onConfirm) {
        this(title, message, confirmLabel, onConfirm, null);
    }

    ConfirmModal(String title, String message, String confirmLabel, Runnable onConfirm,
                 Runnable onCancel) {
        content.add(new Label(title, skin, "title")).colspan(2).padBottom(10).row();

        Label messageLabel = new Label(message, skin, "muted");
        messageLabel.setWrap(true);
        content.add(messageLabel).colspan(2).width(MESSAGE_WIDTH).padBottom(18).row();

        TextButton cancel = new TextButton("Cancel", skin, "secondary");
        TextButton confirm = new TextButton(confirmLabel, skin, "default");

        cancel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
                if (onCancel != null) onCancel.run();
            }
        });
        confirm.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
                onConfirm.run();
            }
        });

        content.add(cancel).width(BUTTON_WIDTH).padRight(8);
        content.add(confirm).width(BUTTON_WIDTH);
    }
}