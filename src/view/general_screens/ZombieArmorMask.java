package view.general_screens;

import model.collections.zombie.Zombie;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Maps the basic-zombie alias to its 4 armor element names, PER CHAPTER -
 * Egypt's PAM art uses different element names than the other 3 chapters,
 * so this is keyed first by a chapter tag (matched the same
 * substring-contains way ZombieAnimationRegistry.pathFor already checks
 * seasonFolder: "egypt", "cave"/"ice"/"frostbite", "beach", "dark"), then
 * by zombie alias. These are NOT a separate PAM asset - they're named
 * sub-elements layered inside the basic zombie's own walk/eat/die clip at
 * the same `path` drawZombies() already resolves below
 * (ZombieAnimationRegistry.pathFor). "ZombieDefault" is the basic zombie
 * every chapter has one of - ZombieArmor1/ZombieArmor2/ZombieArmor4
 * (cone/bucket/brick spawn variants, see ZombieFactory.resolveArmorType)
 * resolve to that exact same clip too (ZombieAnimationRegistry maps all of
 * them to ZOMBIE_TUTORIAL / the same per-season basic path), so
 * "ZombieDefault" is the one key that covers the basic zombie as a
 * character regardless of which armor variant spawned.
 * <p>
 * The base body is drawn as always, PLUS one of these armor elements on
 * top - starting with elements[0] (the intact-armor look). When the zombie
 * takes enough damage we don't fade it, we swap which one is showing:
 * elements[0] gets hidden and elements[1] (a different damaged-armor look)
 * gets revealed, and so on. Once the last element gets hidden, none of them
 * are shown any more - just the plain body clip, i.e. normal damaged-zombie
 * look, no armor left. Chapters/aliases not listed here are drawn with the
 * plain 7-arg drawPam, no mask, same as every other zombie.
 */
class ZombieArmorMask {

    private static final Map<String, Map<String, String[]>> BASIC_ZOMBIE_ARMOR_ELEMENTS_BY_CHAPTER = new java.util.HashMap<>();
    static {
        Map<String, String[]> nonEgypt = new java.util.HashMap<>();
        nonEgypt.put("ZombieDefault", new String[]{
                "zombie_armor_bucket_norm", "zombie_armor_bucket_damage_01", "zombie_armor_bucket_damage_02",
        });
        BASIC_ZOMBIE_ARMOR_ELEMENTS_BY_CHAPTER.put("cave", nonEgypt);
        BASIC_ZOMBIE_ARMOR_ELEMENTS_BY_CHAPTER.put("beach", nonEgypt);
        BASIC_ZOMBIE_ARMOR_ELEMENTS_BY_CHAPTER.put("dark", nonEgypt);

        Map<String, String[]> egypt = new java.util.HashMap<>();
        egypt.put("ZombieDefault", new String[]{ "zombie_armor_brick_norm", "zombie_armor_brick_damage_01", "zombie_armor_brick_damage_02" });
        BASIC_ZOMBIE_ARMOR_ELEMENTS_BY_CHAPTER.put("egypt", egypt);
    }

    private static final float[] ARMOR_STAGE_STEP_THRESHOLDS = { 0.75f, 0.50f, 0.25f};

    private final GameScreen screen;

    // Explicit per-zombie state: which of this zombie's armor elements is
    // currently the one showing. Seeded to 0 (elements[0] revealed) the first
    // time this zombie is drawn - we set that first one true ourselves, it is
    // not derived from HP. Reaches elements.length once the last armor element
    // has been hidden, meaning "no armor element left showing".
    private final Map<Zombie, Integer> armorStageIndex = new IdentityHashMap<>();

    ZombieArmorMask(GameScreen screen) {
        this.screen = screen;
    }

    private String basicZombieArmorChapter() {
        String s = screen.seasonFolder == null ? "" : screen.seasonFolder.toLowerCase().trim();
        if (s.contains("egypt")) return "egypt";
        if (s.contains("beach")) return "beach";
        if (s.contains("dark")) return "dark";
        if (s.contains("cave") || s.contains("ice") || s.contains("frostbite")) return "cave";
        return "cave";
    }

    Map<String, Boolean> basicZombieArmorVisibility(Zombie zombie) {
        Map<String, String[]> chapterElements = BASIC_ZOMBIE_ARMOR_ELEMENTS_BY_CHAPTER.get(basicZombieArmorChapter());
        String[] elements = chapterElements == null ? null : chapterElements.get(zombie.getAlias());
        if (elements == null || allBlank(elements)) return null;

        int revealed = basicZombieArmorStage(zombie, elements);
        if (revealed < 0 || revealed >= elements.length) return null;

        Map<String, Boolean> visibility = new java.util.HashMap<>();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == null || elements[i].isBlank()) continue;
            // Hide every armor element except the one currently revealed -
            // this is the "hide that state and reveal the next state" swap.
            visibility.put(elements[i], i == revealed);
        }
//        if (GameSettings.get().isDebugMode()) {
//            Gdx.app.log("ARMOR_MASK", zombie.getAlias() + " hp=" + zombie.getHp() + "/" + zombie.getMaxHp()
//                    + " revealed=" + revealed + " mask=" + visibility);
//        }
        return visibility;
    }

    private boolean allBlank(String[] elements) {
        for (String element : elements) {
            if (element != null && !element.isBlank()) return false;
        }
        return true;
    }

    /**
     * Returns the index of the armor element currently revealed for this
     * zombie. First call for a given zombie seeds it at 0 - elements[0], the
     * intact-armor look, revealed from the start. Every later call only ever
     * hides the current element and reveals the next one when the zombie's own
     * HP crosses that step's threshold; it never jumps back to an earlier
     * element and never recomputes the reveal from scratch off the raw HP
     * ratio. Once it reaches elements.length, every armor element is hidden and
     * the caller falls back to the plain body clip.
     */
    private int basicZombieArmorStage(Zombie zombie, String[] elements) {
        int revealed = armorStageIndex.computeIfAbsent(zombie, z -> 0);
        if (revealed >= elements.length) return revealed;

        int hp = zombie.getHp();
        int maxHp = Math.max(1, zombie.getMaxHp());
        float ratio = Math.max(0f, Math.min(1f, hp / (float) maxHp));

        // Hide the current element and reveal the next one (possibly stepping
        // more than once, if a single big hit skipped a threshold) while the
        // currently revealed element's own threshold has been crossed.
        while (revealed < ARMOR_STAGE_STEP_THRESHOLDS.length
                && revealed < elements.length
                && ratio <= ARMOR_STAGE_STEP_THRESHOLDS[revealed]) {
            revealed++;
        }
        armorStageIndex.put(zombie, revealed);
        return revealed;
    }

    void pruneDeadZombies() {
        armorStageIndex.keySet().removeIf(z -> !screen.session.getZombies().contains(z));
    }
}
