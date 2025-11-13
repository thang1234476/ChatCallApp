// src/main/java/com/chatapp/client/controller/ForgotPasswordController.java
package com.chatapp.client.controller;

import com.chatapp.client.network.ServerConnection;
import com.chatapp.common.protocol.MessageType;
import com.chatapp.common.protocol.Packet;
import com.chatapp.common.protocol.PacketBuilder;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label statusLabel;
    @FXML private Button sendButton;

    private final ServerConnection connection = ServerConnection.getInstance();

    @FXML private void sendOtp() {
        String email = emailField.getText().trim();
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Email không hợp lệ");
            return;
        }

        sendButton.setDisable(true);
        showStatus("Đang gửi OTP...", "#6c757d");

        new Thread(() -> {
            try {
                Packet req = PacketBuilder.create(MessageType.FORGOT_PASSWORD_REQUEST)
                        .put("email", email)
                        .build();

                Packet res = connection.sendAndReceive(req);

                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        showSuccess("Mã OTP đã được gửi đến email!");
                        openOtpReset(email);
                    } else {
                        showError(res.getString("error"));
                        sendButton.setDisable(false);
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showError("Lỗi kết nối server");
                    sendButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void backToLogin() {
        openScene("/view/login.fxml", "Đăng nhập");
    }

    private void openOtpReset(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/otp_reset.fxml"));
            loader.setController(new ResetPasswordController(email));
            Parent root = loader.load();

            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/auth.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Đặt lại mật khẩu");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openScene(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/auth.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("ChatApp - " + title);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showStatus(String msg, String color) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setStyle("-fx-text-fill: " + color + ";");
        });
    }

    private void showSuccess(String msg) { showStatus(msg, "#27ae60"); }
    private void showError(String msg) { showStatus(msg, "#e74c3c"); }
}