package com.example.violet.ui.apps;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.violet.R;
import com.example.violet.data.AccentColor;
import com.example.violet.data.AppInfo;
import com.example.violet.data.HiddenAppsManager;
import com.example.violet.data.LauncherPreferences;
import com.example.violet.data.TileRepository;
import com.example.violet.data.TileSize;
import com.example.violet.ui.security.PinEntryDialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * RecyclerView Adapter displaying Windows Phone–style alphabetical section headers and launchable apps.
 * Supports real-time search filtering, dynamic accent colors, alphabet jump navigation, uninstallation,
 * and PIN-protected app hiding.
 */
public class AppListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnAppClickListener {
        void onAppClick(View view, AppInfo appInfo);
    }

    public interface OnHeaderClickListener {
        void onHeaderClick(String letter, Set<String> activeLetters);
    }

    private final Context context;
    private final HiddenAppsManager hiddenAppsManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final List<AppItem> rawItems = new ArrayList<>();
    private final List<AppItem> originalItems = new ArrayList<>();
    private final List<AppItem> displayItems = new ArrayList<>();

    private String currentQuery = "";
    private final OnAppClickListener onAppClickListener;
    private OnHeaderClickListener onHeaderClickListener;

    public AppListAdapter(Context context, OnAppClickListener onAppClickListener) {
        this.context = context.getApplicationContext();
        this.onAppClickListener = onAppClickListener;
        this.hiddenAppsManager = HiddenAppsManager.getInstance(this.context);

        this.hiddenAppsManager.addListener(hidden -> {
            mainHandler.post(this::rebuildItems);
        });
    }

    public void setOnHeaderClickListener(OnHeaderClickListener onHeaderClickListener) {
        this.onHeaderClickListener = onHeaderClickListener;
    }

    public void setItems(List<AppItem> newItems) {
        rawItems.clear();
        if (newItems != null) {
            rawItems.addAll(newItems);
        }
        rebuildItems();
    }

    public void filter(String query) {
        this.currentQuery = query != null ? query : "";
        rebuildItems();
    }

    private void rebuildItems() {
        originalItems.clear();
        displayItems.clear();

        String currentSection = null;
        for (AppItem item : rawItems) {
            if (item.getType() == AppItem.TYPE_APP && item.getAppInfo() != null) {
                AppInfo app = item.getAppInfo();
                if (hiddenAppsManager.isAppHidden(app.getPackageName())) {
                    continue;
                }
                String section = app.getSectionHeader();
                if (!section.equals(currentSection)) {
                    currentSection = section;
                    originalItems.add(AppItem.createHeader(currentSection));
                }
                originalItems.add(item);
            }
        }

        if (currentQuery.trim().isEmpty()) {
            displayItems.addAll(originalItems);
        } else {
            String lowerQuery = currentQuery.trim().toLowerCase(Locale.getDefault());
            String filterSection = null;
            for (AppItem item : originalItems) {
                if (item.getType() == AppItem.TYPE_APP && item.getAppInfo() != null) {
                    if (item.getAppInfo().getLabel().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                        String section = item.getAppInfo().getSectionHeader();
                        if (!section.equals(filterSection)) {
                            filterSection = section;
                            displayItems.add(AppItem.createHeader(filterSection));
                        }
                        displayItems.add(item);
                    }
                }
            }
        }
        notifyDataSetChanged();
    }

    public Set<String> getActiveLetters() {
        Set<String> letters = new HashSet<>();
        for (AppItem item : originalItems) {
            if (item.getType() == AppItem.TYPE_HEADER && item.getHeaderTitle() != null) {
                letters.add(item.getHeaderTitle());
            }
        }
        return letters;
    }

    public int findHeaderPosition(String letter) {
        for (int i = 0; i < displayItems.size(); i++) {
            AppItem item = displayItems.get(i);
            if (item.getType() == AppItem.TYPE_HEADER && letter.equalsIgnoreCase(item.getHeaderTitle())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == AppItem.TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_app_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_app_list, parent, false);
            return new AppViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AppItem item = displayItems.get(position);
        AccentColor accentColor = LauncherPreferences.getInstance(holder.itemView.getContext()).getAccentColor();

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item.getHeaderTitle(), accentColor, (letter) -> {
                if (onHeaderClickListener != null) {
                    onHeaderClickListener.onHeaderClick(letter, getActiveLetters());
                }
            });
        } else if (holder instanceof AppViewHolder) {
            ((AppViewHolder) holder).bind(item.getAppInfo(), onAppClickListener, hiddenAppsManager);
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvLetter;

        interface OnHeaderClickInternal {
            void onClick(String letter);
        }

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLetter = itemView.findViewById(R.id.tv_header_letter);
        }

        void bind(String letter, AccentColor accentColor, OnHeaderClickInternal listener) {
            tvLetter.setText(letter);
            tvLetter.setContentDescription("Section " + letter);

            if (accentColor != null && tvLetter.getBackground() != null) {
                tvLetter.getBackground().mutate().setColorFilter(accentColor.getColorInt(), PorterDuff.Mode.SRC_IN);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(letter);
                }
            });
        }
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvName;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_app_icon);
            tvName = itemView.findViewById(R.id.tv_app_name);
        }

        void bind(AppInfo app, OnAppClickListener listener, HiddenAppsManager hiddenAppsManager) {
            Context context = itemView.getContext();
            tvName.setText(app.getLabel());
            ivIcon.setImageDrawable(app.getIcon());
            itemView.setContentDescription(app.getLabel());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAppClick(v, app);
                } else {
                    launchApp(context, app);
                }
            });

            itemView.setOnLongClickListener(v -> {
                showAppOptionsDialog(context, app, hiddenAppsManager);
                return true;
            });
        }

        private void showAppOptionsDialog(Context context, AppInfo app, HiddenAppsManager hiddenAppsManager) {
            TileRepository tileRepo = TileRepository.getInstance(context);
            boolean isPinned = tileRepo.isAppPinned(app.getPackageName(), app.getActivityName());

            boolean isSystemApp = false;
            try {
                ApplicationInfo ai = context.getPackageManager().getApplicationInfo(app.getPackageName(), 0);
                isSystemApp = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            } catch (PackageManager.NameNotFoundException ignored) {
            }

            List<String> options = new ArrayList<>();
            options.add(isPinned ? context.getString(R.string.unpin_from_start) : context.getString(R.string.pin_to_start));
            if (!isSystemApp) {
                options.add(context.getString(R.string.uninstall));
            }
            options.add(context.getString(R.string.hide_app));
            options.add(context.getString(R.string.app_info));

            final int unpinIndex = 0;
            final int uninstallIndex = !isSystemApp ? 1 : -1;
            final int hideIndex = !isSystemApp ? 2 : 1;
            final int infoIndex = options.size() - 1;

            new AlertDialog.Builder(context)
                    .setTitle(app.getLabel())
                    .setItems(options.toArray(new String[0]), (dialog, which) -> {
                        if (which == unpinIndex) {
                            if (isPinned) {
                                for (com.example.violet.data.Tile t : tileRepo.getTiles()) {
                                    if (t.getPackageName().equals(app.getPackageName())) {
                                        tileRepo.removeTile(t.getId());
                                        break;
                                    }
                                }
                                Toast.makeText(context, R.string.tile_unpinned, Toast.LENGTH_SHORT).show();
                            } else {
                                tileRepo.pinApp(app, TileSize.WIDE);
                                Toast.makeText(context, R.string.tile_pinned, Toast.LENGTH_SHORT).show();
                            }
                        } else if (which == uninstallIndex) {
                            uninstallApp(context, app.getPackageName());
                        } else if (which == hideIndex) {
                            promptHideApp(context, app, hiddenAppsManager);
                        } else if (which == infoIndex) {
                            openAppDetails(context, app.getPackageName());
                        }
                    })
                    .show();
        }

        private void promptHideApp(Context context, AppInfo app, HiddenAppsManager hiddenAppsManager) {
            if (!hiddenAppsManager.isPinConfigured()) {
                PinEntryDialog pinDialog = new PinEntryDialog(context, PinEntryDialog.Mode.CREATE, () -> {
                    hiddenAppsManager.hideApp(app.getPackageName());
                    Toast.makeText(context, R.string.app_hidden, Toast.LENGTH_SHORT).show();
                });
                pinDialog.show();
            } else {
                hiddenAppsManager.hideApp(app.getPackageName());
                Toast.makeText(context, R.string.app_hidden, Toast.LENGTH_SHORT).show();
            }
        }

        private void uninstallApp(Context context, String packageName) {
            try {
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignored) {
            }
        }

        private void openAppDetails(Context context, String packageName) {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignored) {
            }
        }

        private void launchApp(Context context, AppInfo app) {
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setClassName(app.getPackageName(), app.getActivityName());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(intent);
            } catch (ActivityNotFoundException | SecurityException e) {
                Toast.makeText(context, "Unable to launch " + app.getLabel(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
