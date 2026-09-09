package controller.ui_menus;

import model.App;
import view.GeneralPrinter;


public class ZombiePackmanMenu extends Menu {
    @Override public String getName() { return "Zombie Packman"; }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;
        if (text != null && text.trim().equalsIgnoreCase("restart")) {
            GeneralPrinter.print("Zombie Packman will restart from the graphical game screen.");
        } else if (text != null && text.trim().equalsIgnoreCase("menu exit")) {
            exitMenu();
        } else {
            GeneralPrinter.print(showMenu());
        }
    }

    @Override public void exitMenu() { App.currentMenu = new ConsoleMenu(); }

    @Override
    public String showMenu() {
        return "[ Zombie Packman ]\n"
                + "Arrow keys: move\n"
                + "SPACE: eat plants\n"
                + "Z: use temporary power\n"
                + "R: restart\n"
                + "ESC: return to Console";
    }
}
