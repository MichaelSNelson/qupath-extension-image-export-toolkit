package qupath.ext.quiet.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import qupath.lib.gui.prefs.PathPrefs;

/**
 * Persistent preferences for the QuIET extension.
 * <p>
 * All settings are automatically persisted across QuPath sessions using
 * QuPath's preference system. Values are restored when the extension loads.
 */
public class QuietPreferences {

    private static final String PREFIX = "quiet.";

    // --- Global preferences ---

    private static final StringProperty lastCategory =
            PathPrefs.createPersistentPreference(PREFIX + "lastCategory", "RENDERED");

    private static final BooleanProperty addToWorkflow =
            PathPrefs.createPersistentPreference(PREFIX + "addToWorkflow", true);

    private static final BooleanProperty exportGeoJson =
            PathPrefs.createPersistentPreference(PREFIX + "exportGeoJson", false);

    private static final DoubleProperty wizardWidth =
            PathPrefs.createPersistentPreference(PREFIX + "wizardWidth", 750.0);

    private static final DoubleProperty wizardHeight =
            PathPrefs.createPersistentPreference(PREFIX + "wizardHeight", 700.0);

    // --- Rendered export preferences ---

    private static final StringProperty renderedMode =
            PathPrefs.createPersistentPreference(PREFIX + "rendered.mode", "CLASSIFIER_OVERLAY");

    private static final StringProperty renderedClassifierName =
            PathPrefs.createPersistentPreference(PREFIX + "rendered.classifierName", "");

    private static final DoubleProperty renderedOpacity =
            PathPrefs.createPersistentPreference(PREFIX + "rendered.opacity", 0.5);

    private static final DoubleProperty renderedDownsample =
            PathPrefs.createPersistentPreference(PREFIX + "rendered.downsample", 4.0);

    private static final StringProperty renderedFormat =
            PathPrefs.createPersistentPreference(PREFIX + "rendered.format", "PNG");

    private static final BooleanProperty renderedIncludeAnnotations =
            PathPrefs.createPersistentPreference(PREFIX + "rendered.includeAnnotations", false);

    private static final BooleanProperty renderedIncludeDetections =
            PathPrefs.createPersistentPreference(PREFIX + "rendered.includeDetections", true);

    private static final BooleanProperty renderedFillAnnotations =
            PathPrefs.createPersistentPreference(PREFIX + "rendered.fillAnnotations", false);

    private static final BooleanProperty renderedShowNames =
            PathPrefs.createPersistentPreference(PREFIX + "rendered.showNames", false);

    private static final StringProperty renderedDisplayMode =
            PathPrefs.createPersistentPreference(PREFIX + "rendered.displayMode", "PER_IMAGE_SAVED");

    private static final StringProperty renderedDisplayPresetName =
            PathPrefs.createPersistentPreference(PREFIX + "rendered.displayPresetName", "");

    private static final BooleanProperty renderedShowScaleBar =
            PathPrefs.createPersistentPreference(PREFIX + "rendered.showScaleBar", false);

    private static final StringProperty renderedScaleBarPosition =
            PathPrefs.createPersistentPreference(PREFIX + "rendered.scaleBarPosition", "LOWER_RIGHT");

    private static final StringProperty renderedScaleBarColor =
            PathPrefs.createPersistentPreference(PREFIX + "rendered.scaleBarColor", "WHITE");

    // --- Mask export preferences ---

    private static final StringProperty maskType =
            PathPrefs.createPersistentPreference(PREFIX + "mask.maskType", "BINARY");

    private static final IntegerProperty maskBackgroundLabel =
            PathPrefs.createPersistentPreference(PREFIX + "mask.backgroundLabel", 0);

    private static final IntegerProperty maskBoundaryLabel =
            PathPrefs.createPersistentPreference(PREFIX + "mask.boundaryLabel", -1);

    private static final BooleanProperty maskEnableBoundary =
            PathPrefs.createPersistentPreference(PREFIX + "mask.enableBoundary", false);

    private static final StringProperty maskObjectSource =
            PathPrefs.createPersistentPreference(PREFIX + "mask.objectSource", "ANNOTATIONS");

    private static final DoubleProperty maskDownsample =
            PathPrefs.createPersistentPreference(PREFIX + "mask.downsample", 4.0);

    private static final StringProperty maskFormat =
            PathPrefs.createPersistentPreference(PREFIX + "mask.format", "PNG");

    private static final BooleanProperty maskGrayscaleLut =
            PathPrefs.createPersistentPreference(PREFIX + "mask.grayscaleLut", false);

    private static final BooleanProperty maskShuffleInstanceLabels =
            PathPrefs.createPersistentPreference(PREFIX + "mask.shuffleInstanceLabels", false);

