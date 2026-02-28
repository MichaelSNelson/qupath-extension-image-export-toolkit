package qupath.ext.quiet.export;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.lib.display.ImageDisplay;
import qupath.lib.display.settings.DisplaySettingUtils;
import qupath.lib.display.settings.ImageDisplaySettings;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.PixelCalibration;
import qupath.lib.projects.ProjectImageEntry;
import qupath.lib.regions.RegionRequest;

/**
 * Scans images at low resolution to compute per-channel global display ranges
 * with percentile-based saturation clipping.
 * <p>
 * Used by the GLOBAL_MATCHED display settings mode to ensure all images
 * in a batch share identical brightness/contrast per channel.
 */
public class GlobalDisplayRangeScanner {

    private static final Logger logger = LoggerFactory.getLogger(GlobalDisplayRangeScanner.class);

    /** Default scan downsample for fast reading. */
    private static final double DEFAULT_SCAN_DOWNSAMPLE = 32.0;

    /** Number of histogram bins for float-type images. */
    private static final int FLOAT_HISTOGRAM_BINS = 10000;

    /**
     * Per-channel computed display range.
     */
    public record ChannelRange(String name, int color, double minDisplay, double maxDisplay) {}

    private GlobalDisplayRangeScanner() {
        // Utility class
    }

    /**
     * Scan all entries and compute global per-channel percentile-based ranges.
     *
     * @param entries          project images to scan
     * @param percentile       saturation percentile (e.g., 0.1 for 0.1% clip at each end)
     * @param scanDownsample   downsample for fast scanning (e.g., 32.0)
     * @param progressCallback optional (index, total) callback for UI progress
     * @return per-channel ranges ordered by channel index
     */
    public static List<ChannelRange> computeGlobalRanges(
            List<ProjectImageEntry<BufferedImage>> entries,
            double percentile, double scanDownsample,
            BiConsumer<Integer, Integer> progressCallback) {

        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        // Get channel info from the first entry
        int nChannels;
        String[] channelNames;
        int[] channelColors;
        boolean isFloatingPoint;

        try {
            var firstData = entries.get(0).readImageData();
            var firstServer = firstData.getServer();
            var metadata = firstServer.getMetadata();
            nChannels = metadata.getChannels().size();
            channelNames = new String[nChannels];
            channelColors = new int[nChannels];
            for (int c = 0; c < nChannels; c++) {
                channelNames[c] = metadata.getChannels().get(c).getName();
                channelColors[c] = metadata.getChannels().get(c).getColor();
            }
            isFloatingPoint = firstServer.getPixelType().isFloatingPoint();
            firstServer.close();
        } catch (Exception e) {
            logger.error("Failed to read first image for channel info: {}", e.getMessage());
            return List.of();
        }

        if (isFloatingPoint) {
            return computeFloatRanges(entries, nChannels, channelNames, channelColors,
                    percentile, scanDownsample, progressCallback);
        } else {
            return computeIntegerRanges(entries, nChannels, channelNames, channelColors,
                    percentile, scanDownsample, progressCallback);
        }
    }

