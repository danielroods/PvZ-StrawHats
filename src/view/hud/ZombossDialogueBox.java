package view.hud;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

public class ZombossDialogueBox extends Table implements Disposable {

    private static final float BOX_HEIGHT = 148f;
    private static final float SIDE_PAD = 60f;
    private static final float NPC_GUTTER = 330f;
    private static final float BOTTOM_PAD = 105f;

    private final Label speaker;
    private final Label body;
    private final Label hint;
    private final Texture boxTexture;
    private final Texture borderTexture;

    private Runnable advanceAction;

    public ZombossDialogueBox(Skin skin) {
        boxTexture = solidTexture(new Color(0.98f, 0.97f, 0.92f, 0.96f));
        borderTexture = solidTexture(new Color(0.25f, 0.20f, 0.10f, 1f));

        Table panel = new Table();
        panel.setBackground(new TextureRegionDrawable(new TextureRegion(boxTexture)));
        panel.pad(18f);

        speaker = new Label("DR. ZOMBOSS", skin, "title");
        speaker.setColor(new Color(0.45f, 0.10f, 0.12f, 1f));
        speaker.setAlignment(Align.left);

        body = new Label("", skin, "main");
        body.setColor(new Color(0.12f, 0.11f, 0.09f, 1f));
        body.setWrap(true);
        body.setAlignment(Align.topLeft);

        hint = new Label("click to continue", skin, "muted");
        hint.setColor(new Color(0.40f, 0.36f, 0.28f, 1f));
        hint.setAlignment(Align.right);

        panel.add(speaker).left().row();
        panel.add(body).growX().expandY().top().padTop(6f).row();
        panel.add(hint).right().padTop(4f);

        Table frame = new Table();
        frame.setBackground(new TextureRegionDrawable(new TextureRegion(borderTexture)));
        frame.pad(3f);
        frame.add(panel).grow();

        setFillParent(true);
        setTouchable(Touchable.enabled);
        bottom().left();
        add(frame).height(BOX_HEIGHT).growX()
                .padLeft(SIDE_PAD).padRight(NPC_GUTTER).padBottom(BOTTOM_PAD);

        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (advanceAction != null) advanceAction.run();
            }
        });
    }

    private static Texture solidTexture(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    public void setAdvanceAction(Runnable action) {
        this.advanceAction = action;
    }

    public void showLine(String line, int index, int total) {
        if (line == null) {
            setVisible(false);
            setTouchable(Touchable.disabled);
            return;
        }
        setVisible(true);
        setTouchable(Touchable.enabled);
        toFront();
        body.setText(line);
        hint.setText(total > 1 ? (index + 1) + " / " + total + "   click to continue"
                : "click to continue");
    }

    @Override
    public void dispose() {
        boxTexture.dispose();
        borderTexture.dispose();
    }
}