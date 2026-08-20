package view.general_screens;

import model.collections.armour.Armour;
import model.collections.armour.ArmourType;
import model.collections.armour.ZombieArmour;
import model.collections.zombie.Zombie;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

class ZombieArmorMask {

    private static final Map<ArmourType, String[]> ARMOUR_ELEMENTS = new LinkedHashMap<>();
    private static final Map<ArmourType, String[]> ARMOUR_CONTAINERS = new LinkedHashMap<>();
    static {
        ARMOUR_ELEMENTS.put(ArmourType.CONE, new String[] {
                "zombie_armor_cone_norm", "zombie_armor_cone_damage_01",
                "zombie_armor_cone_damage_02",
        });
        ARMOUR_ELEMENTS.put(ArmourType.BUCKET, new String[] {
                "zombie_armor_bucket_norm", "zombie_armor_bucket_damage_01",
                "zombie_armor_bucket_damage_02",
        });
        ARMOUR_ELEMENTS.put(ArmourType.BRICK, new String[] {
                "zombie_armor_brick_norm", "zombie_armor_brick_damage_01",
                "zombie_armor_brick_damage_02",
        });
        ARMOUR_ELEMENTS.put(ArmourType.CROWN, new String[] {
                "zombie_armor_crown_norm", "zombie_armor_crown_damage_01",
                "zombie_armor_crown_damage_02",
        });
        ARMOUR_ELEMENTS.put(ArmourType.SHOULDER_ARMOR, new String[] {
                "zombie_shoulder_armor_norm", "zombie_shoulder_armor_damage_01",
                "zombie_shoulder_armor_damage_02",
        });

        // Some chapters wrap the armour states in a container element that is itself
        // hidden by default; its children are unreachable unless the container is shown too.
        ARMOUR_CONTAINERS.put(ArmourType.CONE, new String[] { "_zombie_egypt_armor1_states" });
        ARMOUR_CONTAINERS.put(ArmourType.BUCKET, new String[] { "_zombie_egypt_armor2_states" });
        ARMOUR_CONTAINERS.put(ArmourType.CROWN, new String[] {
                "_zombie_armor_crown_states", "zombie_shoulder_armor",
        });
    }

    Map<String, Boolean> basicZombieArmorVisibility(Zombie zombie) {
        if (zombie == null) return null;
        Armour armour = zombie.getArmour();
        if (!(armour instanceof ZombieArmour zombieArmour)) return null;

        ArmourType type = zombieArmour.getArmorType();
        String[] elements = ARMOUR_ELEMENTS.get(type);
        if (elements == null) return null;

        Map<String, Boolean> visibility = new HashMap<>();
        boolean destroyed = zombieArmour.isDestroyed();
        int layer = Math.min(elements.length - 1, Math.max(0, zombieArmour.getDamageLayer()));
        for (int i = 0; i < elements.length; i++) {
            visibility.put(elements[i], !destroyed && i == layer);
        }
        String[] containers = ARMOUR_CONTAINERS.get(type);
        if (containers != null) {
            for (String container : containers) {
                visibility.put(container, !destroyed);
            }
        }
        if (type == ArmourType.CROWN) {
            String[] shoulder = ARMOUR_ELEMENTS.get(ArmourType.SHOULDER_ARMOR);
            for (int i = 0; i < shoulder.length; i++) {
                visibility.put(shoulder[i], !destroyed && i == layer);
            }
        }
        return visibility;
    }
}
