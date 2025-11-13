package com.chatapp.server.network;

import com.chatapp.common.protocol.*;
import com.chatapp.common.util.JsonUtil;
import com.chatapp.server.core.ClientRegistry;
import com.chatapp.server.handler.AuthHandler;
import com.chatapp.server.service.*;
import com.chatapp.server.util.Logger;

import java.io.*;
import java.net.Socket;

/**
 * ClientHandler — xử lý kết nối và yêu cầu từ từng client riêng biệt.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ClientRegistry clientRegistry;
    private final AuthHandler authHandler;
    private final UserService userService;
    private final AuthService authService;
    private final Logger logger = Logger.getInstance();

    private BufferedReader input;
    private PrintWriter output;
    private Long userId;

    public ClientHandler(Socket socket, ClientRegistry clientRegistry) {
        this.socket = socket;
        this.clientRegistry = clientRegistry;
        this.authHandler = new AuthHandler();
        this.userService = UserService.getInstance();
        this.authService = AuthService.getInstance();
    }

    @Override
    public void run() {
        try {
            initStreams();
            logger.info("Client connected: " + socket.getInetAddress());

            String line;
            while ((line = input.readLine()) != null) {
                logger.info("Received: " + line);
                processIncomingMessage(line);
            }

        } catch (IOException e) {
            logger.error("Connection error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    /**
     * Khởi tạo input/output stream.
     */
    private void initStreams() throws IOException {
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
    }

    /**
     * Xử lý từng gói tin client gửi đến.
     */
    private void processIncomingMessage(String line) {
        try {
            Packet request = JsonUtil.fromJson(line, Packet.class);
            Packet response = handleRequest(request);

            String responseJson = JsonUtil.toJson(response);
            output.println(responseJson);
            output.flush();

            logger.info("Sent: " + responseJson);

        } catch (Exception e) {
            logger.error("Error processing request: " + e.getMessage(), e);
            sendError("Server error: " + e.getMessage());
        }
    }

    /**
     * Gửi thông báo lỗi về client.
     */
    private void sendError(String message) {
        Packet errorResponse = PacketBuilder.create(MessageType.ERROR)
                .error(message)
                .build();
        output.println(JsonUtil.toJson(errorResponse));
    }

    /**
     * Xử lý logic theo từng loại MessageType.
     */
    private Packet handleRequest(Packet request) {
        MessageType type = request.getType();

        try {
            switch (type) {
                // Authentication
                case LOGIN_REQUEST:
                    Packet loginResponse = authHandler.handleLogin(request);
                    if (loginResponse.isSuccess()) {
                        userId = loginResponse.getLong("userId");
                        clientRegistry.addClient(userId, this);
                    }
                    return loginResponse;

                case REGISTER_REQUEST:
                    return authHandler.handleRegister(request);

                case LOGOUT_REQUEST:
                    if (userId != null) clientRegistry.removeClient(userId);
                    return authHandler.handleLogout(request);

                case VERIFY_OTP_REQUEST:
                    return authHandler.handleVerifyOTP(request);

                case RESEND_OTP_REQUEST:
                    return authHandler.handleResendOTP(request);

                case FORGOT_PASSWORD_REQUEST:
                    return authHandler.handleForgotPassword(request);

                case RESET_PASSWORD_REQUEST:
                    return authHandler.handleResetPassword(request);

                // Profile Management
                case UPDATE_PROFILE_REQUEST:
                    return userService.handleUpdateProfile(request);

                case CHANGE_PASSWORD_REQUEST:
                    return userService.handleChangePassword(request);

                case UPLOAD_AVATAR_REQUEST:
                    return userService.handleUploadAvatar(request);

                case GET_USER_INFO_REQUEST:
                    return userService.handleGetUserInfo(request);

                case STATUS_UPDATE:
                    return userService.handleStatusUpdate(request);

                // Friend Management
                case ADD_FRIEND_REQUEST:
                    return FriendService.getInstance().handleSendFriendRequest(request);

                case ACCEPT_FRIEND_REQUEST:
                    return FriendService.getInstance().handleAcceptFriendRequest(request);

                case REJECT_FRIEND_REQUEST:
                    return FriendService.getInstance().handleRejectFriendRequest(request);

                case UNFRIEND_REQUEST:
                    return FriendService.getInstance().handleUnfriend(request);

                case BLOCK_FRIEND_REQUEST:
                    return FriendService.getInstance().handleBlockUser(request);

                case GET_FRIENDS_REQUEST:
                    return FriendService.getInstance().handleGetFriends(request);

                case GET_PENDING_REQUESTS_REQUEST:
                    return FriendService.getInstance().handleGetPendingRequests(request);

                case SEARCH_USERS_REQUEST:
                    return FriendService.getInstance().handleSearchUsers(request);

                // TODO: Thêm handler cho Chat, File, Call...
                default:
                    return PacketBuilder.create(MessageType.ERROR)
                            .error("Unsupported message type: " + type)
                            .build();
            }

        } catch (Exception e) {
            logger.error("Error handling request: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.ERROR)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Dọn dẹp tài nguyên khi client ngắt kết nối.
     */
    private void cleanup() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();

            logger.info("Client disconnected: " + socket.getInetAddress());
        } catch (IOException e) {
            logger.error("Error during cleanup: " + e.getMessage());
        }
    }

    // Getter & Setter
    public Long getUserId() {
        return userId;
    }
}
