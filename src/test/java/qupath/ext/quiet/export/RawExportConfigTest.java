package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RawExportConfigTest {

    @TempDir
    File tempDir;

    @Test
    void testBuildValidConfig() {
        RawExportConfig config = new RawExportConfig.Builder()
                .regionType(RawExportConfig.RegionType.WHOLE_IMAGE)
                .downsample(8.0)
                .format(OutputFormat.TIFF)
                .outputDirectory(tempDir)
                .addToWorkflow(false)
                .build();

        assertEquals(RawExportConfig.RegionType.WHOLE_IMAGE, config.getRegionType());
        assertEquals(8.0, config.getDownsample(), 0.001);
        assertEquals(OutputFormat.TIFF, config.getFormat());
        assertEquals(tempDir, config.getOutputDirectory());
        assertFalse(config.isAddToWorkflow());
    }

    @Test
    void testDefaults() {
        RawExportConfig config = new RawExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        assertEquals(RawExportConfig.RegionType.WHOLE_IMAGE, config.getRegionType());
        assertEquals(4.0, config.getDownsample(), 0.001);
        assertEquals(OutputFormat.TIFF, config.getFormat());
        assertTrue(config.isAddToWorkflow());
    }

    @Test
    void testNullOutputDirectoryThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new RawExportConfig.Builder().build());
    }

    @Test
    void testDownsampleBelowOneThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new RawExportConfig.Builder()
                        .outputDirectory(tempDir)
                        .downsample(0.5)
                        .build());
    }

    @Test
    void testBuildOutputFilename() {
        RawExportConfig config = new RawExportConfig.Builder()
                .format(OutputFormat.TIFF)
                .outputDirectory(tempDir)
                .build();

        assertEquals("my_image.tif", config.buildOutputFilename("my_image"));
    }

    @Test
    void testBuildOutputFilenameWithSuffix() {
        RawExportConfig config = new RawExportConfig.Builder()
                .format(OutputFormat.TIFF)
                .outputDirectory(tempDir)
                .build();

        assertEquals("my_image_region_1.tif", config.buildOutputFilename("my_image", "_region_1"));
    }

    @Test
    void testBuildOutputFilenameHandlesEmptyName() {
        RawExportConfig config = new RawExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String result = config.buildOutputFilename("");
        assertNotNull(result);
        assertTrue(result.endsWith(".tif"));
        assertFalse(result.startsWith("."));
    }

    @Test
    void testAllRegionTypes() {
        for (RawExportConfig.RegionType type : RawExportConfig.RegionType.values()) {
            RawExportConfig config = new RawExportConfig.Builder()
                    .regionType(type)
                    .outputDirectory(tempDir)
                    .build();
            assertEquals(type, config.getRegionType());
        }
    }

    @Test
    void testSelectedAnnotationsRegionType() {
        RawExportConfig config = new RawExportConfig.Builder()
                .regionType(RawExportConfig.RegionType.SELECTED_ANNOTATIONS)
                .outputDirectory(tempDir)
                .build();

        assertEquals(RawExportConfig.RegionType.SELECTED_ANNOTATIONS, config.getRegionType());
    }
}
