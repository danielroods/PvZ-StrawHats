package view.screens.generals;

import java.util.Set;

public final class ChapterLawnArt {

    public record Insets(float left, float top, float right, float bottom) { }

    public static final Insets DEFAULT = new Insets(633f / 1366f, 192f / 768f, 0f, 82f / 768f);

    public static final Insets FUTURE =
            new Insets(535f / 1302f, 200f / 768f, 29f / 1302f, 93f / 768f);
    public static final Insets PIRATE =
            new Insets(530f / 1302f, 204f / 768f, 30f / 1302f, 80f / 768f);

    private static final Set<String> SPLIT_ART_CHAPTERS = Set.of("future", "pirate");

    private ChapterLawnArt() {
    }

    public static boolean isSplitArt(String seasonFolder) {
        return seasonFolder != null && SPLIT_ART_CHAPTERS.contains(seasonFolder);
    }

    public static Insets insetsFor(String seasonFolder) {
        if ("future".equals(seasonFolder)) return FUTURE;
        if ("pirate".equals(seasonFolder)) return PIRATE;
        return DEFAULT;
    }


    public static String[] backgroundLayers(String seasonFolder, String gameplayFolder) {
        if (isSplitArt(seasonFolder)) {
            
            
            
            
            
            
            return new String[] {
                    gameplayFolder + "texture_left.png",
                    gameplayFolder + "texture.png",
            };
        }
        return new String[] { gameplayFolder + "map.png" };
    }
}
