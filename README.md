# Violet 🪟💜

A fast, lightweight, and authentic **Windows Phone / Windows 10 Mobile**–inspired launcher for Android.

Built strictly with native Android Views, Java, and minimal dependencies to achieve blisteringly fast launch times, smooth 60/120fps Metro kinetic motion, low memory usage, and zero battery drain.

---

## ✨ Features

### 📱 Start Screen (Metro Tile Grid)
- **Authentic Tile Geometry**: Supports 3 classic tile sizes:
  - **Small** ($1 \times 1$)
  - **Medium / Wide** ($2 \times 1$)
  - **Large** ($2 \times 2$)
- **3D Tilt Touch Physics**: Tiles depress and tilt along their 3D axes in the direction of your finger press.
- **Drag & Drop Reordering**: Long press any tile to enter edit mode with smooth reordering and auto-scrolling.
- **Context Menu**: Quick shortcuts to resize, unpin, or open system application info.
- **Live Tiles Engine**:
  - **Clock & Calendar**: Real-time dual-sided 3D flipping updates.
  - **Battery Tile**: Displays live battery level and charging state with smooth synchronized 3D flip animations.
  - **Battery-Friendly**: Automatically pauses updates when the screen is off or another app is in the foreground.
- **Empty State Guidance**: Intuitive fallback with quick-pin suggestions when all tiles are unpinned.

### 📋 All Apps List & Quick Jump
- **Alphabetical Categorization**: Clean, single-column app list grouped by alphabet headers (`#`, `A`, `B`... `Z`).
- **Windows Phone Quick Jump Modal**: Tap any letter header to open the signature Metro jump matrix for instant navigation to any letter.
- **Real-Time Search**: Instant filtering as you type.
- **Pin to Start**: Long press any app to pin it directly to your Start screen.

### 🚀 3D Turnstile Launch Kinetics & Interim Metro Splash
- **Windows Phone Turnstile Motion**:
  1. **Phase 1 — 3D Turnstile Sweep**: Tapped tile zooms forward on the Z-axis while surrounding tiles swivel away around their left vertical hinge with a feathered cascade (`0ms – 240ms`).
  2. **Phase 2 — Interim Metro Splash Screen**: Displays the app's accent color, crisp centered app icon (72dp), and app title (`240ms – 560ms`), completely eliminating Android cold-start blank screens.
  3. **Phase 3 — Smooth App Launch**: The target application seamlessly opens over the splash screen.
- **Instant Clean Return**: Resetting immediately to resting state when returning to the launcher with no stuck or missing tiles.

### 🎨 Personalization & Theming
- **14 Signature Metro Accent Colors**: Cobalt, Crimson, Emerald, Amber, Magenta, Violet, Cyan, Lime, Mango, Pink, Teal, Steel, Mauve, and Olive.
- **Dark & Light Modes**: Includes high-contrast OLED Pure Black mode.
- **Tile Transparency**: Configurable transparency slider ($0\% - 100\%$).
- **Panoramic Wallpaper Parallax**: Full-bleed wallpaper support with subtle horizontal parallax scrolling when swiping between the Start screen and the All Apps list.
- **Built-in Wallpaper Cropper**: Easily pan, scale, and crop any image for your Start background.
- **Edge-to-Edge Display**: Transparent status and navigation bars with automatic light/dark icon contrast switching.

---

## ⚡ Performance & Privacy Highlights

| Metric | Violet Launcher | Typical Third-Party Launcher |
| :--- | :--- | :--- |
| **APK Size** | **~4.4 MB** | 15 – 50+ MB |
| **Idle CPU Usage** | **0.0%** | Continuous background polling |
| **Dependencies** | **Core Android Views only** | Heavy UI frameworks & tracking SDKs |
| **Telemetry / Ads** | **None (100% Offline)** | Analytics, telemetry, ad networks |
| **Live Tile Sleep** | **Auto-pauses when launcher stops** | Always-on background services |

---

## 🛠️ Architecture

Violet adheres to the principle: **"Build the smallest launcher that feels complete."**

```text
com.example.violet/
├── data/
│   ├── AccentColor.java          # Metro accent color definitions & palette
│   ├── AppInfo.java              # Application metadata & iconography
│   ├── AppRepository.java        # Package manager discovery & background loader
│   ├── LauncherPreferences.java  # Lightweight persistence (theme, accent, transparency)
│   ├── Tile.java                 # Start screen tile model (size, position, type)
│   ├── TileRepository.java       # Grid layout persistence & default tiles
│   └── WallpaperManager.java     # Start background storage & bitmap cache
├── live/
│   ├── LiveTile.java             # Base interface for flipping dynamic tiles
│   ├── LiveTileManager.java      # Lifecycle-aware update orchestrator
│   ├── BatteryLiveTile.java      # Dynamic battery percentage & charging tile
│   └── ClockLiveTile.java        # Dynamic time & date tile
├── receiver/
│   └── PackageChangeReceiver.java# Listens for app installs, updates, and removals
└── ui/
    ├── LauncherActivity.java     # Main Home Activity & viewport manager
    ├── LauncherPagerAdapter.java # Horizontal ViewPager2 adapter (Start ⟷ Apps)
    ├── anim/
    │   └── TurnstileAnimator.java# Windows Phone 3D Turnstile kinetics & splash
    ├── apps/
    │   ├── AppItem.java          # List item model (Header or App entry)
    │   ├── AppListAdapter.java   # Fast alphabetical RecyclerView adapter
    │   └── QuickJumpDialog.java  # Metro alphabet matrix jump picker
    ├── settings/
    │   └── SettingsActivity.java # Accent, theme, wallpaper & transparency settings
    ├── start/
    │   ├── StartScreenFragment.java
    │   ├── TileAdapter.java      # SpannedGridLayoutManager tile grid adapter
    │   └── TileTouchHelperCallback.java # Drag, drop, and edit mode physics
    └── wallpaper/
        └── WallpaperCropActivity.java # Image cropper for Start wallpaper
```

---

## 🚀 Building & Installing

### Prerequisites
- Android Studio Ladybug / Meerkat or Gradle CLI
- Android SDK 34+
- Java 17+

### Command-Line Build

1. **Clone the repository**:
   ```bash
   git clone https://github.com/username/Violet.git
   cd Violet
   ```

2. **Run unit tests**:
   ```bash
   ./gradlew test
   ```

3. **Build the Release APK**:
   ```bash
   ./gradlew assembleRelease
   ```
   The generated APK will be available at:
   `app/build/outputs/apk/release/app-release.apk`

4. **Install onto your connected device**:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

---

## ⚙️ Setting as Default Launcher

1. Open Android **Settings** $\to$ **Apps** $\to$ **Default Apps** $\to$ **Home App**.
2. Select **Violet**.
3. Press your device's Home button or swipe up from the bottom to return to your new Metro Start screen.

---

## 📄 License

Distributed under the Apache 2.0 License. See `LICENSE` for more information.
