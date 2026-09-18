package com.example.violet.data;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import com.example.violet.ui.apps.AppItem;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Discovers and caches installed launchable applications.
 * Performs background discovery and groups applications by alphabet header.
 */
public class AppRepository {
    private static volatile AppRepository instance;

    private final Context appContext;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private List<AppInfo> cachedRawApps = null;
    private List<AppItem> cachedAppItems = null;
    private boolean isLoading = false;

    public interface AppLoadCallback {
        void onAppsLoaded(List<AppItem> items);
    }

    public static AppRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (AppRepository.class) {
                if (instance == null) {
                    instance = new AppRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private AppRepository(Context context) {
        this.appContext = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Loads installed apps. If already cached and not forced, returns cached items immediately.
     */
    public void loadApps(boolean forceReload, AppLoadCallback callback) {
        if (!forceReload && cachedAppItems != null) {
            if (callback != null) {
                callback.onAppsLoaded(new ArrayList<>(cachedAppItems));
            }
            return;
        }

        if (isLoading) {
            return;
        }
        isLoading = true;

        executor.execute(() -> {
            List<AppInfo> rawApps = queryLaunchableApps();
            List<AppItem> groupedItems = buildGroupedAppItems(rawApps);

            synchronized (AppRepository.this) {
                cachedRawApps = rawApps;
                cachedAppItems = groupedItems;
                isLoading = false;
            }

            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onAppsLoaded(new ArrayList<>(groupedItems));
                }
            });
        });
    }

    /**
     * Look up an AppInfo from cache or resolve on-demand.
     */
    public synchronized AppInfo findApp(String packageName, String activityName) {
        if (packageName == null) return null;

        if (cachedRawApps != null) {
            for (AppInfo info : cachedRawApps) {
                if (info.getPackageName().equals(packageName)) {
                    if (activityName == null || activityName.isEmpty() || info.getActivityName().equals(activityName)) {
                        return info;
                    }
                }
            }
        }

        // Resolve on-demand if not in cache
        try {
            PackageManager pm = appContext.getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(packageName);

            List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            if (!list.isEmpty()) {
                ResolveInfo ri = list.get(0);
                if (activityName != null && !activityName.isEmpty()) {
                    for (ResolveInfo item : list) {
                        if (item.activityInfo != null && activityName.equals(item.activityInfo.name)) {
                            ri = item;
                            break;
                        }
                    }
                }
                if (ri.activityInfo != null) {
                    CharSequence label = ri.loadLabel(pm);
                    Drawable icon = ri.loadIcon(pm);
                    String lbl = label != null ? label.toString() : ri.activityInfo.name;
                    return new AppInfo(ri.activityInfo.packageName, ri.activityInfo.name, lbl, icon, resolveSectionHeader(lbl));
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Invalidate cached apps when a package is installed, updated, or removed.
     */
    public void invalidateCache() {
        synchronized (this) {
            cachedRawApps = null;
            cachedAppItems = null;
        }
    }

    private List<AppInfo> queryLaunchableApps() {
        PackageManager pm = appContext.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> activities = pm.queryIntentActivities(mainIntent, 0);
        List<AppInfo> apps = new ArrayList<>();
        String selfPackage = appContext.getPackageName();

        for (ResolveInfo ri : activities) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (selfPackage.equals(pkg)) {
                continue;
            }

            CharSequence labelCs = ri.loadLabel(pm);
            String label = (labelCs != null) ? labelCs.toString().trim() : ri.activityInfo.name;
            if (label.isEmpty()) {
                label = ri.activityInfo.name;
            }

            Drawable icon = ri.loadIcon(pm);
            String header = resolveSectionHeader(label);

            apps.add(new AppInfo(pkg, ri.activityInfo.name, label, icon, header));
        }

        // Sort alphabetically using Locale Collator
        Collator collator = Collator.getInstance(Locale.getDefault());
        collator.setStrength(Collator.PRIMARY);
        Collections.sort(apps, (a, b) -> collator.compare(a.getLabel(), b.getLabel()));

        return apps;
    }

    private String resolveSectionHeader(String label) {
        if (label == null || label.isEmpty()) {
            return "#";
        }
        char firstChar = label.charAt(0);
        if (Character.isLetter(firstChar)) {
            return String.valueOf(Character.toUpperCase(firstChar));
        }
        return "#";
    }

    private List<AppItem> buildGroupedAppItems(List<AppInfo> apps) {
        List<AppItem> items = new ArrayList<>();
        String currentSection = null;

        for (AppInfo app : apps) {
            String section = app.getSectionHeader();
            if (!section.equals(currentSection)) {
                currentSection = section;
                items.add(AppItem.createHeader(currentSection));
            }
            items.add(AppItem.createApp(app));
        }

        return items;
    }
}
