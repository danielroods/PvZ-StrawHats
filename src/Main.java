import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;

import controller.assets.GameAssetManager;
import controller.ScreenManager;
import model.collections.plant.PlantFactory;
import model.quests.QuestLoader;

public class Main extends ApplicationAdapter {

    @Override
    public void create() {
        try {
            PlantFactory.autoInit();
            QuestLoader.loadTemplates("src/resource/Quest.json");

            GameAssetManager.get().initialize();
            ScreenManager.syncWithCurrentMenu();

            setupCustomCursor();

        } catch (Exception e) {
            Gdx.app.error("Main", "Error during initialization", e);
        }
    }

    private void setupCustomCursor() {
        String cursorPath = "images/ui/Gemini_Generated_Image_oy54csoy54csoy54-removebg-preview.png"; // مسیر است کرسر را اینجا قرار دهید

        int desiredWidth = 64;
        int desiredHeight = 50;

        int xHotspot = 0;
        int yHotspot = 0;

        if (!Gdx.files.internal(cursorPath).exists()) {
            cursorPath = "assets/" + cursorPath;
        }

        if (Gdx.files.internal(cursorPath).exists()) {
            Pixmap originalPixmap = new Pixmap(Gdx.files.internal(cursorPath));

            int canvasSize = 64;
            if (desiredWidth > 64 || desiredHeight > 64) {
                canvasSize = 128;
            }

            Pixmap cursorCanvas = new Pixmap(canvasSize, canvasSize, Pixmap.Format.RGBA8888);
            cursorCanvas.setBlending(Pixmap.Blending.None);

            cursorCanvas.drawPixmap(originalPixmap,
                    0, 0, originalPixmap.getWidth(), originalPixmap.getHeight(),
                    0, 0, desiredWidth, desiredHeight);

            Cursor cursor = Gdx.graphics.newCursor(cursorCanvas, xHotspot, yHotspot);
            Gdx.graphics.setCursor(cursor);

            originalPixmap.dispose();
            cursorCanvas.dispose();
            Gdx.app.log("Main", "Custom cursor loaded successfully.");
        } else {
            Gdx.app.error("Main", "Cursor image file not found at: " + cursorPath);
        }
    }

    @Override
    public void render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F10)) {
            toggleFullscreen();
        }

        try {
            GameAssetManager.get().update();
        } catch (Exception e) {
        }

        try {
            net.client.NetworkClient.get().pump();
            ScreenManager.syncWithCurrentMenu();
        } catch (Exception e) {
            Gdx.app.error("Main", "Network pump failed", e);
        }

        ScreenManager.render(Gdx.graphics.getDeltaTime());
    }

    private void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(1152, 648);
        } else {
            Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(currentMode);
        }
    }

    @Override
    public void resize(int width, int height) {
        ScreenManager.resize(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        try {
            // Flush any pending online progress (and close the connection cleanly)
            // before the game shuts down, so closing the window can't drop it.
            net.client.NetworkClient.get().disconnect();
            ScreenManager.dispose();
            GameAssetManager.get().dispose();
            Gdx.app.log("Main", "Game disposed successfully.");
        } catch (Exception e) {
            Gdx.app.error("Main", "Error during dispose", e);
        }
    }
}