# Home-return and Recents diagnostics (Android 11 / Infinix HOT 10S)

## Scope

This fork provides an ordinary user-installed launcher APK, **not** a privileged
SystemUI replacement. Launcher UI and OEM Recent Apps are different components.
A black home frame after a game can be a Launcher process restart under memory
pressure, UI-thread work, or a window transition. A permanently inert Recents
button while Home still works may be a Quickstep/SystemUI/OEM navigation issue.
Treat them as separate failures; neither a successful build nor an app restart
proves the OEM Recents problem is fixed.

## Safe reproduction matrix

Keep one alternative launcher available, export a layout backup, then compare:

| Case | Repeat | Record |
|---|---:|---|
| Home from a normal app | 10x | First visible home frame, whether icons are ready |
| Home from a heavy game | 10x | Black-screen duration, launcher process PID before/after |
| Recents from game portrait | 10x | Overview opens / empty / button inert |
| Recents from game landscape | 10x | Same, then rotate to portrait and retry |
| Recents after launcher process reclaim | 5x | Launcher and SystemUI PIDs; process death reason |
| Background blur off vs on (MID/HIGH) | 10x | p50/p95 frame time and RSS |

Device class LOW intentionally bypasses the optional wallpaper blur. Never
force-stop SystemUI, change privileged settings, or grant hidden permissions
as an automated recovery strategy; an OEM Recents implementation may require
a compatible rooted/system-level QuickSwitch integration.

## Capture evidence (from a connected computer with ADB)

Run immediately after a failure, before rebooting. Redact account names,
notification contents and unrelated app data before sharing logs.

```bash
adb shell getprop ro.build.version.release
adb shell dumpsys activity processes > processes.txt
adb shell dumpsys activity activities > activities.txt
adb shell dumpsys meminfo app.lawnchair > launcher-meminfo.txt
adb shell dumpsys window > window-state.txt
adb logcat -b main -b system -b crash -d -v threadtime > home-recents-logcat.txt
```

For a time-windowed reproduction, clear only logcat, reproduce once, then dump.
Look for `am_proc_died`, `lmkd`, `ActivityTaskManager`, `TouchInteractionService`,
`RecentsAnimation`, `SystemUI`, `ANR`, `LauncherRootView`, and `OmniWallpaperBlur`.
Compare the launcher PID before/after the game: PID change is evidence of a
process restart, not proof of what caused the kill. Inspect the corresponding
LMKD and activity-manager lines before attributing the cause.

Use Android Studio/Perfetto and Macrobenchmark on the **physical phone** to
measure warm home return, first draw, full icon population, scroll jank, and
the landscape Recents transition. GitHub Actions cannot reproduce this OEM
failure reliably; emulator success is not device evidence.

## What the September 2026 first-pass patch does / does not do

- Removes full-resolution wallpaper rendering and synchronous OpenGL blur
  from `LauncherRootView` construction.
- Uses a bounded one-worker queue and rejects stale view results.
- Uses a conservative LOW-tier fallback and a no-blur LOW path.
- Produces unsigned GitHub release artifacts for local signing.
- Does **not** claim to repair OEM SystemUI, a broken navigation-button
  binding, or root/QuickSwitch compatibility. Those require device traces.

## Outstanding acceptance checks

- Verify an actual unsigned release artifact using `apksigner verify`.
- Run style, compile, instrumentation tests and profile the game-to-Home path.
- Compare behaviour with the stock launcher and with QuickSwitch integration
  enabled/disabled where available.
- Test repeated rotations during Recents animation and wallpaper changes.
