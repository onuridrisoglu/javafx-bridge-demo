package com.example.warehouse.fx.ui;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.example.warehouse.AppSession;
import com.example.warehouse.dao.UserDAO;
import com.example.warehouse.fx.ui.util.Ui;
import com.example.warehouse.model.User;

/**
 * The sign-in screen, a card centered in the window. Signing in swaps the
 * stage's scene for the {@link MainView}.
 */
public final class LoginView extends StackPane {

    private final Stage stage;
    private final UserDAO userDAO = new UserDAO();
    private final TextField usernameField = new TextField("admin");
    private final PasswordField passwordField = new PasswordField();
    private final Label statusLabel = new Label(" ");

    private LoginView(Stage stage) {
        this.stage = stage;
        getStyleClass().add("login-root");

        BorderPane card = new BorderPane();
        card.getStyleClass().add("login-card");
        card.setMaxSize(420, 360);
        card.setTop(buildBanner());
        card.setCenter(buildForm());
        card.setBottom(buildFooter());
        getChildren().add(card);
    }

    /** Creates the login scene for the given stage. */
    public static Scene scene(Stage stage) {
        LoginView view = new LoginView(stage);
        Scene scene = Ui.styled(new Scene(view));
        view.usernameField.requestFocus();
        return scene;
    }

    private VBox buildBanner() {
        Label appName = new Label("WarehousePro™");
        appName.getStyleClass().add("app-name");
        Label subtitle = new Label("Inventory & Warehouse Management  v3.5");
        subtitle.getStyleClass().add("app-subtitle");
        VBox banner = new VBox(appName, subtitle);
        banner.getStyleClass().add("login-banner");
        return banner;
    }

    private StackPane buildForm() {
        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(10);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setHalignment(HPos.RIGHT);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labels, fields);

        passwordField.setText("admin");
        form.addRow(0, Ui.fieldLabel("Username:"), usernameField);
        form.addRow(1, Ui.fieldLabel("Password:"), passwordField);

        statusLabel.getStyleClass().add("error-text");
        form.add(statusLabel, 0, 2, 2, 1);
        GridPane.setHalignment(statusLabel, HPos.CENTER);

        Button loginButton = Ui.primaryButton("Sign In");
        loginButton.setDefaultButton(true);
        loginButton.setPrefWidth(120);
        loginButton.setOnAction(e -> doLogin());
        form.add(loginButton, 0, 3, 2, 1);
        GridPane.setHalignment(loginButton, HPos.CENTER);

        usernameField.setOnAction(e -> passwordField.requestFocus());

        StackPane outer = new StackPane(Ui.section("Sign In", form));
        outer.setPadding(new Insets(20, 40, 14, 40));
        StackPane.setAlignment(outer.getChildren().get(0), Pos.CENTER);
        return outer;
    }

    private StackPane buildFooter() {
        Label hint = new Label(
                "Default credentials:  admin / admin  |  manager / manager");
        hint.getStyleClass().add("hint");
        StackPane footer = new StackPane(hint);
        footer.getStyleClass().add("login-footer");
        return footer;
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter username and password.");
            return;
        }
        User user = userDAO.authenticate(username, password);
        if (user == null) {
            statusLabel.setText("Invalid username or password.");
            passwordField.clear();
            passwordField.requestFocus();
            return;
        }
        AppSession.login(user);
        stage.setTitle(
                "WarehousePro™ 3.5  —  Inventory & Warehouse Management");
        stage.setScene(MainView.scene(stage));
    }
}
