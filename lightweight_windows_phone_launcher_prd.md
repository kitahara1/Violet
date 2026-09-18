# Lightweight Windows Phone–Inspired Android Launcher
## Product Requirements Document (PRD)

**Status:** Draft  
**Version:** 0.1  
**Platform:** Android  
**Primary Language:** Java  
**UI Technology:** Android Views + XML  
**Future Feature:** Lightweight Live Tiles

---

## 1. Product Overview

This project is a lightweight Android launcher inspired by the design language and interaction model of **Windows Phone / Windows 10 Mobile**.

The launcher is intended to provide a fast, simple, visually distinctive home-screen experience while keeping memory usage, CPU usage, background activity, APK size, and implementation complexity as low as reasonably possible.

The first release will focus on the essential responsibilities of a launcher:

- Acting as the Android Home application.
- Displaying a customizable Start screen.
- Displaying installed/launchable applications.
- Launching applications.
- Providing a Windows Phone–inspired tile-based interface.
- Persisting the user's layout and basic preferences.

**Live Tiles are explicitly deferred from the initial release.** However, the initial tile architecture must leave room for adding Live Tiles later without requiring a fundamental redesign.

---

## 2. Product Goals

### 2.1 Primary Goals

1. **Very low resource usage**
   - Minimize RAM consumption.
   - Minimize CPU usage while idle.
   - Avoid unnecessary background services.
   - Avoid unnecessary third-party dependencies.
   - Avoid continuously running animations.

2. **Fast interaction**
   - Fast launcher startup.
   - Fast application launching.
   - Smooth scrolling through the application list.
   - Minimal latency when switching between Start screen and app list.

3. **Windows Phone–inspired UX**
   - Tile-based Start screen.
   - Strong typography.
   - Flat visual design.
   - Accent colors.
   - Dark/light themes.
   - Alphabetical application list.
   - Minimal UI chrome.

4. **Simple implementation**
   - Java.
   - Android SDK APIs where practical.
   - XML layouts / Android Views.
   - Avoid unnecessary frameworks and services.

5. **Extensible architecture**
   - The first version should be simple.
   - Future features such as Live Tiles, icon customization, and additional launcher functionality should be possible without rewriting the core.

---

## 3. Non-Goals

The following are **not** goals for the initial release:

- Recreating every feature of Windows Phone.
- Implementing full Android widget compatibility.
- Building a complete notification-management system.
- Cloud synchronization.
- User accounts.
- Analytics.
- Advertising.
- Launcher-specific social features.
- Heavy visual effects.
- Animated wallpapers.
- A custom Android OS.
- Replacing Android's notification shade, quick settings, or system navigation.
- Implementing Live Tiles in version 1.

---

## 4. Design Philosophy

The launcher should follow this principle:

> **If a feature does not provide substantial value, it should not consume resources.**

The launcher should be closer to a small system utility than a feature-heavy customization suite.

### Preferred characteristics

- Flat UI.
- Simple geometry.
- Minimal shadows.
- Minimal blur.
- Minimal transparency.
- Minimal animation.
- Static rendering where possible.
- Small amount of persistent state.
- No unnecessary background processing.

### Avoid

- Always-running services.
- Large UI frameworks.
- Excessive dependencies.
- Continuous polling.
- Complex databases for simple settings.
- Heavy animations.
- Embedded web content.
- Telemetry unless explicitly introduced later.

---

# 5. Target Users

## Primary User

Android users who:

- Prefer minimalist interfaces.
- Want an alternative to conventional Android launchers.
- Like the Windows Phone / Windows 10 Mobile visual style.
- Value low resource usage.
- Prefer a launcher that "just works."

## Secondary User

Users with:

- Older Android devices.
- Limited RAM.
- Lower-end CPUs.
- A preference for simple software.

---

# 6. Platform Requirements

### Initial platform

- Android smartphone.
- Java application.
- Android Views/XML.
- Support a reasonably broad Android version range where practical.

### Launcher requirements

The application must declare itself as a Home application using:

```xml
<action android:name="android.intent.action.MAIN" />
<category android:name="android.intent.category.HOME" />
<category android:name="android.intent.category.DEFAULT" />
```

The launcher must be selectable as the device's default Home application.

