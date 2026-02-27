package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.SVGHints;
import org.junit.jupiter.api.Test;

/**
 * Tests for SVG vector annotation rendering.
 * <p>
 * Validates that the SVG output from the vector annotation approach
 * contains proper {@code <g>} groups, {@code <path>} elements, and
 * fill/stroke color attributes -- matching the behavior of
 * {@code RenderedImageExporter.paintObjectsAsSvg()}.
 */
class SvgVectorAnnotationTest {

    /**
     * Simulates the paintObjectsAsSvg approach: group shapes by class,
     * apply SVGHints for grouping, fill + stroke each shape.
     */
    private String generateSvgWithAnnotations() {
        SVGGraphics2D g2d = new SVGGraphics2D(400, 400);

        // Group 1: "Tumor" class with two shapes
        g2d.setRenderingHint(SVGHints.KEY_BEGIN_GROUP, "Tumor");
        g2d.setRenderingHint(SVGHints.KEY_ELEMENT_TITLE, "Tumor");

        Shape rect = new Rectangle2D.Double(50, 50, 100, 80);
        g2d.setRenderingHint(SVGHints.KEY_ELEMENT_ID, "Tumor_1");
        g2d.setColor(new Color(255, 0, 0, 64));
        g2d.fill(rect);
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(rect);

        Shape ellipse = new Ellipse2D.Double(200, 100, 60, 40);
        g2d.setRenderingHint(SVGHints.KEY_ELEMENT_ID, "Tumor_2");
        g2d.setColor(new Color(255, 0, 0, 64));
        g2d.fill(ellipse);
        g2d.setColor(Color.RED);
        g2d.draw(ellipse);

        g2d.setRenderingHint(SVGHints.KEY_END_GROUP, "true");

        // Group 2: "Stroma" class with one shape
        g2d.setRenderingHint(SVGHints.KEY_BEGIN_GROUP, "Stroma");
        g2d.setRenderingHint(SVGHints.KEY_ELEMENT_TITLE, "Stroma");

        Shape rect2 = new Rectangle2D.Double(300, 200, 80, 120);
        g2d.setRenderingHint(SVGHints.KEY_ELEMENT_ID, "Stroma_1");
        g2d.setColor(new Color(0, 128, 0, 64));
        g2d.fill(rect2);
        g2d.setColor(new Color(0, 128, 0));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(rect2);

        g2d.setRenderingHint(SVGHints.KEY_END_GROUP, "true");

        g2d.dispose();
        return g2d.getSVGDocument();
    }

    @Test
    void testSvgContainsGroupElements() {
        String svg = generateSvgWithAnnotations();

        assertTrue(svg.contains("<g"), "SVG should contain <g> group elements");
        assertTrue(svg.contains("id=\"Tumor\"") || svg.contains("id='Tumor'"),
                "SVG should contain a group with id 'Tumor'");
        assertTrue(svg.contains("id=\"Stroma\"") || svg.contains("id='Stroma'"),
                "SVG should contain a group with id 'Stroma'");
    }

    @Test
    void testSvgContainsPathElements() {
        String svg = generateSvgWithAnnotations();

        // JFreeSVG renders shapes as various SVG elements; rectangles may be
        // <rect> or <path>, ellipses may be <ellipse> or <path>.
        // Check for the presence of shape-drawing elements.
        boolean hasShapes = svg.contains("<rect") || svg.contains("<path")
                || svg.contains("<ellipse") || svg.contains("<circle");
        assertTrue(hasShapes,
                "SVG should contain shape elements (rect, path, ellipse, or circle)");
    }

    @Test
    void testSvgPathHasFillAndStroke() {
        String svg = generateSvgWithAnnotations();

        assertTrue(svg.contains("fill=") || svg.contains("fill:"),
                "SVG should contain fill attributes");
        assertTrue(svg.contains("stroke=") || svg.contains("stroke:"),
                "SVG should contain stroke attributes");
    }

    @Test
    void testSvgContainsElementIds() {
        String svg = generateSvgWithAnnotations();

        // The element IDs set via SVGHints.KEY_ELEMENT_ID should appear
        assertTrue(svg.contains("Tumor_1"), "SVG should contain element ID 'Tumor_1'");
        assertTrue(svg.contains("Tumor_2"), "SVG should contain element ID 'Tumor_2'");
        assertTrue(svg.contains("Stroma_1"), "SVG should contain element ID 'Stroma_1'");
    }

    @Test
    void testSvgContainsTitleElements() {
        String svg = generateSvgWithAnnotations();

        // SVGHints.KEY_ELEMENT_TITLE should produce <title> elements
        assertTrue(svg.contains("<title>Tumor</title>"),
                "SVG should contain <title>Tumor</title>");
        assertTrue(svg.contains("<title>Stroma</title>"),
                "SVG should contain <title>Stroma</title>");
    }

    @Test
    void testSvgHasMultipleGroups() {
        String svg = generateSvgWithAnnotations();

        // Count <g occurrences -- should have at least 2 (Tumor + Stroma)
        int groupCount = 0;
        int idx = 0;
        while ((idx = svg.indexOf("<g ", idx)) != -1) {
            groupCount++;
            idx++;
        }
        // Also check for <g> without attributes
        idx = 0;
        while ((idx = svg.indexOf("<g>", idx)) != -1) {
            groupCount++;
            idx++;
        }

        assertTrue(groupCount >= 2,
                "SVG should contain at least 2 <g> group elements, found " + groupCount);
    }

    @Test
    void testSvgIsWellFormed() {
        String svg = generateSvgWithAnnotations();

        assertTrue(svg.contains("<?xml"), "SVG should start with XML declaration");
        assertTrue(svg.contains("<svg"), "SVG should contain root <svg> element");
        assertTrue(svg.contains("</svg>"), "SVG should close the root element");
    }
}
