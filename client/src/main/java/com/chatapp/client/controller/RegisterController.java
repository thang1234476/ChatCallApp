package com.chatapp.client.controller;

import com.chatapp.client.service.AuthService;
import com.chatapp.common.protocol.Packet;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;

    private AuthService authService;

    @FXML
    public void initialize() {
        authService = AuthService.getInstance();
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Email không hợp lệ");
            return;
        }

        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp");
            return;
        }

        registerButton.setDisable(true);
        registerButton.setText("Đang đăng ký...");

        new Thread(() -> {
            try {
                Packet response = authService.register(username, email, password, username);
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        Platform.runLater(() -> {
                            try {
                                // Load OTP layout
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/otp.fxml"));
                                loader.setController(new OtpController(username));
                                Parent otpRoot = loader.load();

                                // LẤY CỬA SỔ HIỆN TẠI
                                Stage stage = (Stage) registerButton.getScene().getWindow();
                                Scene scene = new Scene(otpRoot);

                                // Dùng CSS giống register
                                scene.getStylesheets().add(getClass().getResource("/css/auth.css").toExternalForm());

                                stage.setScene(scene);
                                stage.setTitle("Xác thực OTP");
                                stage.centerOnScreen();

                            } catch (IOException e) {
                                e.printStackTrace();
                                showError("Không thể mở OTP");
                            }
                        });
                        return;
                    } else {
                        showError(response.getError());
                        registerButton.setDisable(false);
                        registerButton.setText("REGISTER");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Đăng ký thất bại: " + e.getMessage());
                    registerButton.setDisable(false);
                    registerButton.setText("REGISTER");
                });
            }
        }).start();
    }

    @FXML
    private void handleLogin() {
        try {
            Stage stage = (Stage) registerButton.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1000, 650);
            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource("/css/auth.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("ChatApp - Đăng nhập");
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thành công");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}