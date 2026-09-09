package view.screens.generals;

import com.badlogic.gdx.Gdx;

import model.collections.animations.AnimationFactory;
import model.collections.animations.AnimationJsonParser;
import model.collections.animations.ZombieAnimationRegistry;
import model.utils.GameSettings;
import pvz.libpvz.pam.ClipRef;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import java.util.Map;


class PamRenderer {

    private final GameScreen screen;

    PamRenderer(GameScreen screen) {
        this.screen = screen;
    }

    void initPam() {
        try {
            com.badlogic.gdx.files.FileHandle root = Gdx.files.internal("assets/pvz-assets");
            screen.textureBank = new TextureBank("atlases", root);
            screen.pamPlayer = new PamPlayer(screen.textureBank, root);
        } catch (Throwable t) {
            screen.textureBank = null;
            screen.pamPlayer = null;
            Gdx.app.error("GameScreen", "PAM initialization failed; atlas fallback will be used.", t);
        }
    }

    boolean drawPamExact(String path, String exactState, float time, float x, float y, float scale, boolean flip) {
        return drawPamExact(path, exactState, time, x, y, scale, flip, null);
    }

    boolean drawPamExact(String path, String exactState, float time, float x, float y, float scale,
                         boolean flip, Map<String, Boolean> elementVisibility) {
        PamPlayer pamPlayer = screen.pamPlayer;
        if (pamPlayer == null || path == null || exactState == null) return false;
        try {
            String pamPath = path;
            if (pamPath.startsWith("assets/pvz-assets/")) {
                pamPath = pamPath.substring("assets/pvz-assets/".length());
            }
            
            
            
            
            
            
            String clipName = exactState;
            ClipRef clip = pamPlayer.getClip(pamPath, clipName);
            if (clip == null) {
                clipName = AnimationFactory.exactClipNameForPath(pamPath, exactState);
                if (clipName != null) clip = pamPlayer.getClip(pamPath, clipName);
            }
            if (clip == null) {
                if (GameSettings.get().isDebugMode()) {
                    Gdx.app.error("SQUASH_CLIP_MISSING",
                            "Exact PAM clip not found: path=" + pamPath + " clip=" + exactState);
                }
                return false;
            }

            screen.batch.flush();
            com.badlogic.gdx.math.Matrix4 old = screen.batch.getTransformMatrix().cpy();
            screen.batch.getTransformMatrix().translate(x, y, 0f).scale(scale, scale, 1f);
            screen.batch.setTransformMatrix(screen.batch.getTransformMatrix());
            Map<String, Boolean> resolvedVisibility =
                    PlantCostumeMask.expandHierarchy(pamPlayer, pamPath, elementVisibility);
            if (resolvedVisibility != null) {
                pamPlayer.draw(screen.batch, clip, time, 0f, 0f, flip, resolvedVisibility);
            } else {
                pamPlayer.draw(screen.batch, clip, time, 0f, 0f, flip);
            }
            screen.batch.flush();
            screen.batch.setTransformMatrix(old);
            return true;
        } catch (Throwable t) {
            if (GameSettings.get().isDebugMode()) {
                Gdx.app.error("SQUASH_EXACT_DRAW_FAIL",
                        "Exact PAM draw failed for path=" + path + " clip=" + exactState, t);
            }
            return false;
        }
    }

    boolean drawPam(String path, String preferred, float time, float x, float y, float scale, boolean flip,
                    Map<String, Boolean> elementVisibility) {
        PamPlayer pamPlayer = screen.pamPlayer;
        if (pamPlayer == null || path == null) return false;
        try {
            String pamPath = path;
            if (pamPath.startsWith("assets/pvz-assets/")) {
                pamPath = pamPath.substring("assets/pvz-assets/".length());
            }
            String clipName = AnimationFactory.resolveClipNameForPath(pamPath, preferred);
            if (clipName == null) clipName = preferred;
            if (clipName == null || clipName.isBlank()) return false;
            ClipRef clip = pamPlayer.getClip(pamPath, clipName);
            if (clip == null) {
                if (GameSettings.get().isDebugMode()) {
                    Gdx.app.log("DRAWPAM_NULLCLIP", "getClip returned null for path=" + path + " clip=" + clipName);
                }
                return false;
            }

            screen.batch.flush();
            com.badlogic.gdx.math.Matrix4 old = screen.batch.getTransformMatrix().cpy();

            screen.batch.getTransformMatrix().translate(x, y, 0f).scale(scale, scale, 1f);
            screen.batch.setTransformMatrix(screen.batch.getTransformMatrix());

            Map<String, Boolean> resolvedVisibility =
                    PlantCostumeMask.expandHierarchy(pamPlayer, pamPath, elementVisibility);
            if (resolvedVisibility != null) {
                pamPlayer.draw(screen.batch, clip, time, 0f, 0f, flip, resolvedVisibility);
            } else {
                pamPlayer.draw(screen.batch, clip, time, 0f, 0f, flip);
            }

            screen.batch.flush();
            screen.batch.setTransformMatrix(old);
            return true;
        } catch (Throwable t) {
            if (GameSettings.get().isDebugMode()) {
                Gdx.app.error("DRAWPAM_FAIL", "drawPam threw for path=" + path + " preferred=" + preferred, t);
            }
            return false;
        }
    }

    
    boolean drawPamStretched(String path, String preferred, float time, float x, float y,
                             float scaleX, float scaleY, boolean flip) {
        PamPlayer pamPlayer = screen.pamPlayer;
        if (pamPlayer == null || path == null) return false;
        try {
            String pamPath = path;
            if (pamPath.startsWith("assets/pvz-assets/")) {
                pamPath = pamPath.substring("assets/pvz-assets/".length());
            }
            String clipName = AnimationFactory.resolveClipNameForPath(pamPath, preferred);
            if (clipName == null) clipName = preferred;
            if (clipName == null || clipName.isBlank()) return false;
            ClipRef clip = pamPlayer.getClip(pamPath, clipName);
            if (clip == null) return false;

            screen.batch.flush();
            com.badlogic.gdx.math.Matrix4 old = screen.batch.getTransformMatrix().cpy();

            screen.batch.getTransformMatrix().translate(x, y, 0f).scale(scaleX, scaleY, 1f);
            screen.batch.setTransformMatrix(screen.batch.getTransformMatrix());
            pamPlayer.draw(screen.batch, clip, time, 0f, 0f, flip);

            screen.batch.flush();
            screen.batch.setTransformMatrix(old);
            return true;
        } catch (Throwable t) {
            if (GameSettings.get().isDebugMode()) {
                Gdx.app.error("DRAWPAM_STRETCH_FAIL",
                        "drawPamStretched threw for path=" + path + " preferred=" + preferred, t);
            }
            return false;
        }
    }

