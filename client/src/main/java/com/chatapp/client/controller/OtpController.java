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
import java.util.Timer;
import java.util.TimerTask;

public class OtpController {

    @FXML private TextField otp1, otp2, otp3, otp4, otp5, otp6;
    @FXML private Label statusLabel, timerLabel;
    @FXML private Button resendButton;

    private final String username;
    private final ServerConnection connection = ServerConnection.getInstance();
    private Timer countdownTimer;
    private int secondsLeft = 300; // 5 phút

    public OtpController(String username) {
        this.username = username;
    }

    @FXML
    private void initialize() {
        setupOtpFields();
        startCountdown();
    }

    // ==================== OTP INPUT LOGIC ====================
    private void setupOtpFields() {
        TextField[] fields = {otp1, otp2, otp3, otp4, otp5, otp6};
        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            TextField field = fields[i];

            // Chỉ cho nhập số, tối đa 1 ký tự
            field.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*")) {
                    field.setText(oldVal != null ? oldVal : "");
                } else if (newVal.length() > 1) {
                    field.setText(newVal.substring(0, 1));
                }
            });

            // Di chuyển focus khi nhập đủ 1 số
            field.setOnKeyReleased(e -> {
                String text = field.getText();
                if (text.length() == 1 && text.matches("\\d")) {
                    if (index < 5) {
                        fields[index + 1].requestFocus();
                    }
                    if (getOtp().length() == 6) {
                        verifyOtp();
                    }
                }
            });
        }
        // Focus ô đầu tiên
        Platform.runLater(() -> otp1.requestFocus());
    }

    private String getOtp() {
        return otp1.getText() + otp2.getText() + otp3.getText() +
                otp4.getText() + otp5.getText() + otp6.getText();
    }

    // ==================== VERIFY OTP ====================
    private void verifyOtp() {
        disableInput();
        showStatus("Đang xác thực...", "#6c757d");

        new Thread(() -> {
            try {
                Packet request = PacketBuilder.create(MessageType.VERIFY_OTP_REQUEST)
                        .put("username", username)
                        .put("otpCode", getOtp())
                        .build();

                Packet response = connection.sendAndReceive(request);

                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        success("Xác thực thành công! Vui lòng đăng nhập.");
                        stopTimer();
                        openLoginScreen(); // CHUYỂN SANG LOGIN
                    } else {
                        error(response.getString("error"));
                        enableInput();
                        clearOtp();
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    error("Lỗi kết nối: " + e.getMessage());
                    enableInput();
                });
            }
        }).start();
    }

    // ==================== RESEND OTP ====================
    @FXML
    private void resendOtp() {
        resendButton.setDisable(true);
        showStatus("Đang gửi lại OTP...", "#6c757d");

        new Thread(() -> {
            try {
                Packet request = PacketBuilder.create(MessageType.RESEND_OTP_REQUEST)
                        .put("username", username)
                        .build();

                Packet response = connection.sendAndReceive(request);

                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        showStatus("Mã OTP mới đã được gửi!", "#007bff");
                        secondsLeft = 300;
                        clearOtp();
                        enableInput();
                        otp1.requestFocus();
                    } else {
                        showStatus("Gửi thất bại: " + response.getError(), "#dc3545");
                        resendButton.setDisable(secondsLeft <= 60);
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showStatus("Lỗi gửi lại OTP", "#dc3545");
                    resendButton.setDisable(secondsLeft <= 60);
                });
            }
        }).start();
    }

    // ==================== COUNTDOWN TIMER ====================
    private void startCountdown() {
        countdownTimer = new Timer();
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                secondsLeft--;
                int min = secondsLeft / 60;
                int sec = secondsLeft % 60;

                Platform.runLater(() -> {
                    timerLabel.setText(String.format("%02d:%02d", min, sec));

                    if (secondsLeft <= 0) {
                        stopTimer();
                        showStatus("Hết thời gian! Vui lòng đăng ký lại.", "#dc3545");
                        disableInput();
                    } else if (secondsLeft <= 60) {
                        timerLabel.setTextFill(javafx.scene.paint.Color.RED);
                        resendButton.setDisable(false);
                    }
                });
            }
        }, 0, 1000);
    }

    private void stopTimer() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }

    // ==================== NAVIGATE TO LOGIN ====================
    private void openLoginScreen() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) statusLabel.getScene().getWindow();
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/css/auth.css").toExternalForm());

                stage.setScene(scene);
                stage.setTitle("ChatApp - Đăng nhập");
                stage.centerOnScreen();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // ==================== UI HELPERS ====================
    private void showStatus(String msg, String color) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14;");
        });
    }

    private void success(String msg) { showStatus(msg, "#28a745"); }
    private void error(String msg) { showStatus(msg, "#dc3545"); }

    private void disableInput() {
        Platform.runLater(() -> {
            for (TextField f : new TextField[]{otp1, otp2, otp3, otp4, otp5, otp6}) {
                f.setDisable(true);
            }
            resendButton.setDisable(true);
        });
    }

    private void enableInput() {
        Platform.runLater(() -> {
            for (TextField f : new TextField[]{otp1, otp2, otp3, otp4, otp5, otp6}) {
                f.setDisable(false);
            }
            resendButton.setDisable(secondsLeft <= 60);
        });
    }

    private void clearOtp() {
        Platform.runLater(() -> {
            otp1.clear(); otp2.clear(); otp3.clear(); otp4.clear(); otp5.clear(); otp6.clear();
            otp1.requestFocus();
        });
    }
}