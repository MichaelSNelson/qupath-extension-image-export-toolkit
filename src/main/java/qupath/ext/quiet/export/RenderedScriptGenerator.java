package qupath.ext.quiet.export;

import com.google.gson.Gson;
import qupath.ext.quiet.export.RenderedExportConfig;
import qupath.ext.quiet.export.RenderedExportConfig.DisplaySettingsMode;

import static qupath.ext.quiet.export.ScriptGenerator.appendLine;
import static qupath.ext.quiet.export.ScriptGenerator.quote;

/**
 * Generates self-contained Groovy scripts for rendered image export.
 */
class RenderedScriptGenerator {

    private static final Gson GSON = new Gson();

    private RenderedScriptGenerator() {
        // Utility class
    }

    static String generate(RenderedExportConfig config) {
        if (config.getRenderMode() == RenderedExportConfig.RenderMode.OBJECT_OVERLAY) {
            return generateObjectOverlayScript(config);
        }
        return generateClassifierScript(config);
    }

    /**
     * Serialize display settings to JSON for embedding in scripts.
     * Returns null if no settings are captured.
     */
    private static String serializeDisplaySettings(RenderedExportConfig config) {
        var settings = config.getCapturedDisplaySettings();
        if (settings == null) return null;
        return GSON.toJson(settings);
    }

    /**
     * Emit display-related imports (only for non-RAW modes).
     */
    private static void emitDisplayImports(StringBuilder sb, DisplaySettingsMode mode) {
        if (mode == DisplaySettingsMode.RAW) return;
        appendLine(sb, "import qupath.lib.display.ImageDisplay");
        appendLine(sb, "import qupath.lib.display.settings.DisplaySettingUtils");
        appendLine(sb, "import qupath.lib.gui.images.servers.ChannelDisplayTransformServer");
        if (mode == DisplaySettingsMode.CURRENT_VIEWER) {
            appendLine(sb, "import com.google.gson.JsonParser");
        }
    }

    /**
     * Emit display settings configuration variables.
     */
    private static void emitDisplayConfig(StringBuilder sb, RenderedExportConfig config) {
        var mode = config.getDisplaySettingsMode();
        appendLine(sb, "def displaySettingsMode = " + quote(mode.name()));
        if (mode == DisplaySettingsMode.CURRENT_VIEWER) {
            String json = serializeDisplaySettings(config);
            appendLine(sb, "def displaySettingsJson = " + (json != null ? quote(json) : "null"));
        } else if (mode == DisplaySettingsMode.SAVED_PRESET) {
            String presetName = config.getDisplayPresetName();
            appendLine(sb, "def displayPresetName = " + quote(presetName != null ? presetName : ""));
        }
    }

    /**
     * Emit display settings resolution code (after project is loaded, before the loop).
     */
    private static void emitDisplaySetup(StringBuilder sb, DisplaySettingsMode mode) {
        if (mode == DisplaySettingsMode.RAW) return;

        appendLine(sb, "// Resolve display settings");
        appendLine(sb, "def displaySettings = null");
        if (mode == DisplaySettingsMode.CURRENT_VIEWER) {
            appendLine(sb, "if (displaySettingsJson != null) {");
            appendLine(sb, "    def jsonElement = JsonParser.parseString(displaySettingsJson)");
            appendLine(sb, "    displaySettings = DisplaySettingUtils.parseDisplaySettings(jsonElement).orElse(null)");
            appendLine(sb, "}");
        } else if (mode == DisplaySettingsMode.SAVED_PRESET) {
            appendLine(sb, "def presetManager = DisplaySettingUtils.getResourcesForProject(project)");
            appendLine(sb, "displaySettings = presetManager.get(displayPresetName)");
            appendLine(sb, "if (displaySettings == null) {");
            appendLine(sb, "    println \"WARNING: Display preset not found: ${displayPresetName}\"");
            appendLine(sb, "}");
        }
        appendLine(sb, "");
    }

    // ------------------------------------------------------------------
    // Scale bar helpers
    // ------------------------------------------------------------------

    /**
     * Emit scale bar imports (only when scale bar is enabled).
     */
    private static void emitScaleBarImports(StringBuilder sb) {
        appendLine(sb, "import java.awt.Color");
        appendLine(sb, "import java.awt.Font");
        appendLine(sb, "import java.awt.FontMetrics");
    }

