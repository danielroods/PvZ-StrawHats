import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import view.screens.generals.MiniGameLawnArt;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mini-game lawns are the counterpart of {@link ChapterLawnArtTest}: they ship their
 * board art as texture.png, not the map.png the chapters use. Asking for the wrong name
 * fails silently - the board texture comes back null, the lawn turns into a flat brown
 * rectangle, and BoardLayout's no-texture branch drops the reserved right-hand strip so
 * the lawn slides under the side trays (in co-op that hides the whole zombie drop zone).
 */
class MiniGameLawnArtTest {

    static Set<String> everyMiniGameFolder() {
        return MiniGameLawnArt.FOLDERS;
    }

    @ParameterizedTest
    @MethodSource("everyMiniGameFolder")
    void everyMiniGameLawnActuallyHasItsBoardArtOnDisk(String folder) {
        File art = new File(MiniGameLawnArt.backgroundPath(folder));
        assertTrue(art.isFile(), art.getPath()
                + " is missing, so this mini-game renders a flat brown lawn");

        assertFalseFile(new File(folder + "map.png"), folder);
    }

    @ParameterizedTest
    @MethodSource("everyMiniGameFolder")
    void everyMiniGameLawnUsesTheSame1366x768TemplateTheInsetsAssume(String folder) throws Exception {
        BufferedImage art = ImageIO.read(new File(MiniGameLawnArt.backgroundPath(folder)));
        assertNotNull(art, folder + " board art could not be decoded");
        // ChapterLawnArt.DEFAULT is measured against this template; a differently sized
        // background would put the playable tiles somewhere else entirely.
        assertEquals(1366, art.getWidth(), folder + " board art is off-template");
        assertEquals(768, art.getHeight(), folder + " board art is off-template");
    }

    @Test
    void theCoopLawnIsTheIZombieLawn() {
        // Couch I, Zombie is the co-op match, and its before-match screen has to preview
        // the very same lawn the match is played on.
        assertTrue(new File(MiniGameLawnArt.backgroundPath(MiniGameLawnArt.IZOMBIE)).isFile());
    }

    private static void assertFalseFile(File shouldNotExist, String folder) {
        assertTrue(!shouldNotExist.isFile(),
                folder + " grew a map.png; the mini-game screens ask for texture.png");
    }
}
