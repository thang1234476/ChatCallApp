package com.chatapp.client.controller.component;

import com.chatapp.client.service.FriendService;
import com.chatapp.common.model.Friend;
import com.chatapp.common.model.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller cho Friend/Contact List - Telegram Style
 */
public class FriendListController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Button allContactsTab;
    @FXML private Button requestsTab;
    @FXML private Button addFriendTab;
    @FXML private ListView<Friend> contactListView;
    @FXML private Label friendCountLabel;

    private User currentUser;
    private FriendService friendService;
    private ObservableList<Friend> friendsList;
    private ObservableList<Friend> requestsList;
    private ObservableList<User> searchResults;

    private String currentTab = "ALL"; // ALL, REQUESTS, ADD

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        friendService = FriendService.getInstance();

        // Initialize observable lists
        friendsList = FXCollections.observableArrayList();
        requestsList = FXCollections.observableArrayList();
        searchResults = FXCollections.observableArrayList();

        // Set up ListView
        setupContactListView();

        // Set default tab active
        setActiveTab("ALL");
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadAllContacts();
    }

    /**
     * Setup Contact ListView with custom cell factory
     */
    private void setupContactListView() {
        contactListView.setItems(friendsList);
        contactListView.setCellFactory(lv -> new ListCell<Friend>() {
            @Override
            protected void updateItem(Friend friend, boolean empty) {
                super.updateItem(friend, empty);
                if (empty || friend == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    if (currentTab.equals("REQUESTS")) {
                        setGraphic(createRequestCell(friend));
                    } else {
                        setGraphic(createContactCell(friend));
                    }
                }
            }
        });
    }

    /**
     * Tạo cell cho contact item
     */
    private HBox createContactCell(Friend friend) {
        HBox cell = new HBox(12);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setPadding(new Insets(12, 16, 12, 16));
        cell.getStyleClass().add("contact-cell");

        // Avatar với status indicator
        StackPane avatarPane = createAvatar(
                friend.getFriendUsername(),
                friend.getFriendStatusType()
        );

        // Info
        VBox infoBox = new VBox(4);
        Label nameLabel = new Label(friend.getFriendFullName());
        nameLabel.getStyleClass().add("contact-name");

        Label statusLabel = new Label(getStatusText(
                friend.getFriendStatusType(),
                friend.getFriendLastSeen()
        ));
        statusLabel.getStyleClass().add("contact-status");

        infoBox.getChildren().addAll(nameLabel, statusLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Action buttons (hidden by default, show on hover)
        HBox actionsBox = new HBox(8);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);
        actionsBox.getStyleClass().add("contact-actions");

        Button chatBtn = new Button();
        chatBtn.setGraphic(new FontAwesomeIconView(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.COMMENT));
        chatBtn.getStyleClass().add("icon-button");
        chatBtn.setOnAction(e -> openChat(friend));

        Button videoBtn = new Button();
        videoBtn.setGraphic(new FontAwesomeIconView(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.VIDEO_CAMERA));
        videoBtn.getStyleClass().add("icon-button");
        videoBtn.setOnAction(e -> startVideoCall(friend));

        Button moreBtn = new Button();
        moreBtn.setGraphic(new FontAwesomeIconView(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ELLIPSIS_V));
        moreBtn.getStyleClass().add("icon-button");
        moreBtn.setOnAction(e -> showContactMenu(friend, moreBtn));

        actionsBox.getChildren().addAll(chatBtn, videoBtn, moreBtn);

        cell.getChildren().addAll(avatarPane, infoBox, actionsBox);
        return cell;
    }

    /**
     * Tạo cell cho friend request
     */
    private HBox createRequestCell(Friend request) {
        HBox cell = new HBox(12);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setPadding(new Insets(12, 16, 12, 16));
        cell.getStyleClass().add("request-cell");

        // Avatar
        StackPane avatarPane = createAvatar(request.getFriendUsername(), User.UserStatus.OFFLINE);

        // Info
        VBox infoBox = new VBox(4);
        Label nameLabel = new Label(request.getFriendFullName());
        nameLabel.getStyleClass().add("contact-name");

        Label usernameLabel = new Label("@" + request.getFriendUsername());
        usernameLabel.getStyleClass().add("contact-username");

        infoBox.getChildren().addAll(nameLabel, usernameLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Action buttons
        HBox actionsBox = new HBox(8);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button acceptBtn = new Button("Accept");
        acceptBtn.getStyleClass().addAll("primary-button", "small-button");
        acceptBtn.setOnAction(e -> acceptRequest(request));

        Button rejectBtn = new Button("Reject");
        rejectBtn.getStyleClass().addAll("secondary-button", "small-button");
        rejectBtn.setOnAction(e -> rejectRequest(request));

        actionsBox.getChildren().addAll(acceptBtn, rejectBtn);

        cell.getChildren().addAll(avatarPane, infoBox, actionsBox);
        return cell;
    }

    /**
     * Tạo cell cho search result (add friend mode)
     */
    private HBox createSearchResultCell(Friend friend) {
        HBox cell = new HBox(12);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setPadding(new Insets(12, 16, 12, 16));
        cell.getStyleClass().add("search-result-cell");

        // Avatar
        StackPane avatarPane = createAvatar(
                friend.getFriendUsername(),
                friend.getFriendStatusType() != null ? friend.getFriendStatusType() : User.UserStatus.OFFLINE
        );

        // Info
        VBox infoBox = new VBox(4);
        Label nameLabel = new Label(friend.getFriendFullName());
        nameLabel.getStyleClass().add("contact-name");

        Label usernameLabel = new Label("@" + friend.getFriendUsername());
        usernameLabel.getStyleClass().add("contact-username");

        if (friend.getFriendStatusMessage() != null && !friend.getFriendStatusMessage().isEmpty()) {
            Label bioLabel = new Label(friend.getFriendStatusMessage());
            bioLabel.getStyleClass().add("contact-bio");
            bioLabel.setWrapText(true);
            bioLabel.setMaxWidth(200);
            infoBox.getChildren().addAll(nameLabel, usernameLabel, bioLabel);
        } else {
            infoBox.getChildren().addAll(nameLabel, usernameLabel);
        }
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Add button
        Button addBtn = new Button("Add Friend");
        addBtn.getStyleClass().addAll("primary-button", "small-button");
        addBtn.setOnAction(e -> sendFriendRequestFromSearch(friend));

        cell.getChildren().addAll(avatarPane, infoBox, addBtn);
        return cell;
    }

    private void sendFriendRequestFromSearch(Friend friend) {
        new Thread(() -> {
            try {
                var response = friendService.sendFriendRequest(currentUser.getId(), friend.getFriendId());
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        showSuccess("Friend request sent to " + friend.getFriendFullName());
                        contactListView.getItems().remove(friend);
                    } else {
                        showError(response.getError());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Tạo avatar với status indicator
     */
    private StackPane createAvatar(String username, User.UserStatus status) {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("avatar-container");

        // Avatar circle
        Label avatar = new Label(username.substring(0, 1).toUpperCase());
        avatar.getStyleClass().add("contact-avatar");
        avatar.setStyle("-fx-background-color: " + getAvatarColor(username) + ";");

        // Status indicator
        Circle statusCircle = new Circle(6);
        statusCircle.setFill(Color.web(getStatusDotColor(status)));
        statusCircle.setStroke(Color.WHITE);
        statusCircle.setStrokeWidth(2);
        statusCircle.getStyleClass().add("status-indicator");
        StackPane.setAlignment(statusCircle, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(statusCircle, new Insets(0, 2, 2, 0));

        pane.getChildren().addAll(avatar, statusCircle);
        return pane;
    }

    /**
     * Load all contacts
     */
    private void loadAllContacts() {
        if (currentUser == null) return;

        new Thread(() -> {
            try {
                List<Friend> friends = friendService.getFriendsList(currentUser.getId());
                Platform.runLater(() -> {
                    friendsList.setAll(friends);
                    updateContactCount(friends.size());
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Failed to load contacts: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Load pending requests
     */
    private void loadPendingRequests() {
        if (currentUser == null) return;

        new Thread(() -> {
            try {
                List<Friend> requests = friendService.getPendingRequests(currentUser.getId());
                Platform.runLater(() -> {
                    requestsList.setAll(requests);
                    updateRequestCount(requests.size());
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Failed to load requests: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Tab Actions
     */
    @FXML
    private void showAllContacts() {
        setActiveTab("ALL");
        contactListView.setItems(friendsList);
        loadAllContacts();
    }

    @FXML
    private void showRequests() {
        setActiveTab("REQUESTS");
        contactListView.setItems(requestsList);
        loadPendingRequests();
    }

    @FXML
    private void showAddFriend() {
        setActiveTab("ADD");
        searchField.setPromptText("Search users to add...");

        // Clear current list và setup cho User objects
        contactListView.setItems(FXCollections.observableArrayList());

        // ✅ FIX: Setup ListView để hiển thị User thay vì Friend
        contactListView.setCellFactory(lv -> new ListCell<Friend>() {
            @Override
            protected void updateItem(Friend item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(null);

                // Tab Add Friend không dùng Friend objects
                // Sẽ dùng riêng một ListView khác cho search results
            }
        });

        // Show empty state message
        if (friendCountLabel != null) {
            friendCountLabel.setText("Search users to add friends");
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();

        if (currentTab.equals("ADD")) {
            // ✅ Search for new users to add
            if (keyword.isEmpty()) {
                showError("Please enter a search keyword");
                return;
            }

            new Thread(() -> {
                try {
                    List<User> users = friendService.searchUsers(currentUser.getId(), keyword);
                    Platform.runLater(() -> {
                        if (users.isEmpty()) {
                            showInfo("No users found");
                            contactListView.setItems(FXCollections.observableArrayList());
                        } else {
                            // ✅ Chuyển User list thành Friend list để hiển thị
                            ObservableList<Friend> searchResultsAsFriends = FXCollections.observableArrayList();
                            for (User user : users) {
                                Friend pseudoFriend = new Friend();
                                pseudoFriend.setFriendId(user.getId());
                                pseudoFriend.setFriendUsername(user.getUsername());
                                pseudoFriend.setFriendFullName(user.getFullName());
                                pseudoFriend.setFriendAvatarUrl(user.getAvatarUrl());
                                pseudoFriend.setFriendStatusMessage(user.getStatusMessage());
                                pseudoFriend.setFriendStatusType(user.getStatusType());
                                pseudoFriend.setStatus(null); // Mark as search result
                                searchResultsAsFriends.add(pseudoFriend);
                            }

                            contactListView.setItems(searchResultsAsFriends);

                            // ✅ Setup cell factory để render search results
                            contactListView.setCellFactory(lv -> new ListCell<Friend>() {
                                @Override
                                protected void updateItem(Friend friend, boolean empty) {
                                    super.updateItem(friend, empty);
                                    if (empty || friend == null) {
                                        setGraphic(null);
                                        setText(null);
                                    } else {
                                        setGraphic(createSearchResultCell(friend));
                                    }
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showError("Search failed: " + e.getMessage()));
                }
            }).start();
        } else {
            // Filter current list (All Contacts or Requests)
            if (keyword.isEmpty()) {
                if (currentTab.equals("ALL")) {
                    loadAllContacts();
                } else {
                    loadPendingRequests();
                }
            } else {
                filterContacts(keyword);
            }
        }
    }

    @FXML
    private void startNewChat() {
        setActiveTab("ADD");
        showAddFriend();
    }

    private void filterContacts(String keyword) {
        ObservableList<Friend> sourceList = currentTab.equals("ALL") ? friendsList : requestsList;
        ObservableList<Friend> filtered = sourceList.filtered(friend ->
                friend.getFriendFullName().toLowerCase().contains(keyword.toLowerCase()) ||
                        friend.getFriendUsername().toLowerCase().contains(keyword.toLowerCase())
        );
        contactListView.setItems(filtered);
    }

    private void setActiveTab(String tab) {
        currentTab = tab;

        // Remove active class from all tabs
        allContactsTab.getStyleClass().remove("chat-tab-active");
        requestsTab.getStyleClass().remove("chat-tab-active");
        addFriendTab.getStyleClass().remove("chat-tab-active");

        // Add active class to selected tab
        switch (tab) {
            case "ALL":
                allContactsTab.getStyleClass().add("chat-tab-active");
                break;
            case "REQUESTS":
                requestsTab.getStyleClass().add("chat-tab-active");
                break;
            case "ADD":
                addFriendTab.getStyleClass().add("chat-tab-active");
                break;
        }
    }

    private void updateContactCount(int count) {
        if (friendCountLabel != null) {
            friendCountLabel.setText(count + " contacts");
        }
    }

    private void updateRequestCount(int count) {
        if (count > 0) {
            requestsTab.setText("Requests (" + count + ")");
        } else {
            requestsTab.setText("Requests");
        }
    }

    // Friend Actions
    private void sendFriendRequest(User user) {
        new Thread(() -> {
            try {
                var response = friendService.sendFriendRequest(currentUser.getId(), user.getId());
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        showSuccess("Friend request sent!");
                        searchResults.remove(user);
                    } else {
                        showError(response.getError());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed: " + e.getMessage()));
            }
        }).start();
    }

    private void acceptRequest(Friend request) {
        new Thread(() -> {
            try {
                var response = friendService.acceptFriendRequest(currentUser.getId(), request.getFriendId());
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        showSuccess("Friend request accepted!");
                        requestsList.remove(request);
                        loadAllContacts();
                        loadPendingRequests();
                    } else {
                        showError(response.getError());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed: " + e.getMessage()));
            }
        }).start();
    }

    private void rejectRequest(Friend request) {
        new Thread(() -> {
            try {
                var response = friendService.rejectFriendRequest(currentUser.getId(), request.getFriendId());
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        requestsList.remove(request);
                        loadPendingRequests();
                    } else {
                        showError(response.getError());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed: " + e.getMessage()));
            }
        }).start();
    }

    private void openChat(Friend friend) {
        showInfo("Opening chat with " + friend.getFriendFullName());
        // TODO: Implement open chat
    }

    private void startVideoCall(Friend friend) {
        showInfo("Starting video call with " + friend.getFriendFullName());
        // TODO: Implement video call
    }

    private void showContactMenu(Friend friend, Button button) {
        ContextMenu menu = new ContextMenu();

        MenuItem viewProfile = new MenuItem("View Profile");
        viewProfile.setOnAction(e -> showInfo("View profile coming soon"));

        MenuItem unfriend = new MenuItem("Remove Friend");
        unfriend.setOnAction(e -> confirmUnfriend(friend));

        MenuItem block = new MenuItem("Block");
        block.setOnAction(e -> confirmBlock(friend));

        menu.getItems().addAll(viewProfile, new SeparatorMenuItem(), unfriend, block);
        menu.show(button, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void confirmUnfriend(Friend friend) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Friend");
        alert.setHeaderText("Remove " + friend.getFriendFullName() + "?");
        alert.setContentText("You can add them as a friend again later.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        friendService.unfriend(currentUser.getId(), friend.getFriendId());
                        Platform.runLater(() -> {
                            friendsList.remove(friend);
                            loadAllContacts();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showError("Failed: " + e.getMessage()));
                    }
                }).start();
            }
        });
    }

    private void confirmBlock(Friend friend) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Block User");
        alert.setHeaderText("Block " + friend.getFriendFullName() + "?");
        alert.setContentText("They won't be able to message you.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        friendService.blockUser(currentUser.getId(), friend.getFriendId());
                        Platform.runLater(() -> {
                            friendsList.remove(friend);
                            loadAllContacts();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showError("Failed: " + e.getMessage()));
                    }
                }).start();
            }
        });
    }

    // Helper methods
    private String getAvatarColor(String username) {
        String[] colors = {"#1a73e8", "#34a853", "#ea4335", "#fbbc04", "#ff6d00", "#e91e63", "#9c27b0"};
        return colors[Math.abs(username.hashCode()) % colors.length];
    }

    private String getStatusDotColor(User.UserStatus status) {
        switch (status) {
            case ONLINE: return "#34a853";
            case BUSY: return "#fbbc04";
            default: return "#dadce0";
        }
    }

    private String getStatusText(User.UserStatus status, LocalDateTime lastSeen) {
        if (status == User.UserStatus.ONLINE) {
            return "online";
        } else if (status == User.UserStatus.BUSY) {
            return "busy";
        } else if (lastSeen != null) {
            return "last seen " + lastSeen.format(TIME_FORMATTER);
        }
        return "offline";
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}