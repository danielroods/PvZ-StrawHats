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

import model.match.main.levels.Level;
import model.user_data.User;
import model.utils.LevelLoader;
import service.resource_manager.AudioEnum;
import service.resource_manager.AudioManager;
import view.screens.generals.UiScreen;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LeaderboardScreen extends UiScreen {

    private enum SortColumn { RANK, USERNAME, SEASON, CHAPTER, STAGE, MINIGAMES, QUESTS, SCORE }

    private static final float ROW_WIDTH = 1150f;
    private static final float COL_RANK = 50f;
    private static final float COL_AVATAR = 44f;
    private static final float COL_NAME = 190f;
    private static final float COL_SEASON = 120f;
    private static final float COL_CHAPTER = 170f;
    private static final float COL_STAGE = 150f;
    private static final float COL_MINIGAMES = 100f;
    private static final float COL_QUESTS = 90f;
    private static final float COL_SCORE = 90f;

    private final SortState sort = new SortState();

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
        rootTable.add(buildSortBar()).padTop(SPACE_MD).row();
        rootTable.add(buildColumnHeader()).width(ROW_WIDTH).padTop(SPACE_SM).row();
        rootTable.add(scrollable(buildRows())).expand().fill().width(ROW_WIDTH + 30).padTop(SPACE_XS).row();
    }

    private Table buildTopBar() {
        ImageButton backBtn = createIconButton("assets/images/ui/buttons_hud_back_normal.png", 54, 54,
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
        bar.pad(10, 16, 10, 16);
        bar.defaults().pad(0, 4, 0, 4);

        bar.add(createLabel("Sort by:", "main")).padRight(SPACE_SM);
        for (SortColumn column : SortColumn.values()) {
            bar.add(sortChip(column)).width(112).height(38);
        }

        bar.add(createOrderToggleButton()).size(48, 38).padLeft(SPACE_LG);
        return bar;
    }

    private TextButton sortChip(SortColumn column) {
        return column == sort.column
                ? primaryButton(chipLabel(column), () -> changeSort(column))
                : secondaryButton(chipLabel(column), () -> changeSort(column));
    }

    private void changeSort(SortColumn column) {
        sort.column = column;
        build();
    }

    private String chipLabel(SortColumn column) {
        return switch (column) {
            case RANK -> "Rank";
            case USERNAME -> "Username";
            case SEASON -> "Season";
            case CHAPTER -> "Chapter";
            case STAGE -> "Stage";
            case MINIGAMES -> "Minigames";
            case QUESTS -> "Quests";
            case SCORE -> "Score";
        };
    }

    private Table buildColumnHeader() {
        Table header = new Table();
        header.pad(0, 16, 4, 16);
        header.defaults().left().padRight(8);

        header.add(createLabel("#", "muted")).width(COL_RANK);
        header.add(createLabel("", "muted")).width(COL_AVATAR);
        header.add(createLabel("Player", "muted")).width(COL_NAME);
        header.add(createLabel("Season", "muted")).width(COL_SEASON);
        header.add(createLabel("Chapter", "muted")).width(COL_CHAPTER);
        header.add(createLabel("Stage", "muted")).width(COL_STAGE);
        header.add(createLabel("Minigames", "muted")).width(COL_MINIGAMES);
        header.add(createLabel("Quests", "muted")).width(COL_QUESTS);
        header.add(createLabel("Score", "muted")).width(COL_SCORE);
        return header;
    }

    private Table buildRows() {
        Table list = new Table();
        list.top();

        List<Level> allLevels;
        try {
            allLevels = LevelLoader.loadLevels();
        } catch (Exception e) {
            list.add(createLabel("Could not load leaderboard data.", "muted")).pad(30);
            return list;
        }

        List<LeaderboardRow> rows = User.users.stream()
                .map(u -> LeaderboardRow.of(u, allLevels))
                .collect(Collectors.toList());

        if (rows.isEmpty()) {
            list.add(createLabel("No records yet.", "muted")).pad(30);
            return list;
        }

        sortRows(rows);

        int rank = 1;
        for (LeaderboardRow row : sort.ascending? rows : rows.reversed())
            list.add(buildRow(rank++, row)).width(ROW_WIDTH).padBottom(10).row();

        return list;
    }

    private void sortRows(List<LeaderboardRow> rows) {
        Comparator<LeaderboardRow> comparator = switch (sort.column) {
            case RANK -> null;
            case USERNAME -> Comparator.comparing((LeaderboardRow r) -> r.user.username,
                    String.CASE_INSENSITIVE_ORDER);
            case SEASON -> Comparator.comparing((LeaderboardRow r) -> r.season, String.CASE_INSENSITIVE_ORDER);
            case CHAPTER -> Comparator.comparing((LeaderboardRow r) -> r.chapter, String.CASE_INSENSITIVE_ORDER);
            case STAGE -> Comparator.comparingInt((LeaderboardRow r) -> r.stageLevelId);
            case MINIGAMES -> Comparator.comparingInt((LeaderboardRow r) -> r.miniGamesWon);
            case QUESTS -> Comparator.comparingInt((LeaderboardRow r) -> r.questsCompleted);
            case SCORE -> Comparator.comparingInt((LeaderboardRow r) -> r.highScore);
        };
        if (comparator == null) return;
        rows.sort(comparator);

    }

    private Table buildRow(int rank, LeaderboardRow row) {
        boolean isYou = User.currentUser != null && User.currentUser.username.equals(row.user.username);

        Table card = new Table();
        card.setBackground(isYou ? highlightRowDrawable() : skin.getDrawable("card-background"));
        card.pad(8, 16, 8, 16);
        card.defaults().left().padRight(8);

        card.add(rankBadge(rank)).width(COL_RANK);
        card.add(avatarStack(row.user)).size(COL_AVATAR);
        card.add(nameColumn(row.user)).width(COL_NAME);
        card.add(createLabel(row.season, "main")).width(COL_SEASON);
        card.add(createLabel(row.chapter, "main")).width(COL_CHAPTER);
        card.add(createLabel(row.stage, "main")).width(COL_STAGE);
        card.add(createLabel(String.valueOf(row.miniGamesWon), "main")).width(COL_MINIGAMES);
        card.add(createLabel(String.valueOf(row.questsCompleted), "main")).width(COL_QUESTS);
        card.add(scoreLabel(row.highScore)).width(COL_SCORE);

        return card;
    }

    private Actor rankBadge(int rank) {
        Color medalColor = switch (rank) {
            case 1 -> new Color(1f, 0.84f, 0f, 1f);
            case 2 -> new Color(0.80f, 0.80f, 0.82f, 1f);
            case 3 -> new Color(0.80f, 0.50f, 0.20f, 1f);
            default -> null;
        };

        if (medalColor == null) {
            return createLabel(String.valueOf(rank), "muted");
        }

        Stack badge = new Stack();
        Table circleWrap = new Table();
        circleWrap.add(new Image(circleDrawable(medalColor, 34))).size(34, 34);

        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("default-font"), Color.BLACK);
        Label number = new Label(String.valueOf(rank), style);
        number.setFontScale(0.8f);
        Table numberWrap = new Table();
        numberWrap.add(number).center();

        badge.add(circleWrap);
        badge.add(numberWrap);
        return badge;
    }

    private Actor avatarStack(User user) {
        Stack stack = new Stack();
        Image frame = new Image(loadTextureSafe("assets/images/ui/reward4_bg.png"));

        String avatarPath = (user.profilePicture != null && !user.profilePicture.isEmpty())
                ? user.profilePicture
                : "assets/images/ui/avatar_luffy.png";

        Table avatarWrap = new Table();
        avatarWrap.add(new Image(loadTextureSafe(avatarPath))).size(COL_AVATAR - 8);

        stack.add(frame);
        stack.add(avatarWrap);
        return stack;
    }

    private Table nameColumn(User user) {
        Table col = new Table();
        col.add(new Label(user.username, skin, "title")).left().row();

        if (user.nickname != null && !user.nickname.isEmpty() && !user.nickname.equals(user.username)) {
            Label nick = createLabel(user.nickname, "muted");
            nick.setFontScale(0.75f);
            col.add(nick).left();
        }
        return col;
    }

    private Label scoreLabel(int highScore) {
        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("default-font"), Color.GOLD);
        return new Label(String.valueOf(highScore), style);
    }

    private Drawable circleDrawable(Color color, int diameter) {
        Pixmap pixmap = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setColor(color);
        pixmap.fillCircle(diameter / 2, diameter / 2, diameter / 2);
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
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

    


    private Actor createOrderToggleButton() {
        String iconPath = sort.ascending
                ? "assets/images/ui/leaderboard/sort_ascending_down.png"
                : "assets/images/ui/leaderboard/sort_descending_down.png";

        Texture tex = loadTextureSafe(iconPath);
        if (tex == null || !Gdx.files.internal(iconPath).exists()) {
            return secondaryButton(sort.ascending ? "Asc" : "Desc", this::toggleOrder);
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
        private boolean ascending = false;
    }

    @Override
    protected void onAfterCommand() {
        build();
    }



    private record LeaderboardRow(User user, String season, String chapter, String stage, int stageLevelId,
                                  int miniGamesWon, int questsCompleted, int highScore) {

        static LeaderboardRow of(User user, List<Level> allLevels) {
            Level level = null;
            if (user.userState.lastLevel > 0) {
                level = allLevels.stream()
                        .filter(l -> l.getId() == user.userState.lastLevel)
                        .findFirst()
                        .orElse(null);
            }

            String season = "-";
            String chapter = "-";
            String stage = "-";
            int stageLevelId = -1;

            if (level != null) {
                season = capitalize(level.getSeason().getName());
                stageLevelId = level.getId();
                String name = level.getName();
                int idx = name.indexOf(" - ");
                if (idx >= 0) {
                    chapter = name.substring(0, idx);
                    stage = name.substring(idx + 3);
                } else {
                    chapter = name;
                    stage = name;
                }
            }

            return new LeaderboardRow(user, season, chapter, stage, stageLevelId,
                    user.userState.miniGamesWon, user.userState.questsCompleted, user.userState.highScore);
        }

        private static String capitalize(String s) {
            if (s == null || s.isEmpty()) return s;
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }
}