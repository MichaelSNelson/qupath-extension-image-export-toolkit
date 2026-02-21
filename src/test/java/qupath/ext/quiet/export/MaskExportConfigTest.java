package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MaskExportConfigTest {

    @TempDir
    File tempDir;

    @Test
    void testBuildValidConfig() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .maskType(MaskExportConfig.MaskType.GRAYSCALE_LABELS)
                .selectedClassifications(List.of("Tumor", "Stroma", "Necrosis"))
                .backgroundLabel(0)
                .boundaryLabel(-1)
                .objectSource(MaskExportConfig.ObjectSource.ANNOTATIONS)
                .downsample(4.0)
                .format(OutputFormat.TIFF)
                .outputDirectory(tempDir)
                .build();

        assertEquals(MaskExportConfig.MaskType.GRAYSCALE_LABELS, config.getMaskType());
        assertEquals(3, config.getSelectedClassifications().size());
        assertEquals(0, config.getBackgroundLabel());
        assertEquals(-1, config.getBoundaryLabel());
        assertEquals(MaskExportConfig.ObjectSource.ANNOTATIONS, config.getObjectSource());
        assertEquals(4.0, config.getDownsample(), 0.001);
        assertEquals(OutputFormat.TIFF, config.getFormat());
        assertEquals(tempDir, config.getOutputDirectory());
    }

    @Test
    void testDefaults() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        assertEquals(MaskExportConfig.MaskType.BINARY, config.getMaskType());
        assertTrue(config.getSelectedClassifications().isEmpty());
        assertEquals(0, config.getBackgroundLabel());
        assertEquals(-1, config.getBoundaryLabel());
        assertFalse(config.isEnableBoundary());
        assertEquals(MaskExportConfig.ObjectSource.ANNOTATIONS, config.getObjectSource());
        assertEquals(4.0, config.getDownsample(), 0.001);
        assertEquals(OutputFormat.PNG, config.getFormat());
        assertFalse(config.isGrayscaleLut());
        assertTrue(config.isAddToWorkflow());
    }

    @Test
    void testNullOutputDirectoryThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new MaskExportConfig.Builder().build());
    }

    @Test
    void testDownsampleBelowOneThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new MaskExportConfig.Builder()
                        .outputDirectory(tempDir)
                        .downsample(0.5)
                        .build());
    }

    @Test
    void testBuildOutputFilename() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .format(OutputFormat.PNG)
                .outputDirectory(tempDir)
                .build();

        assertEquals("my_image.png", config.buildOutputFilename("my_image"));
    }

    @Test
    void testBuildOutputFilenameHandlesEmptyName() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String result = config.buildOutputFilename("");
        assertNotNull(result);
        assertTrue(result.endsWith(".png"));
        assertFalse(result.startsWith("."));
    }

    @Test
    void testSelectedClassificationsIsImmutable() {
        List<String> classes = List.of("A", "B");
        MaskExportConfig config = new MaskExportConfig.Builder()
                .selectedClassifications(classes)
                .outputDirectory(tempDir)
                .build();

        assertThrows(UnsupportedOperationException.class, () ->
                config.getSelectedClassifications().add("C"));
    }

    @Test
    void testBoundaryLabelWithToggle() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .enableBoundary(true)
                .boundaryLabel(255)
                .outputDirectory(tempDir)
                .build();

        assertTrue(config.isEnableBoundary());
        assertEquals(255, config.getBoundaryLabel());
    }

    @Test
    void testInstanceMaskType() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .maskType(MaskExportConfig.MaskType.INSTANCE)
                .objectSource(MaskExportConfig.ObjectSource.DETECTIONS)
                .outputDirectory(tempDir)
                .build();

        assertEquals(MaskExportConfig.MaskType.INSTANCE, config.getMaskType());
        assertEquals(MaskExportConfig.ObjectSource.DETECTIONS, config.getObjectSource());
    }

    @Test
    void testMultichannelMaskType() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .maskType(MaskExportConfig.MaskType.MULTICHANNEL)
                .selectedClassifications(List.of("Tumor", "Stroma"))
                .outputDirectory(tempDir)
                .build();

        assertEquals(MaskExportConfig.MaskType.MULTICHANNEL, config.getMaskType());
        assertEquals(2, config.getSelectedClassifications().size());
    }
}
