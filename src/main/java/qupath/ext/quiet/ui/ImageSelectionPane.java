package qupath.ext.quiet.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import qupath.ext.quiet.export.ExportCategory;
import qupath.ext.quiet.preferences.QuietPreferences;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.projects.ProjectImageEntry;

/**
 * Step 3 of the export wizard: Select images, output directory, and run export.
 * <p>
 * Shared across all export categories.
 */
public class ImageSelectionPane extends VBox {

    private static final ResourceBundle resources =
            ResourceBundle.getBundle("qupath.ext.quiet.ui.strings");

    private final QuPathGUI qupath;
    private final Stage ownerStage;

    private TextField outputDirField;
    private ListView<ImageEntryItem> imageListView;
    private Label imageCountLabel;
    private CheckBox addToWorkflowCheck;
    private CheckBox exportGeoJsonCheck;
    private ProgressBar progressBar;
    private Label statusLabel;

    public ImageSelectionPane(QuPathGUI qupath, Stage ownerStage) {
        this.qupath = qupath;
        this.ownerStage = ownerStage;
        setSpacing(10);
        setPadding(new Insets(10));
        buildUI();
        populateImageList();
    }

    private void buildUI() {
        var header = new Label(resources.getString("wizard.step3.title"));
        header.setFont(Font.font(null, FontWeight.BOLD, 14));

        // Output directory
        var dirLabel = new Label(resources.getString("step3.label.outputDir"));
        outputDirField = new TextField();
        outputDirField.setPromptText("Select output folder...");
        outputDirField.setTooltip(createTooltip("tooltip.step3.outputDir"));
        HBox.setHgrow(outputDirField, Priority.ALWAYS);

        var browseButton = new Button(resources.getString("button.browse"));
        browseButton.setOnAction(e -> browseOutputDirectory());
        browseButton.setTooltip(createTooltip("tooltip.step3.browse"));

        var defaultButton = new Button(resources.getString("button.useProjectDefault"));
        defaultButton.setOnAction(e -> setProjectDefaultDir());
        defaultButton.setTooltip(createTooltip("tooltip.step3.useProjectDefault"));

        var dirBox = new HBox(5, outputDirField, browseButton, defaultButton);
        HBox.setHgrow(outputDirField, Priority.ALWAYS);

        // Image list
        var imagesLabel = new Label(resources.getString("step3.label.imagesToExport"));

        var selectAllButton = new Button(resources.getString("button.selectAll"));
        selectAllButton.setOnAction(e -> setAllImagesSelected(true));
        var deselectAllButton = new Button(resources.getString("button.deselectAll"));
        deselectAllButton.setOnAction(e -> setAllImagesSelected(false));
        imageCountLabel = new Label();

        var selectionButtons = new HBox(5, selectAllButton, deselectAllButton, imageCountLabel);
        selectionButtons.setAlignment(Pos.CENTER_LEFT);

        var imagesHeader = new HBox(10, imagesLabel, selectionButtons);
        imagesHeader.setAlignment(Pos.CENTER_LEFT);

        imageListView = new ListView<>();
        imageListView.setPrefHeight(200);
        imageListView.setCellFactory(lv ->
                new CheckBoxListCell<>(ImageEntryItem::selectedProperty));
        VBox.setVgrow(imageListView, Priority.ALWAYS);

        // Script actions
        var copyScriptButton = new Button(resources.getString("button.copyScript"));
        copyScriptButton.setOnAction(e -> {
            if (scriptCopyHandler != null) scriptCopyHandler.run();
        });
        copyScriptButton.setTooltip(createTooltip("tooltip.step3.copyScript"));
        var saveScriptButton = new Button(resources.getString("button.saveScript"));
        saveScriptButton.setOnAction(e -> {
            if (scriptSaveHandler != null) scriptSaveHandler.run();
        });
        saveScriptButton.setTooltip(createTooltip("tooltip.step3.saveScript"));
        var scriptBox = new HBox(10, copyScriptButton, saveScriptButton);

        // Workflow checkbox
        addToWorkflowCheck = new CheckBox(resources.getString("step3.label.addToWorkflow"));
        addToWorkflowCheck.setSelected(QuietPreferences.isAddToWorkflow());
        addToWorkflowCheck.setTooltip(createTooltip("tooltip.step3.addToWorkflow"));

        // GeoJSON checkbox
        exportGeoJsonCheck = new CheckBox(resources.getString("step3.label.exportGeoJson"));
        exportGeoJsonCheck.setSelected(QuietPreferences.isExportGeoJson());
        exportGeoJsonCheck.setTooltip(createTooltip("tooltip.step3.exportGeoJson"));

        // Progress
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        statusLabel = new Label();
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(
                header,
                dirLabel, dirBox,
                imagesHeader,
                imageListView,
                scriptBox,
                addToWorkflowCheck,
                exportGeoJsonCheck,
                progressBar,
                statusLabel
        );
    }