    /**
     * Emit scale bar configuration variables.
     */
    private static void emitScaleBarConfig(StringBuilder sb, RenderedExportConfig config) {
        appendLine(sb, "def showScaleBar = " + config.isShowScaleBar());
        appendLine(sb, "def scaleBarPosition = " + quote(config.getScaleBarPosition().name()));

        // Parse hex color into RGB components for the Groovy script
        java.awt.Color awtColor = config.getScaleBarColorAsAwt();
        appendLine(sb, "def scaleBarColorR = " + awtColor.getRed());
        appendLine(sb, "def scaleBarColorG = " + awtColor.getGreen());
        appendLine(sb, "def scaleBarColorB = " + awtColor.getBlue());
        appendLine(sb, "def scaleBarFontSize = " + config.getScaleBarFontSize());
        appendLine(sb, "def scaleBarBoldText = " + config.isScaleBarBoldText());
    }

    /**
     * Emit a self-contained drawScaleBar Groovy function.
     * Accepts RGB ints, font size (0 = auto), and bold flag.
     */
    private static void emitScaleBarFunction(StringBuilder sb) {
        appendLine(sb, "// Scale bar drawing function");
        appendLine(sb, "def drawScaleBar(Graphics2D g2d, int imgW, int imgH, double pxSize, String pos, int colR, int colG, int colB, int fSize, boolean bold) {");
        appendLine(sb, "    if (pxSize <= 0) return");
        appendLine(sb, "    def niceLengths = [0.1, 0.25, 0.5, 1, 2, 5, 10, 20, 50, 100, 200, 250, 500, 1000, 2000, 5000, 10000, 20000, 50000]");
        appendLine(sb, "    double target = imgW * pxSize * 0.15");
        appendLine(sb, "    double barUm = niceLengths.min { Math.abs(it - target) }");
        appendLine(sb, "    int barPx = (int) Math.round(barUm / pxSize)");
        appendLine(sb, "    if (barPx < 2) return");
        appendLine(sb, "    int barH = Math.max(4, imgH / 150)");
        appendLine(sb, "    int minDim = Math.min(imgW, imgH)");
        appendLine(sb, "    int fontSize = fSize > 0 ? Math.max(4, Math.min(fSize, 200)) : Math.max(12, minDim / 50)");
        appendLine(sb, "    int margin = Math.max(10, minDim / 40)");
        appendLine(sb, "    String label = barUm >= 1000 ? String.format('%d mm', (int)(barUm / 1000)) : (barUm == Math.floor(barUm) ? String.format('%d um', (int)barUm) : String.format('%.1f um', barUm))");
        appendLine(sb, "    int fontStyle = bold ? Font.BOLD : Font.PLAIN");
        appendLine(sb, "    g2d.setFont(new Font(Font.SANS_SERIF, fontStyle, fontSize))");
        appendLine(sb, "    def fm = g2d.getFontMetrics()");
        appendLine(sb, "    int tw = fm.stringWidth(label)");
        appendLine(sb, "    int th = fm.getAscent()");
        appendLine(sb, "    int bx, by");
        appendLine(sb, "    switch (pos) {");
        appendLine(sb, "        case 'LOWER_LEFT': bx = margin; by = imgH - margin - barH; break");
        appendLine(sb, "        case 'UPPER_RIGHT': bx = imgW - margin - barPx; by = margin + th + 4; break");
        appendLine(sb, "        case 'UPPER_LEFT': bx = margin; by = margin + th + 4; break");
        appendLine(sb, "        default: bx = imgW - margin - barPx; by = imgH - margin - barH; break");
        appendLine(sb, "    }");
        appendLine(sb, "    int tx = bx + (barPx - tw) / 2");
        appendLine(sb, "    int ty = by - 4");
        appendLine(sb, "    def primary = new Color(colR, colG, colB)");
        appendLine(sb, "    double lum = (0.299 * colR + 0.587 * colG + 0.114 * colB) / 255.0");
        appendLine(sb, "    def outline = lum > 0.5 ? Color.BLACK : Color.WHITE");
        appendLine(sb, "    g2d.setColor(outline)");
        appendLine(sb, "    g2d.fillRect(bx - 1, by - 1, barPx + 2, barH + 2)");
        appendLine(sb, "    g2d.setColor(primary)");
        appendLine(sb, "    g2d.fillRect(bx, by, barPx, barH)");
        appendLine(sb, "    for (int dx = -1; dx <= 1; dx++) {");
        appendLine(sb, "        for (int dy = -1; dy <= 1; dy++) {");
        appendLine(sb, "            if (dx != 0 || dy != 0) { g2d.setColor(outline); g2d.drawString(label, tx + dx, ty + dy) }");
        appendLine(sb, "        }");
        appendLine(sb, "    }");
        appendLine(sb, "    g2d.setColor(primary)");
        appendLine(sb, "    g2d.drawString(label, tx, ty)");
        appendLine(sb, "}");
        appendLine(sb, "");
    }

