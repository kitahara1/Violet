package com.example.violet.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages Start screen wallpaper image storage, sampling, and memory caching.
 * Keeps memory footprint minimal by downsampling to device screen dimensions.
 */
public class WallpaperManager {

    private static final String WALLPAPER_FILE_NAME = "start_wallpaper.jpg";
    private static volatile WallpaperManager instance;

    private final Context appContext;
    private final File wallpaperFile;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<OnWallpaperChangedListener> listeners = new CopyOnWriteArrayList<>();

    private Bitmap cachedBitmap = null;

    public interface OnWallpaperChangedListener {
        void onWallpaperChanged(boolean hasWallpaper);
    }

    public static WallpaperManager getInstance(Context context) {
        if (instance == null) {
            synchronized (WallpaperManager.class) {
                if (instance == null) {
                    instance = new WallpaperManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private WallpaperManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.wallpaperFile = new File(context.getFilesDir(), WALLPAPER_FILE_NAME);
    }

    public void addListener(OnWallpaperChangedListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(OnWallpaperChangedListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public boolean hasWallpaper() {
        return wallpaperFile.exists() && wallpaperFile.length() > 0;
    }

    public synchronized Bitmap getWallpaperBitmap() {
        if (!hasWallpaper()) {
            return null;
        }
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            return cachedBitmap;
        }

        try {
            DisplayMetrics dm = appContext.getResources().getDisplayMetrics();
            int reqWidth = dm.widthPixels;
            int reqHeight = (int) (dm.heightPixels * 1.8f); // Ample height for smooth parallax travel

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(wallpaperFile.getAbsolutePath(), options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Efficient 16-bit color for low memory

            cachedBitmap = BitmapFactory.decodeFile(wallpaperFile.getAbsolutePath(), options);
            return cachedBitmap;
        } catch (Throwable t) {
            return null;
        }
    }

    public void saveCroppedWallpaper(Bitmap bitmap, Runnable onSuccess, Runnable onError) {
        executor.execute(() -> {
            boolean success = false;
            try {
                if (bitmap != null) {
                    if (wallpaperFile.exists()) {
                        wallpaperFile.delete();
                    }
                    try (FileOutputStream fos = new FileOutputStream(wallpaperFile)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                        fos.flush();
                        success = true;
                    }

                    synchronized (WallpaperManager.this) {
                        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
                            cachedBitmap.recycle();
                        }
                        cachedBitmap = bitmap;
                    }
                }
            } catch (Throwable t) {
                success = false;
            }

            final boolean finalSuccess = success;
            mainHandler.post(() -> {
                if (finalSuccess) {
                    notifyListeners(true);
                    if (onSuccess != null) onSuccess.run();
                } else {
                    if (onError != null) onError.run();
                }
            });
        });
    }

    public void saveWallpaper(Uri sourceUri, Runnable onSuccess, Runnable onError) {
        executor.execute(() -> {
            boolean success = false;
            try {
                DisplayMetrics dm = appContext.getResources().getDisplayMetrics();
                int reqWidth = dm.widthPixels;
                int reqHeight = (int) (dm.heightPixels * 1.8f);

                // Decode bounds
                InputStream input = appContext.getContentResolver().openInputStream(sourceUri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(input, null, options);
                if (input != null) input.close();

                // Decode sampled bitmap
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
                options.inJustDecodeBounds = false;

                InputStream inputSampled = appContext.getContentResolver().openInputStream(sourceUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputSampled, null, options);
                if (inputSampled != null) inputSampled.close();

                if (bitmap != null) {
                    // Write to local file
                    if (wallpaperFile.exists()) {
                        wallpaperFile.delete();
                    }
                    try (FileOutputStream fos = new FileOutputStream(wallpaperFile)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        fos.flush();
                        success = true;
                    }

                    synchronized (WallpaperManager.this) {
                        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
                            cachedBitmap.recycle();
                        }
                        cachedBitmap = bitmap;
                    }
                }
            } catch (Throwable t) {
                success = false;
            }

            final boolean finalSuccess = success;
            mainHandler.post(() -> {
                if (finalSuccess) {
                    notifyListeners(true);
                    if (onSuccess != null) onSuccess.run();
                } else {
                    if (onError != null) onError.run();
                }
            });
        });
    }

    public void removeWallpaper() {
        if (wallpaperFile.exists()) {
            wallpaperFile.delete();
        }
        synchronized (this) {
            if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
                cachedBitmap.recycle();
                cachedBitmap = null;
            }
        }
        notifyListeners(false);
    }

    private void notifyListeners(boolean hasWallpaper) {
        for (OnWallpaperChangedListener listener : listeners) {
            listener.onWallpaperChanged(hasWallpaper);
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
