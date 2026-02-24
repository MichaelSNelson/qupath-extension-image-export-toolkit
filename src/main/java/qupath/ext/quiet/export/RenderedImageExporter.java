package qupath.ext.quiet.export;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.lib.analysis.heatmaps.DensityMaps;
import qupath.lib.analysis.heatmaps.DensityMaps.DensityMapBuilder;
import qupath.lib.classifiers.pixel.PixelClassificationImageServer;
import qupath.lib.classifiers.pixel.PixelClassifier;
import qupath.lib.color.ColorMaps;
import qupath.lib.common.GeneralTools;
import qupath.lib.display.ImageDisplay;
import qupath.lib.display.settings.DisplaySettingUtils;
import qupath.lib.gui.images.servers.ChannelDisplayTransformServer;
import qupath.lib.gui.viewer.OverlayOptions;
import qupath.lib.gui.viewer.overlays.HierarchyOverlay;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.writers.ImageWriterTools;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.regions.ImageRegion;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.interfaces.ROI;

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
     * @param panelIndex zero-based index for auto-incrementing panel labels
     * @throws IOException if the export fails
     * @throws IllegalArgumentException if the classifier does not support this image
     */
    public static void exportWithClassifier(ImageData<BufferedImage> imageData,
                                            PixelClassifier classifier,
                                            RenderedExportConfig config,
                                            String entryName,
                                            int panelIndex) throws IOException {

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

            String panelLabel = resolvePanelLabel(config, panelIndex);
            BufferedImage result = renderClassifierComposite(
                    imageData, baseServer, classificationServer, displayServer, config, panelLabel);

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
     * @param panelIndex zero-based index for auto-incrementing panel labels
     * @throws IOException if the export fails
     */
    public static void exportWithObjectOverlay(ImageData<BufferedImage> imageData,
                                               RenderedExportConfig config,
                                               String entryName,
                                               int panelIndex) throws IOException {

        ImageServer<BufferedImage> baseServer = imageData.getServer();
        ImageServer<BufferedImage> displayServer = null;

        try {
            displayServer = resolveDisplayServer(imageData, baseServer, config);

            String panelLabel = resolvePanelLabel(config, panelIndex);
            BufferedImage result = renderObjectComposite(
                    imageData, baseServer, displayServer, config, panelLabel);

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
     * @param panelIndex     zero-based index for auto-incrementing panel labels
     * @throws IOException if the export fails
     */
    public static void exportWithDensityMap(ImageData<BufferedImage> imageData,
                                             DensityMapBuilder densityBuilder,
                                             RenderedExportConfig config,
                                             String entryName,
                                             int panelIndex) throws IOException {

        ImageServer<BufferedImage> baseServer = imageData.getServer();
        ImageServer<BufferedImage> displayServer = null;
        ImageServer<BufferedImage> densityServer = null;

        try {
            densityServer = densityBuilder.buildServer(imageData);
            displayServer = resolveDisplayServer(imageData, baseServer, config);

            String panelLabel = resolvePanelLabel(config, panelIndex);
            BufferedImage result = renderDensityMapComposite(
                    imageData, baseServer, densityServer, displayServer, config, panelLabel);

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
     * Exports one cropped rendered image per annotation in the image hierarchy.
     * Annotations can be filtered by classification. All three render modes
     * (classifier, object overlay, density map) are supported.
     *
     * @param imageData      the image data to export (caller is responsible for closing)
     * @param classifier     the pixel classifier (null for non-CLASSIFIER modes)
     * @param densityBuilder the density map builder (null for non-DENSITY_MAP modes)
     * @param config         rendered export configuration (must have regionType ALL_ANNOTATIONS)
     * @param entryName      the image entry name (used for filename generation)
     * @param panelIndex     zero-based starting index for auto-incrementing panel labels
     * @return the number of annotation regions successfully exported
     * @throws IOException if the export fails
     */
    public static int exportPerAnnotation(ImageData<BufferedImage> imageData,
                                           PixelClassifier classifier,
                                           DensityMapBuilder densityBuilder,
                                           RenderedExportConfig config,
                                           String entryName,
                                           int panelIndex) throws IOException {

        ImageServer<BufferedImage> baseServer = imageData.getServer();
        int serverW = baseServer.getWidth();
        int serverH = baseServer.getHeight();

        // Collect annotations and filter by classification
        Collection<PathObject> annotations = imageData.getHierarchy()
                .getAnnotationObjects().stream()
                .map(p -> (PathObject) p)
                .toList();

        var selectedClasses = config.getSelectedClassifications();
        if (selectedClasses != null) {
            Set<String> classSet = Set.copyOf(selectedClasses);
            annotations = annotations.stream()
                    .filter(a -> {
                        PathClass pc = a.getPathClass();
                        String name = (pc == null || pc == PathClass.NULL_CLASS)
                                ? "Unclassified" : pc.toString();
                        return classSet.contains(name);
                    })
                    .toList();
        }

        if (annotations.isEmpty()) {
            logger.warn("No matching annotations found for: {}", entryName);
            return 0;
        }

        // Set up servers for the render mode
        PixelClassificationImageServer classificationServer = null;
        ImageServer<BufferedImage> displayServer = null;
        ImageServer<BufferedImage> densityServer = null;

        try {
            displayServer = resolveDisplayServer(imageData, baseServer, config);

            if (config.getRenderMode() == RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY
                    && classifier != null) {
                if (!classifier.supportsImage(imageData)) {
                    throw new IllegalArgumentException(
                            "Classifier does not support image: " + entryName);
                }
                classificationServer = new PixelClassificationImageServer(imageData, classifier);
            } else if (config.getRenderMode() == RenderedExportConfig.RenderMode.DENSITY_MAP_OVERLAY
                    && densityBuilder != null) {
                densityServer = densityBuilder.buildServer(imageData);
            }

            // Per-class index counters for filename suffixes
            Map<String, AtomicInteger> classCounters = new LinkedHashMap<>();
            int padding = config.getPaddingPixels();
            int exported = 0;
            int annotationIndex = 0;

            for (PathObject annotation : annotations) {
                ROI roi = annotation.getROI();
                if (roi == null) continue;

                int x = (int) roi.getBoundsX();
                int y = (int) roi.getBoundsY();
                int w = (int) Math.ceil(roi.getBoundsWidth());
                int h = (int) Math.ceil(roi.getBoundsHeight());

                // Apply padding
                if (padding > 0) {
                    x -= padding;
                    y -= padding;
                    w += 2 * padding;
                    h += 2 * padding;
                }

                // Clamp to image bounds
                x = Math.max(0, x);
                y = Math.max(0, y);
                w = Math.min(w, serverW - x);
                h = Math.min(h, serverH - y);

                if (w <= 0 || h <= 0) continue;

                try {
                    String panelLabel = resolvePanelLabel(config, panelIndex + annotationIndex);
                    BufferedImage regionImage = renderRegion(
                            imageData, baseServer,
                            classificationServer, densityServer, displayServer,
                            config, x, y, w, h, panelLabel);

                    // Build classification-based filename suffix
                    PathClass pc = annotation.getPathClass();
                    String className = (pc == null || pc == PathClass.NULL_CLASS)
                            ? "Unclassified" : pc.toString();
                    String safeName = GeneralTools.stripInvalidFilenameChars(className);
                    if (safeName == null || safeName.isBlank()) safeName = "Unknown";

                    int idx = classCounters.computeIfAbsent(safeName,
                            k -> new AtomicInteger(0)).getAndIncrement();
                    String suffix = "_" + safeName + "_" + idx;

                    String filename = config.buildOutputFilename(entryName, suffix);
                    File outputFile = new File(config.getOutputDirectory(), filename);
                    ImageWriterTools.writeImage(regionImage, outputFile.getAbsolutePath());

                    logger.debug("Exported annotation region: {}", outputFile.getAbsolutePath());
                    exported++;
                    annotationIndex++;

                } catch (Exception e) {
                    logger.warn("Failed to export annotation for {}: {}",
                            entryName, e.getMessage());
                }
            }

            logger.info("Exported {} annotation regions for: {}", exported, entryName);
            return exported;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to export per-annotation images: " + entryName, e);
        } finally {
            closeQuietly(displayServer, entryName);
            closeQuietly(classificationServer, entryName);
            closeQuietly(densityServer, entryName);
        }
    }

    /**
     * Render a specific region of the image with overlays composited on top.
     * Handles all three render modes (classifier, object overlay, density map).
     */
    private static BufferedImage renderRegion(
            ImageData<BufferedImage> imageData,
            ImageServer<BufferedImage> baseServer,
            PixelClassificationImageServer classificationServer,
            ImageServer<BufferedImage> densityServer,
            ImageServer<BufferedImage> displayServer,
            RenderedExportConfig config,
            int x, int y, int w, int h,
            String panelLabel) throws Exception {

        double downsample = config.getDownsample();
        int outW = (int) Math.ceil(w / downsample);
        int outH = (int) Math.ceil(h / downsample);

        // Read base image region
        ImageServer<BufferedImage> readServer =
                displayServer != null ? displayServer : baseServer;
        RegionRequest request = RegionRequest.createInstance(
                readServer.getPath(), downsample, x, y, w, h);
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

        // Composite classifier or density map overlay
        if (classificationServer != null && opacity > 0) {
            RegionRequest classRequest = RegionRequest.createInstance(
                    classificationServer.getPath(), downsample, x, y, w, h);
            BufferedImage classImage = classificationServer.readRegion(classRequest);
            if (classImage != null) {
                g2d.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, opacity));
                g2d.drawImage(classImage,
                        0, 0, baseImage.getWidth(), baseImage.getHeight(), null);
            }
        } else if (densityServer != null && opacity > 0) {
            RegionRequest densityRequest = RegionRequest.createInstance(
                    densityServer.getPath(), downsample, x, y, w, h);
            BufferedImage densityImage = densityServer.readRegion(densityRequest);

            ColorMaps.ColorMap colorMap = resolveColorMap(config.getColormapName());
            double[] minMax = computeMinMax(densityImage);
            BufferedImage colorized = colorizeDensityMap(
                    densityImage, colorMap, minMax[0], minMax[1]);

            if (colorized != null) {
                g2d.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, opacity));
                g2d.drawImage(colorized,
                        0, 0, baseImage.getWidth(), baseImage.getHeight(), null);
            }
        }

        // Paint object overlays with region offset
        if (config.isIncludeAnnotations() || config.isIncludeDetections()) {
            g2d.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 1.0f));
            paintObjectsInRegion(g2d, imageData, downsample, x, y, w, h,
                    config.isIncludeAnnotations(), config.isIncludeDetections(),
                    config.isFillAnnotations(), config.isShowNames());
        }

        // Scale bar
        maybeDrawScaleBar(g2d, imageData, config,
                baseImage.getWidth(), baseImage.getHeight());

        // Color scale bar for density map mode
        if (densityServer != null && config.isShowColorScaleBar()) {
            ColorMaps.ColorMap colorMap = resolveColorMap(config.getColormapName());
            // Re-read density for min/max (already cached in memory)
            RegionRequest densityRequest = RegionRequest.createInstance(
                    densityServer.getPath(), downsample, x, y, w, h);
            BufferedImage densityImage = densityServer.readRegion(densityRequest);
            double[] minMax = computeMinMax(densityImage);
            maybeDrawColorScaleBar(g2d, config, colorMap, minMax[0], minMax[1],
                    baseImage.getWidth(), baseImage.getHeight());
        }

        maybeDrawPanelLabel(g2d, config, panelLabel,
                baseImage.getWidth(), baseImage.getHeight());

        g2d.dispose();
        return result;
    }

    /**
     * Paint object overlays onto a graphics context for a specific region.
     * The graphics origin is translated so objects are drawn at the correct offset.
     */
    private static void paintObjectsInRegion(Graphics2D g2d,
                                              ImageData<BufferedImage> imageData,
                                              double downsample,
                                              int regionX, int regionY,
                                              int regionW, int regionH,
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
            gCopy.translate(-regionX, -regionY);

            var region = ImageRegion.createInstance(
                    regionX, regionY, regionW, regionH, 0, 0);

            hierarchyOverlay.paintOverlay(gCopy, region, downsample, imageData, true);
            gCopy.dispose();
        } catch (Exception e) {
            logger.warn("Failed to paint objects in region: {}", e.getMessage());
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

        // For per-annotation mode, find the first matching annotation and preview its region
        if (config.getRegionType() == RenderedExportConfig.RegionType.ALL_ANNOTATIONS) {
            PathObject firstAnnotation = findFirstMatchingAnnotation(imageData, config);
            if (firstAnnotation != null && firstAnnotation.getROI() != null) {
                ROI roi = firstAnnotation.getROI();
                int x = (int) roi.getBoundsX();
                int y = (int) roi.getBoundsY();
                int w = (int) Math.ceil(roi.getBoundsWidth());
                int h = (int) Math.ceil(roi.getBoundsHeight());
                int padding = config.getPaddingPixels();
                if (padding > 0) {
                    x -= padding;
                    y -= padding;
                    w += 2 * padding;
                    h += 2 * padding;
                }
                x = Math.max(0, x);
                y = Math.max(0, y);
                w = Math.min(w, serverW - x);
                h = Math.min(h, serverH - y);

                if (w > 0 && h > 0) {
                    double longestSide = Math.max(w, h);
                    double previewDownsample = Math.max(config.getDownsample(),
                            longestSide / maxDimension);
                    RenderedExportConfig previewConfig = buildPreviewConfig(config, previewDownsample);
                    return renderRegionPreview(imageData, baseServer, classifier,
                            densityBuilder, previewConfig, x, y, w, h);
                }
            }
            // Fall through to whole-image preview if no annotation found
        }

        // Compute downsample to fit within maxDimension
        double longestSide = Math.max(serverW, serverH);
        double previewDownsample = Math.max(config.getDownsample(),
                longestSide / maxDimension);

        RenderedExportConfig previewConfig = buildPreviewConfig(config, previewDownsample);

        ImageServer<BufferedImage> displayServer = null;
        PixelClassificationImageServer classificationServer = null;
        ImageServer<BufferedImage> densityServer = null;

        try {
            displayServer = resolveDisplayServer(imageData, baseServer, previewConfig);

            // For preview, use fixed text from config or "A" as default
            String previewLabel = resolvePanelLabel(previewConfig, 0);

            if (config.getRenderMode() == RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY
                    && classifier != null) {
                classificationServer = new PixelClassificationImageServer(imageData, classifier);
                try {
                    return renderClassifierComposite(
                            imageData, baseServer, classificationServer, displayServer,
                            previewConfig, previewLabel);
                } catch (Exception e) {
                    throw new IOException("Failed to render classifier preview", e);
                }
            } else if (config.getRenderMode() == RenderedExportConfig.RenderMode.DENSITY_MAP_OVERLAY
                    && densityBuilder != null) {
                densityServer = densityBuilder.buildServer(imageData);
                try {
                    return renderDensityMapComposite(
                            imageData, baseServer, densityServer, displayServer,
                            previewConfig, previewLabel);
                } catch (Exception e) {
                    throw new IOException("Failed to render density map preview", e);
                }
            } else {
                try {
                    return renderObjectComposite(
                            imageData, baseServer, displayServer, previewConfig, previewLabel);
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
     * Build a preview config with the specified downsample, copying all other settings.
     */
    private static RenderedExportConfig buildPreviewConfig(RenderedExportConfig config,
                                                            double previewDownsample) {
        return new RenderedExportConfig.Builder()
                .regionType(config.getRegionType())
                .selectedClassifications(config.getSelectedClassifications())
                .paddingPixels(config.getPaddingPixels())
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
                .showPanelLabel(config.isShowPanelLabel())
                .panelLabelText(config.getPanelLabelText())
                .panelLabelPosition(config.getPanelLabelPosition())
                .panelLabelFontSize(config.getPanelLabelFontSize())
                .panelLabelBold(config.isPanelLabelBold())
                .build();
    }

    /**
     * Find the first annotation matching the classification filter.
     */
    private static PathObject findFirstMatchingAnnotation(ImageData<BufferedImage> imageData,
                                                           RenderedExportConfig config) {
        var annotations = imageData.getHierarchy().getAnnotationObjects();
        var selectedClasses = config.getSelectedClassifications();
        if (selectedClasses != null) {
            Set<String> classSet = Set.copyOf(selectedClasses);
            for (var a : annotations) {
                PathClass pc = a.getPathClass();
                String name = (pc == null || pc == PathClass.NULL_CLASS)
                        ? "Unclassified" : pc.toString();
                if (classSet.contains(name)) return a;
            }
            return null;
        }
        return annotations.isEmpty() ? null : annotations.iterator().next();
    }

    /**
     * Render a preview of a specific region using the renderRegion helper.
     */
    private static BufferedImage renderRegionPreview(
            ImageData<BufferedImage> imageData,
            ImageServer<BufferedImage> baseServer,
            PixelClassifier classifier,
            DensityMapBuilder densityBuilder,
            RenderedExportConfig config,
            int x, int y, int w, int h) throws IOException {

        PixelClassificationImageServer classificationServer = null;
        ImageServer<BufferedImage> displayServer = null;
        ImageServer<BufferedImage> densityServer = null;

        try {
            displayServer = resolveDisplayServer(imageData, baseServer, config);

            if (config.getRenderMode() == RenderedExportConfig.RenderMode.CLASSIFIER_OVERLAY
                    && classifier != null) {
                classificationServer = new PixelClassificationImageServer(imageData, classifier);
            } else if (config.getRenderMode() == RenderedExportConfig.RenderMode.DENSITY_MAP_OVERLAY
                    && densityBuilder != null) {
                densityServer = densityBuilder.buildServer(imageData);
            }

            String previewLabel = resolvePanelLabel(config, 0);
            return renderRegion(imageData, baseServer,
                    classificationServer, densityServer, displayServer,
                    config, x, y, w, h, previewLabel);

        } catch (Exception e) {
            throw new IOException("Failed to render per-annotation preview", e);
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
            RenderedExportConfig config,
            String panelLabel) throws Exception {

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

        maybeDrawPanelLabel(g2d, config, panelLabel,
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
            RenderedExportConfig config,
            String panelLabel) throws Exception {

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

        maybeDrawPanelLabel(g2d, config, panelLabel,
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
            RenderedExportConfig config,
            String panelLabel) throws Exception {

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

        maybeDrawPanelLabel(g2d, config, panelLabel,
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

    /**
     * Draw a panel label if enabled in the config.
     */
    private static void maybeDrawPanelLabel(Graphics2D g2d,
                                             RenderedExportConfig config,
                                             String label, int w, int h) {
        if (!config.isShowPanelLabel()) return;
        if (label == null || label.isEmpty()) return;
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        PanelLabelRenderer.drawPanelLabel(g2d, w, h, label,
                config.getPanelLabelPosition(),
                config.getPanelLabelFontSize(),
                config.isPanelLabelBold(),
                Color.WHITE);
    }

    /**
     * Resolve the panel label text from config and panel index.
     * If config has fixed text, use it. Otherwise auto-increment from index.
     */
    private static String resolvePanelLabel(RenderedExportConfig config, int panelIndex) {
        if (!config.isShowPanelLabel()) return null;
        String text = config.getPanelLabelText();
        if (text != null && !text.isBlank()) {
            return text;
        }
        return PanelLabelRenderer.labelForIndex(panelIndex);
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
