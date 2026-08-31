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

    // Note: while connected, the server is the single owner of the shared account
    // file (Data.json). This store must never write to that file directly - doing so
    // raced against the server's own writes and silently dropped progress (levels,
    // plants, seed packets, coins, diamonds, etc.), since whichever process wrote last
    // won and clobbered the other's in-memory state. Progress is only ever persisted
    // by pushing it to the server below; the server is responsible for saving it to
    // disk (see AccountStore / AccountHandlers#statePush), including flushing pending
    // pushes when it shuts down, so nothing is lost whether the server is on or off.
    @Override
    public void save() {
        if (User.currentUser != null) {
            User.currentUser.userState.greenhousePots = Greenhouse.getInstance().serialize();
        }
        client.markStateDirty();
        client.pushStateNow();
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