package com.example;

import com.example.warehouse.config.PersistenceUtil;
import com.example.warehouse.seed.DataSeeder;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Opens and seeds the shared H2 database at startup, for the host's own
 * views (login, modern dashboard). Each bridged JavaFX session opens its own
 * connection to the same database.
 */
@Component
public class WarehouseInitializer {

    @PostConstruct
    void init() {
        PersistenceUtil.init();
        DataSeeder.seedIfEmpty();
    }
}
