package com.chatapp.server.database.dao;

import com.chatapp.common.model.User;
import com.chatapp.server.database.DatabaseManager;
import com.chatapp.server.util.Logger;

import java.sql.*;
import java.time.LocalDateTime;

public class UserDAO {
    private final Logger logger = Logger.getInstance();
    private final DatabaseManager dbManager;

    public UserDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Insert new user
     */
    public void insert(User user) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, full_name, status_type, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getFullName());
            stmt.setString(5, User.UserStatus.OFFLINE.name());
            stmt.setBoolean(6, true);

            stmt.executeUpdate();

            // Get generated ID
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getLong(1));
            }
        }
    }

    /**
     * Find user by username
     */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND is_active = TRUE ";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        }
    }

    /**
     * Find user by username
     */
    public User findByEmail(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        }
    }


    /**
     * Find user by ID
     */
    public User findById(Long id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ? AND is_active = TRUE";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        }
    }

    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }

    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }

    /**
     * Update user status
     */
    public void updateStatus(Long userId, User.UserStatus status, String ipAddress, Integer port) throws SQLException {
        String sql = "UPDATE users SET status_type = ?, ip_address = ?, port = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setString(2, ipAddress);
            if (port != null) {
                stmt.setInt(3, port);
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setLong(4, userId);

            stmt.executeUpdate();
        }
    }
    /**
     * Update user profile information
     */
    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, email = ?, status_message = ?, " +
                "status_type = ?, avatar_url = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getStatusMessage());
            stmt.setString(4, user.getStatusType().name());
            stmt.setString(5, user.getAvatarUrl());
            stmt.setLong(6, user.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Update failed, user not found with id: " + user.getId());
            }
        }
    }

    /**
     * Update user password
     */
    public void updatePassword(Long userId, String newPasswordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newPasswordHash);
            stmt.setLong(2, userId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Password update failed, user not found with id: " + userId);
            }
        }
    }

    public boolean updatePasswordLogin(long userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Update password error: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Update avatar URL
     */
    public void updateAvatar(Long userId, String avatarUrl) throws SQLException {
        String sql = "UPDATE users SET avatar_url = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, avatarUrl);
            stmt.setLong(2, userId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Avatar update failed, user not found with id: " + userId);
            }
        }
    }

    /**
     * Update last seen timestamp
     */
    public void updateLastSeen(Long userId) throws SQLException {
        String sql = "UPDATE users SET last_seen = NOW() WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * Soft delete user (set is_active = false)
     */
    public void softDelete(Long userId) throws SQLException {
        String sql = "UPDATE users SET is_active = FALSE WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Delete failed, user not found with id: " + userId);
            }
        }
    }

    /**
     * Map ResultSet to User object
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setStatusMessage(rs.getString("status_message"));
        user.setStatusType(User.UserStatus.valueOf(rs.getString("status_type")));
        user.setIpAddress(rs.getString("ip_address"));

        Integer port = rs.getInt("port");
        if (!rs.wasNull()) {
            user.setPort(port);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp lastSeen = rs.getTimestamp("last_seen");
        if (lastSeen != null) {
            user.setLastSeen(lastSeen.toLocalDateTime());
        }

        user.setActive(rs.getBoolean("is_active"));
        user.setverified(rs.getBoolean("is_verified"));

        return user;
    }
    /**
     * Xác thực user (đặt is_verified = 1)
     */
    public boolean verifyUser(long userId) {
        String sql = "UPDATE users SET is_verified = 1 WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✓ Xác thực user thành công (ID: " + userId + ")");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Lỗi xác thực user: " + e.getMessage());
        }

        return false;
    }
    /**
     * Kiểm tra user đã được xác thực chưa
     */
    public boolean isUserVerified(long userId) {
        String sql = "SELECT is_verified FROM users WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("is_verified") == 1;
            }

        } catch (SQLException e) {
            System.err.println("✗ Lỗi kiểm tra xác thực: " + e.getMessage());
        }

        return false;
    }

    /**
     * Lấy email của user
     */
    public String getUserEmail(long userId) {
        String sql = "SELECT email FROM users WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("email");
            }

        } catch (SQLException e) {
            System.err.println("✗ Lỗi lấy email: " + e.getMessage());
        }

        return null;
    }

    public long createUserWithConn(Connection conn, String username, String email, String passwordHash, String fullName) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, full_name, is_verified, is_active, created_at) " +
                "VALUES (?, ?, ?, ?, 0, 1, CURRENT_TIMESTAMP)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, fullName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        }
    }

    public boolean updatePassword(long userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Update password error: " + e.getMessage(), e);
            return false;
        }
    }

}