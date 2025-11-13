package com.chatapp.client.network;

import com.chatapp.common.protocol.Packet;
import com.chatapp.common.util.JsonUtil;

import java.io.*;
import java.net.Socket;

public class ServerConnection {
    private static ServerConnection instance;
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private boolean connected;

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    public void connect(String host, int port) throws IOException {
        System.out.println("[CLIENT] Connecting to " + host + ":" + port);
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true); // Disable Nagle's algorithm
        socket.setSoTimeout(30000); // 30 second timeout

        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        connected = true;

        System.out.println("[CLIENT] Connected successfully");
    }

    /**
     * Send packet and wait for response (SYNCHRONOUS)
     */
    public synchronized Packet sendAndReceive(Packet packet) throws IOException {
        if (!connected || socket == null || socket.isClosed()) {
            throw new IOException("Not connected to server");
        }

        // Serialize and send
        String requestJson = JsonUtil.toJson(packet);
        System.out.println("[CLIENT] >>> SEND: " + requestJson);

        output.println(requestJson);
        output.flush();

        // Wait for response
        String responseLine = input.readLine();

        if (responseLine == null) {
            throw new IOException("Server closed connection");
        }

        System.out.println("[CLIENT] <<< RECV: " + responseLine);

        // Parse response
        Packet response = JsonUtil.fromJson(responseLine, Packet.class);
        return response;
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null) socket.close();
            if (input != null) input.close();
            if (output != null) output.close();
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}