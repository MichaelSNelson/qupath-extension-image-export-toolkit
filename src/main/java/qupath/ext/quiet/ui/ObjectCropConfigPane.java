package qupath.ext.quiet.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

import qupath.ext.quiet.export.ObjectCropConfig;
import qupath.ext.quiet.export.OutputFormat;
import qupath.ext.quiet.preferences.QuietPreferences;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.objects.classes.PathClass;

/**
 * Step 2 config pane for Object Crops export category.
 */
public class ObjectCropConfigPane extends VBox {

    private static final ResourceBundle resources =
            ResourceBundle.getBundle("qupath.ext.quiet.ui.strings");

    private final QuPathGUI qupath;

    private ComboBox<ObjectCropConfig.ObjectType> objectTypeCombo;
    private Spinner<Integer> cropSizeSpinner;
    private Spinner<Integer> paddingSpinner;
    private ComboBox<Double> downsampleCombo;
    private ComboBox<ObjectCropConfig.LabelFormat> labelFormatCombo;
    private ComboBox<OutputFormat> formatCombo;
    private ListView<TiledConfigPane.ClassificationItem> classificationList;

    public ObjectCropConfigPane(QuPathGUI qupath) {
        this.qupath = qupath;
        setSpacing(10);
        setPadding(new Insets(10));
        buildUI();
        populateClassifications();
        restorePreferences();
    }

