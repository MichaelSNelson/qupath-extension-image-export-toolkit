package qupath.ext.quiet.ui;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

import qupath.ext.quiet.export.OutputFormat;
import qupath.ext.quiet.export.RenderedExportConfig;
import qupath.ext.quiet.preferences.QuietPreferences;
import qupath.lib.gui.QuPathGUI;

/**
 * Step 2a of the export wizard: Configure rendered image export.
 */
public class RenderedConfigPane extends GridPane {

    private static final Logger logger = LoggerFactory.getLogger(RenderedConfigPane.class);
    private static final ResourceBundle resources =
            ResourceBundle.getBundle("qupath.ext.quiet.ui.strings");

    private final QuPathGUI qupath;

    private ComboBox<RenderedExportConfig.RenderMode> modeCombo;
    private ComboBox<String> classifierCombo;
    private Slider opacitySlider;
    private Label opacityValueLabel;
    private ComboBox<Double> downsampleCombo;
    private ComboBox<OutputFormat> formatCombo;
    private CheckBox includeAnnotationsCheck;
    private CheckBox includeDetectionsCheck;
    private CheckBox fillAnnotationsCheck;
    private CheckBox showNamesCheck;

    // Controls needing visibility toggling
    private Label classifierLabel;
    private HBox classifierBox;

    public RenderedConfigPane(QuPathGUI qupath) {
        this.qupath = qupath;
        setHgap(10);
        setVgap(10);
        setPadding(new Insets(10));
        buildUI();
        populateClassifiers();
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
                return mode == RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY
                        ? resources.getString("rendered.mode.classifier")
                        : resources.getString("rendered.mode.object");
            }
            @Override
            public RenderedExportConfig.RenderMode fromString(String s) {
                return RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY;
            }
        });
        add(modeCombo, 1, row);
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

        // Mode switching
        modeCombo.valueProperty().addListener((obs, oldMode, newMode) -> updateModeVisibility(newMode));
        updateModeVisibility(RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY);
    }

    private void updateModeVisibility(RenderedExportConfig.RenderMode mode) {
        boolean isClassifier = (mode == RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY);

        classifierLabel.setVisible(isClassifier);
        classifierLabel.setManaged(isClassifier);
        classifierBox.setVisible(isClassifier);
        classifierBox.setManaged(isClassifier);
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

    private void restorePreferences() {
        String savedMode = QuietPreferences.getRenderedMode();
        try {
            modeCombo.setValue(RenderedExportConfig.RenderMode.valueOf(savedMode));
        } catch (IllegalArgumentException e) { /* keep default */ }

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
    }

    /**
     * Save current UI state to persistent preferences.
     */
    public void savePreferences() {
        var mode = modeCombo.getValue();
        if (mode != null) QuietPreferences.setRenderedMode(mode.name());
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
    }

    /**
     * Build a RenderedExportConfig from current UI state.
     *
     * @param outputDir the output directory
     * @return the config, or null if validation fails
     */
    public RenderedExportConfig buildConfig(File outputDir) {
        var builder = new RenderedExportConfig.Builder()
                .renderMode(modeCombo.getValue())
                .overlayOpacity(opacitySlider.getValue())
                .downsample(downsampleCombo.getValue() != null ? downsampleCombo.getValue() : 4.0)
                .format(formatCombo.getValue())
                .outputDirectory(outputDir)
                .includeAnnotations(includeAnnotationsCheck.isSelected())
                .includeDetections(includeDetectionsCheck.isSelected())
                .fillAnnotations(fillAnnotationsCheck.isSelected())
                .showNames(showNamesCheck.isSelected());

        if (modeCombo.getValue() == RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY) {
            builder.classifierName(classifierCombo.getValue());
        }

        return builder.build();
    }

    public String getClassifierName() {
        return classifierCombo.getValue();
    }

    public RenderedExportConfig.RenderMode getRenderMode() {
        return modeCombo.getValue();
    }
}
