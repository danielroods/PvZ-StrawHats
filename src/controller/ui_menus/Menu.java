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
            case "travellog" -> App.currentMenu = new TravelLogMenu();
            case "leaderboard" -> App.currentMenu = new LeaderboardMenu();
            case "trophy", "trophies" -> App.currentMenu = new TrophiesMenu();
            case "console" -> App.currentMenu = new ConsoleMenu();
            case "adventure" -> App.currentMenu = new AdventureMenu();
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