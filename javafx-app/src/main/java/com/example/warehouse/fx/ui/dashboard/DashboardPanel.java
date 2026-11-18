package com.example.warehouse.fx.ui.dashboard;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import com.example.warehouse.fx.ui.util.StatusBar;
import com.example.warehouse.fx.ui.util.Ui;
import com.example.warehouse.model.Product;
import com.example.warehouse.service.ProductService;
import com.example.warehouse.service.PurchaseOrderService;

/**
 * Overview tab: KPI cards, stock and order charts, and the low-stock alert
 * table.
 */
public final class DashboardPanel extends BorderPane {

    private static final String PRIMARY = "#1f497d";
    private static final String PRIMARY_DARK = "#112f58";
    private static final String ACCENT = "#4682b4";
    private static final String SUCCESS = "#008000";
    private static final String WARNING = "#b46400";
    private static final String DANGER = "#b41e1e";
    private static final String CHART_BORDER = "#d2dae6";

    private static final Map<String, String> STATUS_COLORS = Map.of(
            "Delivered", "#32a050",
            "Ordered", "#4682b4",
            "Pending", "#c88c1e",
            "Cancelled", "#b43c3c");

    private final ProductService productService = new ProductService();
    private final PurchaseOrderService orderService = new PurchaseOrderService();
    private final StatusBar statusBar;

    private final Label totalProductsValue = new Label("—");
    private final Label stockValueLabel = new Label("—");
    private final Label pendingOrdersValue = new Label("—");
    private final Label lowStockValue = new Label("—");

    private final XYChart.Series<String, Number> stockSeries = new XYChart.Series<>();
    private final PieChart orderStatusChart = new PieChart();

    private final ObservableList<Product> alerts = FXCollections.observableArrayList();
    private final TableView<Product> alertTable = new TableView<>();

    public DashboardPanel(StatusBar statusBar) {
        this.statusBar = statusBar;
        getStyleClass().add("panel");
        setStyle("-fx-background-color: #f5f7fa; -fx-padding: 10;");

        setTop(buildKpiPanel());
        setCenter(buildChartsPanel());
        setBottom(buildAlertPanel());

        refresh();
    }

    private GridPane buildKpiPanel() {
        GridPane grid = equalColumns(4);
        grid.setHgap(10);
        grid.setPadding(new Insets(0, 0, 8, 0));
        grid.addRow(0,
                kpiCard("Total Products", totalProductsValue, PRIMARY),
                kpiCard("Inventory Value", stockValueLabel, "#00643c"),
                kpiCard("Pending Orders", pendingOrdersValue, "#8c5000"),
                kpiCard("Low Stock Alerts", lowStockValue, DANGER));
        return grid;
    }

