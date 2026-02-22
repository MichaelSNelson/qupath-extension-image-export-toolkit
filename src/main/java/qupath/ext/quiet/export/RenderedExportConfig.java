package qupath.ext.quiet.export;

import java.awt.Color;
import java.io.File;

import qupath.lib.common.GeneralTools;
import qupath.lib.display.settings.ImageDisplaySettings;

/**
 * Immutable configuration for a rendered image export operation.
 * <p>
 * Supports two render modes: pixel classifier overlay or object overlay
 * (annotations/detections).
 */
public class RenderedExportConfig {

    /**
     * The render mode for the exported image.
     */
    public enum RenderMode {
        /** Export with a pixel classifier overlay rendered on top. */
        CLASSIFIER_OVERLAY,
        /** Export with object overlays (annotations/detections) only. */
        OBJECT_OVERLAY
    }

    /**
     * How display settings (brightness/contrast, channel visibility, LUTs)
     * are applied to the base image before overlay compositing.
     */
    public enum DisplaySettingsMode {
        /** Each image uses its own saved display settings from ImageData properties. */
        PER_IMAGE_SAVED,
        /** Capture the current viewer's display settings and apply to all images. */
        CURRENT_VIEWER,
        /** Load a named preset from the project's saved display settings. */
        SAVED_PRESET,
        /** No display adjustments -- export raw pixel data (original behavior). */
        RAW
    }

    private final RenderMode renderMode;
    private final DisplaySettingsMode displaySettingsMode;
    private final ImageDisplaySettings capturedDisplaySettings;
    private final String displayPresetName;
    private final String classifierName;
    private final double overlayOpacity;
    private final double downsample;
    private final OutputFormat format;
    private final File outputDirectory;
    private final boolean includeAnnotations;
    private final boolean includeDetections;
    private final boolean fillAnnotations;
    private final boolean showNames;
    private final boolean addToWorkflow;
    private final boolean showScaleBar;
    private final ScaleBarRenderer.Position scaleBarPosition;
    private final String scaleBarColorHex;
    private final int scaleBarFontSize;
    private final boolean scaleBarBoldText;

    private RenderedExportConfig(Builder builder) {
        this.renderMode = builder.renderMode;
        this.displaySettingsMode = builder.displaySettingsMode;
        this.capturedDisplaySettings = builder.capturedDisplaySettings;
        this.displayPresetName = builder.displayPresetName;
        this.classifierName = builder.classifierName;
        this.overlayOpacity = builder.overlayOpacity;
        this.downsample = builder.downsample;
        this.format = builder.format;
        this.outputDirectory = builder.outputDirectory;
        this.includeAnnotations = builder.includeAnnotations;
        this.includeDetections = builder.includeDetections;
        this.fillAnnotations = builder.fillAnnotations;
        this.showNames = builder.showNames;
        this.addToWorkflow = builder.addToWorkflow;
        this.showScaleBar = builder.showScaleBar;
        this.scaleBarPosition = builder.scaleBarPosition;
        this.scaleBarColorHex = builder.scaleBarColorHex;
        this.scaleBarFontSize = builder.scaleBarFontSize;
        this.scaleBarBoldText = builder.scaleBarBoldText;
    }

    public RenderMode getRenderMode() {
        return renderMode;
    }

    public DisplaySettingsMode getDisplaySettingsMode() {
        return displaySettingsMode;
    }

    /**
     * Returns the captured display settings for CURRENT_VIEWER mode.
     * Null for other modes.
     */
    public ImageDisplaySettings getCapturedDisplaySettings() {
        return capturedDisplaySettings;
    }

    /**
     * Returns the preset name for SAVED_PRESET mode.
     * Null for other modes.
     */
    public String getDisplayPresetName() {
        return displayPresetName;
    }

    public String getClassifierName() {
        return classifierName;
    }

