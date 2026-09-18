package com.example.violet.ui.wallpaper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.violet.R;
import com.example.violet.data.LauncherPreferences;
import com.example.violet.data.WallpaperManager;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fullscreen Metro-styled activity allowing the user to pan, zoom, and frame their
 * wallpaper image to match the Start screen's vertical parallax dimensions.
 */
public class WallpaperCropActivity extends AppCompatActivity {

    private WallpaperCropView cropView;
    private View headerContainer;
    private View bottomContainer;
    private TextView btnCancel;
    private TextView btnApply;
    private ProgressBar pbSaving;

    private Bitmap loadedBitmap;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge transparent system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_wallpaper_crop);

        cropView = findViewById(R.id.crop_view);
        headerContainer = findViewById(R.id.crop_header_container);
        bottomContainer = findViewById(R.id.crop_bottom_container);
        btnCancel = findViewById(R.id.btn_cancel_crop);
        btnApply = findViewById(R.id.btn_apply_crop);
        pbSaving = findViewById(R.id.pb_saving_wallpaper);

        // Apply dynamic accent color to the apply button
        LauncherPreferences preferences = LauncherPreferences.getInstance(this);
        int accentColor = preferences.getAccentColor().getColorInt();
        btnApply.setBackgroundColor(accentColor);

        // Adapt crop aspect ratio according to parallax setting
        cropView.setParallaxEnabled(preferences.isWallpaperParallaxEnabled());

        setupInsets();
        setupActions();

        Uri imageUri = getIntent().getData();
        if (imageUri != null) {
            loadImage(imageUri);
        } else {
            Toast.makeText(this, R.string.wallpaper_failed, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupInsets() {
        View root = findViewById(R.id.root_crop_layout);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            if (headerContainer != null) {
                headerContainer.setPadding(
                        headerContainer.getPaddingLeft(),
                        insets.top + dpToPx(16),
                        headerContainer.getPaddingRight(),
                        headerContainer.getPaddingBottom()
                );
            }
            if (bottomContainer != null) {
                bottomContainer.setPadding(
                        bottomContainer.getPaddingLeft(),
                        bottomContainer.getPaddingTop(),
                        bottomContainer.getPaddingRight(),
                        insets.bottom + dpToPx(16)
                );
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupActions() {
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnApply.setOnClickListener(v -> {
            btnApply.setEnabled(false);
            btnCancel.setEnabled(false);
            pbSaving.setVisibility(View.VISIBLE);

            Bitmap cropped = cropView.getCroppedBitmap();
            if (cropped == null) {
                Toast.makeText(this, R.string.wallpaper_failed, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            WallpaperManager.getInstance(this).saveCroppedWallpaper(cropped, () -> {
                setResult(RESULT_OK);
                finish();
            }, () -> {
                Toast.makeText(this, R.string.wallpaper_failed, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void loadImage(Uri uri) {
        pbSaving.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            Bitmap bitmap = null;
            try {
                DisplayMetrics dm = getResources().getDisplayMetrics();
                int maxTargetDimension = Math.max(dm.widthPixels, dm.heightPixels) * 2;

                // 1. Just decode bounds
                InputStream input = getContentResolver().openInputStream(uri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(input, null, options);
                if (input != null) input.close();

                // 2. Decode sampled image
                options.inSampleSize = WallpaperManager.calculateInSampleSize(options, maxTargetDimension, maxTargetDimension);
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.RGB_565;

                InputStream inputSampled = getContentResolver().openInputStream(uri);
                bitmap = BitmapFactory.decodeStream(inputSampled, null, options);
                if (inputSampled != null) inputSampled.close();
            } catch (Throwable ignored) {
            }

            final Bitmap finalBitmap = bitmap;
            runOnUiThread(() -> {
                pbSaving.setVisibility(View.GONE);
                if (finalBitmap != null) {
                    loadedBitmap = finalBitmap;
                    cropView.setSourceBitmap(finalBitmap);
                } else {
                    Toast.makeText(this, R.string.wallpaper_failed, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        if (loadedBitmap != null && !loadedBitmap.isRecycled()) {
            loadedBitmap.recycle();
            loadedBitmap = null;
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
