package model.collections.animations;

import model.utils.ResourceResolver;
import view.GeneralPrinter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Resolves a plant display name (from Plants.json) or a zombie alias (from Zombie.json)
 * to its matching entry in animations.json.
 * <p>
 * animations.json has no single consistent naming rule: most compound names are just
 * concatenated with no separator ("Cherry Bomb" -> CHERRYBOMB), some keep an underscore
 * ("Primal Sunflower" -> PRIMAL_SUNFLOWER, "Primal Potato Mine" -> PRIMAL_POTATOMINE),
 * and a few are genuinely different words (e.g. "Rotobaga" -> ROTORUTABAGA). So resolution
 * works in two passes: (1) a curated override table for the exceptions found by manually
 * cross-referencing every plant/zombie against the animation list, checked first, then
 * (2) a combinatorial fallback that tries every '' / '_' join of the name's word tokens.
 */
public class AnimationFactory {

    private static Map<String, AnimationJsonParser.AnimationConfig> library = new HashMap<>();
    private static Map<String, AnimationJsonParser.AnimationConfig> byPath = new HashMap<>();
    private static boolean loaded = false;

    public static void init(InputStream jsonStream) {
        library = AnimationJsonParser.loadConfigs(jsonStream);
        byPath = new HashMap<>();
        for (AnimationJsonParser.AnimationConfig config : library.values()) {
            if (config.path != null) byPath.put(config.path, config);
        }
        loaded = true;
    }

    public static void autoInit() {
        if (loaded) return;
        try (InputStream is = ResourceResolver.open("pvz-assets/animations.json")) {
            if (is != null) {
                init(is);
                return;
            }
        } catch (java.io.IOException e) {
            GeneralPrinter.print("Could not load animations.json: " + e.getMessage());
        }
        GeneralPrinter.print("Could not find animations.json in any known location.");
    }

    public static Map<String, AnimationJsonParser.AnimationConfig> getLibrary() {
        autoInit();
        return library;
    }

    
    public static AnimationJsonParser.AnimationConfig get(String rawName) {
        if (rawName == null) return null;
        autoInit();
        return library.get(rawName.toUpperCase());
    }

    /**
     * Picks the best clip name to actually use from a resolved config for a requested state,
     * since some PAM files don't have a clip literally named "idle"/"walk"/etc. Tries, in order:
     * (1) an exact match for {@code preferredState}, (2) an exact "idle" clip, (3) an exact
     * "default" clip, (4) any clip whose name contains {@code preferredState} as a substring,
     * (5) any clip whose name contains "idle", (6) whatever clip happens to be first.
     * Returns null only if the config itself is null or has no clips at all.
     * <p>
     * This is the mechanism every resolver in this factory should route through - callers
     * that used {@link #resolveByDisplayName} or {@link #resolveByZombieAlias} don't need to
     * duplicate this fallback chain themselves.
     */
    public static String resolveClipName(AnimationJsonParser.AnimationConfig config, String preferredState) {
        if (config == null || config.clips == null || config.clips.isEmpty()) return null;

        if (preferredState != null && config.clips.containsKey(preferredState)) {
            return preferredState;
        }
        if (preferredState != null && !preferredState.isEmpty()) {
            String byState = firstClipContaining(config, preferredState);
            if (byState != null) return byState;
        }
        if (config.clips.containsKey("idle")) {
            return "idle";
        }
        if (config.clips.containsKey("default")) {
            return "default";
        }
        String byIdle = firstClipContaining(config, "idle");
        if (byIdle != null) return byIdle;

        return config.clips.keySet().iterator().next();
    }

    
    public static String resolveClipNameForPath(String pamPath, String preferredState) {
        if (pamPath == null) return null;
        autoInit();
        return resolveClipName(byPath.get(pamPath), preferredState);
    }

    
    public static float clipDurationForPath(String pamPath, String preferredState) {
        if (pamPath == null) return -1f;
        autoInit();
        AnimationJsonParser.AnimationConfig config = byPath.get(pamPath);
        if (config == null || config.clips == null) return -1f;
        String clipName = resolveClipName(config, preferredState);
        if (clipName == null) return -1f;
        Double duration = config.clips.get(clipName);
        return (duration != null && duration > 0.0) ? duration.floatValue() : -1f;
    }

    public static String exactClipNameForPath(String pamPath, String exactState) {
        if (pamPath == null || exactState == null || exactState.isBlank()) return null;
        autoInit();

        
        
        
        
        
        
        AnimationJsonParser.AnimationConfig config = byPath.get(pamPath);
        if (config == null || config.clips == null || config.clips.isEmpty()) {
            return exactState;
        }
        return config.clips.containsKey(exactState) ? exactState : null;
    }

