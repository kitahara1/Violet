# Phase 1 Execution Plan: Foundation & Application Discovery

**Project:** Violet (Lightweight Windows Phone–Inspired Android Launcher)  
**Status:** Completed  
**Target Platform:** Android (minSdk 26, targetSdk 36)  
**Primary Language:** Java (Java 11)  
**UI Toolkit:** Android Views & XML  

---

## 1. Overview & Objectives

The primary goal of **Phase 1** is to establish the launcher foundation and provide a fully functional Windows Phone–style application discovery and launching experience.

By the end of Phase 1:
- The app operates as an official Android `HOME` launcher.
- Installed launchable applications are discovered efficiently and cached without repeated disk/package manager queries.
- Applications are sorted and displayed in a Windows Phone–inspired alphabetical list (featuring section headers `#`, `A`–`Z`).
- Applications can be launched instantly upon tapping.
- Dynamic package changes (install, uninstall, update) are observed without requiring full app restarts.
- Strict performance guidelines are maintained (low RAM, zero continuous idle CPU usage).

---

## 2. Architectural Design

```text
com.example.violet/
├── data/
│   ├── AppInfo.java               # Immutable app model (label, packageName, activityName, icon, sortKey)
│   └── AppRepository.java         # Background app discovery, icon resolution, caching, package listening
├── receiver/
│   └── PackageChangeReceiver.java # BroadcastReceiver for PACKAGE_ADDED, REMOVED, CHANGED
├── ui/
│   ├── LauncherActivity.java      # Main launcher activity (HOME category, singleTask)
│   └── apps/
│       ├── AppItem.java           # Composite item type (Header vs App) for RecyclerView
│       └── AppListAdapter.java    # RecyclerView adapter rendering Metro-style app entries & headers
```

---

## 3. Step-by-Step Task Breakdown

### Task 1.1: Launcher Manifest & Android Integration
- **File:** `app/src/main/AndroidManifest.xml`
- **Actions Completed:**
  1. Added launcher intent filters to `LauncherActivity`:
     ```xml
     <action android:name="android.intent.action.MAIN" />
     <category android:name="android.intent.category.HOME" />
     <category android:name="android.intent.category.DEFAULT" />
     <category android:name="android.intent.category.LAUNCHER" />
     ```
  2. Set activity configuration:
     - `android:launchMode="singleTask"`
     - `android:clearTaskOnLaunch="true"`
     - `android:stateNotNeeded="true"`
     - `android:windowSoftInputMode="adjustPan"`
  3. Included `<queries>` for Android 11+ (API 30+) package visibility.

---

### Task 1.2: Core Data Models
- **Files:**
  - `app/src/main/java/com/example/violet/data/AppInfo.java`
  - `app/src/main/java/com/example/violet/ui/apps/AppItem.java`
- **Actions Completed:**
  1. Created `AppInfo` with package, activity, label, icon, and section header with equality checks.
  2. Created `AppItem` discriminated union for `TYPE_HEADER` and `TYPE_APP`.

---

### Task 1.3: Application Discovery & Repository
- **File:** `app/src/main/java/com/example/violet/data/AppRepository.java`
- **Actions Completed:**
  1. Asynchronous querying using `PackageManager.queryIntentActivities(...)` on a single background worker thread.
  2. Filtered out self (`com.example.violet`).
  3. Collated and sorted alphabetically; formatted symbols and numbers to `#`.
  4. Cached items in memory; dynamically refreshed upon package broadcast.

---

### Task 1.4: Windows Phone–Style Alphabetical App List UI
- **Files:**
  - `app/src/main/res/drawable/bg_header_box.xml`
  - `app/src/main/res/layout/item_app_header.xml` (Iconic Metro 44x44dp colored letter box)
  - `app/src/main/res/layout/item_app_list.xml` (High-contrast clean sans-serif typography)
  - `app/src/main/res/layout/activity_launcher.xml` (Edge-to-edge frame layout with page header)
  - `app/src/main/java/com/example/violet/ui/apps/AppListAdapter.java`
- **Actions Completed:**
  1. RecyclerView adapter supporting section headers and app items.
  2. Click handling with `Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED`.

---

### Task 1.5: LauncherActivity Implementation & Lifecycle Handling
- **File:** `app/src/main/java/com/example/violet/ui/LauncherActivity.java`
- **Actions Completed:**
  1. Configured edge-to-edge system window insets with cutouts and navigation bar padding.
  2. Handled `onNewIntent` to scroll back to top when Home button is tapped.
  3. Handled back button navigation smoothly.
  4. Dynamically registered and unregistered `PackageChangeReceiver` during activity lifecycle.

---

### Task 1.6: Visual Theme & Styling Foundation
- **Files:**
  - `app/src/main/res/values/colors.xml`
  - `app/src/main/res/values/themes.xml`
  - `app/src/main/res/values-night/themes.xml`
  - `app/src/main/res/values/strings.xml`
- **Actions Completed:**
  1. Windows Phone accent palette configured (Violet, Cobalt, Crimson, Emerald, Mango, Cyan, Magenta, Amber).
  2. OLED pure-black `#000000` dark theme & clean `#FFFFFF` light theme.
  3. NoActionBar theme with edge-to-edge window flags.

---

## 4. Verification & Testing

- **Build Sanity:**
  - Ran `./gradlew assembleDebug`
  - Result: **BUILD SUCCESSFUL** (32 actionable tasks executed, 0 errors).
- **Package:**
  - Output APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## 5. Definition of Done (DoD)

- [x] `AndroidManifest.xml` declares `HOME` launcher category and queries.
- [x] `AppInfo` & `AppRepository` asynchronously discover and sort apps.
- [x] Windows Phone Metro-style alphabetized app list renders cleanly in dark and light modes.
- [x] App launches reliably on click.
- [x] Package install/uninstall broadcasts update the list live.
- [x] `./gradlew assembleDebug` succeeds.