    /**
     * Emit per-image scale bar drawing code (after overlay painting, before g2d.dispose).
     * Expects variables: baseServer, imageData, g2d, downsample, showScaleBar,
     * scaleBarPosition, scaleBarColorR/G/B, scaleBarFontSize, scaleBarBoldText
     * and outW, outH (output image dimensions).
     */
    private static void emitScaleBarDrawing(StringBuilder sb) {
        appendLine(sb, "        if (showScaleBar) {");
        appendLine(sb, "            def cal = imageData.getServer().getPixelCalibration()");
        appendLine(sb, "            if (cal.hasPixelSizeMicrons()) {");
        appendLine(sb, "                double pxSize = cal.getAveragedPixelSizeMicrons() * downsample");
        appendLine(sb, "                g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f))");
        appendLine(sb, "                drawScaleBar(g2d, outW, outH, pxSize, scaleBarPosition, scaleBarColorR, scaleBarColorG, scaleBarColorB, scaleBarFontSize, scaleBarBoldText)");
        appendLine(sb, "            } else {");
        appendLine(sb, "                println \"  WARNING: Scale bar skipped -- no pixel calibration\"");
        appendLine(sb, "            }");
        appendLine(sb, "        }");
    }

    /**
     * Emit per-image display server wrapping code (inside the try block, after baseServer).
     * Sets up readServer variable that should be used instead of baseServer for image reads.
     */
    private static void emitDisplayServerWrapping(StringBuilder sb, DisplaySettingsMode mode) {
        if (mode == DisplaySettingsMode.RAW) {
            appendLine(sb, "        def readServer = baseServer");
            return;
        }

        appendLine(sb, "        // Apply display settings (brightness/contrast, channel visibility)");
        appendLine(sb, "        def display = ImageDisplay.create(imageData)");
        if (mode != DisplaySettingsMode.PER_IMAGE_SAVED) {
            appendLine(sb, "        if (displaySettings != null) {");
            appendLine(sb, "            DisplaySettingUtils.applySettingsToDisplay(display, displaySettings)");
            appendLine(sb, "        }");
        }
        appendLine(sb, "        def readServer = ChannelDisplayTransformServer.createColorTransformServer(");
        appendLine(sb, "                baseServer, display.selectedChannels())");
    }