    public static float exactClipDurationForPath(String pamPath, String exactState) {
        String clipName = exactClipNameForPath(pamPath, exactState);
        if (clipName == null) return -1f;
        AnimationJsonParser.AnimationConfig config = byPath.get(pamPath);
        if (config == null || config.clips == null) return -1f;
        Double duration = config.clips.get(clipName);
        return (duration != null && duration > 0.0) ? duration.floatValue() : -1f;
    }

    public static boolean hasExactClip(String pamPath, String exactState) {
        if (pamPath == null || exactState == null || exactState.isBlank()) return false;
        autoInit();
        AnimationJsonParser.AnimationConfig config = byPath.get(pamPath);
        return config != null && config.clips != null && config.clips.containsKey(exactState);
    }

    private static String firstClipContaining(AnimationJsonParser.AnimationConfig config, String substring) {
        if (config == null || config.clips == null || substring == null) return null;
        String needle = substring.toLowerCase();
        for (String clipName : config.clips.keySet()) {
            if (clipName.toLowerCase().contains(needle)) {
                return clipName;
            }
        }
        return null;
    }

    
    
    

    /**
     * Exceptions found by cross-checking all 69 Plants.json entries against animations.json.
     * A null value means: verified there is genuinely no matching entry in animations.json
     * (not just unresolved by the automatic algorithm) - callers should fall back to a
     * placeholder icon rather than guess.
     */
    private static final Map<String, String> PLANT_NAME_OVERRIDES = new HashMap<>();
    static {
        PLANT_NAME_OVERRIDES.put("ROTOBAGA", "ROTORUTABAGA");
        PLANT_NAME_OVERRIDES.put("MEGA_GATLING_PEA", "MEGAGATLING");
        PLANT_NAME_OVERRIDES.put("ICEBERG_LETTUCE", "HEADBUTTER_LETTUCE");
        PLANT_NAME_OVERRIDES.put("PHAT_BEET", "PHATBEETS");
        
        PLANT_NAME_OVERRIDES.put("PIERCE_MINT", "SPEARMINT");
        
        PLANT_NAME_OVERRIDES.put("KERNEL_PULT", "KERNALPULT");
        
        PLANT_NAME_OVERRIDES.put("CAT_TAIL", "HOMINGTHISTLE");

        PLANT_NAME_OVERRIDES.put("CATTAIL_MINT", "CONCEALMINT");
    }

