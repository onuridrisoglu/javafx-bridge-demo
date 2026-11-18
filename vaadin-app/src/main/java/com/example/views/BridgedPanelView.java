package com.example.views;

import java.util.function.Consumer;

import com.example.javafx.AppLibsClassLoader;
import com.example.warehouse.fx.bridge.BridgeAppBridge;

import com.vaadin.flow.component.html.Div;
import com.vaadin.swingbridge.javafx.JavaFXBridge;
import com.vaadin.swingbridge.javafx.interop.JavaFXBridgeHandle;

/**
 * Stage 2: one JavaFX module inside the native Vaadin shell. Every route shows
 * the same running application, {@code BridgeApp} in bridge mode, and tells it
 * which module to show through interop, so the user's state in the JavaFX
 * application survives moving between routes. Like the Swing demo's
 * {@code MainFrameBridge::showDashboard}.
 */
abstract class BridgedPanelView extends Div {

    static final String BRIDGE_APP = "com.example.warehouse.fx.bridge.BridgeApp";

    /** Typed calls into the session's {@code BridgeApp}. */
    protected final JavaFXBridgeHandle<BridgeAppBridge> app = JavaFXBridge
            .interop(BRIDGE_APP).of(BridgeAppBridge.class);

    /**
     * @param module
     *            selects this route's module, e.g.
     *            {@code BridgeAppBridge::showProducts}
     */
    BridgedPanelView(Consumer<BridgeAppBridge> module) {
        setSizeFull();
        addClassName("bridged-panel");
        add(new JavaFXBridge(BRIDGE_APP, AppLibsClassLoader.fromAppLibs()));
        // Runs once the application is up, or at once when it already is.
        app.onReady(module);
    }
}
