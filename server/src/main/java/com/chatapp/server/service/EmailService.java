package com.chatapp.server.service;

import com.chatapp.server.config.ConfigLoader;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;

public class EmailService {
    private final String smtpHost;
    private final String smtpPort;
    private final String emailFrom;
    private final String emailPassword;

    public EmailService() {
        ConfigLoader config = ConfigLoader.getInstance();
        this.smtpHost = config.getProperty("email.smtp.host", "smtp.gmail.com");
        this.smtpPort = config.getProperty("email.smtp.port", "587");
        this.emailFrom = config.getProperty("email.from");
        this.emailPassword = config.getProperty("email.password");
    }

    /**
     * Tạo mã OTP 6 chữ số ngẫu nhiên
     */
    public String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Gửi mã OTP qua email
     * @param toEmail Email người nhận
     * @param otp Mã OTP cần gửi
     * @param username Tên người dùng
     * @return true nếu gửi thành công
     */
    public boolean sendOTP(String toEmail, String otp, String username) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.ssl.trust", smtpHost);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailFrom, emailPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Mã xác thực OTP - Đăng ký tài khoản Chat App");

            String emailContent = buildEmailTemplate(otp, username);
            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✓ Email OTP đã được gửi đến: " + toEmail);
            // TEST MODE: IN RA CONSOLE, KHÔNG GỬI EMAIL
            System.out.println("\n" + "=".repeat(60));
            System.out.println("OTP TEST MODE (không gửi email thật)");
            System.out.println("To: " + toEmail);
            System.out.println("OTP: " + otp);
            System.out.println("Username: " + username);
            System.out.println("=".repeat(60) + "\n");

            return true;

        } catch (MessagingException e) {
            System.err.println("✗ Lỗi gửi email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Tạo template HTML cho email OTP
     */
    private String buildEmailTemplate(String otp, String username) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Xác thực OTP</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f7fa;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f7fa; padding: 40px 0;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); overflow: hidden;">
                                <!-- Header -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px 30px; text-align: center;">
                                        <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 600;">
                                            🔐 Xác Thực Tài Khoản
                                        </h1>
                                    </td>
                                </tr>
                                
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px 30px;">
                                        <p style="color: #333333; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">
                                            Xin chào <strong>%s</strong>,
                                        </p>
                                        
                                        <p style="color: #555555; font-size: 15px; line-height: 1.6; margin: 0 0 30px 0;">
                                            Cảm ơn bạn đã đăng ký tài khoản tại <strong>Chat App</strong>. 
                                            Để hoàn tất quá trình đăng ký, vui lòng sử dụng mã OTP bên dưới:
                                        </p>
                                        
                                        <!-- OTP Box -->
                                        <table width="100%%" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td align="center" style="padding: 20px 0;">
                                                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); 
                                                                padding: 25px 50px; 
                                                                border-radius: 10px; 
                                                                display: inline-block;
                                                                box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);">
                                                        <span style="color: #ffffff; 
                                                                     font-size: 36px; 
                                                                     font-weight: bold; 
                                                                     letter-spacing: 8px;
                                                                     font-family: 'Courier New', monospace;">
                                                            %s
                                                        </span>
                                                    </div>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <!-- Info Box -->
                                        <div style="background-color: #fff3cd; 
                                                    border-left: 4px solid #ffc107; 
                                                    padding: 15px 20px; 
                                                    border-radius: 5px; 
                                                    margin-top: 30px;">
                                            <p style="color: #856404; font-size: 14px; margin: 0; line-height: 1.6;">
                                                <strong>⚠️ Lưu ý:</strong><br>
                                                • Mã OTP có hiệu lực trong <strong>5 phút</strong><br>
                                                • Không chia sẻ mã này với bất kỳ ai<br>
                                                • Nếu không yêu cầu, vui lòng bỏ qua email này
                                            </p>
                                        </div>
                                        
                                        <p style="color: #666666; font-size: 14px; margin: 30px 0 0 0; line-height: 1.6;">
                                            Nếu bạn gặp vấn đề, vui lòng liên hệ với chúng tôi.
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f8f9fa; padding: 25px 30px; text-align: center; border-top: 1px solid #e9ecef;">
                                        <p style="color: #6c757d; font-size: 13px; margin: 0 0 10px 0;">
                                            © 2025 Chat App. All rights reserved.
                                        </p>
                                        <p style="color: #adb5bd; font-size: 12px; margin: 0;">
                                            Email tự động, vui lòng không trả lời.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(username, otp);
    }
}