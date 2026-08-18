package view.hud;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

/** لایه HUD لاتاری - قرارگیری در باکس مجزا بالا-راست بدون تداخل */
public class LotteryMatchHud extends Table {
    private final Label meowPointsLabel;
    private final Label comboLabel;
    private final Label killsLabel;
    private int currentPoints = 0;
    private int totalKills = 0;

    public LotteryMatchHud(Skin skin) {
        super(skin);
        setFillParent(true);
        top().right();
        padTop(65f).padRight(15f);

        Label.LabelStyle titleStyle = skin.has("title", Label.LabelStyle.class)
                ? skin.get("title", Label.LabelStyle.class)
                : skin.get(Label.LabelStyle.class);

        Label.LabelStyle defaultStyle = skin.get(Label.LabelStyle.class);

        meowPointsLabel = new Label("Meow Points: 0", titleStyle);
        meowPointsLabel.setAlignment(Align.center);
        meowPointsLabel.setColor(Color.GOLD);

        comboLabel = new Label("Combo: x0", defaultStyle);
        comboLabel.setAlignment(Align.center);
        comboLabel.setColor(Color.ORANGE);

        killsLabel = new Label("Kills: 0", defaultStyle);
        killsLabel.setAlignment(Align.center);
        killsLabel.setColor(Color.CYAN);

        Table boxContainer = new Table(skin);
        TextureRegionDrawable boxBackground = createSemiTransparentBackground();
        if (boxBackground != null) {
            boxContainer.setBackground(boxBackground);
        } else if (skin.has("dialog", TextureRegionDrawable.class)) {
            boxContainer.setBackground(skin.getDrawable("dialog"));
        }

        boxContainer.pad(10f, 18f, 10f, 18f);
        boxContainer.add(meowPointsLabel).center().padBottom(4f).row();

        Table subInfo = new Table(skin);
        subInfo.add(killsLabel).padRight(12f);
        subInfo.add(comboLabel);

        boxContainer.add(subInfo).center();
        add(boxContainer).right();
    }

    private TextureRegionDrawable createSemiTransparentBackground() {
        try {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(0f, 0f, 0f, 0.75f);
            pixmap.fill();
            Texture texture = new Texture(pixmap);
            pixmap.dispose();
            return new TextureRegionDrawable(new TextureRegion(texture));
        } catch (Throwable t) {
            return null;
        }
    }

    public void addPoints(int points) {
        this.currentPoints += points;
        if (meowPointsLabel != null) {
            meowPointsLabel.setText("Meow Points: " + currentPoints);
        }
    }

    public void incrementKills() {
        this.totalKills++;
        if (killsLabel != null) {
            killsLabel.setText("Kills: " + totalKills);
        }
    }

    public void updateCombo(int combo) {
        if (comboLabel != null) {
            comboLabel.setText("Combo: x" + combo);
        }
    }
}