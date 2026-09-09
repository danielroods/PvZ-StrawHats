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
        
        
        assertEquals(1366, art.getWidth(), folder + " board art is off-template");
        assertEquals(768, art.getHeight(), folder + " board art is off-template");
    }

    @Test
    void theCoopLawnIsTheIZombieLawn() {
        
        
        assertTrue(new File(MiniGameLawnArt.backgroundPath(MiniGameLawnArt.IZOMBIE)).isFile());
    }

    private static void assertFalseFile(File shouldNotExist, String folder) {
        assertTrue(!shouldNotExist.isFile(),
                folder + " grew a map.png; the mini-game screens ask for texture.png");
    }
}
