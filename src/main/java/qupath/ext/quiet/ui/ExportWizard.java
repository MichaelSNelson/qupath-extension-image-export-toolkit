package qupath.ext.quiet.ui;

import java.awt.Desktop;
import java.io.File;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import qupath.ext.quiet.export.BatchExportTask;
import qupath.ext.quiet.export.ExportCategory;
import qupath.ext.quiet.export.ExportResult;
import qupath.ext.quiet.export.MaskExportConfig;
import qupath.ext.quiet.export.RawExportConfig;
import qupath.ext.quiet.export.RenderedExportConfig;
import qupath.ext.quiet.export.TiledExportConfig;
import qupath.ext.quiet.preferences.QuietPreferences;
import qupath.ext.quiet.export.ScriptGenerator;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.classifiers.pixel.PixelClassifier;
import qupath.lib.gui.QuPathGUI;

/**
 * Three-step wizard for configuring and running image exports.
 * <p>
 * Step 1: Select export category (Rendered, Mask, Raw)
 * Step 2: Configure export-specific options
 * Step 3: Select images, output directory, run export
 */
public class ExportWizard {

    private static final Logger logger = LoggerFactory.getLogger(ExportWizard.class);
    private static final ResourceBundle resources =
            ResourceBundle.getBundle("qupath.ext.quiet.ui.strings");

    private final QuPathGUI qupath;
    private final Stage stage;
    private final BorderPane root;

    // Wizard steps
    private int currentStep = 1;
    private CategorySelectionPane categoryPane;
    private RenderedConfigPane renderedConfigPane;
    private MaskConfigPane maskConfigPane;
    private RawConfigPane rawConfigPane;
    private TiledConfigPane tiledConfigPane;
    private ImageSelectionPane imageSelectionPane;

    // Navigation buttons
    private Button backButton;
    private Button nextButton;
    private Button cancelButton;
    private Button openFolderButton;

    // Current export state
    private ExportCategory selectedCategory;
    private BatchExportTask currentTask;

    /** Tracks the output directory of the last successful export. */
    private File lastExportDirectory;

    private ExportWizard(QuPathGUI qupath) {
        this.qupath = qupath;
        this.stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(qupath.getStage());
        stage.setTitle(resources.getString("wizard.title"));
        stage.setMinWidth(650);
        stage.setMinHeight(550);

        root = new BorderPane();
        root.setPadding(new Insets(10));

        // Restore wizard size
        stage.setWidth(QuietPreferences.getWizardWidth());
        stage.setHeight(QuietPreferences.getWizardHeight());

        // Save preferences on close
        stage.setOnCloseRequest(e -> {
            saveAllPreferences();
            saveWizardSize();
        });

        buildNavigation();
        initializeSteps();
        showStep(1);

        var scene = new Scene(root);
        stage.setScene(scene);
    }

    /**
     * Show the export wizard.
     *
     * @param qupath the QuPath GUI instance
     */
    public static void showWizard(QuPathGUI qupath) {
        var wizard = new ExportWizard(qupath);
        wizard.stage.show();
    }

    private void buildNavigation() {
        backButton = new Button(resources.getString("button.back"));
        backButton.setOnAction(e -> goBack());

        nextButton = new Button(resources.getString("button.next"));
        nextButton.setDefaultButton(true);
        nextButton.setOnAction(e -> goNext());

        openFolderButton = new Button(resources.getString("button.openResultFolder"));
        openFolderButton.setOnAction(e -> openResultFolder());
        openFolderButton.setVisible(false);
        openFolderButton.setManaged(false);

        cancelButton = new Button(resources.getString("button.cancel"));
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(e -> {
            if (currentTask != null && currentTask.isRunning()) {
                currentTask.cancel();
            } else {
                saveAllPreferences();
                saveWizardSize();
                stage.close();
            }
        });

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var navBar = new HBox(10, backButton, spacer, openFolderButton, cancelButton, nextButton);
        navBar.setAlignment(Pos.CENTER_RIGHT);
        navBar.setPadding(new Insets(10, 0, 0, 0));

        var bottomBox = new javafx.scene.layout.VBox(5, new Separator(), navBar);
        root.setBottom(bottomBox);
    }

