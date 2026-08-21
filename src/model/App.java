package model;

import controller.ui_menus.MainMenu;
import controller.ui_menus.Menu;
import controller.ui_menus.authentication.SignupMenu;
import model.match.main.MainMode;
import model.user_data.User;

import static model.user_data.User.load;

public class App {
    public static User currentUser = null;
    public static Menu currentMenu = new SignupMenu();
    public static MainMode currentMatch = null;
    static {
        load();
        if (currentUser == null) currentMenu = new SignupMenu();
        else currentMenu = new MainMenu();
    }
}
