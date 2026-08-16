package service.resource_manager;

public enum AudioEnum {
    SFX_CLICK("assets/audio/sfx/click 1.mp3"),
    MENU_MUSIC("assets/audio/music/loonboon_142032648.mp3"),
    EGYPT_MUSIC("assets/audio/music/A Man Without Love.mp3"),
    FROSTBITE_MUSIC("assets/audio/music/Just The Two Of Us Grover Washington Jr.mp3"),
    BEACH_MUSIC("assets/audio/music/Elton John-I m Still Standing -musicdel.ir 128.mp3"),
    DARK_AGES_MUSIC("assets/audio/music/Gloria Gaynor - I will survive (128).mp3");

    private final String filePath;

    AudioEnum(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
}