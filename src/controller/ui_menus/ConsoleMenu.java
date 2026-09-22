package controller.ui_menus;

import model.App;
import model.Regex;
import view.GeneralPrinter;


public class ConsoleMenu extends Menu {
    @Override public String getName() { return "Console"; }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;
        if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else {
            GeneralPrinter.print("Choose a Console game from the graphical menu.");
        }
    }

    @Override public void exitMenu() { App.currentMenu = new GameMenu(); }

    @Override
    public String showMenu() {
        return "[ Console ]\n"
                + "Games:\n"
                + "  Zombie Packman\n"
                + "  Zombie Dash\n"
                + "Commands:\n"
                + "  menu enter zombie packman\n"
                + "  menu enter zombie dash\n"
                + "  menu exit | menu show current";
    }
}
