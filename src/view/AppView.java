package view;

import model.App;
import model.game_exceptions.GameException;
import view.menus.*;
import view.menus.collection_view.CollectionMenuView;
import view.menus.match.AfterMatchView;
import view.menus.match.BeforeMatchView;
import view.menus.match.MatchMenuView;
import view.menus.match.MeanwhileMatchView;


public class AppView {
    public static void run() {

        while (true) {
            MenuView currentView = resolveView(App.currentMenu.getName());
            if (currentView == null) {
                GeneralPrinter.print("Unknown menu state, exiting.");
                break;
            }
            try {
                if (!currentView.getInput()) break;
            } catch (GameException e) {
                GeneralPrinter.print("[Error] "+e.getMessage());
            } catch (Exception e) {
                GeneralPrinter.print(e.getMessage());
            }

        }
    }

    private static MenuView resolveView(String menuName) {
        return switch (menuName) {
            case "SignUP Menu" -> new SignupMenuView();
            case "Login Menu" -> new LoginMenuView();
            case "Main Menu" -> new MainMenuView();
            case "Profile Menu" -> new ProfileMenuView();
            case "Game Menu" -> new GameMenuView();
            case "News Menu" -> new NewsMenuView();
            case "Collection Menu" -> new CollectionMenuView();
            case "Setting Menu" -> new SettingMenuView();
            case "Greenhouse Menu" -> new GreenhouseMenuView();
            case "Store Menu" -> new StoreMenuView();
            case "Travel Log Menu" -> new TravelLogMenuView();
            case "Match Menu" -> new MatchMenuView();
            case "Leaderboard Menu" -> new LeaderboardMenuView();
            case "After Menu" -> new AfterMatchView();
            case "Before Menu" -> new BeforeMatchView();
            case "Meanwhile Menu" -> new MeanwhileMatchView();
            case "Vasebreaker Menu", "Wallnut Bowling Menu", "I, Zombie Menu",
                    "Beghouled Menu", "Zombotany Menu" -> new MeanwhileMatchView();
            default -> null;
        };
    }
}
