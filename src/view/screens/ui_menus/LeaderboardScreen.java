package view.screens.ui_menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.math.Rectangle;

import model.match.endless.EndlessChapter;
import model.match.main.levels.Level;
import model.user_data.User;
import model.utils.LevelLoader;
import net.client.NetworkClient;
import net.dto.LeaderboardRowDto;
import net.dto.LeaderboardRows;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.UiScreen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LeaderboardScreen extends UiScreen {

    private enum SortColumn { USERNAME, CHAPTER, MINIGAMES, QUESTS, SCORE }

    private static final String ICON_BASE = "assets/images/ui/leaderboard/";
    private static final String ICON_USERNAME = ICON_BASE + "QuestIcons_rent_a_plant.png";
    private static final String ICON_CHAPTER = ICON_BASE + "QuestIcons_Rift.png";
    private static final String ICON_MINIGAMES = ICON_BASE + "QuestIcons_LOTD.png";
    private static final String ICON_QUESTS = ICON_BASE + "buttons_hud_quests_selected.png";
    private static final String ICON_SCORE = ICON_BASE + "QuestIcons_Arena.png";

    private static final String[] RANK_CUPS = {
            ICON_BASE + "cup_crystal.png",
            ICON_BASE + "cup_jade.png",
            ICON_BASE + "cup_gold.png",
            ICON_BASE + "cup_silver.png",
            ICON_BASE + "cup_bronze.png",
            ICON_BASE + "cup_brick.png",
            ICON_BASE + "cup_wood.png"
    };

    private static final float ROW_WIDTH = 900f;
    private static final float ROW_HEIGHT = 92f;
    private static final float AVATAR_SIZE = 68f;

    private static final float CHIP_WIDTH = 112f;
    private static final float CHIP_HEIGHT = 38f;
    private static final float CHIP_FONT_SCALE = 0.68f;
    private static final float NAME_FONT_SCALE = 1f;
    private static final float NICKNAME_FONT_SCALE = 0.65f;
    private static final float METRIC_LABEL_SCALE = 0.62f;
    private static final float METRIC_VALUE_SCALE = 1.15f;
    private static final float RANK_FONT_SCALE = 0.85f;

    private final SortState sort = new SortState();

    @Override
    public void show() {
        setBackground("assets/images/backg/mainmenu_background.png");
        AudioManager.get().playMusic(AudioEnum.MENU_MUSIC, true);
        super.show();
        build();
        if (NetworkClient.get().isSignedIn()) {
            NetworkClient.get().requestLeaderboard(envelope -> build());
        }
    }

    private void build() {
        rootTable.clear();

        rootTable.add(buildTopBar()).fillX().padTop(15).padLeft(20).padRight(20).row();
        rootTable.add(buildSortBar()).padTop(SPACE_MD).row();
        rootTable.add(scrollable(buildRows())).expand().fill()
                .width(ROW_WIDTH + 30).padTop(SPACE_SM).row();
    }

    private Table buildTopBar() {
        ImageButton backBtn = createIconButton(
                "assets/images/ui/buttons_hud_back_normal.png", 54, 54,
                () -> runCommand("menu exit"));

        Table topLeft = new Table();
        topLeft.add(backBtn).left();
        topLeft.add(new Label("Leaderboard", skin, "title")).padLeft(SPACE_MD);

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

    private Table buildSortBar() {
        Table bar = new Table();
        bar.setBackground(skin.getDrawable("card-background"));
        bar.pad(8, 10, 8, 10);
        bar.defaults().pad(0, 3, 0, 3);

        bar.add(headerLabel("Show:")).padRight(SPACE_XS);
        for (SortColumn column : SortColumn.values()) {
            bar.add(sortChip(column)).width(CHIP_WIDTH).height(CHIP_HEIGHT);
        }
        for (EndlessChapter chapter : EndlessChapter.values()) {
            bar.add(meowChip(chapter)).width(CHIP_WIDTH).height(CHIP_HEIGHT);
        }
        bar.add(createOrderToggleButton()).size(44, CHIP_HEIGHT).padLeft(SPACE_MD);

        ScrollPane scroll = new ScrollPane(bar);
        scroll.setScrollingDisabled(false, true);
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        return wrapInTable(scroll);
    }

    private Table wrapInTable(ScrollPane scroll) {
        Table wrapper = new Table();
        wrapper.add(scroll).width(ROW_WIDTH);
        return wrapper;
    }

    private TextButton sortChip(SortColumn column) {
        boolean active = sort.meowChapter == null && column == sort.column;
        return chip(chipLabel(column), active, () -> changeSort(column));
    }

    private TextButton meowChip(EndlessChapter chapter) {
        return chip(shortChapter(chapter) + " Meow", sort.meowChapter == chapter,
                () -> changeMeowSort(chapter));
    }

    private TextButton chip(String label, boolean active, Runnable action) {
        TextButton button = active ? primaryButton(label, action) : secondaryButton(label, action);
        button.getLabel().setFontScale(CHIP_FONT_SCALE);
        button.getLabel().setEllipsis(true);
        button.getLabelCell().width(CHIP_WIDTH - 8f);
        return button;
    }

    private void changeSort(SortColumn column) {
        sort.column = column;
        sort.meowChapter = null;
        build();
    }

    private void changeMeowSort(EndlessChapter chapter) {
        sort.meowChapter = chapter;
        build();
    }

    private String chipLabel(SortColumn column) {
        return switch (column) {
            case USERNAME -> "Username";
            case CHAPTER -> "Chapter";
            case MINIGAMES -> "Minigames";
            case QUESTS -> "Quests";
            case SCORE -> "My Point";
        };
    }

    private static String shortChapter(EndlessChapter chapter) {
        return switch (chapter) {
            case EGYPT -> "Egypt";
            case FROSTBITE_CAVES -> "Frostbite";
            case BIG_WAVE_BEACH -> "Beach";
            case DARK_AGES -> "Dark Ages";
            case PIRATES -> "pirates";
            case FUTURE -> "Future";
        };
    }

    private Label headerLabel(String text) {
        Label label = createLabel(text, "muted");
        label.setFontScale(0.72f);
        return label;
    }

    private Table buildRows() {
        Table list = new Table();
        list.top();

        List<LeaderboardRowDto> rows = loadRows();
        if (rows == null) {
            list.add(createLabel("Could not load leaderboard data.", "muted")).pad(30);
            return list;
        }
        if (rows.isEmpty()) {
            list.add(createLabel("No records yet.", "muted")).pad(30);
            return list;
        }

        sortRows(rows);
        int rank = 1;
        for (LeaderboardRowDto row : rows) {
            list.add(buildRow(row, rank)).width(ROW_WIDTH).height(ROW_HEIGHT).padBottom(12).row();
            rank++;
        }
        return list;
    }

    private List<LeaderboardRowDto> loadRows() {
        List<LeaderboardRowDto> online = NetworkClient.get().getLeaderboardRows();
        if (NetworkClient.get().isSignedIn() && online != null) {
            return new ArrayList<>(online);
        }
        List<Level> allLevels;
        try {
            allLevels = LevelLoader.loadLevels();
        } catch (Exception e) {
            return null;
        }
        return User.users.stream()
                .map(user -> LeaderboardRows.of(user, allLevels))
                .collect(Collectors.toList());
    }

    private void sortRows(List<LeaderboardRowDto> rows) {
        Comparator<LeaderboardRowDto> byColumn = columnComparator();
        Comparator<LeaderboardRowDto> ordered = sort.ascending ? byColumn : byColumn.reversed();
        rows.sort(ordered.thenComparing(LeaderboardRowDto::usernameOrEmpty,
                String.CASE_INSENSITIVE_ORDER));
    }

    private Comparator<LeaderboardRowDto> columnComparator() {
        if (sort.meowChapter != null) {
            String key = sort.meowChapter.key();
            return Comparator.comparingLong(row -> row.lotteryScoreOrZero(key));
        }
        return switch (sort.column) {
            case USERNAME -> Comparator.comparing(LeaderboardRowDto::usernameOrEmpty,
                    String.CASE_INSENSITIVE_ORDER);
            case CHAPTER -> Comparator.comparingInt((LeaderboardRowDto r) -> r.levelsCleared);
            case MINIGAMES -> Comparator.comparingInt((LeaderboardRowDto r) -> r.miniGamesWon);
            case QUESTS -> Comparator.comparingInt((LeaderboardRowDto r) -> r.questsCompleted);
            case SCORE -> Comparator.comparingLong(LeaderboardRowDto::myPointOrZero);
        };
    }

    /** The metric currently chosen in the sort/scroll bar above the list. */
    private long selectedMetricValue(LeaderboardRowDto row) {
        if (sort.meowChapter != null) return row.lotteryScoreOrZero(sort.meowChapter.key());
        return switch (sort.column) {
            case USERNAME -> row.myPointOrZero();
            case CHAPTER -> row.levelsCleared;
            case MINIGAMES -> row.miniGamesWon;
            case QUESTS -> row.questsCompleted;
            case SCORE -> row.myPointOrZero();
        };
    }

    private String selectedMetricLabel() {
        if (sort.meowChapter != null) return shortChapter(sort.meowChapter) + " Meow";
        return switch (sort.column) {
            case USERNAME -> "My Point";
            case CHAPTER -> "Levels";
            case MINIGAMES -> "Minigames";
            case QUESTS -> "Quests";
            case SCORE -> "My Point";
        };
    }

    private String selectedMetricIcon() {
        if (sort.meowChapter != null) return meowIconPath(sort.meowChapter);
        return switch (sort.column) {
            case USERNAME -> ICON_USERNAME;
            case CHAPTER -> ICON_CHAPTER;
            case MINIGAMES -> ICON_MINIGAMES;
            case QUESTS -> ICON_QUESTS;
            case SCORE -> ICON_SCORE;
        };
    }

    private static String meowIconPath(EndlessChapter chapter) {
        return switch (chapter) {
            case BIG_WAVE_BEACH -> ICON_BASE + "QuestIcons_BigWaveBeach.png";
            case DARK_AGES -> ICON_BASE + "QuestIcons_DarkAges.png";
            case EGYPT -> ICON_BASE + "QuestIcons_Egypt.png";
            case FUTURE -> ICON_BASE + "QuestIcons_FarFuture.png";
            case FROSTBITE_CAVES -> ICON_BASE + "QuestIcons_FrostbiteCaves.png";
            case PIRATES -> ICON_BASE + "QuestIcons_Pirate.png";
        };
    }

    /**
     * Builds one player card: avatar + username on the left, a glowing badge for the
     * currently selected metric on the right, sitting on a card background with a
     * looping diagonal shine sweep for a bit of "alive" polish.
     */
    private Stack buildRow(LeaderboardRowDto row, int rank) {
        boolean isYou = User.currentUser != null
                && User.currentUser.username.equals(row.username);

        Stack cardStack = new Stack();

        Table card = new Table();
        // Reuse the same rounded UI language as the leaderboard scroll area instead of
        // a flat rectangle, so the rows feel like part of the same cartoon panel.
        card.setBackground(skin.getDrawable("card-background"));
        card.setColor(isYou ? new Color(0.82f, 0.90f, 1f, 1f) : Color.WHITE);
        card.pad(8, 18, 8, 18);
        card.defaults().left();

        card.add(rankLabel(rank)).width(40).padRight(6);
        card.add(avatarStack(row)).size(AVATAR_SIZE).padRight(14);
        card.add(nameColumn(row)).expandX().left();
        card.add(metricBadge(row)).width(150).right();

        cardStack.add(card);
        cardStack.add(shineOverlay(isYou));
        cardStack.setTransform(false);
        return cardStack;
    }

    private Actor rankLabel(int rank) {
        Stack rankStack = new Stack();
        String cupPath = RANK_CUPS[Math.min(rank - 1, RANK_CUPS.length - 1)];
        Image cup = new Image(loadTextureSafe(cupPath));
        cup.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        rankStack.add(cup);

        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("default-font"), Color.WHITE);
        Label number = new Label(String.valueOf(rank), style);
        number.setFontScale(RANK_FONT_SCALE);
        number.setAlignment(Align.center);
        rankStack.add(number);
        return rankStack;
    }

    private Actor avatarStack(LeaderboardRowDto row) {
        Stack stack = new Stack();
        Image frame = new Image(loadTextureSafe("assets/images/ui/reward4_bg.png"));

        String avatarPath = (row.profilePicture != null && !row.profilePicture.isEmpty())
                ? row.profilePicture
                : "assets/images/ui/avatar_luffy.png";

        Table avatarWrap = new Table();
        avatarWrap.add(new Image(loadTextureSafe(avatarPath))).size(AVATAR_SIZE - 10);

        stack.add(frame);
        stack.add(avatarWrap);
        return stack;
    }

    private Table nameColumn(LeaderboardRowDto row) {
        Table col = new Table();
        Label name = new Label(row.usernameOrEmpty(), skin, "title");
        name.setFontScale(NAME_FONT_SCALE);
        name.setEllipsis(true);
        col.add(name).width(320).left().row();

        if (row.nickname != null && !row.nickname.isEmpty() && !row.nickname.equals(row.username)) {
            Label nick = createLabel(row.nickname, "muted");
            nick.setFontScale(NICKNAME_FONT_SCALE);
            nick.setEllipsis(true);
            col.add(nick).width(320).left();
        }
        return col;
    }

    /** Icon + value badge for whichever metric is selected in the sort bar. */
    private Table metricBadge(LeaderboardRowDto row) {
        long value = selectedMetricValue(row);

        Table badge = new Table();
        badge.setBackground(badgeDrawable());
        badge.pad(4, 10, 4, 10);

        Image icon = new Image(loadTextureSafe(selectedMetricIcon()));

        Table textCol = new Table();
        Label.LabelStyle metricStyle = new Label.LabelStyle(skin.getFont("default-font"), Color.WHITE);
        Label metricName = new Label(selectedMetricLabel().toUpperCase(), metricStyle);
        metricName.setFontScale(METRIC_LABEL_SCALE);
        metricName.setEllipsis(true);
        textCol.add(metricName).left().row();
        textCol.add(scoreLabel(value)).left();

        badge.add(icon).size(30).padRight(8);
        badge.add(textCol).left();
        return badge;
    }

    private Label scoreLabel(long score) {
        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("default-font"),
                score > 0 ? Color.WHITE : new Color(0.82f, 0.86f, 0.92f, 1f));
        Label label = new Label(String.valueOf(score), style);
        label.setFontScale(METRIC_VALUE_SCALE);
        return label;
    }

    private Drawable cardDrawable() {
        int width = 12;
        int height = 12;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.13f, 0.14f, 0.2f, 0.88f));
        pixmap.fill();
        pixmap.setColor(new Color(1f, 1f, 1f, 0.10f));
        pixmap.drawRectangle(0, 0, width, height);
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
    }

    private Drawable badgeDrawable() {
        int width = 12;
        int height = 12;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.42f, 0.58f, 0.74f, 0.34f));
        pixmap.fill();
        pixmap.setColor(new Color(0.86f, 0.94f, 1f, 0.80f));
        pixmap.drawRectangle(0, 0, width, height);
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
    }

    private Drawable highlightRowDrawable() {
        int width = 12;
        int height = 12;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.55f, 0.42f, 0.08f, 0.92f));
        pixmap.fill();
        pixmap.setColor(new Color(1f, 0.84f, 0.2f, 0.95f));
        pixmap.drawRectangle(0, 0, width, height);
        pixmap.drawRectangle(1, 1, width - 2, height - 2);
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
    }

    /**
     * A thin, diagonal, semi-transparent beam that loops across the card from left to
     * right on a delay, clipped to the card's own bounds - a cheap "shiny card" effect
     * with no extra art assets required.
     */
    private ShineOverlay shineOverlay(boolean brighter) {
        Pixmap pixmap = new Pixmap(24, 4, Pixmap.Format.RGBA8888);
        for (int x = 0; x < 24; x++) {
            float t = Math.abs(x - 12) / 12f;
            float alpha = (1f - t) * (brighter ? 0.55f : 0.35f);
            pixmap.setColor(1f, 1f, 1f, alpha);
            pixmap.drawLine(x, 0, x, 3);
        }
        Texture beamTex = new Texture(pixmap);
        beamTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        Image beam = new Image(new TextureRegionDrawable(beamTex));
        beam.setRotation(20f);

        return new ShineOverlay(ROW_WIDTH, ROW_HEIGHT, beam);
    }

    /** Clips its child beam to the card bounds and loops it across on a delay. */
    private static class ShineOverlay extends Group {
        ShineOverlay(float width, float height, Image beam) {
            setSize(width, height);
            float beamWidth = 60f;
            beam.setSize(beamWidth, height * 2.2f);
            beam.setPosition(-beamWidth, -height * 0.6f);
            addActor(beam);

            beam.addAction(Actions.forever(Actions.sequence(
                    Actions.moveTo(-beamWidth, -height * 0.6f),
                    Actions.delay(1.1f),
                    Actions.moveTo(width + beamWidth, -height * 0.6f, 1.4f, Interpolation.sine),
                    Actions.delay(2.2f)
            )));
        }

        @Override
        public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float parentAlpha) {
            if (getStage() == null) {
                super.draw(batch, parentAlpha);
                return;
            }
            batch.flush();
            com.badlogic.gdx.math.Vector2 stageOrigin =
                    localToStageCoordinates(new com.badlogic.gdx.math.Vector2(0, 0));
            Rectangle stageBounds = new Rectangle(stageOrigin.x, stageOrigin.y, getWidth(), getHeight());
            Rectangle scissors = new Rectangle();
            ScissorStack.calculateScissors(getStage().getCamera(), batch.getTransformMatrix(),
                    stageBounds, scissors);
            if (ScissorStack.pushScissors(scissors)) {
                super.draw(batch, parentAlpha);
                batch.flush();
                ScissorStack.popScissors();
            }
        }
    }

    private Actor createOrderToggleButton() {
        String iconPath = sort.ascending
                ? "assets/images/ui/leaderboard/sort_ascending_down.png"
                : "assets/images/ui/leaderboard/sort_descending_down.png";

        Texture tex = loadTextureSafe(iconPath);
        if (tex == null || !Gdx.files.internal(iconPath).exists()) {
            TextButton fallback = secondaryButton(sort.ascending ? "Asc" : "Desc",
                    this::toggleOrder);
            fallback.getLabel().setFontScale(CHIP_FONT_SCALE);
            return fallback;
        }

        TextureRegionDrawable drawable = new TextureRegionDrawable(tex);
        ImageButton button = new ImageButton(drawable);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleOrder();
            }
        });
        return button;
    }

    private void toggleOrder() {
        sort.ascending = !sort.ascending;
        build();
    }

    private static class SortState {
        private SortColumn column = SortColumn.SCORE;
        private EndlessChapter meowChapter;
        private boolean ascending = false;
    }

    @Override
    protected void onAfterCommand() {
        build();
    }
}