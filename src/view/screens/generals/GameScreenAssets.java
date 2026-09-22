package view.screens.generals;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import controller.assets.AssetPaths;

import java.util.HashMap;
import java.util.Map;

class GameScreenAssets {

    private static final String SLIDER_TILE_UP_IMAGE_PATH = AssetPaths.FROSTBITE_SLIDER_TILE_UP;
    private static final String SLIDER_TILE_DOWN_IMAGE_PATH = AssetPaths.FROSTBITE_SLIDER_TILE_DOWN;
    private static final String SLIDER_TILE_BACKGROUND_UP_IMAGE_PATH = AssetPaths.FROSTBITE_SLIDER_TILE_BACKGROUND_UP;
    private static final String SLIDER_TILE_BACKGROUND_DOWN_IMAGE_PATH = AssetPaths.FROSTBITE_SLIDER_TILE_BACKGROUND_DOWN;
    private static final String PLANT_ICE_BLOCK_IMAGE_PATH_1 = AssetPaths.FROSTBITE_PLANT_ICE_BLOCK_1;
    private static final String PLANT_ICE_BLOCK_IMAGE_PATH_2 = AssetPaths.FROSTBITE_PLANT_ICE_BLOCK_2;
    private static final String PLANT_ICE_BLOCK_IMAGE_PATH_3 = AssetPaths.FROSTBITE_PLANT_ICE_BLOCK_3;
    private static final String ZOMBIE_ICE_BLOCK_IMAGE_PATH = AssetPaths.FROSTBITE_ZOMBIE_ICE_BLOCK;

    private static final String SHOVEL_ICON_PATH = "assets/images/chapters/egypt/gameplay/shovel_icon.png";

    private final GameScreen screen;

    private final Map<String, Texture> graveTextureCache = new HashMap<>();

    private Texture sliderUpTexture;
    private Texture sliderDownTexture;
    private Texture sliderBackgroundTexture;
    private Texture sliderUpBackgroundTexture;
    private Texture sliderDownBackgroundTexture;
    private Texture plantIceBlockTexture1;
    private Texture plantIceBlockTexture2;
    private Texture plantIceBlockTexture3;
    private Texture zombieIceBlockTexture;

    private Texture shovelIconTexture;
    private Texture potTexture;

    private final Map<String, Texture> staticEffectTextures = new HashMap<>();

    GameScreenAssets(GameScreen screen) {
        this.screen = screen;
    }

    
    TextureRegion graveRegionForPath(String path) {
        if (path == null || path.isBlank()) return null;
        Texture texture = graveTextureCache.get(path);
        if (texture == null) {
            String resolved = resolveExistingAssetPath(path);
            if (resolved == null || resolved.isBlank() || !Gdx.files.internal(resolved).exists()) return null;
            texture = new Texture(Gdx.files.internal(resolved));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            graveTextureCache.put(path, texture);
        }
        return new TextureRegion(texture);
    }

    Texture shovelIconTexture() {
        return shovelIconTexture;
    }

    Texture sliderUpTexture() {
        return sliderUpTexture;
    }

    Texture sliderDownTexture() {
        return sliderDownTexture;
    }

    Texture sliderBackgroundTexture() {
        return sliderBackgroundTexture;
    }

    Texture sliderUpBackgroundTexture() {
        return sliderUpBackgroundTexture;
    }

    Texture sliderDownBackgroundTexture() {
        return sliderDownBackgroundTexture;
    }

    Texture plantIceBlockTexture1() {
        return plantIceBlockTexture1;
    }

    Texture plantIceBlockTexture2() {
        return plantIceBlockTexture2;
    }

    Texture plantIceBlockTexture3() {
        return plantIceBlockTexture3;
    }

    Texture zombieIceBlockTexture() {
        return zombieIceBlockTexture;
    }