    public double getOverlayOpacity() {
        return overlayOpacity;
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

    public boolean isIncludeAnnotations() {
        return includeAnnotations;
    }

    public boolean isIncludeDetections() {
        return includeDetections;
    }

    public boolean isFillAnnotations() {
        return fillAnnotations;
    }

    public boolean isShowNames() {
        return showNames;
    }

    public boolean isAddToWorkflow() {
        return addToWorkflow;
    }

    public boolean isShowScaleBar() {
        return showScaleBar;
    }

    public ScaleBarRenderer.Position getScaleBarPosition() {
        return scaleBarPosition;
    }

    /**
     * Returns the scale bar color as a hex string (e.g. "#FFFFFF").
     */
    public String getScaleBarColorHex() {
        return scaleBarColorHex;
    }

    /**
     * Returns the scale bar font size in pixels. 0 means auto-compute.
     */
    public int getScaleBarFontSize() {
        return scaleBarFontSize;
    }

    /**
     * Returns whether scale bar text should be bold.
     */
    public boolean isScaleBarBoldText() {
        return scaleBarBoldText;
    }

    /**
     * Converts the hex color string to a {@link java.awt.Color}.
     * Falls back to white if the hex string is invalid.
     */
    public Color getScaleBarColorAsAwt() {
        try {
            return Color.decode(scaleBarColorHex);
        } catch (NumberFormatException e) {
            return Color.WHITE;
        }
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
     * Builder for creating {@link RenderedExportConfig} instances.
     */
    public static class Builder {

        private RenderMode renderMode = RenderMode.CLASSIFIER_OVERLAY;
        private DisplaySettingsMode displaySettingsMode = DisplaySettingsMode.PER_IMAGE_SAVED;
        private ImageDisplaySettings capturedDisplaySettings;
        private String displayPresetName;
        private String classifierName;
        private double overlayOpacity = 0.5;
        private double downsample = 4.0;
        private OutputFormat format = OutputFormat.PNG;
        private File outputDirectory;
        private boolean includeAnnotations = false;
        private boolean includeDetections = true;
        private boolean fillAnnotations = false;
        private boolean showNames = false;
        private boolean addToWorkflow = true;
        private boolean showScaleBar = false;
        private ScaleBarRenderer.Position scaleBarPosition = ScaleBarRenderer.Position.LOWER_RIGHT;
        private String scaleBarColorHex = "#FFFFFF";
        private int scaleBarFontSize = 0;
        private boolean scaleBarBoldText = true;

        public Builder renderMode(RenderMode mode) {
            this.renderMode = mode;
            return this;
        }

        public Builder displaySettingsMode(DisplaySettingsMode mode) {
            this.displaySettingsMode = mode;
            return this;
        }

        public Builder capturedDisplaySettings(ImageDisplaySettings settings) {
            this.capturedDisplaySettings = settings;
            return this;
        }

        public Builder displayPresetName(String name) {
            this.displayPresetName = name;
            return this;
        }

        public Builder classifierName(String name) {
            this.classifierName = name;
            return this;
        }

        public Builder overlayOpacity(double opacity) {
            this.overlayOpacity = GeneralTools.clipValue(opacity, 0.0, 1.0);
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

        public Builder includeAnnotations(boolean include) {
            this.includeAnnotations = include;
            return this;
        }

        public Builder includeDetections(boolean include) {
            this.includeDetections = include;
            return this;
        }

        public Builder fillAnnotations(boolean fill) {
            this.fillAnnotations = fill;
            return this;
        }

        public Builder showNames(boolean show) {
            this.showNames = show;
            return this;
        }

        public Builder addToWorkflow(boolean add) {
            this.addToWorkflow = add;
            return this;
        }

        public Builder showScaleBar(boolean show) {
            this.showScaleBar = show;
            return this;
        }

        public Builder scaleBarPosition(ScaleBarRenderer.Position position) {
            this.scaleBarPosition = position;
            return this;
        }

        public Builder scaleBarColorHex(String hex) {
            this.scaleBarColorHex = hex != null ? hex : "#FFFFFF";
            return this;
        }

        public Builder scaleBarFontSize(int size) {
            this.scaleBarFontSize = size;
            return this;
        }

        public Builder scaleBarBoldText(boolean bold) {
            this.scaleBarBoldText = bold;
            return this;
        }

        /**
         * Build the export configuration, validating required fields.
         *
         * @return a new RenderedExportConfig
         * @throws IllegalArgumentException if required fields are missing or invalid
         */
        public RenderedExportConfig build() {
            if (renderMode == RenderMode.CLASSIFIER_OVERLAY) {
                if (classifierName == null || classifierName.isBlank()) {
                    throw new IllegalArgumentException("Classifier name is required for classifier overlay mode");
                }
            } else if (renderMode == RenderMode.OBJECT_OVERLAY) {
                if (!includeAnnotations && !includeDetections) {
                    throw new IllegalArgumentException(
                            "At least one object type (annotations or detections) must be selected");
                }
            }
            if (outputDirectory == null) {
                throw new IllegalArgumentException("Output directory is required");
            }
            if (downsample < 1.0) {
                throw new IllegalArgumentException("Downsample must be >= 1.0");
            }
            if (format == null) {
                throw new IllegalArgumentException("Output format is required");
            }
            return new RenderedExportConfig(this);
        }
    }
}
