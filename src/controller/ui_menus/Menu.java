package controller.ui_menus;

import controller.ui_menus.authentication.LoginMenu;
import controller.ui_menus.authentication.SignupMenu;
import model.App;
import model.Regex;
import model.game_exceptions.GameException;
import view.GeneralPrinter;

import java.util.regex.Matcher;

public abstract class Menu {
    protected boolean isGeneralCmd;

    public void changeMenu(String text) {
        String menuKey;
        Matcher matcher = Regex.MENU_ENTER.getMatcherRaw(text);
        if (matcher.matches()) {
            menuKey = matcher.group("menuname");
        } else {
            menuKey = text;
        }

        String normalized = menuKey.toLowerCase().replace("menu", "").trim();

        switch (normalized) {
            case "game" -> App.currentMenu = new GameMenu();
            case "profile" -> App.currentMenu = new ProfileMenu();
            case "settings", "setting" -> App.currentMenu = new SettingMenu();
            case "news" -> App.currentMenu = new NewsMenu();
            case "signup" -> App.currentMenu = new SignupMenu();
            case "main" -> App.currentMenu = new MainMenu();
            case "login" -> App.currentMenu = new LoginMenu();
            case "collection" -> App.currentMenu = new CollectionMenu();
            case "trophies", "trophy" -> App.currentMenu = new TrophiesMenu();
            case "travellog" -> App.currentMenu = new TravelLogMenu();
            case "console" -> App.currentMenu = new ConsoleMenu();
            case "zombie packman", "zombiepackman", "packman", "pacman" -> App.currentMenu = new ZombiePackmanMenu();
            case "leaderboard" -> App.currentMenu = new LeaderboardMenu();
            case "network", "multiplayer", "onlinematch", "online match" ->
                    App.currentMenu = new controller.ui_menus.network.NetworkMenu();
            case "coop", "co-op", "izombiecoop" -> {
                // Co-op has no level-supplied zombie pool, so it gets a bare NormalLevel
                // with an empty pool - the zombie player builds their own roster on the
                // CoopBeforeMatchScreen instead (see CoopBeforeMenu.selectedZombies).
                model.match.main.levels.normal_levels.NormalLevel coopLevel =
                        new model.match.main.levels.normal_levels.NormalLevel();
                coopLevel.setName("Co-op");
                coopLevel.setZombiePool(new java.util.ArrayList<>());

                model.utils.GameSession coopSession = new model.utils.GameSession();
                coopSession.setLevel(coopLevel);

                controller.match.BeforeMenu.selectedPlants.clear();
                controller.match.BeforeMenu.selectedZombies.clear();

                App.currentMenu = new controller.match.CoopBeforeMenu();
            }
            default -> throw new GameException("no such menu.");
        }
        System.out.println("Menu changed to: " + menuKey + " menu");
    }
    public void handleCommand(String text){
        isGeneralCmd = false;
        if (Regex.MENU_ENTER.getMatcherRaw(text).matches()){
            isGeneralCmd = true;
            changeMenu(text);
        } else if (Regex.MENU_SHOW_CURRENT.getMatcherRaw(text).matches()) {
            isGeneralCmd = true;
            GeneralPrinter.print(showMenu());
        }
    }



    public abstract String getName();

    public abstract void exitMenu();
    public String showMenu(){
        return "[ " + getName() + " ]\nCommands:\n  menu exit | menu show current";
    }
}