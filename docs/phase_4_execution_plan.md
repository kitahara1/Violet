# Phase 4 Execution Plan: Metro Animation Polish & Edge Case Hardening

**Project:** Violet (Lightweight Windows Phone–Inspired Android Launcher)  
**Status:** Completed  
**Target Platform:** Android (minSdk 26, targetSdk 36)  
**Primary Language:** Java (Java 11)  
**UI Toolkit:** Android Views & XML  

---

## 1. Overview & Objectives

The primary goal of **Phase 4** is to deliver the authentic Windows Phone **tactile motion experience** (Section 6.4 of the PRD) and harden the launcher against package lifecycle changes, uninstalled apps, and edge cases (Sections 10.4 and 19 of the PRD), while strictly maintaining the zero-background-drain performance standard.

By the end of Phase 4:
- Tapping any tile triggers the signature **Metro 3D Tilt Effect**:
  - The tile tilts inward on the Z-axis with perspective scaling relative to the touch point (top, center, or bottom).
  - Snaps back smoothly on release or touch cancel.
- App launches and page transitions feel like Windows Phone with a subtle **Turnstile horizontal parallax transition**.
- The All Apps list includes an **"Uninstall"** action in the long-press menu for user apps, invoking the system uninstallation dialog.
- **Orphaned / Ghost Tiles**: When an app is uninstalled from the device, the `PackageChangeReceiver` automatically cleans up any pinned tiles corresponding to that package from `TileRepository`.
- **Empty Start Screen State**: A clean, Metro-styled prompt appears if all tiles are unpinned, guiding the user to swipe to All Apps and pin items.
- Smooth page transformer on `ViewPager2` creating a subtle horizontal parallax/turnstile effect between Start and All Apps.

---

## 2. Architectural Design

```text
com.example.violet/
├── data/
│   ├── TileRepository.java         # Added: removeTilesByPackage() for automatic uninstallation cleanup
│   └── AppRepository.java          # Hardened against uninstalled/disabled components
├── ui/
│   ├── LauncherActivity.java       # Coordinates Turnstile PageTransformer and package removals
│   ├── anim/
│   │   ├── MetroTiltTouchListener.java # 3D perspective tilt animator for tiles
│   │   └── MetroPageTransformer.java   # WP-inspired horizontal parallax/turnstile page transition
│   ├── start/
│   │   ├── TileAdapter.java        # Integrates MetroTiltTouchListener
│   │   └── TileViewHolder          # Perspective configuration (CameraDistance)
│   └── apps/
│       └── AppListAdapter.java     # Added: "Uninstall" option for non-system apps
```

---

## 3. Step-by-Step Task Breakdown

### Task 4.1: The Iconic Windows Phone 3D Tile Tilt Effect
- **Files:**
  - `app/src/main/java/com/example/violet/ui/anim/MetroTiltTouchListener.java`
  - `app/src/main/java/com/example/violet/ui/start/TileAdapter.java`
- **Actions Completed:**
  1. Built `MetroTiltTouchListener` calculating relative touch offsets `(dx, dy)` and applying authentic 3D perspective tilt rotations (`rotationX`, `rotationY`, `scaleX`, `scaleY`) using camera distance.
  2. Integrated listener onto tile items in `TileAdapter`, preserving `onClick` and `onLongClick` interactions.

---

### Task 4.2: Metro Page & Turnstile Transitions
- **Files:**
  - `app/src/main/java/com/example/violet/ui/anim/MetroPageTransformer.java`
  - `app/src/main/java/com/example/violet/ui/LauncherActivity.java`
- **Actions Completed:**
  1. Implemented `MetroPageTransformer` applying a subtle horizontal parallax factor (0.35x) and alpha fade between Start and All Apps.
  2. Connected transformer to `ViewPager2` in `LauncherActivity`.

---

### Task 4.3: App Uninstallation Support in App List
- **Files:**
  - `app/src/main/java/com/example/violet/ui/apps/AppListAdapter.java`
  - `app/src/main/res/values/strings.xml`
- **Actions Completed:**
  1. Checked `ApplicationInfo.FLAG_SYSTEM` to differentiate user apps from system packages.
  2. Added **"Uninstall"** dialog option triggering `Intent.ACTION_DELETE` with package URI.

---

### Task 4.4: Package Lifecycle & Orphan Tile Cleanup
- **Files:**
  - `app/src/main/java/com/example/violet/data/TileRepository.java`
  - `app/src/main/java/com/example/violet/receiver/PackageChangeReceiver.java`
  - `app/src/main/java/com/example/violet/ui/LauncherActivity.java`
- **Actions Completed:**
  1. Implemented `TileRepository.removeTilesByPackage(String packageName)` to purge pinned tiles for uninstalled applications.
  2. Updated `PackageChangeReceiver` to detect actual package removals and trigger tile cleanup before app list refresh.

---

### Task 4.5: Empty Start Screen State
- **Files:**
  - `app/src/main/res/layout/fragment_start_screen.xml`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/java/com/example/violet/ui/LauncherPagerAdapter.java`
  - `app/src/main/java/com/example/violet/ui/LauncherActivity.java`
- **Actions Completed:**
  1. Added empty state view container (`ll_empty_start`) in `fragment_start_screen.xml` with guidance text and direct navigation button (`btn_go_to_apps`).
  2. Connected visibility toggling in `LauncherPagerAdapter` when tile count reaches 0.

---

## 4. Verification & Testing

- **Build Sanity:**
  - Ran `./gradlew assembleDebug`
  - Result: **BUILD SUCCESSFUL in 2s** (0 errors).
- **Artifact:**
  - Output APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## 5. Definition of Done (DoD)

- [x] 3D perspective tilt animation active on all Start screen tiles.
- [x] Windows Phone turnstile / parallax page transition on `ViewPager2`.
- [x] "Uninstall" action in App List long-press menu for user applications.
- [x] Automatic cleanup of uninstalled apps from pinned Start tiles.
- [x] Clean empty state on Start screen when all tiles are removed.
- [x] `./gradlew assembleDebug` compiles cleanly with 0 errors.
