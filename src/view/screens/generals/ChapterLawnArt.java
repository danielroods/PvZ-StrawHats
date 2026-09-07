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
            // Future/Pirate never shipped a "map.png" - their lawn art is split into
            // texture_left.png (left decorative panel) + texture.png (the actual
            // playable deck/floor, matching IMAGE_BACKGROUNDS_*_TEXTURE in the
            // original PAM resource pack). texture_right.png exists alongside these
            // but isn't part of the stitched board - the PIRATE/FUTURE insets already
            // account for a small right-edge margin baked into texture.png itself.
            return new String[] {
                    gameplayFolder + "texture_left.png",
                    gameplayFolder + "texture.png",
            };
        }
        return new String[] { gameplayFolder + "map.png" };
    }
}
