package qupath.ext.quiet.export;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

/**
 * Standalone Java2D utility for rendering a magnified inset panel onto
 * an exported image. Crops a region from the composited result, scales
 * it up, and draws it in a corner with an optional source frame and
 * connecting lines.
 * <p>
 * Because the inset is cropped from the final composited image
 * (including overlays), this renderer must be called <b>last</b>
 * in the overlay pipeline -- after all other overlays are drawn
 * but before {@code g2d.dispose()}.
 * <p>
 * Follows the same stateless utility pattern as {@link ScaleBarRenderer}
 * and {@link PanelLabelRenderer}.
 */
public class InsetRenderer {

    private InsetRenderer() {
        // Utility class
    }

    /**
     * Draw a magnified inset panel onto the given image.
     * <p>
     * The source region is specified as fractions (0-1) of the image dimensions,
     * making this function applicable across images of different sizes in batch mode.
     *
     * @param g2d             the graphics context to draw on (from the composited result)
     * @param fullImage       the composited result image to crop from
     * @param sourceX         left edge of source region as fraction of image width (0-1)
     * @param sourceY         top edge of source region as fraction of image height (0-1)
     * @param sourceW         width of source region as fraction of image width (0-1)
     * @param sourceH         height of source region as fraction of image height (0-1)
     * @param magnification   integer magnification factor (2-16)
     * @param position        which corner to place the inset in
     * @param frameColor      color for the source frame and inset border
     * @param frameWidth      frame stroke width in pixels; 0 = auto-compute
     * @param connectingLines true to draw dashed lines from source to inset corners
     */
    public static void drawInset(Graphics2D g2d, BufferedImage fullImage,
                                  double sourceX, double sourceY,
                                  double sourceW, double sourceH,
                                  int magnification,
                                  ScaleBarRenderer.Position position,
                                  Color frameColor, int frameWidth,
                                  boolean connectingLines) {

        int imgW = fullImage.getWidth();
        int imgH = fullImage.getHeight();
        if (imgW <= 0 || imgH <= 0) return;

        // Convert fractional region to pixel coordinates and clamp
        int sx = (int) Math.round(sourceX * imgW);
        int sy = (int) Math.round(sourceY * imgH);
        int sw = (int) Math.round(sourceW * imgW);
        int sh = (int) Math.round(sourceH * imgH);

        sx = Math.max(0, Math.min(sx, imgW - 1));
        sy = Math.max(0, Math.min(sy, imgH - 1));
        sw = Math.max(1, Math.min(sw, imgW - sx));
        sh = Math.max(1, Math.min(sh, imgH - sy));

        // Skip if source region is too small
        if (sw < 10 || sh < 10) return;

        // Compute magnified dimensions, capping at 2048px
        int mag = Math.max(2, Math.min(magnification, 16));
        int insetW = sw * mag;
        int insetH = sh * mag;
        if (insetW > 2048 || insetH > 2048) {
            int maxSrc = Math.max(sw, sh);
            mag = Math.max(2, 2048 / maxSrc);
            insetW = sw * mag;
            insetH = sh * mag;
        }

        // Also cap inset to half the image dimensions
        if (insetW > imgW / 2 || insetH > imgH / 2) {
            int maxAllowed = Math.min(imgW / 2, imgH / 2);
            int maxSrc = Math.max(sw, sh);
            mag = Math.max(2, maxAllowed / maxSrc);
            insetW = sw * mag;
            insetH = sh * mag;
        }

        if (insetW < 10 || insetH < 10) return;

        // Crop source region
        BufferedImage cropped = fullImage.getSubimage(sx, sy, sw, sh);

        // Scale up with nearest-neighbor (preserves pixel structure)
        BufferedImage magnified = new BufferedImage(insetW, insetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D mg = magnified.createGraphics();
        mg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        mg.drawImage(cropped, 0, 0, insetW, insetH, null);
        mg.dispose();

        // Frame width
        int minDim = Math.min(imgW, imgH);
        int effectiveFrameWidth = frameWidth > 0 ? frameWidth : Math.max(2, minDim / 300);
        int margin = Math.max(10, minDim / 40);

        Color color = frameColor != null ? frameColor : Color.YELLOW;

        // Save graphics state
        Composite savedComposite = g2d.getComposite();
        Stroke savedStroke = g2d.getStroke();
        Color savedColor = g2d.getColor();

        try {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            // Compute inset position
            int ix, iy;
            switch (position) {
                case LOWER_LEFT:
                    ix = margin;
                    iy = imgH - margin - insetH;
                    break;
                case UPPER_LEFT:
                    ix = margin;
                    iy = margin;
                    break;
                case LOWER_RIGHT:
                    ix = imgW - margin - insetW;
                    iy = imgH - margin - insetH;
                    break;
                case UPPER_RIGHT:
                default:
                    ix = imgW - margin - insetW;
                    iy = margin;
                    break;
            }

            // Draw source frame rectangle on main image
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(effectiveFrameWidth));
            g2d.drawRect(sx, sy, sw, sh);

            // Draw magnified inset image
            g2d.drawImage(magnified, ix, iy, null);

            // Draw inset border
            g2d.drawRect(ix, iy, insetW, insetH);

            // Optional connecting lines (dashed)
            if (connectingLines) {
                float dashLength = Math.max(4, minDim / 100.0f);
                g2d.setStroke(new BasicStroke(
                        Math.max(1, effectiveFrameWidth / 2),
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER,
                        10.0f,
                        new float[]{dashLength, dashLength},
                        0.0f));
                g2d.setColor(color);

                // Connect nearest corners of source and inset
                // Top-left of source to nearest corner of inset
                // Bottom-right of source to nearest corner of inset
                int srcCenterX = sx + sw / 2;
                int srcCenterY = sy + sh / 2;
                int insetCenterX = ix + insetW / 2;
                int insetCenterY = iy + insetH / 2;

                // Determine which corners to connect based on relative positions
                if (insetCenterX > srcCenterX) {
                    // Inset is to the right of source
                    if (insetCenterY > srcCenterY) {
                        // Inset below-right: connect source BR to inset TL
                        g2d.drawLine(sx + sw, sy + sh, ix, iy);
                        g2d.drawLine(sx + sw, sy, ix, iy);
                    } else {
                        // Inset above-right: connect source TR to inset BL
                        g2d.drawLine(sx + sw, sy, ix, iy + insetH);
                        g2d.drawLine(sx + sw, sy + sh, ix, iy + insetH);
                    }
                } else {
                    // Inset is to the left of source
                    if (insetCenterY > srcCenterY) {
                        // Inset below-left: connect source BL to inset TR
                        g2d.drawLine(sx, sy + sh, ix + insetW, iy);
                        g2d.drawLine(sx, sy, ix + insetW, iy);
                    } else {
                        // Inset above-left: connect source TL to inset BR
                        g2d.drawLine(sx, sy, ix + insetW, iy + insetH);
                        g2d.drawLine(sx, sy + sh, ix + insetW, iy + insetH);
                    }
                }
            }

        } finally {
            g2d.setComposite(savedComposite);
            g2d.setStroke(savedStroke);
            g2d.setColor(savedColor);
        }
    }
}
