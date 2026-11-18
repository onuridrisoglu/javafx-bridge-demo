package com.example.warehouse.fx.ui.products;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import com.example.warehouse.fx.ui.util.StatusBar;
import com.example.warehouse.fx.ui.util.TableExporter;
import com.example.warehouse.fx.ui.util.Ui;
import com.example.warehouse.model.Product;
import com.example.warehouse.model.StockMovement;
import com.example.warehouse.service.ProductService;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

/** Product catalogue: searchable table with add / edit / delete / stock adjust. */
public class ProductsPanel extends BorderPane {

    private static final String OUT_OF_STOCK = "OUT OF STOCK";
    private static final String LOW_STOCK = "Low Stock";
    private static final String OK = "OK";

    private final ProductService service = new ProductService();
    private final StatusBar statusBar;

    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final FilteredList<Product> filtered = new FilteredList<>(products);
    private final TableView<Product> table = new TableView<>();

    public ProductsPanel(StatusBar statusBar) {
        this.statusBar = statusBar;
        getStyleClass().add("panel");

        buildTable();
        setTop(buildToolbar());
        setCenter(table);

        refresh();
    }

    private Node buildToolbar() {
        Button addBtn = Ui.toolbarButton("+ Add Product");
        addBtn.getStyleClass().add("add-button");
        Button editBtn = Ui.toolbarButton("Edit");
        Button deleteBtn = Ui.toolbarButton("Delete");
        Button stockBtn = Ui.toolbarButton("± Stock Adjust");
        Button refreshBtn = Ui.toolbarButton("↺ Refresh");
        Button exportBtn = Ui.toolbarButton("⬇ Export CSV");

        BooleanBinding noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        editBtn.disableProperty().bind(noSelection);
        deleteBtn.disableProperty().bind(noSelection);
        stockBtn.disableProperty().bind(noSelection);

        addBtn.setOnAction(e -> openAddDialog());
        editBtn.setOnAction(e -> openEditDialog());
        deleteBtn.setOnAction(e -> deleteSelected());
        stockBtn.setOnAction(e -> openStockAdjustDialog());
        refreshBtn.setOnAction(e -> refresh());
        exportBtn.setOnAction(e -> TableExporter.exportToCsv(table, "products.csv", Ui.windowOf(this)));

        TextField searchField = new TextField();
        searchField.setPrefColumnCount(18);
        searchField.textProperty().addListener((obs, old, text) -> filterTable(text));

        return Ui.toolbar(new Node[] { addBtn, Ui.toolbarSeparator(), editBtn, deleteBtn,
                Ui.toolbarSeparator(), stockBtn, Ui.toolbarSeparator(), refreshBtn,
                Ui.toolbarSeparator(), exportBtn }, new Label("Search:"), searchField);
    }

