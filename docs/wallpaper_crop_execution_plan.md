# Execution Plan: Wallpaper Crop & Position Adjustment

**Project:** Violet (Lightweight Windows Phone–Inspired Android Launcher)  
**Feature:** Start Screen Wallpaper Crop & Parallax Frame Adjustment  
**Status:** Ready for Implementation  
**Target Platform:** Android (minSdk 26, targetSdk 36)  
**Primary Language:** Java (Java 11)  
**UI Toolkit:** Custom Android View, Touch Gestures, Zero Third-Party Dependencies  

---

## 1. Problem & User Need

When selecting a photo for the Start screen wallpaper, different photos have diverse aspect ratios (square, landscape, ultra-wide, vertical portraits). Currently, the launcher uses an automated `centerCrop` downsampler, which might cut off subjects (faces, logos, scenery) or center on an undesirable portion of the image.

The user needs the ability to:
1. **Pan (drag)** the image horizontally and vertically to frame the exact desired area.
2. **Pinch-to-zoom** the image to scale subjects in or out.
3. **Preview the Parallax Frame**: Clearly see what portion of the photo will be visible on initial launch, and what portion will be traversed during the 0.35x vertical parallax scroll.
4. **Apply or Cancel**: Confirm the crop to generate the final optimized wallpaper or discard changes.

---

## 2. Architecture & Components

```text
com.example.violet/
├── data/
│   └── WallpaperManager.java          # Added saveCroppedWallpaper(Bitmap bitmap, ...)
├── ui/
│   ├── LauncherActivity.java          # Photo picker triggers WallpaperCropActivity via ActivityResult
│   └── wallpaper/
│       ├── WallpaperCropActivity.java # Fullscreen edge-to-edge Metro crop canvas
│       └── WallpaperCropView.java     # Zero-dependency interactive touch view (pan, pinch-zoom, crop)
```

---

## 3. Detailed Component Design

### 3.1. Interactive Touch View (`WallpaperCropView.java`)
- **Matrix Transformation**: Uses an internal `Matrix` with translate $(dx, dy)$ and scale $(s)$.
- **Gestures**:
  - `ScaleGestureDetector` for smooth 2-finger pinch zooming (clamped between minimum fill scale and $4.0\times$).
  - Multi-touch pointer tracking (`ACTION_DOWN`, `ACTION_MOVE`, `ACTION_POINTER_UP`) for smooth 1-finger panning.
- **Viewport Frame**:
  - Matches the Start Screen parallax ratio: $\text{Width} = W_{\text{screen}}$, $\text{Height} = 1.8 \times H_{\text{screen}}$.
  - Draws an authentic Windows Phone Metro overlay:
    - Dimmed dark mask ($65\%$ black) outside the crop box.
    - Clean white hairline border around the active crop area.
    - Subtle dashed guideline indicating the **initial screen viewport** (the part visible before the user scrolls down).
- **Boundary Clamping**:
  - Automatically constraints the matrix translation so the image always completely covers the crop box (no unsightly empty or black bars).
- **High-Precision Crop Export**:
  - Computes the exact source-image rectangle corresponding to the viewport and creates the cropped bitmap using `Bitmap.createBitmap` without unnecessary quality loss.

### 3.2. Dedicated Crop Activity (`WallpaperCropActivity.java`)
- **Launch Flow**:
  - Started from `LauncherActivity` via `ActivityResultLauncher<Intent>` after user picks an image URI.
  - Receives image URI in `Intent.getData()`.
- **UI Structure (`activity_wallpaper_crop.xml`)**:
  - Fullscreen dark canvas (`#000000`).
  - Top header: "adjust wallpaper" title in light Segoe font.
  - Center: `WallpaperCropView` displaying the image with touch controls.
  - Bottom action bar:
    - **"cancel"** button (plain text / Metro borderless).
    - **"apply"** button (accent color background, white bold text).
  - Indeterminate `ProgressBar` during crop saving.
- **Memory Safety**:
  - Decodes the source URI using safe `inSampleSize` to ensure the bitmap fits comfortably within memory limits without risking OOM.
  - Recycles bitmaps safely on activity destruction.

### 3.3. Wallpaper Manager Integration (`WallpaperManager.java`)
- Add `saveCroppedWallpaper(Bitmap bitmap, Runnable onSuccess, Runnable onError)`:
  - Compresses the cropped bitmap to `files/start_wallpaper.jpg` with high JPEG quality ($92\%$).
  - Updates the cached bitmap in memory.
  - Automatically notifies all registered wallpaper change listeners.

### 3.4. Launcher Activity Integration (`LauncherActivity.java`)
- Update photo picker callback:
  - When user picks an image, launch `WallpaperCropActivity` with the chosen image URI.
  - On `RESULT_OK`, display "wallpaper updated" toast and let `WallpaperManager` seamlessly refresh the Start screen.

---

## 4. Step-by-Step Implementation Tasks

| Task | File | Description |
|---|---|---|
| **1** | `WallpaperCropView.java` | Create custom touch view supporting pinch-zoom, pan, bounds clamping, parallax viewport guide, and bitmap extraction. |
| **2** | `activity_wallpaper_crop.xml` | Define Metro layout with header, crop view, guidelines, and bottom action buttons. |
| **3** | `WallpaperCropActivity.java` | Create activity handling image URI loading, user crop action, and background export. |
| **4** | `AndroidManifest.xml` | Register `WallpaperCropActivity`. |
| **5** | `WallpaperManager.java` | Add `saveCroppedWallpaper(Bitmap bitmap, Runnable onSuccess, Runnable onError)`. |
| **6** | `LauncherActivity.java` | Route photo picker result to `WallpaperCropActivity`. |
| **7** | `strings.xml` | Add localization strings for crop screen and buttons. |

---

## 5. Verification & Quality Gates

1. **Compilation Check**:
   - Run `./gradlew assembleDebug` and `./gradlew test` with 0 warnings/errors.
2. **Interactive Functionality**:
   - Open Settings $\rightarrow$ tap "choose photo".
   - Select landscape, portrait, or square image.
   - Verify `WallpaperCropActivity` opens showing the photo with the parallax frame overlay.
   - Pinch to zoom in/out; drag to pan. Verify bounds do not reveal blank black gaps.
   - Tap "cancel": returns to launcher with no changes.
   - Tap "apply": cropped region is saved; Start screen reflects the chosen portion; vertical parallax scrolls as expected.
