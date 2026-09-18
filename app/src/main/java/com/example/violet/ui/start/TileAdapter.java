package com.example.violet.ui.start;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.violet.R;
import com.example.violet.data.AccentColor;
import com.example.violet.data.AppInfo;
import com.example.violet.data.AppRepository;
import com.example.violet.data.LauncherPreferences;
import com.example.violet.data.Tile;
import com.example.violet.data.TileRepository;
import com.example.violet.data.TileSize;
import com.example.violet.data.WallpaperManager;
import com.example.violet.live.LiveTileData;
import com.example.violet.live.LiveTileManager;
import com.example.violet.live.anim.MetroFlipController;
import com.example.violet.ui.anim.MetroTiltTouchListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter for rendering Start screen with a scrollable Metro header and tiles in a
 * 4-column responsive grid with dynamic accent color tinting, tile transparency blending,
 * Live Tile badge counts & 3D flip animation cascade, 3D Metro tilt physics, and drag-to-reorder support.
 */
public class TileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements TileTouchCallback.ItemTouchHelperAdapter {

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_TILE = 1;
    private static final Object PAYLOAD_LIVE_UPDATE = new Object();

    public interface OnSettingsClickListener {
        void onSettingsClick();
    }

    public interface OnTileClickListener {
        void onTileClick(View view, Tile tile);
    }

    private final List<Tile> tiles = new ArrayList<>();
    private final AppRepository appRepository;
    private final TileRepository tileRepository;
    private final LauncherPreferences preferences;
    private final WallpaperManager wallpaperManager;
    private final LiveTileManager liveTileManager;
    private int columnUnitPx = 0;
    private PopupMenu activePopupMenu = null;
    private OnSettingsClickListener settingsClickListener;
    private OnTileClickListener tileClickListener;

