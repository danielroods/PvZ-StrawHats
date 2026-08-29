package model.user_data;

public final class AccountValidation {

    public static final String SPECIAL_CHARS = "!#$%^&*()=+}{}[]|/\\:;'\"<>?";

    private static final String EMAIL_PATTERN =
            "^[a-zA-Z0-9]([a-zA-Z0-9._\\-]*[a-zA-Z0-9])?@[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?"
                    + "(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,}$";

    private AccountValidation() {
    }

    public static String usernameError(String username) {
        if (username == null || !username.matches("[a-zA-Z0-9\\-]+")) {
            return "Username can only contain letters, digits, and '-'.";
        }
        return null;
    }

    public static String passwordError(String password) {
        if (password == null || password.length() < 8) {
            return "Weak password: must be at least 8 characters.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Weak password: must contain at least one lowercase letter.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Weak password: must contain at least one uppercase letter.";
        }
        if (!password.matches(".*[0-9].*")) {
            return "Weak password: must contain at least one digit.";
        }
        if (!hasSpecialChar(password)) {
            return "Weak password: must contain at least one special character (!#$%^&*...).";
        }
        return null;
    }

    public static String nicknameError(String nickname) {
        if (nickname == null || nickname.length() < 3 || nickname.length() > 30) {
            return "Nickname must be between 3 and 30 characters.";
        }
        return null;
    }

    public static String emailError(String email) {
        if (email == null || !email.matches(EMAIL_PATTERN)) {
            return "Invalid email format.";
        }
        if (email.contains("..")) {
            return "Invalid email: consecutive dots are not allowed.";
        }
        for (char c : email.toCharArray()) {
            if (SPECIAL_CHARS.indexOf(c) >= 0 && c != '@' && c != '.' && c != '-' && c != '_') {
                return "Invalid email: contains illegal characters.";
            }
        }
        return null;
    }

    public static String genderError(String gender) {
        boolean known = gender != null
                && (gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("female"));
        if (!known) {
            return "Gender must be 'male' or 'female'.";
        }
        return null;
    }

    public static String registrationError(String username, String password, String nickname,
                                           String email, String gender) {
        String error = usernameError(username);
        if (error == null) error = passwordError(password);
        if (error == null) error = nicknameError(nickname);
        if (error == null) error = emailError(email);
        if (error == null) error = genderError(gender);
        return error;
    }

    public static boolean hasSpecialChar(String value) {
        if (value == null) return false;
        for (char c : value.toCharArray()) {
            if (SPECIAL_CHARS.indexOf(c) >= 0) return true;
        }
        return false;
    }
}
