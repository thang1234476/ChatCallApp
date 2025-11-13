package com.chatapp.client.service;

import com.chatapp.client.util.PreferenceManager;
import com.chatapp.common.model.User;
import com.chatapp.common.protocol.Packet;
import com.chatapp.common.protocol.PacketBuilder;
import com.chatapp.common.protocol.MessageType;
import com.chatapp.client.network.ServerConnection;

/**
 * Service class to handle user-related operations
 */
public class UserService {

    private static UserService instance;
    private ServerConnection connection;
    private User currentUser;

    private UserService() {
        this.connection = ServerConnection.getInstance();
    }

    public static UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    /**
     * Update user profile on server
     * @param user Updated user object
     * @return Response packet from server
     * @throws Exception if connection error or server error
     */
    public Packet updateProfile(User user) throws Exception {
        if (!connection.isConnected()) {
            throw new Exception("Not connected to server");
        }

        System.out.println("[UserService] Updating profile for user: " + user.getUsername());

        // Build request packet
        Packet request = PacketBuilder.create(MessageType.UPDATE_PROFILE_REQUEST)
                .put("userId", user.getId())
                .put("fullName", user.getFullName())
                .put("email", user.getEmail())
                .put("statusMessage", user.getStatusMessage())
                .put("statusType", user.getStatusType().toString())
                .build();

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            request.put("avatarUrl", user.getAvatarUrl());
        }

        // Send and receive response
        Packet response = connection.sendAndReceive(request);

        if (response.isSuccess()) {
            System.out.println("[UserService] Profile updated successfully");
        } else {
            System.err.println("[UserService] Profile update failed: " + response.getError());
        }

        return response;
    }

    /**
     * Change user password
     * @param userId User ID
     * @param oldPassword Current password
     * @param newPassword New password
     * @return Response packet from server
     * @throws Exception if connection error or server error
     */
    public Packet changePassword(Long userId, String oldPassword, String newPassword) throws Exception {
        if (!connection.isConnected()) {
            throw new Exception("Not connected to server");
        }

        System.out.println("[UserService] Changing password for userId: " + userId);

        Packet request = PacketBuilder.create(MessageType.CHANGE_PASSWORD_REQUEST)
                .put("userId", userId)
                .put("oldPassword", oldPassword)
                .put("newPassword", newPassword)
                .build();

        Packet response = connection.sendAndReceive(request);

        if (response.isSuccess()) {
            System.out.println("[UserService] Password changed successfully");
        } else {
            System.err.println("[UserService] Password change failed: " + response.getError());
        }

        return response;
    }

    /**
     * Update user status (online, offline, busy)
     * @param userId User ID
     * @param status New status
     * @return Response packet from server
     * @throws Exception if connection error or server error
     */
    public Packet updateStatus(Long userId, User.UserStatus status) throws Exception {
        if (!connection.isConnected()) {
            throw new Exception("Not connected to server");
        }

        System.out.println("[UserService] Updating status to: " + status);

        Packet request = PacketBuilder.create(MessageType.STATUS_UPDATE)
                .put("userId", userId)
                .put("status", status.toString())
                .build();

        Packet response = connection.sendAndReceive(request);

        if (response.isSuccess()) {
            System.out.println("[UserService] Status updated successfully");
        } else {
            System.err.println("[UserService] Status update failed: " + response.getError());
        }

        return response;
    }

    /**
     * Upload avatar file to server
     * @param userId User ID
     * @param avatarFile Avatar file bytes
     * @param fileName File name
     * @return Response packet from server with avatar URL
     * @throws Exception if connection error or server error
     */
    public Packet uploadAvatar(Long userId, byte[] avatarFile, String fileName) throws Exception {
        if (!connection.isConnected()) {
            throw new Exception("Not connected to server");
        }

        System.out.println("[UserService] Uploading avatar: " + fileName);

        Packet request = PacketBuilder.create(MessageType.UPLOAD_AVATAR_REQUEST)
                .put("userId", userId)
                .put("fileName", fileName)
                .put("fileData", avatarFile)
                .build();

        Packet response = connection.sendAndReceive(request);

        if (response.isSuccess()) {
            String avatarUrl = response.getString("avatarUrl");
            System.out.println("[UserService] Avatar uploaded successfully: " + avatarUrl);
        } else {
            System.err.println("[UserService] Avatar upload failed: " + response.getError());
        }

        return response;
    }

    public void logout() {
        if (currentUser != null) {
            System.out.println("[AUTH] Logout: " + currentUser.getUsername());
            currentUser = null;
            PreferenceManager.getInstance().clearCurrentUser();
        }
        connection.disconnect();
    }

    /**
     * Get user information by ID from server
     * @param userId User ID
     * @return Response packet from server with user data
     * @throws Exception if connection error or server error
     */
    public Packet getUserInfo(Long userId) throws Exception {
        if (!connection.isConnected()) {
            throw new Exception("Not connected to server");
        }

        System.out.println("[UserService] Getting user info for userId: " + userId);

        Packet request = PacketBuilder.create(MessageType.GET_USER_INFO_REQUEST)
                .put("userId", userId)
                .build();

        Packet response = connection.sendAndReceive(request);

        if (response.isSuccess()) {
            System.out.println("[UserService] User info retrieved successfully");
        } else {
            System.err.println("[UserService] Failed to get user info: " + response.getError());
        }

        return response;
    }

    public User getCurrentUser() {
        return currentUser;
    }
}