package view.screens.ui_menus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import model.user_data.User;
import net.Protocol;
import net.client.NetworkClient;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.Toast;
import view.screens.generals.UiScreen;

public class NetworkScreen extends UiScreen {

    private static final float PANEL_WIDTH = 900f;

    private TextField hostField;
    private TextField portField;
    private TextField usernameField;
    private TextField passwordField;
    private TextField opponentField;
    private Label statusLabel;
    private Table playersTable;
    private long shownInviteId = -1;
    private float refreshTimer = 3f;

    @Override
    public void show() {
        setBackground("assets/images/backg/mainmenu_background.png");
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
        super.show();
        NetworkClient.get().setOnDisconnected(message -> {
            if (stage != null) Toast.show(stage, message);
            build();
        });
        build();
    }

    @Override
    public void hide() {
        NetworkClient.get().setOnDisconnected(null);
        super.hide();
    }

    private void build() {
        rootTable.clear();
        rootTable.top();
        rootTable.add(buildTopBar()).fillX().padTop(12).padLeft(20).padRight(20).row();

        NetworkClient client = NetworkClient.get();
        Table card = new Table();
        card.setBackground(skin.getDrawable("card-background"));
        card.pad(CARD_PAD * 2).defaults().pad(SPACE_XS);

        statusLabel = createLabel(statusText(client), "main");
        statusLabel.setWrap(true);
        card.add(statusLabel).width(PANEL_WIDTH - 80).colspan(2).padBottom(SPACE_MD).row();

        if (!client.isConnected()) {
            buildConnectSection(card);
        } else if (!client.isSignedIn()) {
            buildSignInSection(card);
        } else {
            buildLobbySection(card, client);
        }

        rootTable.add(scrollable(card)).width(PANEL_WIDTH).expand().fill().padTop(SPACE_MD).row();
    }

    private String statusText(NetworkClient client) {
        if (!client.isConnected()) {
            String message = client.getStatusMessage();
            return (message == null || message.isEmpty())
                    ? "You are playing offline. Connect to a server to play against someone."
                    : message;
        }
        if (!client.isSignedIn()) return "Connected. Sign in with your online account.";
        return "Signed in as " + client.getSignedInUsername()
                + (client.isQueued() ? "  -  waiting for an opponent..." : "");
    }

    private void buildConnectSection(Table card) {
        hostField = field(false);
        hostField.setText(NetworkClient.get().getHost());
        portField = field(false);
        portField.setText(String.valueOf(NetworkClient.get().getPort()));

        addRow(card, "Server", hostField);
        addRow(card, "Port", portField);
        card.add(primaryButton("Connect", this::connect))
                .colspan(2).width(BUTTON_WIDTH).padTop(SPACE_MD).row();
        card.add(createLabel("Offline play keeps working either way.", "muted"))
                .colspan(2).padTop(SPACE_SM).row();
    }

    private void buildSignInSection(Table card) {
        usernameField = field(false);
        passwordField = field(true);
        addRow(card, "Username", usernameField);
        addRow(card, "Password", passwordField);

        Table buttons = new Table();
        buttons.add(primaryButton("Sign in", this::signIn)).width(200).padRight(SPACE_MD);
        buttons.add(secondaryButton("Create account", this::registerOnline)).width(240);
        card.add(buttons).colspan(2).padTop(SPACE_MD).row();
        card.add(secondaryButton("Disconnect", this::disconnect))
                .colspan(2).width(BUTTON_WIDTH).padTop(SPACE_SM).row();
        card.add(createLabel("Creating an account online uses the details above plus your "
                        + "current offline profile's email and nickname.", "muted"))
                .colspan(2).width(PANEL_WIDTH - 120).padTop(SPACE_SM).row();
    }

    private void buildLobbySection(Table card, NetworkClient client) {
        opponentField = field(false);
        addRow(card, "Challenge", opponentField);
        card.add(primaryButton("Send invite", this::challenge))
                .colspan(2).width(BUTTON_WIDTH).padTop(SPACE_SM).row();

        Table queueButtons = new Table();
        if (client.isQueued()) {
            queueButtons.add(secondaryButton("Cancel search", () -> {
                client.leaveQueue();
                build();
            })).width(260);
        } else {
            queueButtons.add(primaryButton("Find random opponent", () ->
                    client.joinQueue(envelope -> build()))).width(300);
        }
        card.add(queueButtons).colspan(2).padTop(SPACE_MD).row();

        card.add(createLabel("Players online", "title")).colspan(2).padTop(SPACE_XL).row();
        playersTable = new Table();
        playersTable.top();
        card.add(playersTable).colspan(2).width(PANEL_WIDTH - 120).padTop(SPACE_SM).row();
        refreshPlayers();

        Table footer = new Table();
        footer.add(secondaryButton("Sign out", () -> {
            client.logout();
            build();
        })).width(200).padRight(SPACE_MD);
        footer.add(secondaryButton("Disconnect", this::disconnect)).width(200);
        card.add(footer).colspan(2).padTop(SPACE_XL).row();
    }

