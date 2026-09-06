package controller.ui_menus;

import controller.ui_menus.greenhouse.GreenhouseMenu;
import model.App;
import model.Regex;
import model.user_data.User;
import view.GeneralPrinter;

public class GameMenu extends Menu {

    @Override
    public String getName() {
        return "Game Menu";
    }

    @Override
    public void handleCommand(String text) {
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (Regex.MENU_TRAVEL_LOG.getMatcherRaw(text).matches()) {
            App.currentMenu = new TravelLogMenu();

        } else if (Regex.MENU_COIN_WALLET.getMatcherRaw(text).matches()) {
            GeneralPrinter.print("Coins: " + User.currentUser.userState.coins);

        } else if (Regex.MENU_GEM_WALLET.getMatcherRaw(text).matches()) {
            GeneralPrinter.print("Gems: " + User.currentUser.userState.diamonds);

        } else if (Regex.MENU_LEADERBOARD.getMatcherRaw(text).matches()) {
            App.currentMenu = new LeaderboardMenu();

        } else if (Regex.MENU_GREENHOUSE.getMatcherRaw(text).matches()) {
            App.currentMenu = new GreenhouseMenu();

        } else if (Regex.MENU_CHEAT_ADD.getMatcherRaw(text).matches()) {
            var matcher = Regex.MENU_CHEAT_ADD.getMatcherRaw(text);
            matcher.matches();
            int amount = Integer.parseInt(matcher.group("n"));
            String type = matcher.group("r");

            if (type.equals("coin")) {
                User.currentUser.userState.coins += amount;
            } else if (type.equals("diamond")) {
                User.currentUser.userState.diamonds += amount;
            }

        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();

        } else {
            GeneralPrinter.print("Not Valid");
        }
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new MainMenu();
    }

    @Override
    public String showMenu() {
        return "[ Game Menu ]\n"
                + "Commands:\n"
                + "  menu enter adventure\n"
                + "  menu enter collection\n"
                + "  menu enter console\n"
                + "  menu greenhouse | greenhouse menu\n"
                + "  menu enter trophies\n"
                + "  travel-log menu | menu leaderboard\n"
                + "  coin-wallet menu | gem-wallet menu\n"
                + "  menu cheat add <n> <coin/diamond>\n"
                + "  menu exit | menu show current";
    }
}
