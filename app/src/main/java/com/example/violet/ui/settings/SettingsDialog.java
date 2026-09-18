package com.example.violet.ui.settings;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.violet.R;
import com.example.violet.data.AccentColor;
import com.example.violet.data.HiddenAppsManager;
import com.example.violet.data.LauncherPreferences;
import com.example.violet.data.WallpaperManager;
import com.example.violet.live.VioletNotificationService;
import com.example.violet.ui.apps.HiddenAppsDialog;
import com.example.violet.ui.security.PinEntryDialog;

/**
 * Windows Phone Metro–styled settings dialog allowing real-time theme, accent color,
 * Start background & tile transparency, parallax scroll toggle, Live Tile notification access,
 * default launcher selection, and PIN-protected hidden applications management.
 */
public class SettingsDialog extends Dialog {

    public interface OnWallpaperActionListener {
        void onChooseWallpaperRequested();
    }

    private final LauncherPreferences preferences;
    private final OnWallpaperActionListener wallpaperListener;

    public SettingsDialog(@NonNull Context context) {
        this(context, null);
    }

    public SettingsDialog(@NonNull Context context, OnWallpaperActionListener wallpaperListener) {
        super(context);
        this.preferences = LauncherPreferences.getInstance(context);
        this.wallpaperListener = wallpaperListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_settings);

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        setupThemeSelection();
        setupAccentSelection();
        setupBackgroundSection();
        setupLiveTilesSection();
        setupHiddenAppsSection();
        setupDefaultHomeSection();
    }

    private void setupThemeSelection() {
        RadioGroup rgTheme = findViewById(R.id.rg_theme);
        RadioButton rbDark = findViewById(R.id.rb_theme_dark);
        RadioButton rbLight = findViewById(R.id.rb_theme_light);
        RadioButton rbSystem = findViewById(R.id.rb_theme_system);

        int currentTheme = preferences.getThemeMode();
        if (currentTheme == LauncherPreferences.THEME_DARK) {
            rbDark.setChecked(true);
        } else if (currentTheme == LauncherPreferences.THEME_LIGHT) {
            rbLight.setChecked(true);
        } else {
            rbSystem.setChecked(true);
        }

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int targetMode;
            if (checkedId == R.id.rb_theme_dark) {
                targetMode = LauncherPreferences.THEME_DARK;
            } else if (checkedId == R.id.rb_theme_light) {
                targetMode = LauncherPreferences.THEME_LIGHT;
            } else {
                targetMode = LauncherPreferences.THEME_SYSTEM;
            }

            preferences.setThemeMode(targetMode);
            AppCompatDelegate.setDefaultNightMode(targetMode);
        });
    }

    private void setupAccentSelection() {
        RecyclerView rvAccents = findViewById(R.id.rv_accent_colors);
        rvAccents.setLayoutManager(new GridLayoutManager(getContext(), 5));

        AccentColorAdapter adapter = new AccentColorAdapter(AccentColor.values(), preferences.getAccentColor(), newColor -> {
            preferences.setAccentColor(newColor);
        });
        rvAccents.setAdapter(adapter);
    }

    private void setupBackgroundSection() {
        View btnChoose = findViewById(R.id.btn_choose_wallpaper);
        View btnRemove = findViewById(R.id.btn_remove_wallpaper);
        View llTransparency = findViewById(R.id.ll_transparency_container);
        View llParallax = findViewById(R.id.ll_parallax_container);
        TextView tvTransparencyLabel = findViewById(R.id.tv_transparency_label);
        SeekBar sbTransparency = findViewById(R.id.sb_tile_transparency);
        SwitchCompat switchParallax = findViewById(R.id.switch_parallax);

        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getContext());

        Runnable updateVisibility = () -> {
            boolean hasWallpaper = wallpaperManager.hasWallpaper();
            if (btnRemove != null) {
                btnRemove.setVisibility(hasWallpaper ? View.VISIBLE : View.GONE);
            }
            if (llTransparency != null) {
                llTransparency.setVisibility(hasWallpaper ? View.VISIBLE : View.GONE);
            }
            if (llParallax != null) {
                llParallax.setVisibility(hasWallpaper ? View.VISIBLE : View.GONE);
            }
        };

        updateVisibility.run();
        wallpaperManager.addListener(has -> {
            if (btnRemove != null) {
                btnRemove.post(updateVisibility);
            }
        });

        if (btnChoose != null) {
            btnChoose.setOnClickListener(v -> {
                dismiss();
                if (wallpaperListener != null) {
                    wallpaperListener.onChooseWallpaperRequested();
                }
            });
        }

        if (btnRemove != null) {
            btnRemove.setOnClickListener(v -> {
                wallpaperManager.removeWallpaper();
                Toast.makeText(getContext(), R.string.wallpaper_removed, Toast.LENGTH_SHORT).show();
            });
        }

        if (sbTransparency != null && tvTransparencyLabel != null) {
            int currentVal = preferences.getTileTransparency();
            sbTransparency.setProgress(currentVal);
            tvTransparencyLabel.setText(getContext().getString(R.string.settings_tile_transparency_val, currentVal));

            sbTransparency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    tvTransparencyLabel.setText(getContext().getString(R.string.settings_tile_transparency_val, progress));
                    if (fromUser) {
                        preferences.setTileTransparency(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        if (switchParallax != null) {
            switchParallax.setChecked(preferences.isWallpaperParallaxEnabled());
            switchParallax.setOnCheckedChangeListener((buttonView, isChecked) -> {
                preferences.setWallpaperParallaxEnabled(isChecked);
            });
        }
    }

    private void setupLiveTilesSection() {
        View llNotificationAccess = findViewById(R.id.ll_settings_notification_access);
        TextView tvStatus = findViewById(R.id.tv_notification_access_status);

        Runnable refreshStatus = () -> {
            boolean granted = VioletNotificationService.isPermissionGranted(getContext());
            if (tvStatus != null) {
                tvStatus.setText(granted ?
                        R.string.settings_notification_access_enabled :
                        R.string.settings_notification_access_disabled);
            }
        };

        refreshStatus.run();

        if (llNotificationAccess != null) {
            llNotificationAccess.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                try {
                    getContext().startActivity(intent);
                } catch (Exception ignored) {
                }
            });
        }
    }

    private void setupHiddenAppsSection() {
        View llHidden = findViewById(R.id.ll_settings_hidden_apps);
        TextView tvCount = findViewById(R.id.tv_hidden_apps_count);
        HiddenAppsManager hiddenManager = HiddenAppsManager.getInstance(getContext());

        updateHiddenCount(tvCount, hiddenManager);

        hiddenManager.addListener(hidden -> {
            if (tvCount != null) {
                tvCount.post(() -> updateHiddenCount(tvCount, hiddenManager));
            }
        });

        if (llHidden != null) {
            llHidden.setOnClickListener(v -> {
                if (!hiddenManager.isPinConfigured()) {
                    // First time: prompt to create PIN
                    PinEntryDialog pinDialog = new PinEntryDialog(getContext(), PinEntryDialog.Mode.CREATE, () -> {
                        HiddenAppsDialog hiddenDialog = new HiddenAppsDialog(getContext());
                        hiddenDialog.show();
                    });
                    pinDialog.show();
                } else {
                    // Verify existing PIN
                    PinEntryDialog pinDialog = new PinEntryDialog(getContext(), PinEntryDialog.Mode.VERIFY, () -> {
                        HiddenAppsDialog hiddenDialog = new HiddenAppsDialog(getContext());
                        hiddenDialog.show();
                    });
                    pinDialog.show();
                }
            });
        }
    }

    private void setupDefaultHomeSection() {
        View llDefaultHome = findViewById(R.id.ll_settings_default_home);
        if (llDefaultHome != null) {
            llDefaultHome.setOnClickListener(v -> {
                Context context = getContext();
                Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    try {
                        context.startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
                    } catch (Exception ex) {
                        try {
                            context.startActivity(new Intent(Settings.ACTION_SETTINGS));
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        }
    }

    private void updateHiddenCount(TextView tvCount, HiddenAppsManager manager) {
        if (tvCount != null) {
            int count = manager.getHiddenPackages().size();
            tvCount.setText(getContext().getString(R.string.settings_hidden_apps_count, count));
        }
    }

    private static class AccentColorAdapter extends RecyclerView.Adapter<AccentColorAdapter.ViewHolder> {
        private final AccentColor[] colors;
        private AccentColor selectedColor;
        private final OnColorSelectedListener listener;

        interface OnColorSelectedListener {
            void onColorSelected(AccentColor color);
        }

        AccentColorAdapter(AccentColor[] colors, AccentColor selectedColor, OnColorSelectedListener listener) {
            this.colors = colors;
            this.selectedColor = selectedColor;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_accent_color, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AccentColor color = colors[position];
            boolean isSelected = color == selectedColor;
            holder.bind(color, isSelected, c -> {
                selectedColor = c;
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onColorSelected(c);
                }
            });
        }

        @Override
        public int getItemCount() {
            return colors.length;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final View viewColor;
            private final ImageView ivSelected;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                viewColor = itemView.findViewById(R.id.view_accent_color);
                ivSelected = itemView.findViewById(R.id.iv_accent_selected);
            }

            void bind(AccentColor color, boolean isSelected, OnColorSelectedListener listener) {
                viewColor.setBackgroundColor(color.getColorInt());
                ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                itemView.setContentDescription(color.getDisplayName());

                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onColorSelected(color);
                    }
                });
            }
        }
    }
}
