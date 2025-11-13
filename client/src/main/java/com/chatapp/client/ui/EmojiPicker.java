package com.chatapp.client.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

public class EmojiPicker extends Popup {

    private static final String[] EMOJIS = {
            "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
            "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
            "😘", "😗", "😚", "😙", "🥲", "😋", "😛", "😜",
            "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "🤐",
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
            "💯", "💢", "💥", "💫", "💦", "💨", "🕳️", "💬",
            "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏",
            "✌️", "🤞", "🤟", "🤘", "🤙", "👈", "👉", "👆",
            "🔥", "⭐", "✨", "🎉", "🎊", "🎈", "🎁", "🎀"
    };

    private EmojiSelectedHandler handler;

    public interface EmojiSelectedHandler {
        void onEmojiSelected(String emoji);
    }

    public EmojiPicker(EmojiSelectedHandler handler) {
        this.handler = handler;

        VBox container = new VBox(10);
        container.setStyle("-fx-background-color: white; -fx-padding: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2); " +
                "-fx-background-radius: 8;");

        FlowPane emojiGrid = new FlowPane(5, 5);
        emojiGrid.setPadding(new Insets(5));
        emojiGrid.setPrefWrapLength(300);

        for (String emoji : EMOJIS) {
            Button emojiBtn = new Button(emoji);
            emojiBtn.setStyle("-fx-font-size: 20px; -fx-background-color: transparent; " +
                    "-fx-cursor: hand; -fx-padding: 5;");
            emojiBtn.setOnAction(e -> {
                if (handler != null) {
                    handler.onEmojiSelected(emoji);
                }
                hide();
            });

            emojiBtn.setOnMouseEntered(e ->
                    emojiBtn.setStyle("-fx-font-size: 20px; -fx-background-color: #f1f3f4; " +
                            "-fx-cursor: hand; -fx-padding: 5; -fx-background-radius: 4;")
            );
            emojiBtn.setOnMouseExited(e ->
                    emojiBtn.setStyle("-fx-font-size: 20px; -fx-background-color: transparent; " +
                            "-fx-cursor: hand; -fx-padding: 5;")
            );

            emojiGrid.getChildren().add(emojiBtn);
        }

        container.getChildren().add(emojiGrid);
        getContent().add(container);
    }
}