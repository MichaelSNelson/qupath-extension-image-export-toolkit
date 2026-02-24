package qupath.ext.quiet.export;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Shared text rendering utilities for drawing outlined text onto exported images.
 * <p>
 * Extracted from {@link ScaleBarRenderer} and {@link ColorScaleBarRenderer} to
 * eliminate code duplication and provide a single source of truth for text rendering
 * across all overlay renderers (scale bars, panel labels, etc.).
 */
public final class TextRenderUtils {

    /** Minimum allowed font size (pixels). */
    static final int MIN_FONT_SIZE = 4;
    /** Maximum allowed font size (pixels). */
    static final int MAX_FONT_SIZE = 200;

    private TextRenderUtils() {
        // Utility class
    }

    /**
     * Resolve the effective font size. If the requested size is 0 (auto),
     * compute from image dimensions. Otherwise clamp to the allowed range.
     *
     * @param requested   the requested font size (0 = auto)
     * @param minImageDim the smaller of the image's width and height
     * @return the resolved font size in pixels
     */
    public static int resolveFontSize(int requested, int minImageDim) {
        if (requested <= 0) {
            return Math.max(12, minImageDim / 50);
        }
        return Math.max(MIN_FONT_SIZE, Math.min(requested, MAX_FONT_SIZE));
    }

    /**
     * Compute the outline/contrast color from luminance of the primary color.
     * Uses the standard luminance formula: (0.299*R + 0.587*G + 0.114*B) / 255.
     * Returns BLACK for bright colors, WHITE for dark colors.
     *
     * @param primary the primary text color
     * @return a contrasting outline color (BLACK or WHITE)
     */
    public static Color computeOutlineColor(Color primary) {
        double luminance = (0.299 * primary.getRed()
                + 0.587 * primary.getGreen()
                + 0.114 * primary.getBlue()) / 255.0;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Draw text with an outline for contrast on any background.
     * Uses 8-direction offset technique to create a visible outline around the text.
     *
     * @param g2d     the graphics context to draw on
     * @param text    the text to draw
     * @param x       the x position for the text baseline
     * @param y       the y position for the text baseline
     * @param primary the primary text color
     * @param outline the outline color (drawn behind the primary text)
     */
    public static void drawOutlinedText(Graphics2D g2d, String text,
                                         int x, int y,
                                         Color primary, Color outline) {
        g2d.setColor(outline);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    g2d.drawString(text, x + dx, y + dy);
                }
            }
        }
        g2d.setColor(primary);
        g2d.drawString(text, x, y);
    }
}
