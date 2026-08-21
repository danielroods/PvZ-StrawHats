package view.screens.generals;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import controller.assets.GameAssetManager;

import java.util.Map;
import java.util.WeakHashMap;

public final class Toast extends Table {

    private static final float DEFAULT_DURATION = 2.5f;
    private static final float SLIDE_TIME = 0.4f;
    private static final float TOP_MARGIN = 24f;
    private static final float TOAST_WIDTH = 450f;
    private static final float TOAST_MAX_HEIGHT = 115f;

    private static final Map<Stage, Toast> activeByStage = new WeakHashMap<>();

    private Toast(String message, float duration, Stage stage) {
        super();

        Skin skin = GameAssetManager.get().getSkin();
        Label.LabelStyle labelStyle = skin.has("main", Label.LabelStyle.class)
                ? skin.get("main", Label.LabelStyle.class)
                : skin.get(Label.LabelStyle.class);

        Label label = new Label(message, labelStyle);
        label.setColor(Color.BLACK);
        label.setWrap(true);

        TextureRegionDrawable bg = backgroundDrawable();
        if (bg != null) {
            setBackground(bg);
            pad(8f, 32f, 8f, 32f);
            add(label).width(TOAST_WIDTH - 64f).center();
        } else {
            pad(18, 36, 18, 36);
            add(label).width(TOAST_WIDTH - 72f).center();
        }

        pack();
        if (getHeight() > TOAST_MAX_HEIGHT) {
            setHeight(TOAST_MAX_HEIGHT);
            clearChildren();
            add(label).width(TOAST_WIDTH - 64f).center();
        }
        setWidth(TOAST_WIDTH);

        float startX = (stage.getWidth() - getWidth()) * 0.5f;
        float startY = stage.getHeight();
        float targetY = stage.getHeight() - getHeight() - TOP_MARGIN;

        setPosition(startX, startY);

        addAction(Actions.sequence(
                Actions.moveTo(startX, targetY, SLIDE_TIME, Interpolation.pow2Out),
                Actions.delay(duration),
                Actions.moveTo(startX, startY, SLIDE_TIME, Interpolation.pow2In),
                Actions.removeActor(),
                Actions.run(() -> activeByStage.remove(stage, this))
        ));
    }

    private static TextureRegionDrawable backgroundDrawable() {
        String path = "assets/images/ui/pop up.png";
        if (Gdx.files.internal(path).exists()) {
            Texture texture = new Texture(Gdx.files.internal(path), true);
            texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            return new TextureRegionDrawable(texture);
        }
        return null;
    }

    public static void show(Stage stage, String message) {
        if (stage == null || message == null) {
            return;
        }
        Toast previous = activeByStage.remove(stage);
        if (previous != null) {
            previous.remove();
        }
        Toast toast = new Toast(message, DEFAULT_DURATION, stage);
        stage.addActor(toast);
        activeByStage.put(stage, toast);
    }
}