package model.user_data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.greenhouse.Greenhouse;
import view.GeneralPrinter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class LocalUserStore implements UserStore {
    private static final File CLIENT_FILE = new File("client-data", "Data.json");
    private static final File[] LEGACY_FILES = {
        new File("server-data", "Data.json"),
        new File("Data.json"),
    };
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private File accountFile() {
        File parent = CLIENT_FILE.getParentFile();
        if (!parent.exists()) parent.mkdirs();
        if (!CLIENT_FILE.exists()) migrateLegacyFile();
        return CLIENT_FILE;
    }

    private void migrateLegacyFile() {
        for (File legacy : LEGACY_FILES) {
            if (!legacy.isFile() || legacy.length() == 0) continue;
            try (Reader reader = new FileReader(legacy)) {
                ArrayList<User> loaded = GSON.fromJson(reader, listType());
                try (Writer writer = new FileWriter(CLIENT_FILE)) {
                    GSON.toJson(loaded == null ? new ArrayList<User>() : loaded, writer);
                }
                return;
            } catch (Exception e) {
                GeneralPrinter.print("Could not migrate offline accounts: " + e.getMessage());
            }
        }
    }

    private static Type listType() {
        return new TypeToken<ArrayList<User>>() { }.getType();
    }

    @Override public void load() {
        File file = accountFile();
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            ArrayList<User> loaded = GSON.fromJson(reader, listType());
            User.users = loaded == null ? new ArrayList<>() : loaded;
            for (User user : User.users) normalise(user);
            for (User user : User.users) if (user.stayLoggedIn) setUser(user);
        } catch (IOException e) { GeneralPrinter.print("Could not load users: " + e.getMessage()); }
    }

    @Override public void save() {
        User current = User.currentUser;
        if (current != null && current.userState != null) {
            current.userState.greenhousePots = Greenhouse.getInstance().serialize();
            current.userState.markSaved();
        }
        mirror();
    }

   public void mirror() {
        mergeFromDisk();
        writeAtomically();
    }

    private void writeAtomically() {
        File target = accountFile();
        File tmp = null;
        try {
            File parent = target.getAbsoluteFile().getParentFile();
            tmp = File.createTempFile("Data", ".json.tmp", parent);
            try (Writer writer = new FileWriter(tmp)) { GSON.toJson(User.users, writer); }
            moveIntoPlace(tmp, target);
            tmp = null;
        } catch (IOException e) {
            GeneralPrinter.print("Could not save users: " + e.getMessage());
        } finally {
            if (tmp != null && tmp.exists() && !tmp.delete()) tmp.deleteOnExit();
        }
    }

    private void moveIntoPlace(File tmp, File target) throws IOException {
        try {
            java.nio.file.Files.move(tmp.toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailed) {
            java.nio.file.Files.move(tmp.toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void mergeFromDisk() {
        File file = accountFile();
        if (!file.isFile()) return;
        try (Reader reader = new FileReader(file)) {
            ArrayList<User> onDisk = GSON.fromJson(reader, listType());
            if (onDisk == null) return;
            for (User diskUser : onDisk) {
                if (diskUser == null || diskUser.username == null) continue;
                normalise(diskUser);
                if (findById(diskUser.accountId()) == null) User.users.add(diskUser);
            }
        } catch (Exception e) {
            GeneralPrinter.print("Could not merge users: " + e.getMessage());
        }
    }

    private static void normalise(User user) {
        if (user == null) return;
        user.accountId();
        if (user.userState == null) user.userState = new UserState(new ArrayList<>(), 0, 0, 0);
    }

    public User findById(String accountId) {
        if (accountId == null) return null;
        for (User user : User.users) if (accountId.equals(user.accountId())) return user;
        return null;
    }

    @Override public void addUser(User user) { User.users.add(user); save(); }
    @Override public User findByUsername(String username) {
        if (username == null) return null;
        for (User user : User.users) if (username.equalsIgnoreCase(user.username)) return user;
        return null;
    }
    @Override public boolean usernameExists(String username) { return findByUsername(username) != null; }
    @Override public void setUser(User user) {
        User.currentUser = user;
        model.App.currentUser = user;
        Greenhouse.getInstance().load(user.userState.greenhousePots);
        model.quests.QuestLoader.initializeActiveQuestsForUser();
    }
}
