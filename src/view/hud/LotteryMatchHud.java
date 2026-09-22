package view.hud;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import model.match.endless.EndlessRun;


public class LotteryMatchHud extends Table implements Disposable {

    private final Label pointsLabel;
    private final Label recordLabel;
    private final Label waveLabel;
    private final Label killsLabel;
    private final Label comboLabel;
    private Texture backgroundTexture;

    public LotteryMatchHud(Skin skin) {
        super(skin);
        setFillParent(true);
        setTouchable(Touchable.disabled);
        top().right();
        padTop(65f).padRight(15f);

        Label.LabelStyle titleStyle = skin.has("title", Label.LabelStyle.class)
                ? skin.get("title", Label.LabelStyle.class)
                : skin.get(Label.LabelStyle.class);
        Label.LabelStyle defaultStyle = skin.get(Label.LabelStyle.class);

        pointsLabel = styled(new Label("Meow Points: 0", titleStyle), Color.GOLD);
        recordLabel = styled(new Label("Record: -", defaultStyle), Color.LIGHT_GRAY);
        waveLabel = styled(new Label("Wave 1", defaultStyle), Color.WHITE);
        killsLabel = styled(new Label("Kills: 0", defaultStyle), Color.CYAN);
        comboLabel = styled(new Label("Combo: x0", defaultStyle), Color.ORANGE);

        Table box = new Table(skin);
        TextureRegionDrawable background = createBackground();
        if (background != null) {
            box.setBackground(background);
        } else if (skin.has("dialog", TextureRegionDrawable.class)) {
            box.setBackground(skin.getDrawable("dialog"));
        }
        box.pad(10f, 18f, 10f, 18f);
        box.add(pointsLabel).center().padBottom(2f).row();
        box.add(recordLabel).center().padBottom(4f).row();
        box.add(waveLabel).center().padBottom(4f).row();

        Table stats = new Table(skin);
        stats.add(killsLabel).padRight(12f);
        stats.add(comboLabel);
        box.add(stats).center();

        add(box).right();
    }

    private static Label styled(Label label, Color color) {
        label.setAlignment(Align.center);
        label.setColor(color);
        return label;
    }

    
    public void update(EndlessRun run, int waveNumber, long record) {
        if (run == null) return;
        pointsLabel.setText("Meow Points: " + run.getScore());
        recordLabel.setText(record < 0 ? "Record: -" : "Record: " + record);
        waveLabel.setText("Wave " + Math.max(1, waveNumber));
        killsLabel.setText("Kills: " + run.getKills());
        comboLabel.setText("Combo: x" + run.getCombo());
    }

    private TextureRegionDrawable createBackground() {
        try {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(0f, 0f, 0f, 0.75f);
            pixmap.fill();
            backgroundTexture = new Texture(pixmap);
            pixmap.dispose();
            return new TextureRegionDrawable(new TextureRegion(backgroundTexture));
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void dispose() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
    }
}
