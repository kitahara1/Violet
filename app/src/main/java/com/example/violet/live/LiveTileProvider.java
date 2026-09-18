package com.example.violet.live;

import androidx.annotation.Nullable;
import com.example.violet.data.TileSize;

/**
 * Contract for lightweight Live Tile data sources.
 * Adheres strictly to the PRD: event-driven, zero polling, zero idle CPU work.
 */
public interface LiveTileProvider {

    interface OnLiveTileUpdatedListener {
        void onLiveTileUpdated();
    }

    void startListening();

    void stopListening();

    boolean canProvideForPackage(String packageName);

    @Nullable
    LiveTileData getDataForPackage(String packageName, TileSize size);

    void addListener(OnLiveTileUpdatedListener listener);

    void removeListener(OnLiveTileUpdatedListener listener);
}
