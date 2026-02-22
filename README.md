# QuIET - QuPath Image Export Toolkit

[![Release](https://img.shields.io/github/v/release/MichaelSNelson/qupath-extension-image-export-toolkit?include_prereleases)](https://github.com/MichaelSNelson/qupath-extension-image-export-toolkit/releases)
[![License](https://img.shields.io/github/license/MichaelSNelson/qupath-extension-image-export-toolkit)](LICENSE)

A [QuPath](https://qupath.github.io/) extension for batch exporting images in multiple formats for machine learning training and analysis.

QuIET provides a guided wizard UI for exporting rendered overlays, label/instance masks, raw pixel data, and tiled image+label pairs -- all with self-contained Groovy script generation so every export is reproducible and editable.

## Requirements

- **QuPath 0.6.0** or later
- Java 21+

## Installation

1. Download the latest `qupath-extension-image-export-toolkit-*-all.jar` from [Releases](https://github.com/MichaelSNelson/qupath-extension-image-export-toolkit/releases)
2. Drag the JAR onto the running QuPath window, **or** copy it into your QuPath `extensions/` directory
3. Restart QuPath

The extension appears under **Extensions > QuIET > Export Images...**

> The menu item is disabled until a project with at least one image is open.

## Quick Start

1. Open a QuPath project containing annotated images
2. Go to **Extensions > QuIET > Export Images...**
3. **Step 1** -- Choose an export category (Rendered, Mask, Raw, or Tiled)
4. **Step 2** -- Configure export settings (mask type, downsample, format, etc.)
5. **Step 3** -- Select images, choose output directory, and click **Export**

Every export also generates a **Groovy script** that you can copy, save, and re-run from QuPath's built-in script editor -- no extension required.

---

## Export Categories

### Rendered Image

Export images with visual overlays composited onto the base image.

| Option | Description |
|--------|-------------|
| **Classifier Overlay** | Render a pixel classifier's output on top of the image at configurable opacity |
| **Object Overlay** | Render annotation and/or detection objects with fill, outline, and name options |
| **Display Settings** | Control brightness/contrast, channel visibility, and LUTs applied to the base image (see below) |
| **Scale Bar** | Optionally burn a scale bar into the exported image with configurable position and color (see below) |
| **Downsample** | Resolution factor (1x = full resolution, 4x = quarter, etc.) |
| **Format** | PNG, TIFF, JPEG, OME-TIFF |

#### Display Settings

By default, rendered exports now apply per-image brightness/contrast and channel visibility settings -- so fluorescence images look like they do in the QuPath viewer instead of appearing as raw pixel data.

| Mode | Description |
|------|-------------|
| **Per-Image Saved Settings** (default) | Each image uses its own saved display settings from the QuPath project. If you used "Apply to similar images" in the B&C dialog, all images will already share the same settings. |
| **Current Viewer Settings** | Captures the display settings from the currently open image and applies them uniformly to all exported images. Requires an image to be open. |
| **Saved Preset** | Loads a named B&C preset saved in the project (via the Brightness & Contrast dialog's save button) and applies it to all images. |
| **Raw (No Adjustments)** | Exports raw pixel data with no display transforms. This was the only behavior prior to v0.2.1. |

#### Scale Bar

Rendered exports can optionally include a burned-in scale bar with text label. The scale bar automatically picks a "nice" length (e.g., 50 um, 200 um, 1 mm) targeting roughly 15% of the image width, and formats the label with appropriate units.

| Option | Values |
|--------|--------|
| **Show scale bar** | Enable/disable (default: off) |
| **Position** | Lower Right (default), Lower Left, Upper Right, Upper Left |
| **Color** | White (default) or Black -- drawn with a contrast outline for visibility on any background |

The scale bar requires pixel calibration in the image metadata. If an image has no calibration, the scale bar is skipped with a warning.

> **Note:** Scale bars are only available for rendered exports. Mask, raw, and tiled exports preserve exact pixel values and must not be modified.

### Label / Mask

Export segmentation masks from QuPath's object hierarchy using `LabeledImageServer`.

| Mask Type | Description |
|-----------|-------------|
| **Binary** | Single-class foreground/background (label 0 and 1) |
| **Grayscale Labels** | Integer label per classification (1, 2, 3, ...) with optional grayscale LUT |
| **Class-Colored** | RGB mask using each PathClass's assigned color |
| **Instance IDs** | Unique integer per object instance (16-bit), with optional label shuffling for visual clarity |
| **Multi-Channel** | One binary channel per classification |

Additional mask options:

- **Object source** -- Annotations, Detections, or Cells
- **Background label** -- Configurable background value (default 0)
- **Boundary labels** -- Enable boundary erosion with configurable label value and line thickness
- **Classification filter** -- Select/deselect which classifications to include

### Raw Pixel Data

Export unprocessed pixel data at configurable resolution.

| Option | Description |
|--------|-------------|
| **Region type** | Whole image, selected annotations, or all annotations |
| **Annotation padding** | Add pixel padding around annotation bounding boxes (clamped to image bounds) |
| **Channel selection** | Export only specific channels from multi-channel/fluorescence images |
| **OME-TIFF Pyramid** | Multi-resolution pyramidal output with configurable levels, tile size, and compression |
| **Downsample** | Resolution factor |
| **Format** | PNG, TIFF, JPEG, OME-TIFF, OME-TIFF Pyramid |

> **Note:** OME-TIFF Pyramid export uses `OMEPyramidWriter` from `qupath-extension-bioformats`. If that extension is not installed, QuIET falls back to flat OME-TIFF via `ImageWriterTools`.

### Tiled Export (ML Training)

Export fixed-size image + label tile pairs for deep learning frameworks (StarDist, CellPose, HoVer-Net, etc.) using QuPath's `TileExporter` API.

| Option | Description |
|--------|-------------|
| **Tile size** | Width and height in pixels (e.g., 256, 512, 1024) |
| **Overlap** | Pixel overlap between adjacent tiles |
| **Downsample** | Resolution factor for tile extraction |
| **Image format** | Output format for image tiles |
| **Label masks** | Optionally generate a label mask tile alongside each image tile |
| **Label mask type** | Binary, Grayscale Labels, Instance, Colored, or Multi-Channel |
| **Parent filter** | Restrict tiles to annotation regions, TMA cores, or export all |
| **Annotated only** | Skip tiles that contain no annotated objects |
| **GeoJSON per tile** | Export object geometries for each tile |

### GeoJSON Export

An orthogonal option available alongside any export category. When enabled, QuIET exports all annotations and detections as a `.geojson` file per image -- useful for COCO/YOLO-style training pipelines that need geometry alongside image data.

Enable via the **"Also export GeoJSON annotations"** checkbox on the image selection step.

### Metadata Sidecar Files

Every batch export automatically generates a human-readable `.txt` metadata file alongside the exported images. These files provide essential context for interpreting exports later, especially when shared with collaborators or loaded into external tools.

**Mask exports** produce `mask_legend.txt` containing:
- Label-to-class mapping (e.g., `1 = Tumor`, `2 = Stroma` for grayscale labels)
- Mask type, object source, boundary settings
- Pixel size after downsampling

**Rendered, raw, and tiled exports** produce `export_info.txt` containing:
- Channel names and colors (fluorescence) or stain vectors (brightfield H&E/H-DAB)
- Display settings applied during rendering (if applicable)
- Pixel size with downsample factor
- Tile size and overlap (for tiled exports)

Images with different channel configurations are automatically grouped in the metadata file. Metadata writing never interrupts the export -- failures are logged but silently ignored.

---

## Script Generation

Every export produces a **self-contained Groovy script** that:

- Runs standalone in QuPath's script editor (no extension dependencies)
- Contains all configuration as editable variables at the top
- Processes all project images in a batch loop
- Reports progress and error counts
- Can be saved, version-controlled, and shared

Use the **Copy Script** or **Save Script...** buttons in the wizard to capture the generated script before or after running an export.

---

## Output Structure

Exports are written to a configurable output directory. The default structure under a QuPath project is:

```
<project>/
  exports/
    rendered/                # Rendered image exports
      image_001.png
      export_info.txt        # Channel info, display settings, pixel size
    masks/                   # Label/mask exports
      image_001.png
      mask_legend.txt        # Label-to-class mapping, pixel size
    raw/                     # Raw pixel data exports
      image_001.tif
      export_info.txt        # Channel info, pixel size
    tiles/                   # Tiled ML exports
      <image_name>/
        <image_name>_[x,y,w,h].tif
        <image_name>_[x,y,w,h].png   (label)
      export_info.txt        # Channel info, tile params, pixel size
```

Filenames are sanitized using QuPath's `GeneralTools.stripInvalidFilenameChars()` for cross-platform compatibility.

---

## Preferences

All wizard settings are automatically persisted across QuPath sessions. When you reopen the export wizard, your previous configuration (mask type, downsample, format, padding, etc.) is restored.

Preferences are stored in QuPath's standard preference system under the `quiet.*` namespace.

---

## Building from Source

```bash
# Clone the repository
git clone https://github.com/MichaelSNelson/qupath-extension-image-export-toolkit.git
cd qupath-extension-image-export-toolkit

# Build the extension JAR (includes all dependencies)
./gradlew shadowJar

# The JAR is at: build/libs/qupath-extension-image-export-toolkit-*-all.jar
```

### Other Gradle Tasks

```bash
# Run all tests
./gradlew test

# Compile only (quick check)
./gradlew compileJava

# Clean build artifacts
./gradlew clean
```

### Project Structure

```
src/main/java/qupath/ext/quiet/
  QuietExtension.java              # Extension entry point
  export/
    ExportCategory.java            # RENDERED, MASK, RAW, TILED
    OutputFormat.java              # PNG, TIFF, JPEG, OME_TIFF, OME_TIFF_PYRAMID
    RenderedExportConfig.java      # Rendered export configuration
    RenderedImageExporter.java     # Rendered export logic
    MaskExportConfig.java          # Mask/label export configuration
    MaskImageExporter.java         # LabeledImageServer-based mask export
    RawExportConfig.java           # Raw pixel export configuration
    RawImageExporter.java          # Raw pixel export logic
    TiledExportConfig.java         # Tiled ML export configuration
    TiledImageExporter.java        # TileExporter-based tiled export
    GeoJsonExporter.java           # GeoJSON annotation export
    BatchExportTask.java           # JavaFX Task for background batch processing
    ExportResult.java              # Export outcome tracking
    ScaleBarRenderer.java          # Java2D scale bar drawing utility
    ExportMetadataWriter.java      # Metadata sidecar file writer
  scripting/
    ScriptGenerator.java           # Script generation dispatcher
    RenderedScriptGenerator.java   # Groovy script for rendered export
    MaskScriptGenerator.java       # Groovy script for mask export
    RawScriptGenerator.java        # Groovy script for raw export
    TiledScriptGenerator.java      # Groovy script for tiled export
  ui/
    ExportWizard.java              # Main wizard window
    CategorySelectionPane.java     # Step 1: category cards
    RenderedConfigPane.java        # Step 2a: rendered options
    MaskConfigPane.java            # Step 2b: mask options
    RawConfigPane.java             # Step 2c: raw options
    TiledConfigPane.java           # Step 2d: tiled options
    ImageSelectionPane.java        # Step 3: image list + run
  preferences/
    QuietPreferences.java          # Persistent preference storage

src/main/resources/
  qupath/ext/quiet/ui/
    strings.properties             # All UI strings (i18n-ready)
  META-INF/services/
    qupath.lib.gui.extensions.QuPathExtension
```

---

## Known Limitations

- **OME-TIFF Pyramid** requires `qupath-extension-bioformats` to be installed alongside QuIET. Without it, pyramid exports fall back to flat OME-TIFF.
- **Channel selection** currently uses channel indices. The UI populates available channels, but auto-detection from image metadata is planned for a future release.
- **Tiled GeoJSON** produces one GeoJSON file per tile via QuPath's `TileExporter.exportJson()` API (not a single consolidated file).
- Rendered export with classifier overlay requires a pixel classifier saved in the QuPath project.
- **Display Settings** "Current Viewer" mode requires an image to be open in the viewer at export time. "Saved Preset" mode requires presets saved via QuPath's Brightness & Contrast dialog.

## Roadmap

Future releases may include:

- Measurement-filtered export (export objects by area, intensity, etc.)
- Contour/outline mask export
- Z-stack and timepoint handling
- Stain deconvolution channel export
- COCO/YOLO annotation format export
- Per-object crop export for classification training

---

## License

This extension is distributed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Acknowledgments

Built on top of [QuPath](https://qupath.github.io/), an open-source platform for bioimage analysis. QuIET leverages QuPath's `LabeledImageServer`, `TileExporter`, `TransformedServerBuilder`, and `ImageWriterTools` APIs.
