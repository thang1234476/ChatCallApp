package com.chatapp.server.handler;

import com.chatapp.common.model.User;
import com.chatapp.common.protocol.MessageType;
import com.chatapp.common.protocol.Packet;
import com.chatapp.common.protocol.PacketBuilder;
import com.chatapp.common.util.PasswordUtil;
import com.chatapp.server.database.dao.OtpDAO;
import com.chatapp.server.database.dao.UserDAO;
import com.chatapp.server.service.AuthService;
import com.chatapp.server.service.EmailService;
import com.chatapp.server.util.Logger;

import java.net.Socket;

public class AuthHandler {
    private final AuthService authService;
    private final Logger logger = Logger.getInstance();
    private final Socket clientSocket;


    public AuthHandler() {
        this.clientSocket = new Socket();
        this.authService = new AuthService();
    }

    /**
     * Handle login request
     */
    public Packet handleLogin(Packet request) {
        try {
            String username = request.getString("username");
            String password = request.getString("password");

            // Validate input
            if (username == null || password == null) {
                return PacketBuilder.create(MessageType.LOGIN_RESPONSE)
                        .success(false)
                        .error("Vui lòng nhập đầy đủ thông tin")
                        .build();
            }

            // Login
            User user = authService.login(username, password);

            // Return success response
            return PacketBuilder.create(MessageType.LOGIN_RESPONSE)
                    .success(true)
                    .put("userId", user.getId())
                    .put("username", user.getUsername())
                    .put("email", user.getEmail())
                    .put("fullName", user.getFullName())
                    .put("avatarUrl", user.getAvatarUrl())
                    .put("statusMessage", user.getStatusMessage())
                    .build();

        } catch (Exception e) {
            logger.error("Login failed: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.LOGIN_RESPONSE)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }


    /**
     * Handle register request
     */
    public Packet handleRegister(Packet request) {
        try {
            String username = request.getString("username");
            String email = request.getString("email");
            String password = request.getString("password");
            String fullName = request.getString("fullName");

            // Validate input
            if (username == null || email == null || password == null) {
                return PacketBuilder.create(MessageType.REGISTER_RESPONSE)
                        .success(false)
                        .error("Vui lòng nhập đầy đủ thông tin")
                        .build();
            }

            // Register
            User user = authService.register(username, email, password, fullName);

            // Return success response
            return PacketBuilder.create(MessageType.REGISTER_RESPONSE)
                    .success(true)
                    .put("message", "Đăng ký thành công! Vui lòng đăng nhập.")
                    .build();

        } catch (Exception e) {
            logger.error("Registration failed: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.REGISTER_RESPONSE)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }
    /**
     * Handle verify OTP request - METHOD MỚI
     */
    public Packet handleVerifyOTP(Packet request) {
        try {
            String username = request.getString("username");
            String otpCode = request.getString("otpCode");

            if (username == null || otpCode == null || otpCode.trim().isEmpty()) {
                return PacketBuilder.create(MessageType.VERIFY_OTP_RESPONSE)
                        .success(false)
                        .error("Vui lòng nhập mã OTP")
                        .build();
            }

            // LẤY userId từ username
            User user = new UserDAO().findByUsername(username);
            if (user == null) {
                return PacketBuilder.create(MessageType.VERIFY_OTP_RESPONSE)
                        .success(false)
                        .error("Tài khoản không tồn tại")
                        .build();
            }

            boolean verified = authService.verifyOtp(user.getId(), otpCode);

            return verified
                    ? PacketBuilder.create(MessageType.VERIFY_OTP_RESPONSE)
                    .success(true)
                    .put("message", "Xác thực thành công!")
                    .build()
                    : PacketBuilder.create(MessageType.VERIFY_OTP_RESPONSE)
                    .success(false)
                    .error("Mã OTP sai hoặc đã hết hạn")
                    .build();

        } catch (Exception e) {
            logger.error("OTP verification failed: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.VERIFY_OTP_RESPONSE)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Handle resend OTP request - METHOD MỚI
     */
    public Packet handleResendOTP(Packet request) {
        try {
            String username = request.getString("username");

            if (username == null) {
                return PacketBuilder.create(MessageType.RESEND_OTP_RESPONSE)
                        .success(false)
                        .error("Thiếu thông tin")
                        .build();
            }

            User user = new UserDAO().findByUsername(username);
            if (user == null) {
                return PacketBuilder.create(MessageType.RESEND_OTP_RESPONSE)
                        .success(false)
                        .error("Tài khoản không tồn tại")
                        .build();
            }

            boolean sent = authService.resendOTP(user.getId());

            return sent
                    ? PacketBuilder.create(MessageType.RESEND_OTP_RESPONSE)
                    .success(true)
                    .put("message", "Mã OTP mới đã được gửi!")
                    .build()
                    : PacketBuilder.create(MessageType.RESEND_OTP_RESPONSE)
                    .success(false)
                    .error("Không thể gửi OTP")
                    .build();

        } catch (Exception e) {
            logger.error("Resend OTP failed: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.RESEND_OTP_RESPONSE)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Handle logout request
     */
    public Packet handleLogout(Packet request) {
        try {
            Long userId = request.getLong("userId");

            if (userId == null) {
                return PacketBuilder.create(MessageType.LOGOUT_RESPONSE)
                        .success(false)
                        .error("Invalid user ID")
                        .build();
            }

            // Logout
            authService.logout(userId);

            return PacketBuilder.create(MessageType.LOGOUT_RESPONSE)
                    .success(true)
                    .build();

        } catch (Exception e) {
            logger.error("Logout failed: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.LOGOUT_RESPONSE)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    // Fogot Password - METHOD MỚI
    public Packet handleForgotPassword(Packet request) {
        try {
            String email = request.getString("email");
            if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return PacketBuilder.create(MessageType.FORGOT_PASSWORD_RESPONSE)
                        .success(false)
                        .error("Email không hợp lệ")
                        .build();
            }

            User user = new UserDAO().findByEmail(email);
            if (user == null) {
                return PacketBuilder.create(MessageType.FORGOT_PASSWORD_RESPONSE)
                        .success(false)
                        .error("Email chưa được đăng ký")
                        .build();
            }

            // Tạo OTP mới
            String otp = new EmailService().generateOTP();
            long userId = user.getId();
            boolean saved = new OtpDAO().saveOTP( (int) userId, otp, 5);
            boolean sent = new EmailService().sendOTP(email, otp, user.getUsername());

            return (saved && sent)
                    ? PacketBuilder.create(MessageType.FORGOT_PASSWORD_RESPONSE)
                    .success(true)
                    .put("message", "Mã OTP đã được gửi đến email của bạn")
                    .build()
                    : PacketBuilder.create(MessageType.FORGOT_PASSWORD_RESPONSE)
                    .success(false)
                    .error("Không thể gửi OTP")
                    .build();

        } catch (Exception e) {
            return PacketBuilder.create(MessageType.FORGOT_PASSWORD_RESPONSE)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    public Packet handleResetPassword(Packet request) {
        try {
            String email = request.getString("email");
            String otpCode = request.getString("otpCode");
            String newPassword = request.getString("newPassword");

            if (email == null || otpCode == null || newPassword == null || newPassword.length() < 6) {
                return PacketBuilder.create(MessageType.RESET_PASSWORD_RESPONSE)
                        .success(false)
                        .error("Thông tin không hợp lệ")
                        .build();
            }

            User user = new UserDAO().findByEmail(email);
            if (user == null) {
                return PacketBuilder.create(MessageType.RESET_PASSWORD_RESPONSE)
                        .success(false)
                        .error("Email không tồn tại")
                        .build();
            }

            boolean otpValid = new OtpDAO().verifyOTP(user.getId(), otpCode);
            if (!otpValid) {
                return PacketBuilder.create(MessageType.RESET_PASSWORD_RESPONSE)
                        .success(false)
                        .error("Mã OTP không đúng hoặc đã hết hạn")
                        .build();
            }

            String hash = PasswordUtil.hashPassword(newPassword);
            boolean updated = new UserDAO().updatePasswordLogin(user.getId(), hash);

            return updated
                    ? PacketBuilder.create(MessageType.RESET_PASSWORD_RESPONSE)
                    .success(true)
                    .put("message", "Đặt lại mật khẩu thành công!")
                    .build()
                    : PacketBuilder.create(MessageType.RESET_PASSWORD_RESPONSE)
                    .success(false)
                    .error("Cập nhật thất bại")
                    .build();

        } catch (Exception e) {
            return PacketBuilder.create(MessageType.RESET_PASSWORD_RESPONSE)
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

}
