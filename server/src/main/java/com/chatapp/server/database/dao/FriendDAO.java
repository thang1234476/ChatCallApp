package com.chatapp.server.database.dao;

import com.chatapp.common.model.Friend;
import com.chatapp.common.model.User;
import com.chatapp.server.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO để thao tác với bảng friendships
 */
public class FriendDAO {

    /**
     * Gửi lời mời kết bạn
     */
    public void sendFriendRequest(Long userId, Long friendId) throws SQLException {
        String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'PENDING')";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, friendId);
            stmt.executeUpdate();
        }
    }

    /**
     * Chấp nhận lời mời kết bạn
     */
    public void acceptFriendRequest(Long userId, Long friendId) throws SQLException {
        String sql = "UPDATE friendships SET status = 'ACCEPTED', updated_at = NOW() " +
                "WHERE user_id = ? AND friend_id = ? AND status = 'PENDING'";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, friendId); // Người gửi lời mời
            stmt.setLong(2, userId);   // Người nhận lời mời
            int updated = stmt.executeUpdate();

            if (updated > 0) {
                // Tạo mối quan hệ ngược lại để cả 2 người đều là bạn bè
                createReverseFriendship(conn, userId, friendId);
            }
        }
    }

    /**
     * Tạo mối quan hệ bạn bè ngược lại (2 chiều)
     */
    private void createReverseFriendship(Connection conn, Long userId, Long friendId) throws SQLException {
        String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'ACCEPTED') " +
                "ON DUPLICATE KEY UPDATE status = 'ACCEPTED', updated_at = NOW()";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, friendId);
            stmt.executeUpdate();
        }
    }

    /**
     * Từ chối lời mời kết bạn
     */
    public void rejectFriendRequest(Long userId, Long friendId) throws SQLException {
        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ? AND status = 'PENDING'";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, friendId); // Người gửi lời mời
            stmt.setLong(2, userId);   // Người nhận lời mời
            stmt.executeUpdate();
        }
    }

    /**
     * Hủy kết bạn / Xóa bạn
     */
    public void unfriend(Long userId, Long friendId) throws SQLException {
        String sql = "DELETE FROM friendships WHERE " +
                "(user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, friendId);
            stmt.setLong(3, friendId);
            stmt.setLong(4, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * Chặn người dùng
     */
    public void blockUser(Long userId, Long blockedUserId) throws SQLException {
        String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'BLOCKED') " +
                "ON DUPLICATE KEY UPDATE status = 'BLOCKED', updated_at = NOW()";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, blockedUserId);
            stmt.executeUpdate();
        }
    }

    /**
     * Bỏ chặn người dùng
     */
    public void unblockUser(Long userId, Long blockedUserId) throws SQLException {
        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ? AND status = 'BLOCKED'";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, blockedUserId);
            stmt.executeUpdate();
        }
    }

    /**
     * Lấy danh sách bạn bè (status = ACCEPTED)
     */
    public List<Friend> getFriendsList(Long userId) throws SQLException {
        String sql = "SELECT f.*, u.username, u.full_name, u.avatar_url, u.status_message, " +
                "u.status_type, u.last_seen " +
                "FROM friendships f " +
                "INNER JOIN users u ON f.friend_id = u.id " +
                "WHERE f.user_id = ? AND f.status = 'ACCEPTED' AND u.is_active = TRUE " +
                "ORDER BY u.status_type DESC, u.full_name ASC";

        List<Friend> friends = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                friends.add(mapResultSetToFriend(rs));
            }
        }

        return friends;
    }

    /**
     * Lấy danh sách lời mời kết bạn đang chờ (PENDING) mà user nhận được
     */
    public List<Friend> getPendingRequests(Long userId) throws SQLException {
        String sql = "SELECT f.*, u.username, u.full_name, u.avatar_url, u.status_message, " +
                "u.status_type, u.last_seen " +
                "FROM friendships f " +
                "INNER JOIN users u ON f.user_id = u.id " +
                "WHERE f.friend_id = ? AND f.status = 'PENDING' AND u.is_active = TRUE " +
                "ORDER BY f.created_at DESC";

        List<Friend> requests = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Friend friend = mapResultSetToFriend(rs);
                // Đổi userId và friendId vì đây là request nhận được
                Long senderId = rs.getLong("user_id");
                friend.setFriendId(senderId);
                requests.add(friend); // ✅ FIX: Đổi từ friends.add() thành requests.add()
            }
        }

        return requests;
    }

    /**
     * Lấy danh sách người dùng bị chặn
     */
    public List<Friend> getBlockedUsers(Long userId) throws SQLException {
        String sql = "SELECT f.*, u.username, u.full_name, u.avatar_url " +
                "FROM friendships f " +
                "INNER JOIN users u ON f.friend_id = u.id " +
                "WHERE f.user_id = ? AND f.status = 'BLOCKED' " +
                "ORDER BY f.updated_at DESC";

        List<Friend> blocked = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                blocked.add(mapResultSetToFriend(rs));
            }
        }

        return blocked;
    }

    /**
     * Kiểm tra trạng thái bạn bè giữa 2 user
     */
    public Friend.FriendStatus getFriendshipStatus(Long userId, Long otherUserId) throws SQLException {
        String sql = "SELECT status FROM friendships WHERE user_id = ? AND friend_id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, otherUserId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Friend.FriendStatus.valueOf(rs.getString("status"));
            }
        }

        return null; // Không có quan hệ
    }

    /**
     * Kiểm tra xem có phải bạn bè không
     */
    public boolean areFriends(Long userId, Long otherUserId) throws SQLException {
        Friend.FriendStatus status = getFriendshipStatus(userId, otherUserId);
        return status == Friend.FriendStatus.ACCEPTED;
    }

    /**
     * Tìm kiếm người dùng theo username hoặc tên (để thêm bạn)
     */
    public List<User> searchUsers(Long currentUserId, String keyword) throws SQLException {
        String sql = "SELECT u.* FROM users u " +
                "WHERE u.id != ? AND u.is_active = TRUE " +
                "AND (u.username LIKE ? OR u.full_name LIKE ?) " +
                "AND u.id NOT IN (" +
                "  SELECT friend_id FROM friendships WHERE user_id = ? AND status IN ('ACCEPTED', 'PENDING')" +
                ") " +
                "LIMIT 50";

        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + keyword + "%";
            stmt.setLong(1, currentUserId);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setLong(4, currentUserId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }

        return users;
    }

    /**
     * Map ResultSet to Friend object
     */
    private Friend mapResultSetToFriend(ResultSet rs) throws SQLException {
        Friend friend = new Friend();
        friend.setId(rs.getLong("id"));
        friend.setUserId(rs.getLong("user_id"));
        friend.setFriendId(rs.getLong("friend_id"));
        friend.setStatus(Friend.FriendStatus.valueOf(rs.getString("status")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            friend.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            friend.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        // Friend info
        friend.setFriendUsername(rs.getString("username"));
        friend.setFriendFullName(rs.getString("full_name"));
        friend.setFriendAvatarUrl(rs.getString("avatar_url"));
        friend.setFriendStatusMessage(rs.getString("status_message"));
        friend.setFriendStatusType(User.UserStatus.valueOf(rs.getString("status_type")));

        Timestamp lastSeen = rs.getTimestamp("last_seen");
        if (lastSeen != null) {
            friend.setFriendLastSeen(lastSeen.toLocalDateTime());
        }

        return friend;
    }

    /**
     * Map ResultSet to User object
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setStatusMessage(rs.getString("status_message"));
        user.setStatusType(User.UserStatus.valueOf(rs.getString("status_type")));

        Timestamp lastSeen = rs.getTimestamp("last_seen");
        if (lastSeen != null) {
            user.setLastSeen(lastSeen.toLocalDateTime());
        }

        return user;
    }
}