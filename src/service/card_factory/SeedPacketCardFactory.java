package service.card_factory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link SeedPacketCard}s by putting each plant's UI icon (from
 * assets/images/ui/plants_ui) on top of a seed packet background (from
 * assets/images/ui/seedpackets_ui), then hands back the whole group of
 * built cards.
 */
public class SeedPacketCardFactory implements Disposable {

    private static final String PLANTS_UI_DIR = "assets/images/ui/plants_ui/";
    private static final String SEEDPACKETS_UI_DIR = "assets/images/ui/seedpackets_ui/";

    private static final String DEFAULT_PACKET_SKIN = "ready.png";

    private static final float CARD_WIDTH = 150f;
    private static final float CARD_HEIGHT = 190f;

    // Every plant icon file present in assets/images/ui/plants_ui.
    private static final String[] PLANT_ICON_FILES = {
            "ailmint.png", "akee.png", "aloe.png", "appeasemint.png", "applemortar.png", "aquavine.png",
            "armamint.png", "bamboospartan.png", "banana.png", "beansprout.png", "blastberry.png", "blastspinner.png",
            "blazeleaf.png", "blazingknight.png", "blockoli.png", "bloomerang.png", "bloominghearts.png", "blover.png",
            "boingsetta.png", "bombardmint.png", "bombegranate.png", "bonkchoy.png", "boomberry.png", "boomflower.png",
            "bowlingbulb.png", "brainstem.png", "bramblebush.png", "buduhboom.png", "buttercup.png", "buzzbutton.png",
            "cabbagepult.png", "cactus.png", "caulipower.png", "celerystalker.png", "chardguard.png", "cherry_bomb.png",
            "chilibean.png", "chillypepper.png", "chomper.png", "citron.png", "coconutcannon.png", "coldsnapdragon.png",
            "concealmint.png", "containmint.png", "cornfetti.png", "cranjelly.png", "dandelion.png", "dartichoke.png",
            "dazeychain.png", "devourbloom.png", "doomshroom.png", "draftodil.png", "dragonbruit.png", "dusklobber.png",
            "egypt.png", "electricblueberry.png", "electriccurrant.png", "electricitea.png", "electricpeashooter.png", "electricpeel.png",
            "empea.png", "enchantmint.png", "endurian.png", "enforcemint.png", "enlightenmint.png", "escaperoot.png",
            "explodeonut.png", "explodeovine.png", "filamint.png", "firepeashooter.png", "frostbonnet.png", "fumeshroom.png",
            "garlic.png", "ghostpepper.png", "gloomvine.png", "goldbloom.png", "Goldleaf.png", "grapeshot.png",
            "gravebuster.png", "grimrose.png", "guacodile.png", "guardshroom.png", "gumnut.png", "hammeruit.png",
            "headbutter.png", "heathseeker.png", "hocus.png", "hollyknight.png", "holonut.png", "homingthistle.png",
            "hotdate.png", "hotpotato.png", "hurrikale.png", "hypnoshroom.png", "icebloom.png", "iceburg.png",
            "iceshroom.png", "iceweed.png", "icon_premium.png", "icycurrant.png", "imitater.png", "imppear.png",
            "inferno.png", "intensivecarrot.png", "jackolantern.png", "jalapeno.png", "kernelpult.png", "kiwibeast.png",
            "laser_bean.png", "lavaguava.png", "lemonaid.png", "level_tab_gold.png", "levitater.png", "lightningreed.png",
            "lilypad.png", "magnetshroom.png", "magnifyinggrass.png", "mangofier.png", "marigold.png", "maybee.png",
            "megagatling.png", "Melonpult.png", "meteorflower.png", "missiletoe.png", "moonflower.png", "murkadamia.png",
            "nightcap.png", "nightshade.png", "noctarine.png", "olivepit.png", "parsnip.png", "peanut.png",
            "peapod.png", "peashooter.png", "peppermint.png", "pepperpult.png", "perfumeshroom.png", "phatbeet.png",
            "poisonpeashooter.png", "pokra.png", "potatomine.png", "powerlily.png", "powerplant.png", "powervine.png",
            "powervine_connected2.png", "primalpeashooter.png", "primalpotatomine.png", "primalsunflower.png", "primalwallnut.png", "puffball.png",
            "puffshroom.png", "pumpkin.png", "pvine.png", "pyrevine.png", "pyroak.png", "redstinger.png",
            "reinforcemint.png", "repeater.png", "rhubarbarian.png", "rose.png", "sakura.png", "sapfling.png",
            "scaredyshroom.png", "SeaFlora.png", "seashooter.png", "seashroom.png", "shadowpeashooter.png", "shadowshroom.png",
            "shinevine.png", "shrinkingviolet.png", "slingpea.png", "snapdragon.png", "snappea.png", "snowpea.png",
            "solarsage.png", "solartomato.png", "sourshot.png", "spearmint.png", "spikerock.png", "spikeweed.png",
            "splitpea.png", "sporeshroom.png", "springbean.png", "squash.png", "stallia.png", "starfruit.png",
            "stickybombrice.png", "strawburst.png", "stunion.png", "sunbean.png", "sundewtangler.png", "sunflower.png",
            "sunshroom.png", "sweetheartsnare.png", "sweetpotato.png", "tacticalcuke.png", "tallnut.png", "tanglekelp.png",
            "teleportatomine.png", "thornwhip.png", "threepeater.png", "thymewarp.png", "tigergrass.png", "toadstool.png",
            "tombtangler.png", "tools_projectile_bowlingbulb1.png", "tools_projectile_bowlingbulb2.png", "tools_projectile_bowlingbulb3.png", "tools_projectile_bowlingbulb_explode.png", "tools_projectile_bowlingbulb_mega.png",
            "torchwood.png", "tumbleweed.png", "turkeypult.png", "twinsunflower.png", "ultomato.png", "vamporcini.png",
            "voltsnapdragon.png", "wallnut.png", "wasabiwhip.png", "waterrabbit.png", "wintermelon.png", "wintermint.png",
            "witchhazel.png", "xshot.png", "znakelily.png", "zoybeanpod.png", "rotorutbaga.png"
    };

