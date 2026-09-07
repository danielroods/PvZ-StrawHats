package controller.ui_menus;

import controller.cheat.CheatAccess;
import controller.cheat.CurrencyCheatController;
import controller.ui_menus.greenhouse.GreenhouseMenu;
import model.resoures.CurrencyType;
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
            addCurrencyCheat(matcher.group("n"), matcher.group("r"));

        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();

        } else {
            GeneralPrinter.print("Not Valid");
        }
    }

    private void addCurrencyCheat(String rawAmount, String type) {
        if (!CheatAccess.allow()) return;

        CurrencyType currency = null;
        if ("coin".equalsIgnoreCase(type)) {
            currency = CurrencyType.COIN;
        } else if ("diamond".equalsIgnoreCase(type)) {
            currency = CurrencyType.DIAMOND;
        }
        if (currency == null) {
            GeneralPrinter.print("[Cheat] Unknown currency: " + type + ".");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(rawAmount.trim());
        } catch (NumberFormatException e) {
            GeneralPrinter.print("[Cheat] Amount must be a whole number.");
            return;
        }
        new CurrencyCheatController().grant(currency, amount);
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new MainMenu();
    }

    @Override
    public String showMenu() {
        String cheatHelp = CheatAccess.isEnabled()
                ? "  menu cheat add <n> <coin/diamond>\n" : "";
        return "[ Game Menu ]\n"
                + "Commands:\n"
                + "  menu enter adventure\n"
                + "  menu enter collection\n"
                + "  menu enter console\n"
                + "  menu greenhouse | greenhouse menu\n"
                + "  menu enter trophies\n"
                + "  travel-log menu | menu leaderboard\n"
                + "  coin-wallet menu | gem-wallet menu\n"
                + cheatHelp
                + "  menu exit | menu show current";
    }
}
