package controller.ui_menus;

import controller.NewsManager;
import controller.ui_menus.authentication.LoginMenu;
import model.App;
import model.Regex;
import model.user_data.User;
import view.GeneralPrinter;

public class MainMenu extends Menu{

    @Override
    public String getName() {
        return "Main Menu";
    }

    @Override
    public void handleCommand(String text){
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (Regex.MENU_LOGOUT.getMatcherRaw(text).matches()) {
            Logout();

        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        }  else {
            GeneralPrinter.print("Not Valid");
        }
    }

    @Override
    public void exitMenu() {
        Logout();
    }

    @Override
    public String  showMenu() {
        String sb = "[ Main Menu ]\n" +
                "  Game\n" +
                "  Settings\n" +
                (NewsManager.hasUnreadNews() ? "  News [!] (you have unread news)\n" : "  News\n") +
                "  Profile\n" +
                "  Console\n" +
                "Commands:\n" +
                "  menu enter <menu_name>\n" +
                "  menu logout\n" +
                "  menu show current";
        return sb;
    }

    public void Logout() {
        User.save();
        App.currentMenu = new LoginMenu();
        App.currentUser = null;
        if (User.currentUser != null) {
            User.currentUser.stayLoggedIn = false;
        }
        User.currentUser = null;
        GeneralPrinter.print("Logged out successfully.");
    }

}