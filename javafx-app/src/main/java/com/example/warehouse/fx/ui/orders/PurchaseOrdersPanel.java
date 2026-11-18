package com.example.warehouse.fx.ui.orders;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import com.example.warehouse.fx.ui.util.StatusBar;
import com.example.warehouse.fx.ui.util.TableExporter;
import com.example.warehouse.fx.ui.util.Ui;
import com.example.warehouse.model.PurchaseOrder;
import com.example.warehouse.model.PurchaseOrderItem;
import com.example.warehouse.service.PurchaseOrderService;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

/** Purchase orders: the order list on top, the selected order's line items below. */
public class PurchaseOrdersPanel extends BorderPane {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PurchaseOrderService service = new PurchaseOrderService();
    private final StatusBar statusBar;

    private final ObservableList<PurchaseOrder> orders = FXCollections.observableArrayList();
    private final ObservableList<PurchaseOrderItem> orderItems = FXCollections.observableArrayList();

    private TableView<PurchaseOrder> orderTable;
    private TableView<PurchaseOrderItem> itemTable;

    private Button editBtn;
    private Button deleteBtn;

    public PurchaseOrdersPanel(StatusBar statusBar) {
        this.statusBar = statusBar;
        getStyleClass().add("panel");

        Node toolbar = buildToolbar();
        BorderPane.setMargin(toolbar, new Insets(0, 0, 8, 0));
        setTop(toolbar);

        SplitPane split = new SplitPane(buildOrderTable(), buildItemPanel());
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.6);
        setCenter(split);

