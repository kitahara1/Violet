package com.example.violet.live.providers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.format.DateFormat;

import androidx.annotation.Nullable;

import com.example.violet.data.TileSize;
import com.example.violet.live.LiveTileData;
import com.example.violet.live.LiveTileProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Broadcast-driven Clock and Calendar Live Tile Provider.
 * Updates on Android system minute tick (ACTION_TIME_TICK) with zero battery impact.
 * Flips dynamically to show time, date, and calendar information.
 */
public class ClockLiveTileProvider implements LiveTileProvider {

    private final Context context;
    private final List<OnLiveTileUpdatedListener> listeners = new CopyOnWriteArrayList<>();
    private boolean isListening = false;

    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            notifyUpdated();
        }
    };

    public ClockLiveTileProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void startListening() {
        if (!isListening) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_DATE_CHANGED);
            context.registerReceiver(timeReceiver, filter);
            isListening = true;
        }
    }

    @Override
    public void stopListening() {
        if (isListening) {
            try {
                context.unregisterReceiver(timeReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            isListening = false;
        }
    }

    @Override
    public boolean canProvideForPackage(String packageName) {
        if (packageName == null) return false;
        String lower = packageName.toLowerCase(Locale.ROOT);
        return lower.contains("clock") || lower.contains("alarm") || lower.contains("calendar");
    }

    @Nullable
    @Override
    public LiveTileData getDataForPackage(String packageName, TileSize size) {
        if (!canProvideForPackage(packageName)) return null;

        String lower = packageName.toLowerCase(Locale.ROOT);
        Date now = new Date();

        if (lower.contains("calendar")) {
            SimpleDateFormat dayNumFormat = new SimpleDateFormat("d", Locale.getDefault());
            SimpleDateFormat dayNameFormat = new SimpleDateFormat("EEEE, MMMM", Locale.getDefault());

            String dayNum = dayNumFormat.format(now);
            String dayName = dayNameFormat.format(now);

            return new LiveTileData(
                    "CALENDAR",
                    dayNum,
                    dayName,
                    0,
                    true
            );
        } else {
            // Clock
            boolean is24Hour = DateFormat.is24HourFormat(context);
            SimpleDateFormat timeFormat = new SimpleDateFormat(is24Hour ? "HH:mm" : "h:mm a", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());

            String timeStr = timeFormat.format(now);
            String dateStr = dateFormat.format(now);

            return new LiveTileData(
                    "CLOCK",
                    timeStr,
                    dateStr,
                    0,
                    true
            );
        }
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
