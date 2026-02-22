package qupath.ext.quiet.export;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExportCategoryTest {

    @TempDir
    File tempDir;

    @Test
    void testGetDefaultOutputDir() {
        File dir = ExportCategory.RENDERED.getDefaultOutputDir(tempDir);
        assertEquals(new File(new File(tempDir, "exports"), "rendered"), dir);
    }

    @Test
    void testGetNextAvailableReturnsDefaultWhenEmpty() {
        File dir = ExportCategory.RENDERED.getNextAvailableOutputDir(tempDir);
        assertEquals(new File(new File(tempDir, "exports"), "rendered"), dir);
    }

    @Test
    void testGetNextAvailableReturnsDefaultWhenDirDoesNotExist() {
        // Default dir does not exist yet -- should return it
        File dir = ExportCategory.MASK.getNextAvailableOutputDir(tempDir);
        assertEquals(new File(new File(tempDir, "exports"), "masks"), dir);
    }

    @Test
    void testGetNextAvailableSkipsPopulatedDir() throws IOException {
        // Create the default dir with a file in it
        File exportsDir = new File(tempDir, "exports");
        File renderedDir = new File(exportsDir, "rendered");
        assertTrue(renderedDir.mkdirs());
        Files.writeString(renderedDir.toPath().resolve("image.png"), "test");

        File dir = ExportCategory.RENDERED.getNextAvailableOutputDir(tempDir);
        assertEquals(new File(exportsDir, "rendered_2"), dir);
    }

    @Test
    void testGetNextAvailableSkipsMultiplePopulatedDirs() throws IOException {
        File exportsDir = new File(tempDir, "exports");

        // Populate rendered/, rendered_2/, rendered_3/
        for (String name : new String[]{"rendered", "rendered_2", "rendered_3"}) {
            File dir = new File(exportsDir, name);
            assertTrue(dir.mkdirs());
            Files.writeString(dir.toPath().resolve("image.png"), "test");
        }

        File dir = ExportCategory.RENDERED.getNextAvailableOutputDir(tempDir);
        assertEquals(new File(exportsDir, "rendered_4"), dir);
    }

    @Test
    void testGetNextAvailableReusesEmptyGap() throws IOException {
        File exportsDir = new File(tempDir, "exports");

        // Populate rendered/ but leave rendered_2/ empty (just the dir, no files)
        File renderedDir = new File(exportsDir, "rendered");
        assertTrue(renderedDir.mkdirs());
        Files.writeString(renderedDir.toPath().resolve("image.png"), "test");

        // rendered_2 exists but is empty -- should be reused
        File rendered2 = new File(exportsDir, "rendered_2");
        assertTrue(rendered2.mkdirs());

        File dir = ExportCategory.RENDERED.getNextAvailableOutputDir(tempDir);
        assertEquals(rendered2, dir);
    }

    @Test
    void testAllCategoriesHaveDistinctSubdirectories() {
        for (ExportCategory a : ExportCategory.values()) {
            for (ExportCategory b : ExportCategory.values()) {
                if (a != b) {
                    assertNotEquals(a.getDefaultSubdirectory(), b.getDefaultSubdirectory(),
                            a + " and " + b + " share the same subdirectory");
                }
            }
        }
    }

    @Test
    void testToStringReturnsDisplayName() {
        assertEquals("Rendered Image", ExportCategory.RENDERED.toString());
        assertEquals("Label / Mask", ExportCategory.MASK.toString());
        assertEquals("Raw Image Data", ExportCategory.RAW.toString());
        assertEquals("Tiled Export (ML)", ExportCategory.TILED.toString());
    }
}
