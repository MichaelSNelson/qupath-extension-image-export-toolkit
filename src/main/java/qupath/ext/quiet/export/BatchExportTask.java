package qupath.ext.quiet.export;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.concurrent.Task;
import qupath.lib.classifiers.pixel.PixelClassifier;
import qupath.lib.images.ImageData;
import qupath.lib.plugins.workflow.DefaultScriptableWorkflowStep;
import qupath.lib.projects.ProjectImageEntry;

/**
 * Background JavaFX Task that exports selected project images based on
 * the chosen export category.
 * <p>
 * Reports progress (N of M images) and handles errors per-image,
 * continuing to process remaining images if one fails.
 */
public class BatchExportTask extends Task<ExportResult> {

    private static final Logger logger = LoggerFactory.getLogger(BatchExportTask.class);

    private final List<ProjectImageEntry<BufferedImage>> entries;
    private final ExportCategory category;
    private final RenderedExportConfig renderedConfig;
    private final MaskExportConfig maskConfig;
    private final RawExportConfig rawConfig;
    private final TiledExportConfig tiledConfig;
    private final PixelClassifier classifier;
    private final String workflowScript;
    private final boolean exportGeoJson;
    private final File outputDirectory;

    /**
     * Create a batch export task for rendered exports.
     */
    public static BatchExportTask forRendered(List<ProjectImageEntry<BufferedImage>> entries,
                                              RenderedExportConfig config,
                                              PixelClassifier classifier,
                                              String workflowScript,
                                              boolean exportGeoJson) {
        return new BatchExportTask(entries, ExportCategory.RENDERED,
                config, null, null, null, classifier, workflowScript,
                exportGeoJson, config.getOutputDirectory());
    }

    /**
     * Create a batch export task for mask exports.
     */
    public static BatchExportTask forMask(List<ProjectImageEntry<BufferedImage>> entries,
                                          MaskExportConfig config,
                                          String workflowScript,
                                          boolean exportGeoJson) {
        return new BatchExportTask(entries, ExportCategory.MASK,
                null, config, null, null, null, workflowScript,
                exportGeoJson, config.getOutputDirectory());
    }

    /**
     * Create a batch export task for raw exports.
     */
    public static BatchExportTask forRaw(List<ProjectImageEntry<BufferedImage>> entries,
                                         RawExportConfig config,
                                         String workflowScript,
                                         boolean exportGeoJson) {
        return new BatchExportTask(entries, ExportCategory.RAW,
                null, null, config, null, null, workflowScript,
                exportGeoJson, config.getOutputDirectory());
    }

    /**
     * Create a batch export task for tiled exports.
     */
    public static BatchExportTask forTiled(List<ProjectImageEntry<BufferedImage>> entries,
                                           TiledExportConfig config,
                                           String workflowScript,
                                           boolean exportGeoJson) {
        return new BatchExportTask(entries, ExportCategory.TILED,
                null, null, null, config, null, workflowScript,
                exportGeoJson, config.getOutputDirectory());
    }

    private BatchExportTask(List<ProjectImageEntry<BufferedImage>> entries,
                            ExportCategory category,
                            RenderedExportConfig renderedConfig,
                            MaskExportConfig maskConfig,
                            RawExportConfig rawConfig,
                            TiledExportConfig tiledConfig,
                            PixelClassifier classifier,
                            String workflowScript,
                            boolean exportGeoJson,
                            File outputDirectory) {
        this.entries = List.copyOf(entries);
        this.category = category;
        this.renderedConfig = renderedConfig;
        this.maskConfig = maskConfig;
        this.rawConfig = rawConfig;
        this.tiledConfig = tiledConfig;
        this.classifier = classifier;
        this.workflowScript = workflowScript;
        this.exportGeoJson = exportGeoJson;
        this.outputDirectory = outputDirectory;
    }

