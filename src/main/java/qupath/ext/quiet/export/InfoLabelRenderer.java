package qupath.ext.quiet.export;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Standalone Java2D utility for rendering per-image metadata text stamps
 * onto exported images (e.g., image name, pixel size, date).
 * <p>
 * Follows the same pattern as {@link PanelLabelRenderer} and {@link ScaleBarRenderer}.
 * No JavaFX or QuPath viewer dependency -- works with any {@link Graphics2D}.
 */
public class InfoLabelRenderer {

    private InfoLabelRenderer() {
        // Utility class
    }

    /**
     * Draw an info label onto the given graphics context.
     * <p>
     * Saves and restores the graphics state (composite, hints, font, color)
     * so the caller's state is not affected.
     *
     * @param g2d         the graphics context to draw on
     * @param imageWidth  width of the output image in pixels
     * @param imageHeight height of the output image in pixels
     * @param text        the resolved text to draw
     * @param position    which corner to place the label in
     * @param fontSize    font size in pixels; 0 = auto-compute from image dimensions
     * @param bold        true for bold text, false for plain
     * @param color       primary color of the label text
     */
    public static void drawInfoLabel(Graphics2D g2d,
                                      int imageWidth, int imageHeight,
                                      String text,
                                      ScaleBarRenderer.Position position,
                                      int fontSize, boolean bold,
                                      Color color) {
        if (imageWidth <= 0 || imageHeight <= 0 || text == null || text.isEmpty()) {
            return;
        }

        // Save graphics state
        Composite savedComposite = g2d.getComposite();
        Object savedAA = g2d.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        Font savedFont = g2d.getFont();
        Color savedColor = g2d.getColor();

        try {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int minDim = Math.min(imageWidth, imageHeight);

            int effectiveFontSize;
            if (fontSize <= 0) {
                effectiveFontSize = Math.max(12, minDim / 40);
            } else {
                effectiveFontSize = TextRenderUtils.resolveFontSize(fontSize, minDim);
            }

            int margin = Math.max(10, minDim / 40);

            // Font setup
            int fontStyle = bold ? Font.BOLD : Font.PLAIN;
            Font font = new Font(Font.SANS_SERIF, fontStyle, effectiveFontSize);
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textAscent = fm.getAscent();

            // Compute text position based on corner
            int textX, textY;
            switch (position) {
                case LOWER_LEFT:
                    textX = margin;
                    textY = imageHeight - margin;
                    break;
                case UPPER_RIGHT:
                    textX = imageWidth - margin - textWidth;
                    textY = margin + textAscent;
                    break;
                case UPPER_LEFT:
                    textX = margin;
                    textY = margin + textAscent;
                    break;
                case LOWER_RIGHT:
                default:
                    textX = imageWidth - margin - textWidth;
                    textY = imageHeight - margin;
                    break;
            }

            Color primary = color != null ? color : Color.WHITE;
            Color outline = TextRenderUtils.computeOutlineColor(primary);

            TextRenderUtils.drawOutlinedText(g2d, text, textX, textY, primary, outline);

        } finally {
            // Restore graphics state
            g2d.setComposite(savedComposite);
            if (savedAA != null) {
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedAA);
            }
            g2d.setFont(savedFont);
            g2d.setColor(savedColor);
        }
    }
}