    private static String generateClassifierScript(RenderedExportConfig config) {
        var sb = new StringBuilder();
        var displayMode = config.getDisplaySettingsMode();

        appendLine(sb, "/**");
        appendLine(sb, " * Classifier Overlay Export Script");
        appendLine(sb, " * Generated by QuIET (QuPath Image Export Toolkit)");
        appendLine(sb, " *");
        appendLine(sb, " * Exports all project images with a pixel classifier overlay");
        appendLine(sb, " * rendered on top, at the specified downsample and opacity.");
        appendLine(sb, " *");
        appendLine(sb, " * Parameters below can be modified before re-running.");
        appendLine(sb, " */");
        appendLine(sb, "");

        // Imports
        appendLine(sb, "import qupath.lib.classifiers.pixel.PixelClassificationImageServer");
        appendLine(sb, "import qupath.lib.gui.viewer.OverlayOptions");
        appendLine(sb, "import qupath.lib.gui.viewer.overlays.HierarchyOverlay");
        appendLine(sb, "import qupath.lib.images.writers.ImageWriterTools");
        appendLine(sb, "import qupath.lib.regions.RegionRequest");
        appendLine(sb, "import java.awt.AlphaComposite");
        appendLine(sb, "import java.awt.Graphics2D");
        appendLine(sb, "import java.awt.RenderingHints");
        appendLine(sb, "import java.awt.image.BufferedImage");
        emitDisplayImports(sb, displayMode);
        if (config.isShowScaleBar()) {
            emitScaleBarImports(sb);
        }
        appendLine(sb, "");

        // Configuration parameters
        appendLine(sb, "// ========== CONFIGURATION (modify as needed) ==========");
        appendLine(sb, "def classifierName = " + quote(config.getClassifierName()));
        emitDisplayConfig(sb, config);
        appendLine(sb, "def overlayOpacity = " + config.getOverlayOpacity());
        appendLine(sb, "def downsample = " + config.getDownsample());
        appendLine(sb, "def outputFormat = " + quote(config.getFormat().getExtension()));
        appendLine(sb, "def outputDir = " + quote(config.getOutputDirectory().getAbsolutePath()));
        appendLine(sb, "def includeAnnotations = " + config.isIncludeAnnotations());
        appendLine(sb, "def includeDetections = " + config.isIncludeDetections());
        appendLine(sb, "def fillAnnotations = " + config.isFillAnnotations());
        appendLine(sb, "def showNames = " + config.isShowNames());
        emitScaleBarConfig(sb, config);
        appendLine(sb, "// =======================================================");
        appendLine(sb, "");

        // Project and classifier loading
        appendLine(sb, "def project = getProject()");
        appendLine(sb, "if (project == null) {");
        appendLine(sb, "    println 'ERROR: No project is open'");
        appendLine(sb, "    return");
        appendLine(sb, "}");
        appendLine(sb, "");
        appendLine(sb, "def classifier = project.getPixelClassifiers().get(classifierName)");
        appendLine(sb, "if (classifier == null) {");
        appendLine(sb, "    println \"ERROR: Classifier not found: ${classifierName}\"");
        appendLine(sb, "    return");
        appendLine(sb, "}");
        appendLine(sb, "");

        // Display settings resolution
        emitDisplaySetup(sb, displayMode);

        // Output directory
        appendLine(sb, "def outDir = new File(outputDir)");
        appendLine(sb, "outDir.mkdirs()");
        appendLine(sb, "");

        // Scale bar function (before the main loop)
        if (config.isShowScaleBar()) {
            emitScaleBarFunction(sb);
        }

        // Main processing loop
        appendLine(sb, "def entries = project.getImageList()");
        appendLine(sb, "println \"Processing ${entries.size()} images...\"");
        appendLine(sb, "");
        appendLine(sb, "int succeeded = 0");
        appendLine(sb, "int failed = 0");
        appendLine(sb, "int skipped = 0");
        appendLine(sb, "");
        appendLine(sb, "for (int i = 0; i < entries.size(); i++) {");
        appendLine(sb, "    def entry = entries[i]");
        appendLine(sb, "    def entryName = entry.getImageName()");
        appendLine(sb, "    println \"[${i + 1}/${entries.size()}] Processing: ${entryName}\"");
        appendLine(sb, "");
        appendLine(sb, "    def classServer = null");
        appendLine(sb, "    try {");
        appendLine(sb, "        def imageData = entry.readImageData()");
        appendLine(sb, "        def baseServer = imageData.getServer()");
        appendLine(sb, "");
        appendLine(sb, "        if (!classifier.supportsImage(imageData)) {");
        appendLine(sb, "            println \"  SKIP: Classifier does not support this image\"");
        appendLine(sb, "            baseServer.close()");
        appendLine(sb, "            skipped++");
        appendLine(sb, "            continue");
        appendLine(sb, "        }");
        appendLine(sb, "");

        // Display server wrapping
        emitDisplayServerWrapping(sb, displayMode);
        appendLine(sb, "");

        appendLine(sb, "        classServer = new PixelClassificationImageServer(imageData, classifier)");
        appendLine(sb, "");
        appendLine(sb, "        int outW = (int) Math.ceil(baseServer.getWidth() / downsample)");
        appendLine(sb, "        int outH = (int) Math.ceil(baseServer.getHeight() / downsample)");
        appendLine(sb, "");
        appendLine(sb, "        def request = RegionRequest.createInstance(");
        appendLine(sb, "                readServer.getPath(), downsample,");
        appendLine(sb, "                0, 0, readServer.getWidth(), readServer.getHeight())");
        appendLine(sb, "        def baseImage = readServer.readRegion(request)");
        appendLine(sb, "");
        appendLine(sb, "        def classRequest = RegionRequest.createInstance(");
        appendLine(sb, "                classServer.getPath(), downsample,");
        appendLine(sb, "                0, 0, classServer.getWidth(), classServer.getHeight())");
        appendLine(sb, "        def classImage = classServer.readRegion(classRequest)");
        appendLine(sb, "");
        appendLine(sb, "        def result = new BufferedImage(");
        appendLine(sb, "                baseImage.getWidth(), baseImage.getHeight(),");
        appendLine(sb, "                BufferedImage.TYPE_INT_RGB)");
        appendLine(sb, "        def g2d = result.createGraphics()");
        appendLine(sb, "        g2d.setRenderingHint(");
        appendLine(sb, "                RenderingHints.KEY_INTERPOLATION,");
        appendLine(sb, "                RenderingHints.VALUE_INTERPOLATION_BILINEAR)");
        appendLine(sb, "");
        appendLine(sb, "        g2d.drawImage(baseImage, 0, 0, null)");
        appendLine(sb, "");
        appendLine(sb, "        if (overlayOpacity > 0 && classImage != null) {");
        appendLine(sb, "            g2d.setComposite(AlphaComposite.getInstance(");
        appendLine(sb, "                    AlphaComposite.SRC_OVER, (float) overlayOpacity))");
        appendLine(sb, "            g2d.drawImage(classImage,");
        appendLine(sb, "                    0, 0, baseImage.getWidth(), baseImage.getHeight(), null)");
        appendLine(sb, "        }");
        appendLine(sb, "");
        appendLine(sb, "        if (includeAnnotations || includeDetections) {");
        appendLine(sb, "            g2d.setComposite(AlphaComposite.getInstance(");
        appendLine(sb, "                    AlphaComposite.SRC_OVER, 1.0f))");
        appendLine(sb, "            def overlayOptions = new OverlayOptions()");
        appendLine(sb, "            overlayOptions.setShowAnnotations(includeAnnotations)");
        appendLine(sb, "            overlayOptions.setShowDetections(includeDetections)");
        appendLine(sb, "            overlayOptions.setFillAnnotations(fillAnnotations)");
        appendLine(sb, "            overlayOptions.setShowNames(showNames)");
        appendLine(sb, "            def hierOverlay = new HierarchyOverlay(null, overlayOptions, imageData)");
        appendLine(sb, "            def gCopy = (Graphics2D) g2d.create()");
        appendLine(sb, "            gCopy.scale(1.0 / downsample, 1.0 / downsample)");
        appendLine(sb, "            def region = qupath.lib.regions.ImageRegion.createInstance(");
        appendLine(sb, "                    0, 0, baseServer.getWidth(), baseServer.getHeight(), 0, 0)");
        appendLine(sb, "            hierOverlay.paintOverlay(gCopy, region, downsample, imageData, true)");
        appendLine(sb, "            gCopy.dispose()");
        appendLine(sb, "        }");
        appendLine(sb, "");

        // Scale bar drawing
        if (config.isShowScaleBar()) {
            emitScaleBarDrawing(sb);
            appendLine(sb, "");
        }

        appendLine(sb, "        g2d.dispose()");
        appendLine(sb, "");
        appendLine(sb, "        def sanitized = entryName.replaceAll('[^a-zA-Z0-9._\\\\-]', '_')");
        appendLine(sb, "        def outputPath = new File(outDir, sanitized + '.' + outputFormat).getAbsolutePath()");
        appendLine(sb, "        ImageWriterTools.writeImage(result, outputPath)");
        appendLine(sb, "        println \"  OK: ${outputPath}\"");
        appendLine(sb, "        succeeded++");
        appendLine(sb, "");
        appendLine(sb, "        classServer.close()");
        appendLine(sb, "        classServer = null");
        appendLine(sb, "        baseServer.close()");
        appendLine(sb, "");
        appendLine(sb, "    } catch (Exception e) {");
        appendLine(sb, "        println \"  FAIL: ${e.getMessage()}\"");
        appendLine(sb, "        failed++");
        appendLine(sb, "        if (classServer != null) {");
        appendLine(sb, "            try { classServer.close() } catch (Exception ignored) {}");
        appendLine(sb, "        }");
        appendLine(sb, "    }");
        appendLine(sb, "}");
        appendLine(sb, "");
        appendLine(sb, "println ''");
        appendLine(sb, "println \"Export complete: ${succeeded} succeeded, ${skipped} skipped, ${failed} failed\"");

        return sb.toString();
    }

