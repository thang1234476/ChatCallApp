package com.chatapp.server.database.dao;

import com.chatapp.server.database.DatabaseManager;
import com.chatapp.server.util.Logger;

import java.sql.*;
import java.time.LocalDateTime;

public class OtpDAO {
    private final Logger logger = Logger.getInstance();
    private final DatabaseManager dbManager;

    public OtpDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    // Lưu hoặc cập nhật OTP cho user
    public boolean saveOTP(int userId, String otpCode, int expiryMinutes) {
        // Xóa các OTP cũ chưa sử dụng của user này
        deleteUnusedOTP(userId);

        String sql = "INSERT INTO otp_verification (user_id, otp_code, expiration_time) VALUES (?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusMinutes(expiryMinutes));

            pstmt.setInt(1, userId);
            pstmt.setString(2, otpCode);
            pstmt.setTimestamp(3, expiresAt);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✓ Lưu OTP thành công cho user ID: " + userId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("✗ Lỗi lưu OTP: " + e.getMessage());
        }

        return false;
    }

    /**
     * Xác thực OTP
     * @param userId ID của user
     * @param otpCode Mã OTP nhập vào
     * @return true nếu OTP hợp lệ
     */
    public boolean verifyOTP(long userId, String otpCode) {
        String sql = """
            SELECT id, expiration_time, is_used 
            FROM otp_verification 
            WHERE user_id = ? AND otp_code = ?
            ORDER BY id DESC
            LIMIT 1
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setString(2, otpCode);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                long otpId = rs.getLong("id");
                Timestamp expirationTime = rs.getTimestamp("expiration_time");
                int isUsed = rs.getInt("is_used");

                // Kiểm tra đã sử dụng chưa
                if (isUsed == 1) {
                    System.out.println("✗ OTP đã được sử dụng");
                    return false;
                }

                // Kiểm tra hết hạn chưa
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime expiry = expirationTime.toLocalDateTime();

                if (now.isAfter(expiry)) {
                    System.out.println("✗ OTP đã hết hạn (Hết hạn lúc: " + expiry + ", Hiện tại: " + now + ")");
                    return false;
                }

                // Đánh dấu OTP đã sử dụng
                markOTPAsUsed(otpId);

                System.out.println("✓ Xác thực OTP thành công cho user ID: " + userId);
                return true;
            } else {
                System.out.println("✗ OTP không hợp lệ hoặc không tồn tại");
            }

        } catch (SQLException e) {
            System.err.println("✗ Lỗi xác thực OTP: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Đánh dấu OTP đã sử dụng
     */
    private void markOTPAsUsed(long otpId) {
        String sql = "UPDATE otp_verification SET is_used = 1 WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, otpId);
            pstmt.executeUpdate();
            System.out.println("✓ Đánh dấu OTP đã sử dụng (ID: " + otpId + ")");

        } catch (SQLException e) {
            System.err.println("✗ Lỗi đánh dấu OTP: " + e.getMessage());
        }
    }

    /**
     * Xóa các OTP chưa sử dụng của user (để gửi OTP mới)
     */
    private void deleteUnusedOTP(int userId) {
        String sql = "DELETE FROM otp_verification WHERE user_id = ? AND is_used = 0";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            int deletedRows = pstmt.executeUpdate();

            if (deletedRows > 0) {
                System.out.println("✓ Đã xóa " + deletedRows + " OTP cũ của user ID: " + userId);
            }

        } catch (SQLException e) {
            System.err.println("✗ Lỗi xóa OTP cũ: " + e.getMessage());
        }
    }

    /**
     * Xóa các OTP đã hết hạn (dọn dẹp database)
     */
    public void cleanupExpiredOTP() {
        String sql = "DELETE FROM otp_verification WHERE expiration_time < NOW()";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            int deletedRows = stmt.executeUpdate(sql);

            if (deletedRows > 0) {
                System.out.println("✓ Dọn dẹp " + deletedRows + " OTP hết hạn");
            }

        } catch (SQLException e) {
            System.err.println("✗ Lỗi dọn dẹp OTP: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra user có OTP chưa sử dụng và còn hiệu lực không
     */
    public boolean hasUnusedOTP(long userId) {
        String sql = """
            SELECT COUNT(*) 
            FROM otp_verification 
            WHERE user_id = ? AND is_used = 0 AND expiration_time > NOW()
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("✗ Lỗi kiểm tra OTP: " + e.getMessage());
        }

        return false;
    }

    /**
     * Lấy thời gian hết hạn của OTP gần nhất
     */
    public LocalDateTime getOTPExpirationTime(long userId) {
        String sql = """
            SELECT expiration_time 
            FROM otp_verification 
            WHERE user_id = ? AND is_used = 0
            ORDER BY id DESC
            LIMIT 1
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("expiration_time");
                return timestamp.toLocalDateTime();
            }

        } catch (SQLException e) {
            System.err.println("✗ Lỗi lấy thời gian hết hạn OTP: " + e.getMessage());
        }

        return null;
    }

    public boolean saveOTPWithConn(Connection conn, int userId, String otpCode, int expiryMinutes) throws SQLException {
        deleteUnusedOTPWithConn(conn, userId);
        String sql = "INSERT INTO otp_verification (user_id, otp_code, expiration_time) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, otpCode);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().plusMinutes(expiryMinutes)));
            return ps.executeUpdate() > 0;
        }
    }

    public void deleteUnusedOTPWithConn(Connection conn, int userId) throws SQLException {
        String sql = "DELETE FROM otp_verification WHERE user_id = ? AND is_used = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}