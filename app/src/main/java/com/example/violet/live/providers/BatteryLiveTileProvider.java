package com.example.violet.live.providers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.annotation.Nullable;

import com.example.violet.data.TileSize;
import com.example.violet.live.LiveTileData;
import com.example.violet.live.LiveTileProvider;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Event-driven battery state Live Tile provider.
 * Reads battery level and charging status via ACTION_BATTERY_CHANGED broadcast.
 */
public class BatteryLiveTileProvider implements LiveTileProvider {

    private final Context context;
    private final List<OnLiveTileUpdatedListener> listeners = new CopyOnWriteArrayList<>();
    private boolean isListening = false;

    private int lastLevel = -1;
    private boolean lastIsCharging = false;

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            int percent = (level >= 0 && scale > 0) ? (int) ((level / (float) scale) * 100) : -1;
            boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL);

            if (percent != lastLevel || isCharging != lastIsCharging) {
                lastLevel = percent;
                lastIsCharging = isCharging;
                notifyUpdated();
            }
        }
    };

    public BatteryLiveTileProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void startListening() {
        if (!isListening) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent sticky = context.registerReceiver(batteryReceiver, filter);
            if (sticky != null) {
                int level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = sticky.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                lastLevel = (level >= 0 && scale > 0) ? (int) ((level / (float) scale) * 100) : -1;
                lastIsCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL);
            }
            isListening = true;
        }
    }

    @Override
    public void stopListening() {
        if (isListening) {
            try {
                context.unregisterReceiver(batteryReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            isListening = false;
        }
    }

    @Override
    public boolean canProvideForPackage(String packageName) {
        if (packageName == null) return false;
        String lower = packageName.toLowerCase(Locale.ROOT);
        return lower.contains("settings") || lower.contains("power") || lower.contains("battery");
    }

    @Nullable
    @Override
    public LiveTileData getDataForPackage(String packageName, TileSize size) {
        if (!canProvideForPackage(packageName) || lastLevel < 0) {
            return null;
        }

        String primary = lastLevel + "%";
        String subtitle = lastIsCharging ? "Charging" : "On Battery";

        return new LiveTileData(
                "POWER",
                primary,
                subtitle,
                0,
                true
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
