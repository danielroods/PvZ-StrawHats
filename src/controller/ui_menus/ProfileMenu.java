package controller.ui_menus;

import model.Regex;
import model.user_data.User;
import view.GeneralPrinter;

import java.util.regex.Matcher;

public class ProfileMenu extends Menu {

    private static final String SPECIAL_CHARS = "!#$%^&*()=+}{}[]|/\\:;'\"<>?";

    @Override
    public String getName() {
        return "Profile Menu";
    }

    @Override
    public void handleCommand(String text){
        super.handleCommand(text);
        if (isGeneralCmd) return;
        

        if (Regex.MENU_PROFILE_SHOW_INFO.getMatcherRaw(text).matches()) {
            GeneralPrinter.print(showMenu());
        } else if (Regex.MENU_PROFILE_CHANGE_USERNAME.getMatcherRaw(text).matches()) {
            handleChangeUsername(text);
        } else if (Regex.MENU_PROFILE_CHANGE_PASSWORD.getMatcherRaw(text).matches()) {
            handleChangePassword(text);
        } else if (Regex.MENU_PROFILE_CHANGE_NICKNAME.getMatcherRaw(text).matches()) {
            handleChangeNickname(text);
        } else if (Regex.MENU_PROFILE_CHANGE_EMAIL.getMatcherRaw(text).matches()) {
            handleChangeEmail(text);
        }  else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        }  else {
            GeneralPrinter.print("Not Valid");
        }
    }

    private void handleChangeUsername(String text) {
        Matcher matcher = Regex.MENU_PROFILE_CHANGE_USERNAME.getMatcherRaw(text);
        matcher.matches();
        String newUsername = matcher.group("username");

        User user = User.currentUser;
        if (newUsername.equals(user.username)) {
            GeneralPrinter.print("New username must be different from your current username.");
            return;
        }
        if (!newUsername.matches("[a-zA-Z0-9\\-]+")) {
            GeneralPrinter.print("Username can only contain letters, digits, and '-'.");
            return;
        }
        if (User.usernameExists(newUsername)) {
            GeneralPrinter.print("Username already exists. Please choose a different one.");
            return;
        }

        user.username = newUsername;
        User.save();
        GeneralPrinter.print("Username updated successfully.");
    }

    private void handleChangeNickname(String text) {
        Matcher matcher = Regex.MENU_PROFILE_CHANGE_NICKNAME.getMatcherRaw(text);
        matcher.matches();
        String newNickname = matcher.group("nickname");

        User user = User.currentUser;
        if (newNickname.equals(user.nickname)) {
            GeneralPrinter.print("New nickname must be different from your current nickname.");
            return;
        }
        if (newNickname.length() < 3 || newNickname.length() > 30) {
            GeneralPrinter.print("Nickname must be between 3 and 30 characters.");
            return;
        }

        user.nickname = newNickname;
        User.save();
        GeneralPrinter.print("Nickname updated successfully.");
    }

    private void handleChangeEmail(String text) {
        Matcher matcher = Regex.MENU_PROFILE_CHANGE_EMAIL.getMatcherRaw(text);
        matcher.matches();
        String newEmail = matcher.group("email");

        User user = User.currentUser;
        if (newEmail.equals(user.email)) {
            GeneralPrinter.print("New email must be different from your current email.");
            return;
        }
        if (!isValidEmail(newEmail)) {
            GeneralPrinter.print("Invalid email format.");
            return;
        }

        user.email = newEmail;
        User.save();
        GeneralPrinter.print("Email updated successfully.");
    }

    private void handleChangePassword(String text) {
        Matcher matcher = Regex.MENU_PROFILE_CHANGE_PASSWORD.getMatcherRaw(text);
        matcher.matches();
        String newPassword = matcher.group("newpassword"), oldPassword = matcher.group("oldpassword");

        User user = User.currentUser;
        if (!user.checkPassword(oldPassword)) {
            GeneralPrinter.print("Old password is incorrect.");
            return;
        }
        if (user.checkPassword(newPassword)) {
            GeneralPrinter.print("New password must be different from your current password.");
            return;
        }
        if (!isStrongPassword(newPassword)) {
            return;
        }

        user.setPassword(newPassword);
        User.save();
        GeneralPrinter.print("Password updated successfully.");
    }

    private boolean isValidEmail(String email) {
        if (!email.matches("^[a-zA-Z0-9]([a-zA-Z0-9._\\-]*[a-zA-Z0-9])?@[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,}$")) {
            return false;
        }
        return !email.contains("..");
    }

    private boolean isStrongPassword(String password) {
        if (password.length() < 8) {
            GeneralPrinter.print("Weak password: must be at least 8 characters.");
            return false;
        }
        if (!password.matches(".*[a-z].*")) {
            GeneralPrinter.print("Weak password: must contain at least one lowercase letter.");
            return false;
        }
        if (!password.matches(".*[A-Z].*")) {
            GeneralPrinter.print("Weak password: must contain at least one uppercase letter.");
            return false;
        }
        if (!password.matches(".*[0-9].*")) {
            GeneralPrinter.print("Weak password: must contain at least one digit.");
            return false;
        }
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (SPECIAL_CHARS.indexOf(c) >= 0) {
                hasSpecial = true;
                break;
            }
        }
        if (!hasSpecial) {
            GeneralPrinter.print("Weak password: must contain at least one special character (!#$%^&*...).");
            return false;
        }
        return true;
    }

    @Override
    public void exitMenu() {
        changeMenu("main");
    }

    @Override
    public String showMenu() {
        User user = User.currentUser;
        String sb = "[ Profile Menu ]\n" +
                "Username: " + user.username + "\n" +
                "Nickname: " + user.nickname + "\n" +
                "Games Played: " + user.userState.gamesPlayed + "\n" +
                "Coins: " + user.userState.coins + "\n" +
                "Diamonds: " + user.userState.diamonds + "\n" +
                "Levels Passed: " + user.userState.lastLevel + "\n" +
                "High Score: " + user.userState.highScore + "\n" +
                "Commands:\n" +
                "  menu profile show-info\n" +
                "  menu profile change-username -u <username>\n" +
                "  menu profile change-nickname -u <nickname>\n" +
                "  menu profile change-email -e <email>\n" +
                "  menu profile change-password -p <new_password> -o <old_password>\n" +
                "  menu enter <menu_name>\n" +
                "  menu exit\n" +
                "  menu show current";
        return sb;
    }
}
