package net.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.user_data.AccountValidation;
import model.user_data.User;
import model.user_data.UserState;
import service.Log;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AccountStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<ArrayList<User>>() { }.getType();

    private final Object lock = new Object();
    private final File file;
    private final List<User> users = new ArrayList<>();
    private volatile boolean dirty;

    public AccountStore(File dataDirectory) {
        if (!dataDirectory.exists() && !dataDirectory.mkdirs()) {
            Log.error("AccountStore", "Could not create " + dataDirectory.getAbsolutePath());
        }
        this.file = new File(dataDirectory, "Data.json");
        load();
    }

    private void load() {
        synchronized (lock) {
            users.clear();
            if (!file.isFile()) {
                Log.info("AccountStore", "No account file yet, starting empty: " + file.getAbsolutePath());
                return;
            }
            try (Reader reader = new FileReader(file)) {
                ArrayList<User> loaded = GSON.fromJson(reader, LIST_TYPE);
                if (loaded != null) {
                    for (User user : loaded) {
                        if (user != null && user.username != null) {
                            if (user.userState == null) {
                                user.userState = new UserState(new ArrayList<>(), 0, 0, 0);
                            }
                            user.stayLoggedIn = false;
                            users.add(user);
                        }
                    }
                }
                Log.info("AccountStore", "Loaded " + users.size() + " account(s).");
            } catch (Exception e) {
                Log.error("AccountStore", "Could not read " + file.getAbsolutePath(), e);
            }
        }
    }

    public void flushIfDirty() {
        if (!dirty) return;
        saveNow();
    }

    public void saveNow() {
        synchronized (lock) {
            dirty = false;
            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(users, writer);
            } catch (IOException e) {
                Log.error("AccountStore", "Could not write " + file.getAbsolutePath(), e);
            }
        }
    }

    private void markDirty() {
        dirty = true;
    }

    public User find(String username) {
        if (username == null) return null;
        synchronized (lock) {
            for (User user : users) {
                if (user.username.equalsIgnoreCase(username)) return user;
            }
            return null;
        }
    }

    public boolean exists(String username) {
        return find(username) != null;
    }

    public String register(String username, String password, String nickname, String email,
                           String gender, String securityQuestion, String securityAnswer) {
        String error = AccountValidation.registrationError(username, password, nickname, email, gender);
        if (error != null) return error;
        synchronized (lock) {
            if (exists(username)) return "Username already exists. Please choose a different one.";
            User user = new User(username, password, nickname, email, gender);
            if (securityQuestion != null && securityAnswer != null) {
                user.setSecurityQuestion(securityQuestion, securityAnswer);
            }
            users.add(user);
            markDirty();
        }
        saveNow();
        return null;
    }

    public User authenticate(String username, String password) {
        User user = find(username);
        if (user == null || password == null || !user.checkPassword(password)) return null;
        return user;
    }

    public void replaceState(String username, UserState state) {
        if (state == null) return;
        synchronized (lock) {
            User user = find(username);
            if (user == null) return;
            user.userState = state;
            markDirty();
        }
    }

    public UserState stateOf(String username) {
        User user = find(username);
        return user == null ? null : user.userState;
    }

    public String submitBonusScore(String username, int score) {
        synchronized (lock) {
            User user = find(username);
            if (user == null) return null;
            Integer best = user.userState.bonusHighScore;
            if (best == null || score > best) {
                user.userState.bonusHighScore = score;
                markDirty();
            }
            return null;
        }
    }

    public Integer bonusScoreOf(String username) {
        User user = find(username);
        return user == null ? null : user.userState.bonusHighScore;
    }

    public boolean rename(String oldUsername, String newUsername) {
        synchronized (lock) {
            if (exists(newUsername)) return false;
            User user = find(oldUsername);
            if (user == null) return false;
            user.username = newUsername;
            markDirty();
            return true;
        }
    }

    public void touch() {
        markDirty();
    }

    public List<User> snapshot() {
        synchronized (lock) {
            return new ArrayList<>(users);
        }
    }
}
