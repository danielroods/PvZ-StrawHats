package view.screens.generals;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import controller.assets.GameAssetManager;
import controller.ScreenManager;

public class Modal extends Table {

    private static final float MIN_WIDTH = 380f;
    private static final float MAX_WIDTH = 620f;
    private static final float MAX_HEIGHT = 560f;

    protected final Skin skin;
    protected final Table content;

    public Modal() {
        BaseScreen activeScreen = ScreenManager.getScreen();
        skin = (activeScreen != null && activeScreen.skin != null)
                ? activeScreen.skin
                : GameAssetManager.get().getSkin();

        setFillParent(true);
        setTouchable(Touchable.enabled);

        // Full‑screen scrim (the semi‑transparent overlay)
        Image scrim = new Image(scrimDrawable());
        scrim.setFillParent(true);
        scrim.setTouchable(Touchable.childrenOnly);

        // Dialog panel – add your widgets to this table.
        content = new Table();
        content.setBackground(skin.getDrawable("modal-background"));
        content.pad(28).defaults().pad(6);
        content.setTouchable(Touchable.enabled);

        // Wrapper centres the content panel.
        Table wrapper = new Table();
        wrapper.setFillParent(true);
        wrapper.setTouchable(Touchable.enabled);
        // Do NOT use expand().fill() – that would stretch the panel.
        wrapper.add(content).center()
                .minWidth(MIN_WIDTH).maxWidth(MAX_WIDTH)
                .maxHeight(MAX_HEIGHT);

        Stack stack = new Stack();
        stack.setTouchable(Touchable.enabled);
        stack.add(scrim);
        stack.add(wrapper);

        add(stack).grow();

        // Click on the scrim (outside the dialog) closes the modal.
        scrim.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.getTarget() == scrim) {
                    hide();
                }
            }
        });
    }

    private static TextureRegionDrawable scrimDrawable() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0.55f);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    public void show() {
        BaseScreen screen = ScreenManager.getScreen();
        if (screen != null) {
            screen.getModalStack().add(this);
            this.toFront();
            this.setVisible(true);
        }
    }

    public void hide() {
        this.remove();
    }
}