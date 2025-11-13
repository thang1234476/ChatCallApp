package com.chatapp.client.controller.component;

import com.chatapp.client.service.UserService;
import com.chatapp.common.protocol.Packet;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Pair;

import java.util.Optional;

public class ChangePasswordDialog {

    private final Long userId;

    public ChangePasswordDialog(Long userId) {
        this.userId = userId;
    }

    public void show() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("🔒 Update your password securely");

        // Add custom CSS
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/dialog.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("modern-dialog");

        ButtonType changeButtonType = new ButtonType("Change", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(25, 40, 20, 40));

        PasswordField oldPassword = createPasswordField("Current password");
        PasswordField newPassword = createPasswordField("New password");
        PasswordField confirmPassword = createPasswordField("Confirm new password");

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.web("#e53935"));
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);

        grid.add(new Label("Current Password:"), 0, 0);
        grid.add(oldPassword, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPassword, 1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2);
        grid.add(confirmPassword, 1, 2);
        grid.add(errorLabel, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Node changeButton = dialog.getDialogPane().lookupButton(changeButtonType);
        changeButton.setDisable(true);

        // Validation binding
        oldPassword.textProperty().addListener((o, a, b) ->
                validateInput(oldPassword, newPassword, confirmPassword, errorLabel, changeButton));
        newPassword.textProperty().addListener((o, a, b) ->
                validateInput(oldPassword, newPassword, confirmPassword, errorLabel, changeButton));
        confirmPassword.textProperty().addListener((o, a, b) ->
                validateInput(oldPassword, newPassword, confirmPassword, errorLabel, changeButton));

        Platform.runLater(oldPassword::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == changeButtonType) {
                return new Pair<>(oldPassword.getText(), newPassword.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(passwords -> {
            String oldPass = passwords.getKey();
            String newPass = passwords.getValue();

            // Disable dialog UI while processing
            dialog.getDialogPane().lookupButton(changeButtonType).setDisable(true);
            dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setDisable(true);
            errorLabel.setText("Processing...");
            errorLabel.setTextFill(Color.web("#0078ff"));
            errorLabel.setVisible(true);

            new Thread(() -> {
                try {
                    UserService userService = UserService.getInstance();
                    Packet response = userService.changePassword(userId, oldPass, newPass);

                    Platform.runLater(() -> {
                        if (response.isSuccess()) {
                            showSuccessAlert();
                        } else {
                            String errorMsg = response.getError() != null ?
                                    response.getError() : "Failed to change password";
                            showErrorAlert(errorMsg);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() ->
                            showErrorAlert("Connection error: " + e.getMessage()));
                }
            }).start();
        });
    }

    private PasswordField createPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.getStyleClass().add("modern-input");
        field.setPrefWidth(260);
        return field;
    }

    private void validateInput(PasswordField oldPassword, PasswordField newPassword,
                               PasswordField confirmPassword, Label errorLabel,
                               Node changeButton) {

        errorLabel.setVisible(false);
        String oldPass = oldPassword.getText();
        String newPass = newPassword.getText();
        String confirmPass = confirmPassword.getText();

        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            changeButton.setDisable(true);
            return;
        }

        if (newPass.length() < 6) {
            showError(errorLabel, changeButton, "Password must be at least 6 characters");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showError(errorLabel, changeButton, "Passwords do not match");
            return;
        }

        if (oldPass.equals(newPass)) {
            showError(errorLabel, changeButton, "New password must differ from current one");
            return;
        }

        changeButton.setDisable(false);
    }

    private void showError(Label label, Node button, String message) {
        label.setText(message);
        label.setVisible(true);
        button.setDisable(true);
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("✅ Password changed successfully");
        alert.setContentText("Your new password has been updated securely.");
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("❌ Password Change Failed");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
