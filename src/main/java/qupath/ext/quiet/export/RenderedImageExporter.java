package qupath.ext.quiet.export;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.lib.classifiers.pixel.PixelClassificationImageServer;
import qupath.lib.classifiers.pixel.PixelClassifier;
import qupath.lib.gui.viewer.OverlayOptions;
import qupath.lib.gui.viewer.overlays.HierarchyOverlay;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.writers.ImageWriterTools;
import qupath.lib.regions.ImageRegion;
import qupath.lib.regions.RegionRequest;

/**
 * Exports a single image with overlays rendered on top.
 * <p>
 * Supports two modes:
 * <ul>
 *   <li>Classifier overlay: composites a pixel classifier result onto the base image</li>
 *   <li>Object overlay: renders annotations and/or detections onto the base image</li>
 * </ul>
 * Uses direct compositing for performance: reads the base image and overlay
 * as single downsampled BufferedImages and composites with Java2D AlphaComposite.
 */
public class RenderedImageExporter {

    private static final Logger logger = LoggerFactory.getLogger(RenderedImageExporter.class);

    private RenderedImageExporter() {
        // Utility class
    }

    /**
     * Exports a single image with a pixel classifier overlay rendered on top.
     *
     * @param imageData  the image data to export (caller is responsible for closing)
     * @param classifier the pixel classifier to apply as overlay
     * @param config     rendered export configuration
     * @param entryName  the image entry name (used for filename generation)
     * @throws IOException if the export fails
     * @throws IllegalArgumentException if the classifier does not support this image
     */
    public static void exportWithClassifier(ImageData<BufferedImage> imageData,
                                            PixelClassifier classifier,
                                            RenderedExportConfig config,
                                            String entryName) throws IOException {

        if (!classifier.supportsImage(imageData)) {
            throw new IllegalArgumentException(
                    "Classifier does not support image: " + entryName);
        }

        ImageServer<BufferedImage> baseServer = imageData.getServer();
        PixelClassificationImageServer classificationServer = null;

        try {
            classificationServer = new PixelClassificationImageServer(imageData, classifier);

            double downsample = config.getDownsample();
            int outputWidth = (int) Math.ceil(baseServer.getWidth() / downsample);
            int outputHeight = (int) Math.ceil(baseServer.getHeight() / downsample);

            RegionRequest request = RegionRequest.createInstance(
                    baseServer.getPath(), downsample,
                    0, 0, baseServer.getWidth(), baseServer.getHeight());

            logger.debug("Reading base image at downsample {} for: {}", downsample, entryName);
            BufferedImage baseImage = baseServer.readRegion(request);

            logger.debug("Reading classification at downsample {} for: {}", downsample, entryName);
            RegionRequest classRequest = RegionRequest.createInstance(
                    classificationServer.getPath(), downsample,
                    0, 0, classificationServer.getWidth(), classificationServer.getHeight());
            BufferedImage classImage = classificationServer.readRegion(classRequest);

            // Create output image (TYPE_INT_RGB strips alpha for JPEG compatibility)
            BufferedImage result = new BufferedImage(
                    baseImage.getWidth(), baseImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB);

            Graphics2D g2d = result.createGraphics();
            g2d.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g2d.drawImage(baseImage, 0, 0, null);

            float opacity = (float) config.getOverlayOpacity();
            if (opacity > 0 && classImage != null) {
                g2d.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, opacity));
                g2d.drawImage(classImage,
                        0, 0, baseImage.getWidth(), baseImage.getHeight(), null);
            }

