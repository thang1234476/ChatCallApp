package com.chatapp.server.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static ConfigLoader instance;
    private Properties properties;

    private ConfigLoader() {
        properties = new Properties();
        loadConfig();
    }

    public static ConfigLoader getInstance() {
        if (instance == null) {
            synchronized (ConfigLoader.class) {
                if (instance == null) {
                    instance = new ConfigLoader();
                }
            }
        }
        return instance;
    }

    private void loadConfig() {
        try {
            // Thử load từ resources folder
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("server.properties");

            if (inputStream == null) {
                // Thử load từ file system
                inputStream = new FileInputStream("server.properties");
            }

            properties.load(inputStream);
            inputStream.close();

            System.out.println("✓ Đã load cấu hình thành công");

        } catch (IOException e) {
            System.err.println("✗ Không thể load file config: " + e.getMessage());
            System.err.println("→ Sử dụng cấu hình mặc định");
            loadDefaultConfig();
        }
    }

    private void loadDefaultConfig() {
        // Cấu hình mặc định
        properties.setProperty("server.port", "8888");
        properties.setProperty("db.url", "jdbc:mysql://localhost:3306/chatapp_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        properties.setProperty("db.username", "root");
        properties.setProperty("db.password", "");
        properties.setProperty("email.smtp.host", "smtp.gmail.com");
        properties.setProperty("email.smtp.port", "587");
        properties.setProperty("email.from", "travelweb09@gmail.com");
        properties.setProperty("email.password", "rxef rtrt wmxu ztsw");
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}