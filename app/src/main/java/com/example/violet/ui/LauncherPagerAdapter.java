package com.example.violet.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.violet.R;
import com.example.violet.data.LauncherPreferences;
import com.example.violet.data.WallpaperManager;
import com.example.violet.ui.apps.AlphabetJumpDialog;
import com.example.violet.ui.apps.AppItem;
import com.example.violet.ui.apps.AppListAdapter;
import com.example.violet.ui.settings.SettingsDialog;
import com.example.violet.ui.start.TileAdapter;
import com.example.violet.ui.start.TileTouchCallback;

import java.util.List;

/**
 * ViewPager2 adapter managing:
 * - Page 0: Start Screen (Scrollable Metro Header & Tiles, Edge-to-Edge Wallpaper Parallax, Drag reordering, Settings access, Empty state)
 * - Page 1: All Apps List (Search filtering, Alphabet jump modal, Uninstall, Long press pin)
 */
public class LauncherPagerAdapter extends RecyclerView.Adapter<LauncherPagerAdapter.PageViewHolder> {

    public static final int PAGE_START = 0;
    public static final int PAGE_APPS = 1;
    public static final int TOTAL_PAGES = 2;

    public interface OnNavigateToAppsListener {
        void onNavigateToApps();
    }

    private final Context context;
    private final TileAdapter tileAdapter;
    private final AppListAdapter appListAdapter;
    private final WallpaperManager wallpaperManager;
    private final LauncherPreferences preferences;
    private OnNavigateToAppsListener navigateToAppsListener;
    private SettingsDialog.OnWallpaperActionListener wallpaperActionListener;

    private Insets systemInsets = Insets.NONE;
    private float currentScrollY = 0f;

    // View references
    private ImageView ivStartWallpaper;
    private RecyclerView rvStartTiles;
    private View llEmptyStart;

    private View appListContainer;
    private RecyclerView rvAppList;
    private ProgressBar pbLoadingApps;
    private TextView tvEmptyApps;
    private EditText etSearchApps;
    private ImageView ivSearchClear;

    public LauncherPagerAdapter(Context context, TileAdapter tileAdapter, AppListAdapter appListAdapter) {
        this.context = context;
        this.tileAdapter = tileAdapter;
        this.appListAdapter = appListAdapter;
        this.wallpaperManager = WallpaperManager.getInstance(context);
        this.preferences = LauncherPreferences.getInstance(context);

        this.wallpaperManager.addListener(hasWallpaper -> {
            updateWallpaperView();
        });
    }

    public void setOnNavigateToAppsListener(OnNavigateToAppsListener listener) {
        this.navigateToAppsListener = listener;
    }

    public void setOnWallpaperActionListener(SettingsDialog.OnWallpaperActionListener listener) {
        this.wallpaperActionListener = listener;
    }

