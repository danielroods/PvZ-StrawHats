package view.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import model.pitches.obstacles.Grave;

public class GraveActor extends Actor {

    /** Which chapter's grave art set to use. */
    public enum Chapter { EGYPT, DARK_AGE }

    private final Grave grave;
    private final Chapter chapter;

    private Texture graveTexture;
    private int lastStage = -1;

    public GraveActor(float x, float y, float width, float height, Grave grave, Chapter chapter) {
        this.grave = grave;
        this.chapter = chapter;

        setBounds(x, y, width, height);
        updateTexture();
    }

    private String getImagePath() {
        int stage = grave.getStage();

        if (chapter == Chapter.EGYPT) {
            return "images/chapters/egypt/gameplay/egypt_grave/egyptgrave" + stage + ".png";
        }

        return switch (grave.getReward()) {
            case PLANT_FOOD -> "images/chapters/darkage/gameplay/plantfood_grave/darkgraveplantfood" + stage + ".png";
            case SUN -> "images/chapters/darkage/gameplay/sun_grave/darkgravesun" + stage + ".png";
            case NONE -> "images/chapters/darkage/gameplay/dark_grave/darknoop" + stage + ".png";
        };
    }

    private void updateTexture() {
        int stage = grave.getStage();
        if (stage == lastStage && graveTexture != null) return;

        Texture newTexture = new Texture(Gdx.files.internal(getImagePath()));
        if (graveTexture != null) {
            graveTexture.dispose();
        }
        graveTexture = newTexture;
        lastStage = stage;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        updateTexture();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.setColor(getColor().r, getColor().g, getColor().b, getColor().a * parentAlpha);

        batch.draw(
                graveTexture,
                getX(),
                getY(),
                getOriginX(),
                getOriginY(),
                getWidth(),
                getHeight(),
                getScaleX(),
                getScaleY(),
                getRotation(),
                0, 0,
                graveTexture.getWidth(),
                graveTexture.getHeight(),
                false, false
        );
    }

    public void dispose() {
        if (graveTexture != null) {
            graveTexture.dispose();
        }
    }
}