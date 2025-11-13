package com.chatapp.client.controller.component;

import com.chatapp.client.service.FriendService;
import com.chatapp.common.model.Friend;
import com.chatapp.common.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller cho Contact Detail View
 */
public class ContactDetailController implements Initializable {

    @FXML private Label contactAvatarLabel;
    @FXML private Label contactNameLabel;
    @FXML private Label contactStatusLabel;
    @FXML private Button messageBtn;
    @FXML private Button videoCallBtn;
    @FXML private Button menuBtn;

    @FXML private VBox emptyState;
    @FXML private VBox infoSection;
    @FXML private Label largeAvatarLabel;
    @FXML private Label fullNameLabel;
    @FXML private Label statusTextLabel;
    @FXML private Label usernameLabel;
    @FXML private Label bioLabel;
    @FXML private Label emailLabel;
    @FXML private CheckBox notificationsToggle;
    @FXML private Button blockBtn;
    @FXML private Button removeBtn;

    private Friend currentContact;
    private User currentUser;
    private FriendService friendService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        friendService = FriendService.getInstance();
        showEmptyState();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Hiển thị thông tin contact
     */
    public void showContact(Friend friend) {
        this.currentContact = friend;

        if (friend == null) {
            showEmptyState();
            return;
        }

        // Hide empty state, show info
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        infoSection.setVisible(true);
        infoSection.setManaged(true);

        // Header
        String initial = friend.getFriendUsername().substring(0, 1).toUpperCase();
        contactAvatarLabel.setText(initial);
        largeAvatarLabel.setText(initial);

        contactNameLabel.setText(friend.getFriendFullName());
        fullNameLabel.setText(friend.getFriendFullName());

        String status = getStatusText(friend.getFriendStatusType());
        contactStatusLabel.setText(status);
        statusTextLabel.setText(status);

        // Details
        usernameLabel.setText("@" + friend.getFriendUsername());

        if (friend.getFriendStatusMessage() != null && !friend.getFriendStatusMessage().isEmpty()) {
            bioLabel.setText(friend.getFriendStatusMessage());
        } else {
            bioLabel.setText("No bio");
        }

        // Email - cần load từ server nếu cần
        emailLabel.setText("Not available");
    }

    /**
     * Hiển thị empty state
     */
    private void showEmptyState() {
        emptyState.setVisible(true);
        emptyState.setManaged(true);
        infoSection.setVisible(false);
        infoSection.setManaged(false);

        contactNameLabel.setText("Select a contact");
        contactStatusLabel.setText("to view details");
    }

    @FXML
    private void sendMessage() {
        if (currentContact == null) return;
        showInfo("Opening chat with " + currentContact.getFriendFullName());
        // TODO: Switch to chat view
    }

    @FXML
    private void startVideoCall() {
        if (currentContact == null) return;
        showInfo("Starting video call with " + currentContact.getFriendFullName());
        // TODO: Implement video call
    }

    @FXML
    private void openMenu() {
        if (currentContact == null) return;

        ContextMenu menu = new ContextMenu();

        MenuItem viewProfile = new MenuItem("View Full Profile");
        viewProfile.setOnAction(e -> showInfo("View profile coming soon"));

        MenuItem exportChat = new MenuItem("Export Chat");
        exportChat.setOnAction(e -> showInfo("Export chat coming soon"));

        menu.getItems().addAll(viewProfile, exportChat);
        menu.show(menuBtn, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    @FXML
    private void blockContact() {
        if (currentContact == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Block Contact");
        alert.setHeaderText("Block " + currentContact.getFriendFullName() + "?");
        alert.setContentText("They won't be able to call or message you.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    friendService.blockUser(currentUser.getId(), currentContact.getFriendId());
                    Platform.runLater(() -> {
                        showSuccess("Contact blocked");
                        showEmptyState();
                        // TODO: Refresh friend list
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Failed to block: " + e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    private void removeContact() {
        if (currentContact == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Friend");
        alert.setHeaderText("Remove " + currentContact.getFriendFullName() + " from your contacts?");
        alert.setContentText("You can send them a friend request again later.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    friendService.unfriend(currentUser.getId(), currentContact.getFriendId());
                    Platform.runLater(() -> {
                        showSuccess("Friend removed");
                        showEmptyState();
                        // TODO: Refresh friend list
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Failed to remove: " + e.getMessage()));
                }
            }).start();
        }
    }

    private String getStatusText(User.UserStatus status) {
        switch (status) {
            case ONLINE: return "online";
            case BUSY: return "busy";
            default: return "offline";
        }
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}