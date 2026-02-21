package qupath.ext.quiet.export;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.lib.common.GeneralTools;
import qupath.lib.images.ImageData;
import qupath.lib.io.PathIO;
import qupath.lib.objects.PathObject;

/**
 * Exports QuPath object annotations as GeoJSON files.
 * <p>
 * This is orthogonal to image export categories -- it can be enabled alongside
 * any export type to produce GeoJSON annotation files for each image.
 */
public class GeoJsonExporter {

    private static final Logger logger = LoggerFactory.getLogger(GeoJsonExporter.class);

    private GeoJsonExporter() {
        // Utility class
    }

    /**
     * Export all annotations and detections as a GeoJSON file for a single image.
     *
     * @param imageData the image data containing the hierarchy
     * @param outputDir the directory to write the GeoJSON file
     * @param entryName the image entry name (used for filename)
     * @throws IOException if the export fails
     */
    public static void exportGeoJson(ImageData<BufferedImage> imageData,
                                     File outputDir,
                                     String entryName) throws IOException {
        try {
            Collection<PathObject> objects = new ArrayList<>();
            objects.addAll(imageData.getHierarchy().getAnnotationObjects());
            objects.addAll(imageData.getHierarchy().getDetectionObjects());

            if (objects.isEmpty()) {
                logger.debug("No objects to export as GeoJSON for: {}", entryName);
                return;
            }

            String sanitized = GeneralTools.stripInvalidFilenameChars(entryName);
            if (sanitized == null || sanitized.isBlank()) {
                sanitized = "unnamed";
            }

            File outputFile = new File(outputDir, sanitized + ".geojson");
            PathIO.exportObjectsAsGeoJSON(outputFile.toPath(), objects);

            logger.info("Exported GeoJSON ({} objects): {}", objects.size(), outputFile.getAbsolutePath());

        } catch (Exception e) {
            throw new IOException("Failed to export GeoJSON for: " + entryName, e);
        }
    }
}
