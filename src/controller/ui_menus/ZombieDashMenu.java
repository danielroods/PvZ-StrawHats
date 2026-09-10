package controller.ui_menus;

import model.App;
import view.GeneralPrinter;


public class ZombieDashMenu extends Menu {
    @Override public String getName() { return "Zombie Dash"; }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;
        if (text != null && text.trim().equalsIgnoreCase("restart")) {
            GeneralPrinter.print("Zombie Dash will restart from the graphical game screen.");
        } else if (text != null && text.trim().equalsIgnoreCase("menu exit")) {
            exitMenu();
        } else {
            GeneralPrinter.print(showMenu());
        }
    }

    @Override public void exitMenu() { App.currentMenu = new ConsoleMenu(); }

    @Override
    public String showMenu() {
        return "[ Zombie Dash ]\n"
                + "UP/DOWN: switch lane\n"
                + "P: pause\n"
                + "R: restart\n"
                + "ESC: return to Console";
    }
}
