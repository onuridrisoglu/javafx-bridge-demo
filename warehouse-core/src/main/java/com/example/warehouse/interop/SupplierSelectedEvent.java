package com.example.warehouse.interop;

/** The supplier now selected in the JavaFX suppliers table. */
public class SupplierSelectedEvent {

    /** The selected supplier's id, or {@code null} when nothing is selected. */
    public final Long id;

    public SupplierSelectedEvent(Long id) {
        this.id = id;
    }
}
