package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import qupath.ext.quiet.export.ExportCategory;
import qupath.ext.quiet.export.OutputFormat;
import qupath.ext.quiet.export.RawExportConfig;

class RawScriptGeneratorTest {

    @TempDir
    File tempDir;

    @Test
    void testWholeImageScriptContainsImports() {
        RawExportConfig config = new RawExportConfig.Builder()
                .regionType(RawExportConfig.RegionType.WHOLE_IMAGE)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.RAW, config);

        assertTrue(script.contains("import qupath.lib.images.writers.ImageWriterTools"));
        assertTrue(script.contains("import qupath.lib.regions.RegionRequest"));
    }

    @Test
    void testWholeImageScriptContainsConfigValues() {
        RawExportConfig config = new RawExportConfig.Builder()
                .regionType(RawExportConfig.RegionType.WHOLE_IMAGE)
                .downsample(8.0)
                .format(OutputFormat.TIFF)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.RAW, config);

        assertTrue(script.contains("\"WHOLE_IMAGE\""));
        assertTrue(script.contains("8.0"));
        assertTrue(script.contains("\"tif\""));
    }

    @Test
    void testWholeImageScriptContainsRegionRequest() {
        RawExportConfig config = new RawExportConfig.Builder()
                .regionType(RawExportConfig.RegionType.WHOLE_IMAGE)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.RAW, config);

        assertTrue(script.contains("RegionRequest.createInstance"));
        assertTrue(script.contains("exportServer.getWidth()"));
        assertTrue(script.contains("exportServer.getHeight()"));
    }

    @Test
    void testAnnotationScriptContainsAnnotationLogic() {
        RawExportConfig config = new RawExportConfig.Builder()
                .regionType(RawExportConfig.RegionType.ALL_ANNOTATIONS)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.RAW, config);

        assertTrue(script.contains("getAnnotationObjects"));
        assertTrue(script.contains("getROI()"));
        assertTrue(script.contains("getBoundsX"));
        assertTrue(script.contains("region_"));
    }

    @Test
    void testSelectedAnnotationsScript() {
        RawExportConfig config = new RawExportConfig.Builder()
                .regionType(RawExportConfig.RegionType.SELECTED_ANNOTATIONS)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.RAW, config);

        assertTrue(script.contains("getSelectionModel"));
        assertTrue(script.contains("isAnnotation"));
    }

    @Test
    void testScriptIsAsciiOnly() {
        RawExportConfig config = new RawExportConfig.Builder()
                .regionType(RawExportConfig.RegionType.WHOLE_IMAGE)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.RAW, config);
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            assertTrue(c < 128,
                    "Non-ASCII character found at index " + i + ": U+" +
                    String.format("%04X", (int) c) + " '" + c + "'");
        }
    }

    @Test
    void testScriptWithWindowsPath() {
        File windowsDir = new File("C:\\Users\\test\\raw");
        RawExportConfig config = new RawExportConfig.Builder()
                .outputDirectory(windowsDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.RAW, config);

        assertFalse(script.contains("C:\\U"), "Unescaped backslash found");
        assertTrue(script.contains("C:\\\\"), "Backslashes should be escaped");
    }

    @Test
    void testScriptContainsErrorHandling() {
        RawExportConfig config = new RawExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.RAW, config);

        assertTrue(script.contains("try {"));
        assertTrue(script.contains("catch (Exception e)"));
        assertTrue(script.contains("FAIL:"));
    }

    @Test
    void testScriptContainsSummary() {
        RawExportConfig config = new RawExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.RAW, config);
        assertTrue(script.contains("Export complete:"));
        assertTrue(script.contains("succeeded"));
        assertTrue(script.contains("failed"));
    }
}