    private void refreshPlayers() {
        NetworkClient.get().refreshOnlinePlayers();
    }

    private void renderPlayers() {
        if (playersTable == null) return;
        playersTable.clear();
        var players = NetworkClient.get().getOnlinePlayers();
        if (players.isEmpty()) {
            playersTable.add(createLabel("Nobody else is online yet.", "muted")).left().row();
            return;
        }
        for (NetworkClient.OnlinePlayer player : players) {
            Table row = new Table();
            row.add(createLabel(player.nickname() + "  (" + player.username() + ")", "main"))
                    .left().expandX();
            if (player.inMatch()) {
                row.add(createLabel("in a match", "muted")).right();
            } else {
                row.add(secondaryButton("Challenge", () -> {
                    opponentField.setText(player.username());
                    challenge();
                })).width(180).right();
            }
            playersTable.add(row).width(PANEL_WIDTH - 140).padBottom(SPACE_SM).row();
        }
    }

    private void connect() {
        int port = Protocol.DEFAULT_PORT;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ignored) {
            Toast.show(stage, "That port is not a number, using " + port + ".");
        }
        String host = hostField.getText().trim();
        if (host.isEmpty()) host = Protocol.DEFAULT_HOST;
        NetworkClient.get().connect(host, port);
        Toast.show(stage, NetworkClient.get().getStatusMessage());
        build();
    }

    private void disconnect() {
        NetworkClient.get().disconnect();
        Toast.show(stage, "Disconnected. You are playing offline again.");
        build();
    }

    private void signIn() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            Toast.show(stage, "Type your username and password first.");
            return;
        }
        NetworkClient.get().login(username, password, envelope -> {
            if (envelope.isType(Protocol.OK)) {
                Toast.show(stage, "Signed in as " + username);
            } else {
                Toast.show(stage, envelope.getString("message", "Sign-in failed."));
            }
            build();
        });
    }

    private void registerOnline() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            Toast.show(stage, "Type the username and password you want to use.");
            return;
        }
        User local = User.currentUser;
        String nickname = local == null || local.nickname == null ? username : local.nickname;
        String email = local == null || local.email == null
                ? username + "@example.com" : local.email;
        String gender = local == null || local.gender == null ? "male" : local.gender;

        NetworkClient.get().register(username, password, nickname, email, gender,
                "1. What is the name of your first pet?", username,
                envelope -> {
                    if (envelope.isType(Protocol.OK)) {
                        Toast.show(stage, "Account created. Signing you in...");
                        signIn();
                    } else {
                        Toast.show(stage, envelope.getString("message", "Could not register."));
                    }
                });
    }

    private void challenge() {
        String target = opponentField.getText().trim();
        if (target.isEmpty()) {
            Toast.show(stage, "Type the username you want to challenge.");
            return;
        }
        NetworkClient.get().challenge(target, envelope -> Toast.show(stage,
                envelope.isType(Protocol.OK)
                        ? "Invite sent to " + target + "."
                        : envelope.getString("message", "Could not send the invite.")));
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        NetworkClient client = NetworkClient.get();
        NetworkClient.Invite invite = client.getPendingInvite();
        if (invite != null && invite.inviteId() != shownInviteId) {
            shownInviteId = invite.inviteId();
            showInviteModal(invite);
        }

        if (statusLabel != null) statusLabel.setText(statusText(client));

        refreshTimer += delta;
        if (refreshTimer >= 3f) {
            refreshTimer = 0f;
            if (client.isSignedIn()) {
                refreshPlayers();
                renderPlayers();
            }
        }
    }

    private void showInviteModal(NetworkClient.Invite invite) {
        new ConfirmModal(invite.fromNickname() + " wants to play!",
                "You will defend the lawn as the PLANTS. Accept?",
                "Accept",
                () -> NetworkClient.get().respondToInvite(invite.inviteId(), true, null),
                () -> NetworkClient.get().respondToInvite(invite.inviteId(), false, null)).show();
    }

    private Table buildTopBar() {
        ImageButton backBtn = new ImageButton(new TextureRegionDrawable(
                loadTextureSafe("assets/images/ui/buttons_hud_back_normal.png")));
        backBtn.getImageCell().size(54, 54);
        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                runCommand("menu exit");
            }
        });

        Label title = new Label("Online Match", skin, "title");
        title.setColor(Color.WHITE);

        Table left = new Table();
        left.add(backBtn).left();
        left.add(title).padLeft(SPACE_MD);

        Table bar = new Table();
        bar.add(left).left().expandX();
        bar.add(secondaryButton("Leaderboard",
                () -> runCommand("menu enter leaderboard"))).width(190).height(45).right();
        return bar;
    }

    @Override
    protected void onAfterCommand() {
        build();
    }
}
