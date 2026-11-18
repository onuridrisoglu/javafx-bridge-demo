package com.example.warehouse.fx.ui.suppliers;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;

import com.example.warehouse.fx.ui.util.StatusBar;
import com.example.warehouse.fx.ui.util.TableExporter;
import com.example.warehouse.fx.ui.util.Ui;
import com.example.warehouse.interop.SupplierEditRequestEvent;
import com.example.warehouse.interop.SupplierEditRequestListener;
import com.example.warehouse.interop.SupplierSelectedEvent;
import com.example.warehouse.interop.SupplierSelectionListener;
import com.example.warehouse.model.Supplier;
import com.example.warehouse.service.SupplierService;

/** The Suppliers tab: a searchable supplier table with add / edit / delete. */
public class SuppliersPanel extends BorderPane {

    private final SupplierService service = new SupplierService();
    private final StatusBar statusBar;

    private final ObservableList<Supplier> suppliers = FXCollections.observableArrayList();
    private final FilteredList<Supplier> filtered = new FilteredList<>(suppliers);
    private final TableView<Supplier> table = new TableView<>();

    private final Button editBtn = Ui.toolbarButton("Edit");
    private final Button deleteBtn = Ui.toolbarButton("Delete");

    private Node toolbar;
    /** Set when a Vaadin view drives this table through the bridge. */
    private SupplierSelectionListener selectionListener;
    private SupplierEditRequestListener editRequestListener;

    public SuppliersPanel(StatusBar statusBar) {
        this.statusBar = statusBar;
        getStyleClass().add("panel");

        toolbar = buildToolbar();
        setTop(toolbar);
        setCenter(buildTable());

        refresh();
    }

    private Node buildToolbar() {
        Button addBtn = Ui.toolbarButton("+ Add Supplier");
        addBtn.getStyleClass().add("add-button");
        Button refreshBtn = Ui.toolbarButton("↺ Refresh");
        Button exportBtn = Ui.toolbarButton("⬇ Export CSV");

        editBtn.setDisable(true);
        deleteBtn.setDisable(true);

        addBtn.setOnAction(e -> openAddDialog());
        editBtn.setOnAction(e -> openEditDialog());
        deleteBtn.setOnAction(e -> deleteSelected());
        refreshBtn.setOnAction(e -> refresh());
        exportBtn.setOnAction(e -> TableExporter.exportToCsv(table, "suppliers.csv",
                Ui.windowOf(this)));

        TextField searchField = new TextField();
        searchField.setPrefColumnCount(18);
        searchField.textProperty().addListener((obs, old, text) -> filterTable(text));

        return Ui.toolbar(
                new Node[] { addBtn, Ui.toolbarSeparator(), editBtn, deleteBtn,
                        Ui.toolbarSeparator(), refreshBtn, Ui.toolbarSeparator(),
                        exportBtn },
                new Label("Search:"), searchField);
    }

    private Node buildTable() {
        Ui.styleTable(table);
        table.getColumns().addAll(List.of(
                column("ID", 40, Supplier::getId),
                column("Supplier Name", 200, Supplier::getName),
                column("Contact Person", 140, Supplier::getContactName),
                column("Email", 180, Supplier::getEmail),
                column("Phone", 120, Supplier::getPhone),
                column("Address", 280, Supplier::getAddress)));

        SortedList<Supplier> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            editBtn.setDisable(sel == null);
            deleteBtn.setDisable(sel == null);
            if (selectionListener != null) {
                selectionListener.onSelectionChanged(
                        new SupplierSelectedEvent(sel == null ? null : sel.getId()));
            }
        });

        MenuItem editItem = new MenuItem("Edit Supplier");
        MenuItem delItem = new MenuItem("Delete Supplier");
        editItem.setOnAction(e -> openEditDialog());
        delItem.setOnAction(e -> deleteSelected());
        ContextMenu menu = new ContextMenu(editItem, delItem);

        table.setRowFactory(tv -> {
            TableRow<Supplier> row = new TableRow<>();
            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty())
                    .then((ContextMenu) null).otherwise(menu));
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2
                        && !row.isEmpty()) {
                    if (editRequestListener != null) {
                        // Driven from Vaadin: its form edits the supplier.
                        editRequestListener.onEditRequested(
                                new SupplierEditRequestEvent(row.getItem().getId()));
                    } else {
                        openEditDialog();
                    }
                }
            });
            return row;
        });
        return table;
    }

    private static <V> TableColumn<Supplier, V> column(String title, double width,
            Function<Supplier, V> value) {
        TableColumn<Supplier, V> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(
                cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        return col;
    }

    /** Shows or hides the toolbar, for a Vaadin toolbar to take its place. */
    public void setToolbarVisible(boolean visible) {
        setTop(visible ? toolbar : null);
    }

    /** Tells {@code listener} about selection changes; {@code null} to stop. */
    public void setSelectionListener(SupplierSelectionListener listener) {
        this.selectionListener = listener;
    }

    /**
     * Sends double-clicks to {@code listener} instead of opening the edit
     * dialog; {@code null} to open the dialog again.
     */
    public void setEditRequestListener(SupplierEditRequestListener listener) {
        this.editRequestListener = listener;
    }

    /**
     * Shows only rows where any column matches the text, as a case-insensitive
     * regular expression (like Swing's {@code RowFilter.regexFilter}).
     */
    public void filterTable(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            filtered.setPredicate(null);
            return;
        }
        Pattern pattern;
        try {
            pattern = Pattern.compile(trimmed, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException ex) {
            pattern = Pattern.compile(Pattern.quote(trimmed), Pattern.CASE_INSENSITIVE);
        }
        Pattern p = pattern;
        filtered.setPredicate(s -> table.getColumns().stream().anyMatch(col -> {
            Object value = col.getCellData(s);
            return value != null && p.matcher(value.toString()).find();
        }));
    }

    public void refresh() {
        suppliers.setAll(service.findAll());
        statusBar.setRowCount(suppliers.size(), "supplier");
    }

    private Supplier getSelectedSupplier() {
        return table.getSelectionModel().getSelectedItem();
    }

    private void openAddDialog() {
        SupplierDialog dlg = new SupplierDialog(Ui.windowOf(this), null);
        dlg.showAndWait();
        if (dlg.isSaved()) {
            refresh();
        }
    }

    private void openEditDialog() {
        Supplier s = getSelectedSupplier();
        if (s == null) {
            return;
        }
        SupplierDialog dlg = new SupplierDialog(Ui.windowOf(this), s);
        dlg.showAndWait();
        if (dlg.isSaved()) {
            refresh();
        }
    }

    private void deleteSelected() {
        Supplier s = getSelectedSupplier();
        if (s == null) {
            return;
        }
        if (!Ui.confirm(Ui.windowOf(this), "Confirm Delete",
                "Delete supplier '" + s.getName() + "'?\n\nThis cannot be undone.")) {
            return;
        }
        try {
            service.delete(s.getId());
            refresh();
            statusBar.setMessage("Supplier '" + s.getName() + "' deleted.");
        } catch (Exception ex) {
            Ui.error(Ui.windowOf(this), "Delete Error",
                    "Cannot delete: supplier may be referenced by products or orders.");
        }
    }
}
