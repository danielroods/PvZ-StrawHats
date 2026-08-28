package view.screens.ui_menus;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import model.resoures.CurrencyType;
import model.user_data.User;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.UiScreen;


public class MainMenuScreen extends UiScreen {

    private ScrollPane carouselPane;

    @Override
    public void show() {
        setBackground("assets/images/backg/mainmenu_background.png");
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
        super.show();
        build();
    }

    @Override
    protected void refreshContent() {
        build();
    }

    private void build() {
        rootTable.clear();
        rootTable.setFillParent(true);

        Table topBar = new Table();

        Table topLeft = new Table();
        topLeft.add(createProfileButton(() -> runCommand("menu enter profile"))).size(72, 72).padLeft(30).padRight(35);

        topLeft.add(createIconButtonWithLabel("assets/images/ui/buttons_hud_settings_selected.png", 54, 54, "Setting", () -> runCommand("menu enter settings"))).padRight(35);

        topLeft.add(createIconButtonWithLabel("assets/images/ui/buttons_hud_news_selected copy 2.png", 54, 54, "News", () -> runCommand("menu enter news")));

        Table topRight = new Table();
        User user = User.currentUser;
        int coins = (user != null && user.userState != null) ? user.userState.coins : 0;
        int diamonds = (user != null && user.userState != null) ? user.userState.diamonds : 0;

        topRight.add(currencyWidget(CurrencyType.COIN, coins)).padRight(15);
        topRight.add(currencyWidget(CurrencyType.DIAMOND, diamonds));

        topBar.add(topLeft).left().expandX();
        topBar.add(topRight).right();

        Table centerTable = new Table();
        Table carouselContent = new Table();
        carouselContent.padLeft(425).padRight(425);

        Actor gameBtn = createBannerCard("assets/images/ui/calendar_card_7day_tombtangled.png", "Game", () -> runCommand("menu enter game"));
        Actor travelBtn = createBannerCard("assets/images/ui/calendar_card_7day_bigwavebeach.png", "Travel Log", () -> runCommand("menu enter travellog"));
        Actor leaderboardBtn = createBannerCard("assets/images/ui/calendar_card_7day_lunar_new_year.png", "Leaderboard", () -> runCommand("menu enter leaderboard"));

        carouselContent.add(gameBtn).size(420, 260).pad(20);
        carouselContent.add(travelBtn).size(420, 260).pad(20);
        carouselContent.add(leaderboardBtn).size(420, 260).pad(20);

        carouselPane = new ScrollPane(carouselContent);
        carouselPane.setOverscroll(false, false);
        carouselPane.setFlickScroll(true);
        carouselPane.setScrollingDisabled(false, true);

        centerTable.add(carouselPane).width(1270).height(300);

        Table bottomBar = new Table();
        TextButton logoutBtn = secondaryButton("Log out", this::confirmLogout);
        bottomBar.add(logoutBtn).width(160).height(45).left().expandX();

        rootTable.add(topBar).fillX().padTop(5).padLeft(15).padRight(15).row();
        rootTable.add(centerTable).expand().center().row();
        rootTable.add(bottomBar).fillX().padBottom(10).padLeft(15).row();
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        if (carouselPane != null && carouselPane.getWidget() instanceof Table) {
            Table contentTable = (Table) carouselPane.getWidget();
            float viewportCenter = carouselPane.getScrollX() + carouselPane.getWidth() / 2f;
            for (Actor child : contentTable.getChildren()) {
                float childCenter = child.getX() + child.getWidth() / 2f;
                float dist = Math.abs(viewportCenter - childCenter);
                float maxDist = 460f;
                float factor = Math.max(0f, 1f - dist / maxDist);
                float scale = 0.8f + 0.28f * factor;
                child.setScale(scale);
                child.setOrigin(child.getWidth() / 2f, child.getHeight() / 2f);
            }
        }
    }

    private Actor createProfileButton(Runnable action) {
        Stack stack = new Stack();

        Texture frameTex = loadTextureSafe("assets/images/ui/reward4_bg.png");
        Image frameImg = new Image(frameTex);

        User user = User.currentUser;
        String avatarPath = (user != null && user.profilePicture != null && !user.profilePicture.isEmpty())
                ? user.profilePicture
                : "assets/images/ui/avatar_luffy.png";

        Texture avatarTex = loadTextureSafe(avatarPath);
        Image avatarImg = new Image(avatarTex);

        Table avatarContainer = new Table();
        avatarContainer.add(avatarImg).size(48, 48);

        stack.add(frameImg);
        stack.add(avatarContainer);

        stack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });

        return stack;
    }

    private Actor createBannerCard(String path, String title, Runnable action) {
        Stack stack = new Stack();
        stack.setTransform(true);
        Texture texture = loadTextureSafe(path);
        TextureRegionDrawable drawable = new TextureRegionDrawable(texture);
        ImageButton button = new ImageButton(drawable);

        Label label = new Label(title, skin, "title");
        label.setAlignment(Align.center);

        Table textTable = new Table();
        textTable.add(label).expand().bottom().padBottom(20);

        stack.add(button);
        stack.add(textTable);

        stack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });

        return stack;
    }

    private ImageButton createIconButton(String path, float width, float height, Runnable action) {
        Texture texture = loadTextureSafe(path);
        TextureRegionDrawable drawable = new TextureRegionDrawable(texture);
        ImageButton button = new ImageButton(drawable);
        button.getImageCell().size(width, height);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return button;
    }

    private Actor createIconButtonWithLabel(String path, float width, float height, String text, Runnable action) {
        Table container = new Table();
        ImageButton btn = createIconButton(path, width, height, action);
        Label label = new Label(text, skin, "title");
        label.setAlignment(Align.center);

        container.add(btn).row();
        container.add(label).padTop(2);

        container.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return container;
    }

    private void confirmLogout() {
        new ConfirmModal("Log out?", "You'll need your password to sign back in.", "Log out",
                () -> runCommand("menu logout")).show();
    }
}