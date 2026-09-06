package controller.ui_menus;

import view.GeneralPrinter;

/**
 * Purely a navigation placeholder - TrophiesScreen reads everything it shows from
 * {@link controller.TrophyManager}, which is stateless and derives trophy/key status
 * straight from the user's existing level progression, so there is nothing for this
 * menu to track or mutate.
 */
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