    private void initializeSteps() {
        categoryPane = new CategorySelectionPane();

        // Restore last used category
        String lastCat = QuietPreferences.getLastCategory();
        try {
            categoryPane.setSelectedCategory(ExportCategory.valueOf(lastCat));
        } catch (IllegalArgumentException e) {
            // Keep default
        }

        renderedConfigPane = new RenderedConfigPane(qupath);
        maskConfigPane = new MaskConfigPane(qupath);
        rawConfigPane = new RawConfigPane();
        tiledConfigPane = new TiledConfigPane(qupath);
        imageSelectionPane = new ImageSelectionPane(qupath, stage);

        // Wire up script handlers
        imageSelectionPane.setScriptCopyHandler(this::copyScript);
        imageSelectionPane.setScriptSaveHandler(this::saveScript);
    }

    private void showStep(int step) {
        currentStep = step;
        Node centerContent;

        switch (step) {
            case 1 -> centerContent = categoryPane;
            case 2 -> {
                selectedCategory = categoryPane.getSelectedCategory();
                centerContent = switch (selectedCategory) {
                    case RENDERED -> renderedConfigPane;
                    case MASK -> maskConfigPane;
                    case RAW -> rawConfigPane;
                    case TILED -> tiledConfigPane;
                };
            }
            case 3 -> {
                // Set default output dir if empty
                File currentDir = imageSelectionPane.getOutputDirectory();
                if (currentDir == null) {
                    imageSelectionPane.setDefaultOutputDir(selectedCategory);
                }
                centerContent = imageSelectionPane;
            }
            default -> centerContent = categoryPane;
        }

        root.setCenter(centerContent);
        updateNavButtons();
    }

    private void updateNavButtons() {
        backButton.setDisable(currentStep == 1);

        if (currentStep == 3) {
            nextButton.setText(resources.getString("button.finish"));
        } else {
            nextButton.setText(resources.getString("button.next"));
        }
    }

    private void goBack() {
        if (currentStep > 1) {
            showStep(currentStep - 1);
        }
    }

    private void goNext() {
        if (currentStep < 3) {
            showStep(currentStep + 1);
        } else {
            // Step 3: Execute export
            startExport();
        }
    }

