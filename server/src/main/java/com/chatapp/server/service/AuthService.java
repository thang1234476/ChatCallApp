package com.chatapp.server.service;

import com.chatapp.common.model.User;
import com.chatapp.common.protocol.Packet;
import com.chatapp.common.protocol.PacketBuilder;
import com.chatapp.common.protocol.MessageType;
import com.chatapp.common.util.PasswordUtil;
import com.chatapp.server.database.DatabaseManager;
import com.chatapp.server.database.dao.OtpDAO;
import com.chatapp.server.database.dao.UserDAO;
import com.chatapp.server.util.Logger;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Service xử lý authentication (login, register, logout)
 */
public class AuthService {
    private final UserDAO userDAO;
    private final Logger logger = Logger.getInstance();
    private final OtpDAO otpDAO;
    private final EmailService emailService;
    private static AuthService instance;

    private static final int OTP_EXPIRY_MINUTES = 5;



    public AuthService() {
        this.userDAO = new UserDAO();
        this.otpDAO = new OtpDAO();
        this.emailService = new EmailService();
    }

    public static AuthService getInstance() {
        if (instance == null) {
            synchronized (AuthService.class) {
                if (instance == null) {
                    instance = new AuthService();
                }
            }
        }
        return instance;
    }

    public Packet handleLogin(Packet request) {
        try {
            Map<String, Object> data = request.getData();
            String username = (String) data.get("username");
            String password = (String) data.get("password");

            // Validate input
            if (username == null || username.trim().isEmpty() ||
                    password == null || password.trim().isEmpty()) {
                return PacketBuilder.create(MessageType.LOGIN_RESPONSE)
                        .error("Username and password are required")
                        .build();
            }

            // Tìm user
            User user = userDAO.findByUsername(username.trim());
            if (user == null) {
                logger.warn("Login failed: User not found - " + username);
                return PacketBuilder.create(MessageType.LOGIN_RESPONSE)
                        .error("Invalid username or password")
                        .build();
            }

            // Kiểm tra password
            if (!BCrypt.checkpw(password, user.getPasswordHash())) {
                logger.warn("Login failed: Wrong password - " + username);
                return PacketBuilder.create(MessageType.LOGIN_RESPONSE)
                        .error("Invalid username or password")
                        .build();
            }

            // Cập nhật status thành ONLINE
            userDAO.updateStatus(user.getId(), User.UserStatus.ONLINE, null, null);
            user.setStatusType(User.UserStatus.ONLINE);

            // Không trả về password hash
            user.setPasswordHash(null);

            logger.info("User logged in successfully: " + username);

            return PacketBuilder.create(MessageType.LOGIN_RESPONSE)
                    .success(true)
                    .put("message", "Login successful")
                    .put("userId", user.getId())
                    .put("user", user)
                    .build();

        } catch (SQLException e) {
            logger.error("Database error during login: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.LOGIN_RESPONSE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error during login: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.LOGIN_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    public Packet handleRegister(Packet request) {
        try {
            Map<String, Object> data = request.getData();
            String username = (String) data.get("username");
            String password = (String) data.get("password");
            String email = (String) data.get("email");
            String fullName = (String) data.get("fullName");

            // Validate input
            if (username == null || username.trim().isEmpty()) {
                return PacketBuilder.create(MessageType.REGISTER_RESPONSE)
                        .error("Username is required")
                        .build();
            }

            if (password == null || password.length() < 6) {
                return PacketBuilder.create(MessageType.REGISTER_RESPONSE)
                        .error("Password must be at least 6 characters")
                        .build();
            }

            if (email == null || !isValidEmail(email)) {
                return PacketBuilder.create(MessageType.REGISTER_RESPONSE)
                        .error("Invalid email format")
                        .build();
            }

            // Kiểm tra username đã tồn tại
            if (userDAO.existsByUsername(username.trim())) {
                return PacketBuilder.create(MessageType.REGISTER_RESPONSE)
                        .error("Username already exists")
                        .build();
            }

            // Kiểm tra email đã tồn tại
            if (userDAO.existsByEmail(email.trim())) {
                return PacketBuilder.create(MessageType.REGISTER_RESPONSE)
                        .error("Email already registered")
                        .build();
            }

            // Tạo user mới
            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setEmail(email.trim());
            newUser.setFullName(fullName != null ? fullName.trim() : username.trim());
            newUser.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
            newUser.setStatusType(User.UserStatus.OFFLINE);
            newUser.setActive(true);

            // Lưu vào database
            userDAO.insert(newUser);
            newUser.setPasswordHash(null);

            logger.info("User registered successfully: " + username);

            return PacketBuilder.create(MessageType.REGISTER_RESPONSE)
                    .success(true)
                    .put("message", "Registration successful")
                    .put("user", newUser)
                    .build();

        } catch (SQLException e) {
            logger.error("Database error during registration: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.REGISTER_RESPONSE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error during registration: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.REGISTER_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    public Packet handleLogout(Packet request) {
        try {
            Map<String, Object> data = request.getData();
            Long userId = data.get("userId") != null ? ((Number) data.get("userId")).longValue() : null;

            if (userId == null) {
                return PacketBuilder.create(MessageType.LOGOUT_RESPONSE)
                        .error("User ID is required")
                        .build();
            }

            userDAO.updateStatus(userId, User.UserStatus.OFFLINE, null, null);

            logger.info("User logged out: " + userId);

            return PacketBuilder.create(MessageType.LOGOUT_RESPONSE)
                    .success(true)
                    .put("message", "Logout successful")
                    .build();

        } catch (SQLException e) {
            logger.error("Database error during logout: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.LOGOUT_RESPONSE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error during logout: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.LOGOUT_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }
    /**
     * Register new user
     */
    public User register(String username, String email, String password, String fullName) throws Exception {
        System.out.println("\n========== BẮT ĐẦU ĐĂNG KÝ ==========");
        System.out.println("Username: " + username);
        System.out.println("Email: " + email);

        // Validate input
        if (username == null || username.trim().isEmpty()) {
            throw new Exception("Tên đăng nhập không được để trống");
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new Exception("Email không hợp lệ");
        }
        if (password == null || password.length() < 6) {
            throw new Exception("Mật khẩu phải có ít nhất 6 ký tự");
        }

        // Check if username exists
//        if (userDAO.existsByUsername(username)) {
//            throw new Exception("Tên đăng nhập đã tồn tại");
//        }

        // Check if email exists
        if (userDAO.existsByEmail(email)) {
            throw new Exception("Email đã được sử dụng");
        }

        String passwordHash = PasswordUtil.hashPassword(password);
        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false);

            // 1. Tạo user
            long userId = userDAO.createUserWithConn(conn, username, email, passwordHash, fullName != null ? fullName : username);
            if (userId == -1) throw new Exception("Không thể tạo tài khoản");

            // 2. Tạo OTP
            String otpCode = emailService.generateOTP();
            if (!otpDAO.saveOTPWithConn(conn, (int) userId, otpCode, OTP_EXPIRY_MINUTES))
                throw new Exception("Lỗi lưu OTP");

            // 3. Gửi email
            if (!emailService.sendOTP(email, otpCode, username))
                throw new Exception("Gửi email thất bại");

            conn.commit();

            User user = new User();
            user.setId(userId);
            user.setUsername(username);
            user.setEmail(email);
            user.setFullName(fullName);
            user.setverified(false);
            logger.info("New user registered: " + username);
            return user;

        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { logger.error("Rollback failed", ex); }
            throw e;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* ignore */ }
        }
    }

    /**
     * Login user
     */
    public User login(String email, String password) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            throw new Exception("Email không được để trống");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new Exception("Mật khẩu không được để trống");
        }

        // Get user from database
        User user = userDAO.findByEmail(email);

        if (user == null) {
            throw new Exception("Email hoặc mật khẩu không đúng");
        }

        // Check password
        if (!PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            throw new Exception("Email hoặc mật khẩu không đúng");
        }

        // Check if account is active
        if (!user.isActive()) {
            throw new Exception("Tài khoản đã bị khóa");
        }
        // Check if account is OTP
        if (!user.isverified()) {
            throw new Exception("Vui lòng xác thực tài khoản qua OTP trước khi đăng nhập." + user.isverified());
        }


        logger.info("User logged in: " + user.getUsername());
        return user;
    }

    /**
     * Logout user
     */
    public void logout(Long userId) throws SQLException {
        userDAO.updateStatus(userId, User.UserStatus.OFFLINE, null, null);
        logger.info("User logged out: " + userId);
    }


    /**
     * Update user online status
     */
    public void updateOnlineStatus(Long userId, String ipAddress, Integer port) throws SQLException {
        userDAO.updateStatus(userId, User.UserStatus.ONLINE, ipAddress, port);
    }

    /**
     * Xác thực OTP
     */
    public boolean verifyOtp(long userId, String otpCode) {
        System.out.println("\n========== XÁC THỰC OTP ==========");
        System.out.println("User ID: " + userId);
        System.out.println("OTP: " + otpCode);

        // 1. Kiểm tra user đã được xác thực chưa
        if (userDAO.isUserVerified(userId)) {
            System.out.println("✓ User đã được xác thực trước đó");
            return true;
        }

        // 2. Xác thực OTP
        boolean isOtpValid = otpDAO.verifyOTP(userId, otpCode);

        if (!isOtpValid) {
            System.err.println("✗ OTP không hợp lệ hoặc đã hết hạn!");
            System.out.println("====================================\n");
            return false;
        }

        // 3. Cập nhật is_verified = 1
        boolean verified = userDAO.verifyUser(userId);

        if (verified) {
            System.out.println("✓ Xác thực OTP thành công!");
            System.out.println("✓ Tài khoản đã được kích hoạt");
        } else {
            System.err.println("✗ Không thể cập nhật trạng thái xác thực!");
        }

        System.out.println("====================================\n");
        return verified;
    }

    /**
     * Gửi lại OTP
     */
    public boolean resendOTP(long userId) {
        System.out.println("\n========== GỬI LẠI OTP ==========");

        // 1. Lấy email của user
        String email = userDAO.getUserEmail(userId);
        if (email == null) {
            System.err.println("✗ Không tìm thấy user!");
            return false;
        }

        // 2. Kiểm tra user đã được xác thực chưa
        if (userDAO.isUserVerified(userId)) {
            System.out.println("✓ User đã được xác thực, không cần gửi OTP");
            return true;
        }

        // 3. Tạo OTP mới
        String otpCode = emailService.generateOTP();
        boolean otpSaved = otpDAO.saveOTP((int) userId, otpCode, OTP_EXPIRY_MINUTES);

        if (!otpSaved) {
            System.err.println("✗ Không thể lưu OTP!");
            return false;
        }

        // 4. Gửi email
        boolean emailSent = emailService.sendOTP(email, otpCode, "User");

        if (emailSent) {
            System.out.println("✓ OTP mới đã được gửi đến: " + email);
        } else {
            System.err.println("✗ Không thể gửi email!");
        }

        System.out.println("==================================\n");
        return emailSent;
    }

    /**
     * Kiểm tra user đã xác thực chưa
     */
    public boolean isUserVerified(long userId) {
        return userDAO.isUserVerified(userId);
    }

    /**
     * Dọn dẹp OTP hết hạn
     */
    public void cleanupExpiredOTP() {
        otpDAO.cleanupExpiredOTP();
    }


    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email != null && email.matches(emailRegex);
    }
}