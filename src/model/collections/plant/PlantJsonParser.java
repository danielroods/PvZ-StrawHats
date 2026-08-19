package model.collections.plant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import view.GeneralPrinter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlantJsonParser {
    public static class UpgradeConfig {
        public int level;
        public UpgradeType type;
        public double value;
        public String specialTag;
    }

    public static class PlantConfig {
        public int id;
        public String name;
        public model.collections.plant.PlantType category;
        public List<PlantTag> tags;
        public int baseHp;
        public int cost;
        public double actionInterval;
        public int damage;
        public double recharge;
        public double abilityValue;
        public double attackRange;
        public double lifespan;
        public AbilityType abilityType;
        public PlantFoodType plantFoodType;
        public double plantFoodValue;
        public List<UpgradeConfig> upgrades;

        @SerializedName("wramp-up")
        public List<Map<String, Object>> wrampUp;
    }

    private static final JsonDeserializer<PlantTag> TAG_DESERIALIZER = (json, typeOfT, context) -> {
        String raw = json.getAsString();
        return resolveTag(raw);
    };

    private static PlantTag resolveTag(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalized = raw.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        try {
            return PlantTag.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            return PlantTag.valueOf(normalized + "S");
        } catch (IllegalArgumentException ignored) {
        }
        if (normalized.endsWith("S")) {
            try {
                return PlantTag.valueOf(normalized.substring(0, normalized.length() - 1));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return PlantTag.getByName(raw);
    }

    private static Gson buildGson() {
        return new GsonBuilder()
                .registerTypeAdapter(PlantTag.class, TAG_DESERIALIZER)
                .create();
    }

    public static Map<Integer, PlantConfig> loadConfigs(InputStream is) {
        Map<Integer, PlantConfig> result = new HashMap<>();
        if (is == null) return result;
        Gson gson = buildGson();
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<PlantConfig>>() {}.getType();
            List<PlantConfig> configs = gson.fromJson(reader, listType);
            if (configs != null) {
                for (PlantConfig config : configs) {
                    if (config != null) {
                        config.tags = config.tags == null ? List.of() : config.tags.stream()
                                .filter(t -> t != null).toList();
                        result.put(config.id, config);
                    }
                }
            }
        } catch (IOException e) {
            GeneralPrinter.print("Could not load plant data: " + e.getMessage());
        }
        return result;
    }
}