    private void populateImageList() {
        var project = qupath.getProject();
        if (project == null) return;

        ObservableList<ImageEntryItem> items = FXCollections.observableArrayList();
        for (var entry : project.getImageList()) {
            items.add(new ImageEntryItem(entry, true));
        }
        imageListView.setItems(items);
        updateImageCount();

        // Track selection changes for count label
        for (var item : items) {
            item.selectedProperty().addListener((obs, oldVal, newVal) -> updateImageCount());
        }
    }

    private void updateImageCount() {
        long count = imageListView.getItems().stream()
                .filter(ImageEntryItem::isSelected).count();
        imageCountLabel.setText(String.format(resources.getString("step3.label.imageCount"), (int) count));
    }

    private void setAllImagesSelected(boolean selected) {
        for (var item : imageListView.getItems()) {
            item.setSelected(selected);
        }
        updateImageCount();
    }

    private void browseOutputDirectory() {
        var chooser = new DirectoryChooser();
        chooser.setTitle(resources.getString("step3.label.outputDir"));

        String current = outputDirField.getText();
        if (current != null && !current.isEmpty()) {
            File dir = new File(current);
            if (dir.isDirectory()) {
                chooser.setInitialDirectory(dir);
            }
        }

        File selected = chooser.showDialog(ownerStage);
        if (selected != null) {
            outputDirField.setText(selected.getAbsolutePath());
        }
    }

    private void setProjectDefaultDir() {
        var project = qupath.getProject();
        if (project == null) return;

        var projectDir = project.getPath().getParent();
        if (projectDir != null) {
            // Will be combined with category subdirectory by the wizard
            outputDirField.setText(projectDir.toFile().getAbsolutePath());
        }
    }

    /**
     * Set the output directory field to the category-specific default.
     */
    public void setDefaultOutputDir(ExportCategory category) {
        var project = qupath.getProject();
        if (project == null) return;

        var projectDir = project.getPath().getParent();
        if (projectDir != null) {
            File defaultDir = category.getDefaultOutputDir(projectDir.toFile());
            outputDirField.setText(defaultDir.getAbsolutePath());
        }
    }

    // --- Public accessors ---

    public File getOutputDirectory() {
        String path = outputDirField.getText();
        if (path == null || path.isEmpty()) return null;
        return new File(path);
    }

    public void setOutputDirectory(String path) {
        outputDirField.setText(path);
    }

    public List<ProjectImageEntry<BufferedImage>> getSelectedEntries() {
        List<ProjectImageEntry<BufferedImage>> selected = new ArrayList<>();
        for (var item : imageListView.getItems()) {
            if (item.isSelected()) {
                selected.add(item.getEntry());
            }
        }
        return selected;
    }

    public boolean isAddToWorkflow() {
        return addToWorkflowCheck.isSelected();
    }

    public boolean isExportGeoJson() {
        return exportGeoJsonCheck.isSelected();
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public Label getStatusLabel() {
        return statusLabel;
    }

    public void copyScriptToClipboard(String script) {
        var content = new ClipboardContent();
        content.putString(script);
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("Script copied to clipboard.");
    }

    public void saveScriptToFile(String script) {
        var chooser = new FileChooser();
        chooser.setTitle("Save Groovy Script");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Groovy Scripts", "*.groovy"));
        chooser.setInitialFileName("export_script.groovy");

        File file = chooser.showSaveDialog(ownerStage);
        if (file != null) {
            try {
                java.nio.file.Files.writeString(file.toPath(), script);
                statusLabel.setText("Script saved to: " + file.getAbsolutePath());
            } catch (Exception e) {
                statusLabel.setText("Failed to save script: " + e.getMessage());
            }
        }
    }

    private static Tooltip createTooltip(String key) {
        var tip = new Tooltip(resources.getString(key));
        tip.setWrapText(true);
        tip.setMaxWidth(400);
        tip.setShowDuration(javafx.util.Duration.seconds(30));
        return tip;
    }

    // Script action handlers - set by ExportWizard
    private Runnable scriptCopyHandler;
    private Runnable scriptSaveHandler;

    public void setScriptCopyHandler(Runnable handler) {
        this.scriptCopyHandler = handler;
    }

    public void setScriptSaveHandler(Runnable handler) {
        this.scriptSaveHandler = handler;
    }
}
