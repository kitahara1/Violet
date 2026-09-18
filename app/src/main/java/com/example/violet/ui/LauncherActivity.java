package com.example.violet.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.violet.R;
import com.example.violet.data.AccentColor;
import com.example.violet.data.AppInfo;
import com.example.violet.data.AppRepository;
import com.example.violet.data.LauncherPreferences;
import com.example.violet.data.Tile;
import com.example.violet.data.TileRepository;
import com.example.violet.data.WallpaperManager;
import com.example.violet.live.LiveTileManager;
import com.example.violet.receiver.PackageChangeReceiver;
import com.example.violet.ui.anim.TurnstileAnimator;
import com.example.violet.ui.apps.AppItem;
import com.example.violet.ui.apps.AppListAdapter;
import com.example.violet.ui.start.TileAdapter;
import com.example.violet.ui.wallpaper.WallpaperCropActivity;

import java.util.List;

/**
 * Main Android Home activity for the Violet launcher.
 * Manages a 2-page horizontal viewport with authentic Metro transitions, Turnstile app launch kinetics,
 * Windows Phone interim splash screens, and edge-to-edge transparent system bars:
 * - Page 0: Start Screen (Tile Grid, 3D tilt touch physics, Wallpaper Parallax, Empty state guidance)
 * - Page 1: All Apps List (Alphabetical sorted apps, Real-time search, Quick jump modal)
 */
public class LauncherActivity extends AppCompatActivity {

    private View rootLayout;
    private ViewPager2 viewPager;
    private LauncherPagerAdapter pagerAdapter;

    private TileAdapter tileAdapter;
    private AppListAdapter appListAdapter;
    private TurnstileAnimator.SplashViewHolder splashHolder;

    private AppRepository appRepository;
    private TileRepository tileRepository;
    private LauncherPreferences preferences;
    private WallpaperManager wallpaperManager;
    private LiveTileManager liveTileManager;
    private PackageChangeReceiver packageChangeReceiver;
    private boolean isReceiverRegistered = false;

    private Insets lastInsets = Insets.NONE;

