package com.chatapp.client.controller.component;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class ChatListController {
    @FXML private TextField searchField;
    @FXML private ListView<?> chatListView;
    @FXML private Button allChatsTab;
    @FXML private Button groupsTab;

    private Button activeTab;

    @FXML
    public void initialize() {
        activeTab = allChatsTab;
        System.out.println("[CHAT LIST] Initialized");
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        System.out.println("Search: " + query);
    }

    @FXML
    private void startNewChat() {
        System.out.println("Start new chat");
    }

    @FXML
    private void showAllChats() {
        setActiveTab(allChatsTab);
        System.out.println("Show all chats");
    }

    @FXML
    private void showGroups() {
        setActiveTab(groupsTab);
        System.out.println("Show groups");
    }

    private void setActiveTab(Button tab) {
        if (activeTab != null) {
            activeTab.getStyleClass().remove("chat-tab-active");
        }
        tab.getStyleClass().add("chat-tab-active");
        activeTab = tab;
    }
}