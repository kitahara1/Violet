# Execution Plan: Windows 10 Mobile Wallpaper & Vertical Parallax

**Project:** Violet (Lightweight Windows Phone–Inspired Android Launcher)  
**Feature:** Windows 10 Mobile Wallpaper, Tile Transparency & Start Screen Parallax  
**Status:** Approved for Implementation  
**Target Platform:** Android (minSdk 26, targetSdk 36)  
**Primary Language:** Java (Java 11)  
**UI Toolkit:** Android Views, XML & System Photo Picker  

---

## 1. Overview & Objectives

In Windows 10 Mobile and Windows Phone 8.1, personalization reached its peak with the **Tile Wallpaper and Parallax engine**. The user's chosen photo appears behind the Start screen tiles, the tiles become translucent or transparent ("Tile Picture"), and scrolling down the Start screen produces a signature **slower-speed vertical parallax motion** that creates an illusion of real physical depth.

### Core Requirements:
1. **Zero-Permission Photo Selection**:
   - Uses Android's modern Photo Picker (`ActivityResultContracts.PickVisualMedia` with fallback to `ACTION_GET_CONTENT`) requiring **zero** `READ_EXTERNAL_STORAGE` permissions.
   - Decodes and scales the chosen image efficiently to the device screen bounds to prevent memory bloat, saving locally to app-private storage (`files/wallpaper.jpg`).
2. **Authentic Vertical Start Screen Parallax**:
   - Wallpaper `ImageView` is mounted directly behind the `rv_start_tiles` grid.
   - Sized to ~`1.35x` screen height to provide ample vertical scrolling room.
   - Start grid scroll offset (`computeVerticalScrollOffset()`) drives vertical translation at a factor of `0.35x` (moving smoothly at ~1/3 speed of tiles).
3. **Horizontal ViewPager Parallax Shift**:
   - As the user swipes between the Start Screen and All Apps, the wallpaper gently translates horizontally by `~0.2x`, maintaining panoramic depth without bleeding into All Apps text.
4. **Tile Transparency Slider / Control**:
   - Settings offers a **"tile transparency"** option:
     - `0%`: Classic solid Windows Phone 8 opaque accent tiles.
     - `30%`: Subtle translucency.
     - `60%`: Windows 10 Mobile default (accent color tint with photo shining through).
     - `100%`: Pure "Tile Picture" mode where tiles act as clear glass windows into the photo.
5. **Clean Removal / Reset**:
   - One-tap "remove background" resets to the pure minimalist black/white OLED theme instantly.

---

## 2. Architectural Design

```text
com.example.violet/
├── data/
│   ├── LauncherPreferences.java    # Added keys for wallpaper path & tile transparency (0-100)
│   └── WallpaperManager.java       # Local image downsampling, disk caching, and thread-safe loading
├── ui/
│   ├── LauncherActivity.java       # ActivityResultLauncher for Photo Picker; feeds image to PagerAdapter
│   ├── LauncherPagerAdapter.java   # Hosts Start screen wallpaper ImageView & handles parallax scroll listener
│   ├── start/
│   │   └── TileAdapter.java        # Blends accent color with tile transparency alpha
│   └── settings/
│       └── SettingsDialog.java     # Adds "background" section (Choose photo, Remove, Transparency slider)
```

---

## 3. Step-by-Step Task Breakdown

### Task 1: Wallpaper Storage & Processing (`WallpaperManager.java`)
- **File:** `app/src/main/java/com/example/violet/data/WallpaperManager.java`
- **Actions:**
  1. Create `WallpaperManager` singleton to handle background file management.
  2. Implement `saveWallpaper(Uri sourceUri)`:
     - Sample and decode source image to match screen dimensions (avoid OutOfMemory errors).
     - Save as high-quality compressed JPEG in `context.getFilesDir() + "/start_wallpaper.jpg"`.
  3. Implement `hasWallpaper()`, `getWallpaperBitmap()`, and `removeWallpaper()`.
  4. Expose change listener `OnWallpaperChangedListener` to immediately update views.

---

### Task 2: Preferences & Tile Transparency (`LauncherPreferences.java` & `TileAdapter.java`)
- **Files:**
  - `app/src/main/java/com/example/violet/data/LauncherPreferences.java`
  - `app/src/main/java/com/example/violet/ui/start/TileAdapter.java`
