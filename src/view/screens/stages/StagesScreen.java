package view.screens.stages;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import controller.ScreenManager;
import controller.match.MatchMenu;
import model.match.endless.EndlessChapter;
import model.match.endless.EndlessLevels;
import model.match.main.levels.Level;
import model.resoures.CurrencyType;
import model.user_data.User;
import model.utils.LevelLoader;
import model.utils.LevelProgression;

import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;

import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.ParticleCreator;
import view.screens.generals.UiScreen;
import view.screens.match.before.ComicIntroScreen;


import java.util.ArrayList;
import java.util.List;

public abstract class StagesScreen extends UiScreen {

    public enum LevelNodeState {
        LOCKED_IDLE("locked_idle"),
        LOCKED_ANIMATION("locked_animation"),
        UNLOCKED("unlocked"),
        UNLOCKED_ANIMATION("unlocked_animation"),
        FINISHED("finished");

        private final String pamState;

        LevelNodeState(String pamState) {
            this.pamState = pamState;
        }

        public String getPamState() {
            return pamState;
        }
    }

    public enum DangerNodeState {
        LOCKED_IDLE("locked_idle"),
        UNLOCKED_ANIMATION("unlocked_animation"),
        UNLOCKED_IDLE("unlocked_idle");

        private final String pamState;

        DangerNodeState(String pamState) {
            this.pamState = pamState;
        }

        public String getPamState() {
            return pamState;
        }
    }

    protected enum StageStatus { LOCKED, UNLOCKED, COMPLETED, CURRENT }

    protected List<Level> allLevels = new ArrayList<>();
    protected List<Level> chapterLevels = new ArrayList<>();

    protected Label selectionLabel;
    protected TextButton playButton;

    protected TextureBank textureBank;
    protected PamPlayer pamPlayer;

    protected abstract String getChapterName();

    protected abstract String getChapterBackground();

    protected abstract String getBackIcon();

    protected abstract String getCollectionIcon();

    protected abstract String getGreenhouseIcon();

    protected abstract String getLeaderboardIcon();

    protected abstract String getCoinIcon();

    protected abstract String getGemIcon();

    protected abstract String getComicSheetPath();

    protected abstract Table buildPathContainer();

    @Override
    public void show() {
        String background = getChapterBackground();
        if (background != null && !background.isEmpty() && Gdx.files.internal(background).exists()) {
            setBackground(background);
        }
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);

        if (textureBank == null) {
            try {
                FileHandle rootHandle = Gdx.files.internal("assets/pvz-assets");
                textureBank = new TextureBank("atlases", rootHandle);
                pamPlayer = new PamPlayer(textureBank, rootHandle);

                Gdx.app.log("PAM_INIT", "PAM System and TextureBank initialized successfully!");
            } catch (Throwable t) {
                Gdx.app.error("PAM_INIT", "Failed to initialize PAM System", t);
            }
        }

