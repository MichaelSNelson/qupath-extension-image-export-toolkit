package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RenderedExportConfigTest {

    @TempDir
    File tempDir;

    @Test
    void testBuildValidClassifierConfig() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("myClassifier")
                .overlayOpacity(0.75)
                .downsample(8.0)
                .format(OutputFormat.TIFF)
                .outputDirectory(tempDir)
                .includeAnnotations(true)
                .fillAnnotations(true)
                .showNames(false)
                .addToWorkflow(false)
                .build();

        assertEquals(RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY, config.getRenderMode());
        assertEquals("myClassifier", config.getClassifierName());
        assertEquals(0.75, config.getOverlayOpacity(), 0.001);
        assertEquals(8.0, config.getDownsample(), 0.001);
        assertEquals(OutputFormat.TIFF, config.getFormat());
        assertEquals(tempDir, config.getOutputDirectory());
        assertTrue(config.isIncludeAnnotations());
        assertTrue(config.isFillAnnotations());
        assertFalse(config.isShowNames());
        assertFalse(config.isAddToWorkflow());
    }

    @Test
    void testOpacityClipping() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .overlayOpacity(1.5)
                .outputDirectory(tempDir)
                .build();
        assertEquals(1.0, config.getOverlayOpacity(), 0.001);

        RenderedExportConfig config2 = new RenderedExportConfig.Builder()
                .classifierName("test")
                .overlayOpacity(-0.5)
                .outputDirectory(tempDir)
                .build();
        assertEquals(0.0, config2.getOverlayOpacity(), 0.001);
    }

    @Test
    void testNullClassifierNameThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new RenderedExportConfig.Builder()
                        .outputDirectory(tempDir)
                        .build());
    }

    @Test
    void testBlankClassifierNameThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new RenderedExportConfig.Builder()
                        .classifierName("   ")
                        .outputDirectory(tempDir)
                        .build());
    }

    @Test
    void testNullOutputDirectoryThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new RenderedExportConfig.Builder()
                        .classifierName("test")
                        .build());
    }

    @Test
    void testDownsampleBelowOneThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new RenderedExportConfig.Builder()
                        .classifierName("test")
                        .outputDirectory(tempDir)
                        .downsample(0.5)
                        .build());
    }

    @Test
    void testBuildOutputFilenamePng() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .format(OutputFormat.PNG)
                .outputDirectory(tempDir)
                .build();

        assertEquals("my_image.png", config.buildOutputFilename("my_image"));
    }

    @Test
    void testBuildOutputFilenameTiff() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .format(OutputFormat.TIFF)
                .outputDirectory(tempDir)
                .build();

        assertEquals("sample_01.tif", config.buildOutputFilename("sample_01"));
    }

    @Test
    void testBuildOutputFilenameJpeg() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .format(OutputFormat.JPEG)
                .outputDirectory(tempDir)
                .build();

        assertEquals("sample_01.jpg", config.buildOutputFilename("sample_01"));
    }

    @Test
    void testBuildOutputFilenameHandlesEmptyName() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .format(OutputFormat.PNG)
                .outputDirectory(tempDir)
                .build();

        String result = config.buildOutputFilename("");
        assertNotNull(result);
        assertTrue(result.endsWith(".png"));
        assertFalse(result.startsWith("."));
    }

    @Test
    void testDefaults() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .outputDirectory(tempDir)
                .build();

        assertEquals(RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY, config.getRenderMode());
        assertEquals(0.5, config.getOverlayOpacity(), 0.001);
        assertEquals(4.0, config.getDownsample(), 0.001);
        assertEquals(OutputFormat.PNG, config.getFormat());
        assertFalse(config.isIncludeAnnotations());
        assertTrue(config.isIncludeDetections());
        assertFalse(config.isFillAnnotations());
        assertFalse(config.isShowNames());
        assertTrue(config.isAddToWorkflow());
        // Scale bar defaults
        assertFalse(config.isShowScaleBar());
        assertEquals("#FFFFFF", config.getScaleBarColorHex());
        assertEquals(0, config.getScaleBarFontSize());
        assertTrue(config.isScaleBarBoldText());
        assertEquals(java.awt.Color.WHITE, config.getScaleBarColorAsAwt());
    }

    @Test
    void testScaleBarColorHexConversion() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .outputDirectory(tempDir)
                .scaleBarColorHex("#FF0000")
                .scaleBarFontSize(24)
                .scaleBarBoldText(false)
                .build();

        assertEquals("#FF0000", config.getScaleBarColorHex());
        assertEquals(java.awt.Color.RED, config.getScaleBarColorAsAwt());
        assertEquals(24, config.getScaleBarFontSize());
        assertFalse(config.isScaleBarBoldText());
    }

    @Test
    void testScaleBarColorHexFallback() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .outputDirectory(tempDir)
                .scaleBarColorHex("not-a-color")
                .build();

        // Invalid hex should fall back to white
        assertEquals(java.awt.Color.WHITE, config.getScaleBarColorAsAwt());
    }

    @Test
    void testObjectOverlayModeDoesNotRequireClassifier() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .renderMode(RenderedExportConfig.RenderMode.OBJECT_OVERLAY)
                .includeAnnotations(true)
                .outputDirectory(tempDir)
                .build();

        assertEquals(RenderedExportConfig.RenderMode.OBJECT_OVERLAY, config.getRenderMode());
        assertNull(config.getClassifierName());
        assertTrue(config.isIncludeAnnotations());
    }

    @Test
    void testObjectOverlayModeRequiresAtLeastOneObjectType() {
        assertThrows(IllegalArgumentException.class, () ->
                new RenderedExportConfig.Builder()
                        .renderMode(RenderedExportConfig.RenderMode.OBJECT_OVERLAY)
                        .includeAnnotations(false)
                        .includeDetections(false)
                        .outputDirectory(tempDir)
                        .build());
    }

    @Test
    void testObjectOverlayModeWithDetectionsOnly() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .renderMode(RenderedExportConfig.RenderMode.OBJECT_OVERLAY)
                .includeAnnotations(false)
                .includeDetections(true)
                .outputDirectory(tempDir)
                .build();

        assertFalse(config.isIncludeAnnotations());
        assertTrue(config.isIncludeDetections());
    }

    @Test
    void testDensityMapModeRequiresDensityMapName() {
        assertThrows(IllegalArgumentException.class, () ->
                new RenderedExportConfig.Builder()
                        .renderMode(RenderedExportConfig.RenderMode.DENSITY_MAP_OVERLAY)
                        .outputDirectory(tempDir)
                        .build());
    }

    @Test
    void testDensityMapModeBlankNameThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new RenderedExportConfig.Builder()
                        .renderMode(RenderedExportConfig.RenderMode.DENSITY_MAP_OVERLAY)
                        .densityMapName("   ")
                        .outputDirectory(tempDir)
                        .build());
    }

    @Test
    void testDensityMapModeWithValidName() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .renderMode(RenderedExportConfig.RenderMode.DENSITY_MAP_OVERLAY)
                .densityMapName("my_density_map")
                .colormapName("Magma")
                .outputDirectory(tempDir)
                .build();

        assertEquals(RenderedExportConfig.RenderMode.DENSITY_MAP_OVERLAY, config.getRenderMode());
        assertEquals("my_density_map", config.getDensityMapName());
        assertEquals("Magma", config.getColormapName());
    }

    @Test
    void testColorScaleBarDefaults() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .outputDirectory(tempDir)
                .build();

        assertFalse(config.isShowColorScaleBar());
        assertEquals(ScaleBarRenderer.Position.LOWER_RIGHT, config.getColorScaleBarPosition());
        assertEquals(0, config.getColorScaleBarFontSize());
        assertTrue(config.isColorScaleBarBoldText());
    }

    @Test
    void testColormapNameDefault() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .renderMode(RenderedExportConfig.RenderMode.DENSITY_MAP_OVERLAY)
                .densityMapName("test_dm")
                .outputDirectory(tempDir)
                .build();

        assertEquals("Viridis", config.getColormapName());
    }

    @Test
    void testPanelLabelDefaults() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .outputDirectory(tempDir)
                .build();

        assertFalse(config.isShowPanelLabel());
        assertNull(config.getPanelLabelText());
        assertEquals(ScaleBarRenderer.Position.UPPER_LEFT, config.getPanelLabelPosition());
        assertEquals(0, config.getPanelLabelFontSize());
        assertTrue(config.isPanelLabelBold());
    }

    @Test
    void testPanelLabelSettings() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .showPanelLabel(true)
                .panelLabelText("A")
                .panelLabelPosition(ScaleBarRenderer.Position.LOWER_RIGHT)
                .panelLabelFontSize(28)
                .panelLabelBold(false)
                .outputDirectory(tempDir)
                .build();

        assertTrue(config.isShowPanelLabel());
        assertEquals("A", config.getPanelLabelText());
        assertEquals(ScaleBarRenderer.Position.LOWER_RIGHT, config.getPanelLabelPosition());
        assertEquals(28, config.getPanelLabelFontSize());
        assertFalse(config.isPanelLabelBold());
    }

    @Test
    void testColorScaleBarSettings() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .renderMode(RenderedExportConfig.RenderMode.DENSITY_MAP_OVERLAY)
                .densityMapName("test_dm")
                .showColorScaleBar(true)
                .colorScaleBarPosition(ScaleBarRenderer.Position.UPPER_LEFT)
                .colorScaleBarFontSize(18)
                .colorScaleBarBoldText(false)
                .outputDirectory(tempDir)
                .build();

        assertTrue(config.isShowColorScaleBar());
        assertEquals(ScaleBarRenderer.Position.UPPER_LEFT, config.getColorScaleBarPosition());
        assertEquals(18, config.getColorScaleBarFontSize());
        assertFalse(config.isColorScaleBarBoldText());
    }

    @Test
    void testBuildOutputFilenameSvg() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .format(OutputFormat.SVG)
                .outputDirectory(tempDir)
                .build();

        assertEquals("my_image.svg", config.buildOutputFilename("my_image"));
    }

    @Test
    void testSvgFormatAccepted() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .format(OutputFormat.SVG)
                .outputDirectory(tempDir)
                .build();

        assertEquals(OutputFormat.SVG, config.getFormat());
    }
}
