package controller.ui_menus;

import view.GeneralPrinter;


public class TrophiesMenu extends Menu {

    @Override
    public String getName() {
        return "Trophies Menu";
    }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (model.Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else {
            GeneralPrinter.print("Not Valid");
        }
    }

    @Override
    public void exitMenu() {
        model.App.currentMenu = new GameMenu();
    }

    @Override
    public String showMenu() {
        return "[ Trophies Menu ]\nCommands:\n  menu exit | menu show current";
    }
}
