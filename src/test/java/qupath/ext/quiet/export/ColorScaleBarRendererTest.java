package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class ColorScaleBarRendererTest {

    @Test
    void testZeroDimensionsReturnsWithoutDrawing() {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        // Should not throw for zero/negative dimensions
        assertDoesNotThrow(() -> ColorScaleBarRenderer.drawColorScaleBar(
                g2d, 0, 0, new StubColorMap(), 0, 100,
                ScaleBarRenderer.Position.LOWER_RIGHT, 0, true));

        assertDoesNotThrow(() -> ColorScaleBarRenderer.drawColorScaleBar(
                g2d, -1, 100, new StubColorMap(), 0, 100,
                ScaleBarRenderer.Position.LOWER_RIGHT, 0, true));

        g2d.dispose();
    }

    @Test
    void testNullColorMapReturnsWithoutDrawing() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        assertDoesNotThrow(() -> ColorScaleBarRenderer.drawColorScaleBar(
                g2d, 200, 200, null, 0, 100,
                ScaleBarRenderer.Position.LOWER_RIGHT, 0, true));

        g2d.dispose();
    }

    @Test
    void testRendersWithoutExceptionForAllPositions() {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        var colorMap = new StubColorMap();

        for (ScaleBarRenderer.Position pos : ScaleBarRenderer.Position.values()) {
            assertDoesNotThrow(() -> ColorScaleBarRenderer.drawColorScaleBar(
                    g2d, 400, 400, colorMap, 0, 100, pos, 0, true),
                    "Failed for position: " + pos);
        }

        g2d.dispose();
    }

    @Test
    void testRendersWithExplicitFontSize() {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        assertDoesNotThrow(() -> ColorScaleBarRenderer.drawColorScaleBar(
                g2d, 400, 400, new StubColorMap(), 0, 1,
                ScaleBarRenderer.Position.LOWER_RIGHT, 24, false));

        g2d.dispose();
    }

    @Test
    void testFormatValueAsciiOnly() {
        String[] values = {
                ColorScaleBarRenderer.formatValue(0),
                ColorScaleBarRenderer.formatValue(42),
                ColorScaleBarRenderer.formatValue(3.14),
                ColorScaleBarRenderer.formatValue(0.001),
                ColorScaleBarRenderer.formatValue(-100),
                ColorScaleBarRenderer.formatValue(1e-5),
        };
        for (String val : values) {
            for (int i = 0; i < val.length(); i++) {
                char c = val.charAt(i);
                assertTrue(c < 128,
                        "Non-ASCII character in formatValue output: '" + val + "' char=" + c);
            }
        }
    }

    @Test
    void testFormatValueIntegerForWholeNumbers() {
        assertEquals("0", ColorScaleBarRenderer.formatValue(0));
        assertEquals("42", ColorScaleBarRenderer.formatValue(42));
        assertEquals("100", ColorScaleBarRenderer.formatValue(100));
        assertEquals("-5", ColorScaleBarRenderer.formatValue(-5));
    }

    @Test
    void testFormatValueDecimalForFractions() {
        String result = ColorScaleBarRenderer.formatValue(3.14);
        assertTrue(result.contains("3.14"), "Expected 3.14, got: " + result);
    }

    @Test
    void testFormatValueScientificForSmallValues() {
        String result = ColorScaleBarRenderer.formatValue(0.001);
        assertTrue(result.contains("e") || result.contains("E"),
                "Expected scientific notation for 0.001, got: " + result);
    }

    @Test
    void testLuminanceContrastDetection() {
        // White -> dark outline (BLACK)
        assertEquals(Color.BLACK, TextRenderUtils.computeOutlineColor(Color.WHITE));
        // Black -> light outline (WHITE)
        assertEquals(Color.WHITE, TextRenderUtils.computeOutlineColor(Color.BLACK));
    }

    /**
     * Minimal stub ColorMap for testing that returns a gradient from black to white.
     */
    private static class StubColorMap implements qupath.lib.color.ColorMaps.ColorMap {
        @Override
        public String getName() {
            return "stub";
        }

        @Override
        public Integer getColor(double value, double minValue, double maxValue) {
            double t = (maxValue == minValue) ? 0.5 : (value - minValue) / (maxValue - minValue);
            t = Math.max(0, Math.min(1, t));
            int gray = (int) (t * 255);
            return (gray << 16) | (gray << 8) | gray;
        }
    }
}
