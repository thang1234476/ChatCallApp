package com.chatapp.server.service;

import com.chatapp.common.model.User;
import com.chatapp.common.protocol.Packet;
import com.chatapp.common.protocol.PacketBuilder;
import com.chatapp.common.protocol.MessageType;
import com.chatapp.server.database.dao.UserDAO;
import com.chatapp.server.util.Logger;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Service xử lý các thao tác liên quan đến User
 */
public class UserService {
    private static UserService instance;
    private final UserDAO userDAO;
    private final Logger logger = Logger.getInstance();

    // Thư mục lưu avatar (có thể config trong file properties)
    private static final String AVATAR_UPLOAD_DIR = "uploads/avatars/";
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB

    private UserService() {
        this.userDAO = new UserDAO();
        // Tạo thư mục upload nếu chưa tồn tại
        createUploadDirectory();
    }

    public static UserService getInstance() {
        if (instance == null) {
            synchronized (UserService.class) {
                if (instance == null) {
                    instance = new UserService();
                }
            }
        }
        return instance;
    }

    /**
     * Xử lý cập nhật profile
     */
    public Packet handleUpdateProfile(Packet request) {
        try {
            // Lấy thông tin từ request
            Long userId = request.getLong("userId");
            String fullName = request.getString("fullName");
            String email = request.getString("email");
            String statusMessage = request.getString("statusMessage");
            String statusTypeStr = request.getString("statusType");
            String avatarUrl = request.getString("avatarUrl");

            // Validate
            if (userId == null) {
                return PacketBuilder.create(MessageType.UPDATE_PROFILE_RESPONSE)
                        .error("User ID is required")
                        .build();
            }

            if (fullName == null || fullName.trim().isEmpty()) {
                return PacketBuilder.create(MessageType.UPDATE_PROFILE_RESPONSE)
                        .error("Full name cannot be empty")
                        .build();
            }

            if (email == null || !isValidEmail(email)) {
                return PacketBuilder.create(MessageType.UPDATE_PROFILE_RESPONSE)
                        .error("Invalid email format")
                        .build();
            }

            // Kiểm tra user tồn tại
            User user = userDAO.findById(userId);
            if (user == null) {
                return PacketBuilder.create(MessageType.UPDATE_PROFILE_RESPONSE)
                        .error("User not found")
                        .build();
            }

            // Kiểm tra email đã được dùng bởi user khác chưa
            if (!email.equals(user.getEmail()) && userDAO.existsByEmail(email)) {
                return PacketBuilder.create(MessageType.UPDATE_PROFILE_RESPONSE)
                        .error("Email already in use")
                        .build();
            }

            // Cập nhật thông tin
            user.setFullName(fullName.trim());
            user.setEmail(email.trim());
            user.setStatusMessage(statusMessage != null ? statusMessage.trim() : "");

            if (statusTypeStr != null) {
                try {
                    User.UserStatus statusType = User.UserStatus.valueOf(statusTypeStr);
                    user.setStatusType(statusType);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid status type: " + statusTypeStr);
                }
            }

            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                user.setAvatarUrl(avatarUrl);
            }

            // Lưu vào database
            userDAO.update(user);

            logger.info("Profile updated successfully for user: " + userId);

            // Trả về response thành công
            return PacketBuilder.create(MessageType.UPDATE_PROFILE_RESPONSE)
                    .success(true)
                    .put("message", "Profile updated successfully")
                    .put("user", user)
                    .build();

        } catch (SQLException e) {
            logger.error("Database error while updating profile: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.UPDATE_PROFILE_RESPONSE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error updating profile: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.UPDATE_PROFILE_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xử lý đổi mật khẩu
     */
    public Packet handleChangePassword(Packet request) {
        try {
            // Lấy thông tin từ request
            Long userId = request.getLong("userId");
            String oldPassword = request.getString("oldPassword");
            String newPassword = request.getString("newPassword");

            // Validate
            if (userId == null || oldPassword == null || newPassword == null) {
                return PacketBuilder.create(MessageType.CHANGE_PASSWORD_RESPONSE)
                        .error("Missing required fields")
                        .build();
            }

            if (newPassword.length() < 6) {
                return PacketBuilder.create(MessageType.CHANGE_PASSWORD_RESPONSE)
                        .error("New password must be at least 6 characters")
                        .build();
            }

            // Lấy user từ database
            User user = userDAO.findById(userId);
            if (user == null) {
                return PacketBuilder.create(MessageType.CHANGE_PASSWORD_RESPONSE)
                        .error("User not found")
                        .build();
            }

            // Kiểm tra mật khẩu cũ
            if (!BCrypt.checkpw(oldPassword, user.getPasswordHash())) {
                logger.warn("Wrong password attempt for user: " + userId);
                return PacketBuilder.create(MessageType.CHANGE_PASSWORD_RESPONSE)
                        .error("Current password is incorrect")
                        .build();
            }

            // Kiểm tra mật khẩu mới không trùng mật khẩu cũ
            if (BCrypt.checkpw(newPassword, user.getPasswordHash())) {
                return PacketBuilder.create(MessageType.CHANGE_PASSWORD_RESPONSE)
                        .error("New password must be different from current password")
                        .build();
            }

            // Hash mật khẩu mới
            String newPasswordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());

            // Cập nhật mật khẩu
            userDAO.updatePassword(userId, newPasswordHash);

            logger.info("Password changed successfully for user: " + userId);

            return PacketBuilder.create(MessageType.CHANGE_PASSWORD_RESPONSE)
                    .success(true)
                    .put("message", "Password changed successfully")
                    .build();

        } catch (SQLException e) {
            logger.error("Database error while changing password: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.CHANGE_PASSWORD_RESPONSE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error changing password: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.CHANGE_PASSWORD_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xử lý upload avatar
     */
    public Packet handleUploadAvatar(Packet request) {
        try {
            Long userId = request.getLong("userId");
            String fileName = request.getString("fileName");
            Object fileDataObj = request.get("fileData");

            // Validate
            if (userId == null || fileName == null || fileDataObj == null) {
                return PacketBuilder.create(MessageType.UPLOAD_AVATAR_RESPONSE)
                        .error("Missing required fields")
                        .build();
            }

            // Chuyển đổi fileData
            byte[] fileData;
            if (fileDataObj instanceof byte[]) {
                fileData = (byte[]) fileDataObj;
            } else if (fileDataObj instanceof String) {
                // Nếu là Base64 string
                fileData = java.util.Base64.getDecoder().decode((String) fileDataObj);
            } else {
                return PacketBuilder.create(MessageType.UPLOAD_AVATAR_RESPONSE)
                        .error("Invalid file data format")
                        .build();
            }

            // Kiểm tra kích thước file
            if (fileData.length > MAX_AVATAR_SIZE) {
                return PacketBuilder.create(MessageType.UPLOAD_AVATAR_RESPONSE)
                        .error("Avatar size exceeds 5MB limit")
                        .build();
            }

            // Kiểm tra định dạng file
            String extension = getFileExtension(fileName);
            if (!isValidImageExtension(extension)) {
                return PacketBuilder.create(MessageType.UPLOAD_AVATAR_RESPONSE)
                        .error("Invalid file format. Only PNG, JPG, JPEG, GIF allowed")
                        .build();
            }

            // Tạo tên file unique
            String newFileName = userId + "_" + UUID.randomUUID() + "." + extension;
            String filePath = AVATAR_UPLOAD_DIR + newFileName;

            // Lưu file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(fileData);
            }

            // URL avatar (có thể là relative path hoặc full URL)
            String avatarUrl = "/avatars/" + newFileName;

            logger.info("Avatar uploaded successfully for user " + userId + ": " + avatarUrl);

            return PacketBuilder.create(MessageType.UPLOAD_AVATAR_RESPONSE)
                    .success(true)
                    .put("message", "Avatar uploaded successfully")
                    .put("avatarUrl", avatarUrl)
                    .build();

        } catch (IOException e) {
            logger.error("IO error while uploading avatar: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.UPLOAD_AVATAR_RESPONSE)
                    .error("Failed to save avatar: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error uploading avatar: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.UPLOAD_AVATAR_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xử lý lấy thông tin user
     */
    public Packet handleGetUserInfo(Packet request) {
        try {
            Long userId = request.getLong("userId");

            if (userId == null) {
                return PacketBuilder.create(MessageType.GET_USER_INFO_RESPONSE)
                        .error("User ID is required")
                        .build();
            }

            User user = userDAO.findById(userId);
            if (user == null) {
                return PacketBuilder.create(MessageType.GET_USER_INFO_RESPONSE)
                        .error("User not found")
                        .build();
            }

            // Không trả về password hash
            user.setPasswordHash(null);

            return PacketBuilder.create(MessageType.GET_USER_INFO_RESPONSE)
                    .success(true)
                    .put("user", user)
                    .build();

        } catch (SQLException e) {
            logger.error("Database error while getting user info: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.GET_USER_INFO_RESPONSE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error getting user info: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.GET_USER_INFO_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xử lý cập nhật status
     */
    public Packet handleStatusUpdate(Packet request) {
        try {
            Long userId = request.getLong("userId");
            String statusStr = request.getString("status");

            if (userId == null || statusStr == null) {
                return PacketBuilder.create(MessageType.STATUS_UPDATE)
                        .error("Missing required fields")
                        .build();
            }

            User.UserStatus status = User.UserStatus.valueOf(statusStr);
            userDAO.updateStatus(userId, status, null, null);

            logger.info("Status updated for user " + userId + ": " + status);

            return PacketBuilder.create(MessageType.STATUS_UPDATE)
                    .success(true)
                    .put("message", "Status updated successfully")
                    .build();

        } catch (IllegalArgumentException e) {
            return PacketBuilder.create(MessageType.STATUS_UPDATE)
                    .error("Invalid status value")
                    .build();
        } catch (SQLException e) {
            logger.error("Database error while updating status: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.STATUS_UPDATE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error updating status: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.STATUS_UPDATE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    // Helper methods
    private void createUploadDirectory() {
        try {
            Files.createDirectories(Paths.get(AVATAR_UPLOAD_DIR));
        } catch (IOException e) {
            logger.error("Failed to create upload directory: " + e.getMessage(), e);
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email != null && email.matches(emailRegex);
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private boolean isValidImageExtension(String extension) {
        return extension.equals("png") || extension.equals("jpg") ||
                extension.equals("jpeg") || extension.equals("gif");
    }
}