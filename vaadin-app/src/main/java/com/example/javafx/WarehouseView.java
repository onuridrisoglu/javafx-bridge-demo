package com.example.javafx;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.swingbridge.javafx.JavaFXBridge;

/**
 * Stage 1: the whole JavaFX application, its own login screen included, in
 * the browser without a single change.
 */
@Route("")
@PageTitle("WarehousePro 3.5")
public class WarehouseView extends VerticalLayout {

    public WarehouseView() {
        setSizeFull();
        JavaFXBridge bridge = new JavaFXBridge(
                "com.example.warehouse.fx.WarehouseApp",
                AppLibsClassLoader.fromAppLibs());
        add(bridge);
        expand(bridge);
    }
}