    private static final IntegerProperty maskBoundaryThickness =
            PathPrefs.createPersistentPreference(PREFIX + "mask.boundaryThickness", 1);

    // --- Raw export preferences ---

    private static final StringProperty rawRegionType =
            PathPrefs.createPersistentPreference(PREFIX + "raw.regionType", "WHOLE_IMAGE");

    private static final DoubleProperty rawDownsample =
            PathPrefs.createPersistentPreference(PREFIX + "raw.downsample", 4.0);

    private static final StringProperty rawFormat =
            PathPrefs.createPersistentPreference(PREFIX + "raw.format", "TIFF");

    private static final IntegerProperty rawPadding =
            PathPrefs.createPersistentPreference(PREFIX + "raw.padding", 0);

    private static final IntegerProperty rawPyramidLevels =
            PathPrefs.createPersistentPreference(PREFIX + "raw.pyramidLevels", 4);

    private static final IntegerProperty rawTileSize =
            PathPrefs.createPersistentPreference(PREFIX + "raw.tileSize", 512);

    private static final StringProperty rawCompression =
            PathPrefs.createPersistentPreference(PREFIX + "raw.compression", "DEFAULT");

    // --- Tiled export preferences ---

    private static final IntegerProperty tiledTileSize =
            PathPrefs.createPersistentPreference(PREFIX + "tiled.tileSize", 512);

    private static final IntegerProperty tiledOverlap =
            PathPrefs.createPersistentPreference(PREFIX + "tiled.overlap", 0);

    private static final DoubleProperty tiledDownsample =
            PathPrefs.createPersistentPreference(PREFIX + "tiled.downsample", 1.0);

    private static final StringProperty tiledImageFormat =
            PathPrefs.createPersistentPreference(PREFIX + "tiled.imageFormat", "TIFF");

    private static final BooleanProperty tiledAnnotatedOnly =
            PathPrefs.createPersistentPreference(PREFIX + "tiled.annotatedOnly", true);

    private static final BooleanProperty tiledExportJson =
            PathPrefs.createPersistentPreference(PREFIX + "tiled.exportJson", false);

    private static final BooleanProperty tiledEnableLabels =
            PathPrefs.createPersistentPreference(PREFIX + "tiled.enableLabels", true);

    private static final StringProperty tiledLabelFormat =
            PathPrefs.createPersistentPreference(PREFIX + "tiled.labelFormat", "PNG");

    private QuietPreferences() {
        // Utility class
    }

    // ==================== Global ====================

    public static StringProperty lastCategoryProperty() { return lastCategory; }
    public static String getLastCategory() { return lastCategory.get(); }
    public static void setLastCategory(String value) { lastCategory.set(value != null ? value : "RENDERED"); }

    public static BooleanProperty addToWorkflowProperty() { return addToWorkflow; }
    public static boolean isAddToWorkflow() { return addToWorkflow.get(); }
    public static void setAddToWorkflow(boolean value) { addToWorkflow.set(value); }

    public static BooleanProperty exportGeoJsonProperty() { return exportGeoJson; }
    public static boolean isExportGeoJson() { return exportGeoJson.get(); }
    public static void setExportGeoJson(boolean value) { exportGeoJson.set(value); }

    public static DoubleProperty wizardWidthProperty() { return wizardWidth; }
    public static double getWizardWidth() { return wizardWidth.get(); }
    public static void setWizardWidth(double value) { wizardWidth.set(value); }

    public static DoubleProperty wizardHeightProperty() { return wizardHeight; }
    public static double getWizardHeight() { return wizardHeight.get(); }
    public static void setWizardHeight(double value) { wizardHeight.set(value); }

    // ==================== Rendered ====================

    public static StringProperty renderedModeProperty() { return renderedMode; }
    public static String getRenderedMode() { return renderedMode.get(); }
    public static void setRenderedMode(String value) { renderedMode.set(value != null ? value : "CLASSIFIER_OVERLAY"); }

    public static StringProperty renderedClassifierNameProperty() { return renderedClassifierName; }
    public static String getRenderedClassifierName() { return renderedClassifierName.get(); }
    public static void setRenderedClassifierName(String value) { renderedClassifierName.set(value != null ? value : ""); }

    public static DoubleProperty renderedOpacityProperty() { return renderedOpacity; }
    public static double getRenderedOpacity() { return renderedOpacity.get(); }
    public static void setRenderedOpacity(double value) { renderedOpacity.set(value); }

    public static DoubleProperty renderedDownsampleProperty() { return renderedDownsample; }
    public static double getRenderedDownsample() { return renderedDownsample.get(); }
    public static void setRenderedDownsample(double value) { renderedDownsample.set(value); }