    public void applySystemInsets(Insets insets) {
        if (insets == null) return;
        this.systemInsets = insets;
        if (rvStartTiles != null) {
            rvStartTiles.setPadding(dpToPx(12), insets.top, dpToPx(12), insets.bottom);
        }
        if (appListContainer != null) {
            appListContainer.setPadding(0, insets.top, 0, systemInsets.bottom);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public int getItemCount() {
        return TOTAL_PAGES;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view;
        if (viewType == PAGE_START) {
            view = inflater.inflate(R.layout.fragment_start_screen, parent, false);
            setupStartPage(view);
        } else {
            view = inflater.inflate(R.layout.fragment_app_list, parent, false);
            setupAppsPage(view);
        }
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
    }

    private void setupStartPage(View view) {
        ivStartWallpaper = view.findViewById(R.id.iv_start_wallpaper);
        rvStartTiles = view.findViewById(R.id.rv_start_tiles);
        llEmptyStart = view.findViewById(R.id.ll_empty_start);
        View btnGoToApps = view.findViewById(R.id.btn_go_to_apps);

        if (rvStartTiles != null && systemInsets != null) {
            rvStartTiles.setPadding(dpToPx(12), systemInsets.top, dpToPx(12), systemInsets.bottom);
        }

        // Configure hardware acceleration and height
        if (ivStartWallpaper != null) {
            ivStartWallpaper.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            updateParallaxConfiguration();
            updateWallpaperView();
        }

        tileAdapter.setOnSettingsClickListener(() -> {
            SettingsDialog dialog = new SettingsDialog(context, wallpaperActionListener);
            dialog.show();
        });

        if (btnGoToApps != null) {
            btnGoToApps.setOnClickListener(v -> {
                if (navigateToAppsListener != null) {
                    navigateToAppsListener.onNavigateToApps();
                }
            });
        }

        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 4);
        gridLayoutManager.setSpanSizeLookup(tileAdapter.getSpanSizeLookup());
        rvStartTiles.setLayoutManager(gridLayoutManager);
        rvStartTiles.setItemAnimator(null); // Disable DefaultItemAnimator so custom 3D tumble rotations are never canceled
        rvStartTiles.setAdapter(tileAdapter);

        // Attach drag-to-reorder ItemTouchHelper
        TileTouchCallback callback = new TileTouchCallback(tileAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(rvStartTiles);

        // Continuous sub-pixel vertical parallax using physical touch/fling deltas (zero stutter)
        rvStartTiles.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    tileAdapter.dismissActiveMenu();
                } else {
                    if (!recyclerView.canScrollVertically(-1)) {
                        currentScrollY = 0f;
                        if (ivStartWallpaper != null) {
                            ivStartWallpaper.setTranslationY(0f);
                        }
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                currentScrollY += dy;
                if (!recyclerView.canScrollVertically(-1)) {
                    currentScrollY = 0f;
                } else if (currentScrollY < 0f) {
                    currentScrollY = 0f;
                }

                if (ivStartWallpaper != null && ivStartWallpaper.getVisibility() == View.VISIBLE) {
                    if (!preferences.isWallpaperParallaxEnabled()) {
                        ivStartWallpaper.setTranslationY(0f);
                        return;
                    }

                    int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
                    int wallpaperHeight = ivStartWallpaper.getHeight();
                    float maxTravel = Math.max(0, wallpaperHeight - screenHeight);

                    float targetTranslationY = -currentScrollY * 0.35f;
                    if (maxTravel > 0 && -targetTranslationY > maxTravel) {
                        targetTranslationY = -maxTravel;
                    }
                    ivStartWallpaper.setTranslationY(targetTranslationY);
                }
            }
        });

        rvStartTiles.post(() -> {
            int availableWidth = rvStartTiles.getWidth() - rvStartTiles.getPaddingStart() - rvStartTiles.getPaddingEnd();
            if (availableWidth > 0) {
                int unitPx = availableWidth / 4;
                tileAdapter.setColumnUnitPx(unitPx);
                tileAdapter.notifyDataSetChanged();
            }
        });

        updateStartEmptyState(tileAdapter.getTileCount() == 0);
    }

    public void updateParallaxConfiguration() {
        if (ivStartWallpaper == null) return;
        boolean parallax = preferences.isWallpaperParallaxEnabled();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        ViewGroup.LayoutParams lp = ivStartWallpaper.getLayoutParams();
        if (lp != null) {
            lp.height = parallax ? (int) (dm.heightPixels * 1.8f) : dm.heightPixels;
            ivStartWallpaper.setLayoutParams(lp);
        }
        if (!parallax) {
            currentScrollY = 0f;
            ivStartWallpaper.setTranslationY(0f);
            ivStartWallpaper.setTranslationX(0f);
        }
    }

