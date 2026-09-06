package controller.assets;

public final class AssetPaths {

    private AssetPaths() {}

    public static final String ROOT = "assets/";

    public static final String IMAGES = ROOT + "images/";
    public static final String PLANT_IMAGES = IMAGES + "plants/";
    public static final String ZOMBIE_IMAGES = IMAGES + "zombies/";
    public static final String ITEM_IMAGES = IMAGES + "items/";
    public static final String TILE_IMAGES = IMAGES + "tiles/";
    public static final String UI_IMAGES = IMAGES + "ui/";

    public static final String FROSTBITE_SLIDER_TILE_UP = "images/chapters/frostbite_cave/gameplay/up.png";
    public static final String FROSTBITE_SLIDER_TILE_DOWN = "images/chapters/frostbite_cave/gameplay/down.png";
    public static final String FROSTBITE_SLIDER_TILE_BACKGROUND_UP = "images/chapters/frostbite_cave/gameplay/up_bg.png";
    public static final String FROSTBITE_SLIDER_TILE_BACKGROUND_DOWN = "images/chapters/frostbite_cave/gameplay/down_bg.png";
    public static final String FROSTBITE_PLANT_ICE_BLOCK_1 = "images/chapters/frostbite_cave/gameplay/plant_ice_block_1.png";
    public static final String FROSTBITE_PLANT_ICE_BLOCK_2 = "images/chapters/frostbite_cave/gameplay/plant_ice_block_2.png";
    public static final String FROSTBITE_PLANT_ICE_BLOCK_3 = "images/chapters/frostbite_cave/gameplay/plant_ice_block_3.png";
    public static final String FROSTBITE_ZOMBIE_ICE_BLOCK = "images/chapters/frostbite_cave/gameplay/zombie_ice_block.png";

    public static final String PVZ_ASSETS = ROOT + "pvz-assets/IMAGES/";

    public static final String ATLASES = ROOT + "atlases/";
    public static final String PLANTS_ATLAS = ATLASES + "plants.atlas";
    public static final String ZOMBIES_ATLAS = ATLASES + "zombies.atlas";
    public static final String ITEMS_ATLAS = ATLASES + "items.atlas";
    public static final String TILES_ATLAS = ATLASES + "tiles.atlas";
    public static final String UI_ATLAS = ATLASES + "ui.atlas";

    public static final String MAPS = ROOT + "maps/tmx/";
    public static final String TILESETS = ROOT + "maps/tilesets/";

    public static final String FONTS = ROOT + "fonts/";
    public static final String DEFAULT_FONT = FONTS + "default.fnt";

    public static final String AUDIO = ROOT + "audio/";
    public static final String SFX = AUDIO + "sfx/";
    public static final String MUSIC = AUDIO + "music/";

    public static final String TROPHIES_IMAGES = UI_IMAGES + "trophies/";
    public static final String TROPHIES_CABINET_BG = TROPHIES_IMAGES + "cabinet_background.png";
    public static final String TROPHIES_SHELF_TILE = TROPHIES_IMAGES + "shelf_tile.png";
    public static final String TROPHY_LOCKED_SILHOUETTE = TROPHIES_IMAGES + "trophy_locked.png";
    public static final String TROPHY_EGYPT = TROPHIES_IMAGES + "trophy_egypt.png";
    public static final String TROPHY_FROSTBITE_CAVES = TROPHIES_IMAGES + "trophy_iceage.png";
    public static final String TROPHY_BIG_WAVE_BEACH = TROPHIES_IMAGES + "trophy_beach.png";
    public static final String TROPHY_DARK_AGES = TROPHIES_IMAGES + "trophy_dark.png";
    // Not yet playable chapters (see project notes) - paths reserved so TrophyManager's
    // chapter list only needs a new entry, not new constants, once they're added for real.
    public static final String TROPHY_PIRATE_SEAS = TROPHIES_IMAGES + "trophy_pirate.png";
    public static final String TROPHY_FAR_FUTURE = TROPHIES_IMAGES + "trophy_future.png";

    public static String slug(String rawName) {
        if (rawName == null) {
            return "";
        }
        String result = rawName.trim().toLowerCase()
                .replace("-", "")
                .replace("'", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return result;
    }

    public static String plantRegion(String plantName) {
        return slug(plantName);
    }

    public static String zombieRegion(String zombieAlias) {
        return slug(zombieAlias);
    }

    public static String plantTexturePath(String plantName) {
        return PLANT_IMAGES + slug(plantName) + ".png";
    }

    public static String zombieTexturePath(String zombieAlias) {
        return ZOMBIE_IMAGES + slug(zombieAlias) + ".png";
    }

    public static String seasonMapPath(String seasonName) {
        return MAPS + slug(seasonName) + ".tmx";
    }

    public static String sfxPath(String soundName) {
        return SFX + slug(soundName) + ".wav";
    }

    public static String musicPath(String trackName) {
        return MUSIC + slug(trackName) + ".mp3";
    }

    public static String uiRegion(String name) {
        return slug(name);
    }
}