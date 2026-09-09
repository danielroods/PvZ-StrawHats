package view.screens.ui_menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import controller.ScreenManager;
import net.Protocol;
import net.client.NetworkClient;
import view.screens.generals.BaseScreen;
import view.screens.generals.Modal;
import view.screens.generals.Toast;

/**
 * Popup that asks the player to connect to the game server. Opened by tapping the
 * island icon on the online-match hub screen. Reuses the same wood-board look as the
 * other net screens, on top of the shared {@link Modal} scrim/panel plumbing.
 */
class ConnectServerModal extends Modal {

    private static final String WOOD_BACKGROUND_PATH = "assets/images/backg/wood board.png";
    private static final String WIFI_ICON_PATH = "assets/images/ui/net/wifi_icon.png";
    private static final float FIELD_WIDTH = 220f;
    private static final float LABEL_WIDTH = 90f;

    private final TextField hostField;
    private final TextField portField;

    ConnectServerModal(Runnable onConnected) {
        content.setBackground(createWoodBackground());
        content.pad(30);

        Image wifi = new Image(loadTexture(WIFI_ICON_PATH));
        content.add(wifi).size(48, 48).padBottom(6).row();

        content.add(new Label("Connect to Server", skin, "title")).colspan(2).padBottom(12).row();

        NetworkClient client = NetworkClient.get();
        hostField = new TextField(client.getHost(), skin, "main");
        portField = new TextField(String.valueOf(client.getPort()), skin, "main");

        content.add(new Label("Server", skin, "main")).width(LABEL_WIDTH).left();
        content.add(hostField).width(FIELD_WIDTH).left().row();
        content.add(new Label("Port", skin, "main")).width(LABEL_WIDTH).left();
        content.add(portField).width(FIELD_WIDTH).left().row();

        TextButton cancel = new TextButton("Cancel", skin, "secondary");
        cancel.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                hide();
            }
        });

        TextButton connect = new TextButton("Connect", skin, "main");
        connect.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                doConnect(onConnected);
            }
        });

        content.add(cancel).width(150).padTop(16);
        content.add(connect).width(150).padTop(16).row();

        content.add(new Label("Offline play keeps working either way.", skin, "muted"))
                .colspan(2).padTop(10);
    }

    private void doConnect(Runnable onConnected) {
        NetworkClient client = NetworkClient.get();
        int port = Protocol.DEFAULT_PORT;
        BaseScreen screen = ScreenManager.getScreen();
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ignored) {
            if (screen != null) Toast.show(screen.stage, "That port is not a number, using " + port + ".");
        }
        String host = hostField.getText().trim();
        if (host.isEmpty()) host = Protocol.DEFAULT_HOST;

        client.connect(host, port);
        if (screen != null) Toast.show(screen.stage, client.getStatusMessage());
        hide();
        if (onConnected != null) onConnected.run();
    }

    private TextureRegionDrawable createWoodBackground() {
        return new TextureRegionDrawable(loadTexture(WOOD_BACKGROUND_PATH));
    }

    
    private Texture loadTexture(String path) {
        if (path != null && !path.isEmpty() && Gdx.files.internal(path).exists()) {
            Texture texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return texture;
        }
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        Texture fallback = new Texture(pixmap);
        pixmap.dispose();
        return fallback;
    }
}