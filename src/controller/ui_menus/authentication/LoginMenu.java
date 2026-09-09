package controller.ui_menus.authentication;

import controller.ui_menus.MainMenu;
import controller.ui_menus.Menu;
import model.Regex;
import model.user_data.User;
import view.GeneralPrinter;

import java.util.regex.Matcher;

import static model.App.currentMenu;

public class LoginMenu extends Menu {

    private static User pendingPasswordReset = null;
    private static boolean awaitingNewPassword = false;

    @Override
    public String getName() {
        return "Login Menu";
    }

    
    public static boolean isAwaitingSecurityAnswer() {
        return pendingPasswordReset != null && !awaitingNewPassword;
    }

    
    public static boolean isAwaitingNewPassword() {
        return awaitingNewPassword;
    }

    
    public static String getPendingSecurityQuestion() {
        return pendingPasswordReset != null ? pendingPasswordReset.securityQuestion : null;
    }

    @Override
    public void handleCommand(String text){






        if (awaitingNewPassword) {
            handleNewPassword(text);
            return;
        }

        if (Regex.LOGIN.getMatcherRaw(text).matches()) {
            handleLogin(text);
        } else if (Regex.FORGET_PASSWORD.getMatcherRaw(text).matches()) {
            handleForgetPassword(text);
        } else if (Regex.ANSWER.getMatcherRaw(text).matches()) {
            handleAnswer(text);
        } else if (Regex.MENU_ENTER.getMatcherRaw(text).matches()) {
            handleMenuEnter(text);
        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else if (Regex.MENU_SHOW_CURRENT.getMatcherRaw(text).matches()) {
            GeneralPrinter.print(showMenu());
        } else {
            GeneralPrinter.print("Invalid command.");
        }
    }

    private void handleLogin(String text) {
        Matcher matcher = Regex.LOGIN.getMatcherRaw(text);
        matcher.matches();

        String username = matcher.group("username"), password = matcher.group("password");
        boolean stayLoggedIn = text.contains("-stay-logged-in");

        User user = User.findByUsername(username);
        if (user == null) {
            GeneralPrinter.print("Username not found.");
            return;
        }
        if (!user.checkPassword(password)) {
            GeneralPrinter.print("Incorrect password.");
            return;
        }

        for (User u : User.users) {
            u.stayLoggedIn = false;
        }

        user.stayLoggedIn = stayLoggedIn;
        User.setUser(user);

        User.save();

        GeneralPrinter.print("Welcome back, " + user.nickname + "!");
        currentMenu = new MainMenu();
    }

    private void handleForgetPassword(String text) {
        Matcher matcher = Regex.FORGET_PASSWORD.getMatcherRaw(text);
        matcher.matches();

        String username = matcher.group("username"), email = matcher.group("email");

        User user = User.findByUsername(username);
        if (user == null) {
            GeneralPrinter.print("Username not found.");
            return;
        }
        if (!user.email.equals(email)) {
            GeneralPrinter.print("Email does not match our records.");
            return;
        }
        if (user.securityQuestion == null) {
            GeneralPrinter.print("No security question set for this account.");
            return;
        }

        pendingPasswordReset = user;
        GeneralPrinter.print("Security question: " + user.securityQuestion);
        GeneralPrinter.print("Answer with: answer -a <your_answer>");
    }

    private void handleAnswer(String text) {
        if (pendingPasswordReset == null) {
            GeneralPrinter.print("No active password reset. Use 'forget password' first.");
            return;
        }

        Matcher matcher = Regex.ANSWER.getMatcherRaw(text);
        matcher.matches();
        String answer = matcher.group("answer");

        if (!pendingPasswordReset.checkSecurityAnswer(answer)) {
            GeneralPrinter.print("Incorrect answer. Returning to menu.");
            pendingPasswordReset = null;
            return;
        }

        awaitingNewPassword = true;
        GeneralPrinter.print("Correct! Enter your new password:");
    }

    private void handleNewPassword(String text) {
        String newPassword = text.trim();

        if (!isStrongPassword(newPassword)) return;

        pendingPasswordReset.setPassword(newPassword);
        User.save();
        GeneralPrinter.print("Password updated successfully. Please log in.");

        pendingPasswordReset = null;
        awaitingNewPassword = false;
    }

    private boolean isStrongPassword(String password) {
        String specialChars = "!#$%^&*()=+}{}[]|/\\:;'\"<>?";
        if (password.length() < 8) {
            GeneralPrinter.print("Weak password: must be at least 8 characters.");
            return false;
        }
        if (!password.matches(".*[a-z].*")) {
            GeneralPrinter.print("Weak password: must contain a lowercase letter.");
            return false;
        }
        if (!password.matches(".*[A-Z].*")) {
            GeneralPrinter.print("Weak password: must contain an uppercase letter.");
            return false;
        }
        if (!password.matches(".*[0-9].*")) {
            GeneralPrinter.print("Weak password: must contain a digit.");
            return false;
        }
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (specialChars.indexOf(c) >= 0) {
                hasSpecial = true;
                break;
            }
        }
        if (!hasSpecial) {
            GeneralPrinter.print("Weak password: must contain a special character.");
            return false;
        }
        return true;
    }

    private void handleMenuEnter(String text) {
        Matcher matcher = Regex.MENU_ENTER.getMatcherRaw(text);
        matcher.matches();
        String menuName = matcher.group("menuname");

        String normalized = menuName.toLowerCase().replace("menu", "").trim();
        if (normalized.equals("signup")) {
            changeMenu("signup");
        } else {
            GeneralPrinter.print("You can only enter the Signup Menu before logging in.");
        }
    }

    @Override
    public void exitMenu() {
        changeMenu("SignUp Menu");
    }

    @Override
    public String showMenu() {
        return "[ Login Menu ]\nCommands:\n"
                + "  login -u <username> -p <password> [-stay-logged-in]\n"
                + "  forget password -u <username> -e <email>\n"
                + "  answer -a <answer>\n"
                + "  menu enter signup\n"
                + "  menu exit | menu show current";
    }

    
    
}
