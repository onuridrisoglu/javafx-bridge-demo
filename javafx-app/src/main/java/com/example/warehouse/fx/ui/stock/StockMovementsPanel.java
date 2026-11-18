package com.example.warehouse.fx.ui.stock;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

import com.example.warehouse.dao.StockMovementDAO;
import com.example.warehouse.fx.ui.util.StatusBar;
import com.example.warehouse.fx.ui.util.TableExporter;
import com.example.warehouse.fx.ui.util.Ui;
import com.example.warehouse.model.StockMovement;

/** The Stock Log tab: a read-only, searchable log of stock movements. */
public class StockMovementsPanel extends BorderPane {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm");
    private static final List<String> TYPE_STYLES = List.of("status-ok",
            "status-danger", "status-warning");

    private final StockMovementDAO dao = new StockMovementDAO();
    private final StatusBar statusBar;

    private final ObservableList<StockMovement> movements = FXCollections
            .observableArrayList();
    private final FilteredList<StockMovement> filtered = new FilteredList<>(movements);
    private final TableView<StockMovement> table = new TableView<>();

    public StockMovementsPanel(StatusBar statusBar) {
        this.statusBar = statusBar;
        getStyleClass().add("panel");

        setTop(buildToolbar());
        setCenter(buildTable());

        refresh();
    }

    private Node buildToolbar() {
        Button refreshBtn = Ui.toolbarButton("↺ Refresh");
        refreshBtn.setOnAction(e -> refresh());

        Button exportBtn = Ui.toolbarButton("⬇ Export CSV");
        exportBtn.setOnAction(e -> TableExporter.exportToCsv(table,
                "stock-movements.csv", Ui.windowOf(this)));

        Label note = new Label(
                "Read-only log. Use Products → Stock Adjust to record movements.");
        note.getStyleClass().add("hint");
        note.setStyle("-fx-font-size: 11px;");

        Region strut = new Region();
        strut.setMinWidth(12);

        TextField searchField = new TextField();
        searchField.setPrefColumnCount(18);
        searchField.textProperty().addListener((obs, old, text) -> filterTable(text));

        return Ui.toolbar(
                new Node[] { refreshBtn, Ui.toolbarSeparator(), exportBtn, strut, note },
                new Label("Search:"), searchField);
    }

    private Node buildTable() {
        Ui.styleTable(table);

        TableColumn<StockMovement, String> typeCol = column("Type", 90,
                m -> m.getMovementType().toString());
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll(TYPE_STYLES);
                if (empty || value == null) {
                    setText(null);
                    return;
                }
                setText(value);
                getStyleClass().add(switch (value) {
                case "In" -> "status-ok";
                case "Out" -> "status-danger";
                default -> "status-warning";
                });
            }
        });

        TableColumn<StockMovement, Integer> qtyCol = column("Quantity", 75,
                StockMovement::getQuantity);
        qtyCol.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(List.of(
                column("Date / Time", 130, m -> m.getMovementDate().format(DATE_FMT)),
                column("SKU", 80, m -> m.getProduct().getSku()),
                column("Product Name", 200, m -> m.getProduct().getName()),
                column("Category", 130, m -> m.getProduct().getCategory() != null
                        ? m.getProduct().getCategory().getName() : ""),
                typeCol,
                qtyCol,
                column("Reason", 250, m -> m.getReason() != null ? m.getReason() : "—"),
                column("Created By", 100,
                        m -> m.getCreatedBy() != null ? m.getCreatedBy() : "system")));

        SortedList<StockMovement> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        // IN / OUT rows get a green / red tint; adjustments keep the
        // alternating stripes. Selected rows use the regular selection color.
        table.setRowFactory(tv -> new TableRow<>() {
            {
                selectedProperty().addListener((obs, old, sel) -> applyTint());
            }

            @Override
            protected void updateItem(StockMovement m, boolean empty) {
                super.updateItem(m, empty);
                applyTint();
            }

            private void applyTint() {
                StockMovement m = getItem();
                if (m == null || isEmpty() || isSelected()) {
                    setStyle(null);
                    return;
                }
                setStyle(switch (m.getMovementType()) {
                case IN -> "-fx-background-color: #ebfceb;";
                case OUT -> "-fx-background-color: #fcf0eb;";
                case ADJUSTMENT -> null;
                });
            }
        });
        return table;
    }

    private static <V> TableColumn<StockMovement, V> column(String title, double width,
            Function<StockMovement, V> value) {
        TableColumn<StockMovement, V> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(
                cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        return col;
    }

    /**
     * Shows only rows where any column matches the text, as a case-insensitive
     * regular expression (like Swing's {@code RowFilter.regexFilter}).
     */
    private void filterTable(String text) {
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
        filtered.setPredicate(m -> table.getColumns().stream().anyMatch(col -> {
            Object value = col.getCellData(m);
            return value != null && p.matcher(value.toString()).find();
        }));
    }

    public void refresh() {
        movements.setAll(dao.findAll());
        statusBar.setRowCount(movements.size(), "stock movement");
    }
}
