package net.client;

import model.greenhouse.Greenhouse;
import model.user_data.User;
import model.user_data.UserStore;

public class RemoteUserStore implements UserStore {

    private final NetworkClient client;

    public RemoteUserStore(NetworkClient client) {
        this.client = client;
    }

    @Override
    public void load() {
    }

    @Override
    public void save() {
        if (User.currentUser != null) {
            User.currentUser.userState.greenhousePots = Greenhouse.getInstance().serialize();
        }
        client.markStateDirty();
    }

    @Override
    public void addUser(User user) {
        User.users.add(user);
    }

    @Override
    public User findByUsername(String username) {
        if (username == null) return null;
        for (User user : User.users) {
            if (username.equals(user.username)) return user;
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