    /**
     * Best-effort lookup for a plant display name (e.g. "Sun-shroom", "Twin Sunflower").
     * Checks {@link #PLANT_NAME_OVERRIDES} first, then falls back to combinatorial matching.
     * Returns null if nothing matched (including for entries verified as genuinely absent).
     */
    public static AnimationJsonParser.AnimationConfig resolveByDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) return null;
        autoInit();

        String key = normalizeKey(displayName);
        if (PLANT_NAME_OVERRIDES.containsKey(key)) {
            String override = PLANT_NAME_OVERRIDES.get(key);
            return override == null ? null : library.get(override);
        }

        String[] tokens = splitWords(displayName);
        for (String candidate : joinVariants(tokens)) {
            if (library.containsKey(candidate)) return library.get(candidate);
        }
        return null;
    }

    public static String firstAvailableClipState(String displayName, String... preferredStates) {
        AnimationJsonParser.AnimationConfig config = resolveByDisplayName(displayName);
        if (config == null || config.clips == null || preferredStates == null) return null;
        for (String state : preferredStates) {
            if (state == null || state.isBlank()) continue;
            if (config.clips.containsKey(state)) return state;
            if (firstClipContaining(config, state) != null) return state;
        }
        return null;
    }

    public static float clipDurationForDisplayName(String displayName, String preferredState) {
        AnimationJsonParser.AnimationConfig config = resolveByDisplayName(displayName);
        if (config == null || config.clips == null) return -1f;
        String clipName = resolveClipName(config, preferredState);
        if (clipName == null) return -1f;
        Double duration = config.clips.get(clipName);
        return (duration != null && duration > 0.0) ? duration.floatValue() : -1f;
    }

    
    public static String pathForDisplayName(String displayName) {
        AnimationJsonParser.AnimationConfig config = resolveByDisplayName(displayName);
        return config == null ? null : config.path;
    }

    
    
    

    /**
     * Exceptions found by cross-checking all 31 Zombie.json aliases against animations.json.
     * A null value means the zombie's visuals are composited at runtime in the original game
     * (a base body plus a separately-worn armor piece) and animations.json has no single
     * matching entry for that combination - render the base zombie body and layer your own
     * armor sprite/state on top instead of expecting one PAM clip to cover it.
     */
    private static final Map<String, String> ZOMBIE_ALIAS_OVERRIDES = new HashMap<>();
    static {
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_DEFAULT", "ZOMBIE_TUTORIAL");
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_IMP", "ZOMBIE_IMP_BARE");
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_RA", "ZOMBIE_EGYPT_RA");
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_TOMB_RAISER", "ZOMBIE_EGYPT_TOMBRAISER");
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_ICE_AGE_DODO", "ZOMBIE_ICEAGE_DODORIDER");
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_BEACH_SNORKEL", "ZOMBIE_BEACH_SNORKELER");
        
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_DARK_JUGGLER", "ZOMBIE_DARK_JESTER");
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_WIZARD", "ZOMBIE_DARK_WIZARD");
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_CRYSTAL_SKULL", "ZOMBIE_LOSTCITY_CRYSTALSKULL");
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_NEWSPAPER", "ZOMBIE_MODERN_NEWSPAPER");
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_ARCADE", "ZOMBIE_80S_ARCADE");
        
        
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_ARMOR_1", null);      
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_ARMOR_2", null);      
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_ARMOR_4", null);      
        ZOMBIE_ALIAS_OVERRIDES.put("ZOMBIE_DARK_ARMOR_3", null); 
    }

    /**
     * Best-effort lookup for a Zombie.json alias (e.g. "ZombieIceAgeTroglobite", "ZombiePeashooter").
     * Checks {@link #ZOMBIE_ALIAS_OVERRIDES} first, then falls back to combinatorial matching
     * both with and without the "Zombie" prefix (some entries carry it, some don't - e.g. the
     * costume zombies "ZombiePeashooter"/"ZombieWallnut" reuse the bare plant animation).
     * Returns null if nothing matched (including verified-absent armor overlays).
     */
    public static AnimationJsonParser.AnimationConfig resolveByZombieAlias(String alias) {
        if (alias == null || alias.isBlank()) return null;
        autoInit();

        String key = normalizeKey(splitCamelCase(alias));
        if (ZOMBIE_ALIAS_OVERRIDES.containsKey(key)) {
            String override = ZOMBIE_ALIAS_OVERRIDES.get(key);
            return override == null ? null : library.get(override);
        }

        String[] tokens = camelTokens(alias);
        String[] bodyTokens = (tokens.length > 0 && tokens[0].equalsIgnoreCase("zombie"))
                ? java.util.Arrays.copyOfRange(tokens, 1, tokens.length)
                : tokens;

        List<String> candidates = new ArrayList<>();
        candidates.addAll(joinVariants(tokens));                                 
        candidates.addAll(joinVariants(bodyTokens));                             
        for (String v : joinVariants(bodyTokens)) candidates.add("ZOMBIE_" + v); 

        for (String candidate : candidates) {
            if (library.containsKey(candidate)) return library.get(candidate);
        }
        return null;
    }

    
    public static String pathForZombieAlias(String alias) {
        AnimationJsonParser.AnimationConfig config = resolveByZombieAlias(alias);
        return config == null ? null : config.path;
    }

    
    
    

    private static final Pattern WORD_SPLIT = Pattern.compile("[ \\-]+");
    private static final Pattern CAMEL_SPLIT = Pattern.compile("(?=[A-Z])");

    private static String[] splitWords(String raw) {
        String cleaned = raw.trim().replaceAll("[^A-Za-z0-9 \\-]", " ");
        return WORD_SPLIT.split(cleaned.trim());
    }

    private static String[] camelTokens(String alias) {
        String[] parts = CAMEL_SPLIT.split(alias);
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            if (!p.isBlank()) tokens.add(p);
        }
        return tokens.toArray(new String[0]);
    }

    private static String splitCamelCase(String alias) {
        return String.join(" ", camelTokens(alias));
    }

    
    private static String normalizeKey(String raw) {
        String[] tokens = splitWords(raw);
        return String.join("_", tokens).toUpperCase();
    }

    /**
     * All '' / '_' join combinations of the given tokens, uppercased, plus (for exactly
     * two tokens) both orders - covers cases like "Twin Sunflower" -> SUNFLOWER_TWIN.
     * Capped at 5 tokens to keep the combinatorics small; no plant/zombie name needs more.
     */
    private static Set<String> joinVariants(String[] tokens) {
        Set<String> variants = new HashSet<>();
        if (tokens.length == 0) return variants;

        String[] upper = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) upper[i] = tokens[i].toUpperCase();

        if (upper.length == 1) {
            variants.add(upper[0]);
            return variants;
        }
        if (upper.length > 5) {
            variants.add(String.join("", upper));
            variants.add(String.join("_", upper));
            return variants;
        }

        int combos = 1 << (upper.length - 1);
        for (int mask = 0; mask < combos; mask++) {
            StringBuilder sb = new StringBuilder(upper[0]);
            for (int i = 1; i < upper.length; i++) {
                sb.append(((mask >> (i - 1)) & 1) == 1 ? "_" : "").append(upper[i]);
            }
            variants.add(sb.toString());
        }

        if (upper.length == 2) {
            variants.add(upper[1] + "_" + upper[0]);
            variants.add(upper[1] + upper[0]);
        }
        return variants;
    }
}