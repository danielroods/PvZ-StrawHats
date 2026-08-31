package view.screens.ui_menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import controller.QuestManager;
import model.quests.GameQuest;
import model.quests.QuestLoader;
import model.quests.QuestReward;
import model.user_data.User;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.UiScreen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TravelLogScreen extends UiScreen {

    private enum Category { DAILY, MAIN, EPIC, ALL, MINIGAMES }

    private static final float CARD_WIDTH = 820f;
    private static final float BAR_WIDTH = 420f;
    private static final float BAR_HEIGHT = 22f;

    private static final String[] MINIGAME_KEYS = {
            "vasebreaker", "wallnut-bowling", "i-zombie", "beghouled", "zombotany"
    };
    private static final String[] MINIGAME_LABELS = {
            "Vasebreaker", "Wallnut Bowling", "I, Zombie", "Beghouled", "Zombotany"
    };
    private static final String[] MINIGAME_ICONS = {
            "assets/images/ui/travel_log/zvase.jpg",
            "assets/images/ui/travel_log/zwallnutb.jpg",
            "assets/images/ui/travel_log/zizombie.jpg",
            "assets/images/ui/travel_log/zbegho.jpg",
            "assets/images/ui/travel_log/zombotany.jpg"
    };

    private Category currentCategory = Category.DAILY;

    @Override
    public void show() {
        setBackground("assets/images/backg/mainmenu_background.png");
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
        super.show();
        build();
    }

    private void build() {
        rootTable.clear();

        rootTable.add(buildTopBar()).fillX().padTop(15).padLeft(20).padRight(20).row();
        rootTable.add(buildTabs()).padTop(SPACE_MD).row();

        Table content = currentCategory == Category.MINIGAMES
                ? buildMinigamesList()
                : buildQuestList(currentCategory);
        rootTable.add(scrollable(content)).expand().fill().width(CARD_WIDTH + 40).padTop(SPACE_SM).row();
    }

    private Table buildTopBar() {
        ImageButton backBtn = createIconButton("assets/images/ui/buttons_hud_back_normal.png", 54, 54,
                () -> runCommand("menu exit"));

        Table topLeft = new Table();
        topLeft.add(backBtn).left();
        topLeft.add(new Label("Travel Log", skin, "title")).padLeft(SPACE_MD);

        User user = User.currentUser;
        int coins = (user != null && user.userState != null) ? user.userState.coins : 0;
        int diamonds = (user != null && user.userState != null) ? user.userState.diamonds : 0;

        Table topRight = new Table();
        topRight.add(createResourceWidget("assets/images/ui/buttons_coin_buy_normal.png",
                String.valueOf(coins))).padRight(15);
        topRight.add(createResourceWidget("assets/images/ui/buttons_premium_normal.png",
                String.valueOf(diamonds)));

        Table topBar = new Table();
        topBar.add(topLeft).left().expandX();
        topBar.add(topRight).right();
        return topBar;
    }

    private Table buildTabs() {
        Table tabs = new Table();
        tabs.defaults().pad(0, 4, 0, 4);
        for (Category category : Category.values()) {
            tabs.add(tabButton(category)).width(150).height(46);
        }
        return tabs;
    }

    private TextButton tabButton(Category category) {
        Runnable select = () -> {
            currentCategory = category;
            build();
        };
        return category == currentCategory
                ? primaryButton(tabLabel(category), select)
                : secondaryButton(tabLabel(category), select);
    }

    private String tabLabel(Category category) {
        return switch (category) {
            case DAILY -> "Daily";
            case MAIN -> "Main";
            case EPIC -> "Epic";
            case ALL -> "All";
            case MINIGAMES -> "Minigames";
        };
    }

    private Table buildQuestList(Category category) {
        Table list = new Table();
        list.top();

        List<GameQuest> quests = questsForCategory(category);
        if (quests.isEmpty()) {
            list.add(createLabel("No quests here right now.", "muted")).pad(10).row();
            return list;
        }

        for (GameQuest quest : quests) {
            list.add(buildQuestCard(quest)).width(CARD_WIDTH).padBottom(8).row();
        }
        return list;
    }

    private List<GameQuest> questsForCategory(Category category) {
        List<GameQuest> quests = new ArrayList<>();
        for (GameQuest quest : QuestLoader.getAllQuests()) {
            boolean matches = category == Category.ALL
                    || (quest.getType() != null && quest.getType().equalsIgnoreCase(category.name()));
            if (matches) {
                quests.add(quest);
            }
        }
        quests.sort(Comparator.comparingInt(this::priorityRank));
        return quests;
    }

    private int priorityRank(GameQuest quest) {
        String priority = quest.getPriority();
        if (priority == null) return 4;
        return switch (priority.toUpperCase()) {
            case "CRITICAL" -> 0;
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 4;
        };
    }

    private Table buildQuestCard(GameQuest quest) {
        Table card = new Table();
        card.setBackground(skin.getDrawable("card-background"));
        card.pad(CARD_PAD);

        Table header = new Table();
        header.add(new Label(quest.getTitle(), skin, "title")).left().expandX();
        header.add(priorityBadge(quest.getPriority())).right();
        card.add(header).fillX().row();

        String description = quest.getQuestDescription() == null ? "" : quest.getQuestDescription();
        Label desc = new Label(description, skin, "muted");
        desc.setWrap(true);
        card.add(desc).width(CARD_WIDTH - 2 * CARD_PAD).left().padTop(4).row();

        int target = QuestManager.getDisplayTarget(quest);
        int progress = Math.min(quest.getProgress(), target);

        Table progressRow = new Table();
        progressRow.add(progressBar(progress, target)).size(BAR_WIDTH, BAR_HEIGHT).left();
        progressRow.add(createLabel(progress + " / " + target, "main")).padLeft(10);
        card.add(progressRow).left().padTop(8).row();

        Table footer = new Table();
        footer.add(rewardLabel(quest)).left().expandX();
        footer.add(statusWidget(quest)).right();
        card.add(footer).fillX().padTop(8).row();

        return card;
    }

    private Label priorityBadge(String priority) {
        Color color = switch (priority == null ? "" : priority.toUpperCase()) {
            case "CRITICAL" -> Color.SCARLET;
            case "HIGH" -> Color.ORANGE;
            case "MEDIUM" -> Color.GOLD;
            default -> Color.LIGHT_GRAY;
        };
        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("default-font"), color);
        Label badge = new Label(priority == null ? "LOW" : priority, style);
        badge.setFontScale(0.8f);
        return badge;
    }

    private Actor progressBar(int progress, int target) {
        Stack stack = new Stack();
        stack.add(new Image(solidColorDrawable(new Color(0f, 0f, 0f, 0.45f))));

        float ratio = target <= 0 ? 0f : Math.max(0f, Math.min(1f, progress / (float) target));
        Table fillRow = new Table();
        fillRow.left();
        fillRow.add(new Image(skin.getDrawable("button-down"))).width(Math.max(4f, BAR_WIDTH * ratio))
                .height(BAR_HEIGHT);
        stack.add(fillRow);

        return stack;
    }

    private Label rewardLabel(GameQuest quest) {
        QuestReward reward = quest.getReward();
        if (reward == null) {
            return createLabel("", "muted");
        }

        String rewardType = reward.getRewardType() == null ? "" : reward.getRewardType();
        String text = switch (rewardType) {
            case "COIN" -> "+" + reward.getAmount() + " Coins";
            case "GEM" -> "+" + reward.getAmount() + " Gems";
            case "SEED_PACK" -> "+" + reward.getAmount() + " Seed Packet" + (reward.getAmount() == 1 ? "" : "s");
            default -> "Reward";
        };

        String formula = reward.getFormula();
        if (formula != null && !formula.isEmpty()) {
            text += formula.equals("random_new_plant") ? " (random plant)" : " (scales)";
        }

        Color color = switch (rewardType) {
            case "COIN" -> Color.GOLD;
            case "GEM" -> Color.SKY;
            case "SEED_PACK" -> Color.LIME;
            default -> Color.WHITE;
        };
        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("default-font"), color);
        Label label = new Label(text, style);
        label.setFontScale(0.85f);
        return label;
    }

    private Actor statusWidget(GameQuest quest) {
        if (!quest.isCompleted()) {
            return createLabel("In Progress", "muted");
        }
        if (!quest.isRewardCollected()) {
            return primaryButton("Collect", () -> runCommand("travel log collect -q " + quest.getId()));
        }
        return createLabel("Claimed", "muted");
    }

    private Table buildMinigamesList() {
        Table list = new Table();
        list.top();
        for (int i = 0; i < MINIGAME_KEYS.length; i++) {
            list.add(buildMinigameCard(MINIGAME_KEYS[i], MINIGAME_LABELS[i], MINIGAME_ICONS[i]))
                    .width(CARD_WIDTH).height(135).padBottom(14).row();
        }
        return list;
    }

    private Table buildMinigameCard(String key, String displayName, String iconPath) {
        Table card = new Table();
        card.setBackground(loadRoundedTextureSafe(iconPath, 22));
        card.pad(CARD_PAD);

        Table info = new Table();
        info.add(createLabel("Choose a level to play.", "title")).right().padRight(160).row();
        info.add(buildLevelButtons(key)).right().padTop(10);

        card.add(info).expandX().right();
        return card;
    }

    private static final String LOCK_ICON = "assets/images/ui/collection/lock_small_gold.png";

    private Table buildLevelButtons(String key) {
        Table levels = new Table();
        var state = User.currentUser == null ? null : User.currentUser.userState;
        for (int level = 1; level <= 3; level++) {
            int chosenLevel = level;
            boolean unlocked = state == null || state.isMiniGameLevelUnlocked(key, level);

            TextButton button = secondaryButton("Level " + level,
                    () -> runCommand("travel log play -m " + key + " -l " + chosenLevel));
            if (!unlocked) {
                button.setDisabled(true);
                button.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            }

            Stack stack = new Stack();
            stack.add(button);
            if (!unlocked) {
                Image lockImage = new Image(loadTextureSafe(LOCK_ICON));
                Container<Image> lockContainer = new Container<>(lockImage);
                lockContainer.size(24f, 24f);
                lockContainer.center();
                lockContainer.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
                stack.add(lockContainer);
            }
            levels.add(stack).width(150).padRight(8);
        }
        return levels;
    }

    private Drawable solidColorDrawable(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
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



    private Drawable loadRoundedTextureSafe(String path, int cornerRadius) {
        if (path == null || path.isEmpty() || !Gdx.files.internal(path).exists()) {
            return solidColorDrawable(new Color(0f, 0f, 0f, 0f));
        }
        Pixmap src = new Pixmap(Gdx.files.internal(path));
        int w = src.getWidth();
        int h = src.getHeight();
        Pixmap dst = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        int r = Math.min(cornerRadius, Math.min(w, h) / 2);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (x < r && y < r) {
                    int dx = r - x;
                    int dy = r - y;
                    if (dx * dx + dy * dy > r * r) continue;
                }
                if (x >= w - r && y < r) {
                    int dx = x - (w - 1 - r);
                    int dy = r - y;
                    if (dx * dx + dy * dy > r * r) continue;
                }
                if (x < r && y >= h - r) {
                    int dx = r - x;
                    int dy = y - (h - 1 - r);
                    if (dx * dx + dy * dy > r * r) continue;
                }
                if (x >= w - r && y >= h - r) {
                    int dx = x - (w - 1 - r);
                    int dy = y - (h - 1 - r);
                    if (dx * dx + dy * dy > r * r) continue;
                }
                dst.drawPixel(x, y, src.getPixel(x, y));
            }
        }

        Texture tex = new Texture(dst);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        src.dispose();
        dst.dispose();
        return new TextureRegionDrawable(tex);
    }

    @Override
    protected void onAfterCommand() {
        build();
    }
}