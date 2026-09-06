package view.screens.ui_menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import model.news.News;
import model.user_data.User;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.ParticleCreator;
import view.screens.generals.UiScreen;

import java.util.ArrayList;
import java.util.List;

public class NewsScreen extends UiScreen {

    private enum TabState { NONE, UNREAD, ALL }

    private List<String> resultLines = null;
    private String emptyMessage = null;
    private TabState currentTab = TabState.NONE;

    @Override
    public void show() {
        setBackground("assets/images/backg/mainmenu_background.png");
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
        super.show();
        build();
    }

    @Override
    public void initParticles() {
        if (particles != null) {
            particles.dispose();
        }

        particlePaths = new String[]{"assets/images/ui/zombie_bighead_newspaper_117x138.png"};

        particles = new ParticleCreator(particlePaths, 12, 35f, 55f, 1.2f, true);
        Actor particleActor = particles.createActor();
        particleActor.setTouchable(Touchable.disabled);
        rootStack.addActorAt(1, particleActor);
    }

    private void build() {
        rootTable.clear();

        ImageButton backBtn = createIconButton("assets/images/ui/buttons_hud_back_normal.png", 54, 54, () -> runCommand("menu exit"));

        Table topBar = new Table();
        topBar.add(backBtn).left().expandX();
        rootTable.add(topBar).fillX().padTop(15).padLeft(20).row();

        User user = User.currentUser;
        if (user == null) {
            Table card = new Table();
            card.setBackground(skin.getDrawable("card-background"));
            card.pad(40);
            card.add(new Label("No user is currently logged in.", skin, "muted")).row();
            rootTable.add(card).expand().center();
            return;
        }

        TextureRegionDrawable woodBoardBg = createTextureDrawable("assets/images/backg/wood board.png");
        TextureRegionDrawable tornPaperBg = createTextureDrawable("assets/images/backg/notif bg.png");
        TextureRegionDrawable tabBg = createTextureDrawable("assets/images/ui/zombies_active.png");
        TextureRegionDrawable arrowDown = createTextureDrawable("assets/images/ui/zombies_active.png");

        Label.LabelStyle blackStyle = new Label.LabelStyle(skin.getFont("default-font"), Color.BLACK);

        Stack mainStack = new Stack();

        float tabW = 200f;
        float tabH = 55f;
        float spacing = 20f;
        float padL = 40f;

        float arrowW = 36f;
        float arrowH = 26f;
        float arrowPadTop = tabH - 8f;

        float padLeftArrow1 = padL + (tabW / 2f) - (arrowW / 2f);
        float padLeftArrow2 = spacing + tabW - arrowW;

        Table inArrows = new Table();
        inArrows.top().left();

        Image arrowAllIn = new Image(arrowDown);
        Image arrowUnreadIn = new Image(arrowDown);
        arrowAllIn.setVisible(currentTab != TabState.ALL);
        arrowUnreadIn.setVisible(currentTab != TabState.UNREAD);

        inArrows.add(arrowAllIn).size(arrowW, arrowH).padTop(arrowPadTop).padLeft(padLeftArrow1);
        inArrows.add(arrowUnreadIn).size(arrowW, arrowH).padTop(arrowPadTop).padLeft(padLeftArrow2);

        Table woodLayer = new Table();
        woodLayer.padTop(tabH - 12f);
        Table boardWrapper = new Table();
        boardWrapper.setBackground(woodBoardBg);
        boardWrapper.pad(40);
        woodLayer.add(boardWrapper).width(780).height(480);

        Table outArrows = new Table();
        outArrows.top().left();

        Image arrowAllAct = new Image(arrowDown);
        Image arrowUnreadAct = new Image(arrowDown);
        arrowAllAct.setVisible(currentTab == TabState.ALL);
        arrowUnreadAct.setVisible(currentTab == TabState.UNREAD);

        outArrows.add(arrowAllAct).size(arrowW, arrowH).padTop(arrowPadTop).padLeft(padLeftArrow1);
        outArrows.add(arrowUnreadAct).size(arrowW, arrowH).padTop(arrowPadTop).padLeft(padLeftArrow2);

        Table tabsLayer = new Table();
        tabsLayer.top().left();
        tabsLayer.padTop(0).padLeft(padL);

        Table allTab = new Table();
        allTab.setBackground(tabBg);
        Label allLabel = new Label("All News", skin, "title");
        allLabel.setFontScale(0.85f);
        allTab.add(allLabel).center();
        allTab.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleShowAll();
            }
        });

        Table unreadTab = new Table();
        unreadTab.setBackground(tabBg);
        Label unreadLabel = new Label("Unread News", skin, "title");
        unreadLabel.setFontScale(0.85f);
        unreadTab.add(unreadLabel).center();
        unreadTab.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleShowUnread();
            }
        });

        tabsLayer.add(allTab).size(tabW, tabH).padRight(spacing);
        tabsLayer.add(unreadTab).size(tabW, tabH);

        Table list = new Table();
        list.top();

        if (currentTab == TabState.NONE) {
            addNewsItem(list, "Pick an option above to view your news.", tornPaperBg, blackStyle);
        } else if (resultLines == null || resultLines.isEmpty()) {
            addNewsItem(list, emptyMessage, tornPaperBg, blackStyle);
        } else {
            for (String line : resultLines) {
                addNewsItem(list, line, tornPaperBg, blackStyle);
            }
        }

        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        boardWrapper.add(scrollPane).expand().fill();

        mainStack.add(inArrows);
        mainStack.add(woodLayer);
        mainStack.add(outArrows);
        mainStack.add(tabsLayer);

        rootTable.add(mainStack).expand().center().padBottom(30);
    }

    private void addNewsItem(Table list, String text, Drawable bg, Label.LabelStyle style) {
        Table item = new Table();
        item.setBackground(bg);
        item.pad(22, 28, 22, 28);
        Label label = new Label(text, style);
        label.setWrap(true);
        item.add(label).width(600).left();
        list.add(item).padBottom(16).row();
    }

    private void handleShowUnread() {
        currentTab = TabState.UNREAD;
        List<String> lines = new ArrayList<>();

        if (User.currentUser != null && User.currentUser.userState.news.isEmpty()) {
            lines.add("[NEW] Welcome to PvZ! Your account has been successfully created. Stay tuned for more news.");
        } else if (User.currentUser != null) {
            for (News news : User.currentUser.userState.news) {
                if (!news.isRead()) {
                    lines.add("[NEW] " + news.getText());
                }
            }
        }

        resultLines = lines;
        emptyMessage = "No unread news.";
        runCommand("menu news show-unread");
    }

    private void handleShowAll() {
        currentTab = TabState.ALL;
        List<String> lines = new ArrayList<>();

        if (User.currentUser != null && User.currentUser.userState.news.isEmpty()) {
            lines.add("[NEW] Welcome to PvZ! Your account has been successfully created. Stay tuned for more news.");
        } else if (User.currentUser != null) {
            for (News news : User.currentUser.userState.news) {
                String status = news.isRead() ? "[READ]" : "[NEW]";
                lines.add(status + " " + news.getText());
            }
        }

        resultLines = lines;
        emptyMessage = "No news available.";
        runCommand("menu news show-all");
    }


    private TextureRegionDrawable createTextureDrawable(String path) {
        return new TextureRegionDrawable(loadLinearTextureSafe(path));
    }

    private Texture loadLinearTextureSafe(String path) {
        if (path != null && !path.isEmpty() && Gdx.files.internal(path).exists()) {
            Texture tex = new Texture(Gdx.files.internal(path));
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return tex;
        }
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        Texture fallback = new Texture(pixmap);
        pixmap.dispose();
        return fallback;
    }

    @Override
    protected void onAfterCommand() {
        build();
    }
}