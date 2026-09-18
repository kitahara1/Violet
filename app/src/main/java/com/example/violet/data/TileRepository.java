package com.example.violet.data;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles persistent storage and management of Start screen tiles using lightweight SharedPreferences.
 */
public class TileRepository {
    private static final String PREFS_NAME = "violet_tiles";
    private static final String KEY_TILES_JSON = "tiles_json";
    private static final String KEY_DEFAULTS_INITIALIZED = "defaults_initialized";

    private static volatile TileRepository instance;

    private final Context appContext;
    private final SharedPreferences prefs;
    private final List<Tile> tilesCache = new ArrayList<>();
    private final List<OnTilesChangedListener> listeners = new CopyOnWriteArrayList<>();

    public interface OnTilesChangedListener {
        void onTilesChanged(List<Tile> tiles);
    }

    public static TileRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (TileRepository.class) {
                if (instance == null) {
                    instance = new TileRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private TileRepository(Context context) {
        this.appContext = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadFromPrefs();
    }

    public synchronized List<Tile> getTiles() {
        return new ArrayList<>(tilesCache);
    }

    public void addListener(OnTilesChangedListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(OnTilesChangedListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public synchronized boolean isAppPinned(String packageName, String activityName) {
        for (Tile tile : tilesCache) {
            if (tile.getPackageName().equals(packageName) &&
                    (activityName == null || tile.getActivityName().equals(activityName))) {
                return true;
            }
        }
        return false;
    }

    public synchronized void pinApp(AppInfo app, TileSize size) {
        if (isAppPinned(app.getPackageName(), app.getActivityName())) {
            return;
        }

        Tile tile = new Tile(
                app.getPackageName(),
                app.getActivityName(),
                app.getLabel(),
                size != null ? size : TileSize.WIDE
        );
        tile.setOrderIndex(tilesCache.size());
        tilesCache.add(tile);
        saveToPrefs();
        notifyListeners();
    }

    public synchronized void removeTile(String tileId) {
        boolean removed = false;
        for (int i = 0; i < tilesCache.size(); i++) {
            if (tilesCache.get(i).getId().equals(tileId)) {
                tilesCache.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            reindexTiles();
            saveToPrefs();
            notifyListeners();
        }
    }

    public synchronized void removeTilesByPackage(String packageName) {
        if (packageName == null) return;
        boolean removed = false;
        for (int i = tilesCache.size() - 1; i >= 0; i--) {
            if (packageName.equals(tilesCache.get(i).getPackageName())) {
                tilesCache.remove(i);
                removed = true;
            }
        }
        if (removed) {
            reindexTiles();
            saveToPrefs();
            notifyListeners();
        }
    }

    public synchronized void updateTileSize(String tileId, TileSize newSize) {
        for (Tile tile : tilesCache) {
            if (tile.getId().equals(tileId)) {
                tile.setSize(newSize);
                saveToPrefs();
                notifyListeners();
                break;
            }
        }
    }

    public synchronized void updateTileType(String tileId, TileType newType) {
        for (Tile tile : tilesCache) {
            if (tile.getId().equals(tileId)) {
                tile.setType(newType);
                saveToPrefs();
                notifyListeners();
                break;
            }
        }
    }

    public synchronized void reorderTiles(List<Tile> newOrder) {
        tilesCache.clear();
        if (newOrder != null) {
            tilesCache.addAll(newOrder);
        }
        reindexTiles();
        saveToPrefs();
        notifyListeners();
    }

    /**
     * Initializes a balanced default Start screen on the first launch.
     */
    public synchronized void initializeDefaultTilesIfNeeded() {
        if (prefs.getBoolean(KEY_DEFAULTS_INITIALIZED, false)) {
            return;
        }

        PackageManager pm = appContext.getPackageManager();
        List<Tile> defaults = new ArrayList<>();

        // 1. Phone / Dialer (Wide)
        Intent dialerIntent = new Intent(Intent.ACTION_DIAL);
        addDefaultTileIfResolved(pm, dialerIntent, defaults, TileSize.WIDE);

        // 2. Messages (Small)
        Intent smsIntent = new Intent(Intent.ACTION_MAIN);
        smsIntent.addCategory(Intent.CATEGORY_APP_MESSAGING);
        addDefaultTileIfResolved(pm, smsIntent, defaults, TileSize.SMALL);

        // 3. Camera (Small)
        Intent cameraIntent = new Intent("android.media.action.IMAGE_CAPTURE");
        addDefaultTileIfResolved(pm, cameraIntent, defaults, TileSize.SMALL);

        // 4. Browser (Wide)
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"));
        addDefaultTileIfResolved(pm, browserIntent, defaults, TileSize.WIDE);

        // 5. Gallery / Photos (Small)
        Intent galleryIntent = new Intent(Intent.ACTION_MAIN);
        galleryIntent.addCategory(Intent.CATEGORY_APP_GALLERY);
        addDefaultTileIfResolved(pm, galleryIntent, defaults, TileSize.SMALL);

        // 6. Settings (Small)
        Intent settingsIntent = new Intent(android.provider.Settings.ACTION_SETTINGS);
        addDefaultTileIfResolved(pm, settingsIntent, defaults, TileSize.SMALL);

        if (!defaults.isEmpty()) {
            tilesCache.clear();
            tilesCache.addAll(defaults);
            reindexTiles();
            saveToPrefs();
        }

        prefs.edit().putBoolean(KEY_DEFAULTS_INITIALIZED, true).apply();
        notifyListeners();
    }

    private void addDefaultTileIfResolved(PackageManager pm, Intent intent, List<Tile> list, TileSize size) {
        ResolveInfo ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (ri != null && ri.activityInfo != null) {
            String pkg = ri.activityInfo.packageName;
            String cls = ri.activityInfo.name;
            CharSequence label = ri.loadLabel(pm);
            String labelStr = label != null ? label.toString() : pkg;

            // Avoid duplicates
            for (Tile t : list) {
                if (t.getPackageName().equals(pkg)) return;
            }

            list.add(new Tile(pkg, cls, labelStr, size));
        }
    }

    private void reindexTiles() {
        for (int i = 0; i < tilesCache.size(); i++) {
            tilesCache.get(i).setOrderIndex(i);
        }
    }

    private void loadFromPrefs() {
        String jsonStr = prefs.getString(KEY_TILES_JSON, null);
        tilesCache.clear();
        if (jsonStr != null && !jsonStr.trim().isEmpty()) {
            try {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    Tile tile = Tile.fromJsonObject(array.optJSONObject(i));
                    if (tile != null) {
                        tilesCache.add(tile);
                    }
                }
            } catch (JSONException ignored) {
            }
        }
    }

    private void saveToPrefs() {
        JSONArray array = new JSONArray();
        for (Tile tile : tilesCache) {
            array.put(tile.toJsonObject());
        }
        prefs.edit().putString(KEY_TILES_JSON, array.toString()).apply();
    }

    private void notifyListeners() {
        List<Tile> current = getTiles();
        for (OnTilesChangedListener listener : listeners) {
            listener.onTilesChanged(current);
        }
    }
}