    boolean drawPamMirrored(String path, String preferred, float time, float x, float y, float scale) {
        PamPlayer pamPlayer = screen.pamPlayer;
        if (pamPlayer == null || path == null) return false;
        try {
            String pamPath = path;
            if (pamPath.startsWith("assets/pvz-assets/")) {
                pamPath = pamPath.substring("assets/pvz-assets/".length());
            }
            String clipName = AnimationFactory.resolveClipNameForPath(pamPath, preferred);
            if (clipName == null) clipName = preferred;
            if (clipName == null || clipName.isBlank()) return false;

            ClipRef clip = pamPlayer.getClip(pamPath, clipName);
            if (clip == null) return false;

            screen.batch.flush();
            com.badlogic.gdx.math.Matrix4 old = screen.batch.getTransformMatrix().cpy();

            screen.batch.getTransformMatrix().translate(x, y, 0f).scale(-scale, scale, 1f);
            screen.batch.setTransformMatrix(screen.batch.getTransformMatrix());
            pamPlayer.draw(screen.batch, clip, time, 0f, 0f, false);

            screen.batch.flush();
            screen.batch.setTransformMatrix(old);
            return true;
        } catch (Throwable t) {
            if (GameSettings.get().isDebugMode()) {
                Gdx.app.error("DRAWPAM_MIRROR_FAIL",
                        "drawPamMirrored threw for path=" + path + " preferred=" + preferred, t);
            }
            return false;
        }
    }

    
    boolean drawPamRotated(String path, String preferred, float time, float x, float y, float scale,
                           float rotationDegrees) {
        PamPlayer pamPlayer = screen.pamPlayer;
        if (pamPlayer == null || path == null) return false;
        try {
            String pamPath = path;
            if (pamPath.startsWith("assets/pvz-assets/")) {
                pamPath = pamPath.substring("assets/pvz-assets/".length());
            }
            String clipName = AnimationFactory.resolveClipNameForPath(pamPath, preferred);
            if (clipName == null) clipName = preferred;
            if (clipName == null || clipName.isBlank()) return false;

            ClipRef clip = pamPlayer.getClip(pamPath, clipName);
            if (clip == null) return false;

            screen.batch.flush();
            com.badlogic.gdx.math.Matrix4 old = screen.batch.getTransformMatrix().cpy();

            screen.batch.getTransformMatrix()
                    .translate(x, y, 0f)
                    .rotate(0f, 0f, 1f, rotationDegrees)
                    .scale(scale, scale, 1f);
            screen.batch.setTransformMatrix(screen.batch.getTransformMatrix());
            pamPlayer.draw(screen.batch, clip, time, 0f, 0f, false);

            screen.batch.flush();
            screen.batch.setTransformMatrix(old);
            return true;
        } catch (Throwable t) {
            if (GameSettings.get().isDebugMode()) {
                Gdx.app.error("DRAWPAM_ROTATE_FAIL",
                        "drawPamRotated threw for path=" + path + " preferred=" + preferred, t);
            }
            return false;
        }
    }

    String resolveFuseClipState(String displayName) {
        String state = AnimationFactory.firstAvailableClipState(displayName, "explode", "attack");
        return state == null ? "attack" : state;
    }

    
    float resolveClipDuration(String alias, String preferredState) {
        AnimationJsonParser.AnimationConfig config = ZombieAnimationRegistry.resolve(alias);
        if (config == null || config.clips == null) return -1f;
        String clipName = AnimationFactory.resolveClipName(config, preferredState);
        if (clipName == null) return -1f;
        Double duration = config.clips.get(clipName);
        return (duration != null && duration > 0.0) ? duration.floatValue() : -1f;
    }

    
    float resolvePlantClipDuration(String displayName, String preferredState) {
        AnimationJsonParser.AnimationConfig config = AnimationFactory.resolveByDisplayName(displayName);
        if (config == null || config.clips == null) return -1f;
        String clipName = AnimationFactory.resolveClipName(config, preferredState);
        if (clipName == null) return -1f;
        Double duration = config.clips.get(clipName);
        return (duration != null && duration > 0.0) ? duration.floatValue() : -1f;
    }
}