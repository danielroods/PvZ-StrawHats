package controller.ui_menus;

import model.App;
import model.Regex;
import model.game_exceptions.GameException;
import model.user_data.User;
import view.GeneralPrinter;

import java.util.regex.Matcher;

public class SettingMenu extends Menu {

    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 5;

    @Override
    public String getName() {
        return "Setting Menu";
    }

    @Override
    public void handleCommand(String text){
        super.handleCommand(text);
        if (isGeneralCmd) return;





        if (Regex.MENU_SETTINGS_CHANGE_DIFFICULTY.getMatcherRaw(text).matches()) {
            Matcher matcher = Regex.MENU_SETTINGS_CHANGE_DIFFICULTY.getMatcherRaw(text);
            matcher.matches();
            changeDifficulty(matcher.group("difficultylevel"));

        }  else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();

        }else {
            GeneralPrinter.print("Not Valid");
        }
    }

    private void changeDifficulty(String rawLevel) {
        int level;
        try {
            level = Integer.parseInt(rawLevel);
        } catch (NumberFormatException e) {
            throw new GameException("difficulty level must be a whole number between " + MIN_DIFFICULTY + " and " + MAX_DIFFICULTY + ".");

        }

        if (level < MIN_DIFFICULTY || level > MAX_DIFFICULTY) {
            throw new GameException("difficulty level must be between " + MIN_DIFFICULTY + " and " + MAX_DIFFICULTY + ".");

        }

        User.currentUser.userState.difficultyLevel = level;
        User.save();
        GeneralPrinter.print("Difficulty level set to " + level + ".");
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new MainMenu();
    }

    @Override
    public String showMenu() {
        int level = User.currentUser.userState.difficultyLevel;
        double increaseCoefficient = level / 3.0;
        double decreaseCoefficient = 3.0 / level;

        return "[ Setting Menu ]\n"
                + "Current difficulty level: " + level + " (default: 3)\n"
                + "  zombie count / damage / game speed: x" + String.format("%.2f", increaseCoefficient) + "\n"
                + "  wave cost / sky sun rate: x" + String.format("%.2f", decreaseCoefficient) + "\n"
                + "Commands:\n"
                + "  menu settings change-difficulty -l <difficulty_level>\n"
                + "  menu enter <menu_name>\n"
                + "  menu exit\n"
                + "  menu show current";
    }
}
