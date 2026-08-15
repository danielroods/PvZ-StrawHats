package view.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class GraveActor extends Actor {
    private static final String GRAVE_IMAGE_PATH = "images/chapters/egypt/gameplay/grave.png";

    private Texture graveTexture;

    public GraveActor(float x, float y, float width, float height) {
        this.graveTexture = new Texture(Gdx.files.internal(GRAVE_IMAGE_PATH));

        setBounds(x, y, width, height);
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