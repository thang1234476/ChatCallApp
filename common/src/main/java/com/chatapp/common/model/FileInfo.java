package com.chatapp.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class FileInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long uploaderId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String mimeType;
    private String checksum;
    private LocalDateTime uploadedAt;

    public FileInfo() {}

    public FileInfo(Long uploaderId, String fileName, Long fileSize, String fileType, String mimeType) {
        this.uploaderId = uploaderId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.mimeType = mimeType;
        this.uploadedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}