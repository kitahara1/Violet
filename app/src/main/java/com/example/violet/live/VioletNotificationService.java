package com.example.violet.live;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Event-driven notification listener service.
 * Tracks active notifications and active media playback (VLC, Apple Music, Spotify, etc.)
 * per application without polling. Zero CPU usage while idle.
 */
public class VioletNotificationService extends NotificationListenerService {

    public static class MediaTrackInfo {
        private final String title;
        private final String artist;
        private final String album;
        private final boolean isPlaying;

        public MediaTrackInfo(@Nullable String title, @Nullable String artist, @Nullable String album, boolean isPlaying) {
            this.title = title != null ? title : "";
            this.artist = artist != null ? artist : "";
            this.album = album != null ? album : "";
            this.isPlaying = isPlaying;
        }

        @NonNull
        public String getTitle() {
            return title;
        }

        @NonNull
        public String getArtist() {
            return artist;
        }

        @NonNull
        public String getAlbum() {
            return album;
        }

        public boolean isPlaying() {
            return isPlaying;
        }
    }

    public interface NotificationStateListener {
        void onNotificationStateChanged();
    }

    private static volatile VioletNotificationService instance;
    private static final List<NotificationStateListener> listeners = new CopyOnWriteArrayList<>();

    // Map: packageName -> count of active unread alert notifications (excluding media players)
    private static final Map<String, Integer> notificationCounts = new ConcurrentHashMap<>();
    private static final Map<String, String> notificationSnippets = new ConcurrentHashMap<>();

    // Map: packageName -> currently active media track information
    private static final Map<String, MediaTrackInfo> activeMediaTracks = new ConcurrentHashMap<>();

    private MediaSessionManager mediaSessionManager;
    private MediaSessionManager.OnActiveSessionsChangedListener activeSessionsListener;

    public static boolean isPermissionGranted(Context context) {
        Set<String> enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context);
        return enabledPackages.contains(context.getPackageName());
    }

    public static void addListener(NotificationStateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(NotificationStateListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public static int getNotificationCount(String packageName) {
        if (packageName == null) return 0;
        Integer count = notificationCounts.get(packageName);
        return count != null ? count : 0;
    }

    public static String getLatestSnippet(String packageName) {
        if (packageName == null) return "";
        String snippet = notificationSnippets.get(packageName);
        return snippet != null ? snippet : "";
    }

    @Nullable
    public static MediaTrackInfo getMediaTrack(String packageName) {
        if (packageName == null) return null;
        return activeMediaTracks.get(packageName);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
        setupMediaSessionListener();
        refreshActiveNotifications();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        tearDownMediaSessionListener();
        if (instance == this) {
            instance = null;
        }
    }

    private void setupMediaSessionListener() {
        try {
            mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mediaSessionManager != null) {
                ComponentName comp = new ComponentName(this, VioletNotificationService.class);
                activeSessionsListener = controllers -> {
                    updateFromMediaControllers(controllers);
                    notifyListeners();
                };
                mediaSessionManager.addOnActiveSessionsChangedListener(activeSessionsListener, comp);
                updateFromMediaControllers(mediaSessionManager.getActiveSessions(comp));
            }
        } catch (Throwable ignored) {
        }
    }

    private void tearDownMediaSessionListener() {
        if (mediaSessionManager != null && activeSessionsListener != null) {
            try {
                mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsListener);
            } catch (Throwable ignored) {
            }
            activeSessionsListener = null;
        }
    }

    private void updateFromMediaControllers(@Nullable List<MediaController> controllers) {
        if (controllers == null) return;
        for (MediaController controller : controllers) {
            if (controller == null) continue;
            String pkg = controller.getPackageName();
            if (pkg == null) continue;

            PlaybackState state = controller.getPlaybackState();
            boolean isPlaying = state != null && state.getState() == PlaybackState.STATE_PLAYING;

            MediaMetadata metadata = controller.getMetadata();
            if (metadata != null) {
                String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
                String album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);

                if (title != null && !title.trim().isEmpty()) {
                    activeMediaTracks.put(pkg, new MediaTrackInfo(title, artist, album, isPlaying));
                }
            } else if (state != null && state.getState() == PlaybackState.STATE_STOPPED) {
                activeMediaTracks.remove(pkg);
            }
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;

        if (isMediaNotification(sbn)) {
            // Extract media info and suppress notification badge count
            updateMediaFromNotification(sbn);
        } else if (!sbn.isOngoing()) {
            updateRegularNotification(sbn);
        }
        notifyListeners();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) return;

        if (isMediaNotification(sbn)) {
            activeMediaTracks.remove(sbn.getPackageName());
        }
        refreshActiveNotifications();
        notifyListeners();
    }

    private boolean isMediaNotification(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        if (notification == null) return false;

        if (Notification.CATEGORY_TRANSPORT.equals(notification.category)) {
            return true;
        }
        if (notification.extras != null && notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) {
            return true;
        }
        return false;
    }

    private void updateMediaFromNotification(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        if (pkg == null) return;

        Notification notification = sbn.getNotification();
        if (notification != null && notification.extras != null) {
            CharSequence titleSeq = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence textSeq = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
            CharSequence subTextSeq = notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT);

            String title = titleSeq != null ? titleSeq.toString() : "";
            String artist = textSeq != null ? textSeq.toString() : "";
            String album = subTextSeq != null ? subTextSeq.toString() : "";

            if (!title.isEmpty()) {
                activeMediaTracks.put(pkg, new MediaTrackInfo(title, artist, album, true));
            }
        }
    }

    private void updateRegularNotification(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        if (pkg == null) return;

        int current = notificationCounts.containsKey(pkg) ? notificationCounts.get(pkg) : 0;
        notificationCounts.put(pkg, current + 1);

        Notification notification = sbn.getNotification();
        if (notification != null && notification.extras != null) {
            CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
            CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
            if (text != null && text.length() > 0) {
                notificationSnippets.put(pkg, text.toString());
            } else if (title != null && title.length() > 0) {
                notificationSnippets.put(pkg, title.toString());
            }
        }
    }

    private void refreshActiveNotifications() {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            notificationCounts.clear();
            notificationSnippets.clear();

            if (active != null) {
                for (StatusBarNotification sbn : active) {
                    if (sbn == null) continue;

                    if (isMediaNotification(sbn)) {
                        updateMediaFromNotification(sbn);
                    } else if (!sbn.isOngoing()) {
                        String pkg = sbn.getPackageName();
                        if (pkg == null) continue;

                        int count = notificationCounts.containsKey(pkg) ? notificationCounts.get(pkg) : 0;
                        notificationCounts.put(pkg, count + 1);

                        Notification notification = sbn.getNotification();
                        if (notification != null && notification.extras != null) {
                            CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
                            CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
                            if (text != null && text.length() > 0) {
                                notificationSnippets.put(pkg, text.toString());
                            } else if (title != null && title.length() > 0) {
                                notificationSnippets.put(pkg, title.toString());
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void notifyListeners() {
        for (NotificationStateListener listener : listeners) {
            listener.onNotificationStateChanged();
        }
    }
}
