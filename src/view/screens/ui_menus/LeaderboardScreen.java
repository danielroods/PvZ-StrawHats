package view.screens.ui_menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

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

    private static final float ROW_WIDTH = 1190f;
    private static final float COL_PAD = 16f;
    private static final float COL_GAP = 6f;
    private static final float COL_AVATAR = 40f;
    private static final float COL_NAME = 138f;
    private static final float COL_CHAPTER = 158f;
    private static final float COL_MINIGAMES = 82f;
    private static final float COL_QUESTS = 72f;
    private static final float COL_SCORE = 92f;
    private static final float COL_MEOW = 130f;

    /** One row of nine tabs plus the order toggle, inside the same 1230. */
    private static final float CHIP_WIDTH = 112f;
    private static final float CHIP_HEIGHT = 38f;
    private static final float CHIP_FONT_SCALE = 0.68f;
    private static final float HEADER_FONT_SCALE = 0.72f;
    private static final float VALUE_FONT_SCALE = 0.8f;
    private static final float NAME_FONT_SCALE = 0.85f;
    private static final float NICKNAME_FONT_SCALE = 0.65f;

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
        rootTable.add(buildColumnHeader()).width(ROW_WIDTH).padTop(SPACE_SM).row();
        rootTable.add(scrollable(buildRows())).expand().fill()
                .width(ROW_WIDTH + 30).padTop(SPACE_XS).row();
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

        bar.add(headerLabel("Sort by:")).padRight(SPACE_XS);
        for (SortColumn column : SortColumn.values()) {
            bar.add(sortChip(column)).width(CHIP_WIDTH).height(CHIP_HEIGHT);
        }
        for (EndlessChapter chapter : EndlessChapter.values()) {
            bar.add(meowChip(chapter)).width(CHIP_WIDTH).height(CHIP_HEIGHT);
        }
        bar.add(createOrderToggleButton()).size(44, CHIP_HEIGHT).padLeft(SPACE_MD);
        return bar;
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

    private static String meowColumnHeader(EndlessChapter chapter) {
        return chapter.seasonName() + "\nMeow";
    }

    private Table buildColumnHeader() {
        Table header = new Table();
        header.pad(0, COL_PAD, 4, COL_PAD);
        header.defaults().left().padRight(COL_GAP);

        header.add(headerLabel("")).width(COL_AVATAR);
        header.add(headerLabel("Player")).width(COL_NAME);
        header.add(headerLabel("Chapter")).width(COL_CHAPTER);
        header.add(headerLabel("Minigames")).width(COL_MINIGAMES);
        header.add(headerLabel("Quests")).width(COL_QUESTS);
        header.add(headerLabel("My Point")).width(COL_SCORE);
        for (EndlessChapter chapter : EndlessChapter.values()) {
            header.add(headerLabel(meowColumnHeader(chapter))).width(COL_MEOW);
        }
        return header;
    }

    private Label headerLabel(String text) {
        Label label = createLabel(text, "muted");
        label.setFontScale(HEADER_FONT_SCALE);
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
        for (LeaderboardRowDto row : rows) {
            list.add(buildRow(row)).width(ROW_WIDTH).padBottom(10).row();
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

    private Table buildRow(LeaderboardRowDto row) {
        boolean isYou = User.currentUser != null
                && User.currentUser.username.equals(row.username);

        Table card = new Table();
        card.setBackground(isYou ? highlightRowDrawable() : skin.getDrawable("card-background"));
        card.pad(8, COL_PAD, 8, COL_PAD);
        card.defaults().left().padRight(COL_GAP);

        card.add(avatarStack(row)).size(COL_AVATAR);
        card.add(nameColumn(row)).width(COL_NAME);
        card.add(valueLabel(chapterText(row))).width(COL_CHAPTER);
        card.add(valueLabel(String.valueOf(row.miniGamesWon))).width(COL_MINIGAMES);
        card.add(valueLabel(String.valueOf(row.questsCompleted))).width(COL_QUESTS);
        card.add(scoreLabel(row.myPointOrZero())).width(COL_SCORE);
        for (EndlessChapter chapter : EndlessChapter.values()) {
            card.add(scoreLabel(row.lotteryScoreOrZero(chapter.key()))).width(COL_MEOW);
        }
        return card;
    }

    private static String chapterText(LeaderboardRowDto row) {
        if (row.chapter == null || row.chapter.isEmpty() || "-".equals(row.chapter)) return "-";
        if (row.chapterStageCount <= 0) return row.chapter;
        return row.chapter + "  " + row.chapterStagesCleared + "/" + row.chapterStageCount;
    }

    private Actor avatarStack(LeaderboardRowDto row) {
        Stack stack = new Stack();
        Image frame = new Image(loadTextureSafe("assets/images/ui/reward4_bg.png"));

        String avatarPath = (row.profilePicture != null && !row.profilePicture.isEmpty())
                ? row.profilePicture
                : "assets/images/ui/avatar_luffy.png";

        Table avatarWrap = new Table();
        avatarWrap.add(new Image(loadTextureSafe(avatarPath))).size(COL_AVATAR - 8);

        stack.add(frame);
        stack.add(avatarWrap);
        return stack;
    }

    private Table nameColumn(LeaderboardRowDto row) {
        Table col = new Table();
        Label name = new Label(row.usernameOrEmpty(), skin, "title");
        name.setFontScale(NAME_FONT_SCALE);
        name.setEllipsis(true);
        col.add(name).width(COL_NAME).left().row();

        if (row.nickname != null && !row.nickname.isEmpty() && !row.nickname.equals(row.username)) {
            Label nick = createLabel(row.nickname, "muted");
            nick.setFontScale(NICKNAME_FONT_SCALE);
            nick.setEllipsis(true);
            col.add(nick).width(COL_NAME).left();
        }
        return col;
    }

    private Label valueLabel(String text) {
        Label label = createLabel(text, "main");
        label.setFontScale(VALUE_FONT_SCALE);
        label.setEllipsis(true);
        return label;
    }

    private Label scoreLabel(long score) {
        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("default-font"),
                score > 0 ? Color.GOLD : Color.GRAY);
        Label label = new Label(String.valueOf(score), style);
        label.setFontScale(VALUE_FONT_SCALE);
        return label;
    }

    private Drawable highlightRowDrawable() {
        int width = 1050;
        int height = 60;
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
