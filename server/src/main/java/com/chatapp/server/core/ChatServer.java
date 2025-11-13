package com.chatapp.server.core;

import com.chatapp.server.network.ClientHandler;
import com.chatapp.server.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {
    private ServerSocket serverSocket;
    private boolean running;
    private final Logger logger = Logger.getInstance();
    private final ClientRegistry clientRegistry;

    public ChatServer() {
        this.clientRegistry = ClientRegistry.getInstance();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(8888);
            running = true;

            logger.info("Server started on port 8888");
            logger.info("Waiting for clients...");

            // Accept client connections
            while (running) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected: " + clientSocket.getInetAddress());

                // Create handler for client
                ClientHandler handler = new ClientHandler(clientSocket, clientRegistry);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            if (running) {
                logger.error("Server error", e);
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error stopping server", e);
        }
    }
}