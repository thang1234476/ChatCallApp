package com.chatapp.client.network;

import com.chatapp.common.protocol.Packet;
import com.chatapp.common.protocol.MessageType;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.Map;

/**
 * Handler để xử lý các response từ server về client
 * Đặc biệt xử lý các response liên quan đến Profile
 */
public class MessageHandler {

    /**
     * Xử lý packet nhận được từ server
     */
    public static void handlePacket(Packet packet) {
        if (packet == null) return;

        MessageType type = packet.getType();
        Map<String, Object> data = packet.getData();

        switch (type) {
            case UPDATE_PROFILE_RESPONSE:
                handleUpdateProfileResponse(data);
                break;

            case CHANGE_PASSWORD_RESPONSE:
                handleChangePasswordResponse(data);
                break;

            case UPLOAD_AVATAR_RESPONSE:
                handleUploadAvatarResponse(data);
                break;

            case GET_USER_INFO_RESPONSE:
                handleGetUserInfoResponse(data);
                break;

            // Thêm các case khác ở đây
            case LOGIN_RESPONSE:
                handleLoginResponse(data);
                break;

            case RECEIVE_MESSAGE:
                handleReceiveMessage(data);
                break;

            case STATUS_UPDATE:
                handleStatusUpdate(data);
                break;

            case ERROR:
                handleError(data);
                break;

            default:
                System.out.println("Unhandled message type: " + type);
                break;
        }
    }

    /**
     * Xử lý response cập nhật profile
     */
    private static void handleUpdateProfileResponse(Map<String, Object> data) {
        boolean success = (boolean) data.getOrDefault("success", false);
        String message = (String) data.getOrDefault("message", "Unknown response");

        Platform.runLater(() -> {
            Alert alert = new Alert(
                    success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR
            );
            alert.setTitle(success ? "Success" : "Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });

        if (success) {
            System.out.println("Profile updated successfully");
            // TODO: Update UI with new user data
        } else {
            System.err.println("Failed to update profile: " + message);
        }
    }

    /**
     * Xử lý response đổi mật khẩu
     */
    private static void handleChangePasswordResponse(Map<String, Object> data) {
        boolean success = (boolean) data.getOrDefault("success", false);
        String message = (String) data.getOrDefault("message", "Unknown response");

        Platform.runLater(() -> {
            Alert alert = new Alert(
                    success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR
            );
            alert.setTitle(success ? "Success" : "Error");
            alert.setHeaderText(success ? "Password Changed" : "Password Change Failed");
            alert.setContentText(message);
            alert.show();
        });

        System.out.println("Change password response: " + message);
    }

    /**
     * Xử lý response upload avatar
     */
    private static void handleUploadAvatarResponse(Map<String, Object> data) {
        boolean success = (boolean) data.getOrDefault("success", false);
        String message = (String) data.getOrDefault("message", "Unknown response");
        String avatarUrl = (String) data.get("avatarUrl");

        if (success && avatarUrl != null) {
            System.out.println("Avatar uploaded successfully: " + avatarUrl);
            // TODO: Update avatar in UI
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText("Avatar Uploaded");
                alert.setContentText("Your avatar has been updated!");
                alert.show();
            });
        } else {
            System.err.println("Failed to upload avatar: " + message);
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Upload Failed");
                alert.setContentText(message);
                alert.show();
            });
        }
    }

    /**
     * Xử lý response lấy thông tin user
     */
    private static void handleGetUserInfoResponse(Map<String, Object> data) {
        boolean success = (boolean) data.getOrDefault("success", false);

        if (success) {
            // TODO: Parse user data và update UI
            System.out.println("User info received");
        } else {
            String message = (String) data.getOrDefault("message", "Failed to get user info");
            System.err.println(message);
        }
    }

    /**
     * Xử lý login response
     */
    private static void handleLoginResponse(Map<String, Object> data) {
        // TODO: Implement login response handling
        System.out.println("Login response received");
    }

    /**
     * Xử lý tin nhắn nhận được
     */
    private static void handleReceiveMessage(Map<String, Object> data) {
        // TODO: Implement message receiving
        System.out.println("Message received");
    }

    /**
     * Xử lý cập nhật status
     */
    private static void handleStatusUpdate(Map<String, Object> data) {
        // TODO: Implement status update handling
        System.out.println("Status update received");
    }

    /**
     * Xử lý error từ server
     */
    private static void handleError(Map<String, Object> data) {
        String errorMessage = (String) data.getOrDefault("message", "Unknown error occurred");
        System.err.println("Server error: " + errorMessage);

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Server Error");
            alert.setHeaderText("An error occurred");
            alert.setContentText(errorMessage);
            alert.show();
        });
    }
}