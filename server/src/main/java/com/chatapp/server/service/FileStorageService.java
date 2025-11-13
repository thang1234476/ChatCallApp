package com.chatapp.server.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileStorageService {

    private final String avatarStoragePath;

    public FileStorageService(String avatarStoragePath) {
        this.avatarStoragePath = avatarStoragePath;
        initializeStorage();
    }

    private void initializeStorage() {
        try {
            Path path = Paths.get(avatarStoragePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("[FILE_STORAGE] Created avatar directory: " + avatarStoragePath);
            }
        } catch (IOException e) {
            System.err.println("[FILE_STORAGE] Failed to create avatar directory: " + e.getMessage());
        }
    }

    /**
     * Lưu avatar và trả về URL
     */
    public String saveAvatar(byte[] fileData, String fileName) throws IOException {
        Path filePath = Paths.get(avatarStoragePath, fileName);
        Files.write(filePath, fileData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Trả về relative path hoặc URL
        return "/avatars/" + fileName;
    }

    /**
     * Xóa file avatar
     */
    public boolean deleteFile(String avatarUrl) {
        try {
            // Extract file name from URL
            String fileName = avatarUrl.substring(avatarUrl.lastIndexOf('/') + 1);
            Path filePath = Paths.get(avatarStoragePath, fileName);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("[FILE_STORAGE] Deleted avatar: " + fileName);
                return true;
            }
        } catch (IOException e) {
            System.err.println("[FILE_STORAGE] Failed to delete avatar: " + e.getMessage());
        }
        return false;
    }

    /**
     * Lấy file data
     */
    public byte[] getFile(String avatarUrl) throws IOException {
        String fileName = avatarUrl.substring(avatarUrl.lastIndexOf('/') + 1);
        Path filePath = Paths.get(avatarStoragePath, fileName);
        return Files.readAllBytes(filePath);
    }
}