    public static StringProperty renderedFormatProperty() { return renderedFormat; }
    public static String getRenderedFormat() { return renderedFormat.get(); }
    public static void setRenderedFormat(String value) { renderedFormat.set(value != null ? value : "PNG"); }

    public static BooleanProperty renderedIncludeAnnotationsProperty() { return renderedIncludeAnnotations; }
    public static boolean isRenderedIncludeAnnotations() { return renderedIncludeAnnotations.get(); }
    public static void setRenderedIncludeAnnotations(boolean value) { renderedIncludeAnnotations.set(value); }

    public static BooleanProperty renderedIncludeDetectionsProperty() { return renderedIncludeDetections; }
    public static boolean isRenderedIncludeDetections() { return renderedIncludeDetections.get(); }
    public static void setRenderedIncludeDetections(boolean value) { renderedIncludeDetections.set(value); }

    public static BooleanProperty renderedFillAnnotationsProperty() { return renderedFillAnnotations; }
    public static boolean isRenderedFillAnnotations() { return renderedFillAnnotations.get(); }
    public static void setRenderedFillAnnotations(boolean value) { renderedFillAnnotations.set(value); }

    public static BooleanProperty renderedShowNamesProperty() { return renderedShowNames; }
    public static boolean isRenderedShowNames() { return renderedShowNames.get(); }
    public static void setRenderedShowNames(boolean value) { renderedShowNames.set(value); }

    public static StringProperty renderedDisplayModeProperty() { return renderedDisplayMode; }
    public static String getRenderedDisplayMode() { return renderedDisplayMode.get(); }
    public static void setRenderedDisplayMode(String value) { renderedDisplayMode.set(value != null ? value : "PER_IMAGE_SAVED"); }

    public static StringProperty renderedDisplayPresetNameProperty() { return renderedDisplayPresetName; }
    public static String getRenderedDisplayPresetName() { return renderedDisplayPresetName.get(); }
    public static void setRenderedDisplayPresetName(String value) { renderedDisplayPresetName.set(value != null ? value : ""); }

    public static BooleanProperty renderedShowScaleBarProperty() { return renderedShowScaleBar; }
    public static boolean isRenderedShowScaleBar() { return renderedShowScaleBar.get(); }
    public static void setRenderedShowScaleBar(boolean value) { renderedShowScaleBar.set(value); }

    public static StringProperty renderedScaleBarPositionProperty() { return renderedScaleBarPosition; }
    public static String getRenderedScaleBarPosition() { return renderedScaleBarPosition.get(); }
    public static void setRenderedScaleBarPosition(String value) { renderedScaleBarPosition.set(value != null ? value : "LOWER_RIGHT"); }

    public static StringProperty renderedScaleBarColorProperty() { return renderedScaleBarColor; }
    public static String getRenderedScaleBarColor() { return renderedScaleBarColor.get(); }
    public static void setRenderedScaleBarColor(String value) { renderedScaleBarColor.set(value != null ? value : "WHITE"); }

    // ==================== Mask ====================

    public static StringProperty maskTypeProperty() { return maskType; }
    public static String getMaskType() { return maskType.get(); }
    public static void setMaskType(String value) { maskType.set(value != null ? value : "BINARY"); }

    public static IntegerProperty maskBackgroundLabelProperty() { return maskBackgroundLabel; }
    public static int getMaskBackgroundLabel() { return maskBackgroundLabel.get(); }
    public static void setMaskBackgroundLabel(int value) { maskBackgroundLabel.set(value); }

    public static IntegerProperty maskBoundaryLabelProperty() { return maskBoundaryLabel; }
    public static int getMaskBoundaryLabel() { return maskBoundaryLabel.get(); }
    public static void setMaskBoundaryLabel(int value) { maskBoundaryLabel.set(value); }

    public static BooleanProperty maskEnableBoundaryProperty() { return maskEnableBoundary; }
    public static boolean isMaskEnableBoundary() { return maskEnableBoundary.get(); }
    public static void setMaskEnableBoundary(boolean value) { maskEnableBoundary.set(value); }

    public static StringProperty maskObjectSourceProperty() { return maskObjectSource; }
    public static String getMaskObjectSource() { return maskObjectSource.get(); }
    public static void setMaskObjectSource(String value) { maskObjectSource.set(value != null ? value : "ANNOTATIONS"); }

    public static DoubleProperty maskDownsampleProperty() { return maskDownsample; }
    public static double getMaskDownsample() { return maskDownsample.get(); }
    public static void setMaskDownsample(double value) { maskDownsample.set(value); }

    public static StringProperty maskFormatProperty() { return maskFormat; }
    public static String getMaskFormat() { return maskFormat.get(); }
    public static void setMaskFormat(String value) { maskFormat.set(value != null ? value : "PNG"); }

