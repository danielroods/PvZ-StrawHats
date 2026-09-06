import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import view.screens.generals.ChapterLawnArt;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChapterLawnArtTest {

    private static final String ROOT = "assets/images/chapters/";
    private static final int VIEW_W = 1280;
    private static final int VIEW_H = 720;
    private static final int COLS = 9;
    private static final int ROWS = 5;

    private static BufferedImage read(String path) throws Exception {
        File file = new File(path);
        assertTrue(file.isFile(), "missing chapter asset: " + path);
        BufferedImage image = ImageIO.read(file);
        assertNotNull(image, "unreadable image: " + path);
        return image;
    }

    @Test
    void onlyFarFutureAndPirateSeasAreSplitArt() {
        assertTrue(ChapterLawnArt.isSplitArt("future"));
        assertTrue(ChapterLawnArt.isSplitArt("pirate"));
        for (String folder : new String[] {"egypt", "beach", "darkage", "frostbite_cave", null}) {
            assertFalse(ChapterLawnArt.isSplitArt(folder), folder + " should use one map.png");
            assertEquals(ChapterLawnArt.DEFAULT, ChapterLawnArt.insetsFor(folder));
        }
    }

    @Test
    void ordinaryChaptersAskForTheirSingleMap() {
        assertArrayEquals(new String[] {ROOT + "darkage/gameplay/map.png"},
                ChapterLawnArt.backgroundLayers("darkage", ROOT + "darkage/gameplay/"));
    }

    @ParameterizedTest
    @CsvSource({"future", "pirate"})
    void splitArtChaptersStitchTheirTwoStripsIntoOneLawn(String chapter) throws Exception {
        String folder = ROOT + chapter + "/gameplay/";
        String[] layers = ChapterLawnArt.backgroundLayers(chapter, folder);
        assertArrayEquals(new String[] {folder + "texture_left.png", folder + "texture.png"},
                layers);

        assertFalse(new File(folder + "map.png").isFile(),
                chapter + " now has a map.png; the split-art path is no longer right for it");

        BufferedImage left = read(layers[0]);
        BufferedImage main = read(layers[1]);
        assertEquals(left.getHeight(), main.getHeight(),
                "the two strips must be the same height to sit side by side");
        assertEquals(1302, left.getWidth() + main.getWidth(),
                "the stitched background is the 1302x768 template the insets were measured on");
        assertEquals(768, main.getHeight());
    }

    @ParameterizedTest
    @CsvSource({
            "future, 535, 200, 1273, 675",
            "pirate, 530, 204, 1272, 688",
    })
    void theLawnLandsOnThePaintedTiles(String chapter, int lawnLeft, int lawnTop,
                                       int lawnRight, int lawnBottom) throws Exception {
        String folder = ROOT + chapter + "/gameplay/";
        String[] layers = ChapterLawnArt.backgroundLayers(chapter, folder);
        BufferedImage left = read(layers[0]);
        BufferedImage main = read(layers[1]);
        float texW = left.getWidth() + main.getWidth();
        float texH = main.getHeight();

        ChapterLawnArt.Insets insets = ChapterLawnArt.insetsFor(chapter);
        assertEquals(lawnLeft, Math.round(insets.left() * texW), 1,
                chapter + " lawn left edge drifted from the art");
        assertEquals(lawnTop, Math.round(insets.top() * texH), 1,
                chapter + " lawn top edge drifted from the art");
        assertEquals(lawnRight, Math.round(texW - insets.right() * texW), 1,
                chapter + " lawn right edge drifted from the art");
        assertEquals(lawnBottom, Math.round(texH - insets.bottom() * texH), 1,
                chapter + " lawn bottom edge drifted from the art");

        float fit = Math.max(VIEW_H / texH, VIEW_W / texW);
        float bgW = texW * fit;
        float bgH = texH * fit;
        float bgX = VIEW_W - bgW;
        float bgY = (VIEW_H - bgH) * 0.5f;
        float boardX = bgX + bgW * insets.left();
        float boardY = bgY + bgH * insets.bottom();
        float tileW = bgW * (1 - insets.left() - insets.right()) / COLS;
        float tileH = bgH * (1 - insets.top() - insets.bottom()) / ROWS;

        assertTrue(boardX > 0 && boardX < VIEW_W * 0.5f,
                chapter + " puts the lawn's left edge at " + boardX + ", off the left half");
        assertTrue(boardX + tileW * COLS <= VIEW_W + 1,
                chapter + " runs the lawn past the right edge of the screen");
        assertTrue(boardY >= 0 && boardY + tileH * ROWS <= VIEW_H + 1,
                chapter + " runs the lawn off the top or bottom of the screen");
        assertEquals(76.4f, tileW, 8f, chapter + " tile width is out of step with the other chapters");
        assertEquals(92.6f, tileH, 8f, chapter + " tile height is out of step with the other chapters");
    }
}
