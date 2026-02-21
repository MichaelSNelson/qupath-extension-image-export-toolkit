package qupath.ext.quiet.export;

/**
 * Supported output image formats for all export categories.
 */
public enum OutputFormat {

    PNG("png"),
    TIFF("tif"),
    JPEG("jpg"),
    OME_TIFF("ome.tif"),
    OME_TIFF_PYRAMID("ome.tif");

    private final String extension;

    OutputFormat(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public String toString() {
        return name().replace('_', ' ');
    }
}
