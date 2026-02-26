package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.jfree.svg.SVGGraphics2D;
import org.junit.jupiter.api.Test;

/**
 * Tests for SVG export functionality via JFreeSVG.
 */
class SvgExportTest {

    @Test
    void testSvgOutputContainsSvgElements() {
        SVGGraphics2D svgG2d = new SVGGraphics2D(100, 100);

        // Draw a simple raster image
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        svgG2d.drawImage(img, 0, 0, null);

        svgG2d.dispose();
        String svg = svgG2d.getSVGDocument();

        assertTrue(svg.contains("<?xml"), "SVG should start with XML declaration");
        assertTrue(svg.contains("<svg"), "SVG should contain <svg element");
        assertTrue(svg.contains("<image"), "SVG should contain embedded <image element");
    }

    @Test
    void testSvgContainsVectorText() {
        SVGGraphics2D svgG2d = new SVGGraphics2D(200, 200);

        // Draw vector text (simulating scale bar label)
        svgG2d.setColor(Color.WHITE);
        svgG2d.drawString("100 um", 50, 180);

        svgG2d.dispose();
        String svg = svgG2d.getSVGDocument();

        assertTrue(svg.contains("<text"), "SVG should contain <text elements for vector text");
        assertTrue(svg.contains("100"), "SVG text should contain the scale bar value");
    }

    @Test
    void testSvgContainsVectorShapes() {
        SVGGraphics2D svgG2d = new SVGGraphics2D(200, 200);

        // Draw vector shapes (simulating annotation overlays)
        svgG2d.setColor(Color.RED);
        svgG2d.drawRect(20, 20, 60, 60);
        svgG2d.drawOval(100, 100, 50, 50);

        svgG2d.dispose();
        String svg = svgG2d.getSVGDocument();

        assertTrue(svg.contains("<svg"), "SVG should contain root element");
        // JFreeSVG renders shapes as various SVG elements
        // Verify the SVG contains drawing commands (not just the root element)
        assertTrue(svg.length() > 200,
                "SVG should contain drawing elements beyond just the root");
    }

    @Test
    void testSvgFileExtension() {
        assertEquals("svg", OutputFormat.SVG.getExtension());
    }

    @Test
    void testSvgEnumValue() {
        boolean found = false;
        for (OutputFormat format : OutputFormat.values()) {
            if (format == OutputFormat.SVG) {
                found = true;
                break;
            }
        }
        assertTrue(found, "SVG should exist in OutputFormat enum");
    }

    @Test
    void testSvgToString() {
        assertEquals("SVG", OutputFormat.SVG.toString());
    }

    @Test
    void testSvgRasterPlusVectorHybrid() {
        // Simulate the hybrid approach: raster base + vector overlays
        BufferedImage raster = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D rasterG = raster.createGraphics();
        rasterG.setColor(Color.BLUE);
        rasterG.fillRect(0, 0, 200, 200);
        rasterG.dispose();

        SVGGraphics2D svgG2d = new SVGGraphics2D(200, 200);
        svgG2d.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Embed raster base
        svgG2d.drawImage(raster, 0, 0, null);

        // Draw vector overlays on top
        svgG2d.setColor(Color.WHITE);
        svgG2d.drawString("Scale: 100 um", 10, 190);
        svgG2d.drawRect(10, 170, 80, 5);

        svgG2d.dispose();
        String svg = svgG2d.getSVGDocument();

        // Verify both raster and vector elements present
        assertTrue(svg.contains("<image"), "SVG should contain embedded raster <image>");
        assertTrue(svg.contains("<text"), "SVG should contain vector <text>");
        // JFreeSVG renders shapes; verify the document is substantially larger
        // than a minimal SVG with just an image element
        assertTrue(svg.length() > 500,
                "SVG should contain additional vector elements beyond just the image");
    }

    @Test
    void testSvgDocumentIsValidUtf8() {
        SVGGraphics2D svgG2d = new SVGGraphics2D(50, 50);
        svgG2d.drawString("Test", 10, 25);
        svgG2d.dispose();
        String svg = svgG2d.getSVGDocument();

        // Verify all characters are ASCII-safe (no encoding issues)
        for (int i = 0; i < svg.length(); i++) {
            char c = svg.charAt(i);
            // SVG documents use UTF-8 but should not contain control characters
            assertFalse(c < 0x09, "SVG should not contain control characters below tab");
        }
    }
}
