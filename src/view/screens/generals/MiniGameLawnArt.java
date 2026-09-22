package view.screens.generals;

import java.util.Set;


public final class MiniGameLawnArt {

    public static final String ROOT = "assets/images/backg/mini_games/";

    public static final String IZOMBIE = ROOT + "izombie/";
    public static final String BEGHOULED = ROOT + "begh/";
    public static final String WALLNUT_BOWLING = ROOT + "wallnut/";
    public static final String ZOMBOTANY = ROOT + "zombotany/";

    
    public static final Set<String> FOLDERS =
            Set.of(IZOMBIE, BEGHOULED, WALLNUT_BOWLING, ZOMBOTANY);

    private MiniGameLawnArt() {
    }

    
    public static String backgroundPath(String gameplayFolder) {
        return gameplayFolder + "texture.png";
    }
}
