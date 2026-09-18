package com.example.violet.live.providers;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.violet.data.TileSize;
import com.example.violet.live.LiveTileData;
import com.example.violet.live.LiveTileProvider;
import com.example.violet.live.VioletNotificationService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Live Tile provider extracting unread badge counts and notification snippets
 * via the event-driven VioletNotificationService.
 */
public class NotificationLiveTileProvider implements LiveTileProvider {

    private final Context context;
    private final List<OnLiveTileUpdatedListener> listeners = new CopyOnWriteArrayList<>();
    private final VioletNotificationService.NotificationStateListener stateListener = this::notifyUpdated;
    private boolean isListening = false;

    public NotificationLiveTileProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void startListening() {
        if (!isListening) {
            VioletNotificationService.addListener(stateListener);
            isListening = true;
        }
    }

    @Override
    public void stopListening() {
        if (isListening) {
            VioletNotificationService.removeListener(stateListener);
            isListening = false;
        }
    }

    @Override
    public boolean canProvideForPackage(String packageName) {
        if (packageName == null) return false;
        return VioletNotificationService.getNotificationCount(packageName) > 0;
    }

    @Nullable
    @Override
    public LiveTileData getDataForPackage(String packageName, TileSize size) {
        if (packageName == null) return null;

        int count = VioletNotificationService.getNotificationCount(packageName);
        if (count <= 0) {
            return null;
        }

        String snippet = VioletNotificationService.getLatestSnippet(packageName);
        String subtitle;
        if (!snippet.isEmpty()) {
            subtitle = snippet;
        } else {
            subtitle = count == 1 ? "1 new notification" : count + " new notifications";
        }

        return new LiveTileData(
                "NOTIFICATIONS",
                String.valueOf(count),
                subtitle,
                count,
                true // Flip to show message / notification summary
        );
    }

    @Override
    public void addListener(OnLiveTileUpdatedListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(OnLiveTileUpdatedListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifyUpdated() {
        for (OnLiveTileUpdatedListener listener : listeners) {
            listener.onLiveTileUpdated();
        }
    }
}
