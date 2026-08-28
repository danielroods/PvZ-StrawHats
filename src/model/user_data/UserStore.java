package model.user_data;

public interface UserStore {

    void load();

    void save();

    void addUser(User user);

    User findByUsername(String username);

    boolean usernameExists(String username);

    void setUser(User user);
}
