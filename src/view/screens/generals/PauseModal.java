package view.screens.generals;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

class PauseModal extends Modal {

    private static final String TOPPER_PATH = "assets/images/ui/windowtopper.png";
    private static final String BUTTON_PATH = "assets/images/ui/demo.png";

    PauseModal(GameScreen screen) {
        content.clearChildren();
        content.setBackground(pauseBackground());
        content.pad(35, 28, 20, 28);

        Label title = new Label("PAUSED", skin, "title");
        title.setColor(new Color(0.22f, 0.22f, 0.22f, 1f));
        content.add(title).padTop(2).padBottom(6).row();

        TextButton resume = pauseButton("Resume");
        resume.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                screen.paused = false;
                hide();
            }
        });
        content.add(resume).size(220, 50).pad(3).row();

        TextButton restart = pauseButton("Restart");
        restart.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                if (screen.runCommand("restart")) {
                    hide();
                    controller.ScreenManager.forceResync();
                }
            }
        });
        content.add(restart).size(220, 50).pad(3).row();

        TextButton exit = pauseButton("Save & Exit");
        exit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                screen.runCommand("menu exit");
                hide();
                controller.ScreenManager.syncWithCurrentMenu();
            }
        });
        content.add(exit).size(220, 50).pad(3);

        if (Gdx.files.internal(TOPPER_PATH).exists()) {
            Image topper = new Image(new TextureRegionDrawable(
                    new TextureRegion(new Texture(Gdx.files.internal(TOPPER_PATH)))));

            topper.setSize(600, 116);
            content.addActor(topper);

            content.addAction(new com.badlogic.gdx.scenes.scene2d.actions.LayoutAction() {
                @Override
                public boolean act(float delta) {
                    content.layout();

                    topper.setPosition(
                            content.getWidth() / 2f - topper.getWidth() / 2f,
                            content.getHeight() - topper.getHeight() / 2f
                    );

                    return true;
                }
            });
        }
    }

    private TextButton pauseButton(String text) {
        TextButton.TextButtonStyle style =
                new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));

        if (Gdx.files.internal(BUTTON_PATH).exists()) {
            Drawable background = new TextureRegionDrawable(
                    new TextureRegion(new Texture(Gdx.files.internal(BUTTON_PATH))));

            style.up = background;
            style.over = background;
            style.down = background;
            style.checked = background;
        }

        style.fontColor = new Color(0.20f, 0.20f, 0.20f, 1f);
        style.overFontColor = new Color(0.08f, 0.08f, 0.08f, 1f);

        return new TextButton(text, style);
    }

    private Drawable pauseBackground() {
        int size = 512;
        int radius = 42;

        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();

        Color cream = new Color(1f, 0.965f, 0.84f, 1f);
        Color gray = new Color(0.84f, 0.84f, 0.82f, 1f);

        int topBand = 74;
        int bottomBand = 82;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean inside = true;

                if (x < radius && y < radius)
                    inside = (x - radius) * (x - radius)
                            + (y - radius) * (y - radius) <= radius * radius;
                else if (x >= size - radius && y < radius)
                    inside = (x - (size - radius - 1)) * (x - (size - radius - 1))
                            + (y - radius) * (y - radius) <= radius * radius;
                else if (x < radius && y >= size - radius)
                    inside = (x - radius) * (x - radius)
                            + (y - (size - radius - 1)) * (y - (size - radius - 1))
                            <= radius * radius;
                else if (x >= size - radius && y >= size - radius)
                    inside = (x - (size - radius - 1)) * (x - (size - radius - 1))
                            + (y - (size - radius - 1))
                            * (y - (size - radius - 1)) <= radius * radius;

                if (inside) {
                    if (y < bottomBand || y >= size - topBand)
                        pixmap.setColor(gray);
                    else
                        pixmap.setColor(cream);

                    pixmap.drawPixel(x, y);
                }
            }
        }

        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        return new TextureRegionDrawable(new TextureRegion(texture));
    }
}

