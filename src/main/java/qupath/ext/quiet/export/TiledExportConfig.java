package qupath.ext.quiet.export;

import java.io.File;

import qupath.lib.common.GeneralTools;

/**
 * Immutable configuration for tiled image export (ML training data).
 * <p>
 * Wraps QuPath's TileExporter API with configuration for tile size, overlap,
 * label generation, and filtering options.
 */
public class TiledExportConfig {

    /**
     * Which parent objects to restrict tile export to.
     */
    public enum ParentObjectFilter {
        /** Export tiles from annotation regions only. */
        ANNOTATIONS,
        /** Export tiles from TMA core regions. */
        TMA_CORES,
        /** Export tiles from all objects (no parent filter). */
        ALL
    }

    private final int tileSize;
    private final int overlap;
    private final double downsample;
    private final OutputFormat imageFormat;
    private final OutputFormat labelFormat;
    private final MaskExportConfig labeledServerConfig;
    private final ParentObjectFilter parentObjectFilter;
    private final boolean annotatedTilesOnly;
    private final boolean exportGeoJson;
    private final File outputDirectory;
    private final boolean addToWorkflow;

    private TiledExportConfig(Builder builder) {
        this.tileSize = builder.tileSize;
        this.overlap = builder.overlap;
        this.downsample = builder.downsample;
        this.imageFormat = builder.imageFormat;
        this.labelFormat = builder.labelFormat;
        this.labeledServerConfig = builder.labeledServerConfig;
        this.parentObjectFilter = builder.parentObjectFilter;
        this.annotatedTilesOnly = builder.annotatedTilesOnly;
        this.exportGeoJson = builder.exportGeoJson;
        this.outputDirectory = builder.outputDirectory;
        this.addToWorkflow = builder.addToWorkflow;
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getOverlap() {
        return overlap;
    }

    public double getDownsample() {
        return downsample;
    }

    public OutputFormat getImageFormat() {
        return imageFormat;
    }

    /**
     * Label format for mask tiles (null = no label export).
     */
    public OutputFormat getLabelFormat() {
        return labelFormat;
    }

    /**
     * Configuration for the LabeledImageServer used for label tile generation.
     * Only relevant if labelFormat is not null.
     */
    public MaskExportConfig getLabeledServerConfig() {
        return labeledServerConfig;
    }

    public ParentObjectFilter getParentObjectFilter() {
        return parentObjectFilter;
    }

    public boolean isAnnotatedTilesOnly() {
        return annotatedTilesOnly;
    }

    public boolean isExportGeoJson() {
        return exportGeoJson;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public boolean isAddToWorkflow() {
        return addToWorkflow;
    }

    /**
     * Generates a sanitized output filename for a given image entry name.
     */
    public String buildOutputFilename(String entryName) {
        String sanitized = GeneralTools.stripInvalidFilenameChars(entryName);
        if (sanitized == null || sanitized.isBlank()) {
            sanitized = "unnamed";
        }
        return sanitized;
    }

    /**
     * Builder for creating {@link TiledExportConfig} instances.
     */
    public static class Builder {

        private int tileSize = 512;
        private int overlap = 0;
        private double downsample = 1.0;
        private OutputFormat imageFormat = OutputFormat.TIFF;
        private OutputFormat labelFormat = null;
        private MaskExportConfig labeledServerConfig = null;
        private ParentObjectFilter parentObjectFilter = ParentObjectFilter.ANNOTATIONS;
        private boolean annotatedTilesOnly = true;
        private boolean exportGeoJson = false;
        private File outputDirectory;
        private boolean addToWorkflow = true;

        public Builder tileSize(int size) {
            this.tileSize = size;
            return this;
        }

        public Builder overlap(int overlap) {
            this.overlap = overlap;
            return this;
        }

        public Builder downsample(double ds) {
            this.downsample = ds;
            return this;
        }

        public Builder imageFormat(OutputFormat fmt) {
            this.imageFormat = fmt;
            return this;
        }

        public Builder labelFormat(OutputFormat fmt) {
            this.labelFormat = fmt;
            return this;
        }

        public Builder labeledServerConfig(MaskExportConfig config) {
            this.labeledServerConfig = config;
            return this;
        }

        public Builder parentObjectFilter(ParentObjectFilter filter) {
            this.parentObjectFilter = filter;
            return this;
        }

        public Builder annotatedTilesOnly(boolean only) {
            this.annotatedTilesOnly = only;
            return this;
        }

        public Builder exportGeoJson(boolean export) {
            this.exportGeoJson = export;
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

        /**
         * Build the tiled export configuration, validating required fields.
         *
         * @return a new TiledExportConfig
         * @throws IllegalArgumentException if required fields are missing or invalid
         */
        public TiledExportConfig build() {
            if (outputDirectory == null) {
                throw new IllegalArgumentException("Output directory is required");
            }
            if (tileSize < 1) {
                throw new IllegalArgumentException("Tile size must be >= 1");
            }
            if (overlap < 0) {
                throw new IllegalArgumentException("Overlap must be >= 0");
            }
            if (downsample < 1.0) {
                throw new IllegalArgumentException("Downsample must be >= 1.0");
            }
            if (imageFormat == null) {
                throw new IllegalArgumentException("Image format is required");
            }
            return new TiledExportConfig(this);
        }
    }
}
