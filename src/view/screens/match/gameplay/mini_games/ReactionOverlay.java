package view.screens.match.gameplay.mini_games;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;

import net.Protocol;
import net.client.NetMatchState;

import java.util.function.Function;

public class ReactionOverlay extends Actor {

    public static final String[] EMOJI_LABELS = {":D", ">:(", ":'("};
    public static final String[] STICKER_LABELS = {"*1*", "*2*", "*3*"};

    private static final String EMOJI_PATH = "assets/images/ui/reactions/emoji_%d.png";
    private static final String STICKER_PATH = "assets/images/ui/reactions/sticker_%d.png";
    private static final int STICKER_FRAMES = 4;
    private static final float STICKER_FPS = 8f;
    private static final float LIFETIME = 2.5f;
    private static final float PANEL_WIDTH = 300f;
    private static final float PANEL_HEIGHT = 96f;

    private final Skin skin;
    private final Function<String, Texture> textureLoader;
    private final Texture[] emojiTextures = new Texture[3];
    private final Texture[] stickerTextures = new Texture[3];

    private NetMatchState.Reaction reaction;
    private String opponentName = "";
    private float age;

    public ReactionOverlay(Skin skin, Function<String, Texture> textureLoader) {
        this.skin = skin;
        this.textureLoader = textureLoader;
        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
    }

    public void update(NetMatchState.Reaction incoming, String opponentNickname) {
        this.opponentName = opponentNickname == null ? "" : opponentNickname;
        if (incoming == null) {
            reaction = null;
            return;
        }
        if (reaction == null || reaction.kind() != incoming.kind()
                || reaction.index() != incoming.index()
                || !incoming.fromUsername().equals(reaction.fromUsername())) {
            reaction = incoming;
            age = 0f;
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (reaction != null) {
            age += delta;
            if (age > LIFETIME) reaction = null;
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (reaction == null || getStage() == null) return;

        float alpha = age < 0.25f ? age / 0.25f
                : (age > LIFETIME - 0.4f ? Math.max(0f, (LIFETIME - age) / 0.4f) : 1f);
        float slide = age < 0.25f ? (1f - age / 0.25f) * 40f : 0f;

        float x = getStage().getWidth() - PANEL_WIDTH - 16f + slide;
        float y = getStage().getHeight() - PANEL_HEIGHT - 170f;

        Color previous = batch.getColor().cpy();
        batch.setColor(1f, 1f, 1f, alpha);
        skin.getDrawable("card-background").draw(batch, x, y, PANEL_WIDTH, PANEL_HEIGHT);

        var font = skin.getFont("default-font");
        float oldScale = font.getData().scaleX;
        font.getData().setScale(0.62f);
        font.setColor(1f, 0.9f, 0.5f, alpha);
        font.draw(batch, opponentName + " says:", x + 14f, y + PANEL_HEIGHT - 12f);

        if (Protocol.REACTION_TEXT.equals(reaction.kind())) {
            font.getData().setScale(0.78f);
            font.setColor(1f, 1f, 1f, alpha);
            font.draw(batch, safeText(reaction.index()), x + 14f, y + PANEL_HEIGHT - 42f,
                    PANEL_WIDTH - 28f, Align.left, true);
        } else {
            Texture texture = artFor(reaction.kind(), reaction.index());
            float size = PANEL_HEIGHT - 46f;
            if (texture != null) {
                batch.setColor(1f, 1f, 1f, alpha);
                drawArt(batch, texture, reaction.kind(), x + 16f, y + 10f, size);
            } else {
                font.getData().setScale(1.1f);
                font.setColor(1f, 1f, 1f, alpha);
                font.draw(batch, fallbackLabel(reaction.kind(), reaction.index()),
                        x + 18f, y + PANEL_HEIGHT - 44f);
            }
        }

        font.getData().setScale(oldScale);
        font.setColor(Color.WHITE);
        batch.setColor(previous);
    }

    private void drawArt(Batch batch, Texture texture, String kind, float x, float y, float size) {
        if (!Protocol.REACTION_STICKER.equals(kind)) {
            batch.draw(texture, x, y, size, size);
            return;
        }
        int frameWidth = Math.max(1, texture.getWidth() / STICKER_FRAMES);
        int frame = ((int) (age * STICKER_FPS)) % STICKER_FRAMES;
        TextureRegion region = new TextureRegion(texture,
                frame * frameWidth, 0, frameWidth, texture.getHeight());
        batch.draw(region, x, y, size, size);
    }

    private String safeText(int index) {
        if (index < 0 || index >= Protocol.REACTION_TEXTS.length) return "...";
        return Protocol.REACTION_TEXTS[index];
    }

    private String fallbackLabel(String kind, int index) {
        String[] labels = Protocol.REACTION_STICKER.equals(kind) ? STICKER_LABELS : EMOJI_LABELS;
        return index < 0 || index >= labels.length ? "?" : labels[index];
    }

    private Texture artFor(String kind, int index) {
        if (index < 0 || index > 2) return null;
        boolean sticker = Protocol.REACTION_STICKER.equals(kind);
        Texture[] cache = sticker ? stickerTextures : emojiTextures;
        if (cache[index] == null) {
            String path = String.format(sticker ? STICKER_PATH : EMOJI_PATH, index);
            Texture loaded = textureLoader.apply(path);
            if (loaded != null && loaded.getWidth() > 1) cache[index] = loaded;
        }
        return cache[index];
    }
}
