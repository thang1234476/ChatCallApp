package com.chatapp.common.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Packet implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private Map<String, Object> data;
    private Long timestamp;
    private String error;
    private boolean success;

    public Packet() {
        this.data = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
        this.success = true;
    }

    public Packet(MessageType type) {
        this();
        this.type = type;
    }

    // Getters and Setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public void put(String key, Object value) { this.data.put(key, value); }
    public Object get(String key) { return this.data.get(key); }

    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    public Long getLong(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    public Integer getInt(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    public Boolean getBoolean(String key) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String getError() { return error; }
    public void setError(String error) {
        this.error = error;
        this.success = false;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}