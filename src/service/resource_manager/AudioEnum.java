package service.resource_manager;

public enum AudioEnum {
    SFX_CLICK("assets/audio/sfx/click 1.mp3"),
    MENU_MUSIC("assets/audio/music/loonboon_142032648.mp3"),
    EGYPT_MUSIC("assets/audio/music/A Man Without Love.mp3"),
    FROSTBITE_MUSIC("assets/audio/music/Just The Two Of Us Grover Washington Jr.mp3"),
    BEACH_MUSIC("assets/audio/music/Elton John-I m Still Standing -musicdel.ir 128.mp3"),
    DARK_AGES_MUSIC("assets/audio/music/Gloria Gaynor - I will survive (128).mp3"),

    // ---- Gameplay SFX ----
    SFX_ZOMBIE_EAT(""),
    SFX_SHOOT_NORMAL(""),
    SFX_SHOOT_FIRE(""),
    SFX_SHOOT_ICE(""),
    SFX_PLANT_FOOD(""),
    SFX_ZOMBIE_HIT(""),
    SFX_ELECTRIC_SHOCK(""),
    SFX_HYPNOTIZE(""),
    SFX_MELEE_HIT(""),
    SFX_PLANT_EXPLODE(""),
    SFX_SUN_PRODUCE(""),
    SFX_ITEM_COLLECT(""),
    SFX_LASER_SHOT(""),
    SFX_BUBBLE_HIT(""),
    SFX_WAVE(""),
    SFX_WIND(""),
    SFX_SANDSTORM(""),
    SFX_NECROMANCY(""),
    SFX_BLEAT(""),
    SFX_PIANO(""),
    SFX_ZOMBIE_IMP(""),
    SFX_ZOMBOSS_NPC(""),
    SFX_WAVE_START(""),
    SFX_MATCH_WIN(""),
    SFX_MATCH_LOSE("");

    private final String filePath;

    AudioEnum(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
}