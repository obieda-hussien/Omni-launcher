# Omni Launcher: game-return and Recents recovery

## Two independent paths

1. **Home recreation after a heavy game:** Android may reclaim the launcher
   process while the game consumes memory. The new wallpaper effect runs on a
   single worker at bounded resolution; no GPU blur runs during root-view creation.
   This cannot prevent the OS from killing the process.
2. **Recent Apps button / landscape failure:** On OEM Android 11, navigation is
   normally handled by SystemUI (and the device's Recents component), not an
   arbitrary installed home launcher. Quickstep as a privileged Recents provider
   needs platform/OEM integration. Do not use root shell restarts or self-restart
   loops to mask a stuck navigation button.

## Reproduction matrix (physical device)

- With Omni set as default HOME, launch a large game, rotate landscape, then
  navigate HOME and Recents. Repeat 10 times with blur on/off.
- Compare with the stock Infinix launcher as default HOME.
- Repeat in portrait and with 3-button vs gesture navigation, when supported.
- Record whether HOME still works and whether only Recents is unresponsive.
- If Recents also fails under the stock launcher, investigate SystemUI/OEM firmware
  rather than making speculative Launcher3 changes.

## Capture logs without restarting the device first

From a computer with ADB authorization:

```sh
adb shell dumpsys meminfo app.lawnchair
adb shell dumpsys activity activities
adb logcat -d -v time -s LawnchairApp OmniWallpaperBlur OmniRecents TouchInteractionService OverviewCommandHelper ActivityTaskManager
```

Keep the log private if it includes installed application names or activities.
A successful APK build is NOT evidence that OEM Recents integration works.
