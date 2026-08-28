package net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public final class JsonLine {

    public static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private JsonLine() {
    }

    public static String encode(Envelope envelope) {
        return GSON.toJson(envelope);
    }

    public static Envelope decode(String line) {
        if (line == null) return null;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return null;
        Envelope envelope = GSON.fromJson(trimmed, Envelope.class);
        if (envelope == null || envelope.t == null) return null;
        if (envelope.d == null) envelope.d = new JsonObject();
        return envelope;
    }

    public static JsonObject toTree(Object value) {
        return GSON.toJsonTree(value).getAsJsonObject();
    }

    public static <T> T fromTree(JsonObject json, Class<T> type) {
        return json == null ? null : GSON.fromJson(json, type);
    }
}
