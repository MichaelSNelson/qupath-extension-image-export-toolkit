package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import qupath.ext.quiet.export.ExportCategory;
import qupath.ext.quiet.export.OutputFormat;
import qupath.ext.quiet.export.RenderedExportConfig;

class RenderedScriptGeneratorTest {

    @TempDir
    File tempDir;

    private RenderedExportConfig classifierConfig;

    @BeforeEach
    void setUp() {
        classifierConfig = new RenderedExportConfig.Builder()
                .classifierName("tissue_classifier")
                .overlayOpacity(0.6)
                .downsample(4.0)
                .format(OutputFormat.PNG)
                .outputDirectory(tempDir)
                .includeAnnotations(true)
                .includeDetections(true)
                .fillAnnotations(false)
                .showNames(false)
                .addToWorkflow(true)
                .build();
    }

    @Test
    void testClassifierScriptContainsImports() {
        String script = ScriptGenerator.generate(ExportCategory.RENDERED, classifierConfig);

        assertTrue(script.contains("import qupath.lib.classifiers.pixel.PixelClassificationImageServer"));
        assertTrue(script.contains("import qupath.lib.images.writers.ImageWriterTools"));
        assertTrue(script.contains("import qupath.lib.gui.viewer.overlays.HierarchyOverlay"));
        assertTrue(script.contains("import java.awt.AlphaComposite"));
        assertTrue(script.contains("import java.awt.RenderingHints"));
        assertTrue(script.contains("import qupath.lib.regions.RegionRequest"));
    }

    @Test
    void testClassifierScriptContainsConfigValues() {
        String script = ScriptGenerator.generate(ExportCategory.RENDERED, classifierConfig);

        assertTrue(script.contains("\"tissue_classifier\""));
        assertTrue(script.contains("0.6"));
        assertTrue(script.contains("4.0"));
        assertTrue(script.contains("\"png\""));
        assertTrue(script.contains("includeAnnotations = true"));
        assertTrue(script.contains("includeDetections = true"));
        assertTrue(script.contains("fillAnnotations = false"));
        assertTrue(script.contains("showNames = false"));
    }

    @Test
    void testClassifierScriptContainsDirectCompositing() {
        String script = ScriptGenerator.generate(ExportCategory.RENDERED, classifierConfig);

        assertTrue(script.contains("readRegion"));
        assertTrue(script.contains("AlphaComposite.SRC_OVER"));
        assertTrue(script.contains("createGraphics"));
        assertTrue(script.contains("TYPE_INT_RGB"));
    }

    @Test
    void testClassifierScriptContainsMainLoop() {
        String script = ScriptGenerator.generate(ExportCategory.RENDERED, classifierConfig);

        assertTrue(script.contains("for (int i = 0; i < entries.size(); i++)"));
        assertTrue(script.contains("entry.readImageData()"));
        assertTrue(script.contains("ImageWriterTools.writeImage"));
    }

    @Test
    void testClassifierScriptContainsErrorHandling() {
        String script = ScriptGenerator.generate(ExportCategory.RENDERED, classifierConfig);

        assertTrue(script.contains("try {"));
        assertTrue(script.contains("catch (Exception e)"));
        assertTrue(script.contains("FAIL:"));
        assertTrue(script.contains("SKIP:"));
    }

    @Test
    void testClassifierScriptContainsCleanup() {
        String script = ScriptGenerator.generate(ExportCategory.RENDERED, classifierConfig);

        assertTrue(script.contains("classServer.close()"));
        assertTrue(script.contains("baseServer.close()"));
    }

    @Test
    void testClassifierScriptIsAsciiOnly() {
        String script = ScriptGenerator.generate(ExportCategory.RENDERED, classifierConfig);
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            assertTrue(c < 128,
                    "Non-ASCII character found at index " + i + ": U+" +
                    String.format("%04X", (int) c) + " '" + c + "'");
        }
    }

    @Test
    void testClassifierScriptWithWindowsPath() {
        File windowsDir = new File("C:\\Users\\test\\output");
        RenderedExportConfig config = new RenderedExportConfig.Builder()
                .classifierName("test")
                .outputDirectory(windowsDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.RENDERED, config);

        assertFalse(script.contains("C:\\U"), "Unescaped backslash found");
        assertTrue(script.contains("C:\\\\"), "Backslashes should be escaped");
    }

    @Test
    void testClassifierScriptContainsSummary() {
        String script = ScriptGenerator.generate(ExportCategory.RENDERED, classifierConfig);
        assertTrue(script.contains("Export complete:"));
        assertTrue(script.contains("succeeded"));
        assertTrue(script.contains("failed"));
    }

    // --- Object overlay mode tests ---

    @Test
    void testObjectOverlayScriptExcludesClassifier() {
        RenderedExportConfig objConfig = new RenderedExportConfig.Builder()
                .renderMode(RenderedExportConfig.RenderMode.OBJECT_OVERLAY)
                .includeAnnotations(true)
                .includeDetections(true)
                .overlayOpacity(0.7)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.RENDERED, objConfig);

        assertFalse(script.contains("PixelClassificationImageServer"),
                "Object overlay script should not reference PixelClassificationImageServer");
        assertFalse(script.contains("classifierName"),
                "Object overlay script should not reference classifierName");
        assertFalse(script.contains("classServer"),
                "Object overlay script should not reference classServer");
    }

    @Test
    void testObjectOverlayScriptContainsOverlayOptions() {
        RenderedExportConfig objConfig = new RenderedExportConfig.Builder()
                .renderMode(RenderedExportConfig.RenderMode.OBJECT_OVERLAY)
                .includeAnnotations(true)
                .includeDetections(true)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.RENDERED, objConfig);

        assertTrue(script.contains("OverlayOptions"));
        assertTrue(script.contains("HierarchyOverlay"));
        assertTrue(script.contains("setShowAnnotations"));
        assertTrue(script.contains("setShowDetections"));
    }

    @Test
    void testObjectOverlayScriptIsAsciiOnly() {
        RenderedExportConfig objConfig = new RenderedExportConfig.Builder()
                .renderMode(RenderedExportConfig.RenderMode.OBJECT_OVERLAY)
                .includeAnnotations(true)
                .outputDirectory(tempDir)
                .build();

        String script = ScriptGenerator.generate(ExportCategory.RENDERED, objConfig);
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            assertTrue(c < 128,
                    "Non-ASCII character found at index " + i + ": U+" +
                    String.format("%04X", (int) c) + " '" + c + "'");
        }
    }

    @Test
    void testQuoteMethod() {
        assertEquals("null", ScriptGenerator.quote(null));
        assertEquals("\"hello\"", ScriptGenerator.quote("hello"));
        assertEquals("\"say \\\"hi\\\"\"", ScriptGenerator.quote("say \"hi\""));
        assertEquals("\"C:\\\\path\\\\to\"", ScriptGenerator.quote("C:\\path\\to"));
    }
}
