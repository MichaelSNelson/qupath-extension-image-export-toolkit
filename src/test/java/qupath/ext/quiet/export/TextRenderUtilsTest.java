package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class TextRenderUtilsTest {

    @Test
    void testComputeOutlineColorWhiteReturnsDark() {
        assertEquals(Color.BLACK, TextRenderUtils.computeOutlineColor(Color.WHITE));
    }

    @Test
    void testComputeOutlineColorBlackReturnsLight() {
        assertEquals(Color.WHITE, TextRenderUtils.computeOutlineColor(Color.BLACK));
    }

    @Test
    void testComputeOutlineColorYellowReturnsDark() {
        // Yellow (255, 255, 0) has high luminance
        assertEquals(Color.BLACK, TextRenderUtils.computeOutlineColor(Color.YELLOW));
    }

    @Test
    void testComputeOutlineColorDarkBlueReturnsLight() {
        // Dark blue (0, 0, 128) has low luminance
        assertEquals(Color.WHITE, TextRenderUtils.computeOutlineColor(new Color(0, 0, 128)));
    }

    @Test
    void testResolveFontSizeAutoComputesFromDimension() {
        // Auto (requested=0) should compute: max(12, minDim / 50)
        int result = TextRenderUtils.resolveFontSize(0, 1000);
        assertEquals(20, result); // 1000 / 50 = 20

        int result2 = TextRenderUtils.resolveFontSize(0, 100);
        assertEquals(12, result2); // max(12, 100/50=2) = 12
    }

    @Test
    void testResolveFontSizeExplicitReturnsExact() {
        assertEquals(24, TextRenderUtils.resolveFontSize(24, 1000));
        assertEquals(16, TextRenderUtils.resolveFontSize(16, 500));
    }

    @Test
    void testResolveFontSizeClampingMin() {
        // Requested 1 should be clamped to MIN_FONT_SIZE (4)
        assertEquals(TextRenderUtils.MIN_FONT_SIZE,
                TextRenderUtils.resolveFontSize(1, 1000));
    }

    @Test
    void testResolveFontSizeClampingMax() {
        // Requested 500 should be clamped to MAX_FONT_SIZE (200)
        assertEquals(TextRenderUtils.MAX_FONT_SIZE,
                TextRenderUtils.resolveFontSize(500, 1000));
    }

    @Test
    void testDrawOutlinedTextDoesNotThrow() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        assertDoesNotThrow(() -> TextRenderUtils.drawOutlinedText(
                g2d, "Test", 50, 50, Color.WHITE, Color.BLACK));

        g2d.dispose();
    }
}
