package qupath.ext.quiet.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.controlsfx.control.CheckComboBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import qupath.ext.quiet.export.OutputFormat;
import qupath.ext.quiet.export.RenderedExportConfig;
import qupath.ext.quiet.export.RenderedExportConfig.DisplaySettingsMode;
import qupath.ext.quiet.export.RenderedImageExporter;
import qupath.ext.quiet.export.ScaleBarRenderer;
import qupath.ext.quiet.preferences.QuietPreferences;
import qupath.fx.utils.FXUtils;
import qupath.lib.analysis.heatmaps.DensityMaps;
import qupath.lib.analysis.heatmaps.DensityMaps.DensityMapBuilder;
import qupath.lib.classifiers.pixel.PixelClassifier;
import qupath.lib.color.ColorMaps;
import qupath.lib.display.settings.DisplaySettingUtils;
import qupath.lib.display.settings.ImageDisplaySettings;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.ImageData;
import qupath.lib.objects.classes.PathClass;

/**
 * Step 2a of the export wizard: Configure rendered image export.
 */
public class RenderedConfigPane extends GridPane {

    private static final Logger logger = LoggerFactory.getLogger(RenderedConfigPane.class);
    private static final ResourceBundle resources =
            ResourceBundle.getBundle("qupath.ext.quiet.ui.strings");

    private final QuPathGUI qupath;

    private ComboBox<RenderedExportConfig.RegionType> regionTypeCombo;
    private CheckComboBox<String> classificationCombo;
    private Label classificationFilterLabel;
    private HBox classificationFilterBox;
    private Label paddingLabel;
    private Spinner<Integer> paddingSpinner;

    private ComboBox<RenderedExportConfig.RenderMode> modeCombo;
    private ComboBox<DisplaySettingsMode> displaySettingsCombo;
    private ComboBox<String> presetNameCombo;
    private ComboBox<String> classifierCombo;
    private Slider opacitySlider;
    private Label opacityValueLabel;
    private ComboBox<Double> downsampleCombo;
    private ComboBox<OutputFormat> formatCombo;
    private CheckBox includeAnnotationsCheck;
    private CheckBox includeDetectionsCheck;
    private CheckBox fillAnnotationsCheck;
    private CheckBox showNamesCheck;
    private CheckBox showScaleBarCheck;
    private ComboBox<ScaleBarRenderer.Position> scaleBarPositionCombo;
    private ColorPicker scaleBarColorPicker;
    private Spinner<Integer> scaleBarFontSizeSpinner;
    private CheckBox scaleBarBoldCheck;
    private Button previewButton;

    // Density map controls
    private ComboBox<String> densityMapCombo;
    private Label densityMapLabel;
    private HBox densityMapBox;
    private ComboBox<String> colormapCombo;
    private Label colormapLabel;

    // Color scale bar controls
    private CheckBox showColorScaleBarCheck;
    private ComboBox<ScaleBarRenderer.Position> colorScaleBarPositionCombo;
    private Label colorScaleBarPositionLabel;
    private Spinner<Integer> colorScaleBarFontSizeSpinner;
    private Label colorScaleBarFontSizeLabel;
    private CheckBox colorScaleBarBoldCheck;

    // Controls needing visibility toggling
    private Label classifierLabel;
    private HBox classifierBox;
    private Label presetLabel;
    private HBox presetBox;
    private Label scaleBarPositionLabel;
    private Label scaleBarColorLabel;
    private Label scaleBarFontSizeLabel;

