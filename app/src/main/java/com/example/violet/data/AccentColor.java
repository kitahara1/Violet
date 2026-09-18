package com.example.violet.data;

import android.graphics.Color;

/**
 * Authentic Windows Phone / Windows 10 Mobile Accent Colors.
 */
public enum AccentColor {
    VIOLET("Violet", "#76359D"),
    COBALT("Cobalt", "#0050EF"),
    CRIMSON("Crimson", "#E51400"),
    EMERALD("Emerald", "#008A00"),
    MANGO("Mango", "#F09609"),
    CYAN("Cyan", "#1BA1E2"),
    MAGENTA("Magenta", "#D80073"),
    AMBER("Amber", "#F0A30A"),
    LIME("Lime", "#A4C400"),
    TEAL("Teal", "#00ABA9");

    private final String displayName;
    private final String hexColor;
    private final int colorInt;

    AccentColor(String displayName, String hexColor) {
        this.displayName = displayName;
        this.hexColor = hexColor;
        this.colorInt = Color.parseColor(hexColor);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHexColor() {
        return hexColor;
    }

    public int getColorInt() {
        return colorInt;
    }

    public static AccentColor fromName(String name) {
        if (name == null) return VIOLET;
        try {
            return AccentColor.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return VIOLET;
        }
    }
}
