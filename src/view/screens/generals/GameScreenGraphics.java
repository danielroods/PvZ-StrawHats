package view.screens.generals;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

final class GameScreenGraphics {

    private GameScreenGraphics() {
    }

    static Color itemColor(String type) {
        if (type == null) return Color.LIGHT_GRAY;
        return switch (type) {
            case "SUN" -> new Color(1f, 0.84f, 0.1f, 1f);
            case "PLANT_FOOD" -> new Color(0.2f, 0.9f, 0.35f, 1f);
            case "COIN" -> new Color(0.95f, 0.7f, 0.15f, 1f);
            case "DIAMOND" -> new Color(0.25f, 0.8f, 1f, 1f);
            default -> Color.LIGHT_GRAY;
        };
    }

    static String initials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] words = name.split("\\s+");
        StringBuilder b = new StringBuilder();
        for (String w : words) if (!w.isEmpty()) b.append(Character.toUpperCase(w.charAt(0)));
        return b.substring(0, Math.min(2, b.length()));
    }

    static TextureRegion makeWhitePixel() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return new TextureRegion(t);
    }

    /**
     * A small soft-edged circle (white, alpha-blended) used as a generic round particle -
     * currently the rising bubbles drawn over a hypnotized zombie. Tinted via
     * {@code batch.setColor(...)} at draw time, same idiom as {@link #makeWhitePixel()}.
     */
    static TextureRegion makeBubbleTexture() {
        int size = 32;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        float cx = size / 2f;
        float cy = size / 2f;
        float outerR = size / 2f - 1f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x + 0.5f - cx;
                float dy = y + 0.5f - cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > outerR) continue;
                
                
                
                float edgeFade = Math.max(0f, 1f - dist / outerR);
                float alpha = 0.35f + 0.5f * edgeFade;
                boolean highlight = dx < -outerR * 0.15f && dy < -outerR * 0.15f
                        && dist < outerR * 0.55f;
                float brightness = highlight ? 1f : 0.85f;
                p.setColor(brightness, brightness, brightness, Math.min(1f, alpha));
                p.drawPixel(x, y);
            }
        }
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return new TextureRegion(t);
    }

    
    static final class BitmapFontAccess {
        static void draw(Batch batch, Skin skin, String text, float x, float y) {
            if (skin != null && skin.has("default-font", com.badlogic.gdx.graphics.g2d.BitmapFont.class)) {
                skin.getFont("default-font").draw(batch, text, x, y);
            }
        }
    }
}