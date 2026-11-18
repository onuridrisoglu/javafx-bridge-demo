package com.example.warehouse.fx.ui.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Exports the rows a table currently shows (sorted and filtered) as CSV. */
public final class TableExporter {

    private TableExporter() {
    }

    public static <T> void exportToCsv(TableView<T> table, String suggestedName,
            Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export to CSV");
        chooser.setInitialFileName(suggestedName);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }
        try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8)) {
            StringBuilder row = new StringBuilder();
            for (TableColumn<T, ?> column : table.getVisibleLeafColumns()) {
                if (!row.isEmpty()) {
                    row.append(',');
                }
                row.append(escape(column.getText()));
            }
            pw.println(row);
            for (T item : table.getItems()) {
                row.setLength(0);
                boolean first = true;
                for (TableColumn<T, ?> column : table.getVisibleLeafColumns()) {
                    if (!first) {
                        row.append(',');
                    }
                    first = false;
                    Object value = column.getCellData(item);
                    row.append(escape(value != null ? value.toString() : ""));
                }
                pw.println(row);
            }
        } catch (IOException ex) {
            Ui.error(owner, "Export Error", "Export failed: " + ex.getMessage());
        }
    }

    private static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
