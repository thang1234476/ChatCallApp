package com.chatapp.client.controller.component;

import com.chatapp.client.service.AuthService;
import com.chatapp.client.service.UserService;
import com.chatapp.common.model.User;
import com.chatapp.common.model.User.UserStatus;
import com.chatapp.common.protocol.Packet;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Optional;

public class ProfileController implements Initializable {

    @FXML private Label avatarLabel;
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextArea statusMessageField;
    @FXML private ComboBox<UserStatus> statusTypeCombo;
    @FXML private Label createdAtLabel;
    @FXML private Label lastSeenLabel;
    @FXML private Label connectionInfoLabel;
    @FXML private Button editBtn;
    @FXML private Button changeAvatarBtn;
    @FXML private HBox actionButtonsBox;

    private User currentUser;
    private UserService userService;
    private boolean isEditMode = false;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize status combo box
        statusTypeCombo.setItems(FXCollections.observableArrayList(UserStatus.values()));
        userService = UserService.getInstance();
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadUserData();
    }

    private void loadUserData() {
        if (currentUser == null) return;

        // Set avatar (first letter of username)
        avatarLabel.setText(currentUser.getUsername().substring(0, 1).toUpperCase());

        // Set fields
        usernameField.setText(currentUser.getUsername());
        fullNameField.setText(currentUser.getFullName() != null ? currentUser.getFullName() : "");
        emailField.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        statusMessageField.setText(currentUser.getStatusMessage() != null ? currentUser.getStatusMessage() : "");
        statusTypeCombo.setValue(currentUser.getStatusType());

        // Set account info
        if (currentUser.getCreatedAt() != null) {
            createdAtLabel.setText(currentUser.getCreatedAt().format(DATE_FORMATTER));
        }
        if (currentUser.getLastSeen() != null) {
            lastSeenLabel.setText(currentUser.getLastSeen().format(DATE_FORMATTER));
        }
        if (currentUser.getIpAddress() != null && currentUser.getPort() != null) {
            connectionInfoLabel.setText(currentUser.getIpAddress() + ":" + currentUser.getPort());
        }
    }

    @FXML
    private void toggleEditMode() {
        isEditMode = !isEditMode;
        updateEditMode();
    }

    private void updateEditMode() {
        // Enable/disable fields
        fullNameField.setEditable(isEditMode);
        emailField.setEditable(isEditMode);
        statusMessageField.setEditable(isEditMode);
        statusTypeCombo.setDisable(!isEditMode);

        // Update styles
        if (isEditMode) {
            fullNameField.getStyleClass().remove("profile-input-readonly");
            emailField.getStyleClass().remove("profile-input-readonly");
            statusMessageField.getStyleClass().remove("profile-input-readonly");
            editBtn.setText("Cancel Edit");
        } else {
            fullNameField.getStyleClass().add("profile-input-readonly");
            emailField.getStyleClass().add("profile-input-readonly");
            statusMessageField.getStyleClass().add("profile-input-readonly");
            editBtn.setText("Edit");
        }

        // Show/hide buttons
        changeAvatarBtn.setVisible(isEditMode);
        changeAvatarBtn.setManaged(isEditMode);
        actionButtonsBox.setVisible(isEditMode);
        actionButtonsBox.setManaged(isEditMode);
    }

    @FXML
    private void saveProfile() {
        if (currentUser == null) return;

        // Validate input
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String statusMessage = statusMessageField.getText().trim();
        UserStatus statusType = statusTypeCombo.getValue();

        if (fullName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Full name cannot be empty!");
            return;
        }

        if (email.isEmpty() || !isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a valid email!");
            return;
        }

        // Update user object
        currentUser.setFullName(fullName);
        currentUser.setEmail(email);
        currentUser.setStatusMessage(statusMessage);
        currentUser.setStatusType(statusType);

        // Send update request to server in background thread
        new Thread(() -> {
            try {
                Packet response = userService.updateProfile(currentUser);

                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully!");
                        isEditMode = false;
                        updateEditMode();
                        loadUserData();
                    } else {
                        String errorMsg = response.getError() != null ? response.getError() : "Unknown error";
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to update profile: " + errorMsg);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Error", "Connection error: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void cancelEdit() {
        isEditMode = false;
        updateEditMode();
        loadUserData(); // Reload original data
    }

    @FXML
    private void changeAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Avatar Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(avatarLabel.getScene().getWindow());
        if (selectedFile != null) {
            new Thread(() -> {
                try {
                    // Read file as bytes
                    java.nio.file.Path path = selectedFile.toPath();
                    byte[] fileData = java.nio.file.Files.readAllBytes(path);

                    // Upload to server
                    Packet response = userService.uploadAvatar(
                            currentUser.getId(),
                            fileData,
                            selectedFile.getName()
                    );

                    Platform.runLater(() -> {
                        if (response.isSuccess()) {
                            String avatarUrl = response.getString("avatarUrl");
                            currentUser.setAvatarUrl(avatarUrl);
                            showAlert(Alert.AlertType.INFORMATION, "Success",
                                    "Avatar uploaded successfully! Save profile to apply changes.");
                        } else {
                            String errorMsg = response.getError() != null ? response.getError() : "Unknown error";
                            showAlert(Alert.AlertType.ERROR, "Error", "Failed to upload avatar: " + errorMsg);
                        }
                    });

                } catch (java.io.IOException e) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to read avatar file: " + e.getMessage());
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Error", "Connection error: " + e.getMessage());
                    });
                }
            }).start();
        }
    }

    @FXML
    private void openChangePassword() {
        if (currentUser == null || currentUser.getId() == null) {
            showAlert(Alert.AlertType.WARNING, "Error", "User information not available!");
            return;
        }

        ChangePasswordDialog dialog = new ChangePasswordDialog(currentUser.getId());
        dialog.show();
    }

    // Trong ProfileController.java - Method logout()

    @FXML
    private void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Đăng xuất");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc muốn đăng xuất?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    System.out.println("[LOGOUT] ✅ Đang đăng xuất...");

                    // ✅ Set status OFFLINE và logout
                    UserService userService = UserService.getInstance();
                    if (currentUser != null && currentUser.getId() != null) {
                        // Set status OFFLINE trước khi logout
                        userService.updateStatus(currentUser.getId(), User.UserStatus.OFFLINE);
                        System.out.println("[LOGOUT] Status set to OFFLINE");
                    }

                    // Logout
                    AuthService authService = AuthService.getInstance();
                    authService.logout();

                    Platform.runLater(() -> {
                        try {
                            closeProfile();
                            Stage stage = (Stage) Stage.getWindows().stream()
                                    .filter(Window::isShowing)
                                    .findFirst()
                                    .orElse(null);

                            if (stage != null) {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
                                Parent root = loader.load();

                                Scene scene = new Scene(root, 1000, 650);
                                scene.getStylesheets().clear();
                                scene.getStylesheets().add(getClass().getResource("/css/auth.css").toExternalForm());

                                stage.setScene(scene);
                                stage.setTitle("ChatApp - Đăng nhập");
                                stage.setResizable(false);
                                stage.centerOnScreen();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đăng xuất: " + e.getMessage());
                    });
                }
            }).start();
        }
    }

    @FXML
    private void closeProfile() {
        // Close the profile window
        Stage stage = (Stage) avatarLabel.getScene().getWindow();
        stage.close();
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}