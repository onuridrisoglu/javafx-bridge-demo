package com.example.warehouse.fx.ui.suppliers;

import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Window;

import com.example.warehouse.fx.ui.util.Ui;
import com.example.warehouse.model.Supplier;
import com.example.warehouse.service.SupplierService;

/**
 * Add / edit form for a supplier. The dialog saves the supplier itself and
 * only closes once saving succeeded; {@link #isSaved()} tells the caller.
 */
public class SupplierDialog extends Dialog<Supplier> {

    private static final ButtonType SAVE = new ButtonType("Save",
            ButtonBar.ButtonData.OK_DONE);

    private final Supplier existing;
    private final SupplierService service = new SupplierService();
    private boolean saved;
    private Supplier savedSupplier;

    private final TextField nameField = new TextField();
    private final TextField contactField = new TextField();
    private final TextField emailField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextArea addressArea = new TextArea();

    public SupplierDialog(Window owner, Supplier supplier) {
        this.existing = supplier;
        if (owner != null) {
            initOwner(owner);
        }
        setTitle(supplier == null ? "Add New Supplier"
                : "Edit Supplier — " + supplier.getName());
        setResizable(true);

        getDialogPane().getStylesheets().add(Ui.STYLESHEET);
        getDialogPane().setPrefWidth(450);
        getDialogPane().setContent(buildContent());
        getDialogPane().getButtonTypes().addAll(SAVE, ButtonType.CANCEL);

        Button saveButton = (Button) getDialogPane().lookupButton(SAVE);
        saveButton.getStyleClass().add("primary-button");
        saveButton.addEventFilter(ActionEvent.ACTION, e -> {
            if (!doSave()) {
                e.consume();
            }
        });

        setResultConverter(type -> type == SAVE && saved ? savedSupplier : null);

        if (existing != null) {
            populateFields();
        }
        setOnShown(e -> nameField.requestFocus());
    }

    private Node buildContent() {
        Label title = new Label(existing == null ? "New Supplier" : "Edit Supplier");
        title.getStyleClass().add("panel-title");
        title.setStyle("-fx-text-fill: -wp-primary;");

        BorderPane content = new BorderPane();
        content.setTop(title);
        BorderPane.setMargin(title, new Insets(0, 0, 8, 0));
        Node form = Ui.section("Supplier Information", buildForm());
        content.setCenter(form);
        return content;
    }

    private GridPane buildForm() {
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);

        ColumnConstraints labels = new ColumnConstraints();
        labels.setHalignment(HPos.RIGHT);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        fields.setFillWidth(true);
        form.getColumnConstraints().addAll(labels, fields);

        nameField.setPrefColumnCount(24);
        contactField.setPrefColumnCount(24);
        emailField.setPrefColumnCount(24);
        phoneField.setPrefColumnCount(16);
        addressArea.setPrefColumnCount(24);
        addressArea.setPrefRowCount(3);
        addressArea.setWrapText(true);
        for (Control field : new Control[] { nameField, contactField,
                emailField, phoneField, addressArea }) {
            field.setMaxWidth(Double.MAX_VALUE);
        }

        addRow(form, 0, "Company Name: *", nameField);
        addRow(form, 1, "Contact Person:", contactField);
        addRow(form, 2, "Email:", emailField);
        addRow(form, 3, "Phone:", phoneField);

        Label addressLabel = Ui.fieldLabel("Address:");
        GridPane.setValignment(addressLabel, VPos.TOP);
        form.addRow(4, addressLabel, addressArea);

        // Only the address row takes extra height when the dialog grows.
        for (int i = 0; i < 4; i++) {
            form.getRowConstraints().add(new RowConstraints());
        }
        RowConstraints addressRow = new RowConstraints();
        addressRow.setVgrow(Priority.ALWAYS);
        form.getRowConstraints().add(addressRow);
        return form;
    }

    private static void addRow(GridPane form, int row, String label, Node field) {
        form.addRow(row, Ui.fieldLabel(label), field);
    }

    private void populateFields() {
        nameField.setText(existing.getName());
        contactField.setText(orEmpty(existing.getContactName()));
        emailField.setText(orEmpty(existing.getEmail()));
        phoneField.setText(orEmpty(existing.getPhone()));
        addressArea.setText(orEmpty(existing.getAddress()));
    }

    private static String orEmpty(String value) {
        return value != null ? value : "";
    }

    /** Validates and persists the form; returns whether the dialog may close. */
    private boolean doSave() {
        Window window = getDialogPane().getScene().getWindow();
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            Ui.warn(window, "Validation", "Company name is required.");
            nameField.requestFocus();
            return false;
        }
        Supplier s = existing != null ? existing : new Supplier();
        s.setName(name);
        s.setContactName(contactField.getText().trim());
        s.setEmail(emailField.getText().trim());
        s.setPhone(phoneField.getText().trim());
        s.setAddress(addressArea.getText().trim());
        try {
            if (existing == null) {
                service.save(s);
            } else {
                service.update(s);
            }
            saved = true;
            savedSupplier = s;
            return true;
        } catch (Exception ex) {
            Ui.error(window, "Error", "Failed to save:\n" + ex.getMessage());
            return false;
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
