package com.example.warehouse.interop;

/**
 * Told when the supplier selection in the JavaFX suppliers table changes.
 * Shared by the JavaFX application and the Vaadin host (a Vaadin view
 * implements it through {@code @VaadinCallback}), so it lives here, loaded
 * from the host for both sides.
 */
public interface SupplierSelectionListener {
    void onSelectionChanged(SupplierSelectedEvent event);
}
