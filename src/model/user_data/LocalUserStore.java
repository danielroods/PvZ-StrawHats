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

    private static final String SAVE_FILE = "Data.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void load() {
        File file = new File(SAVE_FILE);
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<User>>() { }.getType();
            ArrayList<User> loaded = GSON.fromJson(reader, listType);
            if (loaded != null) User.users = loaded;
            for (User user : User.users) {
                if (user.stayLoggedIn) setUser(user);
            }
        } catch (IOException e) {
            GeneralPrinter.print("Could not load users: " + e.getMessage());
        }
    }

    @Override
    public void save() {
        if (User.currentUser != null) {
            User.currentUser.userState.greenhousePots = Greenhouse.getInstance().serialize();
        }
        try (Writer writer = new FileWriter(SAVE_FILE)) {
            GSON.toJson(User.users, writer);
        } catch (IOException e) {
            GeneralPrinter.print("Could not save users: " + e.getMessage());
        }
    }

    @Override
    public void addUser(User user) {
        User.users.add(user);
        save();
    }

    @Override
    public User findByUsername(String username) {
        for (User user : User.users) {
            if (user.username.equals(username)) return user;
        }
        return null;
    }

    @Override
    public boolean usernameExists(String username) {
        return findByUsername(username) != null;
    }

    @Override
    public void setUser(User user) {
        User.currentUser = user;
        model.App.currentUser = user;
        Greenhouse.getInstance().load(user.userState.greenhousePots);
        model.quests.QuestLoader.initializeActiveQuestsForUser();
    }
}
