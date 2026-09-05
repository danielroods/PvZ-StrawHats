package view.screens.generals;

import model.collections.plant.PlantCostume;
import model.collections.plant.PlantCostumeManager;
import model.collections.plant.PlantCostumeRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds PAM element visibility exactly like the zombie armour mask: every known costume
 * element is explicitly hidden, then the selected costume's element(s) are enabled.
 */
public final class PlantCostumeMask {
    private PlantCostumeMask() {
    }

    static Map<String, Boolean> forPlant(int plantId, String plantName) {
        return forCostume(plantName, PlantCostumeManager.selectedCostume(plantId));
    }

    public static Map<String, Boolean> forCostume(String plantName, String costumeId) {
        List<PlantCostume> costumes = PlantCostumeRegistry.costumesForPlant(plantName);
        if (costumes.isEmpty()) return null;

        Map<String, Boolean> visibility = new HashMap<>();
        for (PlantCostume costume : costumes) {
            for (String element : costume.safeElements()) {
                if (element != null && !element.isBlank()) visibility.put(element, false);
            }
        }

        if (costumeId != null && !costumeId.isBlank()) {
            PlantCostume selected = PlantCostumeRegistry.find(plantName, costumeId);
            if (selected != null) {
                for (String element : selected.safeElements()) {
                    if (element != null && !element.isBlank()) visibility.put(element, true);
                }
            }
        }
        return visibility;
    }

    static Map<String, Boolean> merge(Map<String, Boolean> base, Map<String, Boolean> extra) {
        if (base == null && extra == null) return null;
        Map<String, Boolean> result = new HashMap<>();
        if (base != null) result.putAll(base);
        if (extra != null) result.putAll(extra);
        return result;
    }
}
