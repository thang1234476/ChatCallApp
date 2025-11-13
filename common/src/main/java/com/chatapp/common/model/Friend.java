package com.chatapp.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Model đại diện cho mối quan hệ bạn bè
 */
public class Friend implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long friendId;
    private FriendStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Thông tin chi tiết của friend (từ bảng users)
    private String friendUsername;
    private String friendFullName;
    private String friendAvatarUrl;
    private String friendStatusMessage;
    private User.UserStatus friendStatusType;
    private LocalDateTime friendLastSeen;

    public enum FriendStatus {
        PENDING,    // Chờ chấp nhận
        ACCEPTED,   // Đã chấp nhận (là bạn bè)
        BLOCKED     // Đã chặn
    }

    public Friend() {
    }

    public Friend(Long userId, Long friendId, FriendStatus status) {
        this.userId = userId;
        this.friendId = friendId;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFriendId() {
        return friendId;
    }

    public void setFriendId(Long friendId) {
        this.friendId = friendId;
    }

    public FriendStatus getStatus() {
        return status;
    }

    public void setStatus(FriendStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFriendUsername() {
        return friendUsername;
    }

    public void setFriendUsername(String friendUsername) {
        this.friendUsername = friendUsername;
    }

    public String getFriendFullName() {
        return friendFullName;
    }

    public void setFriendFullName(String friendFullName) {
        this.friendFullName = friendFullName;
    }

    public String getFriendAvatarUrl() {
        return friendAvatarUrl;
    }

    public void setFriendAvatarUrl(String friendAvatarUrl) {
        this.friendAvatarUrl = friendAvatarUrl;
    }

    public String getFriendStatusMessage() {
        return friendStatusMessage;
    }

    public void setFriendStatusMessage(String friendStatusMessage) {
        this.friendStatusMessage = friendStatusMessage;
    }

    public User.UserStatus getFriendStatusType() {
        return friendStatusType;
    }

    public void setFriendStatusType(User.UserStatus friendStatusType) {
        this.friendStatusType = friendStatusType;
    }

    public LocalDateTime getFriendLastSeen() {
        return friendLastSeen;
    }

    public void setFriendLastSeen(LocalDateTime friendLastSeen) {
        this.friendLastSeen = friendLastSeen;
    }

    @Override
    public String toString() {
        return "Friend{" +
                "id=" + id +
                ", userId=" + userId +
                ", friendId=" + friendId +
                ", status=" + status +
                ", friendUsername='" + friendUsername + '\'' +
                ", friendFullName='" + friendFullName + '\'' +
                ", friendStatusType=" + friendStatusType +
                '}';
    }
}