        refresh();
    }

    private Node buildToolbar() {
        Button addBtn = Ui.toolbarButton("+ New Order");
        editBtn = Ui.toolbarButton("Edit / View");
        deleteBtn = Ui.toolbarButton("Delete");
        Button refreshBtn = Ui.toolbarButton("↺ Refresh");
        Button exportBtn = Ui.toolbarButton("⬇ Export CSV");

        addBtn.getStyleClass().add("add-button");
        editBtn.setDisable(true);
        deleteBtn.setDisable(true);

        addBtn.setOnAction(e -> openAddDialog());
        editBtn.setOnAction(e -> openEditDialog());
        deleteBtn.setOnAction(e -> deleteSelected());
        refreshBtn.setOnAction(e -> refresh());
        exportBtn.setOnAction(e -> TableExporter.exportToCsv(orderTable, "orders.csv", Ui.windowOf(this)));

        return Ui.toolbar(new Node[] { addBtn, Ui.toolbarSeparator(), editBtn, deleteBtn,
                Ui.toolbarSeparator(), refreshBtn, Ui.toolbarSeparator(), exportBtn });
    }

    private Node buildOrderTable() {
        orderTable = new TableView<>();
        Ui.styleTable(orderTable);

        TableColumn<PurchaseOrder, String> numberCol = column("Order #", 70,
                o -> String.format("PO-%04d", o.getId()));
        TableColumn<PurchaseOrder, String> supplierCol = column("Supplier", 200,
                o -> o.getSupplier().getName());
        TableColumn<PurchaseOrder, String> orderDateCol = column("Order Date", 100,
                o -> o.getOrderDate().format(DATE_FMT));
        TableColumn<PurchaseOrder, String> expectedCol = column("Expected", 100,
                o -> o.getExpectedDate() != null ? o.getExpectedDate().format(DATE_FMT) : "—");
        TableColumn<PurchaseOrder, String> statusCol = column("Status", 90,
                o -> o.getStatus().toString());
        TableColumn<PurchaseOrder, Integer> itemsCol = column("Items", 60, o -> o.getItems().size());
        TableColumn<PurchaseOrder, String> totalCol = column("Total Amount", 110,
                o -> Ui.CURRENCY.format(o.getTotalAmount()));

        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("status-ok", "status-warning", "status-danger");
                setStyle("");
                if (empty || status == null) {
                    setText(null);
                    return;
                }
                setText(status);
                switch (status) {
                    case "Cancelled" -> getStyleClass().add("status-danger");
                    case "Delivered" -> getStyleClass().add("status-ok");
                    // No accent status class in the stylesheet
                    case "Ordered" -> setStyle("-fx-text-fill: -wp-accent; -fx-font-weight: bold;");
                    default -> getStyleClass().add("status-warning");
                }
            }
        });
        // Values are formatted currency strings; sort by amount, not text
        totalCol.setComparator(Comparator.comparing(PurchaseOrdersPanel::parseCurrency));

        orderTable.getColumns().setAll(List.of(numberCol, supplierCol, orderDateCol,
                expectedCol, statusCol, itemsCol, totalCol));

        SortedList<PurchaseOrder> sorted = new SortedList<>(orders);
        sorted.comparatorProperty().bind(orderTable.comparatorProperty());
        orderTable.setItems(sorted);

        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            editBtn.setDisable(sel == null);
            deleteBtn.setDisable(sel == null);
            showOrderItems();
        });

        MenuItem viewItem = new MenuItem("View / Edit Order");
        MenuItem delItem = new MenuItem("Delete Order");
        viewItem.setOnAction(e -> openEditDialog());
        delItem.setOnAction(e -> deleteSelected());
        ContextMenu menu = new ContextMenu(viewItem, delItem);

        orderTable.setRowFactory(tv -> {
            TableRow<PurchaseOrder> row = new TableRow<>() {
                @Override
                protected void updateItem(PurchaseOrder order, boolean empty) {
                    super.updateItem(order, empty);
                    updateStatusBackground(this);
                }

                @Override
                public void updateSelected(boolean selected) {
                    super.updateSelected(selected);
                    updateStatusBackground(this);
                }
            };
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(menu));
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    openEditDialog();
                }
            });
            return row;
        });

        return Ui.section("Purchase Orders", orderTable);
    }

    /** Tints rows by order status like the Swing renderer; pending rows keep the striping. */
    private static void updateStatusBackground(TableRow<PurchaseOrder> row) {
        PurchaseOrder order = row.getItem();
        if (row.isEmpty() || order == null || row.isSelected()) {
            row.setStyle("");
            return;
        }
        row.setStyle(switch (order.getStatus()) {
            case CANCELLED -> "-fx-background-color: #faebeb;";
            case DELIVERED -> "-fx-background-color: #ebfaeb;";
            case ORDERED -> "-fx-background-color: #ebf2fc;";
            default -> "";
        });
    }

    private Node buildItemPanel() {
        itemTable = new TableView<>(orderItems);
        Ui.styleTable(itemTable);

        TableColumn<PurchaseOrderItem, String> skuCol = column("Product SKU", 75,
                i -> i.getProduct().getSku());
        TableColumn<PurchaseOrderItem, String> nameCol = column("Product Name", 75,
                i -> i.getProduct().getName());
        TableColumn<PurchaseOrderItem, Integer> qtyCol = column("Quantity", 75,
                PurchaseOrderItem::getQuantity);
        TableColumn<PurchaseOrderItem, String> priceCol = column("Unit Price", 75,
                i -> Ui.CURRENCY.format(i.getUnitPrice()));
        TableColumn<PurchaseOrderItem, String> totalCol = column("Line Total", 75,
                i -> Ui.CURRENCY.format(i.getLineTotal()));

        itemTable.getColumns().setAll(List.of(skuCol, nameCol, qtyCol, priceCol, totalCol));
        for (TableColumn<PurchaseOrderItem, ?> col : itemTable.getColumns()) {
            // The Swing detail table has no row sorter
            col.setSortable(false);
        }
        qtyCol.setStyle("-fx-alignment: CENTER;");
        priceCol.setStyle("-fx-alignment: CENTER;");
        totalCol.setStyle("-fx-alignment: CENTER;");

        return Ui.section("Order Line Items", itemTable);
    }

    private static <S, T> TableColumn<S, T> column(String title, double width,
            Function<S, T> value) {
        TableColumn<S, T> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        Callback<TableColumn.CellDataFeatures<S, T>, ObservableValue<T>> factory =
                cd -> new ReadOnlyObjectWrapper<>(value.apply(cd.getValue()));
        col.setCellValueFactory(factory);
        return col;
    }

    private static BigDecimal parseCurrency(String text) {
        try {
            return new BigDecimal(Ui.CURRENCY.parse(text).toString());
        } catch (ParseException | NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private void showOrderItems() {
        PurchaseOrder order = getSelectedOrder();
        if (order == null) {
            orderItems.clear();
        } else {
            orderItems.setAll(order.getItems());
        }
    }

    public void refresh() {
        orderTable.getSelectionModel().clearSelection();
        orders.setAll(service.findAll());
        orderItems.clear();
        statusBar.setRowCount(orders.size(), "purchase order");
    }

    private PurchaseOrder getSelectedOrder() {
        return orderTable.getSelectionModel().getSelectedItem();
    }

    private void openAddDialog() {
        PurchaseOrderDialog dlg = new PurchaseOrderDialog(Ui.windowOf(this), null);
        dlg.showAndWait();
        if (dlg.isSaved()) {
            refresh();
        }
    }

    private void openEditDialog() {
        PurchaseOrder o = getSelectedOrder();
        if (o == null) {
            return;
        }
        PurchaseOrderDialog dlg = new PurchaseOrderDialog(Ui.windowOf(this), o);
        dlg.showAndWait();
        if (dlg.isSaved()) {
            refresh();
        }
    }

    private void deleteSelected() {
        PurchaseOrder o = getSelectedOrder();
        if (o == null) {
            return;
        }
        boolean confirmed = Ui.confirm(Ui.windowOf(this), "Confirm Delete",
                "Delete purchase order PO-" + String.format("%04d", o.getId())
                        + "?\n\nThis cannot be undone.");
        if (confirmed) {
            service.delete(o.getId());
            refresh();
            statusBar.setMessage("Purchase order deleted.");
        }
    }
}
