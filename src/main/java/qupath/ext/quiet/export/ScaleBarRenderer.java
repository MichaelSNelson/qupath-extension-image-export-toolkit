package qupath.ext.quiet.export;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Standalone Java2D utility for rendering scale bars onto exported images.
 * <p>
 * No JavaFX or QuPath viewer dependency -- works with any {@link Graphics2D}.
 * The caller is responsible for providing the correct pixel size (accounting
 * for any downsample applied during export).
 */
public class ScaleBarRenderer {

    /**
     * Corner position for the scale bar.
     */
    public enum Position {
        LOWER_RIGHT,
        LOWER_LEFT,
        UPPER_RIGHT,
        UPPER_LEFT
    }

    // "Nice" bar lengths in microns, targeting ~15% of image width
    private static final double[] NICE_LENGTHS = {
            0.1, 0.25, 0.5, 1, 2, 5, 10, 20, 50, 100,
            200, 250, 500, 1000, 2000, 5000, 10000, 20000, 50000
    };

    /** Minimum allowed font size (pixels). */
    private static final int MIN_FONT_SIZE = 4;
    /** Maximum allowed font size (pixels). */
    private static final int MAX_FONT_SIZE = 200;

    private ScaleBarRenderer() {
        // Utility class
    }

    /**
     * Draw a scale bar with text label onto the given graphics context.
     * <p>
     * Saves and restores the graphics state (composite, hints, font, color)
     * so the caller's state is not affected.
     *
     * @param g2d              the graphics context to draw on
     * @param imageWidth       width of the output image in pixels
     * @param imageHeight      height of the output image in pixels
     * @param pixelSizeMicrons physical size of one pixel in microns
     *                         (must already account for downsample)
     * @param position         which corner to place the bar in
     * @param barColor         primary color of the bar and text (any Color)
     * @param fontSize         font size in pixels; 0 = auto-compute from image dimensions
     * @param boldText         true for bold text, false for plain
     */
    public static void drawScaleBar(Graphics2D g2d, int imageWidth, int imageHeight,
                                     double pixelSizeMicrons, Position position,
                                     Color barColor, int fontSize, boolean boldText) {
        if (pixelSizeMicrons <= 0 || imageWidth <= 0 || imageHeight <= 0) {
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

            // Compute physical image width
            double imagePhysicalWidth = imageWidth * pixelSizeMicrons;

            // Pick a "nice" bar length targeting ~15% of image width
            double targetLength = imagePhysicalWidth * 0.15;
            double barLengthMicrons = pickNiceLength(targetLength);

            // Convert back to pixels
            int barLengthPx = (int) Math.round(barLengthMicrons / pixelSizeMicrons);
            if (barLengthPx < 2) {
                return; // too small to draw
            }

            // Sizing
            int barHeight = Math.max(4, imageHeight / 150);
            int minDim = Math.min(imageWidth, imageHeight);
            int effectiveFontSize = resolveFontSize(fontSize, minDim);
            int margin = Math.max(10, minDim / 40);

            // Format label: >=1000 um -> mm, else um (ASCII-only)
            String label = formatLabel(barLengthMicrons);

            // Font setup
            int fontStyle = boldText ? Font.BOLD : Font.PLAIN;
            Font font = new Font(Font.SANS_SERIF, fontStyle, effectiveFontSize);
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(label);
            int textHeight = fm.getAscent();

            // Compute bar position
            int barX, barY;
            switch (position) {
                case LOWER_LEFT:
                    barX = margin;
                    barY = imageHeight - margin - barHeight;
                    break;
                case UPPER_RIGHT:
                    barX = imageWidth - margin - barLengthPx;
                    barY = margin + textHeight + 4;
                    break;
                case UPPER_LEFT:
                    barX = margin;
                    barY = margin + textHeight + 4;
                    break;
                case LOWER_RIGHT:
                default:
                    barX = imageWidth - margin - barLengthPx;
                    barY = imageHeight - margin - barHeight;
                    break;
            }

            // Text centered above bar
            int textX = barX + (barLengthPx - textWidth) / 2;
            int textY = barY - 4;

            Color primary = barColor != null ? barColor : Color.WHITE;
            Color outline = computeOutlineColor(primary);

            // Draw bar with contrast outline
            g2d.setColor(outline);
            g2d.fillRect(barX - 1, barY - 1, barLengthPx + 2, barHeight + 2);
            g2d.setColor(primary);
            g2d.fillRect(barX, barY, barLengthPx, barHeight);

            // Draw text with 8-direction outline for visibility on any background
            drawOutlinedText(g2d, label, textX, textY, primary, outline);

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

    /**
     * Resolve the effective font size. If the requested size is 0 (auto),
     * compute from image dimensions. Otherwise clamp to the allowed range.
     */
    private static int resolveFontSize(int requested, int minImageDim) {
        if (requested <= 0) {
            return Math.max(12, minImageDim / 50);
        }
        return Math.max(MIN_FONT_SIZE, Math.min(requested, MAX_FONT_SIZE));
    }

    /**
     * Compute the outline/contrast color from luminance of the primary color.
     * Uses the standard luminance formula: (0.299*R + 0.587*G + 0.114*B) / 255.
     * Returns BLACK for bright colors, WHITE for dark colors.
     */
    private static Color computeOutlineColor(Color primary) {
        double luminance = (0.299 * primary.getRed()
                + 0.587 * primary.getGreen()
                + 0.114 * primary.getBlue()) / 255.0;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Pick the closest "nice" bar length to the target.
     */
    private static double pickNiceLength(double targetMicrons) {
        double best = NICE_LENGTHS[0];
        double bestDiff = Math.abs(targetMicrons - best);
        for (double candidate : NICE_LENGTHS) {
            double diff = Math.abs(targetMicrons - candidate);
            if (diff < bestDiff) {
                best = candidate;
                bestDiff = diff;
            }
        }
        return best;
    }

    /**
     * Format the bar length as a human-readable label.
     * Uses mm for lengths >= 1000 um, otherwise um. ASCII-only.
     */
    private static String formatLabel(double microns) {
        if (microns >= 1000) {
            double mm = microns / 1000.0;
            if (mm == Math.floor(mm)) {
                return String.format("%d mm", (int) mm);
            }
            return String.format("%.1f mm", mm);
        }
        if (microns == Math.floor(microns)) {
            return String.format("%d um", (int) microns);
        }
        if (microns >= 1) {
            return String.format("%.1f um", microns);
        }
        return String.format("%.2f um", microns);
    }

    /**
     * Draw text with an outline for contrast on any background.
     * Uses 8-direction offset technique.
     */
    private static void drawOutlinedText(Graphics2D g2d, String text,
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
