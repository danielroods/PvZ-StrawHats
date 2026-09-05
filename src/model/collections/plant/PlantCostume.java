package model.collections.plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One visual plant costume. The PAM element names are the elements that are toggled on. */
public final class PlantCostume {
    public String id;
    public List<String> elements = new ArrayList<>();

    public PlantCostume() {
    }

    public PlantCostume(String id, List<String> elements) {
        this.id = id;
        this.elements = elements == null ? new ArrayList<>() : new ArrayList<>(elements);
    }

    public List<String> safeElements() {
        return elements == null ? Collections.emptyList() : elements;
    }
}
