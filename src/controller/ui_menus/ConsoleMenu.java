package controller.ui_menus;

import model.App;
import model.Regex;
import view.GeneralPrinter;

public class ConsoleMenu extends Menu {

    @Override
    public String getName() {
        return "Console Menu";
    }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else {
            GeneralPrinter.print("Not Valid");
        }
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new GameMenu();
    }

    @Override
    public String showMenu() {
        return "[ Console Menu ]\nCommands:\n  menu exit | menu show current";
    }
}
