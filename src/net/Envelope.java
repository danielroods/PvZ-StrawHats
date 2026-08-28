package net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class Envelope {

    public String t;
    public long id;
    public Long re;
    public JsonObject d;

    public Envelope() {
    }

    public Envelope(String type, long id, Long replyTo, JsonObject payload) {
        this.t = type;
        this.id = id;
        this.re = replyTo;
        this.d = payload == null ? new JsonObject() : payload;
    }

    public JsonObject payload() {
        return d == null ? new JsonObject() : d;
    }

    public boolean isType(String type) {
        return type != null && type.equals(t);
    }

    public String getString(String key) {
        JsonElement element = payload().get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    public String getString(String key, String fallback) {
        String value = getString(key);
        return value == null ? fallback : value;
    }

    public int getInt(String key, int fallback) {
        JsonElement element = payload().get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsInt();
    }

    public long getLong(String key, long fallback) {
        JsonElement element = payload().get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsLong();
    }

    public boolean getBoolean(String key, boolean fallback) {
        JsonElement element = payload().get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
    }

    public JsonObject getObject(String key) {
        JsonElement element = payload().get(key);
        return element == null || !element.isJsonObject() ? null : element.getAsJsonObject();
    }

    public static JsonObject obj(Object... keyValuePairs) {
        JsonObject json = new JsonObject();
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            String key = String.valueOf(keyValuePairs[i]);
            Object value = keyValuePairs[i + 1];
            if (value == null) {
                json.add(key, com.google.gson.JsonNull.INSTANCE);
            } else if (value instanceof JsonElement element) {
                json.add(key, element);
            } else if (value instanceof Number number) {
                json.add(key, new JsonPrimitive(number));
            } else if (value instanceof Boolean bool) {
                json.add(key, new JsonPrimitive(bool));
            } else {
                json.add(key, new JsonPrimitive(String.valueOf(value)));
            }
        }
        return json;
    }
}
