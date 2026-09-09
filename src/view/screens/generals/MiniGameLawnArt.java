package view.screens.generals;

import java.util.Set;

/**
 * Where the mini-game lawns keep their board art.
 * <p>
 * Chapters ship theirs as {@code map.png} (see {@link ChapterLawnArt}), but the
 * mini-game backgrounds under {@code assets/images/backg/mini_games/} ship as
 * {@code texture.png}. Asking for the wrong name is silent: {@code GameScreenAssets}
 * just returns a null board texture, the lawn falls back to a flat brown rectangle,
 * and - worse - {@code BoardLayout} takes its no-texture branch, which ignores
 * {@code GameScreen#reservedRightAreaWidth()} and lets the lawn run underneath the
 * side trays. Keeping the name in one tested place stops that from creeping back.
 */
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
