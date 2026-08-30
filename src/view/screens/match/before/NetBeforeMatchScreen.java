package view.screens.match.before;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

import controller.match.BeforeMenu;
import controller.match.NetBeforeMenu;
import controller.ui_menus.network.NetworkMenu;
import model.App;
import model.match.main.levels.Level;
import model.utils.GameSession;
import net.client.NetMatchState;
import net.client.NetworkClient;
import view.screens.generals.Toast;

public class NetBeforeMatchScreen extends CoopBeforeMatchScreen {

    private static final float STATUS_REFRESH_SECONDS = 0.25f;

    private Label statusLabel;
    private float sinceStatusRefresh;
    private boolean lastOpponentReady;
    private boolean lastMyReady;

    private NetBeforeMenu menu() {
        return App.currentMenu instanceof NetBeforeMenu net ? net : null;
    }

    private NetMatchState state() {
        NetBeforeMenu menu = menu();
        return menu == null ? null : menu.getState();
    }

    private boolean isPlantSide() {
        NetBeforeMenu menu = menu();
        return menu == null || menu.isPlantSide();
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        sinceStatusRefresh += delta;
        if (sinceStatusRefresh < STATUS_REFRESH_SECONDS) return;
        sinceStatusRefresh = 0f;

        NetMatchState state = state();
        if (state == null) return;
        if (state.isEnded()) {
            abandonLobby(state.getEndReason());
            return;
        }
        if (state.isMyReady() != lastMyReady || state.isOpponentReady() != lastOpponentReady) {
            lastMyReady = state.isMyReady();
            lastOpponentReady = state.isOpponentReady();
            scheduleBuild();
        } else if (statusLabel != null) {
            statusLabel.setText(statusText(state));
        }
    }

    @Override
    protected void build() {
        rootTable.clear();
        cards.clear();

        if (startOverlay != null) {
            startOverlay.remove();
            startOverlay = null;
        }

        Level level = GameSession.peekInstance() == null
                ? null : GameSession.peekInstance().getLevel();
        if (level == null) {
            rootTable.add(new Label("Waiting for the match...", skin, "title")).center();
            return;
        }

        rootTable.top().left();

        Table topBar = buildTopBar(level);
        Table middle = isPlantSide() ? buildMiddleSection(level) : buildZombieSideSection(level);

        float shift = isPlantSide() ? 0f : SCREEN_SHIFT_RIGHT;
        rootTable.add(topBar).expandX().fillX().top().left()
                .padTop(8f).padLeft(12f + shift).padRight(16f).row();
        rootTable.add(middle).expand().fill().top().left()
                .padTop(6f).padLeft(12f + shift);

        topBar.toFront();

        startOverlay = buildBottomBar();
        startOverlay.setFillParent(true);
        addBeforeModal(startOverlay);
    }

    private Table buildZombieSideSection(Level level) {
        Table board = new Table();
        board.top().left();

        Table column = new Table();
        column.top().left();
        column.add(buildPreviewPanel()).expandX().fillX().padBottom(SPACE_SM).row();

        Label header = new Label("ZOMBIES", skin, "title");
        header.setFontScale(0.7f);
        column.add(header).left().padBottom(2f).row();
        column.add(buildZombieGrid()).expandX().fillX().expandY().fillY().top().left();

        board.add(column).expand().fill().top().left().padRight(10f);
        board.add(buildZombieLoadoutPanel()).width(90f).expandY().fillY().top().left();
        return board;
    }

    @Override
    protected Table buildBottomBar() {
        NetMatchState state = state();

        Table bottom = new Table();
        bottom.bottom().right();
        bottom.pad(12f).padRight(16f);

        Table panel = new Table();
        panel.setBackground(skin.getDrawable("card-background"));
        panel.pad(8f);

        statusLabel = new Label(statusText(state), skin, "main");
        statusLabel.setFontScale(0.75f);
        statusLabel.setAlignment(Align.center);
        panel.add(statusLabel).width(230f).padBottom(6f).row();

        boolean ready = state != null && state.isMyReady();
        TextButton button = coloredButton(ready ? "Waiting..." : "Ready!",
                ready ? GRAY : GREEN, 0.9f);
        if (!ready) {
            button.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                    float x, float y) {
                    if (runCommand("ready")) scheduleBuild();
                }
            });
        } else {
            button.setDisabled(true);
        }
        panel.add(button).width(180f).height(48f);

        bottom.add(panel);
        return bottom;
    }

    private void abandonLobby(String reason) {
        if (reason != null && !reason.isBlank()) Toast.show(stage, reason);
        NetworkClient.get().clearMatchState();
        BeforeMenu.selectedPlants.clear();
        BeforeMenu.selectedZombies.clear();
        App.currentMenu = new NetworkMenu();
    }

    private String statusText(NetMatchState state) {
        if (state == null) return "Waiting for the match...";
        String opponent = state.getOpponentNickname();
        String side = isPlantSide() ? "You defend the brainz." : "You bring the brainz eaters.";
        if (state.isMyReady() && !state.isOpponentReady()) {
            return side + "\nWaiting for " + opponent + "...";
        }
        if (!state.isMyReady() && state.isOpponentReady()) {
            return side + "\n" + opponent + " is ready!";
        }
        if (state.isMyReady()) {
            return side + "\nStarting the match...";
        }
        return side + "\n" + opponent + " is still picking.";
    }
}
