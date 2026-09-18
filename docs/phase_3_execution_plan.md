# Phase 3 Execution Plan: Customization, Drag Reordering & Theme System

**Project:** Violet (Lightweight Windows Phone–Inspired Android Launcher)  
**Status:** Completed  
**Target Platform:** Android (minSdk 26, targetSdk 36)  
**Primary Language:** Java (Java 11)  
**UI Toolkit:** Android Views & XML  

---

## 1. Overview & Objectives

The primary goal of **Phase 3** is to deliver complete user customization, intuitive layout management, and fast navigation as required by Sections 10, 12, and 13 of the PRD.

By the end of Phase 3:
- Users can **drag and drop** tiles to reorder them on the Start Screen using smooth touch gestures (`ItemTouchHelper`), with layout changes persisted automatically.
- Users can select their favorite **Windows Phone Accent Color** from an authentic palette (Cobalt, Crimson, Emerald, Mango, Violet, Cyan, Magenta, Amber, Lime, Teal).
- Tiles and UI elements immediately and dynamically update their background colors to match the selected accent.
- Users can switch between **OLED Pure Black (Dark)**, **Pure White (Light)**, and **System Default** themes.
- Users can quickly **search/filter** applications in the All Apps list with instant real-time typing.
- Users can tap any section header box (`A`, `B`, `#`) to open the iconic Windows Phone **Alphabet Jump Grid**, instantly jumping to that section of the list.
- A minimalist, Metro-styled **Launcher Settings** dialog lets users manage all customization options with 0 bloat.

---

## 2. Architectural Design

```text
com.example.violet/
├── data/
│   ├── LauncherPreferences.java    # Accent color, theme mode, and preferences (SharedPreferences)
│   ├── AccentColor.java            # Enum: 10 authentic Windows Phone colors with hex & int
│   ├── AppRepository.java          # Includes on-demand resolution and caching
│   ├── TileRepository.java         # Tile CRUD and reorder persistence
│   └── Tile.java                   # Model with native JSON serialization
├── ui/
│   ├── LauncherActivity.java       # Coordinates themes and global preferences
│   ├── LauncherPagerAdapter.java   # ViewPager2 adapter handling Start, Apps, search, and jump modals
│   ├── start/
│   │   ├── TileAdapter.java        # 4-column GridLayoutManager adapter with dynamic accent & drag
│   │   └── TileTouchCallback.java  # ItemTouchHelper.Callback for dragging and dropping tiles
│   ├── apps/
│   │   ├── AppListAdapter.java     # Search filter support and header click jump callback
│   │   └── AlphabetJumpDialog.java # Windows Phone 26-letter grid jump modal
│   └── settings/
│       └── SettingsDialog.java     # Metro-styled customization dialog (theme & accent picker)
```

---

## 3. Step-by-Step Task Breakdown

### Task 3.1: Launcher Preferences & Accent Color System
- **Files:**
  - `app/src/main/java/com/example/violet/data/AccentColor.java`
  - `app/src/main/java/com/example/violet/data/LauncherPreferences.java`
- **Actions Completed:**
  1. Defined `AccentColor` enum with 10 authentic Windows Phone colors (`VIOLET`, `COBALT`, `CRIMSON`, `EMERALD`, `MANGO`, `CYAN`, `MAGENTA`, `AMBER`, `LIME`, `TEAL`).
  2. Implemented `LauncherPreferences` for managing accent colors and theme modes (`DARK`, `LIGHT`, `SYSTEM`) via `SharedPreferences`.

---

### Task 3.2: Dynamic Accent Color & Theme Propagation
- **Files:**
  - `app/src/main/java/com/example/violet/ui/start/TileAdapter.java`
  - `app/src/main/java/com/example/violet/ui/apps/AppListAdapter.java`
  - `app/src/main/java/com/example/violet/ui/LauncherActivity.java`
- **Actions Completed:**
  1. Dynamically tinted tile backgrounds with the active accent color using `ColorFilter` in `TileAdapter`.
  2. Dynamically tinted app list section header boxes in `AppListAdapter`.
  3. Integrated `LauncherPreferences.OnPreferencesChangedListener` to update views in real time and applied `AppCompatDelegate.setDefaultNightMode()`.

---

### Task 3.3: Drag-and-Drop Tile Reordering
- **Files:**
  - `app/src/main/java/com/example/violet/ui/start/TileTouchCallback.java`
  - `app/src/main/java/com/example/violet/ui/start/TileAdapter.java`
  - `app/src/main/java/com/example/violet/data/TileRepository.java`
- **Actions Completed:**
  1. Implemented `TileTouchCallback` with 4-directional dragging and tactile visual lift feedback (scale 1.05x, alpha 0.85).
  2. Handled `onItemMove` item swaps in `TileAdapter` and persisted new orders to `TileRepository` on drag completion.

---

### Task 3.4: All Apps List Search & Filtering
- **Files:**
  - `app/src/main/res/layout/fragment_app_list.xml`
  - `app/src/main/res/drawable/bg_search_bar.xml`
  - `app/src/main/java/com/example/violet/ui/apps/AppListAdapter.java`
  - `app/src/main/java/com/example/violet/ui/LauncherPagerAdapter.java`
- **Actions Completed:**
  1. Added Metro search bar with real-time text listener and clear button (`✕`).
  2. Implemented instant filtering and section header regrouping in `AppListAdapter`.

---

### Task 3.5: Alphabet Quick Jump (Iconic WP Letter Grid)
- **Files:**
  - `app/src/main/res/layout/item_alphabet_jump_tile.xml`
  - `app/src/main/res/layout/dialog_alphabet_jump.xml`
  - `app/src/main/java/com/example/violet/ui/apps/AlphabetJumpDialog.java`
- **Actions Completed:**
  1. Built the 4-column modal letter grid (`#` and `A`–`Z`).
  2. Active letters (having apps) highlighted in the active accent color; inactive letters dimmed.
  3. Tapping an active letter scrolls the app list directly to that header.

---

### Task 3.6: Minimalist Metro Settings UI
- **Files:**
  - `app/src/main/res/drawable/ic_metro_settings.xml`
  - `app/src/main/res/layout/item_accent_color.xml`
  - `app/src/main/res/layout/dialog_settings.xml`
  - `app/src/main/java/com/example/violet/ui/settings/SettingsDialog.java`
  - `app/src/main/res/layout/fragment_start_screen.xml`
- **Actions Completed:**
  1. Added gear settings button in Start screen header.
  2. Created Metro `SettingsDialog` supporting OLED Black / Light / System theme toggling and 10-color visual accent picker with instant preview.

---

## 4. Verification & Testing

- **Build Sanity:**
  - Ran `./gradlew assembleDebug`
  - Result: **BUILD SUCCESSFUL in 2s** (0 errors).
- **Artifact:**
  - Output APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## 5. Definition of Done (DoD)

- [x] Tiles can be reordered via drag-and-drop and the new layout is persisted.
- [x] Theme switching (Dark OLED, Light, System) works smoothly.
- [x] Windows Phone Accent Color palette (10 colors) selectable with instant UI reflection.
- [x] App List search bar filters applications in real time.
- [x] Alphabet quick jump dialog scrolls directly to the chosen letter.
- [x] `./gradlew assembleDebug` compiles cleanly with 0 errors.
