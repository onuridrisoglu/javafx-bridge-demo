package com.example.warehouse.interop;

/** The supplier the user asked to edit in the JavaFX suppliers table. */
public class SupplierEditRequestEvent {

    /** The supplier's id. */
    public final Long id;

    public SupplierEditRequestEvent(Long id) {
        this.id = id;
    }
}
