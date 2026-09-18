package com.example.violet.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages user preferences such as theme mode, accent color, tile transparency, and wallpaper parallax.
 * Uses lightweight SharedPreferences with zero database overhead.
 */
public class LauncherPreferences {
    private static final String PREFS_NAME = "violet_settings";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_TILE_TRANSPARENCY = "tile_transparency";
    private static final String KEY_WALLPAPER_PARALLAX = "wallpaper_parallax";

    public static final int THEME_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    public static final int THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES;
    public static final int THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;

    public static final int DEFAULT_TRANSPARENCY = 50; // 50% opacity by default in Windows 10 Mobile
    public static final boolean DEFAULT_WALLPAPER_PARALLAX = true;

    private static volatile LauncherPreferences instance;

    private final SharedPreferences prefs;
    private final List<OnPreferencesChangedListener> listeners = new CopyOnWriteArrayList<>();

    public interface OnPreferencesChangedListener {
        void onAccentColorChanged(AccentColor newColor);
        void onThemeModeChanged(int newThemeMode);
        void onTileTransparencyChanged(int newTransparency);
        default void onWallpaperParallaxChanged(boolean enabled) {}
    }

    public static LauncherPreferences getInstance(Context context) {
        if (instance == null) {
            synchronized (LauncherPreferences.class) {
                if (instance == null) {
                    instance = new LauncherPreferences(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private LauncherPreferences(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void addListener(OnPreferencesChangedListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(OnPreferencesChangedListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public AccentColor getAccentColor() {
        String name = prefs.getString(KEY_ACCENT_COLOR, AccentColor.VIOLET.name());
        return AccentColor.fromName(name);
    }

    public void setAccentColor(AccentColor accentColor) {
        if (accentColor == null) return;
        prefs.edit().putString(KEY_ACCENT_COLOR, accentColor.name()).apply();
        for (OnPreferencesChangedListener listener : listeners) {
            listener.onAccentColorChanged(accentColor);
        }
    }

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, THEME_DARK);
    }

    public void setThemeMode(int themeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply();
        for (OnPreferencesChangedListener listener : listeners) {
            listener.onThemeModeChanged(themeMode);
        }
    }

    public int getTileTransparency() {
        return prefs.getInt(KEY_TILE_TRANSPARENCY, DEFAULT_TRANSPARENCY);
    }

    public void setTileTransparency(int transparencyPercent) {
        int clamped = Math.max(0, Math.min(100, transparencyPercent));
        prefs.edit().putInt(KEY_TILE_TRANSPARENCY, clamped).apply();
        for (OnPreferencesChangedListener listener : listeners) {
            listener.onTileTransparencyChanged(clamped);
        }
    }

    public boolean isWallpaperParallaxEnabled() {
        return prefs.getBoolean(KEY_WALLPAPER_PARALLAX, DEFAULT_WALLPAPER_PARALLAX);
    }

    public void setWallpaperParallaxEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_WALLPAPER_PARALLAX, enabled).apply();
        for (OnPreferencesChangedListener listener : listeners) {
            listener.onWallpaperParallaxChanged(enabled);
        }
    }
}
