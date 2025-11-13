package com.chatapp.server.core;

import com.chatapp.server.network.ClientHandler;

import java.util.concurrent.ConcurrentHashMap;

public class ClientRegistry {
    private static ClientRegistry instance;
    private final ConcurrentHashMap<Long, ClientHandler> clients;

    private ClientRegistry() {
        this.clients = new ConcurrentHashMap<>();
    }

    public static ClientRegistry getInstance() {
        if (instance == null) {
            synchronized (ClientRegistry.class) {
                if (instance == null) {
                    instance = new ClientRegistry();
                }
            }
        }
        return instance;
    }

    public void addClient(Long userId, ClientHandler handler) {
        if (userId != null && handler != null) {
            clients.put(userId, handler);
        }
    }

    public void removeClient(Long userId) {
        if (userId != null) {
            clients.remove(userId);
        }
    }

    public ClientHandler getClient(Long userId) {
        return userId != null ? clients.get(userId) : null;
    }

    public boolean isOnline(Long userId) {
        return userId != null && clients.containsKey(userId);
    }

    public int getOnlineCount() {
        return clients.size();
    }
}
