package controller.ui_menus;

import model.App;
import model.Regex;
import model.news.News;
import model.user_data.User;
import view.GeneralPrinter;

public class NewsMenu extends Menu {

    @Override
    public String getName() {
        return "News Menu";
    }

    @Override
    public void handleCommand(String text){
        super.handleCommand(text);
        if (isGeneralCmd) return;

        if (Regex.MENU_NEWS_SHOW_UNREAD.getMatcherRaw(text).matches()) {
            showUnread();

        } else if (Regex.MENU_NEWS_SHOW_ALL.getMatcherRaw(text).matches()) {
            showAll();

        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();

        }else {
            GeneralPrinter.print("Not Valid");
        }
    }

    private void showUnread() {
        var newsList = User.currentUser.userState.news;
        boolean hasUnread = false;

        for (News news : newsList) {
            if (!news.isRead()) {
                GeneralPrinter.print(news.getText());
                news.setRead(true);
                hasUnread = true;
            }
        }

        if (!hasUnread) {
            GeneralPrinter.print("No unread news.");
        }

        User.save();
    }

    private void showAll() {
        var newsList = User.currentUser.userState.news;

        if (newsList.isEmpty()) {
            GeneralPrinter.print("No news available.");
            return;
        }

        for (News news : newsList) {
            String status = news.isRead() ? "[READ]" : "[NEW]";
            GeneralPrinter.print(status + " " + news.getText());
            news.setRead(true);
        }

        User.save();
    }

    @Override
    public void exitMenu() {
        App.currentMenu = new MainMenu();
    }

    @Override
    public String showMenu() {
        return "[ News Menu ]\nCommands:\n  menu news show-unread\n  menu news show-all\n  menu enter <menu_name>\n  menu exit\n  menu show current";
    }
}
