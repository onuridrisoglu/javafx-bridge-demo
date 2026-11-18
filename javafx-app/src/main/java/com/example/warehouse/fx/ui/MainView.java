package com.example.warehouse.fx.ui;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.example.warehouse.AppSession;
import com.example.warehouse.config.PersistenceUtil;
import com.example.warehouse.fx.ui.dashboard.DashboardPanel;
import com.example.warehouse.fx.ui.orders.PurchaseOrdersPanel;
import com.example.warehouse.fx.ui.products.ProductsPanel;
import com.example.warehouse.fx.ui.stock.StockMovementsPanel;
import com.example.warehouse.fx.ui.suppliers.SuppliersPanel;
import com.example.warehouse.fx.ui.util.StatusBar;
import com.example.warehouse.fx.ui.util.Ui;

/**
 * The main window: menu bar, title bar, the five module tabs and the status
 * bar. Counterpart of the Swing version's {@code MainFrame}.
 */
public final class MainView extends BorderPane {

    private static final String[] TABS = { "Dashboard", "Products", "Suppliers",
            "Orders", "Stock Log" };

    private final Stage stage;
    private final StatusBar statusBar;
    private final TabPane tabPane = new TabPane();

    private MainView(Stage stage) {
        this.stage = stage;
        this.statusBar = new StatusBar(AppSession.getCurrentUser().getFullName());
        setTop(new VBox(buildMenuBar(), buildTitleBar()));
        setCenter(buildTabs());
        setBottom(statusBar);
    }

    public static Scene scene(Stage stage) {
        return Ui.styled(new Scene(new MainView(stage)));
    }

    private HBox buildTitleBar() {
        Label title = new Label("WarehousePro™ 3.5");
        title.getStyleClass().add("app-title");
        Label userInfo = new Label(AppSession.getCurrentUser().getFullName()
                + "  [" + AppSession.getCurrentUser().getRole() + "]");
        userInfo.getStyleClass().add("user-info");
        Button logout = new Button("Logout");
        logout.getStyleClass().add("logout-button");
        logout.setFocusTraversable(false);
        logout.setOnAction(e -> doLogout());
        HBox bar = new HBox(title, Ui.hgrow(), userInfo, logout);
        bar.getStyleClass().add("title-bar");
        return bar;
    }

    private TabPane buildTabs() {
        tabPane.getStyleClass().add("main-tabs");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(new Tab(TABS[0], new DashboardPanel(statusBar)),
                new Tab(TABS[1], new ProductsPanel(statusBar)),
                new Tab(TABS[2], new SuppliersPanel(statusBar)),
                new Tab(TABS[3], new PurchaseOrdersPanel(statusBar)),
                new Tab(TABS[4], new StockMovementsPanel(statusBar)));
        return tabPane;
    }

    private MenuBar buildMenuBar() {
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> doLogout());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> confirmExit());
        Menu fileMenu = new Menu("File", null, logoutItem,
                new SeparatorMenuItem(), exitItem);

        Menu viewMenu = new Menu("View");
        for (int i = 0; i < TABS.length; i++) {
            int index = i;
            MenuItem item = new MenuItem(TABS[i]);
            item.setOnAction(e -> tabPane.getSelectionModel().select(index));
            viewMenu.getItems().add(item);
        }

        MenuItem aboutItem = new MenuItem("About WarehousePro");
        aboutItem.setOnAction(e -> Ui.info(stage, "About WarehousePro",
                "WarehousePro™ 3.5\nInventory & Warehouse Management System\n\n"
                        + "© 2009 Acme Logistics Software Inc.\nAll rights reserved.\n\n"
                        + "Build 3.5.0 (2009-11-04)"));
        Menu helpMenu = new Menu("Help", null, aboutItem);

        return new MenuBar(fileMenu, viewMenu, helpMenu);
    }

    private void doLogout() {
        AppSession.logout();
        stage.setTitle("WarehousePro 3.5 — Login");
        stage.setScene(LoginView.scene(stage));
    }

    private void confirmExit() {
        if (Ui.confirm(stage, "Confirm Exit",
                "Are you sure you want to exit WarehousePro?")) {
            // Closing the window rather than exiting the JVM: inside the
            // JavaFX bridge the runtime is shared with every other session.
            PersistenceUtil.shutdown();
            stage.close();
        }
    }
}
