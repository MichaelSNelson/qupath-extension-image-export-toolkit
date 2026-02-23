package qupath.ext.quiet.export;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.lib.analysis.heatmaps.DensityMaps;
import qupath.lib.analysis.heatmaps.DensityMaps.DensityMapBuilder;
import qupath.lib.classifiers.pixel.PixelClassificationImageServer;
import qupath.lib.classifiers.pixel.PixelClassifier;
import qupath.lib.color.ColorMaps;
import qupath.lib.display.ImageDisplay;
import qupath.lib.display.settings.DisplaySettingUtils;
import qupath.lib.gui.images.servers.ChannelDisplayTransformServer;
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
 * Supports three modes:
 * <ul>
 *   <li>Classifier overlay: composites a pixel classifier result onto the base image</li>
 *   <li>Object overlay: renders annotations and/or detections onto the base image</li>
 *   <li>Density map overlay: renders a colorized density map with optional color scale bar</li>
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
        ImageServer<BufferedImage> displayServer = null;

        try {
            classificationServer = new PixelClassificationImageServer(imageData, classifier);
            displayServer = resolveDisplayServer(imageData, baseServer, config);

            BufferedImage result = renderClassifierComposite(
                    imageData, baseServer, classificationServer, displayServer, config);

            String filename = config.buildOutputFilename(entryName);
            File outputFile = new File(config.getOutputDirectory(), filename);
            ImageWriterTools.writeImage(result, outputFile.getAbsolutePath());

            logger.info("Exported: {}", outputFile.getAbsolutePath());

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to export image: " + entryName, e);
        } finally {
            closeQuietly(displayServer, entryName);
            closeQuietly(classificationServer, entryName);
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
        ImageServer<BufferedImage> displayServer = null;

        try {
            displayServer = resolveDisplayServer(imageData, baseServer, config);

            BufferedImage result = renderObjectComposite(
                    imageData, baseServer, displayServer, config);

            String filename = config.buildOutputFilename(entryName);
            File outputFile = new File(config.getOutputDirectory(), filename);
            ImageWriterTools.writeImage(result, outputFile.getAbsolutePath());

            logger.info("Exported: {}", outputFile.getAbsolutePath());

        } catch (Exception e) {
            throw new IOException("Failed to export image: " + entryName, e);
        } finally {
            closeQuietly(displayServer, entryName);
        }
    }

    /**
     * Exports a single image with a density map overlay colorized by a LUT.
     *
     * @param imageData      the image data to export (caller is responsible for closing)
     * @param densityBuilder the density map builder (loaded from project resources)
     * @param config         rendered export configuration
     * @param entryName      the image entry name (used for filename generation)
     * @throws IOException if the export fails
     */
    public static void exportWithDensityMap(ImageData<BufferedImage> imageData,
                                             DensityMapBuilder densityBuilder,
                                             RenderedExportConfig config,
                                             String entryName) throws IOException {

        ImageServer<BufferedImage> baseServer = imageData.getServer();
        ImageServer<BufferedImage> displayServer = null;
        ImageServer<BufferedImage> densityServer = null;

        try {
            densityServer = densityBuilder.buildServer(imageData);
            displayServer = resolveDisplayServer(imageData, baseServer, config);

            BufferedImage result = renderDensityMapComposite(
                    imageData, baseServer, densityServer, displayServer, config);

            String filename = config.buildOutputFilename(entryName);
            File outputFile = new File(config.getOutputDirectory(), filename);
            ImageWriterTools.writeImage(result, outputFile.getAbsolutePath());

            logger.info("Exported: {}", outputFile.getAbsolutePath());

        } catch (Exception e) {
            throw new IOException("Failed to export density map image: " + entryName, e);
        } finally {
            closeQuietly(displayServer, entryName);
            closeQuietly(densityServer, entryName);
        }
    }

    /**
     * Renders a preview of the current image with the given config.
     * Returns a BufferedImage sized to fit within {@code maxDimension} on its longest side.
     *
     * @param imageData      the image data to preview
     * @param classifier     the pixel classifier (null for non-CLASSIFIER modes)
     * @param densityBuilder the density map builder (null for non-DENSITY_MAP modes)
     * @param config         rendered export configuration
     * @param maxDimension   maximum pixel dimension for the longest side of the preview
     * @return the rendered preview image
     * @throws IOException if rendering fails
     */
    public static BufferedImage renderPreview(ImageData<BufferedImage> imageData,
                                              PixelClassifier classifier,
                                              DensityMapBuilder densityBuilder,
                                              RenderedExportConfig config,
                                              int maxDimension) throws IOException {

        ImageServer<BufferedImage> baseServer = imageData.getServer();
        int serverW = baseServer.getWidth();
        int serverH = baseServer.getHeight();

        // Compute downsample to fit within maxDimension
        double longestSide = Math.max(serverW, serverH);
        double previewDownsample = Math.max(config.getDownsample(),
                longestSide / maxDimension);

        // Build a temporary config with the preview downsample
        // (reuse all other settings from the original config)
        RenderedExportConfig previewConfig = new RenderedExportConfig.Builder()
                .renderMode(config.getRenderMode())
                .displaySettingsMode(config.getDisplaySettingsMode())
                .capturedDisplaySettings(config.getCapturedDisplaySettings())
                .displayPresetName(config.getDisplayPresetName())
                .classifierName(config.getClassifierName())
                .overlayOpacity(config.getOverlayOpacity())
                .downsample(previewDownsample)
                .format(config.getFormat())
                .outputDirectory(config.getOutputDirectory())
                .includeAnnotations(config.isIncludeAnnotations())
                .includeDetections(config.isIncludeDetections())
                .fillAnnotations(config.isFillAnnotations())
                .showNames(config.isShowNames())
                .showScaleBar(config.isShowScaleBar())
                .scaleBarPosition(config.getScaleBarPosition())
                .scaleBarColorHex(config.getScaleBarColorHex())
                .scaleBarFontSize(config.getScaleBarFontSize())
                .scaleBarBoldText(config.isScaleBarBoldText())
                .densityMapName(config.getDensityMapName())
                .colormapName(config.getColormapName())
                .showColorScaleBar(config.isShowColorScaleBar())
                .colorScaleBarPosition(config.getColorScaleBarPosition())
                .colorScaleBarFontSize(config.getColorScaleBarFontSize())
                .colorScaleBarBoldText(config.isColorScaleBarBoldText())
                .build();

        ImageServer<BufferedImage> displayServer = null;
        PixelClassificationImageServer classificationServer = null;
        ImageServer<BufferedImage> densityServer = null;

        try {
            displayServer = resolveDisplayServer(imageData, baseServer, previewConfig);

            if (config.getRenderMode() == RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY
                    && classifier != null) {
                classificationServer = new PixelClassificationImageServer(imageData, classifier);
                try {
                    return renderClassifierComposite(
                            imageData, baseServer, classificationServer, displayServer, previewConfig);
                } catch (Exception e) {
                    throw new IOException("Failed to render classifier preview", e);
                }
            } else if (config.getRenderMode() == RenderedExportConfig.RenderMode.DENSITY_MAP_OVERLAY
                    && densityBuilder != null) {
                densityServer = densityBuilder.buildServer(imageData);
                try {
                    return renderDensityMapComposite(
                            imageData, baseServer, densityServer, displayServer, previewConfig);
                } catch (Exception e) {
                    throw new IOException("Failed to render density map preview", e);
                }
            } else {
                try {
                    return renderObjectComposite(
                            imageData, baseServer, displayServer, previewConfig);
                } catch (Exception e) {
                    throw new IOException("Failed to render object overlay preview", e);
                }
            }
        } finally {
            closeQuietly(displayServer, "preview");
            closeQuietly(classificationServer, "preview");
            closeQuietly(densityServer, "preview");
        }
    }

    /**
     * Backward-compatible overload without density map builder parameter.
     */
    public static BufferedImage renderPreview(ImageData<BufferedImage> imageData,
                                              PixelClassifier classifier,
                                              RenderedExportConfig config,
                                              int maxDimension) throws IOException {
        return renderPreview(imageData, classifier, null, config, maxDimension);
    }

    /**
     * Shared rendering logic for classifier overlay compositing.
     */
    private static BufferedImage renderClassifierComposite(
            ImageData<BufferedImage> imageData,
            ImageServer<BufferedImage> baseServer,
            PixelClassificationImageServer classificationServer,
            ImageServer<BufferedImage> displayServer,
            RenderedExportConfig config) throws Exception {

        double downsample = config.getDownsample();
        int outputWidth = (int) Math.ceil(baseServer.getWidth() / downsample);
        int outputHeight = (int) Math.ceil(baseServer.getHeight() / downsample);

        ImageServer<BufferedImage> readServer =
                displayServer != null ? displayServer : baseServer;
        RegionRequest request = RegionRequest.createInstance(
                readServer.getPath(), downsample,
                0, 0, readServer.getWidth(), readServer.getHeight());

        BufferedImage baseImage = readServer.readRegion(request);

        RegionRequest classRequest = RegionRequest.createInstance(
                classificationServer.getPath(), downsample,
                0, 0, classificationServer.getWidth(), classificationServer.getHeight());
        BufferedImage classImage = classificationServer.readRegion(classRequest);

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

        if (config.isIncludeAnnotations() || config.isIncludeDetections()) {
            g2d.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 1.0f));
            paintObjects(g2d, imageData, downsample, outputWidth, outputHeight,
                    config.isIncludeAnnotations(), config.isIncludeDetections(),
                    config.isFillAnnotations(), config.isShowNames());
        }

        maybeDrawScaleBar(g2d, imageData, config,
                baseImage.getWidth(), baseImage.getHeight());

        g2d.dispose();
        return result;
    }

    /**
     * Shared rendering logic for object overlay compositing.
     */
    private static BufferedImage renderObjectComposite(
            ImageData<BufferedImage> imageData,
            ImageServer<BufferedImage> baseServer,
            ImageServer<BufferedImage> displayServer,
            RenderedExportConfig config) throws Exception {

        double downsample = config.getDownsample();
        int outputWidth = (int) Math.ceil(baseServer.getWidth() / downsample);
        int outputHeight = (int) Math.ceil(baseServer.getHeight() / downsample);

        ImageServer<BufferedImage> readServer =
                displayServer != null ? displayServer : baseServer;
        RegionRequest request = RegionRequest.createInstance(
                readServer.getPath(), downsample,
                0, 0, readServer.getWidth(), readServer.getHeight());

        BufferedImage baseImage = readServer.readRegion(request);

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

        maybeDrawScaleBar(g2d, imageData, config,
                baseImage.getWidth(), baseImage.getHeight());

        g2d.dispose();
        return result;
    }

    /**
     * Resolve display settings and wrap the base server if needed.
     *
     * @return a wrapped server applying display settings, or null for RAW mode
     *         or if display settings cannot be applied
     */
    private static ImageServer<BufferedImage> resolveDisplayServer(
            ImageData<BufferedImage> imageData,
            ImageServer<BufferedImage> baseServer,
            RenderedExportConfig config) throws IOException {

        var mode = config.getDisplaySettingsMode();
        if (mode == RenderedExportConfig.DisplaySettingsMode.RAW) {
            return null;
        }

        try {
            var display = ImageDisplay.create(imageData);

            if (mode == RenderedExportConfig.DisplaySettingsMode.CURRENT_VIEWER
                    || mode == RenderedExportConfig.DisplaySettingsMode.SAVED_PRESET) {
                var settings = config.getCapturedDisplaySettings();
                if (settings != null) {
                    DisplaySettingUtils.applySettingsToDisplay(display, settings);
                } else {
                    logger.warn("No display settings available for mode {}; "
                            + "using per-image defaults", mode);
                }
            }
            // PER_IMAGE_SAVED: display already loaded from imageData properties

            var channels = display.selectedChannels();
            if (channels == null || channels.isEmpty()) {
                logger.warn("No visible channels after applying display settings; "
                        + "falling back to raw pixel data");
                return null;
            }

            return ChannelDisplayTransformServer.createColorTransformServer(
                    baseServer, channels);
        } catch (Exception e) {
            logger.warn("Failed to create display transform server, "
                    + "falling back to raw pixel data: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Draw a scale bar if enabled and the image has pixel calibration.
     */
    private static void maybeDrawScaleBar(Graphics2D g2d,
                                           ImageData<BufferedImage> imageData,
                                           RenderedExportConfig config,
                                           int w, int h) {
        if (!config.isShowScaleBar()) return;
        var cal = imageData.getServer().getPixelCalibration();
        if (!cal.hasPixelSizeMicrons()) {
            logger.warn("Scale bar skipped -- no pixel calibration");
            return;
        }
        double pxSize = cal.getAveragedPixelSizeMicrons() * config.getDownsample();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        ScaleBarRenderer.drawScaleBar(g2d, w, h, pxSize,
                config.getScaleBarPosition(),
                config.getScaleBarColorAsAwt(),
                config.getScaleBarFontSize(),
                config.isScaleBarBoldText());
    }

    /**
     * Shared rendering logic for density map overlay compositing.
     */
    private static BufferedImage renderDensityMapComposite(
            ImageData<BufferedImage> imageData,
            ImageServer<BufferedImage> baseServer,
            ImageServer<BufferedImage> densityServer,
            ImageServer<BufferedImage> displayServer,
            RenderedExportConfig config) throws Exception {

        double downsample = config.getDownsample();
        int outputWidth = (int) Math.ceil(baseServer.getWidth() / downsample);
        int outputHeight = (int) Math.ceil(baseServer.getHeight() / downsample);

        // Read base image
        ImageServer<BufferedImage> readServer =
                displayServer != null ? displayServer : baseServer;
        RegionRequest request = RegionRequest.createInstance(
                readServer.getPath(), downsample,
                0, 0, readServer.getWidth(), readServer.getHeight());
        BufferedImage baseImage = readServer.readRegion(request);

        // Read density map
        RegionRequest densityRequest = RegionRequest.createInstance(
                densityServer.getPath(), downsample,
                0, 0, densityServer.getWidth(), densityServer.getHeight());
        BufferedImage densityImage = densityServer.readRegion(densityRequest);

        // Resolve colormap
        ColorMaps.ColorMap colorMap = resolveColorMap(config.getColormapName());

        // Compute min/max from density raster
        double[] minMax = computeMinMax(densityImage);
        double minVal = minMax[0];
        double maxVal = minMax[1];

        // Colorize density map
        BufferedImage colorized = colorizeDensityMap(densityImage, colorMap, minVal, maxVal);

        // Composite onto base
        BufferedImage result = new BufferedImage(
                baseImage.getWidth(), baseImage.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2d.drawImage(baseImage, 0, 0, null);

        float opacity = (float) config.getOverlayOpacity();
        if (opacity > 0 && colorized != null) {
            g2d.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, opacity));
            g2d.drawImage(colorized,
                    0, 0, baseImage.getWidth(), baseImage.getHeight(), null);
        }

        // Object overlays on top if requested
        if (config.isIncludeAnnotations() || config.isIncludeDetections()) {
            g2d.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 1.0f));
            paintObjects(g2d, imageData, downsample, outputWidth, outputHeight,
                    config.isIncludeAnnotations(), config.isIncludeDetections(),
                    config.isFillAnnotations(), config.isShowNames());
        }

        maybeDrawScaleBar(g2d, imageData, config,
                baseImage.getWidth(), baseImage.getHeight());

        maybeDrawColorScaleBar(g2d, config, colorMap, minVal, maxVal,
                baseImage.getWidth(), baseImage.getHeight());

        g2d.dispose();
        return result;
    }

    /**
     * Compute min/max float values from band 0 of a density image raster.
     */
    private static double[] computeMinMax(BufferedImage densityImage) {
        WritableRaster raster = densityImage.getRaster();
        int w = raster.getWidth();
        int h = raster.getHeight();
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double val = raster.getSampleDouble(x, y, 0);
                if (Double.isNaN(val)) continue;
                if (val < min) min = val;
                if (val > max) max = val;
            }
        }
        if (min > max) {
            // All NaN or empty
            min = 0;
            max = 1;
        }
        return new double[]{min, max};
    }

    /**
     * Look up a ColorMap by name from QuPath's registered color maps.
     * Falls back to the first available map if the name is not found.
     */
    private static ColorMaps.ColorMap resolveColorMap(String name) {
        Map<String, ColorMaps.ColorMap> maps = ColorMaps.getColorMaps();
        if (name != null && maps.containsKey(name)) {
            return maps.get(name);
        }
        // Try case-insensitive match
        for (var entry : maps.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        // Fallback to first available
        if (!maps.isEmpty()) {
            logger.warn("Colormap '{}' not found, using default", name);
            return maps.values().iterator().next();
        }
        throw new IllegalStateException("No color maps available");
    }

    /**
     * Colorize a density image using a color map.
     * Creates a TYPE_INT_ARGB image with transparent pixels for NaN values.
     */
    private static BufferedImage colorizeDensityMap(BufferedImage densityImage,
                                                     ColorMaps.ColorMap colorMap,
                                                     double min, double max) {
        int w = densityImage.getWidth();
        int h = densityImage.getHeight();
        WritableRaster raster = densityImage.getRaster();
        BufferedImage colorized = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double val = raster.getSampleDouble(x, y, 0);
                if (Double.isNaN(val)) {
                    colorized.setRGB(x, y, 0x00000000); // fully transparent
                } else {
                    int rgb = colorMap.getColor(val, min, max);
                    colorized.setRGB(x, y, 0xFF000000 | (rgb & 0x00FFFFFF));
                }
            }
        }
        return colorized;
    }

    /**
     * Draw a color scale bar if enabled in the config.
     */
    private static void maybeDrawColorScaleBar(Graphics2D g2d,
                                                RenderedExportConfig config,
                                                ColorMaps.ColorMap colorMap,
                                                double min, double max,
                                                int w, int h) {
        if (!config.isShowColorScaleBar()) return;
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        ColorScaleBarRenderer.drawColorScaleBar(g2d, w, h, colorMap, min, max,
                config.getColorScaleBarPosition(),
                config.getColorScaleBarFontSize(),
                config.isColorScaleBarBoldText());
    }

    private static void closeQuietly(AutoCloseable closeable, String context) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.warn("Error closing resource for: {}", context, e);
            }
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
