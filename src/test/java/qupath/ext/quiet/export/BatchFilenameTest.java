package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests that filename prefix/suffix are correctly applied
 * when constructing output filenames via BatchExportTask's entryName logic.
 * <p>
 * The prefix/suffix are prepended/appended to the raw image name before
 * it reaches buildOutputFilename(), so we test the combined result.
 */
class BatchFilenameTest {

    @TempDir
    File tempDir;

    @Test
    void testPrefixApplied() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .format(OutputFormat.PNG)
                .outputDirectory(tempDir)
                .build();

        // Simulate what BatchExportTask.call() does:
        // entryName = filenamePrefix + rawName + filenameSuffix
        String entryName = "pre_" + "image" + "";
        assertEquals("pre_image.png", config.buildOutputFilename(entryName));
    }

    @Test
    void testSuffixApplied() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .format(OutputFormat.PNG)
                .outputDirectory(tempDir)
                .build();

        String entryName = "" + "image" + "_suf";
        assertEquals("image_suf.png", config.buildOutputFilename(entryName));
    }

    @Test
    void testPrefixAndSuffix() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .format(OutputFormat.PNG)
                .outputDirectory(tempDir)
                .build();

        String entryName = "pre_" + "image" + "_suf";
        assertEquals("pre_image_suf.png", config.buildOutputFilename(entryName));
    }

    @Test
    void testEmptyPrefixSuffix() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .format(OutputFormat.PNG)
                .outputDirectory(tempDir)
                .build();

        String entryName = "" + "image" + "";
        assertEquals("image.png", config.buildOutputFilename(entryName));
    }

    @Test
    void testPrefixSuffixWithRenderedConfig() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .format(OutputFormat.TIFF)
                .outputDirectory(tempDir)
                .build();

        String entryName = "run1_" + "slide_001" + "_v2";
        assertEquals("run1_slide_001_v2.tif", config.buildOutputFilename(entryName));
    }

    @Test
    void testPrefixSuffixWithPerAnnotationSuffix() {
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .format(OutputFormat.PNG)
                .outputDirectory(tempDir)
                .build();

        // BatchExportTask applies prefix/suffix to entryName,
        // then exportPerAnnotation appends annotation suffix
        String entryName = "batch1_" + "image" + "_masks";
        String annotSuffix = "_Tumor_0";
        assertEquals("batch1_image_masks_Tumor_0.png",
                config.buildOutputFilename(entryName, annotSuffix));
    }

    @Test
    void testPrefixSuffixWithRawConfig() {
        RawExportConfig config = new RawExportConfig.Builder()
                .format(OutputFormat.TIFF)
                .outputDirectory(tempDir)
                .build();

        String entryName = "exp_" + "sample" + "_raw";
        assertEquals("exp_sample_raw.tif", config.buildOutputFilename(entryName));
    }
}