    public void updateWallpaperView() {
        if (ivStartWallpaper == null) return;

        if (wallpaperManager.hasWallpaper()) {
            Bitmap bmp = wallpaperManager.getWallpaperBitmap();
            if (bmp != null) {
                ivStartWallpaper.setImageBitmap(bmp);
                ivStartWallpaper.setVisibility(View.VISIBLE);
                return;
            }
        }
        ivStartWallpaper.setImageDrawable(null);
        ivStartWallpaper.setVisibility(View.GONE);
    }

    public void setHorizontalWallpaperOffset(float offsetPx) {
        if (ivStartWallpaper != null && ivStartWallpaper.getVisibility() == View.VISIBLE) {
            if (preferences.isWallpaperParallaxEnabled()) {
                ivStartWallpaper.setTranslationX(offsetPx);
            } else {
                ivStartWallpaper.setTranslationX(0f);
            }
        }
    }

    private void setupAppsPage(View view) {
        appListContainer = view.findViewById(R.id.app_list_container);
        rvAppList = view.findViewById(R.id.rv_app_list);
        pbLoadingApps = view.findViewById(R.id.pb_loading);
        tvEmptyApps = view.findViewById(R.id.tv_empty);
        etSearchApps = view.findViewById(R.id.et_search_apps);
        ivSearchClear = view.findViewById(R.id.iv_search_clear);

        if (appListContainer != null && systemInsets != null) {
            appListContainer.setPadding(0, systemInsets.top, 0, systemInsets.bottom);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        rvAppList.setLayoutManager(layoutManager);
        rvAppList.setHasFixedSize(true);
        rvAppList.setAdapter(appListAdapter);

        // Header click -> Alphabet jump modal
        appListAdapter.setOnHeaderClickListener((letter, activeLetters) -> {
            AlphabetJumpDialog dialog = new AlphabetJumpDialog(context, activeLetters, targetLetter -> {
                int position = appListAdapter.findHeaderPosition(targetLetter);
                if (position >= 0) {
                    layoutManager.scrollToPositionWithOffset(position, 0);
                }
            });
            dialog.show();
        });

        // Search text watcher
        if (etSearchApps != null) {
            etSearchApps.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s != null ? s.toString() : "";
                    appListAdapter.filter(query);

                    if (ivSearchClear != null) {
                        ivSearchClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (ivSearchClear != null) {
            ivSearchClear.setOnClickListener(v -> {
                if (etSearchApps != null) {
                    etSearchApps.setText("");
                }
            });
        }
    }

    public void updateStartEmptyState(boolean isEmpty) {
        if (llEmptyStart != null) {
            llEmptyStart.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    public void updateAppListState(List<AppItem> items, boolean isLoading) {
        if (pbLoadingApps != null) {
            pbLoadingApps.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        if (items != null) {
            if (appListAdapter != null) {
                appListAdapter.setItems(items);
            }
            if (tvEmptyApps != null && rvAppList != null) {
                if (items.isEmpty() && !isLoading) {
                    tvEmptyApps.setVisibility(View.VISIBLE);
                    rvAppList.setVisibility(View.GONE);
                } else {
                    tvEmptyApps.setVisibility(View.GONE);
                    rvAppList.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void scrollToStartTop() {
        currentScrollY = 0f;
        if (ivStartWallpaper != null) {
            ivStartWallpaper.animate().translationY(0f).setDuration(250).start();
        }
        if (rvStartTiles != null) {
            rvStartTiles.smoothScrollToPosition(0);
        }
    }

    public void scrollToAppsTop() {
        if (rvAppList != null) {
            rvAppList.smoothScrollToPosition(0);
        }
    }

    public boolean canScrollStartUp() {
        return rvStartTiles != null && rvStartTiles.canScrollVertically(-1);
    }

    public boolean canScrollAppsUp() {
        return rvAppList != null && rvAppList.canScrollVertically(-1);
    }

    public RecyclerView getStartRecyclerView() {
        return rvStartTiles;
    }

    public RecyclerView getAppListRecyclerView() {
        return rvAppList;
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        PageViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
