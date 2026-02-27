package qupath.ext.quiet.ui;

import javafx.scene.Node;
import javafx.scene.control.TitledPane;

/**
 * Utility for creating consistently styled collapsible TitledPane sections.
 */
class SectionBuilder {

    static TitledPane createSection(String title, boolean expanded, Node content) {
        var tp = new TitledPane(title, content);
        tp.setExpanded(expanded);
        tp.setAnimated(false);
        tp.setCollapsible(true);
        tp.setMaxWidth(Double.MAX_VALUE);
        return tp;
    }
}
