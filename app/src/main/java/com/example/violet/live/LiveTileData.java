package com.example.violet.live;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Immutable data model representing live content displayed on a tile.
 * Used for both the front badge count and the back face flip content.
 */
public class LiveTileData {

    private final String header;        // e.g. "MESSAGES", "CLOCK", "NOW PLAYING", "BATTERY"
    private final String primaryText;   // e.g. "3", "22:41", "Starboy", "85%"
    private final String subtitle;      // e.g. "3 unread", "Friday, Sep 18", "Charging"
    private final int badgeCount;       // e.g. 3 (shown on front face)
    private final boolean shouldFlip;   // true if this tile periodically flips between front and back

    public LiveTileData(@Nullable String header,
                        @Nullable String primaryText,
                        @Nullable String subtitle,
                        int badgeCount,
                        boolean shouldFlip) {
        this.header = header != null ? header : "";
        this.primaryText = primaryText != null ? primaryText : "";
        this.subtitle = subtitle != null ? subtitle : "";
        this.badgeCount = Math.max(0, badgeCount);
        this.shouldFlip = shouldFlip;
    }

    @NonNull
    public String getHeader() {
        return header;
    }

    @NonNull
    public String getPrimaryText() {
        return primaryText;
    }

    @NonNull
    public String getSubtitle() {
        return subtitle;
    }

    public int getBadgeCount() {
        return badgeCount;
    }

    public boolean shouldFlip() {
        return shouldFlip;
    }

    public boolean hasContent() {
        return badgeCount > 0 || !primaryText.isEmpty() || !subtitle.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String header = "";
        private String primaryText = "";
        private String subtitle = "";
        private int badgeCount = 0;
        private boolean shouldFlip = false;

        public Builder setHeader(String header) {
            this.header = header;
            return this;
        }

        public Builder setPrimaryText(String primaryText) {
            this.primaryText = primaryText;
            return this;
        }

        public Builder setSubtitle(String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder setBadgeCount(int badgeCount) {
            this.badgeCount = badgeCount;
            return this;
        }

        public Builder setShouldFlip(boolean shouldFlip) {
            this.shouldFlip = shouldFlip;
            return this;
        }

        public LiveTileData build() {
            return new LiveTileData(header, primaryText, subtitle, badgeCount, shouldFlip);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiveTileData that = (LiveTileData) o;
        return badgeCount == that.badgeCount &&
                shouldFlip == that.shouldFlip &&
                Objects.equals(header, that.header) &&
                Objects.equals(primaryText, that.primaryText) &&
                Objects.equals(subtitle, that.subtitle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(header, primaryText, subtitle, badgeCount, shouldFlip);
    }
}
