package model.collections.plant;

import model.user_data.User;
import model.user_data.UserState;
import model.utils.ResourceResolver;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Owns/picks plant costumes for the currently signed-in account. */
public final class PlantCostumeManager {
    public static final int COST = 5000;

    private PlantCostumeManager() {
    }

    public static List<PlantCostume> costumesForPlant(String plantName) {
        return PlantCostumeRegistry.costumesForPlant(plantName);
    }

    public static boolean hasCostumes(String plantName) {
        return PlantCostumeRegistry.hasCostumes(plantName);
    }

    public static boolean isOwned(int plantId, String costumeId) {
        if (costumeId == null || costumeId.isBlank()) return false;
        UserState state = state();
        return state != null && state.ownedPlantCostumes()
                .getOrDefault(plantId, Collections.emptySet())
                .contains(costumeId);
    }

    /** Null/blank means the original/default plant art. */
    /** Name-based lookup for non-Plant instances such as the board drag preview. */
    public static String selectedCostumeForPlantName(String plantName) {
        if (plantName == null || plantName.isBlank()) return null;
        UserState state = state();
        if (state == null) return null;
        Integer id = PlantIds.idForName(plantName);
        return id == null ? null : state.getSelectedPlantCostume(id);
    }

    public static String selectedCostume(int plantId) {
        UserState state = state();
        if (state == null || state.selectedPlantCostumes == null) return null;
        String selected = state.selectedPlantCostumes.get(plantId);
        return selected == null || selected.isBlank() ? null : selected;
    }

    /** Null clears the costume and restores the original plant art. */
    public static boolean choose(int plantId, String plantName, String costumeId) {
        UserState state = state();
        if (state == null || !state.isPlantUnlocked(plantId)) return false;

        if (costumeId == null || costumeId.isBlank()) {
            state.setSelectedPlantCostume(plantId, null);
        } else {
            PlantCostume costume = PlantCostumeRegistry.find(plantName, costumeId);
            if (costume == null || !isOwned(plantId, costume.id)) return false;
            state.setSelectedPlantCostume(plantId, costume.id);
        }
        User.save();
        return true;
    }

    public static boolean purchase(int plantId, String plantName, String costumeId) {
        UserState state = state();
        if (state == null || !state.isPlantUnlocked(plantId)) return false;
        PlantCostume costume = PlantCostumeRegistry.find(plantName, costumeId);
        if (costume == null || isOwned(plantId, costume.id) || state.coins < COST) return false;

        state.coins -= COST;
        state.addOwnedPlantCostume(plantId, costume.id);
        User.save();
        return true;
    }

    public static Set<String> ownedForPlant(int plantId) {
        UserState state = state();
        if (state == null) return Collections.emptySet();
        return state.ownedPlantCostumes()
                .getOrDefault(plantId, Collections.emptySet());
    }

    private static UserState state() {
        return User.currentUser == null ? null : User.currentUser.userState;
    }
    private static final class PlantIds {
        private static final Map<String, Integer> BY_NAME = load();

        private static Map<String, Integer> load() {
            Map<String, Integer> result = new HashMap<>();
            try (InputStream is = ResourceResolver.open("Plants.json")) {
                Map<Integer, PlantJsonParser.PlantConfig> configs = PlantJsonParser.loadConfigs(is);
                for (PlantJsonParser.PlantConfig config : configs.values()) {
                    if (config != null && config.name != null) {
                        result.put(config.name.trim().toLowerCase(Locale.ROOT), config.id);
                    }
                }
            } catch (Exception e) {
                System.err.println("Could not resolve plant ids for costumes: " + e.getMessage());
            }
            return result;
        }

        static Integer idForName(String name) {
            return BY_NAME.get(name.trim().toLowerCase(Locale.ROOT));
        }
    }

}
