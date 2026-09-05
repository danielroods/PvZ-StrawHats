package view.screens.generals;

import model.collections.plant.PlantCostume;
import model.collections.plant.PlantCostumeManager;
import model.collections.plant.PlantCostumeRegistry;
import pvz.libpvz.pam.PamPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds PAM element visibility for plant costumes. Costume element names are matched by
 * name only; their position in the PAM hierarchy is irrelevant. When a requested element
 * lives below one or more hidden containers, all of its ancestors are automatically made
 * visible so the libPVZ renderer can reach the requested element.
 */
public final class PlantCostumeMask {
    private static final Map<String, Map<String, Set<String>>> ANCESTORS_BY_PAM =
            new ConcurrentHashMap<>();

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

    /**
     * Expands every explicitly-visible element with all of its PAM hierarchy ancestors.
     * This is intentionally name-based and contains no plant/container hardcoding.
     *
     * A PAM may place e.g. {@code custom_01} directly in the root, under {@code _custom},
     * or several containers deep. libPVZ skips a hidden parent before it ever visits its
     * children, so the ancestors must also be marked visible.
     */
    public static Map<String, Boolean> expandHierarchy(PamPlayer pamPlayer, String pamPath,
                                                       Map<String, Boolean> visibility) {
        if (visibility == null || visibility.isEmpty() || pamPlayer == null ||
                pamPath == null || pamPath.isBlank()) {
            return visibility;
        }

        Map<String, Set<String>> ancestors = ANCESTORS_BY_PAM.computeIfAbsent(
                pamPath, key -> buildAncestorMap(pamPlayer, key));
        if (ancestors.isEmpty()) return visibility;

        Map<String, Boolean> expanded = new HashMap<>(visibility);
        for (Map.Entry<String, Boolean> entry : visibility.entrySet()) {
            if (!Boolean.TRUE.equals(entry.getValue())) continue;
            Set<String> parents = ancestors.get(entry.getKey());
            if (parents == null) continue;
            for (String parent : parents) {
                if (parent != null && !parent.isBlank()) {
                    // Never override an explicit false entry. This keeps costume elements
                    // that are deliberately hidden from being re-enabled merely because
                    // they happen to be an ancestor of another requested element.
                    expanded.putIfAbsent(parent, true);
                }
            }
        }
        return expanded;
    }

    private static Map<String, Set<String>> buildAncestorMap(PamPlayer pamPlayer, String pamPath) {
        try {
            PamPlayer.AnimationPart root = pamPlayer.getParts(pamPath);
            if (root == null) return Collections.emptyMap();

            Map<String, Set<String>> result = new HashMap<>();
            collectAncestors(root, new ArrayList<>(), result);
            return result;
        } catch (Throwable ignored) {
            // If the hierarchy cannot be inspected, preserve the original visibility map.
            return Collections.emptyMap();
        }
    }

    private static void collectAncestors(PamPlayer.AnimationPart part, List<String> parents,
                                         Map<String, Set<String>> result) {
        if (part == null) return;

        if (part.name != null && !part.name.isBlank()) {
            result.computeIfAbsent(part.name, key -> new HashSet<>())
                    .addAll(parents);
        }

        List<String> nextParents = parents;
        if (part.name != null && !part.name.isBlank()) {
            nextParents = new ArrayList<>(parents);
            nextParents.add(part.name);
        }

        if (part.children == null) return;
        for (PamPlayer.AnimationPart child : part.children) {
            collectAncestors(child, nextParents, result);
        }
    }

    static Map<String, Boolean> merge(Map<String, Boolean> base, Map<String, Boolean> extra) {
        if (base == null && extra == null) return null;
        Map<String, Boolean> result = new HashMap<>();
        if (base != null) result.putAll(base);
        if (extra != null) result.putAll(extra);
        return result;
    }
}
