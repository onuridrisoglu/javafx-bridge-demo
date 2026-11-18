package com.example.warehouse.fx.ui.util;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Shared building blocks of the WarehousePro UI: the JavaFX counterpart of the
 * Swing version's {@code UIConstants}. Colors and fonts live in
 * {@code warehouse.css}; these helpers only attach the right style classes.
 */
public final class Ui {

    /** The application stylesheet; add it to every scene and dialog pane. */
    public static final String STYLESHEET = Ui.class
            .getResource("/com/example/warehouse/fx/warehouse.css").toExternalForm();

    /** Table row states, styled in warehouse.css. */
    public static final PseudoClass LOW_STOCK = PseudoClass.getPseudoClass("low-stock");
    public static final PseudoClass CRITICAL = PseudoClass.getPseudoClass("critical");

    public static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);

    private Ui() {
    }

    /** Applies the application stylesheet to a scene. */
    public static Scene styled(Scene scene) {
        scene.getStylesheets().add(STYLESHEET);
        return scene;
    }

    public static Button toolbarButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("toolbar-button");
        button.setFocusTraversable(false);
        return button;
    }

    public static Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        return button;
    }

    public static Separator toolbarSeparator() {
        return new Separator(Orientation.VERTICAL);
    }

    public static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        // A form's label column must never squeeze its labels into "S...".
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    /** A horizontal spacer that takes up the remaining width of an HBox. */
    public static Region hgrow() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /**
     * A toolbar: the given nodes on the left, and the right-hand nodes pushed
     * to the right edge.
     */
    public static HBox toolbar(Node[] left, Node... right) {
        HBox bar = new HBox();
        bar.getStyleClass().add("toolbar");
        bar.getChildren().addAll(left);
        bar.getChildren().add(hgrow());
        bar.getChildren().addAll(right);
        return bar;
    }

    /**
     * A bordered section with its title on the top border, like the Swing
     * version's titled borders.
     */
    public static StackPane section(String title, Node content) {
        VBox box = new VBox(content);
        box.getStyleClass().add("section");
        VBox.setVgrow(content, Priority.ALWAYS);
        Label label = new Label(title);
        label.getStyleClass().add("section-title");
        StackPane pane = new StackPane(box, label);
        StackPane.setAlignment(label, javafx.geometry.Pos.TOP_LEFT);
        label.setTranslateX(8);
        pane.setPadding(new javafx.geometry.Insets(9, 0, 0, 0));
        return pane;
    }

    /** Table look shared by all panels: single selection, styled rows. */
    public static <T> void styleTable(TableView<T> table) {
        table.getStyleClass().add("data-table");
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No records"));
    }

    public static boolean confirm(Window owner, String title, String message) {
        Alert alert = alert(Alert.AlertType.CONFIRMATION, owner, title, message);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    public static void info(Window owner, String title, String message) {
        alert(Alert.AlertType.INFORMATION, owner, title, message).showAndWait();
    }

    public static void warn(Window owner, String title, String message) {
        alert(Alert.AlertType.WARNING, owner, title, message).showAndWait();
    }

    public static void error(Window owner, String title, String message) {
        alert(Alert.AlertType.ERROR, owner, title, message).showAndWait();
    }

    private static Alert alert(Alert.AlertType type, Window owner, String title,
            String message) {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.getDialogPane().getStylesheets().add(STYLESHEET);
        return alert;
    }

    /** The window a node is shown in, or {@code null} before it is shown. */
    public static Window windowOf(Node node) {
        return node.getScene() != null ? node.getScene().getWindow() : null;
    }
}
