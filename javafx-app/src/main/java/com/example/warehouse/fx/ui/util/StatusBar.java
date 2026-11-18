package com.example.warehouse.fx.ui.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

/**
 * Status line at the bottom of the main window: a message on the left, the
 * user and a clock on the right.
 */
public class StatusBar extends HBox {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm");

    private final Label messageLabel = new Label("Ready");
    private final Label userLabel = new Label();
    private final Label timeLabel = new Label();

    public StatusBar(String username) {
        getStyleClass().add("status-bar");
        setUser(username);
        updateTime();
        getChildren().addAll(messageLabel, Ui.hgrow(), userLabel,
                new Separator(Orientation.VERTICAL), timeLabel);
        Timeline clock = new Timeline(
                new KeyFrame(Duration.seconds(30), e -> updateTime()));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void updateTime() {
        timeLabel.setText(LocalDateTime.now().format(FORMAT));
    }

    public void setUser(String username) {
        userLabel.setText("User: " + username);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void setRowCount(int count, String entity) {
        messageLabel.setText(count + " " + entity + " record"
                + (count != 1 ? "s" : "") + " loaded");
    }
}