---

# 7. Core User Experience

## 7.1 Start Screen

The Start screen is the primary interface.

It consists primarily of tiles arranged in a configurable grid.

Example:

```text
┌───────────────────────────────┐
│ Start                         │
│                               │
│ ┌───────────┐ ┌─────┐ ┌─────┐ │
│ │           │ │     │ │     │ │
│ │           │ │ SMS │ │Music│ │
│ │   Phone   │ │     │ │     │ │
│ │           │ │     │ │     │ │
│ └───────────┘ └─────┘ └─────┘ │
│                               │
│ ┌─────┐ ┌───────────┐         │
│ │Maps │ │  Browser  │         │
│ └─────┘ └───────────┘         │
│                               │
└───────────────────────────────┘
```

The exact visual design is subject to UI iteration.

---

## 7.2 Tile Sizes

The initial launcher should support at least three tile sizes:

### Small

```text
┌──────┐
│      │
│ App  │
│      │
└──────┘
```

### Wide

```text
┌──────────────┐
│              │
│     App      │
│              │
└──────────────┘
```

### Large

```text
┌──────────────┐
│              │
│              │
│     App      │
│              │
│              │
└──────────────┘
```

The implementation should use a grid-based layout system so that tile positions and dimensions can be persisted efficiently.

---

# 8. Application Discovery

The launcher must discover applications that can be launched from the Android Home screen.

Use Android's package/activity APIs, primarily:

- `PackageManager`
- `Intent`
- `ResolveInfo`

Conceptually:

```java
Intent intent = new Intent(Intent.ACTION_MAIN);
intent.addCategory(Intent.CATEGORY_LAUNCHER);

List<ResolveInfo> apps =
        getPackageManager().queryIntentActivities(intent, 0);
```

The launcher should collect, as needed:

- Package name.
- Activity information.
- Application label.
- Application icon.

Application discovery should not occur repeatedly during normal UI rendering.

---

# 9. Application Launching

Tapping a tile or application entry must launch the corresponding application.

The launcher should use the appropriate Android package/activity intent rather than maintaining its own application execution mechanism.

Basic behavior:

1. User taps application.
2. Launcher resolves the application launch intent.
3. Launcher starts the activity.
4. Launcher leaves the foreground.

The launcher should not remain unnecessarily active in the foreground or maintain unnecessary work after launching another application.

---

# 10. Application List

The application list should be inspired by Windows Phone's alphabetical application list.

Example:

```text
APPS

#

    7-Zip
    ADB

C

    Calculator
    Camera
    Chrome
    Contacts

F

    Files

M

    Maps
    Messages
    Music
```

### Requirements

- Alphabetical sorting.
- Fast scrolling.
- Application icons.
- Application names.
- Tap to launch.
- Search may be added after the MVP.

---

# 11. Navigation

The initial interaction model should be simple.

Possible model:

```text
Start Screen
     │
     │ swipe / navigation gesture
     ▼
Application List
     │
     │ tap
     ▼
Application
```

The exact gesture implementation should be evaluated during prototyping.

The launcher should avoid implementing complicated gesture systems unless they provide clear value.

---

# 12. Customization

The first release should provide basic customization.

### Required

- Add application to Start screen.
- Remove application from Start screen.
- Move tiles.
- Change tile size.
- Change accent color.
- Switch between dark and light themes.

### Potential future features

- Custom tile labels.
- Custom icons.
- Tile grouping.
- Wallpaper options.
- Additional tile sizes.
- Advanced layout configuration.

---

# 13. Persistence

The launcher needs to persist:

- Tile positions.
- Tile dimensions.
- Applications assigned to tiles.
- Theme.
- Accent color.
- Basic launcher preferences.

For simple configuration, prefer lightweight storage such as:

- `SharedPreferences`, or
- another lightweight Android-supported preference mechanism if justified later.

A database should **not** be introduced merely for storing a small launcher layout.

---

# 14. Architecture

The initial architecture should remain intentionally small.

Suggested structure:

