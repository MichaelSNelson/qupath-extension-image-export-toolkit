package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import qupath.ext.quiet.export.ExportCategory;
import qupath.ext.quiet.export.MaskExportConfig;
import qupath.ext.quiet.export.OutputFormat;
import qupath.ext.quiet.export.TiledExportConfig;

class TiledScriptGeneratorTest {

    @TempDir
    File tempDir;

    @Test
    void testScriptContainsImports() {
        TiledExportConfig config = new TiledExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.TILED, config);

        assertTrue(script.contains("import qupath.lib.images.writers.TileExporter"));
    }

    @Test
    void testScriptContainsConfigValues() {
        TiledExportConfig config = new TiledExportConfig.Builder()
                .tileSize(256)
                .overlap(64)
                .downsample(2.0)
                .imageFormat(OutputFormat.TIFF)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.TILED, config);

        assertTrue(script.contains("def tileSize = 256"));
        assertTrue(script.contains("def overlap = 64"));
        assertTrue(script.contains("2.0"));
        assertTrue(script.contains("\".tif\""));
    }

    @Test
    void testScriptContainsTileExporter() {
        TiledExportConfig config = new TiledExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.TILED, config);

        assertTrue(script.contains("new TileExporter(imageData)"));
        assertTrue(script.contains(".tileSize(tileSize)"));
        assertTrue(script.contains(".overlap(overlap)"));
        assertTrue(script.contains(".downsample(downsample)"));
        assertTrue(script.contains(".imageExtension(imageFormat)"));
        assertTrue(script.contains("writeTiles"));
    }

    @Test
    void testScriptWithLabelServerContainsLabelImports() {
        MaskExportConfig labelConfig = new MaskExportConfig.Builder()
                .maskType(MaskExportConfig.MaskType.BINARY)
                .outputDirectory(tempDir)
                .build();

        TiledExportConfig config = new TiledExportConfig.Builder()
                .labelFormat(OutputFormat.PNG)
                .labeledServerConfig(labelConfig)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.TILED, config);

        assertTrue(script.contains("import qupath.lib.images.servers.LabeledImageServer"));
        assertTrue(script.contains("import qupath.lib.objects.classes.PathClass"));
        assertTrue(script.contains("labeledServer(labelServer)"));
        assertTrue(script.contains("labeledImageExtension(labelFormat)"));
    }

    @Test
    void testScriptContainsParentFilter() {
        TiledExportConfig config = new TiledExportConfig.Builder()
                .parentObjectFilter(TiledExportConfig.ParentObjectFilter.ANNOTATIONS)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.TILED, config);

        assertTrue(script.contains("parentFilter"));
        assertTrue(script.contains("'ANNOTATIONS'"));
        assertTrue(script.contains("getAnnotationObjects"));
    }

    @Test
    void testScriptContainsErrorHandling() {
        TiledExportConfig config = new TiledExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.TILED, config);

        assertTrue(script.contains("try {"));
        assertTrue(script.contains("catch (Exception e)"));
        assertTrue(script.contains("FAIL:"));
    }

    @Test
    void testScriptIsAsciiOnly() {
        MaskExportConfig labelConfig = new MaskExportConfig.Builder()
                .maskType(MaskExportConfig.MaskType.GRAYSCALE_LABELS)
                .selectedClassifications(List.of("Tumor", "Stroma"))
                .outputDirectory(tempDir)
                .build();

        TiledExportConfig config = new TiledExportConfig.Builder()
                .tileSize(512)
                .overlap(64)
                .labelFormat(OutputFormat.PNG)
                .labeledServerConfig(labelConfig)
                .exportGeoJson(true)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.TILED, config);
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            assertTrue(c < 128,
                    "Non-ASCII character found at index " + i + ": U+" +
                    String.format("%04X", (int) c) + " '" + c + "'");
        }
    }

    @Test
    void testScriptWithWindowsPath() {
        File windowsDir = new File("C:\\Users\\test\\tiles");
        TiledExportConfig config = new TiledExportConfig.Builder()
                .outputDirectory(windowsDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.TILED, config);

        assertFalse(script.contains("C:\\U"), "Unescaped backslash found");
        assertTrue(script.contains("C:\\\\"), "Backslashes should be escaped");
    }

    @Test
    void testScriptContainsSummary() {
        TiledExportConfig config = new TiledExportConfig.Builder()
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.TILED, config);
        assertTrue(script.contains("Export complete:"));
        assertTrue(script.contains("succeeded"));
        assertTrue(script.contains("failed"));
    }

    @Test
    void testScriptWithGeoJsonExport() {
        TiledExportConfig config = new TiledExportConfig.Builder()
                .exportGeoJson(true)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.TILED, config);
        assertTrue(script.contains("exportJson"));
    }
}
