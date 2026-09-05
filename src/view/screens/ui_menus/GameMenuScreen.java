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
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import model.match.main.levels.Level;
import model.resoures.CurrencyType;
import model.user_data.User;
import model.utils.LevelLoader;
import model.utils.LevelProgression;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.UiScreen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameMenuScreen extends UiScreen {

    private static final String[] CHAPTERS = {
            "Egypt", "Frostbite Caves", "Big Wave Beach", "Dark Ages", "Pirates" , "Future"
    };
    private static final String[] CHAPTER_ART = {
            "assets/images/chapters/egypt.png",
            "assets/images/chapters/iceage.png",
            "assets/images/chapters/beach.png",
            "assets/images/chapters/dark.png",
            "assets/images/chapters/pirate/pirate.png",
            "assets/images/chapters/future/future.png"
    };

    private static final float CARD_WIDTH = 280f;
    private static final float CARD_HEIGHT = 230f;
    private static final float CHAPTER_CARD_PAD = 35f;
    private static final float CAROUSEL_VIEWPORT_WIDTH = 1270f;

    private ScrollPane carouselPane;
    private Table carouselContent;

    @Override
    public void show() {
        setBackground("assets/images/backg/mainmenu_background.png");
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
        super.show();
        build();
    }

    private void build() {
        rootTable.clear();

        rootTable.add(buildTopBar()).fillX().padTop(5).padLeft(15).padRight(15).row();

        Table centerTable = new Table();
        Label label = new Label("Choose a Chapter", skin, "title");
        label.setFontScale(1.9f);
        centerTable.add(label).padTop(-180).padBottom(25).row();
        centerTable.add(buildChapterCarousel());

        rootTable.add(centerTable).expand().center().row();
    }

    private Table buildChapterCarousel() {
        Map<String, Boolean> unlockStatus = computeChapterUnlockStatus();

        carouselContent = new Table();

        float sidePadding = (CAROUSEL_VIEWPORT_WIDTH - CARD_WIDTH) / 2f;

        for (int i = 0; i < CHAPTERS.length; i++) {
            String chapter = CHAPTERS[i];
            float leftPad = (i == 0) ? sidePadding : CHAPTER_CARD_PAD;
            float rightPad = (i == CHAPTERS.length - 1) ? sidePadding : CHAPTER_CARD_PAD;

            carouselContent.add(createChapterCard(chapter, CHAPTER_ART[i], unlockStatus.getOrDefault(chapter, false)))
                    .size(CARD_WIDTH, CARD_HEIGHT)
                    .padLeft(leftPad)
                    .padRight(rightPad);
        }

        carouselPane = new ScrollPane(carouselContent);
        carouselPane.setOverscroll(false, false);
        carouselPane.setFlickScroll(true);
        carouselPane.setScrollingDisabled(false, true);

        Table viewport = new Table();
        viewport.add(carouselPane).width(CAROUSEL_VIEWPORT_WIDTH).height(CARD_HEIGHT + 90f);

        return viewport;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        updateCarouselScaling();
    }

    private void updateCarouselScaling() {
        if (carouselPane == null || carouselContent == null) return;

        float paneWidth = carouselPane.getWidth();
        float scrollX = carouselPane.getScrollX();
        float centerX = scrollX + paneWidth / 2f;

        float maxDistance = 380f;

        for (Actor child : carouselContent.getChildren()) {
            float childCenterX = child.getX() + child.getWidth() / 2f;
            float distance = Math.abs(centerX - childCenterX);

            float scale = 1.0f;
            if (distance < maxDistance) {
                float factor = 1.0f - (distance / maxDistance);
                scale = 1.0f + 0.30f * factor;
            }

            child.setOrigin(Align.center);
            child.setScale(scale);
        }
    }

    private Table buildTopBar() {
        ImageButton backBtn = createIconButton("assets/images/ui/buttons_hud_back_normal.png", 54, 54,
                () -> runCommand("menu exit"));

        Table topLeft = new Table();
        topLeft.add(backBtn).padRight(16);
        topLeft.add(createIconButtonWithLabel("assets/images/ui/collection.png", 54, 54,
                "Collection", () -> runCommand("menu enter collection"))).padRight(16);
        topLeft.add(createIconButtonWithLabel("assets/images/ui/greenhouse.png", 54, 54,
                "Greenhouse", () -> runCommand("menu greenhouse"))).padRight(16);
        topLeft.add(createIconButtonWithLabel("assets/images/ui/leaderboard.png", 54, 54,
                "Leaderboard", () -> runCommand("menu leaderboard")));

        User user = User.currentUser;
        int coins = (user != null && user.userState != null) ? user.userState.coins : 0;
        int diamonds = (user != null && user.userState != null) ? user.userState.diamonds : 0;

        Table topRight = new Table();
        topRight.add(currencyWidget(CurrencyType.COIN, coins)).padRight(15);
        topRight.add(currencyWidget(CurrencyType.DIAMOND, diamonds));

        Table topBar = new Table();
        topBar.add(topLeft).left().expandX();
        topBar.add(topRight).right();
        return topBar;
    }

    private Map<String, Boolean> computeChapterUnlockStatus() {
        Map<String, Boolean> status = new LinkedHashMap<>();
        try {
            List<Level> allLevels = LevelProgression.sorted(LevelLoader.loadLevels());
            int lastLevel = (User.currentUser != null && User.currentUser.userState != null)
                    ? User.currentUser.userState.lastLevel : 0;
            for (String chapter : CHAPTERS) {
                boolean unlocked = allLevels.stream()
                        .filter(level -> level.getSeason().getName().equalsIgnoreCase(chapter))
                        .anyMatch(level -> LevelProgression.isUnlocked(allLevels, lastLevel, level));
                status.put(chapter, unlocked);
            }
        } catch (Exception e) {
            for (String chapter : CHAPTERS) status.put(chapter, false);
        }
        return status;
    }

    private Actor createChapterCard(String chapter, String artPath, boolean unlocked) {
        Stack stack = new Stack();
        stack.setTransform(true);
        stack.setSize(CARD_WIDTH, CARD_HEIGHT);
        stack.setOrigin(Align.center);

        TextureRegionDrawable drawable = new TextureRegionDrawable(loadTextureSafe(artPath));
        ImageButton button = new ImageButton(drawable);

        stack.add(button);

        if (!unlocked) {
            Texture lockTex = loadTextureSafe("assets/images/ui/collection/lock_small_gold.png");
            Image lockImg = new Image(lockTex);
            Table lockTable = new Table();
            lockTable.add(lockImg).center();
            stack.add(lockTable);
        }

        stack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                runCommand("menu enter chapter -c " + chapter);
            }
        });

        return stack;
    }

    private ImageButton createIconButton(String path, float width, float height, Runnable action) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(loadTextureSafe(path));
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

    @Override
    protected void onAfterCommand() {
        build();
    }

    @Override
    protected void refreshContent() {
        build();
    }
}