# MacroMate — Android macro recorder starter project

A minimal Android Studio project in Kotlin. It supports:
- recording accessible button click/long-click events where Android exposes them;
- adding taps manually by screen coordinates;
- per-action delay in milliseconds;
- sequential playback, repeat count, stop, save and load.

## Build
1. Install Android Studio (current stable) and Android SDK Platform 35.
2. Open this folder as a project.
3. Let Gradle sync, then Build > Build APK(s).
4. Install the debug APK on your own device.

## First launch
1. Open MacroMate.
2. Tap **Open accessibility settings**.
3. Enable **MacroMate automation**.
4. Return to the app and record or add actions.

## Important limitations
- Android does not provide a universal way for ordinary apps to record every arbitrary touch in every other app. The recorder captures accessible click events only. Many games draw their own controls and do not expose click events, so use manual X/Y coordinates for those.
- Coordinates are screen pixels and can change with screen resolution, orientation, display scaling, or game layout.
- Keep the app visible while editing the sequence. This starter version does not include a floating overlay or touch-exploration-based raw touch capture.
- Accessibility permission is powerful. Enable it only for an app you trust, and turn it off in Settings when you are done.
- Use only where automation is permitted by the app/game rules. It is not a guarantee of identical timing; Android scheduling and app performance can vary.
- This is source code, not a prebuilt APK. Build it with Android Studio.


## Floating overlay (added)
- Tap **Enable floating overlay** in the main app.
- Allow **Display over other apps** when Android opens that settings page.
- A draggable panel appears over other apps with Record, Stop record, Play saved, and Hide controls.
- The panel can be moved by dragging its header.

Note: the overlay's **Play saved** button opens MacroMate; in this starter version, start playback from the main screen after loading the macro. Full playback directly from the overlay and persistent foreground-service behavior are follow-up improvements. Android may restrict overlays in some secure apps.