    private void startExport() {
        // Validate output directory
        File outputDir = imageSelectionPane.getOutputDirectory();
        if (outputDir == null) {
            Dialogs.showWarningNotification(
                    resources.getString("name"),
                    resources.getString("error.invalidDir"));
            return;
        }
        if (!outputDir.isDirectory()) {
            if (!outputDir.mkdirs()) {
                Dialogs.showWarningNotification(
                        resources.getString("name"),
                        resources.getString("error.invalidDir"));
                return;
            }
        }

        // Validate image selection
        var selectedEntries = imageSelectionPane.getSelectedEntries();
        if (selectedEntries.isEmpty()) {
            Dialogs.showWarningNotification(
                    resources.getString("name"),
                    resources.getString("error.noImages"));
            return;
        }

        boolean addToWorkflow = imageSelectionPane.isAddToWorkflow();
        boolean exportGeoJson = imageSelectionPane.isExportGeoJson();
        QuietPreferences.setAddToWorkflow(addToWorkflow);
        QuietPreferences.setExportGeoJson(exportGeoJson);
        QuietPreferences.setLastCategory(selectedCategory.name());

        try {
            switch (selectedCategory) {
                case RENDERED -> startRenderedExport(outputDir, addToWorkflow, exportGeoJson);
                case MASK -> startMaskExport(outputDir, addToWorkflow, exportGeoJson);
                case RAW -> startRawExport(outputDir, addToWorkflow, exportGeoJson);
                case TILED -> startTiledExport(outputDir, addToWorkflow, exportGeoJson);
            }
        } catch (IllegalArgumentException e) {
            Dialogs.showWarningNotification(
                    resources.getString("name"), e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to start export", e);
            Dialogs.showErrorMessage(
                    resources.getString("error.title"),
                    "Failed to start export: " + e.getMessage());
        }
    }

    private void startRenderedExport(File outputDir, boolean addToWorkflow, boolean exportGeoJson) {
        renderedConfigPane.savePreferences();
        RenderedExportConfig config = renderedConfigPane.buildConfig(outputDir);

        PixelClassifier classifier = null;
        if (config.getRenderMode() == RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY) {
            String classifierName = renderedConfigPane.getClassifierName();
            if (classifierName == null || classifierName.isEmpty()) {
                Dialogs.showWarningNotification(
                        resources.getString("name"),
                        resources.getString("error.noClassifier"));
                return;
            }
            try {
                classifier = qupath.getProject().getPixelClassifiers().get(classifierName);
            } catch (Exception e) {
                logger.error("Failed to load classifier: {}", classifierName, e);
                Dialogs.showErrorMessage(
                        resources.getString("error.title"),
                        String.format(resources.getString("error.classifierLoad"), classifierName));
                return;
            }
        }

        String workflowScript = addToWorkflow
                ? ScriptGenerator.generate(ExportCategory.RENDERED, config) : null;

        currentTask = BatchExportTask.forRendered(
                imageSelectionPane.getSelectedEntries(), config, classifier, workflowScript, exportGeoJson);
        lastExportDirectory = outputDir;
        runTask();
    }

    private void startMaskExport(File outputDir, boolean addToWorkflow, boolean exportGeoJson) {
        maskConfigPane.savePreferences();
        MaskExportConfig config = maskConfigPane.buildConfig(outputDir);

        String workflowScript = addToWorkflow
                ? ScriptGenerator.generate(ExportCategory.MASK, config) : null;

        currentTask = BatchExportTask.forMask(
                imageSelectionPane.getSelectedEntries(), config, workflowScript, exportGeoJson);
        lastExportDirectory = outputDir;
        runTask();
    }

    private void startRawExport(File outputDir, boolean addToWorkflow, boolean exportGeoJson) {
        rawConfigPane.savePreferences();
        RawExportConfig config = rawConfigPane.buildConfig(outputDir);

        String workflowScript = addToWorkflow
                ? ScriptGenerator.generate(ExportCategory.RAW, config) : null;

        currentTask = BatchExportTask.forRaw(
                imageSelectionPane.getSelectedEntries(), config, workflowScript, exportGeoJson);
        lastExportDirectory = outputDir;
        runTask();
    }

    private void startTiledExport(File outputDir, boolean addToWorkflow, boolean exportGeoJson) {
        tiledConfigPane.savePreferences();
        TiledExportConfig config = tiledConfigPane.buildConfig(outputDir);

        String workflowScript = addToWorkflow
                ? ScriptGenerator.generate(ExportCategory.TILED, config) : null;

        currentTask = BatchExportTask.forTiled(
                imageSelectionPane.getSelectedEntries(), config, workflowScript, exportGeoJson);
        lastExportDirectory = outputDir;
        runTask();
    }

    private void runTask() {
        var progressBar = imageSelectionPane.getProgressBar();
        var statusLabel = imageSelectionPane.getStatusLabel();

        progressBar.setVisible(true);
        progressBar.progressProperty().bind(currentTask.progressProperty());
        statusLabel.textProperty().bind(currentTask.messageProperty());
        nextButton.setDisable(true);
        backButton.setDisable(true);

        // Reset button labels for the running state
        cancelButton.setText(resources.getString("button.cancel"));

        // Hide open folder button while export is running
        openFolderButton.setVisible(false);
        openFolderButton.setManaged(false);

        currentTask.setOnSucceeded(e -> Platform.runLater(() -> onExportComplete(currentTask.getValue())));
        currentTask.setOnFailed(e -> Platform.runLater(() -> onExportFailed(currentTask.getException())));
        currentTask.setOnCancelled(e -> Platform.runLater(this::onExportCancelled));

        Thread exportThread = new Thread(currentTask, "QuIET-Export");
        exportThread.setDaemon(true);
        exportThread.start();
    }

    private void onExportComplete(ExportResult result) {
        unbindProgress();
        showPostExportButtons();

        imageSelectionPane.getStatusLabel().setText(result.getSummary());

        // Show open folder button after successful export
        if (lastExportDirectory != null && result.getSucceeded() > 0) {
            openFolderButton.setVisible(true);
            openFolderButton.setManaged(true);
        }

        if (result.hasErrors()) {
            String errorText = String.join("\n", result.getErrors());
            Dialogs.showErrorMessage(
                    resources.getString("error.title"),
                    result.getSummary() + "\n\nErrors:\n" + errorText);
        } else {
            Dialogs.showInfoNotification(
                    resources.getString("name"),
                    result.getSummary());
        }
    }

    private void onExportFailed(Throwable exception) {
        unbindProgress();
        showPostExportButtons();
        logger.error("Export task failed", exception);

        String message = exception != null ? exception.getMessage() : "Unknown error";
        imageSelectionPane.getStatusLabel().setText("Export failed: " + message);
        Dialogs.showErrorMessage(
                resources.getString("error.title"),
                "Export failed: " + message);
    }

    private void onExportCancelled() {
        unbindProgress();
        showPostExportButtons();
        imageSelectionPane.getStatusLabel().setText("Export cancelled.");
    }

    /**
     * After an export finishes (success, failure, or cancel),
     * switch "Cancel" to "Close" since there is nothing to cancel.
     */
    private void showPostExportButtons() {
        cancelButton.setText(resources.getString("button.close"));
    }

    private void unbindProgress() {
        imageSelectionPane.getProgressBar().progressProperty().unbind();
        imageSelectionPane.getStatusLabel().textProperty().unbind();
        nextButton.setDisable(false);
        backButton.setDisable(false);
        updateNavButtons();
    }

    /**
     * Open the result folder in the system file manager.
     */
    private void openResultFolder() {
        if (lastExportDirectory == null || !lastExportDirectory.isDirectory()) return;
        try {
            Desktop.getDesktop().open(lastExportDirectory);
        } catch (Exception e) {
            logger.warn("Failed to open result folder: {}", e.getMessage());
        }
    }

    /**
     * Save preferences for all config panes.
     * Called when the wizard is closed (Cancel, X button) to ensure
     * user settings persist even without running an export.
     */
    private void saveAllPreferences() {
        try {
            renderedConfigPane.savePreferences();
            maskConfigPane.savePreferences();
            rawConfigPane.savePreferences();
            tiledConfigPane.savePreferences();
        } catch (Exception e) {
            logger.warn("Failed to save some preferences on wizard close: {}", e.getMessage());
        }
    }

    private void copyScript() {
        String script = generateCurrentScript();
        if (script != null) {
            imageSelectionPane.copyScriptToClipboard(script);
        }
    }

    private void saveScript() {
        String script = generateCurrentScript();
        if (script != null) {
            imageSelectionPane.saveScriptToFile(script);
        }
    }

    private String generateCurrentScript() {
        try {
            File outputDir = imageSelectionPane.getOutputDirectory();
            if (outputDir == null) {
                Dialogs.showWarningNotification(
                        resources.getString("name"),
                        resources.getString("error.invalidDir"));
                return null;
            }

            ExportCategory category = selectedCategory != null
                    ? selectedCategory : categoryPane.getSelectedCategory();

            Object config = switch (category) {
                case RENDERED -> renderedConfigPane.buildConfig(outputDir);
                case MASK -> maskConfigPane.buildConfig(outputDir);
                case RAW -> rawConfigPane.buildConfig(outputDir);
                case TILED -> tiledConfigPane.buildConfig(outputDir);
            };

            return ScriptGenerator.generate(category, config);
        } catch (Exception e) {
            logger.warn("Failed to generate script: {}", e.getMessage());
            Dialogs.showErrorMessage(
                    resources.getString("error.title"),
                    "Failed to generate script: " + e.getMessage());
            return null;
        }
    }

    private void saveWizardSize() {
        QuietPreferences.setWizardWidth(stage.getWidth());
        QuietPreferences.setWizardHeight(stage.getHeight());
    }
}
