package service.resource_manager;

public enum AudioEnum {
    SFX_CLICK("assets/audio/sfx/click 1.mp3"),
    MENU_MUSIC("assets/audio/music/loonboon_142032648.mp3"),
    MINI_GAME_MUSIC("assets/audio/music/Screen_Recording_20250817_161231_YouTubemp4_162500.mp3"),
    EGYPT_MUSIC("assets/audio/music/A Man Without Love.mp3"),
    FROSTBITE_MUSIC("assets/audio/music/Just The Two Of Us Grover Washington Jr.mp3"),
    BEACH_MUSIC("assets/audio/music/Elton John-I m Still Standing -musicdel.ir 128.mp3"),
    DARK_AGES_MUSIC("assets/audio/music/Gloria Gaynor - I will survive (128).mp3"),


    
    SFX_ZOMBIE_EAT("assets/audio/sfx/zombie_eating.mp3"),
    SFX_SHOOT_NORMAL("assets/audio/sfx/normal_shooting.mp3"),
    SFX_SHOOT_FIRE("assets/audio/sfx/fire_shooting.mp3"),
    SFX_SHOOT_ICE("assets/audio/sfx/ice_shooting.mp3"),
    SFX_PLANT_FOOD("assets/audio/sfx/plantfood_shooting.mp3"),
    SFX_ZOMBIE_HIT("assets/audio/sfx/zombie_hit.mp3"),
    SFX_ELECTRIC_SHOCK("assets/audio/sfx/electric_shot.mp3"),
    SFX_HYPNOTIZE("assets/audio/sfx/hypnotize.mp3"),
    SFX_MELEE_HIT("assets/audio/sfx/melee_hit.mp3"),
    SFX_PLANT_EXPLODE("assets/audio/sfx/plant_explosion.mp3"),
    SFX_SUN_PRODUCE("assets/audio/sfx/sun_produce.mp3"),
    SFX_ITEM_COLLECT("assets/audio/sfx/item_collect.mp3"),
    SFX_LASER_SHOT("assets/audio/sfx/laser_shot.mp3"),
    SFX_BUBBLE_HIT("assets/audio/sfx/bubble_hit.mp3"),
    SFX_WAVE("assets/audio/sfx/sea_wave.mp3"),
    SFX_WIND("assets/audio/sfx/wind.mp3"),
    SFX_SANDSTORM("assets/audio/sfx/sandstorm.mp3"),
    SFX_NECROMANCY("assets/audio/sfx/necromacy.mp3"),
    SFX_BLEAT("assets/audio/sfx/bleat.mp3"),
    SFX_PIANO("assets/audio/sfx/piano.mp3"),
    SFX_ZOMBIE_IMP("assets/audio/sfx/pvz_imp.mp3"),
    SFX_ZOMBOSS_NPC("assets/audio/sfx/telegram-cloud-document-4-5814532192818175685.mp3"),
    SFX_WAVE_START("assets/audio/sfx/zombie_wave.mp3"),
    SFX_MATCH_WIN("assets/audio/sfx/win.mp3"),
    SFX_MATCH_LOSE("assets/audio/sfx/lose.mp3"),
    SFX_LAWN_MOWER("assets/audio/sfx/lawn-mower.mp3");

    private final String filePath;

    AudioEnum(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
}