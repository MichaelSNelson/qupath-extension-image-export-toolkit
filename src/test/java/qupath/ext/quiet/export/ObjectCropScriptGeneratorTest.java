package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link ObjectCropScriptGenerator}.
 */
class ObjectCropScriptGeneratorTest {

    @TempDir
    File tempDir;

    @Test
    void testScriptContainsImports() {
        var config = new ObjectCropConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.OBJECT_CROPS, config);

        assertTrue(script.contains("import javax.imageio.ImageIO"));
        assertTrue(script.contains("import qupath.lib.regions.RegionRequest"));
    }

    @Test
    void testScriptContainsConfigValues() {
        var config = new ObjectCropConfig.Builder()
                .objectType(ObjectCropConfig.ObjectType.CELLS)
                .cropSize(128)
                .padding(16)
                .downsample(2.0)
                .labelFormat(ObjectCropConfig.LabelFormat.FILENAME_PREFIX)
                .format(OutputFormat.TIFF)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.OBJECT_CROPS, config);

        assertTrue(script.contains("\"CELLS\""), "Script should contain object type");
        assertTrue(script.contains("def cropSize = 128"), "Script should contain crop size");
        assertTrue(script.contains("def padding = 16"), "Script should contain padding");
        assertTrue(script.contains("2.0"), "Script should contain downsample");
        assertTrue(script.contains("\"FILENAME_PREFIX\""), "Script should contain label format");
        assertTrue(script.contains("\".tif\"") || script.contains("\"tif\""),
                "Script should contain TIFF format");
    }

    @Test
    void testScriptContainsObjectSwitch() {
        var config = new ObjectCropConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.OBJECT_CROPS, config);

        assertTrue(script.contains("getDetectionObjects()"));
        assertTrue(script.contains("getCellObjects()"));
        assertTrue(script.contains("switch (objectType)"));
    }

    @Test
    void testScriptContainsSelectedClasses() {
        var config = new ObjectCropConfig.Builder()
                .outputDirectory(tempDir)
                .selectedClasses(List.of("Tumor", "Stroma"))
                .build();

        String script = ScriptGenerator.generate(ExportCategory.OBJECT_CROPS, config);

        assertTrue(script.contains("\"Tumor\""), "Script should contain Tumor class");
        assertTrue(script.contains("\"Stroma\""), "Script should contain Stroma class");
        assertTrue(script.contains("selectedClasses"), "Script should reference selectedClasses");
    }

    @Test
    void testScriptContainsEmptyClassesDefault() {
        var config = new ObjectCropConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.OBJECT_CROPS, config);

        assertTrue(script.contains("def selectedClasses = []"),
                "Script should contain empty classes list when none selected");
    }

    @Test
    void testScriptContainsLabelFormatLogic() {
        var config = new ObjectCropConfig.Builder()
                .outputDirectory(tempDir)
                .labelFormat(ObjectCropConfig.LabelFormat.SUBDIRECTORY)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.OBJECT_CROPS, config);

        assertTrue(script.contains("SUBDIRECTORY"),
                "Script should contain SUBDIRECTORY format reference");
        assertTrue(script.contains("labelFormat"),
                "Script should use labelFormat variable");
    }

    @Test
    void testScriptContainsRegionClamping() {
        var config = new ObjectCropConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.OBJECT_CROPS, config);

        assertTrue(script.contains("Math.max(0,"), "Script should clamp coordinates");
        assertTrue(script.contains("Math.min("), "Script should clamp to image bounds");
    }

    @Test
    void testScriptIsAsciiOnly() {
        var config = new ObjectCropConfig.Builder()
                .cropSize(256)
                .padding(32)
                .downsample(4.0)
                .selectedClasses(List.of("Tumor", "Stroma", "Necrosis"))
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.OBJECT_CROPS, config);
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            assertTrue(c < 128,
                    "Non-ASCII character found at index " + i + ": U+" +
                    String.format("%04X", (int) c) + " '" + c + "'");
        }
    }

    @Test
    void testScriptWithWindowsPath() {
        File windowsDir = new File("C:\\Users\\test\\crops");
        var config = new ObjectCropConfig.Builder()
                .outputDirectory(windowsDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.OBJECT_CROPS, config);

        assertFalse(script.contains("C:\\U"), "Unescaped backslash found");
        assertTrue(script.contains("C:\\\\"), "Backslashes should be escaped");
    }

    @Test
    void testScriptContainsExportSummary() {
        var config = new ObjectCropConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.OBJECT_CROPS, config);

        assertTrue(script.contains("Exported"),
                "Script should contain export summary");
    }
}
