package service.card_factory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builds {@link ZombieIconCard}s from assets/images/ui/zombies_ui, the zombie equivalent
 * of {@link SeedPacketCardFactory} (which does the same thing for assets/images/ui/plants_ui).
 * <p>
 * The pack has 48 icons but Zombie.json only defines 31 aliases, and several of those
 * (the base ZombieDefault/Armor1/2/4/Gargantuar/Imp) have no world tag of their own while
 * every matching icon here is world-specific - see the comments on {@link #ALIAS_ICON_OVERRIDES}
 * for the reasoning behind each guess. The remaining ~17 icons (dino_flag, summer_flag,
 * pirate_barrel, egypt_gargantuar, etc.) don't correspond to any alias currently in
 * Zombie.json and are simply unused.
 */
public class ZombieIconCardFactory implements Disposable {

    /** {@link ZombieIconCard#getIconFile()} value used for cards built by {@link #buildPlaceholderCard}. */
    public static final String PLACEHOLDER_ICON_FILE = "(placeholder)";

    private static final String ZOMBIES_UI_DIR = "assets/images/ui/zombies_ui/";

    private static final float CARD_WIDTH = 135f;
    private static final float CARD_HEIGHT = 190f;

    // Every icon file present in assets/images/ui/zombies_ui.
    private static final String[] ZOMBIE_ICON_FILES = {
            "ra.png", "dark.png", "mummy.png", "piano.png", "iceage.png", "dark_imp.png", "dino_imp.png",
            "explorer.png", "beach_fem.png", "beach_imp.png", "dark_king.png", "dino_flag.png", "egypt_imp.png",
            "mummy_flag.png", "prospector.png", "dark_armor1.png", "dark_armor2.png", "dark_armor3.png",
            "dark_wizard.png", "iceage_dodo.png", "summer_flag.png", "tomb_raiser.png", "barrelroller.png",
            "dark_juggler.png", "mummy_armor1.png", "mummy_armor2.png", "mummy_armor4.png", "beach_octopus.png",
            "beach_snorkel.png", "iceage_armor1.png", "iceage_armor2.png", "iceage_hunter.png", "lostcity_jane.png",
            "pirate_barrel.png", "modern_allstar.png", "beach_fisherman.png", "dark_gargantuar.png",
            "dark_imp_dragon.png", "dino_gargantuar.png", "eighties_arcade.png", "beach_fem_armor1.png",
            "beach_fem_armor2.png", "beach_gargantuar.png", "egypt_gargantuar.png", "modern_newspaper.png",
            "dark_flag_veteran.png", "iceage_troglobite.png", "lostcity_crystalskull.png",
            "tutorial.png", "tutorial_armor1.png", "tutorial_armor2.png", "tutorial_armor4.png",
            "tutorial_imp.png", "tutorial_gargantuar.png"
    };

    // Zombie.json alias (exact) -> icon file, for cases the automatic matching below
    // can't be expected to get right on its own (mirrors SeedPacketCardFactory's
    // DISPLAY_NAME_ICON_OVERRIDES). Add entries here as you find mismatches.
    private static final Map<String, String> ALIAS_ICON_OVERRIDES = new HashMap<>();
    static {
        // "IceAge" is one merged word in the file names (ICEAGE_*) but two camelCase
        // tokens in the alias (Ice + Age), so the automatic ICE_AGE_* candidate misses.
        ALIAS_ICON_OVERRIDES.put("ZombieIceAgeDodo", "iceage_dodo.png");
        ALIAS_ICON_OVERRIDES.put("ZombieIceAgeHunter", "iceage_hunter.png");
        ALIAS_ICON_OVERRIDES.put("ZombieIceAgeTroglobite", "iceage_troglobite.png");
        // World prefix isn't part of the alias name at all.
        ALIAS_ICON_OVERRIDES.put("ZombieWizard", "dark_wizard.png");
        ALIAS_ICON_OVERRIDES.put("ZombieNewspaper", "modern_newspaper.png");
        ALIAS_ICON_OVERRIDES.put("ZombieArcade", "eighties_arcade.png");
        ALIAS_ICON_OVERRIDES.put("ZombieCrystalSkull", "lostcity_crystalskull.png");
        // File name doesn't separate the compound word the same way the alias does.
        ALIAS_ICON_OVERRIDES.put("ZombieModernAllStar", "modern_allstar.png");
        ALIAS_ICON_OVERRIDES.put("ZombieLostCityJane", "lostcity_jane.png");
        ALIAS_ICON_OVERRIDES.put("ZombieDefault", "tutorial.png");
        ALIAS_ICON_OVERRIDES.put("ZombieGargantuar", "tutorial_gargantuar.png");
        ALIAS_ICON_OVERRIDES.put("ZombieImp", "tutorial_imp.png");
        ALIAS_ICON_OVERRIDES.put("ZombieArmor1", "tutorial_armor1.png");
        ALIAS_ICON_OVERRIDES.put("ZombieArmor2", "tutorial_armor2.png");
        ALIAS_ICON_OVERRIDES.put("ZombieArmor4", "tutorial_armor4.png");
        ALIAS_ICON_OVERRIDES.put("ZombieDarkArmor3", "dark_armor3.png");
        ALIAS_ICON_OVERRIDES.put("ZombieDarkJuggler", "dark_juggler.png");
    }

    // Aliases confirmed to have no icon in zombies_ui at all - skip straight to the
    // placeholder card instead of logging a "not found" error every rebuild.
    // These are the Zombotany-style costume zombies (ZombiePeashooter etc.) - the
    // pack has no icon for them, unlike ZombieAnimationRegistry's PAM lookup which
    // reuses the plant's own animation for these.
    private static final java.util.Set<String> ALIASES_WITHOUT_ICON = new java.util.HashSet<>(java.util.List.of(
            "ZombiePeashooter", "ZombieWallnut", "ZombieJalapeno", "ZombieSquash"
    ));

    private static final Pattern CAMEL_SPLIT = Pattern.compile("(?=[A-Z])");

    // Texture cache so the same icon PNG isn't loaded from disk more than once.
    private final Map<String, Texture> textureCache = new HashMap<>();

    // A plain panel behind every icon so adjacent cards read as separate slots in the
    // grid instead of icons floating directly on the board background. Built once and
    // shared - same tone as the "unseen" mystery-card frame in CollectionScreen.
    private Texture cardBackgroundTexture = new Texture("images/ui/zombies_ui/frame.png");

    /**
     * The shared "ZOMBIES" tombstone-frame panel every card is built on top of
     * (see {@link #cardBackground()}). Exposed so callers building a card outside
     * this factory (e.g. a fallback icon for an alias with no flat art) can still
     * use the exact same frame instead of a bare panel.
     */
    public Texture getCardBackground() {
        return cardBackground();
    }

    private Texture cardBackground() {
        if (cardBackgroundTexture == null) {
            com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(
                    4, 4, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pixmap.setColor(0.13f, 0.11f, 0.09f, 0.85f);
            pixmap.fill();
            cardBackgroundTexture = new Texture(pixmap);
            pixmap.dispose();
        }
        return cardBackgroundTexture;
    }

    public ZombieIconCard buildCardForAlias(String alias) {
        return buildCardForAlias(alias, CARD_WIDTH, CARD_HEIGHT);
    }

    /**
     * Same as {@link #buildCardForAlias(String)}, but builds the card (frame + icon
     * together) at the given size instead of the default {@link #CARD_WIDTH}x
     * {@link #CARD_HEIGHT}. Use this instead of calling {@code setSize(...)} on the
     * returned card afterward - {@link ZombieIconCard}'s icon inset is fixed at
     * construction time, so resizing it later leaves the icon at its original pixel
     * size while the frame/clip shrink, cropping it.
     */
    public ZombieIconCard buildCardForAlias(String alias, float cardWidth, float cardHeight) {
        try {
            String iconFile = resolveIconFile(alias);
            if (iconFile != null) {
                ZombieIconCard card = buildCard(alias, iconFile, cardWidth, cardHeight);
                if (card != null) {
                    return card;
                }
            }
            return buildPlaceholderCard(alias, cardWidth, cardHeight);
        } catch (Throwable t) {
            Gdx.app.error("ZombieIconCardFactory", "Failed to build card for '" + alias + "'", t);
            return buildPlaceholderCard(alias, cardWidth, cardHeight);
        }
    }

    private String resolveIconFile(String alias) {
        if (alias == null || alias.isBlank() || ALIASES_WITHOUT_ICON.contains(alias)) {
            return null;
        }
        String override = ALIAS_ICON_OVERRIDES.get(alias);
        if (override != null) {
            return override;
        }

        String withoutPrefix = alias.startsWith("Zombie") ? alias.substring("Zombie".length()) : alias;
        String[] tokens = camelTokens(withoutPrefix);
        String underscored = String.join("_", tokens).toUpperCase();
        String concatenated = String.join("", tokens).toUpperCase();

        for (String iconFile : ZOMBIE_ICON_FILES) {
            String stem = stripExtension(iconFile).toUpperCase();
            if (stem.equals(underscored) || stem.equals(concatenated)) {
                return iconFile;
            }
        }
        return null;
    }

    private String[] camelTokens(String raw) {
        String[] parts = CAMEL_SPLIT.split(raw);
        java.util.List<String> tokens = new java.util.ArrayList<>();
        for (String p : parts) {
            if (!p.isBlank()) tokens.add(p);
        }
        return tokens.toArray(new String[0]);
    }

    private ZombieIconCard buildPlaceholderCard(String alias) {
        return buildPlaceholderCard(alias, CARD_WIDTH, CARD_HEIGHT);
    }

    private ZombieIconCard buildPlaceholderCard(String alias, float cardWidth, float cardHeight) {
        String name = (alias == null || alias.isBlank()) ? "unknown" : alias;
        Gdx.app.error("ZombieIconCardFactory", "No zombies_ui icon found for '" + name
                + "' - showing a placeholder card.");

        Texture placeholderTexture = placeholderIconTexture(name);
        if (placeholderTexture == null) {
            return null;
        }
        return new ZombieIconCard(name, PLACEHOLDER_ICON_FILE, cardBackground(), placeholderTexture, cardWidth, cardHeight);
    }

    private Texture placeholderIconTexture(String alias) {
        String cacheKey = "placeholder:" + alias.toLowerCase();
        Texture cached = textureCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        int size = 128;
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(
                size, size, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(0.30f, 0.32f, 0.30f, 1f);
        pixmap.fillCircle(size / 2, size / 2, size / 2 - 4);
        pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        pixmap.drawCircle(size / 2, size / 2, size / 2 - 4);
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        textureCache.put(cacheKey, texture);
        return texture;
    }

    public ZombieIconCard buildCard(String alias, String iconFile) {
        return buildCard(alias, iconFile, CARD_WIDTH, CARD_HEIGHT);
    }

    public ZombieIconCard buildCard(String alias, String iconFile, float cardWidth, float cardHeight) {
        if (iconFile == null || iconFile.isEmpty()) {
            return null;
        }
        Texture iconTexture = loadTexture(ZOMBIES_UI_DIR + iconFile);
        if (iconTexture == null) {
            Gdx.app.error("ZombieIconCardFactory", "Could not build card for '" + alias
                    + "' - missing icon texture at " + ZOMBIES_UI_DIR + iconFile);
            return null;
        }
        return new ZombieIconCard(alias, iconFile, cardBackground(), iconTexture, cardWidth, cardHeight);
    }

    private String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private Texture loadTexture(String path) {
        Texture cached = textureCache.get(path);
        if (cached != null) {
            return cached;
        }
        if (!Gdx.files.internal(path).exists()) {
            return null;
        }
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        textureCache.put(path, texture);
        return texture;
    }

    @Override
    public void dispose() {
        for (Texture texture : textureCache.values()) {
            texture.dispose();
        }
        textureCache.clear();
    }
}