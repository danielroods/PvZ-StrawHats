package controller.ui_menus;

import model.App;
import model.Regex;
import model.match.main.levels.Level;
import model.user_data.User;
import model.utils.LevelLoader;
import view.GeneralPrinter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class LeaderboardMenu extends Menu {

    private static final List<String> VALID_COLUMNS = List.of(
            "rank", "username", "season", "chapter", "stage", "minigames", "quests", "score"
    );

    private String sortColumn = "score";
    private boolean ascending = false;

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

        if (!VALID_COLUMNS.contains(column)) {
            GeneralPrinter.print("Error: unknown column '" + column + "'. Valid columns: "
                    + String.join(", ", VALID_COLUMNS) + ".");
            return;
        }

        this.sortColumn = column;
        this.ascending = order.equals("asc");
        GeneralPrinter.print(showMenu());
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new MainMenu();
    }

    @Override
    public String showMenu() {
        String commands = "\nCommands:\n"
                + "  leaderboard sort -c <rank|username|season|chapter|stage|minigames|quests|score> -o <asc|desc>\n"
                + "  menu exit | menu show current";

        List<Level> allLevels;
        try {
            allLevels = LevelLoader.loadLevels();
        } catch (Exception e) {
            return "[ Leaderboard Menu ]\nError: could not load levels." + commands;
        }

        List<Row> rows = User.users.stream()
                .map(u -> Row.of(u, allLevels))
                .collect(Collectors.toList());

        if (rows.isEmpty()) {
            return "[ Leaderboard Menu ]\nNo records yet." + commands;
        }

        sortRows(rows);

        StringBuilder sb = new StringBuilder("[ Leaderboard Menu ]  (sorted by ")
                .append(sortColumn).append(", ").append(ascending ? "ascending" : "descending").append(")\n");
        sb.append(String.format("%-5s %-16s %-10s %-20s %-14s %-11s %-8s %-6s%n",
                "Rank", "Username", "Season", "Chapter", "Stage", "MiniGames", "Quests", "Score"));

        int rank = 1;
        for (Row row : rows) {
            sb.append(String.format("%-5d %-16s %-10s %-20s %-14s %-11d %-8d %-6d%n",
                    rank++, row.username, row.season, row.chapter, row.stage,
                    row.miniGamesWon, row.questsCompleted, row.highScore));
        }

        return sb.toString().trim() + commands;
    }

    private void sortRows(List<Row> rows) {
        Comparator<Row> comparator = switch (sortColumn) {
            case "rank" -> null; // "rank" just means: keep insertion order, flip if descending
            case "username" -> Comparator.comparing((Row r) -> r.username, String.CASE_INSENSITIVE_ORDER);
            case "season" -> Comparator.comparing((Row r) -> r.season, String.CASE_INSENSITIVE_ORDER);
            case "chapter" -> Comparator.comparing((Row r) -> r.chapter, String.CASE_INSENSITIVE_ORDER);
            case "stage" -> Comparator.comparingInt((Row r) -> r.stageLevelId);
            case "minigames" -> Comparator.comparingInt((Row r) -> r.miniGamesWon);
            case "quests" -> Comparator.comparingInt((Row r) -> r.questsCompleted);
            case "score" -> Comparator.comparingInt((Row r) -> r.highScore);
            default -> Comparator.comparingInt((Row r) -> r.highScore);
        };

        if (comparator == null) {
            // "rank": reverse whatever order rows arrived in
            if (!ascending) Collections.reverse(rows);
            return;
        }

        rows.sort(ascending ? comparator : comparator.reversed());
    }


    private record Row(String username, String season, String chapter, String stage, int stageLevelId, int miniGamesWon,
                       int questsCompleted, int highScore) {

        static Row of(User user, List<Level> allLevels) {
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

                return new Row(user.username, season, chapter, stage, stageLevelId,
                        user.userState.miniGamesWon, user.userState.questsCompleted, user.userState.highScore);
            }

            private static String capitalize(String s) {
                if (s == null || s.isEmpty()) return s;
                return Character.toUpperCase(s.charAt(0)) + s.substring(1);
            }
        }
}