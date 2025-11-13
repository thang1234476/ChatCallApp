package com.chatapp.client.controller.component;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ChatViewController {
    @FXML private Label contactNameLabel;
    @FXML private Label contactStatusLabel;
    @FXML private Label contactAvatarLabel;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesContainer;
    @FXML private TextField messageInputField;

    @FXML private Button videoCallBtn;
    @FXML private Button searchBtn;
    @FXML private Button menuBtn;
    @FXML private Button attachBtn;
    @FXML private Button emojiBtn;
    @FXML private Button voiceBtn;

    @FXML
    public void initialize() {
        System.out.println("[CHAT VIEW] Initialized");

        // Test buttons
        if (videoCallBtn != null) System.out.println("[CHAT VIEW] Video call button found");
        if (attachBtn != null) System.out.println("[CHAT VIEW] Attach button found");
        if (emojiBtn != null) System.out.println("[CHAT VIEW] Emoji button found");
    }

    @FXML
    private void startVideoCall() {
        System.out.println("[ACTION] ✅ Video call button clicked!");
        showInfo("Video Call", "Đang phát triển tính năng gọi video...");
    }

    @FXML
    private void searchInChat() {
        System.out.println("[ACTION] ✅ Search button clicked!");
        showInfo("Tìm kiếm", "Đang phát triển tính năng tìm kiếm...");
    }

    @FXML
    private void openChatMenu() {
        System.out.println("[ACTION] ✅ Menu button clicked!");
        showInfo("Menu", "Đang phát triển menu chat...");
    }

    @FXML
    private void attachFile() {
        System.out.println("[ACTION] ✅ Attach file button clicked!");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tất cả", "*.*"),
                new FileChooser.ExtensionFilter("Hình ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tài liệu", "*.pdf", "*.doc", "*.docx", "*.txt")
        );

        Stage stage = (Stage) attachBtn.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            System.out.println("[FILE] ✅ Selected: " + file.getName());
            showInfo("File đã chọn", file.getName() + "\n(" + file.length() + " bytes)");
        }
    }

    @FXML
    private void sendMessage() {
        String text = messageInputField.getText().trim();
        if (!text.isEmpty()) {
            System.out.println("[MESSAGE] ✅ Send: " + text);

            // Add message to UI (temporary)
            addMessageToUI(text, true);

            messageInputField.clear();
        }
    }

    @FXML
    private void addEmoji() {
        System.out.println("[ACTION] ✅ Emoji button clicked!");

        // Simple emoji menu
        ChoiceDialog<String> dialog = new ChoiceDialog<>("😀",
                "😀", "😂", "❤️", "👍", "🎉", "😊", "🔥", "✨", "👋", "🎈");
        dialog.setTitle("Chọn emoji");
        dialog.setHeaderText("Chọn emoji để thêm");
        dialog.setContentText("Emoji:");

        dialog.showAndWait().ifPresent(emoji -> {
            String current = messageInputField.getText();
            messageInputField.setText(current + emoji);
            messageInputField.positionCaret(messageInputField.getText().length());
        });
    }

    @FXML
    private void recordVoice() {
        System.out.println("[ACTION] ✅ Voice button clicked!");
        showInfo("Ghi âm", "Đang phát triển tính năng ghi âm...");
    }

    private void addMessageToUI(String text, boolean isSent) {
        Label messageLabel = new Label(text);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);
        messageLabel.setPadding(new Insets(10));

        if (isSent) {
            messageLabel.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; " +
                    "-fx-background-radius: 18 18 4 18; -fx-padding: 10;");
        } else {
            messageLabel.setStyle("-fx-background-color: white; -fx-text-fill: black; " +
                    "-fx-background-radius: 18 18 18 4; -fx-padding: 10;");
        }

        messagesContainer.getChildren().add(messageLabel);

        // Scroll to bottom
        messagesScrollPane.setVvalue(1.0);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}