    // Every seed packet background present in assets/images/ui/seedpackets_ui.
    private static final String[] PACKET_SKIN_FILES = {
            "beach.png", "boost.png", "cowboy.png", "dark.png", "dino.png", "eighties.png",
            "empty_packet.png", "future.png", "homeless.png", "iceage.png", "lostcity.png", "modernday.png",
            "pirate.png", "ready.png", "ready_premium.png", "selected.png", "selected_premium.png"
    };

    // Mapping plants to their canonical world/category background packets.
    private static final Map<String, String> DEFAULT_WORLD_PACKETS = new HashMap<>();

    static {
        // Pirate Seas
        registerSkin("pirate.png", "cherry_bomb", "coconutcannon", "kernelpult", "powerlily", "snapdragon", "spikerock", "spikeweed", "springbean", "threepeater");

        // Wild West
        registerSkin("cowboy.png", "cactus", "chilibean", "lightningreed", "melonpult", "peapod", "splitpea", "tallnut", "wintermelon");

        // Far Future
        registerSkin("future.png", "blover", "citron", "empea", "holonut", "laser_bean", "magnifyinggrass", "powerplant", "tileturnip", "ultomato");

        // Dark Ages & Mushrooms
        registerSkin("dark.png", "doomshroom", "fumeshroom", "guardshroom", "hypnoshroom", "magnetshroom", "peanut", "puffshroom", "scaredyshroom", "sunbean", "sunshroom", "tombtangler", "vamporcini", "witchhazel");

        // Big Wave Beach & Aquatic
        registerSkin("beach.png", "aquavine", "banana", "bowlingbulb", "guacodile", "lilypad", "seaflora", "seashooter", "seashroom", "sundewtangler", "tanglekelp", "waterrabbit");

        // Frostbite Caves & Ice/Cold
        registerSkin("iceage.png", "boingsetta", "chardguard", "chillypepper", "frostbonnet", "hotpotato", "icebloom", "iceshroom", "iceweed", "icycurrant", "pepperpult", "rotorutbaga", "rotobaga", "stunion");

        // Lost City
        registerSkin("lostcity.png", "akee", "endurian", "goldleaf", "lavaguava", "redstinger", "stallia");

        // Neon Mixtape Tour & Electric
        registerSkin("eighties.png", "celerystalker", "electriccurrant", "electricitea", "garlic", "intensivecarrot", "phatbeet", "sporeshroom", "thymewarp");

        // Jurassic Marsh & Primal
        registerSkin("dino.png", "bramblebush", "perfumeshroom", "primalpeashooter", "primalpotatomine", "primalsunflower", "primalwallnut", "rhubarbarian");

        // Modern Day & Shadow
        registerSkin("modernday.png", "dusklobber", "gloomvine", "grimrose", "moonflower", "murkadamia", "nightcap", "nightshade", "noctarine", "shadowpeashooter", "shadowshroom");

        // Premium / Mints / Arena / Event Plants
        registerSkin("ready_premium.png",
                "ailmint", "aloe", "appeasemint", "applemortar", "armamint", "bamboospartan", "beansprout", "blastberry", "blastspinner", "blazeleaf", "blazingknight", "blockoli", "bloominghearts", "bombardmint", "bombegranate", "boomberry", "boomflower", "brainstem", "buduhboom", "buttercup", "buzzbutton", "caulipower", "chomper", "coldsnapdragon", "concealmint", "containmint", "cornfetti", "cranjelly", "dandelion", "dartichoke", "dazeychain", "devourbloom", "draftodil", "dragonbruit", "electricblueberry", "electricpeashooter", "electricpeel", "enchantmint", "enforcemint", "enlightenmint", "escaperoot", "explodeonut", "explodeovine", "filamint", "firepeashooter", "ghostpepper", "goldbloom", "grapeshot", "gumnut", "hammeruit", "headbutter", "heathseeker", "hocus", "hollyknight", "homingthistle", "hotdate", "hurrikale", "imitater", "imppear", "inferno", "jackolantern", "jalapeno", "kiwibeast", "lemonaid", "levitater", "mangofier", "maybee", "megagatling", "meteorflower", "missiletoe", "olivepit", "parsnip", "peppermint", "poisonpeashooter", "pokra", "powervine", "puffball", "pvine", "pyrevine", "pyroak", "reinforcemint", "rose", "sakura", "sapfling", "shinevine", "shrinkingviolet", "slingpea", "snappea", "snowpea", "solarsage", "solartomato", "sourshot", "spearmint", "squash", "stickybombrice", "strawburst", "sweetheartsnare", "sweetpotato", "tacticalcuke", "teleportatomine", "thornwhip", "tigergrass", "toadstool", "tumbleweed", "turkeypult", "voltsnapdragon", "wasabiwhip", "wintermint", "xshot", "znakelily", "zoybeanpod"
        );
    }