    private final ActivityResultLauncher<Intent> wallpaperCropLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, R.string.wallpaper_updated, Toast.LENGTH_SHORT).show();
                    updateSystemBarAppearance();
                }
            });

    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    Intent cropIntent = new Intent(this, WallpaperCropActivity.class);
                    cropIntent.setData(uri);
                    wallpaperCropLauncher.launch(cropIntent);
                }
            });

    private final TileRepository.OnTilesChangedListener tilesChangedListener = new TileRepository.OnTilesChangedListener() {
        @Override
        public void onTilesChanged(List<Tile> tiles) {
            runOnUiThread(() -> {
                tileAdapter.setTiles(tiles);
                if (pagerAdapter != null) {
                    pagerAdapter.updateStartEmptyState(tiles.isEmpty());
                }
            });
        }
    };

    private final LauncherPreferences.OnPreferencesChangedListener preferencesChangedListener = new LauncherPreferences.OnPreferencesChangedListener() {
        @Override
        public void onAccentColorChanged(AccentColor newColor) {
            runOnUiThread(() -> {
                tileAdapter.notifyDataSetChanged();
                appListAdapter.notifyDataSetChanged();
            });
        }

        @Override
        public void onThemeModeChanged(int newThemeMode) {
            runOnUiThread(() -> updateSystemBarAppearance());
        }

        @Override
        public void onTileTransparencyChanged(int newTransparency) {
            runOnUiThread(() -> tileAdapter.notifyDataSetChanged());
        }

        @Override
        public void onWallpaperParallaxChanged(boolean enabled) {
            runOnUiThread(() -> {
                if (pagerAdapter != null) {
                    pagerAdapter.updateParallaxConfiguration();
                }
            });
        }
    };

    private final WallpaperManager.OnWallpaperChangedListener wallpaperChangedListener = hasWallpaper -> {
        runOnUiThread(this::updateSystemBarAppearance);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = LauncherPreferences.getInstance(this);
        wallpaperManager = WallpaperManager.getInstance(this);
        liveTileManager = LiveTileManager.getInstance(this);

        // Apply saved theme mode before views are inflated
        AppCompatDelegate.setDefaultNightMode(preferences.getThemeMode());

        super.onCreate(savedInstanceState);

        // Edge-to-edge transparent system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_launcher);

        appRepository = AppRepository.getInstance(this);
        tileRepository = TileRepository.getInstance(this);

        initViews();
        setupInsets();
        setupAdapters();
        setupBackNavigation();
        updateSystemBarAppearance();

        // Initialize default tiles if first launch
        tileRepository.initializeDefaultTilesIfNeeded();
        tileAdapter.setTiles(tileRepository.getTiles());
        tileRepository.addListener(tilesChangedListener);
        preferences.addListener(preferencesChangedListener);
        wallpaperManager.addListener(wallpaperChangedListener);

        if (pagerAdapter != null) {
            pagerAdapter.updateStartEmptyState(tileRepository.getTiles().isEmpty());
        }

        // Discover applications
        loadApplications(false);
    }

    private void initViews() {
        rootLayout = findViewById(R.id.root_launcher_layout);
        viewPager = findViewById(R.id.view_pager);
        View splashOverlay = findViewById(R.id.splash_overlay);
        if (splashOverlay != null) {
            splashHolder = new TurnstileAnimator.SplashViewHolder(splashOverlay);
        }
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            lastInsets = insets;
            view.setPadding(0, 0, 0, 0);

            if (splashHolder != null && splashHolder.root != null) {
                splashHolder.root.setPadding(0, insets.top, 0, insets.bottom);
            }

            if (pagerAdapter != null) {
                pagerAdapter.applySystemInsets(insets);
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void updateSystemBarAppearance() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            boolean isNight = (preferences.getThemeMode() == LauncherPreferences.THEME_DARK);
            boolean hasWallpaper = wallpaperManager.hasWallpaper();
            boolean lightStatusBars = (!isNight && !hasWallpaper);
            controller.setAppearanceLightStatusBars(lightStatusBars);
            controller.setAppearanceLightNavigationBars(lightStatusBars);
        }
    }

    private void setupAdapters() {
        tileAdapter = new TileAdapter(this);
        tileAdapter.setOnTileClickListener(this::launchTile);

        appListAdapter = new AppListAdapter(this, this::launchApplication);

        pagerAdapter = new LauncherPagerAdapter(this, tileAdapter, appListAdapter);
        pagerAdapter.setOnNavigateToAppsListener(() -> {
            if (viewPager != null) {
                viewPager.setCurrentItem(LauncherPagerAdapter.PAGE_APPS, true);
            }
        });
        pagerAdapter.setOnWallpaperActionListener(() -> photoPickerLauncher.launch("image/*"));

        if (lastInsets != null && lastInsets != Insets.NONE) {
            pagerAdapter.applySystemInsets(lastInsets);
        }

        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(1);

        // Subtle horizontal panoramic parallax shift when swiping between Start and Apps
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 0) {
                    float offsetPx = -positionOffset * 0.25f * viewPager.getWidth();
                    pagerAdapter.setHorizontalWallpaperOffset(offsetPx);
                } else if (position == 1) {
                    pagerAdapter.setHorizontalWallpaperOffset(-0.25f * viewPager.getWidth());
                }
            }
        });
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (splashHolder != null && splashHolder.isVisible()) {
                    TurnstileAnimator.resetAll(
                            pagerAdapter != null ? pagerAdapter.getStartRecyclerView() : null,
                            pagerAdapter != null ? pagerAdapter.getAppListRecyclerView() : null,
                            splashHolder
                    );
                    return;
                }
                if (viewPager.getCurrentItem() == LauncherPagerAdapter.PAGE_APPS) {
                    viewPager.setCurrentItem(LauncherPagerAdapter.PAGE_START, true);
                } else {
                    if (pagerAdapter.canScrollStartUp()) {
                        pagerAdapter.scrollToStartTop();
                    }
                }
            }
        });
    }

    private void loadApplications(boolean forceReload) {
        pagerAdapter.updateAppListState(null, true);
        appRepository.loadApps(forceReload, this::onAppsLoaded);
    }

    private void onAppsLoaded(List<AppItem> items) {
        pagerAdapter.updateAppListState(items, false);
        tileAdapter.notifyDataSetChanged();
    }

    private void launchTile(View view, Tile tile) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            if (tile.getActivityName() != null && !tile.getActivityName().isEmpty()) {
                intent.setClassName(tile.getPackageName(), tile.getActivityName());
            } else {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(tile.getPackageName());
                if (launchIntent != null) {
                    intent = launchIntent;
                }
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

            // Resolve app icon, label, and accent color for the Metro splash screen
            Drawable icon = null;
            AppInfo appInfo = appRepository.findApp(tile.getPackageName(), tile.getActivityName());
            if (appInfo != null) {
                icon = appInfo.getIcon();
            }
            if (icon == null) {
                try {
                    icon = getPackageManager().getApplicationIcon(tile.getPackageName());
                } catch (Exception ignored) {
                }
            }

            int splashColor = preferences.getAccentColor().getColorInt();

            TurnstileAnimator.animateTileLaunch(
                    this,
                    view,
                    pagerAdapter != null ? pagerAdapter.getStartRecyclerView() : null,
                    splashHolder,
                    icon,
                    tile.getLabel(),
                    splashColor,
                    intent
            );
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Unable to launch " + tile.getLabel(), Toast.LENGTH_SHORT).show();
            if (splashHolder != null) {
                splashHolder.hide();
            }
            TurnstileAnimator.resetLaunchingState();
        }
    }

    private void launchApplication(View view, AppInfo app) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setClassName(app.getPackageName(), app.getActivityName());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

            int splashColor = preferences.getAccentColor().getColorInt();

            TurnstileAnimator.animateListLaunch(
                    this,
                    view,
                    pagerAdapter != null ? pagerAdapter.getAppListRecyclerView() : null,
                    splashHolder,
                    app.getIcon(),
                    app.getLabel(),
                    splashColor,
                    intent
            );
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Unable to launch " + app.getLabel(), Toast.LENGTH_SHORT).show();
            if (splashHolder != null) {
                splashHolder.hide();
            }
            TurnstileAnimator.resetLaunchingState();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pagerAdapter != null) {
            TurnstileAnimator.resetAll(
                    pagerAdapter.getStartRecyclerView(),
                    pagerAdapter.getAppListRecyclerView(),
                    splashHolder
            );
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (Intent.ACTION_MAIN.equals(intent.getAction()) && intent.hasCategory(Intent.CATEGORY_HOME)) {
            if (viewPager != null) {
                if (viewPager.getCurrentItem() != LauncherPagerAdapter.PAGE_START) {
                    viewPager.setCurrentItem(LauncherPagerAdapter.PAGE_START, true);
                } else {
                    pagerAdapter.scrollToStartTop();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (liveTileManager != null) {
            liveTileManager.resume();
        }
        registerPackageReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (splashHolder != null) {
            splashHolder.hide();
        }
        if (liveTileManager != null) {
            liveTileManager.pause();
        }
        unregisterPackageReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tileRepository.removeListener(tilesChangedListener);
        if (preferences != null) {
            preferences.removeListener(preferencesChangedListener);
        }
        if (wallpaperManager != null) {
            wallpaperManager.removeListener(wallpaperChangedListener);
        }
    }

    private void registerPackageReceiver() {
        if (!isReceiverRegistered) {
            packageChangeReceiver = new PackageChangeReceiver((packageName, isRemoved) -> {
                if (isRemoved && packageName != null) {
                    tileRepository.removeTilesByPackage(packageName);
                }
                loadApplications(true);
            });
            registerReceiver(packageChangeReceiver, PackageChangeReceiver.createIntentFilter());
            isReceiverRegistered = true;
        }
    }

    private void unregisterPackageReceiver() {
        if (isReceiverRegistered && packageChangeReceiver != null) {
            try {
                unregisterReceiver(packageChangeReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            isReceiverRegistered = false;
        }
    }
}
