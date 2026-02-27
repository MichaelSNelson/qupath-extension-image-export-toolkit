package qupath.ext.quiet.export;

import java.io.File;
import java.util.List;

import qupath.lib.common.GeneralTools;

/**
 * Immutable configuration for per-object crop export.
 * <p>
 * Each detected/cell object produces a small image patch centered on
 * the object centroid, organized by classification label. The output
 * structure is compatible with PyTorch ImageFolder when using
 * {@link LabelFormat#SUBDIRECTORY}.
 */
public class ObjectCropConfig {

    /**
     * Which object types to crop around.
     */
    public enum ObjectType {
        DETECTIONS,
        CELLS,
        ALL
    }

    /**
     * How classification labels are applied to output filenames.
     */
    public enum LabelFormat {
        /** Crops placed in subdirectories named by class: {@code ClassName/image_1.png} */
        SUBDIRECTORY,
        /** Class name prepended to filename: {@code ClassName_image_1.png} */
        FILENAME_PREFIX
    }

    private final ObjectType objectType;
    private final int cropSize;
    private final int padding;
    private final double downsample;
    private final LabelFormat labelFormat;
    private final OutputFormat format;
    private final File outputDirectory;
    private final List<String> selectedClasses;
    private final boolean addToWorkflow;

    private ObjectCropConfig(Builder builder) {
        this.objectType = builder.objectType;
        this.cropSize = builder.cropSize;
        this.padding = builder.padding;
        this.downsample = builder.downsample;
        this.labelFormat = builder.labelFormat;
        this.format = builder.format;
        this.outputDirectory = builder.outputDirectory;
        this.selectedClasses = builder.selectedClasses != null
                ? List.copyOf(builder.selectedClasses) : List.of();
        this.addToWorkflow = builder.addToWorkflow;
    }

    public ObjectType getObjectType() { return objectType; }
    public int getCropSize() { return cropSize; }
    public int getPadding() { return padding; }
    public double getDownsample() { return downsample; }
    public LabelFormat getLabelFormat() { return labelFormat; }
    public OutputFormat getFormat() { return format; }
    public File getOutputDirectory() { return outputDirectory; }
    public List<String> getSelectedClasses() { return selectedClasses; }
    public boolean isAddToWorkflow() { return addToWorkflow; }

    /**
     * Resolve the output file for a crop image.
     *
     * @param entryName the image entry name
     * @param className the classification name
     * @param index     one-based index within the class
     * @return the output file path
     */
    public File resolveOutputFile(String entryName, String className, int index) {
        String safeEntry = sanitize(entryName);
        String safeClass = sanitize(className);
        String ext = "." + format.getExtension();

        return switch (labelFormat) {
            case SUBDIRECTORY -> new File(
                    new File(outputDirectory, safeClass),
                    safeEntry + "_" + index + ext);
            case FILENAME_PREFIX -> new File(
                    outputDirectory,
                    safeClass + "_" + safeEntry + "_" + index + ext);
        };
    }

    /**
     * Build a display filename for metadata tracking.
     */
    public String buildOutputFilename(String entryName) {
        String sanitized = sanitize(entryName);
        return sanitized + "." + format.getExtension();
    }

    private static String sanitize(String name) {
        String safe = GeneralTools.stripInvalidFilenameChars(name);
        if (safe == null || safe.isBlank()) return "unnamed";
        return safe;
    }

    /**
     * Builder for creating {@link ObjectCropConfig} instances.
     */
    public static class Builder {

        private ObjectType objectType = ObjectType.DETECTIONS;
        private int cropSize = 64;
        private int padding = 0;
        private double downsample = 1.0;
        private LabelFormat labelFormat = LabelFormat.SUBDIRECTORY;
        private OutputFormat format = OutputFormat.PNG;
        private File outputDirectory;
        private List<String> selectedClasses;
        private boolean addToWorkflow = true;

        public Builder objectType(ObjectType type) { this.objectType = type; return this; }
        public Builder cropSize(int size) { this.cropSize = size; return this; }
        public Builder padding(int padding) { this.padding = padding; return this; }
        public Builder downsample(double ds) { this.downsample = ds; return this; }
        public Builder labelFormat(LabelFormat fmt) { this.labelFormat = fmt; return this; }
        public Builder format(OutputFormat fmt) { this.format = fmt; return this; }
        public Builder outputDirectory(File dir) { this.outputDirectory = dir; return this; }
        public Builder selectedClasses(List<String> classes) { this.selectedClasses = classes; return this; }
        public Builder addToWorkflow(boolean add) { this.addToWorkflow = add; return this; }

        /**
         * Build the object crop configuration, validating required fields.
         *
         * @return a new ObjectCropConfig
         * @throws IllegalArgumentException if required fields are missing or invalid
         */
        public ObjectCropConfig build() {
            if (outputDirectory == null) {
                throw new IllegalArgumentException("Output directory is required");
            }
            if (cropSize < 1) {
                throw new IllegalArgumentException("Crop size must be >= 1");
            }
            if (padding < 0) {
                throw new IllegalArgumentException("Padding must be >= 0");
            }
            if (downsample < 1.0) {
                throw new IllegalArgumentException("Downsample must be >= 1.0");
            }
            return new ObjectCropConfig(this);
        }
    }
}