            // Optionally paint annotations/detections on top of classifier
            if (config.isIncludeAnnotations() || config.isIncludeDetections()) {
                g2d.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 1.0f));
                paintObjects(g2d, imageData, downsample, outputWidth, outputHeight,
                        config.isIncludeAnnotations(), config.isIncludeDetections(),
                        config.isFillAnnotations(), config.isShowNames());
            }

            g2d.dispose();
            baseImage = null;
            classImage = null;

            String filename = config.buildOutputFilename(entryName);
            File outputFile = new File(config.getOutputDirectory(), filename);
            ImageWriterTools.writeImage(result, outputFile.getAbsolutePath());

            logger.info("Exported: {}", outputFile.getAbsolutePath());

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to export image: " + entryName, e);
        } finally {
            if (classificationServer != null) {
                try {
                    classificationServer.close();
                } catch (Exception e) {
                    logger.warn("Error closing classification server for: {}", entryName, e);
                }
            }
        }
    }

    /**
     * Exports a single image with object overlays (annotations and/or detections)
     * rendered on top, without requiring a pixel classifier.
     *
     * @param imageData  the image data to export (caller is responsible for closing)
     * @param config     rendered export configuration
     * @param entryName  the image entry name (used for filename generation)
     * @throws IOException if the export fails
     */
    public static void exportWithObjectOverlay(ImageData<BufferedImage> imageData,
                                               RenderedExportConfig config,
                                               String entryName) throws IOException {

        ImageServer<BufferedImage> baseServer = imageData.getServer();

        try {
            double downsample = config.getDownsample();
            int outputWidth = (int) Math.ceil(baseServer.getWidth() / downsample);
            int outputHeight = (int) Math.ceil(baseServer.getHeight() / downsample);

            RegionRequest request = RegionRequest.createInstance(
                    baseServer.getPath(), downsample,
                    0, 0, baseServer.getWidth(), baseServer.getHeight());

            logger.debug("Reading base image at downsample {} for: {}", downsample, entryName);
            BufferedImage baseImage = baseServer.readRegion(request);

            BufferedImage result = new BufferedImage(
                    baseImage.getWidth(), baseImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB);

            Graphics2D g2d = result.createGraphics();
            g2d.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g2d.drawImage(baseImage, 0, 0, null);

            float opacity = (float) config.getOverlayOpacity();
            if (opacity > 0) {
                g2d.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, opacity));
                paintObjects(g2d, imageData, downsample, outputWidth, outputHeight,
                        config.isIncludeAnnotations(), config.isIncludeDetections(),
                        config.isFillAnnotations(), config.isShowNames());
            }

            g2d.dispose();
            baseImage = null;

            String filename = config.buildOutputFilename(entryName);
            File outputFile = new File(config.getOutputDirectory(), filename);
            ImageWriterTools.writeImage(result, outputFile.getAbsolutePath());

            logger.info("Exported: {}", outputFile.getAbsolutePath());

        } catch (Exception e) {
            throw new IOException("Failed to export image: " + entryName, e);
        }
    }

    /**
     * Paint object overlays onto a graphics context.
     */
    private static void paintObjects(Graphics2D g2d,
                                     ImageData<BufferedImage> imageData,
                                     double downsample,
                                     int width, int height,
                                     boolean showAnnotations,
                                     boolean showDetections,
                                     boolean fillAnnotations,
                                     boolean showNames) {
        try {
            var overlayOptions = new OverlayOptions();
            overlayOptions.setShowAnnotations(showAnnotations);
            overlayOptions.setShowDetections(showDetections);
            overlayOptions.setFillAnnotations(fillAnnotations);
            overlayOptions.setShowNames(showNames);
            var hierarchyOverlay = new HierarchyOverlay(null, overlayOptions, imageData);

            var gCopy = (Graphics2D) g2d.create();
            gCopy.scale(1.0 / downsample, 1.0 / downsample);

            var region = ImageRegion.createInstance(
                    0, 0,
                    imageData.getServer().getWidth(),
                    imageData.getServer().getHeight(),
                    0, 0);

            hierarchyOverlay.paintOverlay(gCopy, region, downsample, imageData, true);
            gCopy.dispose();
        } catch (Exception e) {
            logger.warn("Failed to paint objects: {}", e.getMessage());
        }
    }
}
