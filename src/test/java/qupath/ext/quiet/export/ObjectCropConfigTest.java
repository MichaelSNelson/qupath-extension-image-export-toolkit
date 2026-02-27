package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link ObjectCropConfig} builder and filename resolution.
 */
class ObjectCropConfigTest {

    @TempDir
    File tempDir;

    @Test
    void testBuilderDefaults() {
        var config = new ObjectCropConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        assertEquals(ObjectCropConfig.ObjectType.DETECTIONS, config.getObjectType());
        assertEquals(64, config.getCropSize());
        assertEquals(0, config.getPadding());
        assertEquals(1.0, config.getDownsample());
        assertEquals(ObjectCropConfig.LabelFormat.SUBDIRECTORY, config.getLabelFormat());
        assertEquals(OutputFormat.PNG, config.getFormat());
        assertTrue(config.getSelectedClasses().isEmpty());
        assertTrue(config.isAddToWorkflow());
    }

    @Test
    void testSubdirectoryFilename() {
        var config = new ObjectCropConfig.Builder()
                .outputDirectory(tempDir)
                .labelFormat(ObjectCropConfig.LabelFormat.SUBDIRECTORY)
                .format(OutputFormat.PNG)
                .build();

        File result = config.resolveOutputFile("image1", "Tumor", 1);
        assertEquals(new File(new File(tempDir, "Tumor"), "image1_1.png"), result);
    }

    @Test
    void testFilenamePrefixFormat() {
        var config = new ObjectCropConfig.Builder()
                .outputDirectory(tempDir)
                .labelFormat(ObjectCropConfig.LabelFormat.FILENAME_PREFIX)
                .format(OutputFormat.PNG)
                .build();

        File result = config.resolveOutputFile("image1", "Tumor", 1);
        assertEquals(new File(tempDir, "Tumor_image1_1.png"), result);
    }

    @Test
    void testTiffExtension() {
        var config = new ObjectCropConfig.Builder()
                .outputDirectory(tempDir)
                .format(OutputFormat.TIFF)
                .build();

        File result = config.resolveOutputFile("img", "Stroma", 3);
        assertTrue(result.getName().endsWith(".tif"));
    }

    @Test
    void testSelectedClassesImmutable() {
        var classes = new java.util.ArrayList<>(List.of("Tumor", "Stroma"));
        var config = new ObjectCropConfig.Builder()
                .outputDirectory(tempDir)
                .selectedClasses(classes)
                .build();

        classes.add("Background");
        assertEquals(2, config.getSelectedClasses().size(),
                "Modifying the original list should not affect the config");
    }

    @Test
    void testBuilderValidationRequiresOutputDir() {
        assertThrows(IllegalArgumentException.class, () ->
                new ObjectCropConfig.Builder().build());
    }

    @Test
    void testBuilderValidationCropSizePositive() {
        assertThrows(IllegalArgumentException.class, () ->
                new ObjectCropConfig.Builder()
                        .outputDirectory(tempDir)
                        .cropSize(0)
                        .build());
    }

    @Test
    void testBuilderValidationPaddingNonNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                new ObjectCropConfig.Builder()
                        .outputDirectory(tempDir)
                        .padding(-1)
                        .build());
    }

    @Test
    void testBuilderValidationDownsampleMinimum() {
        assertThrows(IllegalArgumentException.class, () ->
                new ObjectCropConfig.Builder()
                        .outputDirectory(tempDir)
                        .downsample(0.5)
                        .build());
    }

    @Test
    void testBuildOutputFilename() {
        var config = new ObjectCropConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String filename = config.buildOutputFilename("test_image");
        assertEquals("test_image.png", filename);
    }
}
