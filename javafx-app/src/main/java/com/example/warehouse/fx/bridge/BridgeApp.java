package com.example.warehouse.fx.bridge;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import com.example.warehouse.AppSession;
import com.example.warehouse.config.PersistenceUtil;
import com.example.warehouse.fx.ui.dashboard.DashboardPanel;
import com.example.warehouse.fx.ui.orders.PurchaseOrdersPanel;
import com.example.warehouse.fx.ui.products.ProductsPanel;
import com.example.warehouse.fx.ui.stock.StockMovementsPanel;
import com.example.warehouse.fx.ui.suppliers.SuppliersPanel;
import com.example.warehouse.fx.ui.util.StatusBar;
import com.example.warehouse.fx.ui.util.Ui;
import com.example.warehouse.interop.SupplierEditRequestListener;
import com.example.warehouse.interop.SupplierSelectionListener;
import com.example.warehouse.seed.DataSeeder;
import com.vaadin.swingbridge.interop.ExposedMethod;
import com.vaadin.swingbridge.interop.InstanceProvider;

/**
 * The bridge entry point of WarehousePro: the main window in <em>bridge
 * mode</em>, without menu, title bar or tabs, showing one module at a time.
 * The Vaadin application provides the shell (login, navigation) and picks the
 * module through the {@link ExposedMethod} methods, which Swing Bridge's
 * codegen plugin turns into a typed {@code BridgeAppBridge} for the Vaadin
 * views. The JavaFX counterpart of the Swing demo's {@code BridgeMain} and
 * {@code MainFrame} in bridge mode.
 * <p>
 * Every Vaadin route of a user shares this one running application, so moving
 * between modules keeps its state.
 * </p>
 */
public class BridgeApp extends Application {

    /** Per session: the bridge loads the application once per Vaadin session. */
    private static volatile BridgeApp current;

    private final StackPane modules = new StackPane();
    private DashboardPanel dashboard;
    private ProductsPanel products;
    private SuppliersPanel suppliers;
    private PurchaseOrdersPanel orders;
    private StockMovementsPanel stock;

    /**
     * The running application, which the bridge calls the exposed methods on.
     *
     * @return this session's application, or {@code null} before it started
     */
    @InstanceProvider
    public static BridgeApp current() {
        return current;
    }

    @Override
    public void init() {
        PersistenceUtil.init();
        DataSeeder.seedIfEmpty();
    }

    @Override
    public void start(Stage stage) {
        StatusBar statusBar = new StatusBar(AppSession.getUsername());
        dashboard = new DashboardPanel(statusBar);
        products = new ProductsPanel(statusBar);
        suppliers = new SuppliersPanel(statusBar);
        orders = new PurchaseOrdersPanel(statusBar);
        stock = new StockMovementsPanel(statusBar);
        modules.getChildren().addAll(dashboard, products, suppliers, orders,
                stock);
        show(dashboard);

        BorderPane root = new BorderPane(modules);
        root.setBottom(statusBar);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(Ui.styled(new Scene(root, 1200, 700)));
        stage.setOnCloseRequest(e -> PersistenceUtil.shutdown());
        stage.show();
        current = this;
    }

    @ExposedMethod
    public void showDashboard() {
        show(dashboard);
    }

    @ExposedMethod
    public void showProducts() {
        show(products);
    }

    @ExposedMethod
    public void showSuppliers() {
        suppliers.setToolbarVisible(true);
        show(suppliers);
    }

    /**
     * Shows the suppliers module as a plain table, for a Vaadin toolbar and
     * dialogs to drive it (stage 3).
     */
    @ExposedMethod
    public void showSuppliersTable() {
        suppliers.setToolbarVisible(false);
        show(suppliers);
    }

    /** Tells {@code listener} about supplier selection changes. */
    @ExposedMethod
    public void setSupplierSelectionListener(
            SupplierSelectionListener listener) {
        suppliers.setSelectionListener(listener);
    }

    /** Sends supplier edit requests (double-clicks) to {@code listener}. */
    @ExposedMethod
    public void setSupplierEditRequestListener(
            SupplierEditRequestListener listener) {
        suppliers.setEditRequestListener(listener);
    }

    /** Reloads the suppliers, e.g. after a Vaadin form saved one. */
    @ExposedMethod
    public void refreshSuppliers() {
        suppliers.refresh();
    }

    /** Filters the suppliers table as its own search field would. */
    @ExposedMethod
    public void setSupplierFilter(String filter) {
        suppliers.filterTable(filter);
    }

    @ExposedMethod
    public void showOrders() {
        show(orders);
    }

    @ExposedMethod
    public void showStock() {
        show(stock);
    }

    private void show(Node module) {
        for (Node child : modules.getChildren()) {
            boolean shown = child == module;
            child.setVisible(shown);
            child.setManaged(shown);
        }
    }
}
