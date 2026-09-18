# Live Tiles (Phase 5) Implementation Plan

## 1. Objectives
Implement lightweight, energy-efficient Live Tiles for the Violet Start screen inspired by Windows 10 Mobile and Windows Phone, strictly adhering to the PRD:
- **No full Android widgets / RemoteViews**: Pure lightweight data provider model.
- **Strict Battery & Background Zero-Work Compliance**:
  - Broadcast-driven (`ACTION_TIME_TICK`, `ACTION_BATTERY_CHANGED`) and `NotificationListenerService`-driven updates.
  - Zero idle polling.
  - All flip animations and timers are active **only** while `LauncherActivity` is in the foreground (`onStart()` / `onResume()`). In `onStop()`, all handlers and animators immediately cancel and pause.
- **Authentic Metro 3D Flip & Badge Visuals**:
  - Unread notification badges on tile front face.
  - Staggered 3D Y-axis perspective flip between Front (Icon + Label) and Back (Live data content).
  - Clock / Calendar, Battery, and Notification live tile providers.

---

## 2. Architecture & Components

```text
com.example.violet.live/
├── LiveTileData.java               # Immutable payload (header, primaryText, subtitle, badgeCount, shouldFlip)
├── LiveTileProvider.java           # Contract interface for data providers
├── LiveTileManager.java            # Coordinates active providers, manages foreground lifecycle
├── VioletNotificationService.java   # NotificationListenerService tracking unread active notifications
├── providers/
│   ├── ClockLiveTileProvider.java         # Time (22:41), Day (Thursday), Date (18 Sep) via ACTION_TIME_TICK
│   ├── BatteryLiveTileProvider.java       # Battery %, status, health via ACTION_BATTERY_CHANGED
│   └── NotificationLiveTileProvider.java  # Active notifications, unread counts & snippets
└── anim/
    └── MetroFlipController.java     # Staggered 3D camera rotation between Front & Back tile faces
```

---

## 3. Step-by-Step Execution
1. **Model & Providers**:
   - Create `LiveTileData` and `LiveTileProvider`.
   - Create `VioletNotificationService` and `NotificationLiveTileProvider`.
   - Create `ClockLiveTileProvider` and `BatteryLiveTileProvider`.
   - Create `LiveTileManager` coordinating providers and listener lifecycle.
2. **Layout & Badge Styling**:
   - Update `item_tile.xml` with Front Face (`tile_front_face`, badge `tv_tile_badge`) and Back Face (`tile_back_face`, header, big Segoe text, subtitle).
   - Create `bg_tile_badge.xml` for notification count.
3. **Animation & Binding**:
   - Create `MetroFlipController` with 3D perspective flip and staggered intervals.
   - Update `TileAdapter` and `TileViewHolder` to bind `LiveTileData` and attach flip animations when `tile.getType() == TileType.LIVE` or provider data is available.
4. **Settings & Permissions**:
   - Add notification listener permission check and toggle in `SettingsDialog` so users can enable Notification Live Tiles in Android Settings with one tap.
   - Register `VioletNotificationService` in `AndroidManifest.xml`.
5. **Compilation & Unit Testing**:
   - Add tests for `LiveTileData` and providers.
   - Verify `./gradlew assembleDebug` and `./gradlew test`.