    private void buildUI() {
        var header = new Label(resources.getString("wizard.step2.title") + " - " +
                resources.getString("step2.objectCrops.title"));
        header.setFont(Font.font(null, FontWeight.BOLD, 14));

        var grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        int row = 0;

        // Object type
        grid.add(new Label(resources.getString("objectCrops.label.objectType")), 0, row);
        objectTypeCombo = new ComboBox<>(FXCollections.observableArrayList(
                ObjectCropConfig.ObjectType.values()));
        objectTypeCombo.setValue(ObjectCropConfig.ObjectType.DETECTIONS);
        objectTypeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ObjectCropConfig.ObjectType type) {
                if (type == null) return "";
                return switch (type) {
                    case DETECTIONS -> resources.getString("objectCrops.objectType.detections");
                    case CELLS -> resources.getString("objectCrops.objectType.cells");
                    case ALL -> resources.getString("objectCrops.objectType.all");
                };
            }
            @Override
            public ObjectCropConfig.ObjectType fromString(String s) {
                return ObjectCropConfig.ObjectType.DETECTIONS;
            }
        });
        grid.add(objectTypeCombo, 1, row);
        row++;

        // Crop size
        grid.add(new Label(resources.getString("objectCrops.label.cropSize")), 0, row);
        cropSizeSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                16, 512, 64, 16));
        cropSizeSpinner.setEditable(true);
        cropSizeSpinner.setPrefWidth(100);
        grid.add(cropSizeSpinner, 1, row);
        row++;

        // Padding
        grid.add(new Label(resources.getString("objectCrops.label.cropPadding")), 0, row);
        paddingSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, 128, 0, 4));
        paddingSpinner.setEditable(true);
        paddingSpinner.setPrefWidth(100);
        grid.add(paddingSpinner, 1, row);
        row++;

        // Downsample
        grid.add(new Label(resources.getString("objectCrops.label.cropDownsample")), 0, row);
        downsampleCombo = new ComboBox<>(FXCollections.observableArrayList(
                1.0, 2.0, 4.0, 8.0));
        downsampleCombo.setEditable(true);
        downsampleCombo.setValue(1.0);
        downsampleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Double value) {
                if (value == null) return "";
                return value == Math.floor(value)
                        ? String.valueOf(value.intValue()) : String.valueOf(value);
            }
            @Override
            public Double fromString(String string) {
                try { return Double.parseDouble(string); }
                catch (NumberFormatException e) { return 1.0; }
            }
        });
        grid.add(downsampleCombo, 1, row);
        row++;

        // Label format
        grid.add(new Label(resources.getString("objectCrops.label.labelFormat")), 0, row);
        labelFormatCombo = new ComboBox<>(FXCollections.observableArrayList(
                ObjectCropConfig.LabelFormat.values()));
        labelFormatCombo.setValue(ObjectCropConfig.LabelFormat.SUBDIRECTORY);
        labelFormatCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ObjectCropConfig.LabelFormat fmt) {
                if (fmt == null) return "";
                return switch (fmt) {
                    case SUBDIRECTORY -> resources.getString("objectCrops.labelFormat.subdirectory");
                    case FILENAME_PREFIX -> resources.getString("objectCrops.labelFormat.filenamePrefix");
                };
            }
            @Override
            public ObjectCropConfig.LabelFormat fromString(String s) {
                return ObjectCropConfig.LabelFormat.SUBDIRECTORY;
            }
        });
        grid.add(labelFormatCombo, 1, row);
        row++;

        // Output format
        grid.add(new Label(resources.getString("objectCrops.label.format")), 0, row);
        formatCombo = new ComboBox<>(FXCollections.observableArrayList(
                OutputFormat.PNG, OutputFormat.TIFF, OutputFormat.JPEG));
        formatCombo.setValue(OutputFormat.PNG);
        grid.add(formatCombo, 1, row);
        row++;

        // Section 1: Crop Settings
        var cropSettingsSection = SectionBuilder.createSection(
                resources.getString("objectCrops.section.cropSettings"), true, grid);

        // Section 2: Classifications
        var classLabel = new Label(resources.getString("objectCrops.label.classifications"));
        classificationList = new ListView<>();
        classificationList.setPrefHeight(120);
        classificationList.setCellFactory(lv ->
                new CheckBoxListCell<>(TiledConfigPane.ClassificationItem::selectedProperty));

        var selectAllBtn = new javafx.scene.control.Button(resources.getString("button.selectAll"));
        selectAllBtn.setOnAction(e -> classificationList.getItems().forEach(it -> it.setSelected(true)));
        var selectNoneBtn = new javafx.scene.control.Button(resources.getString("button.deselectAll"));
        selectNoneBtn.setOnAction(e -> classificationList.getItems().forEach(it -> it.setSelected(false)));
        var selectionBtns = new HBox(5, selectAllBtn, selectNoneBtn);

        var classBox = new VBox(5, classLabel, classificationList, selectionBtns);
        VBox.setVgrow(classificationList, Priority.ALWAYS);

        var classificationsSection = SectionBuilder.createSection(
                resources.getString("objectCrops.section.classifications"), true, classBox);

        getChildren().addAll(header, cropSettingsSection, classificationsSection);
        VBox.setVgrow(classificationsSection, Priority.ALWAYS);
        wireTooltips();
    }

    private void wireTooltips() {
        objectTypeCombo.setTooltip(createTooltip("tooltip.objectCrops.objectType"));
        cropSizeSpinner.setTooltip(createTooltip("tooltip.objectCrops.cropSize"));
        paddingSpinner.setTooltip(createTooltip("tooltip.objectCrops.cropPadding"));
        downsampleCombo.setTooltip(createTooltip("tooltip.objectCrops.cropDownsample"));
        labelFormatCombo.setTooltip(createTooltip("tooltip.objectCrops.labelFormat"));
        formatCombo.setTooltip(createTooltip("tooltip.objectCrops.format"));
        classificationList.setTooltip(createTooltip("tooltip.objectCrops.classifications"));
    }

    private static Tooltip createTooltip(String key) {
        var tip = new Tooltip(resources.getString(key));
        tip.setWrapText(true);
        tip.setMaxWidth(400);
        tip.setShowDuration(javafx.util.Duration.seconds(30));
        return tip;
    }

    private void populateClassifications() {
        classificationList.getItems().clear();
        var project = qupath.getProject();
        if (project == null) return;

        var classes = project.getPathClasses();
        for (PathClass pc : classes) {
            if (pc == null || pc == PathClass.NULL_CLASS) continue;
            classificationList.getItems().add(
                    new TiledConfigPane.ClassificationItem(pc.toString(), true));
        }
    }

    private void restorePreferences() {
        String savedType = QuietPreferences.getObjectCropType();
        try { objectTypeCombo.setValue(ObjectCropConfig.ObjectType.valueOf(savedType)); }
        catch (IllegalArgumentException e) { /* keep default */ }

        cropSizeSpinner.getValueFactory().setValue(QuietPreferences.getObjectCropSize());
        paddingSpinner.getValueFactory().setValue(QuietPreferences.getObjectCropPadding());

        double savedDs = QuietPreferences.getObjectCropDownsample();
        if (savedDs >= 1.0) downsampleCombo.setValue(savedDs);

        String savedLabelFmt = QuietPreferences.getObjectCropLabelFormat();
        try { labelFormatCombo.setValue(ObjectCropConfig.LabelFormat.valueOf(savedLabelFmt)); }
        catch (IllegalArgumentException e) { /* keep default */ }

        String savedFmt = QuietPreferences.getObjectCropFormat();
        try { formatCombo.setValue(OutputFormat.valueOf(savedFmt)); }
        catch (IllegalArgumentException e) { /* keep default */ }
    }

    /**
     * Save current UI state to persistent preferences.
     */
    public void savePreferences() {
        var objType = objectTypeCombo.getValue();
        if (objType != null) QuietPreferences.setObjectCropType(objType.name());
        QuietPreferences.setObjectCropSize(cropSizeSpinner.getValue());
        QuietPreferences.setObjectCropPadding(paddingSpinner.getValue());
        Double ds = downsampleCombo.getValue();
        if (ds != null) QuietPreferences.setObjectCropDownsample(ds);
        var labelFmt = labelFormatCombo.getValue();
        if (labelFmt != null) QuietPreferences.setObjectCropLabelFormat(labelFmt.name());
        var fmt = formatCombo.getValue();
        if (fmt != null) QuietPreferences.setObjectCropFormat(fmt.name());
    }

    /**
     * Build an ObjectCropConfig from the current UI state.
     */
    public ObjectCropConfig buildConfig(File outputDir) {
        List<String> selectedClasses = new ArrayList<>();
        for (var item : classificationList.getItems()) {
            if (item.isSelected()) {
                selectedClasses.add(item.getClassName());
            }
        }

        return new ObjectCropConfig.Builder()
                .objectType(objectTypeCombo.getValue())
                .cropSize(cropSizeSpinner.getValue())
                .padding(paddingSpinner.getValue())
                .downsample(downsampleCombo.getValue() != null ? downsampleCombo.getValue() : 1.0)
                .labelFormat(labelFormatCombo.getValue())
                .format(formatCombo.getValue())
                .outputDirectory(outputDir)
                .selectedClasses(selectedClasses)
                .build();
    }
}