    @Override
    protected ExportResult call() throws Exception {
        int total = entries.size();
        int succeeded = 0;
        int failed = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            if (isCancelled()) {
                logger.info("Export cancelled by user after {} of {} images", i, total);
                break;
            }

            var entry = entries.get(i);
            String entryName = entry.getImageName();

            updateMessage(String.format("Exporting %d of %d: %s", i + 1, total, entryName));
            updateProgress(i, total);

            ImageData<BufferedImage> imageData = null;
            try {
                imageData = entry.readImageData();

                switch (category) {
                    case RENDERED -> exportRendered(imageData, entryName);
                    case MASK -> MaskImageExporter.exportMask(imageData, maskConfig, entryName);
                    case RAW -> RawImageExporter.exportRaw(imageData, rawConfig, entryName);
                    case TILED -> TiledImageExporter.exportTiled(imageData, tiledConfig, entryName);
                }

                // GeoJSON export (orthogonal to image export)
                if (exportGeoJson && outputDirectory != null) {
                    try {
                        GeoJsonExporter.exportGeoJson(imageData, outputDirectory, entryName);
                    } catch (Exception ge) {
                        logger.warn("GeoJSON export failed for {}: {}", entryName, ge.getMessage());
                    }
                }
                succeeded++;

                // Add workflow step if configured
                if (shouldAddWorkflow() && workflowScript != null) {
                    addWorkflowStep(imageData, entry);
                }

            } catch (IllegalArgumentException e) {
                // Classifier doesn't support image - skip
                skipped++;
                String msg = entryName + ": " + e.getMessage();
                errors.add(msg);
                logger.warn("Skipping {}: {}", entryName, e.getMessage());
            } catch (Exception e) {
                failed++;
                String errorMsg = entryName + ": " + e.getMessage();
                errors.add(errorMsg);
                logger.error("Failed to export image: {}", entryName, e);
            } finally {
                if (imageData != null) {
                    try {
                        imageData.getServer().close();
                    } catch (Exception e) {
                        logger.warn("Error closing image server for: {}", entryName, e);
                    }
                }
            }
        }

        updateProgress(total, total);
        updateMessage("Export complete");

        return new ExportResult(succeeded, failed, skipped, errors);
    }

    private void exportRendered(ImageData<BufferedImage> imageData, String entryName)
            throws Exception {
        if (renderedConfig.getRenderMode() == RenderedExportConfig.RenderMode.OBJECT_OVERLAY) {
            RenderedImageExporter.exportWithObjectOverlay(imageData, renderedConfig, entryName);
        } else {
            if (!classifier.supportsImage(imageData)) {
                throw new IllegalArgumentException(
                        "Classifier does not support image: " + entryName);
            }
            RenderedImageExporter.exportWithClassifier(
                    imageData, classifier, renderedConfig, entryName);
        }
    }

    private boolean shouldAddWorkflow() {
        return switch (category) {
            case RENDERED -> renderedConfig != null && renderedConfig.isAddToWorkflow();
            case MASK -> maskConfig != null && maskConfig.isAddToWorkflow();
            case RAW -> rawConfig != null && rawConfig.isAddToWorkflow();
            case TILED -> tiledConfig != null && tiledConfig.isAddToWorkflow();
        };
    }

    private void addWorkflowStep(ImageData<BufferedImage> imageData,
                                 ProjectImageEntry<BufferedImage> entry) {
        try {
            String stepName = switch (category) {
                case RENDERED -> renderedConfig.getRenderMode() ==
                        RenderedExportConfig.RenderMode.OBJECT_OVERLAY
                        ? "Object Overlay Export" : "Classifier Export";
                case MASK -> "Mask Export";
                case RAW -> "Raw Image Export";
                case TILED -> "Tiled Export";
            };
            imageData.getHistoryWorkflow().addStep(
                    new DefaultScriptableWorkflowStep(stepName, workflowScript));
            entry.saveImageData(imageData);
            logger.debug("Added workflow step for: {}", entry.getImageName());
        } catch (Exception e) {
            logger.warn("Failed to add workflow step for: {}", entry.getImageName(), e);
        }
    }
}
