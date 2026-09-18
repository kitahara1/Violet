package com.example.violet.live.providers;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.violet.data.TileSize;
import com.example.violet.live.LiveTileData;
import com.example.violet.live.LiveTileProvider;
import com.example.violet.live.VioletNotificationService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Live Tile provider for active media playback (VLC, Apple Music, Spotify, YouTube Music, etc.).
 * Displays "NOW PLAYING", the current track title, and the artist/album via 3D tumble flip,
 * while suppressing misleading unread notification count badges.
 */
public class MediaLiveTileProvider implements LiveTileProvider {

    private final Context context;
    private final List<OnLiveTileUpdatedListener> listeners = new CopyOnWriteArrayList<>();
    private final VioletNotificationService.NotificationStateListener stateListener = this::notifyUpdated;
    private boolean isListening = false;

    public MediaLiveTileProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void startListening() {
        if (!isListening) {
            VioletNotificationService.addListener(stateListener);
            isListening = true;
        }
    }

    @Override
    public void stopListening() {
        if (isListening) {
            VioletNotificationService.removeListener(stateListener);
            isListening = false;
        }
    }

    @Override
    public boolean canProvideForPackage(String packageName) {
        if (packageName == null) return false;
        VioletNotificationService.MediaTrackInfo track = VioletNotificationService.getMediaTrack(packageName);
        return track != null && !track.getTitle().isEmpty();
    }

    @Nullable
    @Override
    public LiveTileData getDataForPackage(String packageName, TileSize size) {
        if (packageName == null) return null;

        VioletNotificationService.MediaTrackInfo track = VioletNotificationService.getMediaTrack(packageName);
        if (track == null || track.getTitle().isEmpty()) {
            return null;
        }

        String header = track.isPlaying() ? "NOW PLAYING" : "PAUSED";
        String primaryText = track.getTitle();

        String subtitle;
        if (!track.getArtist().isEmpty() && !track.getAlbum().isEmpty()) {
            subtitle = track.getArtist() + " • " + track.getAlbum();
        } else if (!track.getArtist().isEmpty()) {
            subtitle = track.getArtist();
        } else if (!track.getAlbum().isEmpty()) {
            subtitle = track.getAlbum();
        } else {
            subtitle = "";
        }

        return new LiveTileData(
                header,
                primaryText,
                subtitle,
                0, // Zero badge count: suppress notification numbers for media playback
                true // Flip to show song and artist
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