    private VBox kpiCard(String label, Label valueLabel, String accentColor) {
        Label caption = new Label(label.toUpperCase());
        caption.getStyleClass().add("kpi-label");

        valueLabel.getStyleClass().add("kpi-value");
        valueLabel.setStyle(textFill(PRIMARY_DARK));

        VBox card = new VBox(4, caption, valueLabel);
        card.getStyleClass().add("kpi-card");
        card.setStyle("-fx-border-color: " + accentColor + "; -fx-border-width: 0 0 0 5;");
        card.setMinHeight(90);
        card.setPrefHeight(90);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private GridPane buildChartsPanel() {
        GridPane grid = equalColumns(2);
        grid.setHgap(10);
        grid.setPadding(new Insets(0, 0, 8, 0));
        grid.addRow(0, buildStockByCategoryChart(), buildOrderStatusChart());
        return grid;
    }

    private BarChart<String, Number> buildStockByCategoryChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Category");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Units in Stock");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Stock Quantity by Category");
        stockSeries.setName("Qty");
        chart.getData().add(stockSeries);
        chart.setVerticalGridLinesVisible(false);
        styleChart(chart, "CHART_COLOR_1: " + ACCENT + ";");
        return chart;
    }

    private PieChart buildOrderStatusChart() {
        orderStatusChart.setTitle("Purchase Orders by Status");
        orderStatusChart.setLegendVisible(true);
        styleChart(orderStatusChart, "");
        return orderStatusChart;
    }

    private static void styleChart(Chart chart, String extraStyle) {
        chart.setAnimated(false);
        chart.setPrefHeight(220);
        chart.setMinHeight(0);
        chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        chart.setStyle(chartStyle(extraStyle));
    }

    private static String chartStyle(String extraStyle) {
        return "-fx-background-color: white; -fx-border-color: " + CHART_BORDER + ";"
                + extraStyle;
    }

    private StackPane buildAlertPanel() {
        Ui.styleTable(alertTable);
        alertTable.getColumns().setAll(List.of(
                textColumn("SKU", Product::getSku),
                textColumn("Product Name", Product::getName),
                textColumn("Category", p -> p.getCategory().getName()),
                textColumn("Supplier",
                        p -> p.getSupplier() != null ? p.getSupplier().getName() : "—"),
                numberColumn("On Hand", Product::getQuantity),
                numberColumn("Reorder Level", Product::getReorderLevel),
                numberColumn("Shortage", p -> p.getReorderLevel() - p.getQuantity())));

        alertTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                boolean critical = !empty && item != null && item.getQuantity() == 0;
                pseudoClassStateChanged(Ui.CRITICAL, critical);
                pseudoClassStateChanged(Ui.LOW_STOCK, !empty && item != null && !critical);
            }
        });

        SortedList<Product> sorted = new SortedList<>(alerts);
        sorted.comparatorProperty().bind(alertTable.comparatorProperty());
        alertTable.setItems(sorted);

        StackPane section = Ui.section("⚠ Low Stock Alerts", alertTable);
        section.setPrefHeight(180);
        section.setMinHeight(180);
        return section;
    }

    public void refresh() {
        long total = productService.count();
        long lowStock = productService.countLowStock();
        long pendingOrders = orderService.countPending();
        BigDecimal value = productService.totalInventoryValue();

        totalProductsValue.setText(String.valueOf(total));
        lowStockValue.setText(String.valueOf(lowStock));
        lowStockValue.setStyle(textFill(lowStock > 0 ? DANGER : SUCCESS));
        pendingOrdersValue.setText(String.valueOf(pendingOrders));
        pendingOrdersValue.setStyle(textFill(pendingOrders > 0 ? WARNING : SUCCESS));
        stockValueLabel.setText(Ui.CURRENCY.format(value));

        refreshCharts();

        List<Product> lowStockProducts = productService.findLowStock();
        alerts.setAll(lowStockProducts);

        statusBar.setMessage("Dashboard refreshed — " + lowStockProducts.size()
                + " low-stock alert(s)");
    }

    private void refreshCharts() {
        ObservableList<XYChart.Data<String, Number>> bars = FXCollections.observableArrayList();
        for (Object[] row : productService.findStockByCategory()) {
            bars.add(new XYChart.Data<>((String) row[0], ((Number) row[1]).doubleValue()));
        }
        stockSeries.getData().setAll(bars);

        ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();
        for (Object[] row : orderService.countByAllStatuses()) {
            slices.add(new PieChart.Data(row[0].toString(), ((Number) row[1]).doubleValue()));
        }
        orderStatusChart.setData(slices);
        applyStatusColors(slices);
    }

    /**
     * Pie slices and legend symbols share a "default-colorN" style class; map
     * each one's looked-up CHART_COLOR to its status color so both match.
     */
    private void applyStatusColors(List<PieChart.Data> slices) {
        StringBuilder colors = new StringBuilder();
        for (PieChart.Data slice : slices) {
            String color = STATUS_COLORS.get(slice.getName());
            if (color == null || slice.getNode() == null) {
                continue;
            }
            slice.getNode().getStyleClass().stream()
                    .filter(c -> c.startsWith("default-color"))
                    .findFirst()
                    .map(c -> Integer.parseInt(c.substring("default-color".length())))
                    .ifPresent(i -> colors.append("CHART_COLOR_").append(i + 1)
                            .append(": ").append(color).append(';'));
        }
        orderStatusChart.setStyle(chartStyle(colors.toString()));
    }

    private static TableColumn<Product, String> textColumn(String title,
            Function<Product, String> value) {
        TableColumn<Product, String> column = new TableColumn<>(title);
        column.setCellValueFactory(c -> new SimpleStringProperty(value.apply(c.getValue())));
        return column;
    }

    private static TableColumn<Product, Integer> numberColumn(String title,
            Function<Product, Integer> value) {
        TableColumn<Product, Integer> column = new TableColumn<>(title);
        column.setCellValueFactory(c -> new SimpleObjectProperty<>(value.apply(c.getValue())));
        column.setStyle("-fx-alignment: CENTER;");
        return column;
    }

    private static GridPane equalColumns(int count) {
        GridPane grid = new GridPane();
        for (int i = 0; i < count; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / count);
            cc.setFillWidth(true);
            grid.getColumnConstraints().add(cc);
        }
        RowConstraints rc = new RowConstraints();
        rc.setVgrow(Priority.ALWAYS);
        rc.setFillHeight(true);
        grid.getRowConstraints().add(rc);
        return grid;
    }

    private static String textFill(String color) {
        return "-fx-text-fill: " + color + ";";
    }
}
