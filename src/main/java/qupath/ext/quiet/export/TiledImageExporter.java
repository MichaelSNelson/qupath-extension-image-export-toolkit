package qupath.ext.quiet.export;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.LabeledImageServer;
import qupath.lib.images.writers.TileExporter;
import qupath.lib.objects.classes.PathClass;

/**
 * Exports tiled image + label pairs for ML training using QuPath's TileExporter API.
 */
public class TiledImageExporter {

    private static final Logger logger = LoggerFactory.getLogger(TiledImageExporter.class);

    private TiledImageExporter() {
        // Utility class
    }

    /**
     * Export tiled images (and optionally label masks) for a single image entry.
     *
     * @param imageData the image data to export
     * @param config    tiled export configuration
     * @param entryName the image entry name (used for subdirectory naming)
     * @throws IOException if the export fails
     */
    public static void exportTiled(ImageData<BufferedImage> imageData,
                                   TiledExportConfig config,
                                   String entryName) throws IOException {

        ImageServer<BufferedImage> labelServer = null;

        try {
            // Build the TileExporter
            var exporter = new TileExporter(imageData)
                    .tileSize(config.getTileSize())
                    .overlap(config.getOverlap())
                    .downsample(config.getDownsample())
                    .imageExtension("." + config.getImageFormat().getExtension())
                    .annotatedTilesOnly(config.isAnnotatedTilesOnly());

            // Build label server if label format is specified
            if (config.getLabelFormat() != null) {
                labelServer = buildLabelServer(imageData, config);
                exporter.labeledServer(labelServer);
                exporter.labeledImageExtension("." + config.getLabelFormat().getExtension());
            }

            // Set parent object filter
            switch (config.getParentObjectFilter()) {
                case ANNOTATIONS -> exporter.parentObjects(
                        imageData.getHierarchy().getAnnotationObjects());
                case TMA_CORES -> exporter.parentObjects(
                        imageData.getHierarchy().getTMAGrid() != null
                                ? imageData.getHierarchy().getTMAGrid().getTMACoreList()
                                : java.util.Collections.emptyList());
                case ALL -> {
                    // No parent filter -- export all tiles
                }
            }

            // Export GeoJSON per tile if requested
            if (config.isExportGeoJson()) {
                exporter.exportJson(true);
            }

            // Write tiles
            File outputDir = new File(config.getOutputDirectory(),
                    config.buildOutputFilename(entryName));
            outputDir.mkdirs();

            exporter.writeTiles(outputDir.getAbsolutePath());

            logger.info("Exported tiles for {} to: {}", entryName, outputDir.getAbsolutePath());

        } catch (Exception e) {
            throw new IOException("Failed to export tiles for: " + entryName, e);
        } finally {
            if (labelServer != null) {
                try {
                    labelServer.close();
                } catch (Exception e) {
                    logger.warn("Error closing label server for: {}", entryName, e);
                }
            }
        }
    }

    /**
     * Build a LabeledImageServer for tile label generation.
     * Uses a simplified configuration suitable for tile-based ML training.
     */
    private static ImageServer<BufferedImage> buildLabelServer(
            ImageData<BufferedImage> imageData,
            TiledExportConfig config) throws IOException {

        var builder = new LabeledImageServer.Builder(imageData)
                .backgroundLabel(0)
                .downsample(config.getDownsample());

        MaskExportConfig labelConfig = config.getLabeledServerConfig();
        if (labelConfig != null) {
            // Use the mask config to configure the label server
            switch (labelConfig.getObjectSource()) {
                case ANNOTATIONS -> builder.useAnnotations();
                case DETECTIONS -> builder.useDetections();
                case CELLS -> builder.useCells();
            }

            switch (labelConfig.getMaskType()) {
                case BINARY -> {
                    if (!labelConfig.getSelectedClassifications().isEmpty()) {
                        for (String className : labelConfig.getSelectedClassifications()) {
                            builder.addLabel(PathClass.fromString(className), 1);
                        }
                    } else {
                        builder.addUnclassifiedLabel(1);
                    }
                }
                case GRAYSCALE_LABELS -> {
                    builder.grayscale(true);
                    var classes = labelConfig.getSelectedClassifications();
                    for (int i = 0; i < classes.size(); i++) {
                        builder.addLabel(PathClass.fromString(classes.get(i)), i + 1);
                    }
                }
                case INSTANCE -> {
                    builder.useInstanceLabels();
                    if (labelConfig.isShuffleInstanceLabels()) {
                        builder.shuffleInstanceLabels(true);
                    }
                }
                case COLORED -> {
                    var colorClasses = labelConfig.getSelectedClassifications();
                    for (int i = 0; i < colorClasses.size(); i++) {
                        builder.addLabel(PathClass.fromString(colorClasses.get(i)), i + 1);
                    }
                }
                case MULTICHANNEL -> {
                    builder.multichannelOutput(true);
                    var mcClasses = labelConfig.getSelectedClassifications();
                    for (int i = 0; i < mcClasses.size(); i++) {
                        builder.addLabel(PathClass.fromString(mcClasses.get(i)), i + 1);
                    }
                }
            }

            if (labelConfig.isEnableBoundary()) {
                if (labelConfig.getBoundaryThickness() > 1) {
                    builder.setBoundaryLabel("Boundary", labelConfig.getBoundaryLabel(),
                            Integer.valueOf(labelConfig.getBoundaryThickness()));
                } else {
                    builder.setBoundaryLabel("Boundary", labelConfig.getBoundaryLabel());
                }
            }
        } else {
            // Default: binary mask from annotations
            builder.useAnnotations();
            builder.addUnclassifiedLabel(1);
        }

        return builder.build();
    }
}
