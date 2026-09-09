package controller.ui_menus;

import model.App;
import model.Regex;
import model.match.endless.EndlessChapter;
import model.match.main.levels.Level;
import model.user_data.User;
import model.utils.LevelLoader;
import net.dto.LeaderboardRowDto;
import net.dto.LeaderboardRows;
import view.GeneralPrinter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

public class LeaderboardMenu extends Menu {

    private static final Map<String, EndlessChapter> MEOW_COLUMNS = meowColumns();
    private static final List<String> BASE_COLUMNS =
            List.of("username", "chapter", "minigames", "quests", "score");

    private static final String ROW_FORMAT =
            "%-16s %-22s %-11s %-8s %-10s %-12s %-22s %-21s %-16s%n";

    private String sortColumn = "score";
    private boolean ascending = false;

    private static Map<String, EndlessChapter> meowColumns() {
        Map<String, EndlessChapter> columns = new LinkedHashMap<>();
        columns.put("egypt", EndlessChapter.EGYPT);
        columns.put("frostbite", EndlessChapter.FROSTBITE_CAVES);
        columns.put("beach", EndlessChapter.BIG_WAVE_BEACH);
        columns.put("darkages", EndlessChapter.DARK_AGES);
        columns.put("future", EndlessChapter.FUTURE);
        return columns;
    }

    private static List<String> validColumns() {
        List<String> columns = new ArrayList<>(BASE_COLUMNS);
        columns.addAll(MEOW_COLUMNS.keySet());
        return columns;
    }

    @Override
    public String getName() {
        return "Leaderboard Menu";
    }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else if (Regex.LEADERBOARD_SORT.getMatcherRaw(text).matches()) {
            sort(text);
        } else {
            GeneralPrinter.print("Not Valid");
        }
    }

    private void sort(String text) {
        Matcher matcher = Regex.LEADERBOARD_SORT.getMatcherRaw(text);
        matcher.matches();
        String column = matcher.group("column").toLowerCase(Locale.ROOT);
        String order = matcher.group("order").toLowerCase(Locale.ROOT);

        if (!validColumns().contains(column)) {
            GeneralPrinter.print("Error: unknown column '" + column + "'. Valid columns: "
                    + String.join(", ", validColumns()) + ".");
            return;
        }

        this.sortColumn = column;
        this.ascending = order.equals("asc");
        GeneralPrinter.print(showMenu());
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new GameMenu();
    }

    @Override
    public String showMenu() {
        String commands = "\nCommands:\n"
                + "  leaderboard sort -c <" + String.join("|", validColumns()) + "> -o <asc|desc>\n"
                + "  menu exit | menu show current";

        List<Level> allLevels;
        try {
            allLevels = LevelLoader.loadLevels();
        } catch (Exception e) {
            return "[ Leaderboard Menu ]\nError: could not load levels." + commands;
        }

        List<LeaderboardRowDto> rows = new ArrayList<>();
        for (User user : User.users) {
            rows.add(LeaderboardRows.of(user, allLevels));
        }

        if (rows.isEmpty()) {
            return "[ Leaderboard Menu ]\nNo records yet." + commands;
        }

        sortRows(rows);

        StringBuilder sb = new StringBuilder("[ Leaderboard Menu ]  (sorted by ")
                .append(sortColumn).append(", ").append(ascending ? "ascending" : "descending")
                .append(")\n");
        sb.append(String.format(ROW_FORMAT,
                "Username", "Chapter", "MiniGames", "Quests", "MyPoint",
                "Egypt Meow", "Frostbite Caves Meow", "Big Wave Beach Meow", "Dark Ages Meow"));

        for (LeaderboardRowDto row : rows) {
            sb.append(String.format(ROW_FORMAT,
                    row.usernameOrEmpty(), chapterText(row),
                    String.valueOf(row.miniGamesWon), String.valueOf(row.questsCompleted),
                    String.valueOf(row.myPointOrZero()),
                    meowText(row, EndlessChapter.EGYPT),
                    meowText(row, EndlessChapter.FROSTBITE_CAVES),
                    meowText(row, EndlessChapter.BIG_WAVE_BEACH),
                    meowText(row, EndlessChapter.DARK_AGES)));
        }

        return sb.toString().trim() + commands;
    }

    private static String chapterText(LeaderboardRowDto row) {
        if (row.chapter == null || "-".equals(row.chapter)) return "-";
        if (row.chapterStageCount <= 0) return row.chapter;
        return row.chapter + " " + row.chapterStagesCleared + "/" + row.chapterStageCount;
    }

    
    private static String meowText(LeaderboardRowDto row, EndlessChapter chapter) {
        return String.valueOf(row.lotteryScoreOrZero(chapter.key()));
    }

    /**
     * Every value column is plain numeric - a chapter never played counts as 0 - so
     * descending is simply the reversed comparator, with username breaking ties the same
     * way whichever direction the list runs.
     */
    private void sortRows(List<LeaderboardRowDto> rows) {
        Comparator<LeaderboardRowDto> byColumn = columnComparator();
        Comparator<LeaderboardRowDto> ordered = ascending ? byColumn : byColumn.reversed();
        rows.sort(ordered.thenComparing(LeaderboardRowDto::usernameOrEmpty,
                String.CASE_INSENSITIVE_ORDER));
    }

    private Comparator<LeaderboardRowDto> columnComparator() {
        EndlessChapter meow = MEOW_COLUMNS.get(sortColumn);
        if (meow != null) {
            String key = meow.key();
            return Comparator.comparingLong(row -> row.lotteryScoreOrZero(key));
        }
        return switch (sortColumn) {
            case "username" -> Comparator.comparing(LeaderboardRowDto::usernameOrEmpty,
                    String.CASE_INSENSITIVE_ORDER);
            case "chapter" -> Comparator.comparingInt((LeaderboardRowDto r) -> r.levelsCleared);
            case "minigames" -> Comparator.comparingInt((LeaderboardRowDto r) -> r.miniGamesWon);
            case "quests" -> Comparator.comparingInt((LeaderboardRowDto r) -> r.questsCompleted);
            default -> Comparator.comparingLong(LeaderboardRowDto::myPointOrZero);
        };
    }
}
