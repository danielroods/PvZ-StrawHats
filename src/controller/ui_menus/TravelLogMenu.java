package controller.ui_menus;

import controller.QuestManager;
import controller.match.mini_games.*;
import model.App;
import model.Regex;
import model.game_exceptions.GameException;
import model.match.mini_games.Beghouled;
import model.match.mini_games.Zombotany;
import model.match.mini_games.izombie.IZombie;
import model.match.mini_games.vasebreaker.Vasebreaker;
import model.match.mini_games.wallnutbowlling.WallnutBowling;
import model.quests.GameQuest;
import model.quests.QuestLoader;
import model.user_data.User;
import view.GeneralPrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TravelLogMenu extends Menu {
    private static final int QUESTS_PER_PAGE = 4;
    private static final Pattern PAGE_TOKEN = Pattern.compile("^(?<category>[A-Za-z]+)(?<pageNum>\\d*)$");

    private String currentCategory = "DAILY";
    private int currentSubPage = 1;

    @Override
    public String getName() {
        return "Travel Log Menu";
    }

    @Override
    public void handleCommand(String text) {
        isGeneralCmd = false;
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (Regex.TRAVEL_LOG_PAGE.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.TRAVEL_LOG_PAGE.getMatcherRaw(text);
            matcher.matches();
            changePage(matcher.group("pagename"));
        } else if (Regex.TRAVEL_LOG_COLLECT.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.TRAVEL_LOG_COLLECT.getMatcherRaw(text);
            matcher.matches();
            GeneralPrinter.print(QuestManager.collectReward(matcher.group("questid")));
        } else if (Regex.TRAVEL_LOG_PLAY_MINIGAME.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.TRAVEL_LOG_PLAY_MINIGAME.getMatcherRaw(text);
            matcher.matches();
            startMiniGame(matcher.group("minigame"), Integer.parseInt(matcher.group("level")));
        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else {
            GeneralPrinter.print("Invalid command.");
        }
    }

    private void changePage(String pageToken) {
        Matcher tokenMatcher = PAGE_TOKEN.matcher(pageToken.trim());
        if (!tokenMatcher.matches()) {
            GeneralPrinter.print("Error: invalid page name \"" + pageToken + "\".");
            return;
        }

        String category = tokenMatcher.group("category").toUpperCase();
        String pageNumRaw = tokenMatcher.group("pageNum");
        int requestedSubPage = pageNumRaw.isEmpty() ? 1 : Integer.parseInt(pageNumRaw);

        if (category.equals("MINIGAMES")) {
            if (requestedSubPage != 1) {
                GeneralPrinter.print("Error: the MINIGAMES page has only one page.");
                return;
            }
            currentCategory = category;
            currentSubPage = 1;
            GeneralPrinter.print(renderCurrentPage());
            return;
        }

        List<GameQuest> questsInCategory = questsForCategory(category);
        if (questsInCategory.isEmpty()) {
            GeneralPrinter.print("Error: no quests on page \"" + category + "\".");
            return;
        }

        int totalPages = (int) Math.ceil(questsInCategory.size() / (double) QUESTS_PER_PAGE);
        if (requestedSubPage < 1 || requestedSubPage > totalPages) {
            GeneralPrinter.print("Error: no quests on page \"" + pageToken + "\" (page "
                    + category + " has " + totalPages + " page(s)).");
            return;
        }

        currentCategory = category;
        currentSubPage = requestedSubPage;
        GeneralPrinter.print("Changing to Quest page: " + category
                + " (" + requestedSubPage + "/" + totalPages + ")");
        GeneralPrinter.print(renderCurrentPage());
    }

    private List<GameQuest> questsForCategory(String category) {
        if ("ALL".equals(category)) {
            return new ArrayList<>(QuestLoader.getAllQuests());
        }
        return QuestLoader.getAllQuests().stream()
                .filter(q -> q.getType() != null && q.getType().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    private String renderCurrentPage() {
        if (currentCategory.equals("MINIGAMES")) return renderMiniGames();
        List<GameQuest> questsInCategory = questsForCategory(currentCategory);
        if (questsInCategory.isEmpty()) {
            return "No quests on page \"" + currentCategory + "\".";
        }

        int totalPages = Math.max(1, (int) Math.ceil(questsInCategory.size() / (double) QUESTS_PER_PAGE));
        int safeSubPage = Math.min(currentSubPage, totalPages);
        int fromIndex = (safeSubPage - 1) * QUESTS_PER_PAGE;
        int toIndex = Math.min(fromIndex + QUESTS_PER_PAGE, questsInCategory.size());

        StringBuilder sb = new StringBuilder();
        sb.append("Page: ").append(currentCategory)
                .append(" (").append(safeSubPage).append("/").append(totalPages).append(")\n");

        for (int i = fromIndex; i < toIndex; i++) {
            sb.append(formatQuest(questsInCategory.get(i))).append("\n");
        }
        return sb.toString().trim();
    }

    private String formatQuest(GameQuest quest) {
        int progress = quest.getProgress();
        int target = QuestManager.getDisplayTarget(quest);

        String status;
        if (!quest.isCompleted()) {
            status = "In Progress";
        } else if (!quest.isRewardCollected()) {
            status = "Completed (not yet collected)";
        } else {
            status = "achieved";
        }

        StringBuilder line = new StringBuilder();
        line.append("- [").append(quest.getId()).append("] ")
                .append(quest.getTitle())
                .append(" (").append(quest.getType()).append("/").append(quest.getPriority()).append(")")
                .append(" - ").append(quest.getQuestDescription() == null ? "" : quest.getQuestDescription())
                .append("\n    Status: ").append(status);

        if (target > 1) {
            line.append("  ").append(progressBar(progress, target));
        }
        return line.toString();
    }

    private String progressBar(int progress, int target) {
        int clamped = Math.max(0, Math.min(progress, target));
        int barLength = 10;
        int filled = target == 0 ? 0 : (int) Math.round(barLength * (clamped / (double) target));
        String bar = "[" + "#".repeat(filled) + "-".repeat(barLength - filled) + "]";
        return bar + " " + clamped + "/" + target;
    }

    private String renderMiniGames() {
        return "Page: MINIGAMES\n"
                + "- Vasebreaker | Game mode: Mini-game | Levels: 1-3\n"
                + "- Wallnut Bowling | Game mode: Mini-game | Levels: 1-3\n"
                + "- I-Zombie | Game mode: Mini-game | Levels: 1-3\n"
                + "- Beghouled | Game mode: Mini-game | Levels: 1-3\n"
                + "- Zombotany | Game mode: Mini-game | Levels: 1-3";
    }

    private void startMiniGame(String name, int level) {
        String normalized = name.toLowerCase().replace("-", "").replace(",", "");
        switch (normalized) {
            case "vasebreaker" -> {
                int[] unlocked = User.currentUser.userState.unlockedPlantIds.stream()
                        .mapToInt(Integer::intValue).toArray();
                if (unlocked.length == 0) unlocked = new int[] {1};
                App.currentMenu = new VasebreakerController(new Vasebreaker(level, unlocked));
            }
            case "wallnutbowling", "walnutbowling" ->
                    App.currentMenu = new WallnutBowlingController(new WallnutBowling(level));
            case "izombie" -> App.currentMenu = new ImZombieController(new IZombie(level));
            case "izombie2p", "izombiecouch" ->
                    App.currentMenu = new CouchIZombieController();
            case "beghouled" -> App.currentMenu = new BeghouledController(new Beghouled(level));
            case "zombotany" -> App.currentMenu = new ZombotanyController(new Zombotany(level));
            default -> throw new GameException("no such mini-game.");
        }
        GeneralPrinter.print("Starting " + App.currentMenu.getName() + " level " + level
                + ". Game mode: Mini-game.");
        GeneralPrinter.print(App.currentMenu.showMenu());
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new MainMenu();
    }

    @Override
    public String showMenu() {
        return "[ Travel Log Menu ]\n" + renderCurrentPage() + "\nCommands:\n"
                + "  travel log page <DAILY/MAIN/EPIC/ALL/MINIGAMES>\n"
                + "  travel log collect -q <quest_id>\n"
                + "  travel log play -m <vasebreaker/wallnut-bowling/i-zombie/beghouled/zombotany> -l <1-3>\n"
                + "  menu exit | menu show current";
    }
}
