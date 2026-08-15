package service.card_factory;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

public class SeedPacketCard extends Stack {

    private static final float PLANT_SCALE_W = 0.83f;
    private static final float PLANT_SCALE_H = 0.83f;
    private static final float OFFSET_X = -40f;
    private static final float OFFSET_Y = 13f;

    private static final float CLIP_INSET = 1f;

    private final String name;
    private final String plantIconFile;
    private final String packetSkinFile;

    private final Container<Image> plantContainer;
    private final float baseWidth;
    private final float baseHeight;

    SeedPacketCard(String name, String plantIconFile, String packetSkinFile,
                   Texture packetTexture, Texture plantTexture,
                   float cardWidth, float cardHeight) {
        this.name = name;
        this.plantIconFile = plantIconFile;
        this.packetSkinFile = packetSkinFile;

        this.baseWidth = cardWidth;
        this.baseHeight = cardHeight;

        float aspect = (packetTexture != null && packetTexture.getWidth() > 0)
                ? (float) packetTexture.getHeight() / (float) packetTexture.getWidth()
                : (cardHeight / cardWidth);
        float actualHeight = cardWidth * aspect;

        setSize(cardWidth, actualHeight);

        Image packetImage = new Image(new TextureRegionDrawable(packetTexture));
        packetImage.setScaling(Scaling.stretch);
        add(packetImage);

        Image plantImage = new Image(new TextureRegionDrawable(plantTexture));
        plantImage.setScaling(Scaling.fit);
        plantImage.setAlign(Align.center);

        plantContainer = new Container<>(plantImage);
        plantContainer.align(Align.top);
        add(plantContainer);

        updateLayout(cardWidth, actualHeight);
    }

    @Override
    protected void sizeChanged() {
        super.sizeChanged();
        updateLayout(getWidth(), getHeight());
    }

    private void updateLayout(float w, float h) {
        if (plantContainer == null) return;

        plantContainer.size(w * PLANT_SCALE_W, h * PLANT_SCALE_H);

        float scaleX = w / baseWidth;
        float scaleY = h / baseHeight;

        plantContainer.pad(0);
        if (OFFSET_X < 0) {
            plantContainer.padRight(-OFFSET_X * scaleX);
        } else {
            plantContainer.padLeft(OFFSET_X * scaleX);
        }
        plantContainer.padTop(OFFSET_Y * scaleY);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        boolean transform = isTransform();

        if (transform) {
            applyTransform(batch, computeTransform());
        }

        float clipX = (transform ? 0 : getX()) + CLIP_INSET;
        float clipY = (transform ? 0 : getY()) + CLIP_INSET;
        float clipW = Math.max(0, getWidth() - (CLIP_INSET * 2f));
        float clipH = Math.max(0, getHeight() - (CLIP_INSET * 2f));

        if (clipBegin(clipX, clipY, clipW, clipH)) {
            drawChildren(batch, parentAlpha);
            batch.flush();
            clipEnd();
        } else {
            drawChildren(batch, parentAlpha);
        }

        if (transform) {
            resetTransform(batch);
        }
    }

    public String getName() { return name; }
    public String getPlantIconFile() { return plantIconFile; }
    public String getPacketSkinFile() { return packetSkinFile; }
}