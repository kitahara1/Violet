package com.example.violet.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.example.violet.data.AppRepository;

/**
 * Broadcast receiver that detects installed, updated, or uninstalled apps.
 */
public class PackageChangeReceiver extends BroadcastReceiver {

    public interface OnPackageChangeListener {
        void onPackageChanged(String packageName, boolean isRemoved);
    }

    private final OnPackageChangeListener listener;

    public PackageChangeReceiver(OnPackageChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (Intent.ACTION_PACKAGE_ADDED.equals(action)
                || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                || Intent.ACTION_PACKAGE_CHANGED.equals(action)
                || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {

            String packageName = intent.getData() != null ? intent.getData().getSchemeSpecificPart() : null;
            boolean isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            boolean isRemoved = Intent.ACTION_PACKAGE_REMOVED.equals(action) && !isReplacing;

            AppRepository.getInstance(context).invalidateCache();
            if (listener != null) {
                listener.onPackageChanged(packageName, isRemoved);
            }
        }
    }

    public static IntentFilter createIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        return filter;
    }
}