- **Actions:**
  1. Add `tile_transparency` (int 0–100, default 50 when wallpaper enabled, 0 otherwise) to `LauncherPreferences`.
  2. In `TileAdapter.TileViewHolder.bind()`:
     - Compute alpha: `int alpha = (int) ((1.0f - (transparencyPercent / 100f)) * 255);`
     - Apply RGBA color to tile background:
       - If transparency == 100: use subtle semi-transparent border/glass so empty tiles remain tactile.
       - If transparency < 100: blend accent color with `Color.argb(alpha, r, g, b)`.

---

### Task 3: Layout & Parallax Motion Engine
- **Files:**
  - `app/src/main/res/layout/fragment_start_screen.xml`
  - `app/src/main/java/com/example/violet/ui/LauncherPagerAdapter.java`
  - `app/src/main/java/com/example/violet/ui/LauncherActivity.java`
- **Actions:**
  1. Update `fragment_start_screen.xml`:
     - Add `ImageView android:id="@+id/iv_start_wallpaper"` behind `rv_start_tiles`.
     - Sized with height `match_parent` (or scaled programmatically to `1.35x` viewport height) and `scaleType="centerCrop"`.
  2. Implement vertical parallax in `LauncherPagerAdapter`:
     - Attach `OnScrollListener` to `rv_start_tiles`.
     - In `onScrolled()`:
       ```java
       int scrollY = rv_start_tiles.computeVerticalScrollOffset();
       ivStartWallpaper.setTranslationY(-scrollY * 0.35f);
       ```
  3. Implement horizontal viewport parallax in `LauncherActivity`:
     - In `ViewPager2.OnPageChangeCallback.onPageScrolled(position, positionOffset, ...)`:
       Pass normalized offset to translate the wallpaper slightly (`-offset * 0.2f * screenWidth`).

---

### Task 4: Settings Integration & Photo Picker
- **Files:**
  - `app/src/main/res/layout/dialog_settings.xml`
  - `app/src/main/java/com/example/violet/ui/settings/SettingsDialog.java`
  - `app/src/main/res/values/strings.xml`
- **Actions:**
  1. Add string resources (`background`, `choose_photo`, `remove_photo`, `tile_transparency`, etc.).
  2. In `dialog_settings.xml`, add **"background"** section:
     - "choose photo" button / row
     - "remove photo" button (visible only when wallpaper is active)
     - Transparency SeekBar / slider (0% to 100%) with live percentage readout.
  3. In `LauncherActivity`:
     - Register `ActivityResultLauncher<PickVisualMediaRequest>`.
     - Pass callback to `SettingsDialog` to trigger the system photo picker.

---

## 4. Verification & Testing

1. **Compilation Check**:
   - `./gradlew assembleDebug` compiles cleanly with 0 errors.
2. **Functional Test Cases**:
   - **Pick Photo**: Open Settings $\rightarrow$ tap "choose photo" $\rightarrow$ select photo $\rightarrow$ wallpaper instantly loads behind Start screen.
   - **Vertical Parallax**: Scroll Start screen tiles up and down $\rightarrow$ tiles scroll normally while wallpaper translates at 0.35x speed with zero stutter.
   - **Horizontal Parallax**: Swipe to All Apps page $\rightarrow$ wallpaper subtly shifts without overlapping or bleeding into the app list.
   - **Transparency Slider**: Adjust slider from 0% to 100% $\rightarrow$ tiles smoothly transition from solid accent color to transparent windows.
   - **Remove Wallpaper**: Tap "remove photo" $\rightarrow$ wallpaper is deleted and tiles revert to solid OLED theme.

---

## 5. Definition of Done (DoD)

- [ ] Zero-permission system Photo Picker integrated for wallpaper selection.
- [ ] Image downsampled safely to screen resolution and cached in app-private storage.
- [ ] Vertical parallax (`0.35x`) implemented on Start screen grid scrolling.
- [ ] Horizontal ViewPager parallax (`0.2x`) implemented during page swipes.
- [ ] Tile transparency customization (0% to 100%) with live accent color alpha blending.
- [ ] "Remove photo" resets launcher cleanly.
- [ ] `./gradlew assembleDebug` passes with 0 errors.
