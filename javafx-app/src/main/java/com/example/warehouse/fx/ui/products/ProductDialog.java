package com.example.warehouse.fx.ui.products;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.example.warehouse.dao.CategoryDAO;
import com.example.warehouse.fx.ui.util.Ui;
import com.example.warehouse.model.Category;
import com.example.warehouse.model.Product;
import com.example.warehouse.model.Supplier;
import com.example.warehouse.service.ProductService;
import com.example.warehouse.service.SupplierService;

import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Add / edit form for a product. {@link #showAndWait()} yields the saved
 * product, or nothing when the dialog was cancelled.
 */
public class ProductDialog extends Dialog<Product> {

    private static final String NO_SUPPLIER = "— None —";

    private final Product existing;
    private final ProductService productService = new ProductService();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final SupplierService supplierService = new SupplierService();

    private boolean saved = false;
    private Product result;

    private final TextField skuField = new TextField();
    private final TextField nameField = new TextField();
    private final ComboBox<Category> categoryBox = new ComboBox<>();
    private final ComboBox<Supplier> supplierBox = new ComboBox<>();
    private final TextArea descriptionArea = new TextArea();
    private final TextField priceField = new TextField();
    private final Spinner<Integer> quantitySpinner = new Spinner<>(0, 999999, 0);
    private final Spinner<Integer> reorderSpinner = new Spinner<>(0, 999999, 5);
    private final TextField unitField = new TextField();

    public ProductDialog(Window owner, Product product) {
        this.existing = product;
        setTitle(product == null ? "Add New Product" : "Edit Product — " + product.getSku());
        if (owner != null) {
            initOwner(owner);
        }
        setResizable(true);

        DialogPane pane = getDialogPane();
        pane.getStylesheets().add(Ui.STYLESHEET);
        pane.setPrefSize(480, 480);

        Label title = new Label(existing == null ? "New Product" : "Edit Product");
        title.getStyleClass().add("panel-title");
        title.setStyle("-fx-text-fill: -wp-primary; -fx-font-size: 16px;");
        Node form = Ui.section("Product Details", buildForm());
        VBox.setVgrow(form, Priority.ALWAYS);
        VBox content = new VBox(8, title, form);
        content.setPadding(new Insets(12, 16, 0, 16));
        pane.setContent(content);

        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(save, ButtonType.CANCEL);
        pane.lookupButton(save).getStyleClass().add("primary-button");
        // Save runs inside the filter so that invalid input or a failed save
        // keeps the dialog open, like the Swing version.
        pane.lookupButton(save).addEventFilter(ActionEvent.ACTION, e -> {
            if (!doSave()) {
                e.consume();
            }
        });
        setResultConverter(bt -> bt == save && saved ? result : null);

        if (existing != null) {
            populateFields();
        }
    }

    private GridPane buildForm() {
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(8);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setHalignment(HPos.RIGHT);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setFillWidth(true);
        form.getColumnConstraints().addAll(labels, fields);

        categoryBox.getItems().setAll(categoryDAO.findAll());
        if (!categoryBox.getItems().isEmpty()) {
            categoryBox.getSelectionModel().selectFirst();
        }

        List<Supplier> suppliers = new ArrayList<>();
        suppliers.add(null); // "no supplier" entry
        suppliers.addAll(supplierService.findAll());
        supplierBox.getItems().setAll(suppliers);
        supplierBox.setCellFactory(lv -> new SupplierCell(false));
        supplierBox.setButtonCell(new SupplierCell(true));
        supplierBox.getSelectionModel().selectFirst();

        quantitySpinner.setEditable(true);
        reorderSpinner.setEditable(true);
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);

        int row = 0;
        addRow(form, row++, "SKU: *", skuField);
        addRow(form, row++, "Name: *", nameField);
        addRow(form, row++, "Category: *", categoryBox);
        addRow(form, row++, "Supplier:", supplierBox);
        addRow(form, row++, "Unit Price ($):*", priceField);
        addRow(form, row++, "Quantity:", quantitySpinner);
        addRow(form, row++, "Reorder Level:", reorderSpinner);
        addRow(form, row++, "Unit:", unitField);
        Label descLabel = addRow(form, row, "Description:", descriptionArea);
        GridPane.setValignment(descLabel, VPos.TOP);
        GridPane.setVgrow(descriptionArea, Priority.ALWAYS);
        return form;
    }

    private static Label addRow(GridPane form, int row, String label, Control field) {
        Label l = Ui.fieldLabel(label);
        field.setMaxWidth(Double.MAX_VALUE);
        form.addRow(row, l, field);
        return l;
    }

    private void populateFields() {
        skuField.setText(existing.getSku());
        nameField.setText(existing.getName());
        priceField.setText(existing.getUnitPrice().toPlainString());
        quantitySpinner.getValueFactory().setValue(existing.getQuantity());
        reorderSpinner.getValueFactory().setValue(existing.getReorderLevel());
        unitField.setText(existing.getUnit() != null ? existing.getUnit() : "");
        descriptionArea.setText(existing.getDescription() != null ? existing.getDescription() : "");

        categoryBox.getItems().stream()
                .filter(c -> c.getId().equals(existing.getCategory().getId()))
                .findFirst()
                .ifPresent(categoryBox.getSelectionModel()::select);
        if (existing.getSupplier() != null) {
            supplierBox.getItems().stream()
                    .filter(s -> s != null && s.getId().equals(existing.getSupplier().getId()))
                    .findFirst()
                    .ifPresent(supplierBox.getSelectionModel()::select);
        }
    }

    /** Validates and persists the form; returns whether the dialog may close. */
    private boolean doSave() {
        Window window = getDialogPane().getScene().getWindow();
        String sku = skuField.getText().trim();
        String name = nameField.getText().trim();
        String priceText = priceField.getText().trim();

        if (sku.isEmpty() || name.isEmpty() || priceText.isEmpty()) {
            Ui.warn(window, "Validation Error", "SKU, Name, and Unit Price are required.");
            return false;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(priceText);
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            Ui.warn(window, "Validation Error", "Unit Price must be a valid positive number.");
            return false;
        }

        Product p = existing != null ? existing : new Product();
        p.setSku(sku);
        p.setName(name);
        p.setCategory(categoryBox.getValue());
        p.setSupplier(supplierBox.getValue());
        p.setUnitPrice(price);
        p.setQuantity(spinnerValue(quantitySpinner));
        p.setReorderLevel(spinnerValue(reorderSpinner));
        p.setUnit(unitField.getText().trim());
        p.setDescription(descriptionArea.getText().trim());

        try {
            if (existing == null) {
                productService.save(p);
            } else {
                productService.update(p);
            }
            saved = true;
            result = p;
            return true;
        } catch (Exception ex) {
            Ui.error(window, "Error", "Failed to save product:\n" + ex.getMessage());
            return false;
        }
    }

    /**
     * The spinner's value including text typed but not yet committed (JavaFX
     * spinners only commit on Enter).
     */
    static int spinnerValue(Spinner<Integer> spinner) {
        if (spinner.isEditable()) {
            try {
                Integer typed = spinner.getValueFactory().getConverter()
                        .fromString(spinner.getEditor().getText());
                if (typed != null) {
                    spinner.getValueFactory().setValue(typed);
                }
            } catch (RuntimeException ignored) {
                // keep the last valid value
            }
        }
        return spinner.getValue();
    }

    public boolean isSaved() {
        return saved;
    }

    /** Renders the {@code null} supplier as "— None —". */
    private static final class SupplierCell extends ListCell<Supplier> {
        private final boolean buttonCell;

        SupplierCell(boolean buttonCell) {
            this.buttonCell = buttonCell;
        }

        @Override
        protected void updateItem(Supplier item, boolean empty) {
            super.updateItem(item, empty);
            if (empty && !buttonCell) {
                setText(null);
            } else {
                setText(item == null ? NO_SUPPLIER : item.getName());
            }
        }
    }
}
