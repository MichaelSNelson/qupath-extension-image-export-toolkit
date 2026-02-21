package qupath.ext.quiet.ui;

import java.awt.image.BufferedImage;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import qupath.lib.projects.ProjectImageEntry;

/**
 * Wrapper around a {@link ProjectImageEntry} for use in a CheckBoxListCell.
 */
public class ImageEntryItem {

    private final ProjectImageEntry<BufferedImage> entry;
    private final BooleanProperty selected;

    public ImageEntryItem(ProjectImageEntry<BufferedImage> entry, boolean selected) {
        this.entry = entry;
        this.selected = new SimpleBooleanProperty(selected);
    }

    public ProjectImageEntry<BufferedImage> getEntry() {
        return entry;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.set(value);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    @Override
    public String toString() {
        return entry.getImageName();
    }
}