    private static void registerSkin(String skin, String... plants) {
        for (String plant : plants) {
            DEFAULT_WORLD_PACKETS.put(plant.toLowerCase(), skin);
        }
    }

    // plant name (lower-case) -> packet file name, overridable via setPacketSkin().
    private final Map<String, String> packetSkinOverrides = new HashMap<>();

    // Texture cache so the same packet/plant PNG isn't loaded from disk more than once.
    private final Map<String, Texture> textureCache = new HashMap<>();

    private static final Map<String, String> DISPLAY_NAME_ICON_OVERRIDES = Map.of(
            "MEGA_GATLING_PEA", "megagatling.png",
            "ICEBERG_LETTUCE", "headbutter.png",
            "PIERCE_MINT", "spearmint.png",
            "ROTOBAGA", "rotorutbaga.png",
            "GOO_PEASHOOTER", "goopeashooter.png",
            "CATTAIL_MINT", "concealmint.png",
            "CAT_TAIL", "homingthistle.png"
    );

    private static final java.util.Set<String> DISPLAY_NAMES_WITHOUT_ICON = java.util.Set.of(
    );

    public SeedPacketCard buildCardForDisplayName(String displayName) {
        return buildCardForDisplayName(displayName, null);
    }

    /**
     * Builds a card with an optional one-call packet background override.
     * The override is intentionally not stored in packetSkinOverrides, so it
     * cannot affect Collection/Shop cards or any other normal factory usage.
     */
    public SeedPacketCard buildCardForDisplayName(String displayName, String packetSkinOverride) {
        try {
            String iconFile = resolveIconFile(displayName);
            if (iconFile != null) {
                SeedPacketCard card = packetSkinOverride == null
                        ? buildCard(iconFile)
                        : buildCard(iconFile, packetSkinOverride);
                if (card != null) {
                    return card;
                }
            }
            return buildPlaceholderCard(displayName);
        } catch (Throwable t) {
            Gdx.app.error("SeedPacketCardFactory", "Failed to build card for '" + displayName + "'", t);
            return buildPlaceholderCard(displayName);
        }
    }

    private String resolveIconFile(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        String key = normalizeKey(displayName, "_");
        if (DISPLAY_NAMES_WITHOUT_ICON.contains(key)) {
            return null;
        }
        String override = DISPLAY_NAME_ICON_OVERRIDES.get(key);
        if (override != null) {
            return override;
        }
        String underscored = normalizeKey(displayName, "_");
        String concatenated = normalizeKey(displayName, "");
        for (String iconFile : PLANT_ICON_FILES) {
            String stem = stripExtension(iconFile);
            if (stem.equalsIgnoreCase(underscored) || stem.equalsIgnoreCase(concatenated)) {
                return iconFile;
            }
        }
        return null;
    }

    private String normalizeKey(String raw, String separator) {
        return raw.trim().toUpperCase()
                .replace("-", separator)
                .replace(" ", separator)
                .replaceAll("[^A-Z0-9_]", "");
    }

