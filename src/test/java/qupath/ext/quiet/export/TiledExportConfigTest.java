package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TiledExportConfigTest {

    @TempDir
    File tempDir;

    @Test
    void testBuildValidConfig() {
        TiledExportConfig config = new TiledExportConfig.Builder()
                .tileSize(256)
                .overlap(32)
                .downsample(2.0)
                .imageFormat(OutputFormat.TIFF)
                .labelFormat(OutputFormat.PNG)
                .annotatedTilesOnly(true)
                .exportGeoJson(false)
                .outputDirectory(tempDir)
                .addToWorkflow(false)
                .build();

        assertEquals(256, config.getTileSize());
        assertEquals(32, config.getOverlap());
        assertEquals(2.0, config.getDownsample(), 0.001);
        assertEquals(OutputFormat.TIFF, config.getImageFormat());
        assertEquals(OutputFormat.PNG, config.getLabelFormat());
        assertTrue(config.isAnnotatedTilesOnly());
        assertFalse(config.isExportGeoJson());
        assertEquals(tempDir, config.getOutputDirectory());
        assertFalse(config.isAddToWorkflow());
    }

    @Test
    void testDefaults() {
        TiledExportConfig config = new TiledExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        assertEquals(512, config.getTileSize());
        assertEquals(0, config.getOverlap());
        assertEquals(1.0, config.getDownsample(), 0.001);
        assertEquals(OutputFormat.TIFF, config.getImageFormat());
        assertNull(config.getLabelFormat());
        assertNull(config.getLabeledServerConfig());
        assertEquals(TiledExportConfig.ParentObjectFilter.ANNOTATIONS, config.getParentObjectFilter());
        assertTrue(config.isAnnotatedTilesOnly());
        assertFalse(config.isExportGeoJson());
        assertTrue(config.isAddToWorkflow());
    }

    @Test
    void testNullOutputDirectoryThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new TiledExportConfig.Builder().build());
    }

    @Test
    void testTileSizeBelowOneThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new TiledExportConfig.Builder()
                        .outputDirectory(tempDir)
                        .tileSize(0)
                        .build());
    }

    @Test
    void testNegativeOverlapThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new TiledExportConfig.Builder()
                        .outputDirectory(tempDir)
                        .overlap(-1)
                        .build());
    }

    @Test
    void testDownsampleBelowOneThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new TiledExportConfig.Builder()
                        .outputDirectory(tempDir)
                        .downsample(0.5)
                        .build());
    }

    @Test
    void testBuildOutputFilename() {
        TiledExportConfig config = new TiledExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        assertEquals("my_image", config.buildOutputFilename("my_image"));
    }

    @Test
    void testBuildOutputFilenameHandlesEmptyName() {
        TiledExportConfig config = new TiledExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String result = config.buildOutputFilename("");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testWithLabeledServerConfig() {
        MaskExportConfig labelConfig = new MaskExportConfig.Builder()
                .maskType(MaskExportConfig.MaskType.BINARY)
                .outputDirectory(tempDir)
                .build();

        TiledExportConfig config = new TiledExportConfig.Builder()
                .labelFormat(OutputFormat.PNG)
                .labeledServerConfig(labelConfig)
                .outputDirectory(tempDir)
                .build();

        assertNotNull(config.getLabelFormat());
        assertNotNull(config.getLabeledServerConfig());
        assertEquals(MaskExportConfig.MaskType.BINARY, config.getLabeledServerConfig().getMaskType());
    }

    @Test
    void testAllParentObjectFilters() {
        for (TiledExportConfig.ParentObjectFilter filter : TiledExportConfig.ParentObjectFilter.values()) {
            TiledExportConfig config = new TiledExportConfig.Builder()
                    .parentObjectFilter(filter)
                    .outputDirectory(tempDir)
                    .build();
            assertEquals(filter, config.getParentObjectFilter());
        }
    }

    @Test
    void testExportGeoJsonPerTile() {
        TiledExportConfig config = new TiledExportConfig.Builder()
                .exportGeoJson(true)
                .outputDirectory(tempDir)
                .build();

        assertTrue(config.isExportGeoJson());
    }
}