    private static String generateObjectOverlayScript(RenderedExportConfig config) {
        var sb = new StringBuilder();
        var displayMode = config.getDisplaySettingsMode();

        appendLine(sb, "/**");
        appendLine(sb, " * Object Overlay Export Script");
        appendLine(sb, " * Generated by QuIET (QuPath Image Export Toolkit)");
        appendLine(sb, " *");
        appendLine(sb, " * Exports all project images with object overlays");
        appendLine(sb, " * (annotations and/or detections) rendered on top.");
        appendLine(sb, " *");
        appendLine(sb, " * Parameters below can be modified before re-running.");
        appendLine(sb, " */");
        appendLine(sb, "");

        // Imports
        appendLine(sb, "import qupath.lib.gui.viewer.OverlayOptions");
        appendLine(sb, "import qupath.lib.gui.viewer.overlays.HierarchyOverlay");
        appendLine(sb, "import qupath.lib.images.writers.ImageWriterTools");
        appendLine(sb, "import qupath.lib.regions.RegionRequest");
        appendLine(sb, "import java.awt.AlphaComposite");
        appendLine(sb, "import java.awt.Graphics2D");
        appendLine(sb, "import java.awt.RenderingHints");
        appendLine(sb, "import java.awt.image.BufferedImage");
        emitDisplayImports(sb, displayMode);
        if (config.isShowScaleBar()) {
            emitScaleBarImports(sb);
        }
        appendLine(sb, "");

        // Configuration parameters
        appendLine(sb, "// ========== CONFIGURATION (modify as needed) ==========");
        emitDisplayConfig(sb, config);
        appendLine(sb, "def overlayOpacity = " + config.getOverlayOpacity());
        appendLine(sb, "def downsample = " + config.getDownsample());
        appendLine(sb, "def outputFormat = " + quote(config.getFormat().getExtension()));
        appendLine(sb, "def outputDir = " + quote(config.getOutputDirectory().getAbsolutePath()));
        appendLine(sb, "def includeAnnotations = " + config.isIncludeAnnotations());
        appendLine(sb, "def includeDetections = " + config.isIncludeDetections());
        appendLine(sb, "def fillAnnotations = " + config.isFillAnnotations());
        appendLine(sb, "def showNames = " + config.isShowNames());
        emitScaleBarConfig(sb, config);
        appendLine(sb, "// =======================================================");
        appendLine(sb, "");

        // Project loading
        appendLine(sb, "def project = getProject()");
        appendLine(sb, "if (project == null) {");
        appendLine(sb, "    println 'ERROR: No project is open'");
        appendLine(sb, "    return");
        appendLine(sb, "}");
        appendLine(sb, "");

        // Display settings resolution
        emitDisplaySetup(sb, displayMode);

        // Output directory
        appendLine(sb, "def outDir = new File(outputDir)");
        appendLine(sb, "outDir.mkdirs()");
        appendLine(sb, "");

        // Scale bar function (before the main loop)
        if (config.isShowScaleBar()) {
            emitScaleBarFunction(sb);
        }

        // Main processing loop
        appendLine(sb, "def entries = project.getImageList()");
        appendLine(sb, "println \"Processing ${entries.size()} images...\"");
        appendLine(sb, "");
        appendLine(sb, "int succeeded = 0");
        appendLine(sb, "int failed = 0");
        appendLine(sb, "");
        appendLine(sb, "for (int i = 0; i < entries.size(); i++) {");
        appendLine(sb, "    def entry = entries[i]");
        appendLine(sb, "    def entryName = entry.getImageName()");
        appendLine(sb, "    println \"[${i + 1}/${entries.size()}] Processing: ${entryName}\"");
        appendLine(sb, "");
        appendLine(sb, "    try {");
        appendLine(sb, "        def imageData = entry.readImageData()");
        appendLine(sb, "        def baseServer = imageData.getServer()");
        appendLine(sb, "");

        // Display server wrapping
        emitDisplayServerWrapping(sb, displayMode);
        appendLine(sb, "");

        appendLine(sb, "        int outW = (int) Math.ceil(baseServer.getWidth() / downsample)");
        appendLine(sb, "        int outH = (int) Math.ceil(baseServer.getHeight() / downsample)");
        appendLine(sb, "");
        appendLine(sb, "        def request = RegionRequest.createInstance(");
        appendLine(sb, "                readServer.getPath(), downsample,");
        appendLine(sb, "                0, 0, readServer.getWidth(), readServer.getHeight())");
        appendLine(sb, "        def baseImage = readServer.readRegion(request)");
        appendLine(sb, "");
        appendLine(sb, "        def result = new BufferedImage(");
        appendLine(sb, "                baseImage.getWidth(), baseImage.getHeight(),");
        appendLine(sb, "                BufferedImage.TYPE_INT_RGB)");
        appendLine(sb, "        def g2d = result.createGraphics()");
        appendLine(sb, "        g2d.setRenderingHint(");
        appendLine(sb, "                RenderingHints.KEY_INTERPOLATION,");
        appendLine(sb, "                RenderingHints.VALUE_INTERPOLATION_BILINEAR)");
        appendLine(sb, "");
        appendLine(sb, "        g2d.drawImage(baseImage, 0, 0, null)");
        appendLine(sb, "");
        appendLine(sb, "        if (overlayOpacity > 0) {");
        appendLine(sb, "            g2d.setComposite(AlphaComposite.getInstance(");
        appendLine(sb, "                    AlphaComposite.SRC_OVER, (float) overlayOpacity))");
        appendLine(sb, "            def overlayOptions = new OverlayOptions()");
        appendLine(sb, "            overlayOptions.setShowAnnotations(includeAnnotations)");
        appendLine(sb, "            overlayOptions.setShowDetections(includeDetections)");
        appendLine(sb, "            overlayOptions.setFillAnnotations(fillAnnotations)");
        appendLine(sb, "            overlayOptions.setShowNames(showNames)");
        appendLine(sb, "            def hierOverlay = new HierarchyOverlay(null, overlayOptions, imageData)");
        appendLine(sb, "            def gCopy = (Graphics2D) g2d.create()");
        appendLine(sb, "            gCopy.scale(1.0 / downsample, 1.0 / downsample)");
        appendLine(sb, "            def region = qupath.lib.regions.ImageRegion.createInstance(");
        appendLine(sb, "                    0, 0, baseServer.getWidth(), baseServer.getHeight(), 0, 0)");
        appendLine(sb, "            hierOverlay.paintOverlay(gCopy, region, downsample, imageData, true)");
        appendLine(sb, "            gCopy.dispose()");
        appendLine(sb, "        }");
        appendLine(sb, "");

        // Scale bar drawing
        if (config.isShowScaleBar()) {
            emitScaleBarDrawing(sb);
            appendLine(sb, "");
        }

        appendLine(sb, "        g2d.dispose()");
        appendLine(sb, "");
        appendLine(sb, "        def sanitized = entryName.replaceAll('[^a-zA-Z0-9._\\\\-]', '_')");
        appendLine(sb, "        def outputPath = new File(outDir, sanitized + '.' + outputFormat).getAbsolutePath()");
        appendLine(sb, "        ImageWriterTools.writeImage(result, outputPath)");
        appendLine(sb, "        println \"  OK: ${outputPath}\"");
        appendLine(sb, "        succeeded++");
        appendLine(sb, "");
        appendLine(sb, "        baseServer.close()");
        appendLine(sb, "");
        appendLine(sb, "    } catch (Exception e) {");
        appendLine(sb, "        println \"  FAIL: ${e.getMessage()}\"");
        appendLine(sb, "        failed++");
        appendLine(sb, "    }");
        appendLine(sb, "}");
        appendLine(sb, "");
        appendLine(sb, "println ''");
        appendLine(sb, "println \"Export complete: ${succeeded} succeeded, ${failed} failed\"");

        return sb.toString();
    }
}
