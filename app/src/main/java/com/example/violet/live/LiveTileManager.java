package com.example.violet.live;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.example.violet.data.TileSize;
import com.example.violet.live.providers.BatteryLiveTileProvider;
import com.example.violet.live.providers.ClockLiveTileProvider;
import com.example.violet.live.providers.MediaLiveTileProvider;
import com.example.violet.live.providers.NotificationLiveTileProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coordinator for all Live Tile providers.
 * Manages foreground/background lifecycle to guarantee zero idle battery consumption
 * and dispatches all update notifications safely on Android's Main UI thread.
 */
public class LiveTileManager {

    private static volatile LiveTileManager instance;

    public interface LiveTileChangeListener {
        void onLiveTilesChanged();
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<LiveTileProvider> providers = new ArrayList<>();
    private final MediaLiveTileProvider mediaProvider;
    private final NotificationLiveTileProvider notificationProvider;
    private final ClockLiveTileProvider clockProvider;
    private final BatteryLiveTileProvider batteryProvider;

    private final List<LiveTileChangeListener> changeListeners = new CopyOnWriteArrayList<>();
    private boolean isForeground = false;

    private final LiveTileProvider.OnLiveTileUpdatedListener providerListener = () -> {
        if (isForeground) {
            notifyChangeListeners();
        }
    };

    public static LiveTileManager getInstance(Context context) {
        if (instance == null) {
            synchronized (LiveTileManager.class) {
                if (instance == null) {
                    instance = new LiveTileManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private LiveTileManager(Context context) {
        this.context = context;

        mediaProvider = new MediaLiveTileProvider(context);
        notificationProvider = new NotificationLiveTileProvider(context);
        clockProvider = new ClockLiveTileProvider(context);
        batteryProvider = new BatteryLiveTileProvider(context);

        providers.add(mediaProvider);
        providers.add(notificationProvider);
        providers.add(clockProvider);
        providers.add(batteryProvider);

        for (LiveTileProvider provider : providers) {
            provider.addListener(providerListener);
        }
    }

    public void addChangeListener(LiveTileChangeListener listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(LiveTileChangeListener listener) {
        if (listener != null) {
            changeListeners.remove(listener);
        }
    }

    public void resume() {
        if (!isForeground) {
            isForeground = true;
            for (LiveTileProvider provider : providers) {
                provider.startListening();
            }
            notifyChangeListeners();
        }
    }

    public void pause() {
        if (isForeground) {
            isForeground = false;
            for (LiveTileProvider provider : providers) {
                provider.stopListening();
            }
        }
    }

    public boolean isForeground() {
        return isForeground;
    }

    @Nullable
    public LiveTileData getDataForPackage(String packageName, TileSize size) {
        if (packageName == null) return null;

        // 1. Media provider (VLC, Apple Music, Spotify, etc.) takes priority to show Now Playing
        if (mediaProvider.canProvideForPackage(packageName)) {
            LiveTileData data = mediaProvider.getDataForPackage(packageName, size);
            if (data != null) return data;
        }

        // 2. Notification provider (unread alerts for messaging, email, etc.)
        if (notificationProvider.canProvideForPackage(packageName)) {
            LiveTileData data = notificationProvider.getDataForPackage(packageName, size);
            if (data != null) return data;
        }

        // 3. Clock & Calendar
        if (clockProvider.canProvideForPackage(packageName)) {
            LiveTileData data = clockProvider.getDataForPackage(packageName, size);
            if (data != null) return data;
        }

        // 4. Battery / Device
        if (batteryProvider.canProvideForPackage(packageName)) {
            LiveTileData data = batteryProvider.getDataForPackage(packageName, size);
            if (data != null) return data;
        }

        return null;
    }

    private void notifyChangeListeners() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            for (LiveTileChangeListener listener : changeListeners) {
                listener.onLiveTilesChanged();
            }
        } else {
            mainHandler.post(() -> {
                for (LiveTileChangeListener listener : changeListeners) {
                    listener.onLiveTilesChanged();
                }
            });
        }
    }
}