    private void buildTable() {
        Ui.styleTable(table);
        SortedList<Product> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        TableColumn<Product, Integer> onHand = column("On Hand", 75, Product::getQuantity);
        TableColumn<Product, Integer> reorder = column("Reorder", 75, Product::getReorderLevel);
        TableColumn<Product, BigDecimal> price = column("Unit Price", 90, Product::getUnitPrice);
        // Numeric columns are centered, like the Swing renderer
        List.of(onHand, reorder, price).forEach(c -> c.setStyle("-fx-alignment: CENTER;"));

        TableColumn<Product, String> status = column("Status", 90, ProductsPanel::statusOf);
        status.setCellFactory(col -> new StatusCell());

        table.getColumns().addAll(List.of(
                column("SKU", 80, Product::getSku),
                column("Product Name", 200, Product::getName),
                column("Category", 130, p -> p.getCategory().getName()),
                column("Supplier", 180, p -> p.getSupplier() != null ? p.getSupplier().getName() : "—"),
                onHand, reorder, price, status));

        ContextMenu menu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit Product");
        MenuItem deleteItem = new MenuItem("Delete Product");
        MenuItem stockItem = new MenuItem("Adjust Stock");
        editItem.setOnAction(e -> openEditDialog());
        deleteItem.setOnAction(e -> deleteSelected());
        stockItem.setOnAction(e -> openStockAdjustDialog());
        menu.getItems().addAll(editItem, deleteItem, new SeparatorMenuItem(), stockItem);

        table.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>() {
                @Override
                protected void updateItem(Product p, boolean empty) {
                    super.updateItem(p, empty);
                    boolean out = !empty && p != null && p.getQuantity() == 0;
                    pseudoClassStateChanged(Ui.CRITICAL, out);
                    pseudoClassStateChanged(Ui.LOW_STOCK, !out && !empty && p != null && p.isLowStock());
                }
            };
            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty())
                    .then((ContextMenu) null).otherwise(menu));
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    openEditDialog();
                }
            });
            return row;
        });
    }

    private static <T> TableColumn<Product, T> column(String title, double width,
            Function<Product, T> value) {
        TableColumn<Product, T> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(value.apply(cd.getValue())));
        return col;
    }

    private static String statusOf(Product p) {
        return p.isLowStock() ? (p.getQuantity() == 0 ? OUT_OF_STOCK : LOW_STOCK) : OK;
    }

    /** Status text colored green / orange / red, like the Swing renderer. */
    private static final class StatusCell extends TableCell<Product, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            getStyleClass().removeAll("status-ok", "status-warning", "status-danger");
            if (empty || status == null) {
                setText(null);
                return;
            }
            setText(status);
            getStyleClass().add(switch (status) {
                case OUT_OF_STOCK -> "status-danger";
                case LOW_STOCK -> "status-warning";
                default -> "status-ok";
            });
        }
    }

    /** Case-insensitive match of the text against any column's displayed value. */
    public void filterTable(String text) {
        String needle = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            filtered.setPredicate(null);
        } else {
            filtered.setPredicate(p -> table.getColumns().stream()
                    .map(c -> c.getCellData(p))
                    .anyMatch(v -> v != null && v.toString().toLowerCase(Locale.ROOT).contains(needle)));
        }
        statusBar.setMessage(filtered.size() + " of " + products.size() + " products shown");
    }

    public void refresh() {
        products.setAll(service.findAll());
        table.refresh(); // entities may have changed in place (edit / stock adjust)
        statusBar.setRowCount(products.size(), "product");
    }

    private Product getSelectedProduct() {
        return table.getSelectionModel().getSelectedItem();
    }

    private void openAddDialog() {
        ProductDialog dlg = new ProductDialog(Ui.windowOf(this), null);
        dlg.showAndWait();
        if (dlg.isSaved()) {
            refresh();
        }
    }

    private void openEditDialog() {
        Product p = getSelectedProduct();
        if (p == null) {
            return;
        }
        ProductDialog dlg = new ProductDialog(Ui.windowOf(this), p);
        dlg.showAndWait();
        if (dlg.isSaved()) {
            refresh();
        }
    }

    private void deleteSelected() {
        Product p = getSelectedProduct();
        if (p == null) {
            return;
        }
        Window window = Ui.windowOf(this);
        if (!Ui.confirm(window, "Confirm Delete", "Delete product '" + p.getName() + "' ("
                + p.getSku() + ")?\n\nThis cannot be undone.")) {
            return;
        }
        try {
            service.delete(p.getId());
            refresh();
            statusBar.setMessage("Product '" + p.getName() + "' deleted.");
        } catch (Exception ex) {
            Ui.error(window, "Delete Error",
                    "Cannot delete: product may be referenced by orders or stock movements.");
        }
    }

    private void openStockAdjustDialog() {
        Product p = getSelectedProduct();
        if (p == null) {
            return;
        }

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Stock Adjustment — " + p.getName());
        dlg.initOwner(Ui.windowOf(this));
        dlg.setResizable(true);
        DialogPane pane = dlg.getDialogPane();
        pane.getStylesheets().add(Ui.STYLESHEET);
        pane.setPrefWidth(380);

        Label currentQty = new Label(String.valueOf(p.getQuantity()));
        currentQty.setStyle("-fx-font-weight: bold;");
        ComboBox<StockMovement.MovementType> typeBox = new ComboBox<>(
                FXCollections.observableArrayList(StockMovement.MovementType.values()));
        typeBox.getSelectionModel().selectFirst();
        Spinner<Integer> qtySpinner = new Spinner<>(0, 99999, 1);
        qtySpinner.setEditable(true);
        TextField reasonField = new TextField();
        reasonField.setPrefColumnCount(20);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(16, 20, 8, 20));
        ColumnConstraints labels = new ColumnConstraints();
        labels.setHalignment(HPos.LEFT);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setFillWidth(true);
        form.getColumnConstraints().addAll(labels, fields);
        typeBox.setMaxWidth(Double.MAX_VALUE);
        qtySpinner.setMaxWidth(Double.MAX_VALUE);
        reasonField.setMaxWidth(Double.MAX_VALUE);
        form.addRow(0, Ui.fieldLabel("Product:"), new Label(p.getName()));
        form.addRow(1, Ui.fieldLabel("Current Qty:"), currentQty);
        form.addRow(2, Ui.fieldLabel("Movement Type:"), typeBox);
        form.addRow(3, Ui.fieldLabel("Quantity:"), qtySpinner);
        form.addRow(4, Ui.fieldLabel("Reason:"), reasonField);
        pane.setContent(form);

        ButtonType apply = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(apply, ButtonType.CANCEL);
        pane.lookupButton(apply).getStyleClass().add("primary-button");
        pane.lookupButton(apply).addEventFilter(ActionEvent.ACTION, e -> {
            Window dlgWindow = pane.getScene().getWindow();
            String reason = reasonField.getText().trim();
            if (reason.isEmpty()) {
                Ui.warn(dlgWindow, "Validation", "Please enter a reason.");
                e.consume();
                return;
            }
            try {
                service.adjustStock(p, typeBox.getValue(),
                        ProductDialog.spinnerValue(qtySpinner), reason);
            } catch (RuntimeException ex) {
                Ui.error(dlgWindow, "Error", ex.getMessage());
                e.consume();
                return;
            }
            refresh();
        });
        dlg.showAndWait();
    }
}
