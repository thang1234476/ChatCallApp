// src/main/java/com/chatapp/client/controller/ResetPasswordController.java
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
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class ResetPasswordController {

    @FXML private TextField otp1, otp2, otp3, otp4, otp5, otp6;
    @FXML private PasswordField newPassField, confirmPassField;
    @FXML private Label statusLabel;
    @FXML private Button resetButton;

    private final String email;
    private final ServerConnection connection = ServerConnection.getInstance();

    public ResetPasswordController(String email) {
        this.email = email;
    }

    @FXML private void initialize() {
        setupOtpFields();
    }

    private void setupOtpFields() {
        TextField[] fields = {otp1, otp2, otp3, otp4, otp5, otp6};
        for (int i = 0; i < fields.length; i++) {
            final int idx = i;
            TextField f = fields[i];
            f.textProperty().addListener((obs, o, n) -> {
                if (!n.matches("\\d*")) f.setText(o);
                else if (n.length() > 1) f.setText(n.substring(0, 1));
            });
            f.setOnKeyReleased(e -> {
                if (f.getText().length() == 1 && idx < 5) fields[idx + 1].requestFocus();
                if (getOtp().length() == 6) resetPassword();
            });
        }
        Platform.runLater(() -> otp1.requestFocus());
    }

    private String getOtp() {
        return otp1.getText() + otp2.getText() + otp3.getText() +
                otp4.getText() + otp5.getText() + otp6.getText();
    }

    @FXML
    private void resetPassword() {
        String otp = getOtp();
        String newPass = newPassField.getText();
        String confirm = confirmPassField.getText();

        if (otp.length() != 6) {
            showError("Vui lòng nhập đủ 6 số OTP");
            return;
        }
        if (!newPass.equals(confirm)) {
            showError("Mật khẩu xác nhận không khớp");
            return;
        }
        if (newPass.length() < 6) {
            showError("Mật khẩu phải từ 6 ký tự trở lên");
            return;
        }

        disableInput();
        showStatus("Đang xử lý...", "#6c757d");

        new Thread(() -> {
            try {
                Packet req = PacketBuilder.create(MessageType.RESET_PASSWORD_REQUEST)
                        .put("email", email)
                        .put("otpCode", otp)
                        .put("newPassword", newPass)
                        .build();

                Packet res = connection.sendAndReceive(req);

                Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        showSuccess("Đặt lại mật khẩu thành công!");
                        new Alert(Alert.AlertType.INFORMATION,
                                "Bạn có thể đăng nhập bằng mật khẩu mới.", ButtonType.OK)
                                .showAndWait();
                        openLogin();
                    } else {
                        showError(res.getString("error"));
                        enableInput();
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showError("Lỗi kết nối server");
                    enableInput();
                });
            }
        }).start();
    }

    @FXML
    private void resendOtp() {
        // Gọi lại FORGOT_PASSWORD_REQUEST để gửi OTP mới
        new Thread(() -> {
            try {
                Packet req = PacketBuilder.create(MessageType.FORGOT_PASSWORD_REQUEST)
                        .put("email", email)
                        .build();
                connection.sendAndReceive(req);
                Platform.runLater(() -> showSuccess("OTP mới đã được gửi!"));
            } catch (IOException e) {
                Platform.runLater(() -> showError("Gửi lại thất bại"));
            }
        }).start();
    }

    @FXML
    private void backToLogin() {
        openScene("/view/login.fxml", "Đăng nhập");
    }

    private void openLogin() {
        openScene("/view/login.fxml", "Đăng nhập");
    }

    private void openScene(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) statusLabel.getScene().getWindow();
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

    private void disableInput() {
        Platform.runLater(() -> resetButton.setDisable(true));
    }

    private void enableInput() {
        Platform.runLater(() -> resetButton.setDisable(false));
    }
}