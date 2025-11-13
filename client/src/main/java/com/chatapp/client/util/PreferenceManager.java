package com.chatapp.client.util;

import com.chatapp.common.model.User;
import java.util.prefs.Preferences;

/**
 * Manager để lưu trữ preferences/settings của app
 */
public class PreferenceManager {
    private static PreferenceManager instance;
    private final Preferences prefs;
    private User currentUser;

    // Keys
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_LAST_LOGIN = "last_login";

    private PreferenceManager() {
        // Sử dụng node riêng cho app
        prefs = Preferences.userRoot().node("chatapp");
    }

    public static PreferenceManager getInstance() {
        if (instance == null) {
            synchronized (PreferenceManager.class) {
                if (instance == null) {
                    instance = new PreferenceManager();
                }
            }
        }
        return instance;
    }

    /**
     * Lưu thông tin user hiện tại
     */
    public void saveCurrentUser(User user) {
        if (user != null) {
            prefs.putLong(KEY_USER_ID, user.getId() != null ? user.getId() : 0L);
            prefs.put(KEY_USERNAME, user.getUsername() != null ? user.getUsername() : "");
            prefs.put(KEY_FULL_NAME, user.getFullName() != null ? user.getFullName() : "");
            prefs.put(KEY_EMAIL, user.getEmail() != null ? user.getEmail() : "");
            prefs.putLong(KEY_LAST_LOGIN, System.currentTimeMillis());

            System.out.println("[PREFS] User saved: " + user.getUsername());
        }
    }

    /**
     * Lấy thông tin user đã lưu (nếu có)
     */
    public User getSavedUser() {
        long userId = prefs.getLong(KEY_USER_ID, 0L);
        if (userId > 0) {
            User user = new User();
            user.setId(userId);
            user.setUsername(prefs.get(KEY_USERNAME, ""));
            user.setFullName(prefs.get(KEY_FULL_NAME, ""));
            user.setEmail(prefs.get(KEY_EMAIL, ""));

            System.out.println("[PREFS] User loaded: " + user.getUsername());
            return user;
        }
        return null;
    }

    /**
     * Xóa thông tin user hiện tại
     */
    public void clearCurrentUser() {
        prefs.remove(KEY_USER_ID);
        prefs.remove(KEY_USERNAME);
        prefs.remove(KEY_FULL_NAME);
        prefs.remove(KEY_EMAIL);

        System.out.println("[PREFS] User data cleared");
    }

    /**
     * Lưu trạng thái "Remember Me"
     */
    public void setRememberMe(boolean remember) {
        prefs.putBoolean(KEY_REMEMBER_ME, remember);
    }

    /**
     * Lấy trạng thái "Remember Me"
     */
    public boolean isRememberMe() {
        return prefs.getBoolean(KEY_REMEMBER_ME, false);
    }

    /**
     * Lưu username/password (chỉ nếu remember me)
     * CHÚ Ý: Đây KHÔNG an toàn, chỉ dùng để demo
     * Production nên dùng KeyStore hoặc credential manager
     */
    public void saveCredentials(String username, String password) {
        if (isRememberMe()) {
            // TODO: Encrypt password trước khi lưu
            prefs.put("saved_username", username);
            prefs.put("saved_password", password); // UNSAFE - chỉ demo
        }
    }

    /**
     * Lấy username đã lưu
     */
    public String getSavedUsername() {
        return prefs.get("saved_username", "");
    }

    /**
     * Lấy password đã lưu
     * CHÚ Ý: KHÔNG an toàn
     */
    public String getSavedPassword() {
        return prefs.get("saved_password", "");
    }

    /**
     * Xóa credentials đã lưu
     */
    public void clearCredentials() {
        prefs.remove("saved_username");
        prefs.remove("saved_password");
    }

    /**
     * Lưu setting chung
     */
    public void setSetting(String key, String value) {
        prefs.put(key, value);
    }

    /**
     * Lấy setting
     */
    public String getSetting(String key, String defaultValue) {
        return prefs.get(key, defaultValue);
    }

    /**
     * Xóa tất cả preferences
     */
    public void clearAll() {
        try {
            prefs.clear();
            System.out.println("[PREFS] All preferences cleared");
        } catch (Exception e) {
            System.err.println("[PREFS] Error clearing preferences: " + e.getMessage());
        }
    }
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}