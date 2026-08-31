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

/** Offline store backed by the same canonical account file used by the online server. */
public class LocalUserStore implements UserStore {
    private static final File CANONICAL_FILE = new File("server-data", "Data.json");
    private static final File LEGACY_FILE = new File("Data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private File accountFile() {
        File parent = CANONICAL_FILE.getParentFile();
        if (!parent.exists()) parent.mkdirs();
        if (!CANONICAL_FILE.exists() && LEGACY_FILE.isFile() && LEGACY_FILE.length() > 0) {
            try (Reader reader = new FileReader(LEGACY_FILE)) {
                Type listType = new TypeToken<ArrayList<User>>() { }.getType();
                ArrayList<User> loaded = GSON.fromJson(reader, listType);
                try (Writer writer = new FileWriter(CANONICAL_FILE)) { GSON.toJson(loaded == null ? new ArrayList<User>() : loaded, writer); }
            } catch (Exception e) { GeneralPrinter.print("Could not migrate offline accounts: " + e.getMessage()); }
        }
        return CANONICAL_FILE;
    }

    @Override public void load() {
        File file = accountFile();
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<User>>() { }.getType();
            ArrayList<User> loaded = GSON.fromJson(reader, listType);
            User.users = loaded == null ? new ArrayList<>() : loaded;
            for (User user : User.users) if (user.stayLoggedIn) setUser(user);
        } catch (IOException e) { GeneralPrinter.print("Could not load users: " + e.getMessage()); }
    }

    @Override public void save() {
        if (User.currentUser != null) User.currentUser.userState.greenhousePots = Greenhouse.getInstance().serialize();
        mergeFromDisk();
        writeAtomically();
    }

    /** Writes to a temp file and swaps it into place, so a save interrupted mid-write
     *  can never leave Data.json half-written (a half-written file fails to parse on the
     *  next load() and silently resets every account). */
    private void writeAtomically() {
        File target = accountFile();
        try {
            File parent = target.getAbsoluteFile().getParentFile();
            File tmp = File.createTempFile("Data", ".json.tmp", parent);
            try (Writer writer = new FileWriter(tmp)) { GSON.toJson(User.users, writer); }
            try {
                java.nio.file.Files.move(tmp.toPath(), target.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                java.nio.file.Files.move(tmp.toPath(), target.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            GeneralPrinter.print("Could not save users: " + e.getMessage());
        }
    }

    /** Pulls in any accounts that exist on disk but aren't in memory here yet, so this
     *  save doesn't overwrite progress the online server (or another client) wrote to
     *  the same file since this process last loaded it. */
    private void mergeFromDisk() {
        File file = accountFile();
        if (!file.isFile()) return;
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<User>>() { }.getType();
            ArrayList<User> onDisk = GSON.fromJson(reader, listType);
            if (onDisk == null) return;
            for (User diskUser : onDisk) {
                if (diskUser == null || diskUser.username == null) continue;
                boolean known = false;
                for (User memUser : User.users) {
                    if (memUser.username.equalsIgnoreCase(diskUser.username)) { known = true; break; }
                }
                if (!known) User.users.add(diskUser);
            }
        } catch (Exception e) {
            GeneralPrinter.print("Could not merge users: " + e.getMessage());
        }
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