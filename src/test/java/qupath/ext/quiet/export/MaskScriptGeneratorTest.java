package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import qupath.ext.quiet.export.ExportCategory;
import qupath.ext.quiet.export.MaskExportConfig;
import qupath.ext.quiet.export.OutputFormat;

class MaskScriptGeneratorTest {

    @TempDir
    File tempDir;

    @Test
    void testScriptContainsImports() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .maskType(MaskExportConfig.MaskType.GRAYSCALE_LABELS)
                .selectedClassifications(List.of("Tumor", "Stroma"))
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.MASK, config);

        assertTrue(script.contains("import qupath.lib.images.servers.LabeledImageServer"));
        assertTrue(script.contains("import qupath.lib.images.writers.ImageWriterTools"));
        assertTrue(script.contains("import qupath.lib.objects.classes.PathClass"));
        assertTrue(script.contains("import qupath.lib.regions.RegionRequest"));
    }

    @Test
    void testScriptContainsConfigValues() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .maskType(MaskExportConfig.MaskType.GRAYSCALE_LABELS)
                .selectedClassifications(List.of("Tumor", "Stroma"))
                .backgroundLabel(0)
                .downsample(8.0)
                .format(OutputFormat.TIFF)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.MASK, config);

        assertTrue(script.contains("\"GRAYSCALE_LABELS\""));
        assertTrue(script.contains("8.0"));
        assertTrue(script.contains("\"tif\""));
        assertTrue(script.contains("\"Tumor\""));
        assertTrue(script.contains("\"Stroma\""));
        assertTrue(script.contains("backgroundLabel = 0"));
    }

    @Test
    void testScriptContainsMaskTypeSwitch() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.MASK, config);

        assertTrue(script.contains("switch (maskType)"));
        assertTrue(script.contains("'BINARY'"));
        assertTrue(script.contains("'GRAYSCALE_LABELS'"));
        assertTrue(script.contains("'COLORED'"));
        assertTrue(script.contains("'INSTANCE'"));
        assertTrue(script.contains("'MULTICHANNEL'"));
    }

    @Test
    void testScriptContainsLabeledImageServerBuilder() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.MASK, config);

        assertTrue(script.contains("new LabeledImageServer.Builder(imageData)"));
        assertTrue(script.contains("builder.backgroundLabel"));
        assertTrue(script.contains("builder.downsample"));
        assertTrue(script.contains("builder.build()"));
    }

    @Test
    void testScriptContainsErrorHandling() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.MASK, config);

        assertTrue(script.contains("try {"));
        assertTrue(script.contains("catch (Exception e)"));
        assertTrue(script.contains("FAIL:"));
    }

    @Test
    void testScriptIsAsciiOnly() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .maskType(MaskExportConfig.MaskType.MULTICHANNEL)
                .selectedClassifications(List.of("A", "B", "C"))
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.MASK, config);
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            assertTrue(c < 128,
                    "Non-ASCII character found at index " + i + ": U+" +
                    String.format("%04X", (int) c) + " '" + c + "'");
        }
    }

    @Test
    void testScriptWithWindowsPath() {
        File windowsDir = new File("C:\\Users\\test\\masks");
        MaskExportConfig config = new MaskExportConfig.Builder()
                .outputDirectory(windowsDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.MASK, config);

        assertFalse(script.contains("C:\\U"), "Unescaped backslash found");
        assertTrue(script.contains("C:\\\\"), "Backslashes should be escaped");
    }

    @Test
    void testScriptContainsSummary() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.MASK, config);
        assertTrue(script.contains("Export complete:"));
        assertTrue(script.contains("succeeded"));
        assertTrue(script.contains("failed"));
    }

    @Test
    void testScriptWithBoundaryLabel() {
        MaskExportConfig config = new MaskExportConfig.Builder()
                .enableBoundary(true)
                .boundaryLabel(255)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.MASK, config);
        assertTrue(script.contains("boundaryLabel = 255"));
        assertTrue(script.contains("setBoundaryLabel('Boundary', boundaryLabel)"));
    }
}
