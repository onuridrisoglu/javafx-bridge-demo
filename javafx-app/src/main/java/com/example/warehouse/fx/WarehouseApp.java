package com.example.warehouse.fx;

import javafx.application.Application;
import javafx.stage.Stage;

import com.example.warehouse.config.PersistenceUtil;
import com.example.warehouse.fx.ui.LoginView;
import com.example.warehouse.seed.DataSeeder;

/**
 * WarehousePro 3.5, the JavaFX edition. Opens the database, seeds it on first
 * run, and shows the login screen; signing in replaces it with the main
 * window, in the same stage.
 * <p>
 * Everything happens in the primary stage, so the application behaves the
 * same on the desktop and inside the JavaFX bridge, where the primary stage
 * is the page and any other stage becomes a dialog.
 */
public class WarehouseApp extends Application {

    @Override
    public void init() {
        PersistenceUtil.init();
        DataSeeder.seedIfEmpty();
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("WarehousePro 3.5 — Login");
        stage.setWidth(1280);
        stage.setHeight(800);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(LoginView.scene(stage));
        stage.setOnCloseRequest(e -> PersistenceUtil.shutdown());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