        super.show();
        loadLevels();
        build();
    }

    @Override
    public void render(float delta) {
        if (textureBank != null) {
            try {
                textureBank.update();
            } catch (Throwable t) {
            }
        }
        super.render(delta);
    }

    protected void setupParticles(String particlePath, int count, float minSpeed, float maxSpeed, float scale) {
        if (particles != null) {
            particles.dispose();
        }
        particlePaths = new String[]{ particlePath };
        particles = new ParticleCreator(particlePaths, count, minSpeed, maxSpeed, scale, true);

        Actor particleActor = particles.createActor();
        particleActor.setTouchable(Touchable.disabled);
        rootStack.addActorAt(1, particleActor);
    }

    protected void loadLevels() {
        try {
            allLevels = LevelProgression.sorted(LevelLoader.loadLevels());
        } catch (Exception e) {
            allLevels = new ArrayList<>();
        }
        if (allLevels != null) {
            chapterLevels = allLevels.stream()
                    .filter(level -> level != null && level.getSeason() != null && level.getSeason().getName() != null)
                    .filter(level -> level.getSeason().getName().equalsIgnoreCase(getChapterName()))
                    .toList();
        } else {
            chapterLevels = new ArrayList<>();
        }
    }

    /**
     * The danger node is this chapter's Lottery stage: a hidden, unlisted endless level
     * built from the chapter's own stages, so it plays the same season (map, hazards,
     * obstacles, assets) with the chapter's full zombie roster - and, unlike an authored
     * stage, keeps generating waves for as long as the player survives.
     */
    protected Level buildDangerLevel() {
        EndlessChapter chapter = EndlessChapter.forSeason(getChapterName());
        if (chapter == null) return null;
        return EndlessLevels.forChapter(chapter, chapterLevels);
    }

    protected void playDangerNode() {
        Level lottery = buildDangerLevel();
        if (lottery == null) return;
        MatchMenu.selectedLevel = lottery;
        runCommand("start game");
    }

    protected void build() {
        rootTable.clear();

        Table topBar = buildTopBar();
        Table pathContainer = buildPathContainer();
        Table selectionBar = buildSelectionBar();

        rootTable.add(topBar).fillX().padTop(5).padLeft(15).padRight(15).row();
        rootTable.add(pathContainer).expand().padTop(SPACE_SM).row();
        rootTable.add(selectionBar).fillX().padBottom(SPACE_SM);

        topBar.toFront();
        selectionBar.toFront();
    }

    protected Table buildTopBar() {
        ImageButton backBtn = createIconButton(getBackIcon(), 54, 54, () -> runCommand("menu exit"));

        Table topLeft = new Table();
        topLeft.add(backBtn).padRight(16);
        topLeft.add(createIconButtonWithLabel(getCollectionIcon(), 54, 54,
                "Collection", () -> runCommand("menu enter collection"))).padRight(16);
        topLeft.add(createIconButtonWithLabel(getGreenhouseIcon(), 54, 54,
                "Greenhouse", () -> runCommand("menu greenhouse"))).padRight(16);
        topLeft.add(createIconButtonWithLabel(getLeaderboardIcon(), 54, 54,
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

    protected Table buildSelectionBar() {
        Table bar = new Table();
        bar.setBackground(skin.getDrawable("card-background"));
        bar.pad(14);

        selectionLabel = new Label("", skin, "main");
        selectionLabel.setWrap(true);

        playButton = primaryButton("Play", () -> {
            Level selected = MatchMenu.selectedLevel;

            if (selected != null && !chapterLevels.isEmpty() && selected.getId() == chapterLevels.get(0).getId()) {
                ComicIntroScreen.COMIC_SHEET_PATH = getComicSheetPath();
                ScreenManager.setScreen(new ComicIntroScreen(() -> runCommand("start game")));
            } else {
                runCommand("start game");
            }
        });
        bar.add(selectionLabel).width(760).left().expandX();
        bar.add(playButton).width(180).height(56).padLeft(SPACE_LG);

        refreshSelectionBar();
        return bar;
    }

    protected void refreshSelectionBar() {
        Level selected = MatchMenu.selectedLevel;
        boolean isChapterSelection = selected != null && selected.getSeason() != null
                && selected.getSeason().getName().equalsIgnoreCase(getChapterName());
        if (isChapterSelection) {
            selectionLabel.setText(selected.getName() + "\n" + selected.getGameMode());
            playButton.setDisabled(false);
            playButton.setTouchable(Touchable.enabled);
        } else {
            selectionLabel.setText("Tap an unlocked stage to select it.");
            playButton.setDisabled(true);
            playButton.setTouchable(Touchable.disabled);
        }
    }

    @Override
    protected void onAfterCommand() {
        build();
    }

    @Override
    protected void refreshContent() {
        build();
    }

    protected StageStatus statusOf(Level level) {
        int lastLevel = (User.currentUser != null && User.currentUser.userState != null)
                ? User.currentUser.userState.lastLevel : 0;
        Level selected = MatchMenu.selectedLevel;
        if (selected != null && selected.getId() == level.getId()) {
            return StageStatus.CURRENT;
        }
        if (LevelProgression.isCompleted(allLevels, lastLevel, level)) {
            return StageStatus.COMPLETED;
        }
        if (LevelProgression.isUnlocked(allLevels, lastLevel, level)) {
            return StageStatus.UNLOCKED;
        }
        return StageStatus.LOCKED;
    }

    protected LevelNodeState levelNodeStateOf(int index, StageStatus status) {
        switch (status) {
            case COMPLETED:
                return LevelNodeState.FINISHED;
            case CURRENT:
                return LevelNodeState.UNLOCKED_ANIMATION;
            case UNLOCKED:
                return LevelNodeState.UNLOCKED;
            case LOCKED:
            default:
                boolean nextUp = index > 0 && statusOf(chapterLevels.get(index - 1)) != StageStatus.LOCKED;
                return nextUp ? LevelNodeState.LOCKED_ANIMATION : LevelNodeState.LOCKED_IDLE;
        }
    }

    protected DangerNodeState calculateDangerNodeState() {
        if (chapterLevels.size() >= 3) {
            StageStatus s2 = statusOf(chapterLevels.get(1));
            StageStatus s3 = statusOf(chapterLevels.get(2));
            if (s3 != StageStatus.LOCKED) {
                return DangerNodeState.UNLOCKED_IDLE;
            } else if (s2 == StageStatus.COMPLETED) {
                return DangerNodeState.UNLOCKED_ANIMATION;
            }
        }
        return DangerNodeState.LOCKED_IDLE;
    }

    protected Drawable getTextureDrawable(String path, int w, int h, Color fill) {
        if (Gdx.files.internal(path).exists()) {
            return new TextureRegionDrawable(loadTextureSafe(path));
        }
        return circleDrawable(Math.min(w, h), fill, Color.WHITE, 2);
    }

    protected Drawable circleDrawable(int diameter, Color fill, Color border, int borderWidth) {
        Pixmap pixmap = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
        pixmap.setColor(border);
        pixmap.fillCircle(diameter / 2, diameter / 2, diameter / 2);
        pixmap.setColor(fill);
        pixmap.fillCircle(diameter / 2, diameter / 2, diameter / 2 - borderWidth);
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
    }

    protected ImageButton createIconButton(String path, float width, float height, Runnable action) {
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

    protected Actor createIconButtonWithLabel(String path, float width, float height, String text, Runnable action) {
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
}
