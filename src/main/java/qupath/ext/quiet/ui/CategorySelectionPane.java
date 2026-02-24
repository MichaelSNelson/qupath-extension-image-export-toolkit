package qupath.ext.quiet.ui;

import java.util.ResourceBundle;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import qupath.ext.quiet.export.ExportCategory;

/**
 * Step 1 of the export wizard: Select an export category.
 * <p>
 * Presents three clickable cards, each representing an export category
 * (Rendered, Mask, Raw). The selected card is highlighted.
 */
public class CategorySelectionPane extends VBox {

    private static final ResourceBundle resources =
            ResourceBundle.getBundle("qupath.ext.quiet.ui.strings");

    private static final String CARD_STYLE_DEFAULT =
            "-fx-border-color: #cccccc; -fx-border-radius: 8; -fx-background-radius: 8; " +
            "-fx-background-color: #f8f8f8; -fx-padding: 15; -fx-cursor: hand;";

    private static final String CARD_STYLE_SELECTED =
            "-fx-border-color: #0078d7; -fx-border-width: 2; -fx-border-radius: 8; " +
            "-fx-background-radius: 8; -fx-background-color: #e8f0fe; -fx-padding: 14; -fx-cursor: hand;";

    private ExportCategory selectedCategory = ExportCategory.RENDERED;
    private Runnable onAdvance;
    private VBox renderedCard;
    private VBox maskCard;
    private VBox rawCard;
    private VBox tiledCard;

    public CategorySelectionPane() {
        setSpacing(15);
        setPadding(new Insets(10));
        setAlignment(Pos.TOP_CENTER);

        var header = new Label(resources.getString("wizard.step1.title"));
        header.setFont(Font.font(null, FontWeight.BOLD, 16));

        renderedCard = createCard(
                resources.getString("category.rendered.title"),
                resources.getString("category.rendered.description"),
                ExportCategory.RENDERED);

        maskCard = createCard(
                resources.getString("category.mask.title"),
                resources.getString("category.mask.description"),
                ExportCategory.MASK);

        rawCard = createCard(
                resources.getString("category.raw.title"),
                resources.getString("category.raw.description"),
                ExportCategory.RAW);

        tiledCard = createCard(
                resources.getString("category.tiled.title"),
                resources.getString("category.tiled.description"),
                ExportCategory.TILED);

        var cardsBox = new HBox(15, renderedCard, maskCard, rawCard, tiledCard);
        HBox.setHgrow(renderedCard, Priority.ALWAYS);
        HBox.setHgrow(maskCard, Priority.ALWAYS);
        HBox.setHgrow(rawCard, Priority.ALWAYS);
        HBox.setHgrow(tiledCard, Priority.ALWAYS);

        getChildren().addAll(header, cardsBox);
        VBox.setVgrow(cardsBox, Priority.ALWAYS);

        updateCardStyles();
    }

    private VBox createCard(String title, String description, ExportCategory category) {
        var titleLabel = new Label(title);
        titleLabel.setFont(Font.font(null, FontWeight.BOLD, 14));
        titleLabel.setWrapText(true);

        var descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Double.MAX_VALUE);

        var card = new VBox(8, titleLabel, descLabel);
        card.setPrefWidth(200);
        card.setMinHeight(120);
        card.setAlignment(Pos.TOP_LEFT);

        card.setOnMouseClicked(e -> {
            selectedCategory = category;
            updateCardStyles();
            if (e.getClickCount() >= 2 && onAdvance != null) {
                onAdvance.run();
            }
        });

        return card;
    }

    private void updateCardStyles() {
        renderedCard.setStyle(selectedCategory == ExportCategory.RENDERED
                ? CARD_STYLE_SELECTED : CARD_STYLE_DEFAULT);
        maskCard.setStyle(selectedCategory == ExportCategory.MASK
                ? CARD_STYLE_SELECTED : CARD_STYLE_DEFAULT);
        rawCard.setStyle(selectedCategory == ExportCategory.RAW
                ? CARD_STYLE_SELECTED : CARD_STYLE_DEFAULT);
        tiledCard.setStyle(selectedCategory == ExportCategory.TILED
                ? CARD_STYLE_SELECTED : CARD_STYLE_DEFAULT);
    }

    public ExportCategory getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(ExportCategory category) {
        this.selectedCategory = category;
        updateCardStyles();
    }

    /**
     * Set a callback to invoke when the user double-clicks a category card
     * (advancing to the next wizard step).
     */
    public void setOnAdvance(Runnable onAdvance) {
        this.onAdvance = onAdvance;
    }
}
