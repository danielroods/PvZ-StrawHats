package net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public static JsonObject compactTree(Object value) {
        JsonObject tree = toTree(value);
        stripDefaults(tree);
        return tree;
    }

    private static void stripDefaults(JsonObject object) {
        List<String> drop = new ArrayList<>();
        for (Map.Entry<String, JsonElement> member : object.entrySet()) {
            JsonElement element = member.getValue();
            if (element.isJsonObject()) stripDefaults(element.getAsJsonObject());
            if (element.isJsonArray()) stripDefaults(element.getAsJsonArray());
            if (isDefault(element)) drop.add(member.getKey());
        }
        for (String key : drop) {
            object.remove(key);
        }
    }

    private static void stripDefaults(JsonArray array) {
        for (JsonElement element : array) {
            if (element.isJsonObject()) stripDefaults(element.getAsJsonObject());
            if (element.isJsonArray()) stripDefaults(element.getAsJsonArray());
        }
    }

    private static boolean isDefault(JsonElement element) {
        if (element.isJsonNull()) return true;
        if (element.isJsonArray()) return element.getAsJsonArray().isEmpty();
        if (!element.isJsonPrimitive()) return false;
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) return !primitive.getAsBoolean();
        if (primitive.isNumber()) return primitive.getAsDouble() == 0.0;
        return false;
    }

    public static <T> T fromTree(JsonObject json, Class<T> type) {
        return json == null ? null : GSON.fromJson(json, type);
    }
}
