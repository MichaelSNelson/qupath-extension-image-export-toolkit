package qupath.ext.quiet.export;

import java.io.File;
import java.util.List;

import qupath.lib.common.GeneralTools;

/**
 * Immutable configuration for a raw pixel data export operation.
 */
public class RawExportConfig {

    /**
     * Which region(s) of the image to export.
     */
    public enum RegionType {
        /** Export the entire image at the given downsample. */
        WHOLE_IMAGE,
        /** Export only the currently selected annotations. */
        SELECTED_ANNOTATIONS,
        /** Export all annotations in the hierarchy. */
        ALL_ANNOTATIONS
    }

    private final RegionType regionType;
    private final double downsample;
    private final OutputFormat format;
    private final File outputDirectory;
    private final boolean addToWorkflow;
    private final int paddingPixels;
    private final List<Integer> selectedChannels;
    private final int pyramidLevels;
    private final String compressionType;
    private final int tileSize;

    private RawExportConfig(Builder builder) {
        this.regionType = builder.regionType;
        this.downsample = builder.downsample;
        this.format = builder.format;
        this.outputDirectory = builder.outputDirectory;
        this.addToWorkflow = builder.addToWorkflow;
        this.paddingPixels = builder.paddingPixels;
        this.selectedChannels = builder.selectedChannels == null
                ? null : List.copyOf(builder.selectedChannels);
        this.pyramidLevels = builder.pyramidLevels;
        this.compressionType = builder.compressionType;
        this.tileSize = builder.tileSize;
    }

    public RegionType getRegionType() {
        return regionType;
    }

    public double getDownsample() {
        return downsample;
    }

    public OutputFormat getFormat() {
        return format;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public boolean isAddToWorkflow() {
        return addToWorkflow;
    }

    /**
     * Padding in pixels to add around annotation bounding boxes.
     * Only applies when region type is SELECTED_ANNOTATIONS or ALL_ANNOTATIONS.
     */
    public int getPaddingPixels() {
        return paddingPixels;
    }

    /**
     * Channel indices to export (null = all channels).
     */
    public List<Integer> getSelectedChannels() {
        return selectedChannels;
    }

    /**
     * Number of pyramid levels for OME-TIFF pyramid export.
     */
    public int getPyramidLevels() {
        return pyramidLevels;
    }

    /**
     * Compression type for OME-TIFF pyramid export (e.g., "LZW", "JPEG", "J2K", "ZLIB", "UNCOMPRESSED").
     * Null means default compression.
     */
    public String getCompressionType() {
        return compressionType;
    }

    /**
     * Tile size for OME-TIFF pyramid export.
     */
    public int getTileSize() {
        return tileSize;
    }

    /**
     * Generates a sanitized output filename for a given image entry name.
     *
     * @param entryName the project image entry name
     * @return sanitized filename with appropriate extension
     */
    public String buildOutputFilename(String entryName) {
        String sanitized = GeneralTools.stripInvalidFilenameChars(entryName);
        if (sanitized == null || sanitized.isBlank()) {
            sanitized = "unnamed";
        }
        return sanitized + "." + format.getExtension();
    }

    /**
     * Generates a sanitized output filename with a suffix (e.g., for annotation regions).
     *
     * @param entryName the project image entry name
     * @param suffix    additional suffix (e.g., "_region_1")
     * @return sanitized filename with suffix and appropriate extension
     */
    public String buildOutputFilename(String entryName, String suffix) {
        String sanitized = GeneralTools.stripInvalidFilenameChars(entryName);
        if (sanitized == null || sanitized.isBlank()) {
            sanitized = "unnamed";
        }
        return sanitized + suffix + "." + format.getExtension();
    }

    /**
     * Builder for creating {@link RawExportConfig} instances.
     */
    public static class Builder {

        private RegionType regionType = RegionType.WHOLE_IMAGE;
        private double downsample = 4.0;
        private OutputFormat format = OutputFormat.TIFF;
        private File outputDirectory;
        private boolean addToWorkflow = true;
        private int paddingPixels = 0;
        private List<Integer> selectedChannels = null;
        private int pyramidLevels = 4;
        private String compressionType = null;
        private int tileSize = 512;

        public Builder regionType(RegionType type) {
            this.regionType = type;
            return this;
        }

        public Builder downsample(double ds) {
            this.downsample = ds;
            return this;
        }

        public Builder format(OutputFormat fmt) {
            this.format = fmt;
            return this;
        }

        public Builder outputDirectory(File dir) {
            this.outputDirectory = dir;
            return this;
        }

        public Builder addToWorkflow(boolean add) {
            this.addToWorkflow = add;
            return this;
        }

        public Builder paddingPixels(int padding) {
            this.paddingPixels = padding;
            return this;
        }

        public Builder selectedChannels(List<Integer> channels) {
            this.selectedChannels = channels;
            return this;
        }

        public Builder pyramidLevels(int levels) {
            this.pyramidLevels = levels;
            return this;
        }

        public Builder compressionType(String compression) {
            this.compressionType = compression;
            return this;
        }

        public Builder tileSize(int size) {
            this.tileSize = size;
            return this;
        }

        /**
         * Build the raw export configuration, validating required fields.
         *
         * @return a new RawExportConfig
         * @throws IllegalArgumentException if required fields are missing or invalid
         */
        public RawExportConfig build() {
            if (outputDirectory == null) {
                throw new IllegalArgumentException("Output directory is required");
            }
            if (downsample < 1.0) {
                throw new IllegalArgumentException("Downsample must be >= 1.0");
            }
            if (format == null) {
                throw new IllegalArgumentException("Output format is required");
            }
            if (regionType == null) {
                throw new IllegalArgumentException("Region type is required");
            }
            return new RawExportConfig(this);
        }
    }
}
