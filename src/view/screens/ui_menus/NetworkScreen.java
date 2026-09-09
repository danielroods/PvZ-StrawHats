package view.screens.ui_menus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
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
    private static final float PLAYERS_PANEL_WIDTH = 340f;

    private static final String WOOD_BACKGROUND_PATH = "assets/images/backg/wood board.png";
    private static final String LEADERBOARD_ICON_PATH = "assets/images/ui/leaderboard.png";
    private static final String BACK_ICON_PATH = "assets/images/ui/buttons_hud_back_normal.png";
    private static final String ISLAND_ICON_PATH = "assets/images/ui/net/joust_icicle.png";
    private static final String GOOGLE_ACCOUNT_ICON_PATH = "assets/images/ui/net/LinkAccountGooglePlay.png";
    private static final String WIFI_ICON_PATH = "assets/images/ui/net/wifi_icon.png";
    private static final String UNKNOWN_AVATAR_PATH = "assets/images/ui/net/avatar_practice.png";
    private static final String AVATAR_FRAME_PATH = "assets/images/ui/reward4_bg.png";
    private static final String PLAYER_ICON = "assets/images/ui/net/MM_playerIcon.png";

    private TextField opponentField;
    private Label statusLabel;
    private Table playersTable;
    private long shownInviteId = -1;
    private boolean shownConnected = false;
    private boolean shownSignedIn = false;
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
        shownConnected = client.isConnected();
        shownSignedIn = client.isSignedIn();
        if (!client.isConnected()) {
            rootTable.add(buildHubSection(client)).expand().fill().padTop(SPACE_MD).row();
        } else if (!client.isSignedIn()) {
            rootTable.add(buildSigningInSection(client)).expand().fill().padTop(SPACE_MD).row();
        } else {
            rootTable.add(buildLobbySection(client)).expand().fill().padTop(SPACE_MD).row();
        }
    }

    private String statusText(NetworkClient client) {
        if (!client.isConnected()) {
            String message = client.getStatusMessage();
            return (message == null || message.isEmpty())
                    ? "You are playing offline. Connect to a server to play against someone."
                    : message;
        }
        if (!client.isSignedIn()) {
            String message = client.getStatusMessage();
            return (message == null || message.isEmpty())
                    ? "Connected. Taking your account online..." : message;
        }
        return "Signed in as " + client.getSignedInUsername()
                + (client.isQueued() ? "  -  waiting for an opponent..." : "");
    }

    
    
    
    
    private Table buildHubSection(NetworkClient client) {
        Table board = new Table();
        board.setBackground(woodDrawable());
        board.pad(CARD_PAD * 3);

        statusLabel = createLabel(statusText(client), "main");
        statusLabel.setWrap(true);
        statusLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        board.add(statusLabel).width(PANEL_WIDTH - 160).padBottom(SPACE_XL).row();

        Table icons = new Table();
        icons.add(hubIcon(ISLAND_ICON_PATH, "Play Online", 140, 140,
                () -> new ConnectServerModal(this::build).show())).padRight(SPACE_XL * 2);
        icons.add(hubIcon(LEADERBOARD_ICON_PATH, "Leaderboard", 96, 96,
                () -> runCommand("menu enter leaderboard")));
        board.add(icons).row();

        Table wrap = new Table();
        wrap.add(board).width(PANEL_WIDTH);
        return wrap;
    }

    private Actor hubIcon(String iconPath, String label, float width, float height, Runnable action) {
        Table container = new Table();
        ImageButton button = new ImageButton(new TextureRegionDrawable(loadTextureSafe(iconPath)));
        button.getImageCell().size(width, height);
        button.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                AudioManager.get().playSound(AudioEnum.SFX_CLICK, 0.5f);
                action.run();
            }
        });
        container.add(button).row();
        container.add(createLabel(label, "title")).padTop(SPACE_SM);
        return container;
    }

    private Table buildSigningInSection(NetworkClient client) {
        Table board = new Table();
        board.setBackground(woodDrawable());
        board.pad(CARD_PAD * 3).defaults().pad(SPACE_XS);

        Image accountIcon = new Image(loadTextureSafe(GOOGLE_ACCOUNT_ICON_PATH));
        board.add(accountIcon).size(72, 72).padBottom(SPACE_MD).row();

        User local = User.currentUser;
        board.add(createLabel(local == null
                ? "Log into your account first, then come back online."
                : "Playing as " + local.nickname + " (" + local.username + ")", "title"))
                .padBottom(SPACE_SM).row();

        statusLabel = createLabel(statusText(client), "main");
        statusLabel.setWrap(true);
        statusLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        board.add(statusLabel).width(PANEL_WIDTH - 160).padBottom(SPACE_MD).row();

        board.add(secondaryButton("Disconnect", this::disconnect))
                .width(BUTTON_WIDTH).padTop(SPACE_SM).row();
        board.add(createLabel("Your progress syncs to the server automatically. Offline play "
                        + "keeps working with the same account either way.", "muted"))
                .width(PANEL_WIDTH - 200).padTop(SPACE_SM).row();

        Table wrap = new Table();
        wrap.add(scrollable(board)).width(PANEL_WIDTH);
        return wrap;
    }

    private Table buildLobbySection(NetworkClient client) {
        Table left = new Table();
        left.top();
        left.setBackground(skin.getDrawable("card-background"));
        left.pad(CARD_PAD * 2).defaults().pad(SPACE_XS);

        Table statusRow = new Table();
        statusRow.add(new Image(loadTextureSafe(WIFI_ICON_PATH))).size(28, 28).padRight(SPACE_SM);
        statusLabel = createLabel(statusText(client), "main");
        statusLabel.setWrap(true);
        statusRow.add(statusLabel).width(PANEL_WIDTH - PLAYERS_PANEL_WIDTH - 160);
        left.add(statusRow).colspan(2).padBottom(SPACE_MD).row();

        opponentField = field(false);
        addRow(left, "Challenge", opponentField);
        left.add(primaryButton("Send invite", this::challenge))
                .colspan(2).width(BUTTON_WIDTH).padTop(SPACE_SM).row();

        left.add(buildQueueArea(client)).colspan(2).padTop(SPACE_MD).row();

        Table footer = new Table();
        footer.add(secondaryButton("Disconnect", this::disconnect)).width(200);
        left.add(footer).colspan(2).padTop(SPACE_XL).row();

        Table right = buildPlayersPanel();

        Table row = new Table();
        row.add(left).width(PANEL_WIDTH - PLAYERS_PANEL_WIDTH - SPACE_MD).top().padRight(SPACE_MD);
        row.add(right).width(PLAYERS_PANEL_WIDTH).top();

        Table wrap = new Table();
        wrap.add(row).expand().fill();
        return wrap;
    }

    private Actor buildQueueArea(NetworkClient client) {
        if (!client.isQueued()) {
            return primaryButton("Find random opponent", () -> client.joinQueue(envelope -> build()));
        }

        Table waiting = new Table();
        Stack avatarStack = new Stack();
        avatarStack.add(new Image(loadTextureSafe(AVATAR_FRAME_PATH)));
        Table avatarWrap = new Table();
        avatarWrap.add(new Image(loadTextureSafe(UNKNOWN_AVATAR_PATH))).size(48, 48);
        avatarStack.add(avatarWrap);

        waiting.add(avatarStack).size(60, 60).padBottom(SPACE_SM).row();
        Table wifiRow = new Table();
        wifiRow.add(new Image(loadTextureSafe(WIFI_ICON_PATH))).size(22, 22).padRight(SPACE_XS);
        wifiRow.add(createLabel("Searching for an opponent...", "muted"));
        waiting.add(wifiRow).padBottom(SPACE_SM).row();
        waiting.add(secondaryButton("Cancel search", () -> {
            client.leaveQueue();
            build();
        })).width(260);
        return waiting;
    }

    private Table buildPlayersPanel() {
        Table panel = new Table();
        panel.top();
        panel.setBackground(woodDrawable());
        panel.pad(SPACE_MD);

        Table header = new Table();
        header.add(new Image(loadTextureSafe(PLAYER_ICON))).size(30, 30).padRight(SPACE_SM);
        header.add(createLabel("Players Online", "title"));
        panel.add(header).left().padBottom(SPACE_SM).row();

        playersTable = new Table();
        playersTable.top();
        panel.add(scrollable(playersTable)).grow().row();
        refreshPlayers();

        return panel;
    }

    private void refreshPlayers() {
        NetworkClient.get().refreshOnlinePlayers();
    }

    private void renderPlayers() {
        if (playersTable == null) return;
        playersTable.clear();
        var players = NetworkClient.get().getOnlinePlayers();
        if (players.isEmpty()) {
            playersTable.add(createLabel("Nobody else is online yet.", "muted")).left().pad(SPACE_SM).row();
            return;
        }
        for (NetworkClient.OnlinePlayer player : players) {
            playersTable.add(buildPlayerRow(player)).width(PLAYERS_PANEL_WIDTH - 40).padBottom(SPACE_SM).row();
        }
    }

    
    private Table buildPlayerRow(NetworkClient.OnlinePlayer player) {
        Table row = new Table();
        row.setBackground(skin.getDrawable("card-background"));
        row.pad(6, 10, 6, 10).defaults().pad(0, 4, 0, 4);

        Stack avatarStack = new Stack();
        avatarStack.add(new Image(loadTextureSafe(AVATAR_FRAME_PATH)));
        Table avatarWrap = new Table();
        avatarWrap.add(new Image(loadTextureSafe(UNKNOWN_AVATAR_PATH))).size(26, 26);
        avatarStack.add(avatarWrap);
        row.add(avatarStack).size(36, 36);

        Table nameCol = new Table();
        nameCol.add(new Label(player.nickname(), skin, "main")).left().row();
        Label username = createLabel(player.username(), "muted");
        username.setFontScale(0.75f);
        nameCol.add(username).left();
        row.add(nameCol).left().expandX();

        if (player.inMatch()) {
            row.add(createLabel("in a match", "muted")).right();
        } else {
            row.add(secondaryButton("Challenge", () -> {
                opponentField.setText(player.username());
                challenge();
            })).width(140).right();
        }
        return row;
    }

    private void disconnect() {
        NetworkClient.get().disconnect();
        Toast.show(stage, "Disconnected. You are playing offline again.");
        build();
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

        if (client.isConnected() != shownConnected || client.isSignedIn() != shownSignedIn) {
            build();
            return;
        }

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

    private TextureRegionDrawable woodDrawable() {
        return new TextureRegionDrawable(loadTextureSafe(WOOD_BACKGROUND_PATH));
    }

    private Table buildTopBar() {
        ImageButton backBtn = new ImageButton(new TextureRegionDrawable(loadTextureSafe(BACK_ICON_PATH)));
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

        ImageButton leaderboardBtn = new ImageButton(
                new TextureRegionDrawable(loadTextureSafe(LEADERBOARD_ICON_PATH)));
        leaderboardBtn.getImageCell().size(48, 48);
        leaderboardBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                runCommand("menu enter leaderboard");
            }
        });

        Table bar = new Table();
        bar.add(left).left().expandX();
        bar.add(leaderboardBtn).right();
        return bar;
    }

    @Override
    protected void onAfterCommand() {
        build();
    }
}