package controller.ui_menus;

import model.App;
import model.Regex;
import model.game_exceptions.GameException;
import model.match.main.levels.Level;
import model.match.main.season.Season;
import model.match.main.season.SeasonFactory;
import model.user_data.User;
import model.user_data.UserState;
import model.utils.LevelLoader;
import model.utils.LevelProgression;
import view.GeneralPrinter;

import java.util.List;
import java.util.regex.Matcher;

public class AdventureMenu extends Menu {

    @Override
    public String getName() {
        return "Adventure Menu";
    }

    @Override
    public void handleCommand(String text) {
        if (Regex.MENU_SHOW_CHAPTERS.getMatcherRaw(text).matches()) {
            showChapters();
            return;
        }
        if (Regex.MENU_ENTER_CHAPTER.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.MENU_ENTER_CHAPTER.getMatcherRaw(text);
            matcher.matches();
            enterChapter(matcher.group("chaptername"));
            return;
        }

        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else {
            GeneralPrinter.print("Not Valid");
        }
    }

    private void enterChapter(String chapterName) {
        Season season;
        try {
            season = SeasonFactory.create(chapterName);
        } catch (IllegalArgumentException e) {
            throw new GameException("no such chapter.");
        }

        List<Level> allLevels;
        try {
            allLevels = LevelProgression.sorted(LevelLoader.loadLevels());
        } catch (Exception e) {
            throw new GameException("could not load levels.");
        }
        UserState state = User.currentUser.userState;
        List<Level> chapterLevels = allLevels.stream()
                .filter(level -> level.getSeason().getName().equalsIgnoreCase(season.getName()))
                .filter(level -> LevelProgression.isUnlocked(allLevels, state.lastLevel, level))
                .toList();

        if (chapterLevels.isEmpty()) {
            throw new GameException("chapter is locked.");
        }

        Level target = chapterLevels.stream()
                .filter(level -> !LevelProgression.isCompleted(allLevels, state.lastLevel, level))
                .findFirst()
                .orElse(chapterLevels.get(chapterLevels.size() - 1));

        controller.match.MatchMenu.configureChapter(allLevels, chapterLevels, target);
        App.currentMenu = new controller.match.MatchMenu();
    }

    private void showChapters() {
        try {
            List<Level> allLevels = LevelProgression.sorted(LevelLoader.loadLevels());
            for (String chapter : List.of("Egypt", "Frostbite Caves", "Big Wave Beach", "Dark Ages", "Pirates", "Future")) {
                boolean unlocked = allLevels.stream()
                        .filter(level -> level.getSeason().getName().equalsIgnoreCase(chapter))
                        .anyMatch(level -> LevelProgression.isUnlocked(allLevels,
                                User.currentUser.userState.lastLevel, level));
                GeneralPrinter.print(chapter + " - " + (unlocked ? "Unlocked" : "Locked"));
            }
        } catch (Exception e) {
            throw new GameException("could not load levels.");
        }
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new GameMenu();
    }

    @Override
    public String showMenu() {
        return "[ Adventure Menu ]\n"
                + "Commands:\n"
                + "  menu show chapters\n"
                + "  menu enter chapter -c <chapter_name>\n"
                + "  menu exit | menu show current";
    }
}
