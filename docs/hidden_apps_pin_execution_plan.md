# Execution Plan: PIN-Protected Hidden Apps

**Project:** Violet (Lightweight Windows Phone–Inspired Android Launcher)  
**Feature:** PIN-Protected Hidden Applications  
**Status:** Completed  
**Target Platform:** Android (minSdk 26, targetSdk 36)  
**Primary Language:** Java (Java 11)  
**UI Toolkit:** Android Views & XML  

---

## 1. Overview & Objectives

The goal of this feature is to allow users to hide sensitive or unwanted applications from the All Apps list and search results, while securing access to hidden apps using a Windows Phone–styled **4-digit PIN lock**.

### Core Requirements:
1. **Hide from App List**: Long-pressing any app in the All Apps list provides a **"Hide app"** option. Hidden apps disappear immediately from the list and search index.
2. **PIN Security (Zero-Dependency)**:
   - Uses Android's standard `java.security.MessageDigest` (SHA-256) with a unique salt to hash the PIN before storing it in `SharedPreferences`.
   - Never stores plain-text PINs.
3. **PIN Entry Experience**:
   - Iconic Metro-styled numeric keypad (0–9, backspace) with 4 dot indicators.
   - Smooth shake/error animation on incorrect PIN.
4. **Hidden Apps Management (Settings)**:
   - In Settings, a dedicated **"hidden apps"** section displays the hidden count (e.g., *"3 apps hidden"*).
   - Tapping prompts for the PIN (or prompts to create one if not yet configured).
   - Once verified, opens the **Hidden Apps Hub** where users can:
     - View all hidden applications with their icons and names.
     - Tap **"Unhide"** to return an app back to the All Apps list.
     - Tap an app directly to launch it securely from inside the vault.
     - Change the PIN.

---

## 2. Architectural Design

```text
com.example.violet/
├── data/
│   └── HiddenAppsManager.java       # SHA-256 PIN hashing, SharedPreferences storage, hide/unhide CRUD
├── ui/
│   ├── apps/
│   │   ├── AppListAdapter.java      # Filters out hidden apps; adds "Hide app" context menu action
│   │   └── HiddenAppsDialog.java    # Hub displaying hidden apps with unhide action and launch capability
│   ├── security/
│   │   └── PinEntryDialog.java      # Metro-styled 4-digit PIN entry, setup, and validation
│   └── settings/
│       └── SettingsDialog.java      # Adds "hidden apps" section launching PIN verification
```

---

## 3. Step-by-Step Task Breakdown

### Task 1: Security & Storage Engine (`HiddenAppsManager.java`)
- **File:** `app/src/main/java/com/example/violet/data/HiddenAppsManager.java`
- **Actions Completed:**
  1. Created singleton `HiddenAppsManager` using private `SharedPreferences` (`violet_hidden_apps`).
  2. Implemented PIN security with `MessageDigest` (SHA-256) + auto-generated salt UUID.
  3. Implemented `Set<String>` hidden package storage and real-time `OnHiddenAppsChangedListener` callbacks.

---

### Task 2: Metro PIN Entry & Setup Dialog (`PinEntryDialog.java`)
- **Files:**
  - `app/src/main/res/layout/dialog_pin_entry.xml`
  - `app/src/main/res/drawable/bg_pin_dot_empty.xml`
  - `app/src/main/res/drawable/bg_pin_dot_filled.xml`
  - `app/src/main/java/com/example/violet/ui/security/PinEntryDialog.java`
- **Actions Completed:**
  1. Designed 3×4 Metro keypad and 4 indicator dots.
  2. Implemented `VERIFY` and `CREATE` (with 2-step confirmation) modes.
  3. Added tactile shake error animation on invalid PIN or PIN mismatch.

---

### Task 3: Hidden Apps Vault Dialog (`HiddenAppsDialog.java`)
- **Files:**
  - `app/src/main/res/layout/dialog_hidden_apps.xml`
  - `app/src/main/res/layout/item_hidden_app.xml`
  - `app/src/main/java/com/example/violet/ui/apps/HiddenAppsDialog.java`
- **Actions Completed:**
  1. Created vault dialog listing all hidden apps with system icons and labels.
  2. Direct tap launches the hidden app privately.
  3. "Unhide" button instantly restores the app to the main All Apps list.
  4. "Change PIN" button allows updating the vault credentials.

---

### Task 4: Integrate "Hide App" in All Apps Context Menu
- **Files:**
  - `app/src/main/java/com/example/violet/ui/apps/AppListAdapter.java`
  - `app/src/main/res/values/strings.xml`
- **Actions Completed:**
  1. Added **"Hide app"** to the long-press options dialog.
  2. Prompts to configure a PIN if one has not been created yet.
  3. Dynamically excludes hidden apps and empty alphabet headers from the list and search index.

---

### Task 5: Settings Integration
- **Files:**
  - `app/src/main/res/layout/dialog_settings.xml`
  - `app/src/main/java/com/example/violet/ui/settings/SettingsDialog.java`
- **Actions Completed:**
  1. Added **"security & privacy"** section with live count badge (`"X apps hidden"`).
  2. Tapping prompts for PIN before opening `HiddenAppsDialog`.

---

## 4. Verification & Testing

- **Build Sanity:**
  - Ran `./gradlew assembleDebug`
  - Result: **BUILD SUCCESSFUL in 2s** (0 errors).
- **Artifact:**
  - Output APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## 5. Definition of Done (DoD)

- [x] `HiddenAppsManager` securely hashes PIN with SHA-256 + salt and persists hidden packages.
- [x] Metro-styled 4-digit PIN keypad dialog with input dots and verification.
- [x] "Hide app" action in All Apps long-press menu.
- [x] Hidden apps excluded from All Apps list, alphabet headers, and search queries.
- [x] Hidden Apps Vault in Settings allowing unhiding, direct launch, and PIN change.
- [x] `./gradlew assembleDebug` compiles cleanly with 0 errors.
