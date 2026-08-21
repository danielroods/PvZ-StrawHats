package view.screens.match.gameplay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.files.FileHandle;

import model.match.main.levels.Level;
import model.utils.GameSession;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.GameScreen;

public class EgyptGameScreen extends GameScreen {

    private static final String PANEL_BACKGROUND = "images/chapters/egypt/egypt_gameplay/map.png";

    private Table egyptSidePanel;
    private TextureBank textureBank;
    private PamPlayer pamPlayer;
    private boolean sidePanelBuilt = false;

    @Override
    public void show() {
        super.show();
        AudioManager.get().playMusic(AudioEnum.EGYPT_MUSIC, true);
        initPam();
        sidePanelBuilt = false;
        buildEgyptSidePanel();
    }

    private void initPam() {
        if (textureBank == null) {
            try {
                FileHandle root = Gdx.files.internal("assets/pvz-assets");
                textureBank = new TextureBank("atlases", root);
                pamPlayer = new PamPlayer(textureBank, root);
                Gdx.app.log("PAM_INIT", "PAM System and TextureBank initialized successfully!");
            } catch (Throwable t) {
                textureBank = null;
                pamPlayer = null;
                Gdx.app.error("PAM_INIT", "Failed to initialize PAM System", t);
            }
        }
    }

    private void buildEgyptSidePanel() {
        GameSession session = GameSession.peekInstance();
        Level level = session == null ? null : session.getLevel();
        if (level == null) {
            return;
        }

        if (egyptSidePanel != null) {
            egyptSidePanel.remove();
            egyptSidePanel = null;
        }

        egyptSidePanel = new Table();
        egyptSidePanel.setTouchable(Touchable.childrenOnly);
        try {
            egyptSidePanel.setBackground(new TextureRegionDrawable(loadTextureSafe(PANEL_BACKGROUND)));
        } catch (Throwable t) {
            Gdx.app.error("EgyptGameScreen", "Failed to load side panel background: " + PANEL_BACKGROUND, t);
            egyptSidePanel.setBackground(skin.getDrawable("card-background"));
        }
        egyptSidePanel.pad(12f);

        Label title = new Label("ANCIENT EGYPT", skin, "title");
        title.setAlignment(Align.center);
        egyptSidePanel.add(title).width(300f).center().row();

        Label stageLabel = new Label(level.getName(), skin, "main");
        stageLabel.setAlignment(Align.center);
        stageLabel.setWrap(true);
        egyptSidePanel.add(stageLabel).width(300f).padBottom(6f).row();

        Label poolTitle = new Label("ZOMBIES", skin, "main");
        poolTitle.setAlignment(Align.center);
        egyptSidePanel.add(poolTitle).row();

        Table pool = new Table();
        pool.top().left();
    }
}