    public static BooleanProperty maskGrayscaleLutProperty() { return maskGrayscaleLut; }
    public static boolean isMaskGrayscaleLut() { return maskGrayscaleLut.get(); }
    public static void setMaskGrayscaleLut(boolean value) { maskGrayscaleLut.set(value); }

    public static BooleanProperty maskShuffleInstanceLabelsProperty() { return maskShuffleInstanceLabels; }
    public static boolean isMaskShuffleInstanceLabels() { return maskShuffleInstanceLabels.get(); }
    public static void setMaskShuffleInstanceLabels(boolean value) { maskShuffleInstanceLabels.set(value); }

    public static IntegerProperty maskBoundaryThicknessProperty() { return maskBoundaryThickness; }
    public static int getMaskBoundaryThickness() { return maskBoundaryThickness.get(); }
    public static void setMaskBoundaryThickness(int value) { maskBoundaryThickness.set(value); }

    // ==================== Raw ====================

    public static StringProperty rawRegionTypeProperty() { return rawRegionType; }
    public static String getRawRegionType() { return rawRegionType.get(); }
    public static void setRawRegionType(String value) { rawRegionType.set(value != null ? value : "WHOLE_IMAGE"); }

    public static DoubleProperty rawDownsampleProperty() { return rawDownsample; }
    public static double getRawDownsample() { return rawDownsample.get(); }
    public static void setRawDownsample(double value) { rawDownsample.set(value); }

    public static StringProperty rawFormatProperty() { return rawFormat; }
    public static String getRawFormat() { return rawFormat.get(); }
    public static void setRawFormat(String value) { rawFormat.set(value != null ? value : "TIFF"); }

    public static IntegerProperty rawPaddingProperty() { return rawPadding; }
    public static int getRawPadding() { return rawPadding.get(); }
    public static void setRawPadding(int value) { rawPadding.set(value); }

    public static IntegerProperty rawPyramidLevelsProperty() { return rawPyramidLevels; }
    public static int getRawPyramidLevels() { return rawPyramidLevels.get(); }
    public static void setRawPyramidLevels(int value) { rawPyramidLevels.set(value); }

    public static IntegerProperty rawTileSizeProperty() { return rawTileSize; }
    public static int getRawTileSize() { return rawTileSize.get(); }
    public static void setRawTileSize(int value) { rawTileSize.set(value); }

    public static StringProperty rawCompressionProperty() { return rawCompression; }
    public static String getRawCompression() { return rawCompression.get(); }
    public static void setRawCompression(String value) { rawCompression.set(value != null ? value : "DEFAULT"); }

    // ==================== Tiled ====================

    public static IntegerProperty tiledTileSizeProperty() { return tiledTileSize; }
    public static int getTiledTileSize() { return tiledTileSize.get(); }
    public static void setTiledTileSize(int value) { tiledTileSize.set(value); }

    public static IntegerProperty tiledOverlapProperty() { return tiledOverlap; }
    public static int getTiledOverlap() { return tiledOverlap.get(); }
    public static void setTiledOverlap(int value) { tiledOverlap.set(value); }

    public static DoubleProperty tiledDownsampleProperty() { return tiledDownsample; }
    public static double getTiledDownsample() { return tiledDownsample.get(); }
    public static void setTiledDownsample(double value) { tiledDownsample.set(value); }

    public static StringProperty tiledImageFormatProperty() { return tiledImageFormat; }
    public static String getTiledImageFormat() { return tiledImageFormat.get(); }
    public static void setTiledImageFormat(String value) { tiledImageFormat.set(value != null ? value : "TIFF"); }

    public static BooleanProperty tiledAnnotatedOnlyProperty() { return tiledAnnotatedOnly; }
    public static boolean isTiledAnnotatedOnly() { return tiledAnnotatedOnly.get(); }
    public static void setTiledAnnotatedOnly(boolean value) { tiledAnnotatedOnly.set(value); }

    public static BooleanProperty tiledExportJsonProperty() { return tiledExportJson; }
    public static boolean isTiledExportJson() { return tiledExportJson.get(); }
    public static void setTiledExportJson(boolean value) { tiledExportJson.set(value); }

    public static BooleanProperty tiledEnableLabelsProperty() { return tiledEnableLabels; }
    public static boolean isTiledEnableLabels() { return tiledEnableLabels.get(); }
    public static void setTiledEnableLabels(boolean value) { tiledEnableLabels.set(value); }

    public static StringProperty tiledLabelFormatProperty() { return tiledLabelFormat; }
    public static String getTiledLabelFormat() { return tiledLabelFormat.get(); }
    public static void setTiledLabelFormat(String value) { tiledLabelFormat.set(value != null ? value : "PNG"); }
}
