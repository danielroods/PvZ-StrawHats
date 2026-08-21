package view.screens.match.gameplay.mini_games;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ZombotanyArt {

    public static final String BODY_PAM =
            "768/INITIAL/ZOMBIE/ZOMBIE_TUTORIAL/ZOMBIE_TUTORIAL.PAM";

    private static final Map<String, Boolean> HEADLESS_BODY = buildHeadlessBody();

    private static final Map<String, Head> HEADS = buildHeads();

    public record Head(String pam, String plantName, float scale, float offsetX, float offsetY) {
    }

    private ZombotanyArt() {
    }

    public static boolean isPlantZombie(String alias) {
        return alias != null && HEADS.containsKey(alias);
    }

    public static Head headFor(String alias) {
        return alias == null ? null : HEADS.get(alias);
    }

    public static Map<String, Boolean> headlessBodyMask() {
        return HEADLESS_BODY;
    }

    public static java.util.List<String> allPamPaths() {
        java.util.List<String> paths = new java.util.ArrayList<>();
        paths.add(BODY_PAM);
        for (Head head : HEADS.values()) paths.add(head.pam());
        return paths;
    }

    private static Map<String, Boolean> buildHeadlessBody() {
        Map<String, Boolean> mask = new HashMap<>();
        mask.put("zombie_skull", false);
        mask.put("zombie_jaw", false);
        mask.put("zombie_pupil", false);
        return Map.copyOf(mask);
    }

    private static Map<String, Head> buildHeads() {
        Map<String, Head> heads = new LinkedHashMap<>();
        heads.put("ZombiePeashooter", new Head(
                "768/INITIAL/PLANT/PEASHOOTER/PEASHOOTER.PAM", "Peashooter", 0.62f, 34f, 52f));
        heads.put("ZombieGatlingPea", new Head(
                "768/INITIAL/PLANT/MEGAGATLING/MEGAGATLING.PAM", "Mega Gatling Pea",
                0.62f, 34f, 52f));
        heads.put("ZombieWallnut", new Head(
                "768/INITIAL/PLANT/WALLNUT/WALLNUT.PAM", "Wall-nut", 0.60f, 34f, 50f));
        heads.put("ZombieTallnut", new Head(
                "768/FULL/PLANT/TALLNUT/TALLNUT.PAM", "Tall-nut", 0.50f, 34f, 58f));
        heads.put("ZombieJalapeno", new Head(
                "768/INITIAL/PLANT/JALAPENO/JALAPENO.PAM", "Jalapeno", 0.58f, 34f, 54f));
        heads.put("ZombieSquash", new Head(
                "768/INITIAL/PLANT/SQUASH/SQUASH.PAM", "Squash", 0.58f, 34f, 52f));
        return Map.copyOf(heads);
    }
}