    private SeedPacketCard buildPlaceholderCard(String displayName) {
        String name = (displayName == null || displayName.isBlank()) ? "unknown" : displayName;
        Gdx.app.error("SeedPacketCardFactory", "No plants_ui icon found for '" + name
                + "' - showing a placeholder card.");

        Texture packetTexture = loadTexture(SEEDPACKETS_UI_DIR + DEFAULT_PACKET_SKIN);
        Texture placeholderTexture = placeholderIconTexture(name);
        if (packetTexture == null || placeholderTexture == null) {
            return null;
        }
        return new SeedPacketCard(name, "(placeholder)", DEFAULT_PACKET_SKIN,
                packetTexture, placeholderTexture, CARD_WIDTH, CARD_HEIGHT);
    }

    private Texture placeholderIconTexture(String name) {
        String cacheKey = "placeholder:" + name.toLowerCase();
        Texture cached = textureCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        int size = 128;
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(
                size, size, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(0.25f, 0.45f, 0.20f, 1f);
        pixmap.fillCircle(size / 2, size / 2, size / 2 - 4);
        pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        pixmap.drawCircle(size / 2, size / 2, size / 2 - 4);
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        textureCache.put(cacheKey, texture);
        return texture;
    }

    public void setPacketSkin(String plantName, String packetFile) {
        if (plantName == null || packetFile == null) {
            return;
        }
        packetSkinOverrides.put(plantName.toLowerCase(), packetFile.toLowerCase());
    }

    public void setPacketSkins(Map<String, String> plantNameToPacketFile) {
        if (plantNameToPacketFile == null) {
            return;
        }
        for (Map.Entry<String, String> entry : plantNameToPacketFile.entrySet()) {
            setPacketSkin(entry.getKey(), entry.getValue());
        }
    }

    public List<SeedPacketCard> buildAllCards() {
        List<SeedPacketCard> cards = new ArrayList<>();
        for (String plantFile : PLANT_ICON_FILES) {
            SeedPacketCard card = buildCard(plantFile);
            if (card != null) {
                cards.add(card);
            }
        }
        return cards;
    }

    public Map<String, SeedPacketCard> buildAllCardsByName() {
        Map<String, SeedPacketCard> byName = new LinkedHashMap<>();
        for (SeedPacketCard card : buildAllCards()) {
            byName.put(card.getName(), card);
        }
        return byName;
    }

    public SeedPacketCard buildCard(String plantIconFile) {
        return buildCard(plantIconFile, null);
    }

    private SeedPacketCard buildCard(String plantIconFile, String packetSkinOverride) {
        if (plantIconFile == null || plantIconFile.isEmpty()) {
            return null;
        }
        String name = stripExtension(plantIconFile);
        String packetFile = packetSkinOverride == null
                ? resolvePacketSkin(name)
                : packetSkinOverride.toLowerCase();
        if (!isKnownPacketSkin(packetFile)) {
            packetFile = resolvePacketSkin(name);
        }

        Texture plantTexture = loadTexture(PLANTS_UI_DIR + plantIconFile);
        Texture packetTexture = loadTexture(SEEDPACKETS_UI_DIR + packetFile);
        if (plantTexture == null || packetTexture == null) {
            Gdx.app.error("SeedPacketCardFactory", "Could not build card for '" + name
                    + "' - missing plant icon or packet texture.");
            return null;
        }

        return new SeedPacketCard(name, plantIconFile, packetFile,
                packetTexture, plantTexture, CARD_WIDTH, CARD_HEIGHT);
    }

    private boolean isKnownPacketSkin(String packetFile) {
        if (packetFile == null) return false;
        for (String known : PACKET_SKIN_FILES) {
            if (known.equalsIgnoreCase(packetFile)) return true;
        }
        return false;
    }

    public SeedPacketCard buildCardByPlantName(String plantName) {
        if (plantName == null) {
            return null;
        }
        String fileName = plantName.toLowerCase().endsWith(".png") ? plantName : plantName + ".png";
        for (String plantFile : PLANT_ICON_FILES) {
            if (plantFile.equalsIgnoreCase(fileName)) {
                return buildCard(plantFile);
            }
        }
        String overrideFile = resolveIconFile(plantName);
        if (overrideFile != null) {
            return buildCard(overrideFile);
        }
        return null;
    }

    private String resolvePacketSkin(String plantName) {
        String lowerName = plantName.toLowerCase();

        // ۱. بررسی دستی کاربر (در صورت وجود)
        String override = packetSkinOverrides.get(lowerName);
        if (override != null && isKnownPacketSkin(override)) {
            return override;
        }

        // ۲. بررسی پس‌زمینه اختصاصی دنیای گیاه
        String worldSkin = DEFAULT_WORLD_PACKETS.get(lowerName);
        if (worldSkin != null && isKnownPacketSkin(worldSkin)) {
            return worldSkin;
        }

        // ۳. پس‌زمینه پیش‌فرض
        return DEFAULT_PACKET_SKIN;
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