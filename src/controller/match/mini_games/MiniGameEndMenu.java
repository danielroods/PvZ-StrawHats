package controller.match.mini_games;

import controller.ui_menus.Menu;
import controller.ui_menus.TravelLogMenu;
import model.App;
import model.Regex;
import view.GeneralPrinter;

public class MiniGameEndMenu extends Menu {
    private final String gameName;
    private final String result;
    private final String details;
    private final Runnable restartAction;

    public MiniGameEndMenu(String gameName, boolean won, String details) {
        this(gameName, won, details, null);
    }

    public MiniGameEndMenu(String gameName, boolean won, String details, Runnable restartAction) {
        this.gameName = gameName;
        this.result = won ? "WIN" : "LOSS";
        this.details = details == null ? "" : details;
        this.restartAction = restartAction;
    }

    public String getGameName() { return gameName; }
    public boolean isWon() { return "WIN".equals(result); }
    public String getDetails() { return details; }
    public boolean canRestart() { return restartAction != null; }

    public void restart() {
        if (restartAction != null) restartAction.run();
    }

    @Override
    public String getName() {
        return gameName + " Ending Menu";
    }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else {
            GeneralPrinter.print("The " + gameName
                    + " game has ended. Use 'menu exit' to return to the Travel Log.");
        }
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new TravelLogMenu();
        GeneralPrinter.print("Returning to the Travel Log.");
    }

    @Override
    public String showMenu() {
        return "[ " + getName() + " ]\n"
                + "Result: " + result + "\n"
                + (details.isBlank() ? "" : details + "\n")
                + "Commands:\n"
                + "  menu exit\n"
                + "  menu show current";
    }
}
