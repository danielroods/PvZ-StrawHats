package model.user_data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class User {

    public static ArrayList<User> users = new ArrayList<>();
    public static User currentUser = null;

    public String accountId;
    public String username, passwordHash, nickname, email, gender, securityQuestion, securityAnswerHash;
    public String syncedPasswordHash;
    public boolean stayLoggedIn;
    public UserState userState;
    public String profilePicture = "assets/images/ui/avatar_luffy.png";

    public User(String username, String password, String nickname, String email, String gender) {
        this.accountId = java.util.UUID.randomUUID().toString();
        this.username = username;
        this.passwordHash = hashPassword(password);
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.stayLoggedIn = false;
        this.profilePicture = "assets/images/ui/avatar_luffy.png";
        this.userState = new UserState(new ArrayList<>(), 0, 0, 0);
    }



    public String accountId() {
        if (accountId == null || accountId.isBlank()) accountId = legacyAccountId(username);
        return accountId;
    }

    public static String legacyAccountId(String username) {
        return "legacy-" + hashPassword(username == null ? "" : username.toLowerCase());
    }

    public boolean isSameAccountAs(User other) {
        return other != null && accountId().equals(other.accountId());
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : encoded)
                hex.append(String.format("%02x", b));

            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static User withHash(String accountId, String username, String passwordHash,
                                String nickname, String email, String gender) {
        User user = new User(username, "", nickname, email, gender);
        if (accountId != null && !accountId.isBlank()) user.accountId = accountId;
        user.passwordHash = passwordHash;
        return user;
    }

    public boolean checkPassword(String password) {
        return this.passwordHash.equals(hashPassword(password));
    }

    public void setPassword(String newPassword) {
        this.passwordHash = hashPassword(newPassword);
    }

    public void setSecurityQuestion(String question, String answer) {
        this.securityQuestion = question;
        this.securityAnswerHash = hashPassword(answer.toLowerCase().trim());
    }

    public boolean checkSecurityAnswer(String answer) {
        return this.securityAnswerHash.equals(hashPassword(answer.toLowerCase().trim()));
    }

    private static final LocalUserStore LOCAL_STORE = new LocalUserStore();
    private static UserStore activeStore = LOCAL_STORE;

    public static UserStore store() {
        return activeStore;
    }

    public static LocalUserStore localStore() {
        return LOCAL_STORE;
    }

    public static void useStore(UserStore store) {
        activeStore = store == null ? LOCAL_STORE : store;
    }

    public static void useLocalStore() {
        activeStore = LOCAL_STORE;
    }

    public static boolean isRemote() {
        return activeStore != LOCAL_STORE;
    }

    public static User findByUsername(String username) {
        return activeStore.findByUsername(username);
    }

    public static boolean usernameExists(String username) {
        return activeStore.usernameExists(username);
    }

    public static void load() {
        activeStore.load();
    }

    public static void setUser(User user) {
        activeStore.setUser(user);
    }

    public static void save() {
        activeStore.save();
    }

    public static void addUser(User user) {
        activeStore.addUser(user);
    }
}