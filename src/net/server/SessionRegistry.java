package net.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionRegistry {

    private final Map<String, ClientSession> byUsername = new ConcurrentHashMap<>();

    public boolean bind(String username, ClientSession session) {
        if (username == null || session == null) return false;
        return byUsername.putIfAbsent(username.toLowerCase(), session) == null;
    }

    public void release(ClientSession session) {
        if (session == null) return;
        byUsername.entrySet().removeIf(entry -> entry.getValue() == session);
    }

    public void release(String username) {
        if (username == null) return;
        byUsername.remove(username.toLowerCase());
    }

    public ClientSession find(String username) {
        return username == null ? null : byUsername.get(username.toLowerCase());
    }

    public boolean isOnline(String username) {
        return find(username) != null;
    }

    public List<String> onlineUsernames() {
        List<String> names = new ArrayList<>();
        for (ClientSession session : byUsername.values()) {
            String name = session.getUsername();
            if (name != null) names.add(name);
        }
        return names;
    }

    public List<ClientSession> sessions() {
        return new ArrayList<>(byUsername.values());
    }
}
