package com.example.violet.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages hidden applications and 4-digit PIN security using SHA-256 hashing.
 * Persists data strictly via lightweight private SharedPreferences.
 */
public class HiddenAppsManager {

    private static final String PREFS_NAME = "violet_hidden_apps";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_PIN_SALT = "pin_salt";
    private static final String KEY_HIDDEN_PACKAGES = "hidden_packages";

    private static volatile HiddenAppsManager instance;

    private final SharedPreferences prefs;
    private final List<OnHiddenAppsChangedListener> listeners = new CopyOnWriteArrayList<>();

    public interface OnHiddenAppsChangedListener {
        void onHiddenAppsChanged(Set<String> hiddenPackages);
    }

    public static HiddenAppsManager getInstance(Context context) {
        if (instance == null) {
            synchronized (HiddenAppsManager.class) {
                if (instance == null) {
                    instance = new HiddenAppsManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private HiddenAppsManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ensureSaltExists();
    }

    private void ensureSaltExists() {
        if (!prefs.contains(KEY_PIN_SALT)) {
            String salt = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_PIN_SALT, salt).apply();
        }
    }

    private String getSalt() {
        return prefs.getString(KEY_PIN_SALT, "violet_salt_default");
    }

    private String hashPin(String pin, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = salt + pin;
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf((salt + pin).hashCode());
        }
    }

    public boolean isPinConfigured() {
        return prefs.contains(KEY_PIN_HASH);
    }

    public boolean verifyPin(String pin) {
        if (!isPinConfigured() || pin == null) {
            return false;
        }
        String expectedHash = prefs.getString(KEY_PIN_HASH, "");
        String actualHash = hashPin(pin, getSalt());
        return expectedHash.equals(actualHash);
    }

    public void setupPin(String pin) {
        if (pin == null || pin.length() != 4) return;
        String hash = hashPin(pin, getSalt());
        prefs.edit().putString(KEY_PIN_HASH, hash).apply();
    }

    public void clearPin() {
        prefs.edit().remove(KEY_PIN_HASH).apply();
    }

    public synchronized Set<String> getHiddenPackages() {
        Set<String> set = prefs.getStringSet(KEY_HIDDEN_PACKAGES, null);
        return set != null ? new HashSet<>(set) : new HashSet<>();
    }

    public synchronized boolean isAppHidden(String packageName) {
        if (packageName == null) return false;
        Set<String> hidden = getHiddenPackages();
        return hidden.contains(packageName);
    }

    public synchronized void hideApp(String packageName) {
        if (packageName == null) return;
        Set<String> hidden = getHiddenPackages();
        if (hidden.add(packageName)) {
            prefs.edit().putStringSet(KEY_HIDDEN_PACKAGES, hidden).apply();
            notifyListeners(hidden);
        }
    }

    public synchronized void unhideApp(String packageName) {
        if (packageName == null) return;
        Set<String> hidden = getHiddenPackages();
        if (hidden.remove(packageName)) {
            prefs.edit().putStringSet(KEY_HIDDEN_PACKAGES, hidden).apply();
            notifyListeners(hidden);
        }
    }

    public void addListener(OnHiddenAppsChangedListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(OnHiddenAppsChangedListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners(Set<String> hidden) {
        for (OnHiddenAppsChangedListener listener : listeners) {
            listener.onHiddenAppsChanged(hidden);
        }
    }
}
