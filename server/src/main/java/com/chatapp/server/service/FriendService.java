package com.chatapp.server.service;

import com.chatapp.common.model.Friend;
import com.chatapp.common.model.User;
import com.chatapp.common.protocol.Packet;
import com.chatapp.common.protocol.PacketBuilder;
import com.chatapp.common.protocol.MessageType;
import com.chatapp.server.database.dao.FriendDAO;
import com.chatapp.server.database.dao.UserDAO;
import com.chatapp.server.util.Logger;

import java.sql.SQLException;
import java.util.List;

/**
 * Service xử lý các thao tác liên quan đến Friend
 */
public class FriendService {
    private static FriendService instance;
    private final FriendDAO friendDAO;
    private final UserDAO userDAO;
    private final Logger logger = Logger.getInstance();

    private FriendService() {
        this.friendDAO = new FriendDAO();
        this.userDAO = new UserDAO();
    }

    public static FriendService getInstance() {
        if (instance == null) {
            synchronized (FriendService.class) {
                if (instance == null) {
                    instance = new FriendService();
                }
            }
        }
        return instance;
    }

    /**
     * Xử lý gửi lời mời kết bạn
     */
    public Packet handleSendFriendRequest(Packet request) {
        try {
            Long userId = request.getLong("userId");
            Long friendId = request.getLong("friendId");

            if (userId == null || friendId == null) {
                return PacketBuilder.create(MessageType.ADD_FRIEND_RESPONSE)
                        .error("Missing required fields")
                        .build();
            }

            if (userId.equals(friendId)) {
                return PacketBuilder.create(MessageType.ADD_FRIEND_RESPONSE)
                        .error("Cannot send friend request to yourself")
                        .build();
            }

            // Kiểm tra user tồn tại
            User friend = userDAO.findById(friendId);
            if (friend == null) {
                return PacketBuilder.create(MessageType.ADD_FRIEND_RESPONSE)
                        .error("User not found")
                        .build();
            }

            // Kiểm tra trạng thái hiện tại
            Friend.FriendStatus status = friendDAO.getFriendshipStatus(userId, friendId);
            if (status != null) {
                if (status == Friend.FriendStatus.ACCEPTED) {
                    return PacketBuilder.create(MessageType.ADD_FRIEND_RESPONSE)
                            .error("Already friends")
                            .build();
                } else if (status == Friend.FriendStatus.PENDING) {
                    return PacketBuilder.create(MessageType.ADD_FRIEND_RESPONSE)
                            .error("Friend request already sent")
                            .build();
                } else if (status == Friend.FriendStatus.BLOCKED) {
                    return PacketBuilder.create(MessageType.ADD_FRIEND_RESPONSE)
                            .error("Cannot send friend request")
                            .build();
                }
            }

            // Gửi lời mời
            friendDAO.sendFriendRequest(userId, friendId);

            logger.info("Friend request sent from " + userId + " to " + friendId);

            return PacketBuilder.create(MessageType.ADD_FRIEND_RESPONSE)
                    .success(true)
                    .put("message", "Friend request sent successfully")
                    .build();

        } catch (SQLException e) {
            logger.error("Database error while sending friend request: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.ADD_FRIEND_RESPONSE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error sending friend request: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.ADD_FRIEND_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xử lý chấp nhận lời mời kết bạn
     */
    public Packet handleAcceptFriendRequest(Packet request) {
        try {
            Long userId = request.getLong("userId");
            Long friendId = request.getLong("friendId");

            if (userId == null || friendId == null) {
                return PacketBuilder.create(MessageType.ACCEPT_FRIEND_RESPONSE)
                        .error("Missing required fields")
                        .build();
            }

            // Chấp nhận lời mời
            friendDAO.acceptFriendRequest(userId, friendId);

            logger.info("Friend request accepted: " + userId + " accepted " + friendId);

            return PacketBuilder.create(MessageType.ACCEPT_FRIEND_RESPONSE)
                    .success(true)
                    .put("message", "Friend request accepted")
                    .build();

        } catch (SQLException e) {
            logger.error("Database error while accepting friend request: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.ACCEPT_FRIEND_RESPONSE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error accepting friend request: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.ACCEPT_FRIEND_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xử lý từ chối lời mời kết bạn
     */
    public Packet handleRejectFriendRequest(Packet request) {
        try {
            Long userId = request.getLong("userId");
            Long friendId = request.getLong("friendId");

            if (userId == null || friendId == null) {
                return PacketBuilder.create(MessageType.REJECT_FRIEND_RESPONSE)
                        .error("Missing required fields")
                        .build();
            }

            friendDAO.rejectFriendRequest(userId, friendId);

            logger.info("Friend request rejected: " + userId + " rejected " + friendId);

            return PacketBuilder.create(MessageType.REJECT_FRIEND_RESPONSE)
                    .success(true)
                    .put("message", "Friend request rejected")
                    .build();

        } catch (SQLException e) {
            logger.error("Database error while rejecting friend request: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.REJECT_FRIEND_RESPONSE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error rejecting friend request: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.REJECT_FRIEND_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xử lý lấy danh sách bạn bè
     */
    public Packet handleGetFriends(Packet request) {
        try {
            Long userId = request.getLong("userId");

            if (userId == null) {
                return PacketBuilder.create(MessageType.GET_FRIENDS_RESPONSE)
                        .error("User ID is required")
                        .build();
            }

            List<Friend> friends = friendDAO.getFriendsList(userId);

            logger.info("Retrieved " + friends.size() + " friends for user " + userId);

            return PacketBuilder.create(MessageType.GET_FRIENDS_RESPONSE)
                    .success(true)
                    .put("friends", friends)
                    .put("count", friends.size())
                    .build();

        } catch (SQLException e) {
            logger.error("Database error while getting friends: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.GET_FRIENDS_RESPONSE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error getting friends: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.GET_FRIENDS_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xử lý lấy danh sách lời mời kết bạn đang chờ
     */
    public Packet handleGetPendingRequests(Packet request) {
        try {
            Long userId = request.getLong("userId");

            if (userId == null) {
                return PacketBuilder.create(MessageType.GET_PENDING_REQUESTS_RESPONSE)
                        .error("User ID is required")
                        .build();
            }

            List<Friend> requests = friendDAO.getPendingRequests(userId);

            logger.info("Retrieved " + requests.size() + " pending requests for user " + userId);

            return PacketBuilder.create(MessageType.GET_PENDING_REQUESTS_RESPONSE)
                    .success(true)
                    .put("requests", requests)
                    .put("count", requests.size())
                    .build();

        } catch (SQLException e) {
            logger.error("Database error while getting pending requests: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.GET_PENDING_REQUESTS_RESPONSE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error getting pending requests: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.GET_PENDING_REQUESTS_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xử lý xóa bạn
     */
    public Packet handleUnfriend(Packet request) {
        try {
            Long userId = request.getLong("userId");
            Long friendId = request.getLong("friendId");

            if (userId == null || friendId == null) {
                return PacketBuilder.create(MessageType.UNFRIEND_RESPONSE)
                        .error("Missing required fields")
                        .build();
            }

            friendDAO.unfriend(userId, friendId);

            logger.info("Unfriend: " + userId + " unfriended " + friendId);

            return PacketBuilder.create(MessageType.UNFRIEND_RESPONSE)
                    .success(true)
                    .put("message", "Friend removed successfully")
                    .build();

        } catch (SQLException e) {
            logger.error("Database error while unfriending: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.UNFRIEND_RESPONSE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error unfriending: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.UNFRIEND_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xử lý chặn người dùng
     */
    public Packet handleBlockUser(Packet request) {
        try {
            Long userId = request.getLong("userId");
            Long blockedUserId = request.getLong("blockedUserId");

            if (userId == null || blockedUserId == null) {
                return PacketBuilder.create(MessageType.BLOCK_FRIEND_RESPONSE)
                        .error("Missing required fields")
                        .build();
            }

            friendDAO.blockUser(userId, blockedUserId);

            logger.info("User blocked: " + userId + " blocked " + blockedUserId);

            return PacketBuilder.create(MessageType.BLOCK_FRIEND_RESPONSE)
                    .success(true)
                    .put("message", "User blocked successfully")
                    .build();

        } catch (SQLException e) {
            logger.error("Database error while blocking user: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.BLOCK_FRIEND_RESPONSE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error blocking user: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.BLOCK_FRIEND_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xử lý tìm kiếm người dùng
     */
    public Packet handleSearchUsers(Packet request) {
        try {
            Long userId = request.getLong("userId");
            String keyword = request.getString("keyword");

            if (userId == null || keyword == null || keyword.trim().isEmpty()) {
                return PacketBuilder.create(MessageType.SEARCH_USERS_RESPONSE)
                        .error("Missing required fields")
                        .build();
            }

            List<User> users = friendDAO.searchUsers(userId, keyword.trim());

            // Không trả về password hash
            users.forEach(user -> user.setPasswordHash(null));

            logger.info("Search users: found " + users.size() + " results for keyword: " + keyword);

            return PacketBuilder.create(MessageType.SEARCH_USERS_RESPONSE)
                    .success(true)
                    .put("users", users)
                    .put("count", users.size())
                    .build();

        } catch (SQLException e) {
            logger.error("Database error while searching users: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.SEARCH_USERS_RESPONSE)
                    .error("Database error: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Error searching users: " + e.getMessage(), e);
            return PacketBuilder.create(MessageType.SEARCH_USERS_RESPONSE)
                    .error("Server error: " + e.getMessage())
                    .build();
        }
    }
}