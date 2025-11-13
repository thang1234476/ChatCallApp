package com.chatapp.server;

import com.chatapp.server.core.ChatServer;
import com.chatapp.server.database.DatabaseManager;
import com.chatapp.server.util.Logger;

public class ServerMain {
    private static final Logger logger = Logger.getInstance();

    public static void main(String[] args) {
        logger.info("=================================");
        logger.info("  ChatApp Server Starting...   ");
        logger.info("=================================");

        try {
            // Initialize database
            logger.info("Initializing database connection...");
            DatabaseManager.getInstance().initialize();
            logger.info("Database connection established");

            // Start server
            logger.info("Starting chat server...");
            ChatServer server = new ChatServer();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down server...");
                server.stop();
                DatabaseManager.getInstance().close();
                logger.info("Server stopped");
            }));

            // Start server (blocking call)
            server.start();

        } catch (Exception e) {
            logger.error("Failed to start server: " + e.getMessage(), e);
            System.exit(1);
        }
    }
}