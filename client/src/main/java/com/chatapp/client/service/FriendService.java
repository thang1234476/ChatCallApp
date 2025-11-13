package com.chatapp.client.service;

import com.chatapp.client.network.ServerConnection;
import com.chatapp.common.model.Friend;
import com.chatapp.common.model.User;
import com.chatapp.common.protocol.Packet;
import com.chatapp.common.protocol.PacketBuilder;
import com.chatapp.common.protocol.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service xử lý các thao tác friend ở client
 */
public class FriendService {
    private static FriendService instance;
    private ServerConnection connection;

    private FriendService() {
        this.connection = ServerConnection.getInstance();
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
     * Gửi lời mời kết bạn
     */
    public Packet sendFriendRequest(Long userId, Long friendId) throws Exception {
        if (!connection.isConnected()) {
            throw new Exception("Not connected to server");
        }

        System.out.println("[FriendService] Sending friend request from " + userId + " to " + friendId);

        Packet request = PacketBuilder.create(MessageType.ADD_FRIEND_REQUEST)
                .put("userId", userId)
                .put("friendId", friendId)
                .build();

        Packet response = connection.sendAndReceive(request);

        if (response.isSuccess()) {
            System.out.println("[FriendService] Friend request sent successfully");
        } else {
            System.err.println("[FriendService] Failed to send friend request: " + response.getError());
        }

        return response;
    }

    /**
     * Chấp nhận lời mời kết bạn
     */
    public Packet acceptFriendRequest(Long userId, Long friendId) throws Exception {
        if (!connection.isConnected()) {
            throw new Exception("Not connected to server");
        }

        System.out.println("[FriendService] Accepting friend request from " + friendId);

        Packet request = PacketBuilder.create(MessageType.ACCEPT_FRIEND_REQUEST)
                .put("userId", userId)
                .put("friendId", friendId)
                .build();

        Packet response = connection.sendAndReceive(request);

        if (response.isSuccess()) {
            System.out.println("[FriendService] Friend request accepted");
        } else {
            System.err.println("[FriendService] Failed to accept friend request: " + response.getError());
        }

        return response;
    }

    /**
     * Từ chối lời mời kết bạn
     */
    public Packet rejectFriendRequest(Long userId, Long friendId) throws Exception {
        if (!connection.isConnected()) {
            throw new Exception("Not connected to server");
        }

        System.out.println("[FriendService] Rejecting friend request from " + friendId);

        Packet request = PacketBuilder.create(MessageType.REJECT_FRIEND_REQUEST)
                .put("userId", userId)
                .put("friendId", friendId)
                .build();

        Packet response = connection.sendAndReceive(request);

        if (response.isSuccess()) {
            System.out.println("[FriendService] Friend request rejected");
        } else {
            System.err.println("[FriendService] Failed to reject friend request: " + response.getError());
        }

        return response;
    }

    /**
     * Xóa bạn
     */
    public Packet unfriend(Long userId, Long friendId) throws Exception {
        if (!connection.isConnected()) {
            throw new Exception("Not connected to server");
        }

        System.out.println("[FriendService] Unfriending user " + friendId);

        Packet request = PacketBuilder.create(MessageType.UNFRIEND_REQUEST)
                .put("userId", userId)
                .put("friendId", friendId)
                .build();

        Packet response = connection.sendAndReceive(request);

        if (response.isSuccess()) {
            System.out.println("[FriendService] Friend removed successfully");
        } else {
            System.err.println("[FriendService] Failed to unfriend: " + response.getError());
        }

        return response;
    }

    /**
     * Chặn người dùng
     */
    public Packet blockUser(Long userId, Long blockedUserId) throws Exception {
        if (!connection.isConnected()) {
            throw new Exception("Not connected to server");
        }

        System.out.println("[FriendService] Blocking user " + blockedUserId);

        Packet request = PacketBuilder.create(MessageType.BLOCK_FRIEND_REQUEST)
                .put("userId", userId)
                .put("blockedUserId", blockedUserId)
                .build();

        Packet response = connection.sendAndReceive(request);

        if (response.isSuccess()) {
            System.out.println("[FriendService] User blocked successfully");
        } else {
            System.err.println("[FriendService] Failed to block user: " + response.getError());
        }

        return response;
    }

    /**
     * Lấy danh sách bạn bè
     */
    public List<Friend> getFriendsList(Long userId) throws Exception {
        if (!connection.isConnected()) {
            throw new Exception("Not connected to server");
        }

        System.out.println("[FriendService] Getting friends list for user " + userId);

        Packet request = PacketBuilder.create(MessageType.GET_FRIENDS_REQUEST)
                .put("userId", userId)
                .build();

        Packet response = connection.sendAndReceive(request);

        if (response.isSuccess()) {
            Object friendsObj = response.get("friends");
            if (friendsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> friendMaps = (List<Map<String, Object>>) friendsObj;
                List<Friend> friends = new ArrayList<>();

                for (Map<String, Object> map : friendMaps) {
                    friends.add(mapToFriend(map));
                }

                System.out.println("[FriendService] Retrieved " + friends.size() + " friends");
                return friends;
            }
        } else {
            System.err.println("[FriendService] Failed to get friends: " + response.getError());
        }

        return new ArrayList<>();
    }

    /**
     * Lấy danh sách lời mời kết bạn đang chờ
     */
    public List<Friend> getPendingRequests(Long userId) throws Exception {
        if (!connection.isConnected()) {
            throw new Exception("Not connected to server");
        }

        System.out.println("[FriendService] Getting pending requests for user " + userId);

        Packet request = PacketBuilder.create(MessageType.GET_PENDING_REQUESTS_REQUEST)
                .put("userId", userId)
                .build();

        Packet response = connection.sendAndReceive(request);

        if (response.isSuccess()) {
            Object requestsObj = response.get("requests");
            if (requestsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> requestMaps = (List<Map<String, Object>>) requestsObj;
                List<Friend> requests = new ArrayList<>();

                for (Map<String, Object> map : requestMaps) {
                    requests.add(mapToFriend(map));
                }

                System.out.println("[FriendService] Retrieved " + requests.size() + " pending requests");
                return requests;
            }
        } else {
            System.err.println("[FriendService] Failed to get pending requests: " + response.getError());
        }

        return new ArrayList<>();
    }

    /**
     * Tìm kiếm người dùng
     */
    public List<User> searchUsers(Long userId, String keyword) throws Exception {
        if (!connection.isConnected()) {
            throw new Exception("Not connected to server");
        }

        System.out.println("[FriendService] Searching users with keyword: " + keyword);

        Packet request = PacketBuilder.create(MessageType.SEARCH_USERS_REQUEST)
                .put("userId", userId)
                .put("keyword", keyword)
                .build();

        Packet response = connection.sendAndReceive(request);

        if (response.isSuccess()) {
            Object usersObj = response.get("users");
            if (usersObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> userMaps = (List<Map<String, Object>>) usersObj;
                List<User> users = new ArrayList<>();

                for (Map<String, Object> map : userMaps) {
                    users.add(mapToUser(map));
                }

                System.out.println("[FriendService] Found " + users.size() + " users");
                return users;
            }
        } else {
            System.err.println("[FriendService] Failed to search users: " + response.getError());
        }

        return new ArrayList<>();
    }

    /**
     * Convert Map to Friend object
     */
    private Friend mapToFriend(Map<String, Object> map) {
        Friend friend = new Friend();

        if (map.get("id") instanceof Number) {
            friend.setId(((Number) map.get("id")).longValue());
        }
        if (map.get("userId") instanceof Number) {
            friend.setUserId(((Number) map.get("userId")).longValue());
        }
        if (map.get("friendId") instanceof Number) {
            friend.setFriendId(((Number) map.get("friendId")).longValue());
        }
        if (map.get("status") != null) {
            friend.setStatus(Friend.FriendStatus.valueOf(map.get("status").toString()));
        }

        friend.setFriendUsername((String) map.get("friendUsername"));
        friend.setFriendFullName((String) map.get("friendFullName"));
        friend.setFriendAvatarUrl((String) map.get("friendAvatarUrl"));
        friend.setFriendStatusMessage((String) map.get("friendStatusMessage"));

        if (map.get("friendStatusType") != null) {
            friend.setFriendStatusType(User.UserStatus.valueOf(map.get("friendStatusType").toString()));
        }

        return friend;
    }

    /**
     * Convert Map to User object
     */
    private User mapToUser(Map<String, Object> map) {
        User user = new User();

        if (map.get("id") instanceof Number) {
            user.setId(((Number) map.get("id")).longValue());
        }

        user.setUsername((String) map.get("username"));
        user.setFullName((String) map.get("fullName"));
        user.setEmail((String) map.get("email"));
        user.setAvatarUrl((String) map.get("avatarUrl"));
        user.setStatusMessage((String) map.get("statusMessage"));

        if (map.get("statusType") != null) {
            user.setStatusType(User.UserStatus.valueOf(map.get("statusType").toString()));
        }

        return user;
    }
}