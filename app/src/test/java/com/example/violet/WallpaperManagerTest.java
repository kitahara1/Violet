package com.example.violet;

import android.graphics.BitmapFactory;

import com.example.violet.data.LauncherPreferences;
import com.example.violet.data.WallpaperManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WallpaperManagerTest {

    @Test
    public void testInSampleSizeCalculation() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outWidth = 2160;
        options.outHeight = 3840;

        int sampleSize = WallpaperManager.calculateInSampleSize(options, 1080, 1920);
        assertEquals(2, sampleSize);
    }

    @Test
    public void testInSampleSizeSmallImage() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outWidth = 500;
        options.outHeight = 800;

        int sampleSize = WallpaperManager.calculateInSampleSize(options, 1080, 1920);
        assertEquals(1, sampleSize);
    }

    @Test
    public void testInSampleSizeLargeImage() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.outWidth = 4000;
        options.outHeight = 6000;

        int sampleSize = WallpaperManager.calculateInSampleSize(options, 1000, 1500);
        assertTrue(sampleSize >= 4);
    }

    @Test
    public void testDefaultParallaxEnabled() {
        assertTrue(LauncherPreferences.DEFAULT_WALLPAPER_PARALLAX);
    }
}
