package qupath.ext.quiet.export;

import java.io.File;

/**
 * The three export categories supported by QuIET.
 */
public enum ExportCategory {

    RENDERED("Rendered Image", "rendered"),
    MASK("Label / Mask", "masks"),
    RAW("Raw Image Data", "raw"),
    TILED("Tiled Export (ML)", "tiles");

    private final String displayName;
    private final String defaultSubdirectory;

    ExportCategory(String displayName, String defaultSubdirectory) {
        this.displayName = displayName;
        this.defaultSubdirectory = defaultSubdirectory;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultSubdirectory() {
        return defaultSubdirectory;
    }

    /**
     * Returns the default output directory for this category within a project.
     *
     * @param projectDir the project root directory
     * @return a File pointing to {@code <projectDir>/exports/<subdirectory>/}
     */
    public File getDefaultOutputDir(File projectDir) {
        return new File(new File(projectDir, "exports"), defaultSubdirectory);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
