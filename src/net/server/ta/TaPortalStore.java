package net.server.ta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.user_data.User;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;


public final class TaPortalStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<Data>() { }.getType();

    private final Object lock = new Object();
    private final File file;
    private final Data data;

    public TaPortalStore(File dataDirectory) {
        if (!dataDirectory.exists()) dataDirectory.mkdirs();
        file = new File(dataDirectory, "TAData.json");
        data = load();
        if (data.taCodes.isEmpty()) {
            data.taCodes.add(new TaCode(hash("TA-ARVIN-2026"), "Arvin", "PUT_ARVIN_EMAIL_HERE@example.com"));
            save();
        }
    }

    private Data load() {
        if (!file.isFile()) return new Data();
        try (Reader reader = new FileReader(file)) {
            Data loaded = GSON.fromJson(reader, DATA_TYPE);
            return loaded == null ? new Data() : loaded;
        } catch (Exception ignored) {
            return new Data();
        }
    }

    private void save() {
        synchronized (lock) {
            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(data, writer);
            } catch (Exception ignored) {
                
            }
        }
    }

    public void addTaCode(String code, String name, String email) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("TA code is required.");
        synchronized (lock) {
            data.taCodes.add(new TaCode(hash(code.trim()),
                    name == null || name.isBlank() ? "TA" : name.trim(),
                    email == null ? "" : email.trim()));
            save();
        }
    }

    public TaCode authenticateCode(String code) {
        String normalized = code == null ? "" : code.trim();
        if (normalized.isEmpty()) return null;
        String hash = hash(normalized);
        synchronized (lock) {
            for (TaCode ta : data.taCodes) {
                if (hash.equals(ta.codeHash)) return ta;
            }
        }
        return null;
    }

    public RewardResult grant(String groupId, String username, double addedScore, String taName) {
        if (groupId == null || groupId.isBlank()) return RewardResult.error("Group ID is required.");
        if (username == null || username.isBlank()) return RewardResult.error("Username is required.");
        if (!Double.isFinite(addedScore) || addedScore < 0.25) {
            return RewardResult.error("Added score must be at least 0.25.");
        }
        synchronized (lock) {
            
            double oldScore = data.groupScores.getOrDefault(groupId, 0.0);
            double newScore = oldScore + addedScore;
            int oldUnits = (int) Math.floor((oldScore + 1e-9) / 0.25);
            int newUnits = (int) Math.floor((newScore + 1e-9) / 0.25);
            int rewardUnits = Math.max(0, newUnits - oldUnits);
            int rewardCoins = rewardUnits * 10000;
            data.groupScores.put(groupId, newScore);
            save();
            return new RewardResult(true, null, oldScore, newScore, rewardUnits, rewardCoins, taName);
        }
    }

    public double groupScore(String groupId) {
        synchronized (lock) {
            return data.groupScores.getOrDefault(groupId, 0.0);
        }
    }

    public void recordTransaction(String groupId, String username, String email, double addedScore, int coins, String taName, String taEmail) {
        synchronized (lock) {
            data.transactions.add(new Transaction(
                    System.currentTimeMillis(), groupId, username, email, addedScore, coins, taName, taEmail));
            save();
        }
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static final class Data {
        public List<TaCode> taCodes = new ArrayList<>();
        public Map<String, Double> groupScores = new HashMap<>();
        public List<Transaction> transactions = new ArrayList<>();
    }

    public record TaCode(String codeHash, String name, String email) { }
    public record Transaction(long timestamp, String groupId, String username, String email, double addedScore, int coins, String taName, String taEmail) { }
    public record RewardResult(boolean success, String error, double oldScore, double newScore,
                               int rewardUnits, int rewardCoins, String taName) {
        public static RewardResult error(String message) {
            return new RewardResult(false, message, 0, 0, 0, 0, null);
        }
    }
}
