package net.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.user_data.AccountValidation;
import model.user_data.User;
import model.user_data.UserState;
import net.dto.AccountDto;
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

    public record SyncOutcome(User user, boolean usernameTaken, String error) {

        static SyncOutcome accepted(User user, boolean usernameTaken) {
            return new SyncOutcome(user, usernameTaken, null);
        }

        static SyncOutcome rejected(String error) {
            return new SyncOutcome(null, false, error);
        }

        public boolean isError() {
            return error != null;
        }
    }

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
                            normalise(user);
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

    private static void normalise(User user) {
        user.accountId();
        if (user.userState == null) user.userState = new UserState(new ArrayList<>(), 0, 0, 0);
    }

    public void flushIfDirty() {
        if (!dirty) return;
        saveNow();
    }

    public void saveNow() {
        synchronized (lock) {
            dirty = false;
            mergeFromDisk();
            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(users, writer);
            } catch (IOException e) {
                Log.error("AccountStore", "Could not write " + file.getAbsolutePath(), e);
            }
        }
    }

    private void mergeFromDisk() {
        if (!file.isFile()) return;
        try (Reader reader = new FileReader(file)) {
            ArrayList<User> onDisk = GSON.fromJson(reader, LIST_TYPE);
            if (onDisk == null) return;
            for (User diskUser : onDisk) {
                if (diskUser == null || diskUser.username == null) continue;
                normalise(diskUser);
                if (findById(diskUser.accountId()) == null) users.add(diskUser);
            }
        } catch (Exception e) {
            Log.error("AccountStore", "Could not merge " + file.getAbsolutePath(), e);
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

    public User findById(String accountId) {
        if (accountId == null) return null;
        synchronized (lock) {
            for (User user : users) {
                if (accountId.equals(user.accountId())) return user;
            }
            return null;
        }
    }

    public User locate(String accountId, String username) {
        User user = findById(accountId);
        return user != null ? user : find(username);
    }

    public boolean exists(String username) {
        return find(username) != null;
    }

    public boolean credentialsMatch(User user, AccountDto dto) {
        if (user == null || dto == null) return false;
        if (dto.passwordHash != null && dto.passwordHash.equals(user.passwordHash)) return true;
        return dto.syncedPasswordHash != null
                && dto.syncedPasswordHash.equals(user.passwordHash);
    }

    public SyncOutcome sync(AccountDto dto, UserState incoming) {
        if (dto == null) return SyncOutcome.rejected("Missing account.");
        synchronized (lock) {
            User user = locate(dto.accountId, dto.username);
            if (user == null) return provision(dto, incoming);
            if (incoming == null || !incoming.isNewerThan(user.userState)) {
                return SyncOutcome.accepted(user, false);
            }

            boolean usernameTaken = !canTakeUsername(user, dto.username);
            dto.applyProfileTo(user);
            if (!usernameTaken) user.username = dto.username;
            user.userState = incoming;
            markDirty();
            return SyncOutcome.accepted(user, usernameTaken);
        }
    }

    private boolean canTakeUsername(User user, String requested) {
        if (requested == null || requested.isBlank()) return false;
        if (requested.equalsIgnoreCase(user.username)) return true;
        if (AccountValidation.usernameError(requested) != null) return false;
        User holder = find(requested);
        return holder == null || holder == user;
    }

    private SyncOutcome provision(AccountDto dto, UserState incoming) {
        String error = AccountValidation.usernameError(dto.username);
        if (error == null) error = AccountValidation.nicknameError(dto.nickname);
        if (error == null) error = AccountValidation.emailError(dto.email);
        if (error == null) error = AccountValidation.genderError(dto.gender);
        if (error != null) return SyncOutcome.rejected(error);
        if (dto.passwordHash == null || dto.passwordHash.isBlank()) {
            return SyncOutcome.rejected("Missing account credentials.");
        }
        if (exists(dto.username)) {
            return SyncOutcome.rejected("Username already exists. Please choose a different one.");
        }
        User user = User.withHash(dto.accountId, dto.username, dto.passwordHash,
                dto.nickname, dto.email, dto.gender);
        user.securityQuestion = dto.securityQuestion;
        user.securityAnswerHash = dto.securityAnswerHash;
        if (dto.profilePicture != null && !dto.profilePicture.isEmpty()) {
            user.profilePicture = dto.profilePicture;
        }
        if (incoming != null) user.userState = incoming;
        users.add(user);
        markDirty();
        saveNow();
        return SyncOutcome.accepted(user, false);
    }

    public int addCoins(String username, int amount) {
        if (amount <= 0) return 0;
        synchronized (lock) {
            User user = find(username);
            if (user == null) return 0;
            if (user.userState == null) user.userState = new UserState(new ArrayList<>(), 0, 0, 0);
            user.userState.coins += amount;
            markDirty();
            return user.userState.coins;
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
                user.userState.markSaved();
                markDirty();
            }
            return null;
        }
    }

    public Integer bonusScoreOf(String username) {
        User user = find(username);
        return user == null ? null : user.userState.bonusHighScore;
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
