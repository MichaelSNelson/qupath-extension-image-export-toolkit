package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class PanelLabelRendererTest {

    @Test
    void testRendersWithoutExceptionForAllPositions() {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        for (ScaleBarRenderer.Position pos : ScaleBarRenderer.Position.values()) {
            assertDoesNotThrow(() -> PanelLabelRenderer.drawPanelLabel(
                    g2d, 400, 400, "A", pos, 0, true, Color.WHITE),
                    "Failed for position: " + pos);
        }

        g2d.dispose();
    }

    @Test
    void testLabelForIndexSingleLetters() {
        assertEquals("A", PanelLabelRenderer.labelForIndex(0));
        assertEquals("B", PanelLabelRenderer.labelForIndex(1));
        assertEquals("Z", PanelLabelRenderer.labelForIndex(25));
    }

    @Test
    void testLabelForIndexDoubleLetters() {
        assertEquals("AA", PanelLabelRenderer.labelForIndex(26));
        assertEquals("AB", PanelLabelRenderer.labelForIndex(27));
        assertEquals("AZ", PanelLabelRenderer.labelForIndex(51));
        assertEquals("BA", PanelLabelRenderer.labelForIndex(52));
    }

    @Test
    void testLabelForIndexNegativeReturnA() {
        assertEquals("A", PanelLabelRenderer.labelForIndex(-1));
        assertEquals("A", PanelLabelRenderer.labelForIndex(-100));
    }

    @Test
    void testZeroDimensionsReturnsWithoutDrawing() {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        assertDoesNotThrow(() -> PanelLabelRenderer.drawPanelLabel(
                g2d, 0, 0, "A", ScaleBarRenderer.Position.UPPER_LEFT,
                0, true, Color.WHITE));

        g2d.dispose();
    }

    @Test
    void testNullLabelReturnsWithoutDrawing() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        assertDoesNotThrow(() -> PanelLabelRenderer.drawPanelLabel(
                g2d, 200, 200, null, ScaleBarRenderer.Position.UPPER_LEFT,
                0, true, Color.WHITE));

        assertDoesNotThrow(() -> PanelLabelRenderer.drawPanelLabel(
                g2d, 200, 200, "", ScaleBarRenderer.Position.UPPER_LEFT,
                0, true, Color.WHITE));

        g2d.dispose();
    }

    @Test
    void testRenderedLabelIsAsciiOnly() {
        // Verify labelForIndex produces ASCII-only output
        for (int i = 0; i < 100; i++) {
            String label = PanelLabelRenderer.labelForIndex(i);
            for (int j = 0; j < label.length(); j++) {
                char c = label.charAt(j);
                assertTrue(c < 128,
                        "Non-ASCII character in label at index " + i + ": " + label);
            }
        }
    }

    @Test
    void testExplicitFontSizeRenders() {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        assertDoesNotThrow(() -> PanelLabelRenderer.drawPanelLabel(
                g2d, 400, 400, "B", ScaleBarRenderer.Position.LOWER_RIGHT,
                32, false, Color.WHITE));

        g2d.dispose();
    }
}