    void initShovelTexture() {
        String path = resolveExistingAssetPath(SHOVEL_ICON_PATH);
        if (path != null && Gdx.files.internal(path).exists()) {
            shovelIconTexture = new Texture(Gdx.files.internal(path));
            shovelIconTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    void initBoardTexture() {
        screen.whitePixel = GameScreenGraphics.makeWhitePixel();
        screen.bubbleTexture = GameScreenGraphics.makeBubbleTexture();
        screen.boardTexture = loadBoardTexture(screen.getGameplayBackgroundLayers());
        if (screen.boardTexture != null) {
            screen.boardTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
    }

    private Texture loadBoardTexture(String[] layers) {
        if (layers == null || layers.length == 0) return null;

        java.util.List<FileHandle> existing = new java.util.ArrayList<>();
        for (String layer : layers) {
            FileHandle file = Gdx.files.internal(resolveExistingAssetPath(layer));
            if (file.exists()) {
                existing.add(file);
            } else {
                Gdx.app.error("GameScreen", "Missing chapter background layer: " + layer);
            }
        }
        if (existing.isEmpty()) return null;
        FileHandle[] files = existing.toArray(new FileHandle[0]);
        if (files.length == 1) return new Texture(files[0]);

        Pixmap[] parts = new Pixmap[files.length];
        Pixmap stitched = null;
        try {
            int width = 0;
            int height = 0;
            for (int i = 0; i < files.length; i++) {
                parts[i] = new Pixmap(files[i]);
                width += parts[i].getWidth();
                height = Math.max(height, parts[i].getHeight());
            }
            stitched = new Pixmap(width, height, Pixmap.Format.RGBA8888);
            stitched.setBlending(Pixmap.Blending.None);
            int x = 0;
            for (Pixmap part : parts) {
                stitched.drawPixmap(part, x, 0);
                x += part.getWidth();
            }
            return new Texture(stitched);
        } catch (Throwable t) {
            Gdx.app.error("GameScreen", "Could not stitch the chapter background", t);
            return null;
        } finally {
            for (Pixmap part : parts) {
                if (part != null) part.dispose();
            }
            if (stitched != null) stitched.dispose();
        }
    }

    String resolveExistingAssetPath(String path) {
        if (path == null) return "";
        if (Gdx.files.internal(path).exists()) return path;
        if (path.startsWith("assets/")) {
            String noPrefix = path.substring("assets/".length());
            if (Gdx.files.internal(noPrefix).exists()) return noPrefix;
        } else {
            String withPrefix = "assets/" + path;
            if (Gdx.files.internal(withPrefix).exists()) return withPrefix;
        }
        return path;
    }

    void initFrostbiteTextures() {
        sliderUpTexture = loadOptionalTexture(SLIDER_TILE_UP_IMAGE_PATH);
        sliderDownTexture = loadOptionalTexture(SLIDER_TILE_DOWN_IMAGE_PATH);
        sliderUpBackgroundTexture = loadOptionalTexture(SLIDER_TILE_BACKGROUND_UP_IMAGE_PATH);
        sliderDownBackgroundTexture = loadOptionalTexture(SLIDER_TILE_BACKGROUND_DOWN_IMAGE_PATH);
        plantIceBlockTexture1 = loadOptionalTexture(PLANT_ICE_BLOCK_IMAGE_PATH_1);
        plantIceBlockTexture2 = loadOptionalTexture(PLANT_ICE_BLOCK_IMAGE_PATH_2);
        plantIceBlockTexture3 = loadOptionalTexture(PLANT_ICE_BLOCK_IMAGE_PATH_3);
        zombieIceBlockTexture = loadOptionalTexture(ZOMBIE_ICE_BLOCK_IMAGE_PATH);
        logIceBlockTextureDiagnostics();
    }

    private void logIceBlockTextureDiagnostics() {
        logTextureLoadResult("plant ice block state 1", PLANT_ICE_BLOCK_IMAGE_PATH_1, plantIceBlockTexture1);
        logTextureLoadResult("plant ice block state 2", PLANT_ICE_BLOCK_IMAGE_PATH_2, plantIceBlockTexture2);
        logTextureLoadResult("plant ice block state 3", PLANT_ICE_BLOCK_IMAGE_PATH_3, plantIceBlockTexture3);
        logTextureLoadResult("zombie ice block", ZOMBIE_ICE_BLOCK_IMAGE_PATH, zombieIceBlockTexture);
    }

    private void logTextureLoadResult(String label, String requestedPath, Texture loaded) {
        if (loaded != null) {
            Gdx.app.log("ICE_BLOCK_DIAG", label + " loaded OK (" + loaded.getWidth() + "x" + loaded.getHeight() + ")");
            return;
        }
        String resolved = resolveExistingAssetPath(requestedPath);
        String absolute = Gdx.files.internal(resolved).file().getAbsolutePath();
        Gdx.app.error("ICE_BLOCK_DIAG", label + " FAILED to load. requested='" + requestedPath
                + "' resolved='" + resolved + "' checked absolute path='" + absolute
                + "' exists=" + Gdx.files.internal(resolved).exists());
    }

    Texture loadOptionalTexture(String path) {
        String resolved = resolveExistingAssetPath(path);
        if (resolved == null || resolved.isBlank() || !Gdx.files.internal(resolved).exists()) return null;
        Texture texture = new Texture(Gdx.files.internal(resolved));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    Texture getPotTexture() {
        if (potTexture == null) {
            String path = resolveExistingAssetPath("assets/images/ui/Stack_1.png");
            if (path != null && Gdx.files.internal(path).exists()) {
                potTexture = new Texture(Gdx.files.internal(path));
                potTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }
        }
        return potTexture;
    }

    private Texture staticEffectTexture(String path) {
        if (path == null) return null;
        if (staticEffectTextures.containsKey(path)) return staticEffectTextures.get(path);
        Texture texture = loadOptionalTexture(path);
        staticEffectTextures.put(path, texture);
        return texture;
    }

    boolean drawStaticEffect(String path, float x, float y, float scale) {
        return drawStaticEffect(path, x, y, scale, false);
    }

    
    boolean drawStaticEffect(String path, float x, float y, float scale, boolean flip) {
        Texture texture = staticEffectTexture(path);
        if (texture == null) return false;
        float width = texture.getWidth() * scale;
        float height = texture.getHeight() * scale;
        screen.batch.draw(texture, x - width * 0.5f, y - height * 0.5f, width * 0.5f, height * 0.5f,
                width, height, 1f, 1f, 0f,
                0, 0, texture.getWidth(), texture.getHeight(), flip, false);
        return true;
    }

    
    boolean drawStaticEffectStretched(String path, float x, float y, float width, float height) {
        Texture texture = staticEffectTexture(path);
        if (texture == null) return false;
        screen.batch.draw(texture, x, y, width, height);
        return true;
    }

    void dispose() {
        for (Texture texture : graveTextureCache.values()) {
            if (texture != null) texture.dispose();
        }
        graveTextureCache.clear();
        if (shovelIconTexture != null) shovelIconTexture.dispose();
        if (potTexture != null) potTexture.dispose();
        if (sliderUpTexture != null) sliderUpTexture.dispose();
        if (sliderDownTexture != null) sliderDownTexture.dispose();
        if (sliderBackgroundTexture != null) sliderBackgroundTexture.dispose();
        if (sliderUpBackgroundTexture != null) sliderUpBackgroundTexture.dispose();
        if (sliderDownBackgroundTexture != null) sliderDownBackgroundTexture.dispose();
        if (plantIceBlockTexture1 != null) plantIceBlockTexture1.dispose();
        if (plantIceBlockTexture2 != null) plantIceBlockTexture2.dispose();
        if (plantIceBlockTexture3 != null) plantIceBlockTexture3.dispose();
        if (zombieIceBlockTexture != null) zombieIceBlockTexture.dispose();
        for (Texture texture : staticEffectTextures.values()) {
            if (texture != null) texture.dispose();
        }
        staticEffectTextures.clear();
    }
}