    public RenderedConfigPane(QuPathGUI qupath) {
        this.qupath = qupath;
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(10));
        buildUI();
        populateClassifiers();
        populatePresets();
        populateDensityMaps();
        restorePreferences();
    }

    private void buildUI() {
        int row = 0;

        var header = new Label(resources.getString("wizard.step2.title") + " - Rendered Image");
        header.setFont(Font.font(null, FontWeight.BOLD, 14));
        add(header, 0, row, 2, 1);
        row++;

        // Render mode selection
        add(new Label(resources.getString("rendered.label.mode")), 0, row);
        modeCombo = new ComboBox<>(FXCollections.observableArrayList(
                RenderedExportConfig.RenderMode.values()));
        modeCombo.setValue(RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY);
        modeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(RenderedExportConfig.RenderMode mode) {
                if (mode == null) return "";
                return switch (mode) {
                    case CLASSIFIER_OVERLAY -> resources.getString("rendered.mode.classifier");
                    case OBJECT_OVERLAY -> resources.getString("rendered.mode.object");
                    case DENSITY_MAP_OVERLAY -> resources.getString("rendered.mode.densityMap");
                };
            }
            @Override
            public RenderedExportConfig.RenderMode fromString(String s) {
                return RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY;
            }
        });
        add(modeCombo, 1, row);
        row++;

        // Region type selection
        add(new Label(resources.getString("rendered.label.regionType")), 0, row);
        regionTypeCombo = new ComboBox<>(FXCollections.observableArrayList(
                RenderedExportConfig.RegionType.values()));
        regionTypeCombo.setValue(RenderedExportConfig.RegionType.WHOLE_IMAGE);
        regionTypeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(RenderedExportConfig.RegionType type) {
                if (type == null) return "";
                return switch (type) {
                    case WHOLE_IMAGE -> resources.getString("rendered.region.wholeImage");
                    case ALL_ANNOTATIONS -> resources.getString("rendered.region.allAnnotations");
                };
            }
            @Override
            public RenderedExportConfig.RegionType fromString(String s) {
                return RenderedExportConfig.RegionType.WHOLE_IMAGE;
            }
        });
        regionTypeCombo.valueProperty().addListener(
                (obs, oldVal, newVal) -> updateRegionTypeVisibility(newVal));
        add(regionTypeCombo, 1, row);
        row++;

        // Display settings mode
        add(new Label(resources.getString("rendered.label.displaySettings")), 0, row);
        displaySettingsCombo = new ComboBox<>(FXCollections.observableArrayList(
                DisplaySettingsMode.values()));
        displaySettingsCombo.setValue(DisplaySettingsMode.PER_IMAGE_SAVED);
        displaySettingsCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(DisplaySettingsMode mode) {
                if (mode == null) return "";
                return switch (mode) {
                    case PER_IMAGE_SAVED -> resources.getString("rendered.display.perImage");
                    case CURRENT_VIEWER -> resources.getString("rendered.display.currentViewer");
                    case SAVED_PRESET -> resources.getString("rendered.display.savedPreset");
                    case RAW -> resources.getString("rendered.display.raw");
                };
            }
            @Override
            public DisplaySettingsMode fromString(String s) {
                return DisplaySettingsMode.PER_IMAGE_SAVED;
            }
        });
        // Disable CURRENT_VIEWER when no image is open in the viewer
        displaySettingsCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DisplaySettingsMode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                } else {
                    setText(displaySettingsCombo.getConverter().toString(item));
                    if (item == DisplaySettingsMode.CURRENT_VIEWER) {
                        boolean noViewer = qupath.getViewer() == null
                                || qupath.getViewer().getImageData() == null;
                        setDisable(noViewer);
                        if (noViewer) {
                            setTooltip(new Tooltip(
                                    resources.getString("rendered.display.viewerRequired")));
                        }
                    } else {
                        setDisable(false);
                        setTooltip(null);
                    }
                }
            }
        });
        displaySettingsCombo.valueProperty().addListener(
                (obs, oldVal, newVal) -> updateDisplaySettingsVisibility(newVal));
        add(displaySettingsCombo, 1, row);
        row++;

        // Preset name selection (SAVED_PRESET mode only)
        presetLabel = new Label(resources.getString("rendered.label.presetName"));
        add(presetLabel, 0, row);
        presetNameCombo = new ComboBox<>();
        presetNameCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(presetNameCombo, Priority.ALWAYS);
        presetNameCombo.setPromptText(resources.getString("rendered.display.noPresets"));
        var presetRefreshButton = new Button(resources.getString("button.refresh"));
        presetRefreshButton.setOnAction(e -> populatePresets());
        presetBox = new HBox(5, presetNameCombo, presetRefreshButton);
        HBox.setHgrow(presetNameCombo, Priority.ALWAYS);
        add(presetBox, 1, row);
        row++;

        // Classifier selection (classifier mode only)
        classifierLabel = new Label(resources.getString("rendered.label.classifier"));
        add(classifierLabel, 0, row);
        classifierCombo = new ComboBox<>();
        classifierCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(classifierCombo, Priority.ALWAYS);
        var refreshButton = new Button(resources.getString("button.refresh"));
        refreshButton.setOnAction(e -> populateClassifiers());
        classifierBox = new HBox(5, classifierCombo, refreshButton);
        HBox.setHgrow(classifierCombo, Priority.ALWAYS);
        add(classifierBox, 1, row);
        row++;

        // Opacity slider
        add(new Label(resources.getString("rendered.label.opacity")), 0, row);
        opacitySlider = new Slider(0.0, 1.0, 0.5);
        opacitySlider.setShowTickMarks(true);
        opacitySlider.setShowTickLabels(true);
        opacitySlider.setMajorTickUnit(0.25);
        opacitySlider.setBlockIncrement(0.05);
        opacityValueLabel = new Label("0.50");
        opacityValueLabel.setMinWidth(35);
        opacitySlider.valueProperty().addListener((obs, oldVal, newVal) ->
                opacityValueLabel.setText(String.format("%.2f", newVal.doubleValue())));
        var opacityBox = new HBox(5, opacitySlider, opacityValueLabel);
        HBox.setHgrow(opacitySlider, Priority.ALWAYS);
        add(opacityBox, 1, row);
        row++;

        // Downsample combo
        add(new Label(resources.getString("rendered.label.downsample")), 0, row);
        downsampleCombo = new ComboBox<>(FXCollections.observableArrayList(
                1.0, 2.0, 4.0, 8.0, 16.0, 32.0));
        downsampleCombo.setEditable(true);
        downsampleCombo.setValue(4.0);
        downsampleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Double value) {
                if (value == null) return "";
                return value == Math.floor(value) ?
                        String.valueOf(value.intValue()) : String.valueOf(value);
            }
            @Override
            public Double fromString(String string) {
                try { return Double.parseDouble(string); }
                catch (NumberFormatException e) { return 4.0; }
            }
        });
        add(downsampleCombo, 1, row);
        row++;

        // Format combo
        add(new Label(resources.getString("rendered.label.format")), 0, row);
        formatCombo = new ComboBox<>(FXCollections.observableArrayList(OutputFormat.values()));
        formatCombo.setValue(OutputFormat.PNG);
        add(formatCombo, 1, row);
        row++;

        // Classification filter (ALL_ANNOTATIONS mode only)
        classificationFilterLabel = new Label(resources.getString("rendered.label.classificationFilter"));
        add(classificationFilterLabel, 0, row);
        classificationCombo = new CheckComboBox<>();
        classificationCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(classificationCombo, Priority.ALWAYS);
        classificationCombo.setTitle("All selected");
        FXUtils.installSelectAllOrNoneMenu(classificationCombo);
        var classRefreshButton = new Button(resources.getString("button.refresh"));
        classRefreshButton.setOnAction(e -> populateAnnotationClassifications());
        classificationFilterBox = new HBox(5, classificationCombo, classRefreshButton);
        HBox.setHgrow(classificationCombo, Priority.ALWAYS);
        add(classificationFilterBox, 1, row);
        row++;

        // Padding spinner (ALL_ANNOTATIONS mode only)
        paddingLabel = new Label(resources.getString("rendered.label.padding"));
        add(paddingLabel, 0, row);
        paddingSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, 0, 10));
        paddingSpinner.setEditable(true);
        paddingSpinner.setPrefWidth(100);
        add(paddingSpinner, 1, row);
        row++;

        // Object overlay options
        includeAnnotationsCheck = new CheckBox(resources.getString("rendered.label.includeAnnotations"));
        add(includeAnnotationsCheck, 1, row);
        row++;

        includeDetectionsCheck = new CheckBox(resources.getString("rendered.label.includeDetections"));
        includeDetectionsCheck.setSelected(true);
        add(includeDetectionsCheck, 1, row);
        row++;

        fillAnnotationsCheck = new CheckBox(resources.getString("rendered.label.fillAnnotations"));
        add(fillAnnotationsCheck, 1, row);
        row++;

        showNamesCheck = new CheckBox(resources.getString("rendered.label.showNames"));
        add(showNamesCheck, 1, row);
        row++;

        // Scale bar options
        showScaleBarCheck = new CheckBox(resources.getString("rendered.label.showScaleBar"));
        add(showScaleBarCheck, 1, row);
        row++;

        scaleBarPositionLabel = new Label(resources.getString("rendered.label.scaleBarPosition"));
        add(scaleBarPositionLabel, 0, row);
        scaleBarPositionCombo = new ComboBox<>(FXCollections.observableArrayList(
                ScaleBarRenderer.Position.values()));
        scaleBarPositionCombo.setValue(ScaleBarRenderer.Position.LOWER_RIGHT);
        scaleBarPositionCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ScaleBarRenderer.Position pos) {
                if (pos == null) return "";
                return switch (pos) {
                    case LOWER_RIGHT -> resources.getString("rendered.scaleBar.lowerRight");
                    case LOWER_LEFT -> resources.getString("rendered.scaleBar.lowerLeft");
                    case UPPER_RIGHT -> resources.getString("rendered.scaleBar.upperRight");
                    case UPPER_LEFT -> resources.getString("rendered.scaleBar.upperLeft");
                };
            }
            @Override
            public ScaleBarRenderer.Position fromString(String s) {
                return ScaleBarRenderer.Position.LOWER_RIGHT;
            }
        });
        add(scaleBarPositionCombo, 1, row);
        row++;

        scaleBarColorLabel = new Label(resources.getString("rendered.label.scaleBarColor"));
        add(scaleBarColorLabel, 0, row);
        scaleBarColorPicker = new ColorPicker(javafx.scene.paint.Color.WHITE);
        add(scaleBarColorPicker, 1, row);
        row++;

        scaleBarFontSizeLabel = new Label(resources.getString("rendered.label.scaleBarFontSize"));
        add(scaleBarFontSizeLabel, 0, row);
        var fontSizeFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 72, 0);
        fontSizeFactory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                if (value == null || value == 0)
                    return resources.getString("rendered.scaleBar.fontSizeAuto");
                return String.valueOf(value);
            }
            @Override
            public Integer fromString(String string) {
                if (string == null || string.isBlank()
                        || string.equalsIgnoreCase(resources.getString("rendered.scaleBar.fontSizeAuto"))) {
                    return 0;
                }
                try { return Integer.parseInt(string); }
                catch (NumberFormatException e) { return 0; }
            }
        });
        scaleBarFontSizeSpinner = new Spinner<>(fontSizeFactory);
        scaleBarFontSizeSpinner.setEditable(true);
        add(scaleBarFontSizeSpinner, 1, row);
        row++;

        scaleBarBoldCheck = new CheckBox(resources.getString("rendered.label.scaleBarBold"));
        scaleBarBoldCheck.setSelected(true);
        add(scaleBarBoldCheck, 1, row);
        row++;

        // Density map selector (density map mode only)
        densityMapLabel = new Label(resources.getString("rendered.label.densityMap"));
        add(densityMapLabel, 0, row);
        densityMapCombo = new ComboBox<>();
        densityMapCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(densityMapCombo, Priority.ALWAYS);
        var dmRefreshButton = new Button(resources.getString("button.refresh"));
        dmRefreshButton.setOnAction(e -> populateDensityMaps());
        densityMapBox = new HBox(5, densityMapCombo, dmRefreshButton);
        HBox.setHgrow(densityMapCombo, Priority.ALWAYS);
        add(densityMapBox, 1, row);
        row++;

        // Colormap/LUT selector (density map mode only)
        colormapLabel = new Label(resources.getString("rendered.label.colormap"));
        add(colormapLabel, 0, row);
        colormapCombo = new ComboBox<>();
        colormapCombo.getItems().addAll(ColorMaps.getColorMaps().keySet());
        colormapCombo.setValue("Viridis");
        add(colormapCombo, 1, row);
        row++;

        // Color scale bar options
        showColorScaleBarCheck = new CheckBox(resources.getString("rendered.label.showColorScaleBar"));
        add(showColorScaleBarCheck, 1, row);
        row++;

        colorScaleBarPositionLabel = new Label(resources.getString("rendered.label.colorScaleBarPosition"));
        add(colorScaleBarPositionLabel, 0, row);
        colorScaleBarPositionCombo = new ComboBox<>(FXCollections.observableArrayList(
                ScaleBarRenderer.Position.values()));
        colorScaleBarPositionCombo.setValue(ScaleBarRenderer.Position.LOWER_RIGHT);
        colorScaleBarPositionCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ScaleBarRenderer.Position pos) {
                if (pos == null) return "";
                return switch (pos) {
                    case LOWER_RIGHT -> resources.getString("rendered.scaleBar.lowerRight");
                    case LOWER_LEFT -> resources.getString("rendered.scaleBar.lowerLeft");
                    case UPPER_RIGHT -> resources.getString("rendered.scaleBar.upperRight");
                    case UPPER_LEFT -> resources.getString("rendered.scaleBar.upperLeft");
                };
            }
            @Override
            public ScaleBarRenderer.Position fromString(String s) {
                return ScaleBarRenderer.Position.LOWER_RIGHT;
            }
        });
        add(colorScaleBarPositionCombo, 1, row);
        row++;

        colorScaleBarFontSizeLabel = new Label(resources.getString("rendered.label.colorScaleBarFontSize"));
        add(colorScaleBarFontSizeLabel, 0, row);
        var csFontSizeFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 72, 0);
        csFontSizeFactory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                if (value == null || value == 0)
                    return resources.getString("rendered.scaleBar.fontSizeAuto");
                return String.valueOf(value);
            }
            @Override
            public Integer fromString(String string) {
                if (string == null || string.isBlank()
                        || string.equalsIgnoreCase(resources.getString("rendered.scaleBar.fontSizeAuto"))) {
                    return 0;
                }
                try { return Integer.parseInt(string); }
                catch (NumberFormatException e) { return 0; }
            }
        });
        colorScaleBarFontSizeSpinner = new Spinner<>(csFontSizeFactory);
        colorScaleBarFontSizeSpinner.setEditable(true);
        add(colorScaleBarFontSizeSpinner, 1, row);
        row++;

        colorScaleBarBoldCheck = new CheckBox(resources.getString("rendered.label.colorScaleBarBold"));
        colorScaleBarBoldCheck.setSelected(true);
        add(colorScaleBarBoldCheck, 1, row);
        row++;

        // Preview button
        previewButton = new Button(resources.getString("rendered.label.previewImage"));
        previewButton.setOnAction(e -> handlePreview());
        previewButton.setMaxWidth(Double.MAX_VALUE);
        add(previewButton, 0, row, 2, 1);
        row++;

        // Scale bar visibility toggling
        showScaleBarCheck.selectedProperty().addListener(
                (obs, oldVal, newVal) -> updateScaleBarVisibility(newVal));
        updateScaleBarVisibility(false);

        // Color scale bar visibility toggling
        showColorScaleBarCheck.selectedProperty().addListener(
                (obs, oldVal, newVal) -> updateColorScaleBarVisibility(newVal));
        updateColorScaleBarVisibility(false);

        // Preview button enabled state depends on image being open
        updatePreviewButtonState();
        qupath.imageDataProperty().addListener((obs, oldVal, newVal) -> updatePreviewButtonState());

        // Mode switching
        modeCombo.valueProperty().addListener((obs, oldMode, newMode) -> updateModeVisibility(newMode));
        updateModeVisibility(RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY);

        // Region type visibility (default hidden)
        updateRegionTypeVisibility(RenderedExportConfig.RegionType.WHOLE_IMAGE);

        wireTooltips();
    }

    private void updateModeVisibility(RenderedExportConfig.RenderMode mode) {
        boolean isClassifier = (mode == RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY);
        boolean isDensityMap = (mode == RenderedExportConfig.RenderMode.DENSITY_MAP_OVERLAY);

        classifierLabel.setVisible(isClassifier);
        classifierLabel.setManaged(isClassifier);
        classifierBox.setVisible(isClassifier);
        classifierBox.setManaged(isClassifier);

        densityMapLabel.setVisible(isDensityMap);
        densityMapLabel.setManaged(isDensityMap);
        densityMapBox.setVisible(isDensityMap);
        densityMapBox.setManaged(isDensityMap);
        colormapLabel.setVisible(isDensityMap);
        colormapLabel.setManaged(isDensityMap);
        colormapCombo.setVisible(isDensityMap);
        colormapCombo.setManaged(isDensityMap);

        // Color scale bar only makes sense for density map mode
        showColorScaleBarCheck.setVisible(isDensityMap);
        showColorScaleBarCheck.setManaged(isDensityMap);
        if (!isDensityMap) {
            updateColorScaleBarVisibility(false);
        } else {
            updateColorScaleBarVisibility(showColorScaleBarCheck.isSelected());
        }
    }

    private void updateDisplaySettingsVisibility(DisplaySettingsMode mode) {
        boolean isPreset = (mode == DisplaySettingsMode.SAVED_PRESET);
        presetLabel.setVisible(isPreset);
        presetLabel.setManaged(isPreset);
        presetBox.setVisible(isPreset);
        presetBox.setManaged(isPreset);
    }

    private void updateScaleBarVisibility(boolean showScaleBar) {
        scaleBarPositionLabel.setVisible(showScaleBar);
        scaleBarPositionLabel.setManaged(showScaleBar);
        scaleBarPositionCombo.setVisible(showScaleBar);
        scaleBarPositionCombo.setManaged(showScaleBar);
        scaleBarColorLabel.setVisible(showScaleBar);
        scaleBarColorLabel.setManaged(showScaleBar);
        scaleBarColorPicker.setVisible(showScaleBar);
        scaleBarColorPicker.setManaged(showScaleBar);
        scaleBarFontSizeLabel.setVisible(showScaleBar);
        scaleBarFontSizeLabel.setManaged(showScaleBar);
        scaleBarFontSizeSpinner.setVisible(showScaleBar);
        scaleBarFontSizeSpinner.setManaged(showScaleBar);
        scaleBarBoldCheck.setVisible(showScaleBar);
        scaleBarBoldCheck.setManaged(showScaleBar);
    }

    private void updateColorScaleBarVisibility(boolean show) {
        colorScaleBarPositionLabel.setVisible(show);
        colorScaleBarPositionLabel.setManaged(show);
        colorScaleBarPositionCombo.setVisible(show);
        colorScaleBarPositionCombo.setManaged(show);
        colorScaleBarFontSizeLabel.setVisible(show);
        colorScaleBarFontSizeLabel.setManaged(show);
        colorScaleBarFontSizeSpinner.setVisible(show);
        colorScaleBarFontSizeSpinner.setManaged(show);
        colorScaleBarBoldCheck.setVisible(show);
        colorScaleBarBoldCheck.setManaged(show);
    }

    private void updateRegionTypeVisibility(RenderedExportConfig.RegionType regionType) {
        boolean isPerAnnotation = (regionType == RenderedExportConfig.RegionType.ALL_ANNOTATIONS);
        classificationFilterLabel.setVisible(isPerAnnotation);
        classificationFilterLabel.setManaged(isPerAnnotation);
        classificationFilterBox.setVisible(isPerAnnotation);
        classificationFilterBox.setManaged(isPerAnnotation);
        paddingLabel.setVisible(isPerAnnotation);
        paddingLabel.setManaged(isPerAnnotation);
        paddingSpinner.setVisible(isPerAnnotation);
        paddingSpinner.setManaged(isPerAnnotation);
        if (isPerAnnotation) {
            populateAnnotationClassifications();
        }
    }

    private void populateAnnotationClassifications() {
        classificationCombo.getItems().clear();
        classificationCombo.getCheckModel().clearChecks();
        var project = qupath.getProject();
        if (project == null) return;

        // Add "Unclassified" entry first
        classificationCombo.getItems().add("Unclassified");

        var classes = project.getPathClasses();
        for (PathClass pc : classes) {
            if (pc == null || pc == PathClass.NULL_CLASS) continue;
            classificationCombo.getItems().add(pc.toString());
        }

        // Check all items by default
        for (int i = 0; i < classificationCombo.getItems().size(); i++) {
            classificationCombo.getCheckModel().check(i);
        }
    }

    private void updatePreviewButtonState() {
        boolean hasImage = qupath.getViewer() != null
                && qupath.getViewer().getImageData() != null;
        previewButton.setDisable(!hasImage);
    }

    private void wireTooltips() {
        regionTypeCombo.setTooltip(createTooltip("tooltip.rendered.regionType"));
        classificationCombo.setTooltip(createTooltip("tooltip.rendered.classificationFilter"));
        paddingSpinner.setTooltip(createTooltip("tooltip.rendered.padding"));
        modeCombo.setTooltip(createTooltip("tooltip.rendered.mode"));
        displaySettingsCombo.setTooltip(createTooltip("tooltip.rendered.displaySettings"));
        presetNameCombo.setTooltip(createTooltip("tooltip.rendered.preset"));
        classifierCombo.setTooltip(createTooltip("tooltip.rendered.classifier"));
        opacitySlider.setTooltip(createTooltip("tooltip.rendered.opacity"));
        downsampleCombo.setTooltip(createTooltip("tooltip.rendered.downsample"));
        formatCombo.setTooltip(createTooltip("tooltip.rendered.format"));
        includeAnnotationsCheck.setTooltip(createTooltip("tooltip.rendered.includeAnnotations"));
        includeDetectionsCheck.setTooltip(createTooltip("tooltip.rendered.includeDetections"));
        fillAnnotationsCheck.setTooltip(createTooltip("tooltip.rendered.fillAnnotations"));
        showNamesCheck.setTooltip(createTooltip("tooltip.rendered.showNames"));
        showScaleBarCheck.setTooltip(createTooltip("tooltip.rendered.showScaleBar"));
        scaleBarPositionCombo.setTooltip(createTooltip("tooltip.rendered.scaleBarPosition"));
        scaleBarColorPicker.setTooltip(createTooltip("tooltip.rendered.scaleBarColor"));
        scaleBarFontSizeSpinner.setTooltip(createTooltip("tooltip.rendered.scaleBarFontSize"));
        scaleBarBoldCheck.setTooltip(createTooltip("tooltip.rendered.scaleBarBold"));
        previewButton.setTooltip(createTooltip("tooltip.rendered.previewImage"));
        densityMapCombo.setTooltip(createTooltip("tooltip.rendered.densityMap"));
        colormapCombo.setTooltip(createTooltip("tooltip.rendered.colormap"));
        showColorScaleBarCheck.setTooltip(createTooltip("tooltip.rendered.showColorScaleBar"));
        colorScaleBarPositionCombo.setTooltip(createTooltip("tooltip.rendered.colorScaleBarPosition"));
        colorScaleBarFontSizeSpinner.setTooltip(createTooltip("tooltip.rendered.colorScaleBarFontSize"));
        colorScaleBarBoldCheck.setTooltip(createTooltip("tooltip.rendered.colorScaleBarBold"));
    }

    private static Tooltip createTooltip(String key) {
        var tip = new Tooltip(resources.getString(key));
        tip.setWrapText(true);
        tip.setMaxWidth(400);
        tip.setShowDuration(javafx.util.Duration.seconds(30));
        return tip;
    }

    private void populateClassifiers() {
        classifierCombo.getItems().clear();
        var project = qupath.getProject();
        if (project == null) return;
        try {
            var names = project.getPixelClassifiers().getNames();
            classifierCombo.getItems().addAll(names);
            if (!names.isEmpty()) {
                String saved = QuietPreferences.getRenderedClassifierName();
                if (saved != null && names.contains(saved)) {
                    classifierCombo.setValue(saved);
                } else {
                    classifierCombo.getSelectionModel().selectFirst();
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load pixel classifier names", e);
        }
    }

    private void populatePresets() {
        presetNameCombo.getItems().clear();
        var project = qupath.getProject();
        if (project == null) return;
        try {
            var manager = DisplaySettingUtils.getResourcesForProject(project);
            var names = manager.getNames();
            presetNameCombo.getItems().addAll(names);
            if (!names.isEmpty()) {
                String saved = QuietPreferences.getRenderedDisplayPresetName();
                if (saved != null && names.contains(saved)) {
                    presetNameCombo.setValue(saved);
                } else {
                    presetNameCombo.getSelectionModel().selectFirst();
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load display setting presets", e);
        }
    }

    private void populateDensityMaps() {
        densityMapCombo.getItems().clear();
        var project = qupath.getProject();
        if (project == null) return;
        try {
            var resources = project.getResources(
                    DensityMaps.PROJECT_LOCATION, DensityMapBuilder.class, "json");
            var names = resources.getNames();
            densityMapCombo.getItems().addAll(names);
            if (!names.isEmpty()) {
                String saved = QuietPreferences.getRenderedDensityMapName();
                if (saved != null && names.contains(saved)) {
                    densityMapCombo.setValue(saved);
                } else {
                    densityMapCombo.getSelectionModel().selectFirst();
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load density map names", e);
        }
    }

    private void restorePreferences() {
        String savedRegionType = QuietPreferences.getRenderedRegionType();
        try {
            regionTypeCombo.setValue(RenderedExportConfig.RegionType.valueOf(savedRegionType));
        } catch (IllegalArgumentException e) { /* keep default */ }
        updateRegionTypeVisibility(regionTypeCombo.getValue());

        paddingSpinner.getValueFactory().setValue(QuietPreferences.getRenderedPadding());

        String savedMode = QuietPreferences.getRenderedMode();
        try {
            modeCombo.setValue(RenderedExportConfig.RenderMode.valueOf(savedMode));
        } catch (IllegalArgumentException e) { /* keep default */ }

        String savedDisplayMode = QuietPreferences.getRenderedDisplayMode();
        try {
            displaySettingsCombo.setValue(DisplaySettingsMode.valueOf(savedDisplayMode));
        } catch (IllegalArgumentException e) { /* keep default */ }
        updateDisplaySettingsVisibility(displaySettingsCombo.getValue());

        opacitySlider.setValue(QuietPreferences.getRenderedOpacity());
        opacityValueLabel.setText(String.format("%.2f", QuietPreferences.getRenderedOpacity()));

        double savedDs = QuietPreferences.getRenderedDownsample();
        if (savedDs >= 1.0) downsampleCombo.setValue(savedDs);

        String savedFormat = QuietPreferences.getRenderedFormat();
        try { formatCombo.setValue(OutputFormat.valueOf(savedFormat)); }
        catch (IllegalArgumentException e) { /* keep default */ }

        includeAnnotationsCheck.setSelected(QuietPreferences.isRenderedIncludeAnnotations());
        includeDetectionsCheck.setSelected(QuietPreferences.isRenderedIncludeDetections());
        fillAnnotationsCheck.setSelected(QuietPreferences.isRenderedFillAnnotations());
        showNamesCheck.setSelected(QuietPreferences.isRenderedShowNames());

        showScaleBarCheck.setSelected(QuietPreferences.isRenderedShowScaleBar());
        try {
            scaleBarPositionCombo.setValue(
                    ScaleBarRenderer.Position.valueOf(QuietPreferences.getRenderedScaleBarPosition()));
        } catch (IllegalArgumentException e) { /* keep default */ }

        // Restore scale bar color from hex string (handle legacy enum names gracefully)
        String savedColor = QuietPreferences.getRenderedScaleBarColor();
        scaleBarColorPicker.setValue(hexToFxColor(savedColor));

        scaleBarFontSizeSpinner.getValueFactory().setValue(QuietPreferences.getRenderedScaleBarFontSize());
        scaleBarBoldCheck.setSelected(QuietPreferences.isRenderedScaleBarBold());

        updateScaleBarVisibility(showScaleBarCheck.isSelected());

        // Density map preferences
        String savedColormap = QuietPreferences.getRenderedColormapName();
        if (savedColormap != null && !savedColormap.isBlank()) {
            colormapCombo.setValue(savedColormap);
        }

        // Color scale bar preferences
        showColorScaleBarCheck.setSelected(QuietPreferences.isRenderedShowColorScaleBar());
        try {
            colorScaleBarPositionCombo.setValue(
                    ScaleBarRenderer.Position.valueOf(QuietPreferences.getRenderedColorScaleBarPosition()));
        } catch (IllegalArgumentException e) { /* keep default */ }
        colorScaleBarFontSizeSpinner.getValueFactory().setValue(QuietPreferences.getRenderedColorScaleBarFontSize());
        colorScaleBarBoldCheck.setSelected(QuietPreferences.isRenderedColorScaleBarBold());
        updateColorScaleBarVisibility(showColorScaleBarCheck.isSelected());
    }

    /**
     * Save current UI state to persistent preferences.
     */
    public void savePreferences() {
        var regionType = regionTypeCombo.getValue();
        if (regionType != null) QuietPreferences.setRenderedRegionType(regionType.name());
        QuietPreferences.setRenderedPadding(
                paddingSpinner.getValue() != null ? paddingSpinner.getValue() : 0);
        var mode = modeCombo.getValue();
        if (mode != null) QuietPreferences.setRenderedMode(mode.name());
        var dsMode = displaySettingsCombo.getValue();
        if (dsMode != null) QuietPreferences.setRenderedDisplayMode(dsMode.name());
        var preset = presetNameCombo.getValue();
        if (preset != null) QuietPreferences.setRenderedDisplayPresetName(preset);
        var classifier = classifierCombo.getValue();
        if (classifier != null) QuietPreferences.setRenderedClassifierName(classifier);
        QuietPreferences.setRenderedOpacity(opacitySlider.getValue());
        Double ds = downsampleCombo.getValue();
        if (ds != null) QuietPreferences.setRenderedDownsample(ds);
        var fmt = formatCombo.getValue();
        if (fmt != null) QuietPreferences.setRenderedFormat(fmt.name());
        QuietPreferences.setRenderedIncludeAnnotations(includeAnnotationsCheck.isSelected());
        QuietPreferences.setRenderedIncludeDetections(includeDetectionsCheck.isSelected());
        QuietPreferences.setRenderedFillAnnotations(fillAnnotationsCheck.isSelected());
        QuietPreferences.setRenderedShowNames(showNamesCheck.isSelected());
        QuietPreferences.setRenderedShowScaleBar(showScaleBarCheck.isSelected());
        var sbPos = scaleBarPositionCombo.getValue();
        if (sbPos != null) QuietPreferences.setRenderedScaleBarPosition(sbPos.name());
        QuietPreferences.setRenderedScaleBarColor(fxColorToHex(scaleBarColorPicker.getValue()));
        QuietPreferences.setRenderedScaleBarFontSize(
                scaleBarFontSizeSpinner.getValue() != null ? scaleBarFontSizeSpinner.getValue() : 0);
        QuietPreferences.setRenderedScaleBarBold(scaleBarBoldCheck.isSelected());
        var densityMap = densityMapCombo.getValue();
        if (densityMap != null) QuietPreferences.setRenderedDensityMapName(densityMap);
        var colormap = colormapCombo.getValue();
        if (colormap != null) QuietPreferences.setRenderedColormapName(colormap);
        QuietPreferences.setRenderedShowColorScaleBar(showColorScaleBarCheck.isSelected());
        var csPos = colorScaleBarPositionCombo.getValue();
        if (csPos != null) QuietPreferences.setRenderedColorScaleBarPosition(csPos.name());
        QuietPreferences.setRenderedColorScaleBarFontSize(
                colorScaleBarFontSizeSpinner.getValue() != null ? colorScaleBarFontSizeSpinner.getValue() : 0);
        QuietPreferences.setRenderedColorScaleBarBold(colorScaleBarBoldCheck.isSelected());
    }

    /**
     * Build a RenderedExportConfig from current UI state.
     *
     * @param outputDir the output directory
     * @return the config, or null if validation fails
     */
    public RenderedExportConfig buildConfig(File outputDir) {
        var dsMode = displaySettingsCombo.getValue() != null
                ? displaySettingsCombo.getValue()
                : DisplaySettingsMode.PER_IMAGE_SAVED;

        // Collect selected classifications from CheckComboBox
        List<String> selectedClassifications = null;
        var currentRegionType = regionTypeCombo.getValue() != null
                ? regionTypeCombo.getValue()
                : RenderedExportConfig.RegionType.WHOLE_IMAGE;
        if (currentRegionType == RenderedExportConfig.RegionType.ALL_ANNOTATIONS) {
            selectedClassifications = new ArrayList<>(
                    classificationCombo.getCheckModel().getCheckedItems());
        }

        var builder = new RenderedExportConfig.Builder()
                .regionType(currentRegionType)
                .selectedClassifications(selectedClassifications)
                .paddingPixels(paddingSpinner.getValue() != null ? paddingSpinner.getValue() : 0)
                .renderMode(modeCombo.getValue())
                .displaySettingsMode(dsMode)
                .overlayOpacity(opacitySlider.getValue())
                .downsample(downsampleCombo.getValue() != null ? downsampleCombo.getValue() : 4.0)
                .format(formatCombo.getValue())
                .outputDirectory(outputDir)
                .includeAnnotations(includeAnnotationsCheck.isSelected())
                .includeDetections(includeDetectionsCheck.isSelected())
                .fillAnnotations(fillAnnotationsCheck.isSelected())
                .showNames(showNamesCheck.isSelected())
                .showScaleBar(showScaleBarCheck.isSelected())
                .scaleBarPosition(scaleBarPositionCombo.getValue() != null
                        ? scaleBarPositionCombo.getValue()
                        : ScaleBarRenderer.Position.LOWER_RIGHT)
                .scaleBarColorHex(fxColorToHex(scaleBarColorPicker.getValue()))
                .scaleBarFontSize(scaleBarFontSizeSpinner.getValue() != null
                        ? scaleBarFontSizeSpinner.getValue() : 0)
                .scaleBarBoldText(scaleBarBoldCheck.isSelected());

        builder.densityMapName(densityMapCombo.getValue())
                .colormapName(colormapCombo.getValue())
                .showColorScaleBar(showColorScaleBarCheck.isSelected())
                .colorScaleBarPosition(colorScaleBarPositionCombo.getValue() != null
                        ? colorScaleBarPositionCombo.getValue()
                        : ScaleBarRenderer.Position.LOWER_RIGHT)
                .colorScaleBarFontSize(colorScaleBarFontSizeSpinner.getValue() != null
                        ? colorScaleBarFontSizeSpinner.getValue() : 0)
                .colorScaleBarBoldText(colorScaleBarBoldCheck.isSelected());

        if (modeCombo.getValue() == RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY) {
            builder.classifierName(classifierCombo.getValue());
        }

        // Capture display settings based on selected mode
        if (dsMode == DisplaySettingsMode.CURRENT_VIEWER) {
            var viewer = qupath.getViewer();
            if (viewer != null && viewer.getImageDisplay() != null) {
                var settings = DisplaySettingUtils.displayToSettings(
                        viewer.getImageDisplay(), "export");
                builder.capturedDisplaySettings(settings);
            }
        } else if (dsMode == DisplaySettingsMode.SAVED_PRESET) {
            String presetName = presetNameCombo.getValue();
            builder.displayPresetName(presetName);
            if (presetName != null && !presetName.isBlank()) {
                resolvePresetSettings(presetName, builder);
            }
        }

        return builder.build();
    }

    private void resolvePresetSettings(String presetName, RenderedExportConfig.Builder builder) {
        var project = qupath.getProject();
        if (project == null) return;
        try {
            var manager = DisplaySettingUtils.getResourcesForProject(project);
            var settings = manager.get(presetName);
            if (settings != null) {
                builder.capturedDisplaySettings(settings);
            } else {
                logger.warn("Display preset not found: {}", presetName);
            }
        } catch (IOException e) {
            logger.error("Failed to load display preset: {}", presetName, e);
        }
    }

    /**
     * Handle the preview button click.
     * Renders the current image with current settings in a background thread
     * and shows the result in a popup window.
     */
    private void handlePreview() {
        var viewer = qupath.getViewer();
        if (viewer == null || viewer.getImageData() == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        ImageData<BufferedImage> imageData = (ImageData<BufferedImage>) viewer.getImageData();

        // Build config with a temp dir placeholder (not used for preview)
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        RenderedExportConfig config;
        try {
            config = buildConfig(tempDir);
        } catch (Exception e) {
            logger.error("Failed to build config for preview", e);
            return;
        }

        // Load classifier or density map builder if needed
        PixelClassifier classifier = null;
        DensityMapBuilder densityBuilder = null;
        if (config.getRenderMode() == RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY) {
            String classifierName = config.getClassifierName();
            if (classifierName == null || classifierName.isBlank()) {
                logger.warn("No classifier selected for preview");
                return;
            }
            var project = qupath.getProject();
            if (project != null) {
                try {
                    classifier = project.getPixelClassifiers().get(classifierName);
                } catch (Exception e) {
                    logger.error("Failed to load classifier for preview: {}", classifierName, e);
                    return;
                }
            }
        } else if (config.getRenderMode() == RenderedExportConfig.RenderMode.DENSITY_MAP_OVERLAY) {
            String dmName = config.getDensityMapName();
            if (dmName == null || dmName.isBlank()) {
                logger.warn("No density map selected for preview");
                return;
            }
            var project = qupath.getProject();
            if (project != null) {
                try {
                    var dmResources = project.getResources(
                            DensityMaps.PROJECT_LOCATION, DensityMapBuilder.class, "json");
                    densityBuilder = dmResources.get(dmName);
                } catch (Exception e) {
                    logger.error("Failed to load density map for preview: {}", dmName, e);
                    return;
                }
            }
        }

        // Show progress indicator
        var progressStage = new Stage();
        progressStage.setTitle("Rendering Preview...");
        var progressIndicator = new ProgressIndicator(-1);
        progressIndicator.setPrefSize(80, 80);
        var progressPane = new StackPane(progressIndicator);
        progressPane.setPadding(new Insets(20));
        progressStage.setScene(new Scene(progressPane));
        progressStage.setResizable(false);
        progressStage.show();

        final PixelClassifier finalClassifier = classifier;
        final DensityMapBuilder finalDensityBuilder = densityBuilder;

        Thread previewThread = new Thread(() -> {
            try {
                BufferedImage preview = RenderedImageExporter.renderPreview(
                        imageData, finalClassifier, finalDensityBuilder, config, 800);

                Platform.runLater(() -> {
                    progressStage.close();
                    showPreviewWindow(preview);
                });
            } catch (Exception e) {
                logger.error("Preview rendering failed", e);
                Platform.runLater(progressStage::close);
            }
        });
        previewThread.setDaemon(true);
        previewThread.setName("quiet-preview");
        previewThread.start();
    }

    private void showPreviewWindow(BufferedImage preview) {
        var fxImage = SwingFXUtils.toFXImage(preview, null);
        var imageView = new ImageView(fxImage);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(800);
        imageView.setFitHeight(600);

        var pane = new StackPane(imageView);
        pane.setPadding(new Insets(5));

        var stage = new Stage();
        stage.setTitle("Export Preview");
        stage.setScene(new Scene(pane));
        stage.setResizable(true);
        stage.show();
    }

    public String getClassifierName() {
        return classifierCombo.getValue();
    }

    public RenderedExportConfig.RenderMode getRenderMode() {
        return modeCombo.getValue();
    }

    public String getDensityMapName() {
        return densityMapCombo.getValue();
    }

    /**
     * Convert a JavaFX Color to a hex string like "#RRGGBB".
     */
    private static String fxColorToHex(javafx.scene.paint.Color color) {
        if (color == null) return "#FFFFFF";
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     * Convert a hex string to a JavaFX Color.
     * Handles legacy enum names ("WHITE", "BLACK") gracefully.
     */
    private static javafx.scene.paint.Color hexToFxColor(String hex) {
        if (hex == null || hex.isBlank()) return javafx.scene.paint.Color.WHITE;
        // Handle legacy enum names from old preferences
        if ("WHITE".equalsIgnoreCase(hex)) return javafx.scene.paint.Color.WHITE;
        if ("BLACK".equalsIgnoreCase(hex)) return javafx.scene.paint.Color.BLACK;
        try {
            return javafx.scene.paint.Color.web(hex);
        } catch (IllegalArgumentException e) {
            return javafx.scene.paint.Color.WHITE;
        }
    }
}
