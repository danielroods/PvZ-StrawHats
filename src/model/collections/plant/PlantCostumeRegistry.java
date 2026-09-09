package model.collections.plant;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.utils.ResourceResolver;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public final class PlantCostumeRegistry {
    private static final String RESOURCE = "PlantCostumes.json";
    private static final Map<String, List<PlantCostume>> BY_PLANT = new LinkedHashMap<>();
    private static boolean initialized;

    private PlantCostumeRegistry() {
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        try (InputStream is = ResourceResolver.open(RESOURCE)) {
            if (is == null) throw new IllegalStateException(RESOURCE + " was not found");
            Type type = new TypeToken<List<PlantCostumeEntry>>() { }.getType();
            List<PlantCostumeEntry> entries = new Gson().fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8), type);
            if (entries == null) return;
            for (PlantCostumeEntry entry : entries) {
                if (entry == null || entry.plant == null || entry.plant.isBlank()) continue;
                List<PlantCostume> costumes = new ArrayList<>();
                if (entry.costumes != null) {
                    for (PlantCostume costume : entry.costumes) {
                        if (costume == null || costume.id == null || costume.id.isBlank()) continue;
                        costumes.add(costume);
                    }
                }
                BY_PLANT.put(key(entry.plant), Collections.unmodifiableList(costumes));
            }
        } catch (Exception e) {
            System.err.println("Could not load PlantCostumes.json: " + e.getMessage());
        }
    }

    public static List<PlantCostume> costumesForPlant(String plantName) {
        init();
        List<PlantCostume> result = BY_PLANT.get(key(plantName));
        return result == null ? Collections.emptyList() : result;
    }

    public static PlantCostume find(String plantName, String costumeId) {
        if (costumeId == null || costumeId.isBlank()) return null;
        for (PlantCostume costume : costumesForPlant(plantName)) {
            if (costumeId.equalsIgnoreCase(costume.id)) return costume;
        }
        return null;
    }

    public static boolean hasCostumes(String plantName) {
        return !costumesForPlant(plantName).isEmpty();
    }

    private static String key(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class PlantCostumeEntry {
        String plant;
        List<PlantCostume> costumes;
    }
}
