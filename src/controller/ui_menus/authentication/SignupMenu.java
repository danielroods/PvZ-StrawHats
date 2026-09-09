package controller.ui_menus.authentication;

import controller.ui_menus.Menu;
import model.Regex;
import model.user_data.User;
import view.GeneralPrinter;

import java.util.List;
import java.util.regex.Matcher;

import static model.App.currentMenu;

public class SignupMenu extends Menu {

    private static final List<String> SECURITY_QUESTIONS = List.of(
            "1. What is the name of your first pet?",
            "2. What city were you born in?",
            "3. What is your mother's maiden name?",
            "4. What was the name of your elementary school?",
            "5. What is your oldest sibling's middle name?"
    );

    private static final String SPECIAL_CHARS = "!#$%^&*()=+}{}[]|/\\:;'\"<>?";

    // the things we are working on currently
    private static String pendingUsername, pendingPassword, pendingNickname, pendingEmail, pendingGender;

    private static boolean isPendingSecurityAnswer = false;

    @Override
    public String getName() {
        return "SignUP Menu";
    }

    /** True between a successful "register" and a successful "pick question". Read-only view onto the wizard state above - no behavior change. */
    public static boolean isPendingSecurityAnswer() {
        return isPendingSecurityAnswer;
    }

    /** The same fixed question list handleRegister()/handlePickQuestion() already use, so a view never has to duplicate this text. */
    public static List<String> getSecurityQuestions() {
        return SECURITY_QUESTIONS;
    }

    @Override
    public void handleCommand(String text){

        if (Regex.MENU_SHOW_CURRENT.getMatcherRaw(text).matches()) {
            GeneralPrinter.print(showMenu());
            return;
        }

        if (isPendingSecurityAnswer) {
            if (Regex.PICK_QUESTION.getMatcherRaw(text).matches()) {
                handlePickQuestion(text);
            } else {
                GeneralPrinter.print("Please pick a security question first.");
            }
            return;
        }

        if (Regex.REGISTER.getMatcherRaw(text).matches()) {
            handleRegister(text);
        } else if (Regex.MENU_ENTER.getMatcherRaw(text).matches()) {
            handleMenuEnter(text);
        } else if (Regex.MENU_EXIT.getMatcherRaw(text).matches()) {
            exitMenu();
        } else {
            GeneralPrinter.print("Invalid command");
        }
    }

    private void handleRegister(String text) {
        Matcher matcher = Regex.REGISTER.getMatcherRaw(text);
        matcher.matches();

        String username = matcher.group("username"), password = matcher.group("password"),
                passwordConfirm = matcher.group("passwordConfirm"), nickname = matcher.group("nickname");
        String email = matcher.group("email"), gender = matcher.group("gender");

        if (!(validateUsername(username) && validatePassword(password, passwordConfirm) && validateNickname(nickname)
                && validateEmail(email) && validateGender(gender))) return; // discrete structure

        pendingUsername = username;
        pendingPassword = password;
        pendingNickname = nickname;
        pendingEmail = email;
        pendingGender = gender;
        isPendingSecurityAnswer = true;

        GeneralPrinter.print("Please pick a security question:");
        for (String q : SECURITY_QUESTIONS) {
            GeneralPrinter.print(q);
        }
        GeneralPrinter.print("Usage: pick question -q <number> -a <answer> -c <answer_confirm>");
    }

    private void handlePickQuestion(String text) {
        Matcher matcher = Regex.PICK_QUESTION.getMatcherRaw(text);
        matcher.matches();

        int questionNumber;
        try {
            questionNumber = Integer.parseInt(matcher.group("questionnumber"));
        } catch (NumberFormatException e) {
            GeneralPrinter.print("Invalid question number.");
            return;
        }

        if (questionNumber < 1 || questionNumber > SECURITY_QUESTIONS.size()) {
            GeneralPrinter.print("Question number must be between 1 and " + SECURITY_QUESTIONS.size() + ".");
            return;
        }

        String answer = matcher.group("answer"),  answerConfirm = matcher.group("answerconfirm");

        if (!answer.equals(answerConfirm)) {
            GeneralPrinter.print("Answers do not match. Please try again.");
            return;
        }

        User newUser = new User(pendingUsername, pendingPassword, pendingNickname, pendingEmail, pendingGender);
        newUser.setSecurityQuestion(SECURITY_QUESTIONS.get(questionNumber - 1), answer);
        User.addUser(newUser);

        isPendingSecurityAnswer = false;
        GeneralPrinter.print("Account created successfully! Please log in.");
        currentMenu = new LoginMenu();
    }

    private boolean validateUsername(String username) {
        if (!username.matches("[a-zA-Z0-9\\-]+")) {
            GeneralPrinter.print("Username can only contain letters, digits, and '-'.");
            return false;
        }
        if (User.usernameExists(username)) {
            GeneralPrinter.print("Username already exists. Please choose a different one.");
            return false;
        }
        return true;
    }

    private boolean validatePassword(String password, String confirm) {
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
        if (!password.equals(confirm)) {
            GeneralPrinter.print("Passwords do not match. Please re-enter.");
            return false;
        }
        return true;
    }

    private boolean validateNickname(String nickname) {
        if (nickname.length() < 3 || nickname.length() > 30) {
            GeneralPrinter.print("Nickname must be between 3 and 30 characters.");
            return false;
        }
        return true;
    }

    private boolean validateEmail(String email) {
        if (!email.matches("^[a-zA-Z0-9]([a-zA-Z0-9._\\-]*[a-zA-Z0-9])?@[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,}$")) {
            GeneralPrinter.print("Invalid email format.");
            return false;
        }
        if (email.contains("..")) {
            GeneralPrinter.print("Invalid email: consecutive dots are not allowed.");
            return false;
        }
        for (char c : email.toCharArray()) {
            if (SPECIAL_CHARS.indexOf(c) >= 0 && c != '@' && c != '.' && c != '-' && c != '_') {
                GeneralPrinter.print("Invalid email: contains illegal characters.");
                return false;
            }
        }
        return true;
    }

    private boolean validateGender(String gender) {
        if (!gender.equalsIgnoreCase("male") && !gender.equalsIgnoreCase("female")) {
            GeneralPrinter.print("Gender must be 'male' or 'female'.");
            return false;
        }
        return true;
    }

    private void handleMenuEnter(String text) {
        Matcher matcher = Regex.MENU_ENTER.getMatcherRaw(text);
        matcher.matches();
        String menuName = matcher.group("menuname").toLowerCase().trim();
        if (menuName.equals("login")) {
            currentMenu = new LoginMenu();
        } else {
            GeneralPrinter.print("You can only enter the Login Menu from here.");
        }
    }

    @Override
    public void exitMenu() {
        GeneralPrinter.print("Goodbye!");
        System.exit(0);
    }

    @Override
    public String showMenu() {
        return "[ Signup Menu ]\nCommands:\n"
                + "  register -u <username> -p <password> <password_confirm> -n <nickname> -e <email> -g <gender>\n"
                + "  pick question -q <question_number> -a <answer> -c <answer_confirm>\n"
                + "  menu enter login\n"
                + "  menu exit | menu show current";
    }

    
}
