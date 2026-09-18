# Phase 2 Execution Plan: Start Screen & Tile Grid Architecture

**Project:** Violet (Lightweight Windows Phone–Inspired Android Launcher)  
**Status:** Completed  
**Target Platform:** Android (minSdk 26, targetSdk 36)  
**Primary Language:** Java (Java 11)  
**UI Toolkit:** Android Views & XML  

---

## 1. Overview & Objectives

The primary goal of **Phase 2** is to implement the core signature feature of Windows Phone: the **Start Screen Tile Grid**, along with **Pin/Unpin capabilities**, **Horizontal Pager Navigation** between Start and the App List, and **Lightweight Persistence**.

By the end of Phase 2:
- The user can swipe smoothly between the **Start Screen** and the **App List** using a two-page viewport (`ViewPager2`).
- The Start Screen features a responsive grid supporting the 3 canonical Windows Phone tile sizes:
  - **Small** (1×1)
  - **Wide** (2×1)
  - **Large** (4×2 / full-width banner)
- Tiles are rendered with classic Windows Phone Metro styling (accent background, white app icon, bottom-left typography).
- Users can **Pin to Start** by long-pressing an item in the App List.
- Users can **Unpin from Start** and **Cycle Tile Size** (Small ↔ Wide ↔ Large) via long-press options dialog.
- Start screen tile layout is persisted across launcher restarts using lightweight `SharedPreferences` (native JSON array of tile descriptors).
- Prepared data model for future **Live Tiles** (`TileType.STATIC` vs `TileType.LIVE`).

---

## 2. Architectural Design

```text
com.example.violet/
├── data/
│   ├── AppInfo.java               # App metadata (from Phase 1)
│   ├── AppRepository.java         # Discovery, caching, and on-demand app resolution
│   ├── Tile.java                  # Model: id, packageName, activityName, label, size, type, orderIndex
│   ├── TileSize.java              # Enum: SMALL (1-span), WIDE (2-span), LARGE (4-span)
│   ├── TileType.java              # Enum: STATIC, LIVE (future extensibility)
│   └── TileRepository.java        # SharedPreferences persistence (JSON) & CRUD operations
├── ui/
│   ├── LauncherActivity.java      # Orchestrates Start Screen & App List pages via ViewPager2
│   ├── LauncherPagerAdapter.java  # ViewPager2 adapter managing Page 0 (Start) and Page 1 (Apps)
│   ├── start/
│   │   ├── TileAdapter.java       # 4-column GridLayoutManager adapter & SpanSizeLookup
│   │   └── TileViewHolder         # Metro tile rendering with accent background & sizing
│   └── apps/
│       ├── AppItem.java           # Section header vs App entry model
│       └── AppListAdapter.java    # Alphabetical list adapter with Long-Click "Pin to Start" options
```

---

## 3. Step-by-Step Task Breakdown

### Task 2.1: Tile Models & Tile Repository
- **Files:**
  - `app/src/main/java/com/example/violet/data/TileSize.java`
  - `app/src/main/java/com/example/violet/data/TileType.java`
  - `app/src/main/java/com/example/violet/data/Tile.java`
  - `app/src/main/java/com/example/violet/data/TileRepository.java`
- **Actions Completed:**
  1. Defined `TileSize` with span mappings (`SMALL` = 1, `WIDE` = 2, `LARGE` = 4) and cycling logic (`next()`).
  2. Defined `TileType` with `STATIC` and `LIVE` for future Live Tile expansion.
  3. Built `Tile` with native JSON serialization and deserialization.
  4. Built `TileRepository` with `SharedPreferences` backing, change listeners, and out-of-the-box default tile initialization.

---

### Task 2.2: Windows Phone Two-Page Viewport (Start Screen ↔ App List)
- **Files:**
  - `app/src/main/res/layout/activity_launcher.xml`
  - `app/src/main/res/layout/fragment_start_screen.xml`
  - `app/src/main/res/layout/fragment_app_list.xml`
  - `app/src/main/java/com/example/violet/ui/LauncherActivity.java`
  - `app/src/main/java/com/example/violet/ui/LauncherPagerAdapter.java`
- **Actions Completed:**
  1. Configured `ViewPager2` in `LauncherActivity` hosting Start Screen (Page 0) and App List (Page 1).
  2. Handled hardware Back button: switches back to Start screen if on App list, or scrolls to top.
  3. Handled Home button (`onNewIntent`): switches to Start screen and scrolls to top.

---

### Task 2.3: Start Screen Grid & Tile Rendering
- **Files:**
  - `app/src/main/res/drawable/bg_tile.xml`
  - `app/src/main/res/layout/item_tile.xml`
  - `app/src/main/java/com/example/violet/ui/start/TileAdapter.java`
- **Actions Completed:**
  1. 4-column `GridLayoutManager` with dynamic `SpanSizeLookup`.
  2. Dynamically computed proportional height (`unitPx`) for square Small tiles, 2:1 Wide tiles, and full-width Large tiles.
  3. Windows Phone Metro aesthetic: solid accent background (`wp_violet`), centered icons, bottom-left labels, touch ripple feedback.

---

### Task 2.4: Pin, Unpin & Tile Resizing
- **Files:**
  - `app/src/main/java/com/example/violet/ui/apps/AppListAdapter.java`
  - `app/src/main/java/com/example/violet/ui/start/TileAdapter.java`
  - `app/src/main/res/values/strings.xml`
- **Actions Completed:**
  1. Added long-press action in `AppListAdapter` with "Pin to Start" (or "Unpin from Start" if already pinned) and "App info".
  2. Added long-press action on Start Screen tiles with "Resize" (cycle Small ↔ Wide ↔ Large), "Unpin from Start", and "App info".
  3. Dynamic UI updates and immediate persistence to `SharedPreferences`.

---

### Task 2.5: Default Out-of-the-Box Tile Layout
- **Files:**
  - `app/src/main/java/com/example/violet/data/TileRepository.java`
- **Actions Completed:**
  1. Automatic resolution and pinning of key system applications on first launch: Phone (Wide), Messages (Small), Camera (Small), Browser (Wide), Gallery (Small), Settings (Small).
  2. Stored initialization flag in `SharedPreferences` to only run once.

---

## 4. Verification & Testing

- **Build Sanity:**
  - Ran `./gradlew assembleDebug`
  - Result: **BUILD SUCCESSFUL in 2s** (0 errors).
- **Artifact:**
  - Output APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## 5. Definition of Done (DoD)

- [x] Two-page horizontal viewport (Start Screen ↔ App List) working with back/home navigation.
- [x] Responsive tile grid supporting Small, Wide, and Large tile layouts.
- [x] Authentic Windows Phone flat tile design with accent color and bottom-left labels.
- [x] Pin to Start from App List.
- [x] Unpin and Resize from Start Screen.
- [x] Tile configuration persisted to `SharedPreferences` via JSON.
- [x] `./gradlew assembleDebug` compiles cleanly.