    public TileAdapter(Context context) {
        this.appRepository = AppRepository.getInstance(context);
        this.tileRepository = TileRepository.getInstance(context);
        this.preferences = LauncherPreferences.getInstance(context);
        this.wallpaperManager = WallpaperManager.getInstance(context);
        this.liveTileManager = LiveTileManager.getInstance(context);

        this.wallpaperManager.addListener(has -> {
            if (activePopupMenu == null) {
                notifyDataSetChanged();
            }
        });

        // Granular update: when live tiles update, notify items via payload so ViewHolders
        // are NEVER destroyed or recycled, allowing staggered 3D tumble flips to run smoothly across all tiles.
        this.liveTileManager.addChangeListener(() -> {
            if (activePopupMenu == null && !tiles.isEmpty()) {
                notifyItemRangeChanged(1, tiles.size(), PAYLOAD_LIVE_UPDATE);
            }
        });

        this.preferences.addListener(new LauncherPreferences.OnPreferencesChangedListener() {
            @Override
            public void onAccentColorChanged(AccentColor newColor) {
                if (activePopupMenu == null) {
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onThemeModeChanged(int newThemeMode) {}

            @Override
            public void onTileTransparencyChanged(int newTransparency) {
                if (activePopupMenu == null) {
                    notifyDataSetChanged();
                }
            }
        });
    }

    public void setOnSettingsClickListener(OnSettingsClickListener listener) {
        this.settingsClickListener = listener;
    }

    public void setOnTileClickListener(OnTileClickListener listener) {
        this.tileClickListener = listener;
    }

    public void setTiles(List<Tile> newTiles) {
        tiles.clear();
        if (newTiles != null) {
            tiles.addAll(newTiles);
        }
        notifyDataSetChanged();
    }

    public int getTileCount() {
        return tiles.size();
    }

    public void setColumnUnitPx(int unitPx) {
        this.columnUnitPx = unitPx;
    }

    public GridLayoutManager.SpanSizeLookup getSpanSizeLookup() {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 0) {
                    return 4; // Start header spans full grid width
                }
                int tileIndex = position - 1;
                if (tileIndex >= 0 && tileIndex < tiles.size()) {
                    return tiles.get(tileIndex).getSize().getSpanSize();
                }
                return 1;
            }
        };
    }

    @Override
    public int getItemCount() {
        return tiles.size() + 1; // Header at index 0 + tile list
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_TILE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_start_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_tile, parent, false);
            return new TileViewHolder(view, this);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(settingsClickListener);
        } else if (holder instanceof TileViewHolder) {
            int tileIndex = position - 1;
            if (tileIndex >= 0 && tileIndex < tiles.size()) {
                Tile tile = tiles.get(tileIndex);
                ((TileViewHolder) holder).bind(
                        tile,
                        appRepository,
                        tileRepository,
                        liveTileManager,
                        columnUnitPx,
                        preferences.getAccentColor(),
                        preferences.getTileTransparency(),
                        wallpaperManager.hasWallpaper()
                );
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && holder instanceof TileViewHolder) {
            int tileIndex = position - 1;
            if (tileIndex >= 0 && tileIndex < tiles.size()) {
                Tile tile = tiles.get(tileIndex);
                ((TileViewHolder) holder).updateLiveTile(tile, liveTileManager, position);
            }
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof TileViewHolder) {
            ((TileViewHolder) holder).onRecycled();
        }
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition <= 0 || toPosition <= 0) return false;
        dismissActiveMenu();

        int fromIndex = fromPosition - 1;
        int toIndex = toPosition - 1;

        if (fromIndex < toIndex) {
            for (int i = fromIndex; i < toIndex; i++) {
                Collections.swap(tiles, i, i + 1);
            }
        } else {
            for (int i = fromIndex; i > toIndex; i--) {
                Collections.swap(tiles, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onDragStarted() {
        dismissActiveMenu();
    }

    @Override
    public void onDragFinished() {
        dismissActiveMenu();
        tileRepository.reorderTiles(tiles);
    }

    public void dismissActiveMenu() {
        if (activePopupMenu != null) {
            activePopupMenu.dismiss();
            activePopupMenu = null;
        }
    }

    public void showTileContextMenu(View anchor, Tile tile) {
        dismissActiveMenu();
        Context context = anchor.getContext();
        PopupMenu popup = new PopupMenu(context, anchor);
        this.activePopupMenu = popup;

        popup.getMenu().add(0, 1, 0, context.getString(R.string.resize_tile) + " (" + tile.getSize().next().name().toLowerCase() + ")");
        popup.getMenu().add(0, 2, 1, context.getString(R.string.unpin_from_start));
        popup.getMenu().add(0, 3, 2, context.getString(R.string.app_info));

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                tileRepository.updateTileSize(tile.getId(), tile.getSize().next());
                return true;
            } else if (id == 2) {
                tileRepository.removeTile(tile.getId());
                Toast.makeText(context, R.string.tile_unpinned, Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == 3) {
                openAppDetails(context, tile.getPackageName());
                return true;
            }
            return false;
        });

        popup.setOnDismissListener(p -> {
            if (activePopupMenu == p) {
                activePopupMenu = null;
            }
        });

        popup.show();
    }

    private static void openAppDetails(Context context, String packageName) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivSettings;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSettings = itemView.findViewById(R.id.iv_settings_button);
        }

        void bind(OnSettingsClickListener listener) {
            if (ivSettings != null) {
                ivSettings.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onSettingsClick();
                    }
                });
            }
        }
    }

    static class TileViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout tileRoot;
        private final View frontFace;
        private final ImageView ivIcon;
        private final TextView tvLabel;
        private final TextView tvBadge;

        private final View backFace;
        private final TextView tvLiveHeader;
        private final TextView tvLivePrimary;
        private final TextView tvLiveSubtitle;

        private final MetroFlipController flipController;
        private final TileAdapter adapter;
        private final MetroTiltTouchListener tiltTouchListener = new MetroTiltTouchListener();

        private LiveTileData currentLiveData = null;

        TileViewHolder(@NonNull View itemView, TileAdapter adapter) {
            super(itemView);
            this.adapter = adapter;
            tileRoot = itemView.findViewById(R.id.tile_root);
            frontFace = itemView.findViewById(R.id.tile_front_face);
            ivIcon = itemView.findViewById(R.id.iv_tile_icon);
            tvLabel = itemView.findViewById(R.id.tv_tile_label);
            tvBadge = itemView.findViewById(R.id.tv_tile_badge);

            backFace = itemView.findViewById(R.id.tile_back_face);
            tvLiveHeader = itemView.findViewById(R.id.tv_live_header);
            tvLivePrimary = itemView.findViewById(R.id.tv_live_primary);
            tvLiveSubtitle = itemView.findViewById(R.id.tv_live_subtitle);

            flipController = new MetroFlipController(tileRoot, frontFace, backFace);

            // Enable signature Windows Phone 3D perspective tilt physics
            itemView.setOnTouchListener(tiltTouchListener);
        }

        void onRecycled() {
            flipController.stopFlipping();
            currentLiveData = null;
        }

        void bind(Tile tile, AppRepository appRepo, TileRepository tileRepo, LiveTileManager liveTileManager,
                  int columnUnitPx, AccentColor accentColor, int transparency, boolean hasWallpaper) {
            Context context = itemView.getContext();
            AppInfo app = appRepo.findApp(tile.getPackageName(), tile.getActivityName());

            String label = (app != null) ? app.getLabel() : tile.getLabel();
            tvLabel.setText(label);
            itemView.setContentDescription(label);

            if (app != null && app.getIcon() != null) {
                ivIcon.setImageDrawable(app.getIcon());
            } else {
                ivIcon.setImageDrawable(null);
            }

            // Apply active accent color & transparency dynamically
            int baseColor = (accentColor != null) ? accentColor.getColorInt() : 0xFF68217A;
            if (hasWallpaper) {
                int alpha = Math.max(0, Math.min(255, (int) ((1.0f - (transparency / 100.0f)) * 255)));
                int tintedColor = (alpha == 0) ? Color.TRANSPARENT : Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
                if (tileRoot.getBackground() != null) {
                    tileRoot.getBackground().mutate().setColorFilter(tintedColor, PorterDuff.Mode.SRC_IN);
                }
            } else {
                if (tileRoot.getBackground() != null) {
                    tileRoot.getBackground().mutate().setColorFilter(baseColor, PorterDuff.Mode.SRC_IN);
                }
            }

            // Adjust height and typography based on TileSize
            adjustTileSize(tile.getSize(), columnUnitPx);

            // Bind Live Tile Content (Badges + 3D Tumble Flip)
            LiveTileData liveData = liveTileManager.getDataForPackage(tile.getPackageName(), tile.getSize());
            currentLiveData = liveData;

            if (liveData != null) {
                bindLiveContent(liveData, tile.getSize());

                if (liveData.shouldFlip() && liveData.hasContent()) {
                    flipController.startFlipping();
                } else {
                    flipController.stopFlipping();
                }
            } else {
                tvBadge.setVisibility(View.GONE);
                flipController.stopFlipping();
            }

            // Click: Launch application with Turnstile animation
            itemView.setOnClickListener(v -> {
                if (adapter.tileClickListener != null) {
                    adapter.tileClickListener.onTileClick(v, tile);
                } else {
                    launchApp(context, tile);
                }
            });

            // Long Click: Anchored context menu (reset tilt cleanly first so popup anchors accurately)
            itemView.setOnLongClickListener(v -> {
                v.animate().rotationX(0f).rotationY(0f).scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
                adapter.showTileContextMenu(v, tile);
                return true;
            });
        }

        /**
         * Lightweight update called via PAYLOAD_LIVE_UPDATE.
         * Detects content updates and triggers a staggered 3D cascade flip across all updating tiles.
         */
        void updateLiveTile(Tile tile, LiveTileManager liveTileManager, int position) {
            LiveTileData newLiveData = liveTileManager.getDataForPackage(tile.getPackageName(), tile.getSize());
            boolean hasChanged = (currentLiveData == null && newLiveData != null) ||
                    (currentLiveData != null && !currentLiveData.equals(newLiveData));

            if (newLiveData != null && newLiveData.shouldFlip() && newLiveData.hasContent()) {
                if (hasChanged) {
                    currentLiveData = newLiveData;
                    // Windows Phone cascade: stagger tumble flip by 160ms per position to ensure
                    // that ALL updating tiles flip gracefully without canceling each other.
                    int staggerDelay = (position % 6) * 160;
                    flipController.triggerUpdateFlip(staggerDelay, () -> {
                        bindLiveContent(newLiveData, tile.getSize());
                    });
                } else {
                    // Content unchanged: maintain normal periodic idle flip loop
                    flipController.startFlipping();
                }
            } else if (newLiveData != null) {
                currentLiveData = newLiveData;
                bindLiveContent(newLiveData, tile.getSize());
                flipController.stopFlipping();
            } else {
                currentLiveData = null;
                tvBadge.setVisibility(View.GONE);
                flipController.stopFlipping();
            }
        }

        private void bindLiveContent(LiveTileData liveData, TileSize size) {
            if (liveData == null) {
                tvBadge.setVisibility(View.GONE);
                return;
            }

            if (liveData.getBadgeCount() > 0) {
                tvBadge.setText(String.valueOf(liveData.getBadgeCount()));
                tvBadge.setVisibility(View.VISIBLE);
            } else {
                tvBadge.setVisibility(View.GONE);
            }

            if (liveData.hasContent()) {
                tvLiveHeader.setText(liveData.getHeader());
                tvLivePrimary.setText(liveData.getPrimaryText());
                tvLiveSubtitle.setText(liveData.getSubtitle());

                // Responsive typography: scale song titles vs numbers/time
                boolean isLongText = liveData.getPrimaryText().length() > 6;
                if (size == TileSize.SMALL) {
                    tvLivePrimary.setTextSize(isLongText ? 13 : 22);
                    tvLiveSubtitle.setVisibility(View.GONE);
                } else if (size == TileSize.WIDE) {
                    tvLivePrimary.setTextSize(isLongText ? 18 : 28);
                    tvLiveSubtitle.setVisibility(View.VISIBLE);
                } else if (size == TileSize.MEDIUM) {
                    tvLivePrimary.setTextSize(isLongText ? 20 : 32);
                    tvLiveSubtitle.setVisibility(View.VISIBLE);
                } else { // LARGE
                    tvLivePrimary.setTextSize(isLongText ? 24 : 36);
                    tvLiveSubtitle.setVisibility(View.VISIBLE);
                }
            }
        }

        private void adjustTileSize(TileSize size, int columnUnitPx) {
            ViewGroup.LayoutParams lp = itemView.getLayoutParams();
            if (lp == null) return;

            int targetHeight;
            if (columnUnitPx > 0) {
                switch (size) {
                    case LARGE:
                    case MEDIUM:
                        targetHeight = columnUnitPx * 2;
                        break;
                    case WIDE:
                    case SMALL:
                    default:
                        targetHeight = columnUnitPx;
                        break;
                }
                lp.height = targetHeight;
                itemView.setLayoutParams(lp);
            }

            // Visual density & typography based on size
            if (size == TileSize.SMALL) {
                tvLabel.setVisibility(View.GONE);
                FrameLayout.LayoutParams iconLp = (FrameLayout.LayoutParams) ivIcon.getLayoutParams();
                iconLp.width = dpToPx(itemView.getContext(), 32);
                iconLp.height = dpToPx(itemView.getContext(), 32);
                ivIcon.setLayoutParams(iconLp);
                tvLivePrimary.setTextSize(22);
                tvLiveSubtitle.setVisibility(View.GONE);
            } else if (size == TileSize.WIDE) {
                tvLabel.setVisibility(View.VISIBLE);
                tvLabel.setTextSize(12);
                FrameLayout.LayoutParams iconLp = (FrameLayout.LayoutParams) ivIcon.getLayoutParams();
                iconLp.width = dpToPx(itemView.getContext(), 38);
                iconLp.height = dpToPx(itemView.getContext(), 38);
                ivIcon.setLayoutParams(iconLp);
                tvLivePrimary.setTextSize(28);
                tvLiveSubtitle.setVisibility(View.VISIBLE);
            } else if (size == TileSize.MEDIUM) {
                // 2x2 square: prominent icon, bottom-left label, balanced typography
                tvLabel.setVisibility(View.VISIBLE);
                tvLabel.setTextSize(13);
                FrameLayout.LayoutParams iconLp = (FrameLayout.LayoutParams) ivIcon.getLayoutParams();
                iconLp.width = dpToPx(itemView.getContext(), 48);
                iconLp.height = dpToPx(itemView.getContext(), 48);
                ivIcon.setLayoutParams(iconLp);
                tvLivePrimary.setTextSize(32);
                tvLiveSubtitle.setVisibility(View.VISIBLE);
            } else { // LARGE (4x2)
                tvLabel.setVisibility(View.VISIBLE);
                tvLabel.setTextSize(14);
                FrameLayout.LayoutParams iconLp = (FrameLayout.LayoutParams) ivIcon.getLayoutParams();
                iconLp.width = dpToPx(itemView.getContext(), 56);
                iconLp.height = dpToPx(itemView.getContext(), 56);
                ivIcon.setLayoutParams(iconLp);
                tvLivePrimary.setTextSize(36);
                tvLiveSubtitle.setVisibility(View.VISIBLE);
            }
        }

        private void launchApp(Context context, Tile tile) {
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                if (tile.getActivityName() != null && !tile.getActivityName().isEmpty()) {
                    intent.setClassName(tile.getPackageName(), tile.getActivityName());
                } else {
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(tile.getPackageName());
                    if (launchIntent != null) {
                        intent = launchIntent;
                    }
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(intent);
            } catch (ActivityNotFoundException | SecurityException e) {
                Toast.makeText(context, "Unable to launch " + tile.getLabel(), Toast.LENGTH_SHORT).show();
            }
        }

        private int dpToPx(Context context, int dp) {
            return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
        }
    }
}
