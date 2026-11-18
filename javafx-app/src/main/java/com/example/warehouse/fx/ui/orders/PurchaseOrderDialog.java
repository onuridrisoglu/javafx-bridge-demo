package com.example.warehouse.fx.ui.orders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.example.warehouse.fx.ui.util.Ui;
import com.example.warehouse.model.Product;
import com.example.warehouse.model.PurchaseOrder;
import com.example.warehouse.model.PurchaseOrderItem;
import com.example.warehouse.model.Supplier;
import com.example.warehouse.service.ProductService;
import com.example.warehouse.service.PurchaseOrderService;
import com.example.warehouse.service.SupplierService;

import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

/** Create / edit dialog for a purchase order with an editable line items table. */
public class PurchaseOrderDialog extends Dialog<Boolean> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PurchaseOrder existing;
    private final PurchaseOrderService orderService = new PurchaseOrderService();
    private final SupplierService supplierService = new SupplierService();
    private final ProductService productService = new ProductService();
    private boolean saved = false;

    private ComboBox<Supplier> supplierBox;
    private TextField orderDateField;
    private TextField expectedDateField;
    private ComboBox<PurchaseOrder.OrderStatus> statusBox;
    private TextArea notesArea;

    private TableView<ItemRow> itemTable;
    // The extractor reports edits of any row property as list updates, the
    // counterpart of the Swing TableModelListener
    private final ObservableList<ItemRow> items = FXCollections.observableArrayList(
            row -> new Observable[] { row.quantity, row.unitPrice, row.lineTotal });
    private Label totalLabel;
    private boolean updatingTotal = false;

    public PurchaseOrderDialog(Window owner, PurchaseOrder order) {
        this.existing = order;
        setTitle(order == null ? "New Purchase Order"
                : "Purchase Order — PO-" + String.format("%04d", order.getId()));
        initOwner(owner);
        setResizable(true);
        getDialogPane().getStylesheets().add(Ui.STYLESHEET);
        getDialogPane().setPrefSize(700, 540);

        buildUI();
        if (existing != null) {
            populateFields();
        } else {
            orderDateField.setText(LocalDate.now().format(DATE_FMT));
        }
    }

    private void buildUI() {
        Label title = new Label(existing == null ? "New Purchase Order" : "Edit Purchase Order");
        title.getStyleClass().add("panel-title");
        BorderPane.setMargin(title, new Insets(0, 0, 8, 0));

        SplitPane split = new SplitPane(buildHeaderPanel(), buildItemsPanel());
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.4);

        BorderPane content = new BorderPane(split);
        content.setTop(title);
        getDialogPane().setContent(content);

        ButtonType saveType = new ButtonType("Save Order", ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().setAll(saveType, ButtonType.CANCEL);
        Button saveBtn = (Button) getDialogPane().lookupButton(saveType);
        saveBtn.getStyleClass().add("primary-button");
        // Keep the dialog open on validation or save errors
        saveBtn.addEventFilter(ActionEvent.ACTION, e -> {
            if (!doSave()) {
                e.consume();
            }
        });
        setResultConverter(type -> type == saveType);
    }

    private Node buildHeaderPanel() {
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(8);
        form.setPadding(new Insets(0, 6, 0, 6));
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setHalignment(HPos.RIGHT);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        fieldCol.setFillWidth(true);
        form.getColumnConstraints().setAll(labelCol, fieldCol);

        List<Supplier> suppliers = supplierService.findAll();
        supplierBox = new ComboBox<>(FXCollections.observableArrayList(suppliers));
        if (!suppliers.isEmpty()) {
            supplierBox.getSelectionModel().selectFirst();
        }

        orderDateField = new TextField();
        expectedDateField = new TextField();
        orderDateField.setPrefColumnCount(10);
        expectedDateField.setPrefColumnCount(10);

        statusBox = new ComboBox<>(FXCollections.observableArrayList(PurchaseOrder.OrderStatus.values()));
        statusBox.getSelectionModel().selectFirst();

        notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        notesArea.setPrefColumnCount(30);
        notesArea.setWrapText(true);

        int row = 0;
        addRow(form, row++, "Supplier: *", supplierBox);
        addRow(form, row++, "Order Date: *", orderDateField);
        addRow(form, row++, "Expected Date:", expectedDateField);
        addRow(form, row++, "Status:", statusBox);
        Label notesLabel = addRow(form, row, "Notes:", notesArea);
        GridPane.setValignment(notesLabel, VPos.TOP);
        GridPane.setVgrow(notesArea, Priority.ALWAYS);

        return Ui.section("Order Details", form);
    }

    private Label addRow(GridPane form, int row, String label, Control field) {
        Label fieldLabel = Ui.fieldLabel(label);
        field.setMaxWidth(Double.MAX_VALUE);
        form.addRow(row, fieldLabel, field);
        return fieldLabel;
    }

    private Node buildItemsPanel() {
        Button addItemBtn = Ui.toolbarButton("+ Add Item");
        Button removeItemBtn = Ui.toolbarButton("Remove");
        addItemBtn.setOnAction(e -> addItem());
        removeItemBtn.setOnAction(e -> removeItem());
        Node itemToolbar = Ui.toolbar(new Node[] { addItemBtn, removeItemBtn });

        itemTable = new TableView<>(items);
        Ui.styleTable(itemTable);
        itemTable.setEditable(true);

        TableColumn<ItemRow, String> skuCol = readOnlyColumn("Product SKU", r -> r.sku);
        TableColumn<ItemRow, String> nameCol = readOnlyColumn("Product Name", r -> r.name);
        TableColumn<ItemRow, String> qtyCol = editableColumn("Quantity", r -> r.quantity);
        TableColumn<ItemRow, String> priceCol = editableColumn("Unit Price ($)", r -> r.unitPrice);
        TableColumn<ItemRow, String> totalCol = new TableColumn<>("Line Total");
        totalCol.setCellValueFactory(cd -> cd.getValue().lineTotal);
        totalCol.setEditable(false);

        qtyCol.setOnEditCommit(e -> commitIfValid(e, true));
        priceCol.setOnEditCommit(e -> commitIfValid(e, false));

        itemTable.getColumns().setAll(List.of(skuCol, nameCol, qtyCol, priceCol, totalCol));
        for (TableColumn<ItemRow, ?> col : itemTable.getColumns()) {
            col.setPrefWidth(75);
            col.setSortable(false);
        }

        items.addListener((ListChangeListener<ItemRow>) c -> updateTotal());

        totalLabel = new Label("Total: $0.00");
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        totalLabel.setMaxWidth(Double.MAX_VALUE);
        totalLabel.setAlignment(Pos.CENTER_RIGHT);
        totalLabel.setPadding(new Insets(4, 8, 0, 0));

        BorderPane panel = new BorderPane(itemTable);
        panel.setTop(itemToolbar);
        panel.setBottom(totalLabel);
        BorderPane.setMargin(itemToolbar, new Insets(0, 0, 4, 0));
        return Ui.section("Line Items", panel);
    }

    private static TableColumn<ItemRow, String> readOnlyColumn(String title,
            Function<ItemRow, String> value) {
        TableColumn<ItemRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> new ReadOnlyStringWrapper(value.apply(cd.getValue())));
        col.setEditable(false);
        return col;
    }

    private static TableColumn<ItemRow, String> editableColumn(String title,
            Function<ItemRow, StringProperty> property) {
        TableColumn<ItemRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> property.apply(cd.getValue()));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setEditable(true);
        return col;
    }

    /** Accepts a quantity / price edit only when it parses; otherwise keeps the old value. */
    private void commitIfValid(TableColumn.CellEditEvent<ItemRow, String> e, boolean quantity) {
        ItemRow row = e.getRowValue();
        String text = e.getNewValue() == null ? "" : e.getNewValue().trim();
        try {
            if (quantity) {
                row.quantity.set(String.valueOf(Integer.parseInt(text)));
            } else {
                row.unitPrice.set(new BigDecimal(text).toPlainString());
            }
        } catch (NumberFormatException ex) {
            itemTable.refresh();
        }
    }

    private void addItem() {
        List<Product> products = productService.findAll();
        ChoiceDialog<Product> chooser = new ChoiceDialog<>(
                products.isEmpty() ? null : products.get(0), products);
        chooser.initOwner(getDialogPane().getScene().getWindow());
        chooser.setTitle("Add Line Item");
        chooser.setHeaderText(null);
        chooser.setContentText("Select product to add:");
        chooser.getDialogPane().getStylesheets().add(Ui.STYLESHEET);
        Optional<Product> selected = chooser.showAndWait();
        if (selected.isEmpty()) {
            return;
        }
        Product product = selected.get();
        items.add(new ItemRow(new PurchaseOrderItem(product, 1, product.getUnitPrice())));
    }

    private void removeItem() {
        ItemRow row = itemTable.getSelectionModel().getSelectedItem();
        if (row == null) {
            return;
        }
        items.remove(row);
        updateTotal();
    }

    private void updateTotal() {
        // Setting the line totals below fires list updates again
        if (updatingTotal) {
            return;
        }
        updatingTotal = true;
        try {
            BigDecimal total = BigDecimal.ZERO;
            for (ItemRow row : items) {
                try {
                    int qty = Integer.parseInt(row.quantity.get());
                    BigDecimal price = new BigDecimal(row.unitPrice.get());
                    BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(qty));
                    row.lineTotal.set(lineTotal.toPlainString());
                    total = total.add(lineTotal);
                } catch (RuntimeException ignored) {
                    // Unparseable rows do not count, as in the Swing version
                }
            }
            totalLabel.setText("Order Total: " + Ui.CURRENCY.format(total));
        } finally {
            updatingTotal = false;
        }
    }

    private void populateFields() {
        for (Supplier s : supplierBox.getItems()) {
            if (s.getId().equals(existing.getSupplier().getId())) {
                supplierBox.getSelectionModel().select(s);
                break;
            }
        }
        orderDateField.setText(existing.getOrderDate().format(DATE_FMT));
        expectedDateField.setText(existing.getExpectedDate() != null
                ? existing.getExpectedDate().format(DATE_FMT) : "");
        statusBox.getSelectionModel().select(existing.getStatus());
        notesArea.setText(existing.getNotes() != null ? existing.getNotes() : "");

        items.setAll(existing.getItems().stream().map(ItemRow::new).toList());
        updateTotal();
    }

    /** Validates and saves; returns false to keep the dialog open. */
    private boolean doSave() {
        Window window = getDialogPane().getScene().getWindow();
        String dateStr = orderDateField.getText().trim();
        if (supplierBox.getValue() == null || dateStr.isEmpty()) {
            Ui.warn(window, "Validation", "Supplier and Order Date are required.");
            return false;
        }

        LocalDate orderDate;
        try {
            orderDate = LocalDate.parse(dateStr, DATE_FMT);
        } catch (DateTimeParseException e) {
            Ui.warn(window, "Validation", "Order date must be in format yyyy-MM-dd.");
            return false;
        }

        LocalDate expectedDate = null;
        String expStr = expectedDateField.getText().trim();
        if (!expStr.isEmpty()) {
            try {
                expectedDate = LocalDate.parse(expStr, DATE_FMT);
            } catch (DateTimeParseException e) {
                Ui.warn(window, "Validation", "Expected date must be in format yyyy-MM-dd.");
                return false;
            }
        }

        // Sync item quantities/prices from the table
        for (ItemRow row : items) {
            try {
                row.item.setQuantity(Integer.parseInt(row.quantity.get()));
                row.item.setUnitPrice(new BigDecimal(row.unitPrice.get()));
            } catch (RuntimeException ignored) {
                // Keep the previous value
            }
        }

        PurchaseOrder order = existing != null ? existing : new PurchaseOrder();
        order.setSupplier(supplierBox.getValue());
        order.setOrderDate(orderDate);
        order.setExpectedDate(expectedDate);
        order.setStatus(statusBox.getValue());
        order.setNotes(notesArea.getText().trim());

        List<PurchaseOrderItem> lineItems = items.stream().map(r -> r.item).toList();
        if (existing == null) {
            lineItems.forEach(order::addItem);
            try {
                orderService.save(order);
            } catch (Exception ex) {
                Ui.error(window, "Error", "Failed to save order:\n" + ex.getMessage());
                return false;
            }
        } else {
            // Apply added / removed line items to the existing order
            order.getItems().retainAll(lineItems);
            lineItems.stream().filter(i -> !order.getItems().contains(i)).forEach(order::addItem);
            try {
                orderService.update(order);
            } catch (Exception ex) {
                Ui.error(window, "Error", "Failed to update order:\n" + ex.getMessage());
                return false;
            }
        }

        saved = true;
        return true;
    }

    public boolean isSaved() {
        return saved;
    }

    /** One line of the items table; quantity and price are edited as text like the Swing table. */
    private static final class ItemRow {
        final PurchaseOrderItem item;
        final String sku;
        final String name;
        final StringProperty quantity;
        final StringProperty unitPrice;
        final StringProperty lineTotal;

        ItemRow(PurchaseOrderItem item) {
            this.item = item;
            this.sku = item.getProduct().getSku();
            this.name = item.getProduct().getName();
            this.quantity = new SimpleStringProperty(String.valueOf(item.getQuantity()));
            this.unitPrice = new SimpleStringProperty(item.getUnitPrice().toPlainString());
            this.lineTotal = new SimpleStringProperty(item.getLineTotal().toPlainString());
        }
    }
}