    /**
     * Compute ranges for integer-type images using exact histograms.
     * Works well for 8-bit (256 bins) and 16-bit (65536 bins).
     */
    private static List<ChannelRange> computeIntegerRanges(
            List<ProjectImageEntry<BufferedImage>> entries,
            int nChannels, String[] channelNames, int[] channelColors,
            double percentile, double scanDownsample,
            BiConsumer<Integer, Integer> progressCallback) {

        // Determine bit depth from first image to size histograms
        int histogramSize = 65536; // default for 16-bit
        try {
            var firstData = entries.get(0).readImageData();
            var server = firstData.getServer();
            int bitsPerPixel = server.getPixelType().getBitsPerPixel();
            if (bitsPerPixel <= 8) {
                histogramSize = 256;
            }
            server.close();
        } catch (Exception e) {
            // Fall back to 65536
        }

        // Per-channel histograms
        long[][] histograms = new long[nChannels][histogramSize];

        int total = entries.size();
        for (int i = 0; i < total; i++) {
            if (progressCallback != null) {
                progressCallback.accept(i, total);
            }

            try {
                var imageData = entries.get(i).readImageData();
                var server = imageData.getServer();
                double ds = Math.max(scanDownsample, 1.0);
                RegionRequest request = RegionRequest.createInstance(
                        server.getPath(), ds, 0, 0, server.getWidth(), server.getHeight());
                BufferedImage img = server.readRegion(request);
                Raster raster = img.getRaster();

                int w = raster.getWidth();
                int h = raster.getHeight();
                int bands = Math.min(raster.getNumBands(), nChannels);

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        for (int c = 0; c < bands; c++) {
                            int val = raster.getSample(x, y, c);
                            if (val >= 0 && val < histogramSize) {
                                histograms[c][val]++;
                            }
                        }
                    }
                }

                server.close();
            } catch (Exception e) {
                logger.warn("Failed to scan image {} for display ranges: {}",
                        entries.get(i).getImageName(), e.getMessage());
            }
        }

        // Compute percentile-based min/max from histograms
        List<ChannelRange> ranges = new ArrayList<>();
        for (int c = 0; c < nChannels; c++) {
            long totalPixels = 0;
            for (long count : histograms[c]) totalPixels += count;

            if (totalPixels == 0) {
                ranges.add(new ChannelRange(channelNames[c], channelColors[c], 0, histogramSize - 1));
                continue;
            }

            double clipCount = totalPixels * percentile / 100.0;
            double minVal = findPercentileFromHistogram(histograms[c], clipCount, true);
            double maxVal = findPercentileFromHistogram(histograms[c], clipCount, false);

            if (minVal >= maxVal) {
                maxVal = minVal + 1;
            }

            ranges.add(new ChannelRange(channelNames[c], channelColors[c], minVal, maxVal));
        }

        return ranges;
    }

    /**
     * Compute ranges for float-type images using a two-pass approach:
     * pass 1 finds global min/max, pass 2 builds a binned histogram.
     */
    private static List<ChannelRange> computeFloatRanges(
            List<ProjectImageEntry<BufferedImage>> entries,
            int nChannels, String[] channelNames, int[] channelColors,
            double percentile, double scanDownsample,
            BiConsumer<Integer, Integer> progressCallback) {

        // Pass 1: find global min/max per channel
        double[] globalMin = new double[nChannels];
        double[] globalMax = new double[nChannels];
        for (int c = 0; c < nChannels; c++) {
            globalMin[c] = Double.MAX_VALUE;
            globalMax[c] = -Double.MAX_VALUE;
        }

        int total = entries.size();
        for (int i = 0; i < total; i++) {
            if (progressCallback != null) {
                progressCallback.accept(i, total * 2); // Two passes
            }
            try {
                var imageData = entries.get(i).readImageData();
                var server = imageData.getServer();
                double ds = Math.max(scanDownsample, 1.0);
                RegionRequest request = RegionRequest.createInstance(
                        server.getPath(), ds, 0, 0, server.getWidth(), server.getHeight());
                BufferedImage img = server.readRegion(request);
                Raster raster = img.getRaster();
                int w = raster.getWidth();
                int h = raster.getHeight();
                int bands = Math.min(raster.getNumBands(), nChannels);

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        for (int c = 0; c < bands; c++) {
                            double val = raster.getSampleDouble(x, y, c);
                            if (Double.isFinite(val)) {
                                if (val < globalMin[c]) globalMin[c] = val;
                                if (val > globalMax[c]) globalMax[c] = val;
                            }
                        }
                    }
                }
                server.close();
            } catch (Exception e) {
                logger.warn("Pass 1 failed for {}: {}",
                        entries.get(i).getImageName(), e.getMessage());
            }
        }

        // Pass 2: binned histograms
        long[][] histograms = new long[nChannels][FLOAT_HISTOGRAM_BINS];
        double[] binWidth = new double[nChannels];
        for (int c = 0; c < nChannels; c++) {
            double range = globalMax[c] - globalMin[c];
            binWidth[c] = range > 0 ? range / FLOAT_HISTOGRAM_BINS : 1.0;
        }

        for (int i = 0; i < total; i++) {
            if (progressCallback != null) {
                progressCallback.accept(total + i, total * 2);
            }
            try {
                var imageData = entries.get(i).readImageData();
                var server = imageData.getServer();
                double ds = Math.max(scanDownsample, 1.0);
                RegionRequest request = RegionRequest.createInstance(
                        server.getPath(), ds, 0, 0, server.getWidth(), server.getHeight());
                BufferedImage img = server.readRegion(request);
                Raster raster = img.getRaster();
                int w = raster.getWidth();
                int h = raster.getHeight();
                int bands = Math.min(raster.getNumBands(), nChannels);

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        for (int c = 0; c < bands; c++) {
                            double val = raster.getSampleDouble(x, y, c);
                            if (Double.isFinite(val)) {
                                int bin = (int) ((val - globalMin[c]) / binWidth[c]);
                                bin = Math.max(0, Math.min(bin, FLOAT_HISTOGRAM_BINS - 1));
                                histograms[c][bin]++;
                            }
                        }
                    }
                }
                server.close();
            } catch (Exception e) {
                logger.warn("Pass 2 failed for {}: {}",
                        entries.get(i).getImageName(), e.getMessage());
            }
        }

        // Compute percentile-based min/max
        List<ChannelRange> ranges = new ArrayList<>();
        for (int c = 0; c < nChannels; c++) {
            long totalPixels = 0;
            for (long count : histograms[c]) totalPixels += count;

            if (totalPixels == 0) {
                ranges.add(new ChannelRange(channelNames[c], channelColors[c],
                        globalMin[c], globalMax[c]));
                continue;
            }

            double clipCount = totalPixels * percentile / 100.0;
            double minBin = findPercentileFromHistogram(histograms[c], clipCount, true);
            double maxBin = findPercentileFromHistogram(histograms[c], clipCount, false);

            // Convert bin indices back to actual values
            double minVal = globalMin[c] + minBin * binWidth[c];
            double maxVal = globalMin[c] + maxBin * binWidth[c];

            if (minVal >= maxVal) {
                maxVal = minVal + binWidth[c];
            }

            ranges.add(new ChannelRange(channelNames[c], channelColors[c], minVal, maxVal));
        }

        return ranges;
    }

    /**
     * Find the histogram bin at which the cumulative count exceeds clipCount.
     *
     * @param histogram  the histogram array
     * @param clipCount  number of pixels to clip
     * @param fromLow    true for low percentile (scan from left), false for high (scan from right)
     * @return the bin index at the percentile boundary
     */
    private static double findPercentileFromHistogram(long[] histogram, double clipCount, boolean fromLow) {
        long cumulative = 0;
        if (fromLow) {
            for (int i = 0; i < histogram.length; i++) {
                cumulative += histogram[i];
                if (cumulative > clipCount) {
                    return i;
                }
            }
            return histogram.length - 1;
        } else {
            for (int i = histogram.length - 1; i >= 0; i--) {
                cumulative += histogram[i];
                if (cumulative > clipCount) {
                    return i;
                }
            }
            return 0;
        }
    }

    /**
     * Build ImageDisplaySettings from computed channel ranges.
     * Uses the first entry to create a template display, then adjusts
     * each channel's min/max to match the computed global ranges.
     *
     * @param ranges  computed per-channel ranges
     * @param firstEntry  a project image entry to use as template
     * @return display settings with global ranges applied, or null on failure
     */
    public static ImageDisplaySettings buildDisplaySettings(
            List<ChannelRange> ranges,
            ProjectImageEntry<BufferedImage> firstEntry) {
        if (ranges == null || ranges.isEmpty() || firstEntry == null) {
            return null;
        }
        try {
            var imageData = firstEntry.readImageData();
            var display = ImageDisplay.create(imageData);

            var channels = display.selectedChannels();
            for (int i = 0; i < Math.min(channels.size(), ranges.size()); i++) {
                var channel = channels.get(i);
                var range = ranges.get(i);
                display.setMinMaxDisplay(channel, (float) range.minDisplay(), (float) range.maxDisplay());
            }

            var settings = DisplaySettingUtils.displayToSettings(display, "global_matched");
            imageData.getServer().close();
            return settings;
        } catch (Exception e) {
            logger.error("Failed to build display settings from computed ranges: {}", e.getMessage());
            return null;
        }
    }
}