```text
app/
└── src/main/
    ├── java/
    │   └── .../
    │       ├── LauncherActivity.java
    │       ├── AppInfo.java
    │       ├── AppRepository.java
    │       ├── Tile.java
    │       ├── TileView.java
    │       ├── TileRepository.java
    │       └── LauncherPreferences.java
    │
    └── res/
        ├── layout/
        ├── drawable/
        ├── mipmap/
        ├── values/
        └── xml/
```

This is a starting point, not a rigid requirement.

---

# 15. Proposed Components

## LauncherActivity

Responsible for:

- Serving as the launcher entry point.
- Managing the primary launcher UI.
- Switching between Start screen and application list.
- Handling lifecycle events.

## AppRepository

Responsible for:

- Discovering launchable applications.
- Loading application metadata.
- Maintaining the application list during the launcher session.

## AppInfo

Represents an application:

```text
packageName
activityName
label
icon
```

## Tile

Represents a Start screen tile:

```text
packageName
position
width
height
type
```

The `type` field should allow future differentiation between static and Live Tiles.

For example:

```text
STATIC
LIVE
```

The Live Tile implementation itself is deferred.

## TileView

Responsible for drawing an individual tile.

The initial implementation should favor direct Android View rendering where practical.

## TileRepository

Responsible for:

- Loading the saved Start screen layout.
- Saving layout changes.
- Adding/removing tiles.
- Updating tile positions.

## LauncherPreferences

Responsible for lightweight settings such as:

- Theme.
- Accent color.
- Miscellaneous launcher preferences.

---

# 16. Future Live Tile Architecture

Live Tiles are explicitly out of scope for the MVP but must be considered during architecture design.

A future tile model could conceptually look like:

```text
Tile
├── package
├── position
├── size
├── type
│   ├── STATIC
│   └── LIVE
└── provider
```

A Live Tile may eventually contain:

```text
LiveTile
├── content
├── update mechanism
├── update policy
└── click action
```

The goal is **not** to embed full Android widgets into the Start screen.

Instead, the launcher should investigate a lightweight model in which applications or launcher-owned providers supply small amounts of display content.

Possible future examples:

```text
Weather
┌──────────────┐
│ WEATHER      │
│              │
│     28°      │
│   Bandung    │
└──────────────┘
```

```text
Messages
┌──────────────┐
│ MESSAGES     │
│              │
│ 3 new        │
│ messages     │
└──────────────┘
```

```text
Clock
┌──────────────┐
│              │
│    22:41     │
│   Thursday   │
└──────────────┘
```

Live Tile updates must respect modern Android background-execution restrictions.

No assumption should be made that the launcher can run arbitrary code continuously in the background.

---

# 17. Performance Requirements

Performance is a first-class product requirement.

## Memory

The launcher should minimize:

- Java heap allocations.
- Native memory usage.
- Large cached images.
- Duplicate application icons.
- Unnecessary object retention.

Application icons should be cached carefully and only when useful.

## CPU

When idle, the launcher should perform essentially no continuous work.

Avoid:

```text
while(true)
poll()
update()
poll()
update()
```

unless a future feature explicitly requires it and Android permits it.

## Startup

The launcher should:

1. Display the basic UI quickly.
2. Load the saved Start screen.
3. Avoid blocking the first frame on unnecessary work.
4. Load application metadata efficiently.

## Battery

The launcher should not:

- Poll continuously.
- Maintain unnecessary wake locks.
- Run background services without a strong reason.

---

# 18. Dependency Policy

The project should use as few external dependencies as reasonably possible.

Every dependency should be evaluated based on:

- Functionality provided.
- APK size.
- Runtime memory.
- Initialization cost.
- Maintenance cost.

Prefer Android platform APIs when they are sufficient.

---

# 19. Visual Design

## Design language

Inspired by Windows Phone / Windows 10 Mobile:

- Flat surfaces.
- Rectangular tiles.
- Strong typography.
- Large headings.
- High contrast.
- Minimal borders.
- Minimal shadows.
- Accent-color-driven UI.
- Dark and light themes.

The design should be **inspired by**, rather than attempting to reproduce proprietary Microsoft assets exactly.

## Typography

Typography should be prominent.

The launcher should favor:

- Large section titles.
- Clear application names.
- Strong visual hierarchy.
- Minimal decorative elements.

---

# 20. Accessibility

The launcher should support basic Android accessibility behavior.

At minimum:

- Tiles must expose meaningful content descriptions.
- Application names should be readable by accessibility services.
- Touch targets must remain usable.
- Text should respect Android font scaling where practical.

---

# 21. Security & Privacy

The launcher should collect no user data by default.

No:

- Account system.
- Analytics.
- Advertising.
- Cloud telemetry.

Application information should remain local to the device.

---

# 22. MVP Scope

The MVP is complete when the launcher can:

- [ ] Install as a normal Android application.
- [ ] Appear as a selectable Home application.
- [ ] Become the default launcher.
- [ ] Display a Start screen.
- [ ] Discover launchable applications.
- [ ] Display applications in an app list.
- [ ] Launch applications.
- [ ] Add applications to the Start screen.
- [ ] Remove applications from the Start screen.
- [ ] Move tiles.
- [ ] Resize tiles between supported sizes.
- [ ] Persist tile layout.
- [ ] Persist basic settings.
- [ ] Support dark/light themes.
- [ ] Support an accent color.
- [ ] Provide basic Windows Phone–inspired visual styling.
- [ ] Avoid unnecessary persistent background activity.

---

# 23. Post-MVP Roadmap

## Phase 1 — Foundation

- Android project setup.
- Java application.
- Launcher manifest.
- Basic `LauncherActivity`.
- Default launcher registration.

## Phase 2 — Application Discovery

- Query launchable applications.
- Create `AppInfo`.
- Sort applications.
- Launch applications.

## Phase 3 — Start Screen

- Tile model.
- Tile grid.
- Tile rendering.
- Add/remove tiles.
- Tile persistence.

## Phase 4 — UX

- App list navigation.
- Tile editing.
- Drag/move support.
- Theme support.
- Accent colors.
- Animation polish.

## Phase 5 — Optimization

Measure and optimize:

- PSS.
- Java heap.
- Native heap.
- Startup time.
- CPU usage.
- Idle behavior.
- Scrolling performance.
- APK size.

Optimization should be based on measurements rather than assumptions.

## Phase 6 — Live Tiles

Research and prototype:

- Live Tile provider model.
- Lightweight update mechanism.
- Update scheduling.
- Tile content format.
- Security boundaries.
- Background execution constraints.

Only after the foundation is stable should Live Tiles become part of the production launcher.

---

# 24. Testing

Testing should cover:

### Functional

- Application discovery.
- Application launching.
- Tile creation.
- Tile deletion.
- Tile movement.
- Tile resizing.
- Persistence.
- Theme changes.
- Default launcher behavior.

### Device

Test on:

- Low-RAM device.
- Mid-range device.
- Modern Android device.

### Performance

Record:

- Cold startup time.
- Warm startup time.
- Idle RAM/PSS.
- RAM after opening the app list.
- RAM after launching several applications.
- CPU usage while idle.
- CPU usage during scrolling.
- APK size.

---

# 25. Success Criteria

The project succeeds if it provides a launcher that feels:

**Fast + simple + lightweight + Windows Phone–inspired.**

Performance should be treated as a measurable property rather than a marketing claim.

The project should prioritize:

```text
Low resource usage
        ↓
Simple architecture
        ↓
Fast interaction
        ↓
Distinctive visual design
        ↓
Optional advanced features
```

rather than:

```text
Feature count
        ↓
Complexity
        ↓
Resource usage
```

---

# 26. Open Questions

These should be resolved during implementation rather than prematurely locking the architecture:

1. What Android API level should be the minimum supported version?
2. Should the Start screen scroll vertically, horizontally, or use a fixed viewport?
3. How closely should tile dimensions follow Windows Phone's original proportions?
4. Should the app list be a separate Activity, Fragment, or View state?
5. Should tile drag-and-drop use a custom implementation or Android's existing interaction APIs?
6. How should launcher icon caching be implemented for minimum memory usage?
7. Should the launcher support Android widgets at all?
8. What should the future Live Tile provider API look like?
9. How should Live Tile updates work within Android's modern background restrictions?
10. What is the target RAM budget for the launcher?

---

# 27. Guiding Principle

> **Build the smallest launcher that feels complete.**

Everything else—including Live Tiles—comes after the core experience is fast, reliable, and pleasant to use.
