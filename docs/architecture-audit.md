# Architecture Audit

This document is an audit of the current repository based on exploring its structure.

## 1. Build Verification

The correct Gradle task for a debug build on the Github channel across the `app`, `recents`, and `channel` flavor dimensions is:
`assembleLawnWithQuickstepGithubDebug`

- `lawn` (app)
- `withQuickstep` (recents)
- `github` (channel)

When resolving these flavors, the build task merges them into the above string, e.g., `assembleLawnWithQuickstepGithubDebug`.

### Build Warnings/Errors

During the build of `assembleLawnWithQuickstepGithubDebug`, we encountered these warnings/errors:

- `Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.`: Standard Gradle deprecation warnings.
- `WARNING: The option setting 'android.builtInKotlin=false' is deprecated.`: Deprecation warning for a gradle.properties property, will be removed in AGP 10.0.
- `WARNING: The option setting 'android.newDsl=false' is deprecated.`: Deprecation warning for a gradle.properties property, will be removed in AGP 10.0.
- `WARNING: The option setting 'android.r8.maxWorkers=4' is experimental.`: Warning from R8.
- R8/AGP warnings from the Baseline Profile Gradle Plugin related to AGP version compatibility.
- `⚠️ Deprecated 'org.jetbrains.kotlin.android' plugin usage`: A warning from multiple subprojects complaining about unnecessary kotlin android plugin usage which is default in newer AGPs.

The build ultimately succeeded.

## 2. Responsibilities of Key Directories

- **`lawnchair/`**: Contains custom code for the launcher itself, including the modern Compose-based UI elements, preferences, custom icon providers, app drawer folder implementations (using Room databases), headless widget handling, and more.
- **`src/`**: A largely unadulterated clone of AOSP Launcher3. Core launcher logic, interaction handling, dragging, and basic UI components exist here.
- **`quickstep/`**: Contains code for Recents/Overview integration, hooking into system-level window management. Highly sensitive to root/Android version.
- **`systemUI/`**: Contains subfolders like `anim`, `common`, `log`, `plugin`, `plugin_core`, `shared`, `unfold`, `viewcapture`. These are system UI dependencies/libraries vendored into the project to support quickstep/recent apps functionality and other system interactions.
- **`flags/` & `aconfig/`**: These folders handle AOSP's modern Aconfig flag system. `aconfig/` contains `.aconfig` definitions, and `flags/` provides generated/custom feature flag Java files for standard launcher domains (e.g. `launcher3`, `systemui`).

## 3. New-to-this-tag Folders

- **`androidx-lib/`**: Contains a minimal Android library configuration that appears to define the `androidx.dynamicanimation.animation` namespace, essentially polyfilling or acting as a custom stub for AndroidX dynamic animation.
- **`nightly/`**: Contains an `AndroidManifest.xml` specific to the "nightly" build flavor. This flavor gets a debug signing configuration for release.
- **`src_no_quickstep/`**: An alternative source directory used when building without Quickstep. It likely contains stub implementations or default behavior that replaces Quickstep-specific logic.
- **`wmshell/`**: A port/vendor of WindowManager Shell (`com.android.wm.shell`) dependencies, needed to interface with recent versions of Android's window management, split screen, and other multi-window features.
- **`fastlane/`**: Used for Fastlane, a popular tool for automating iOS and Android app releases and screenshots. It contains a `metadata` folder.
- **`flowerpot/`**: A python-based tooling environment for processing "flowerpot" files (`.flowerpot`). These are configuration files to categorize apps into folders and tabs. It contains a parser (`merge.py`) and scripts for fetching categories from the Play Store (`playstore.py`).
- **`github/`**: Similar to `nightly/`, this contains an `AndroidManifest.xml` specific to the "github" build channel.
- **`go/`**: Contains `AndroidManifest.xml` and `AndroidManifest-launcher.xml` overrides likely intended for an "Android Go" optimized build, disabling certain heavy features (like `PinItemRequest`).

## 4. Unused/Dead Code and TODOs in `lawnchair/`

There are several outstanding TODO markers in the `lawnchair/` package indicating incomplete features or refactoring needs:

- `lawnchair/src/app/lawnchair/backup/NovaBackupConverter.kt`: `TODO: Lawnchair folder synchronization support`
- `lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt`: `// TODO REMOVE`
- `lawnchair/src/app/lawnchair/util/LawnchairLockedStateController.kt`: `// TODO Implement this, when the app is uninstalled`
- `lawnchair/src/app/lawnchair/util/SmartBorder.kt`: `// TODO: https://mrmans0n.github.io/compose-rules/rules/#avoid-modifier-extension-factory-functions`
- `lawnchair/src/app/lawnchair/ui/placeholder/Placeholder.kt`: `// TODO: need to migrate to Modifier.Node`
- `lawnchair/src/app/lawnchair/ui/preferences/components/layout/PreferenceLayout.kt`: `TODO: use DSL to represent all preferences`
- `lawnchair/src/app/lawnchair/ui/preferences/components/layout/LazyColumnGrid.kt`: `TODO: use [LazyVerticalGrid]`
- `lawnchair/src/app/lawnchair/ui/preferences/components/QuickActionsPreferences.kt`: `TODO migrate from index-based to item (class)-based list sorting`
- `lawnchair/src/app/lawnchair/ui/preferences/navigation/PreferenceNavigation.kt`: `TODO: navigate to nav3`
- `lawnchair/src/app/lawnchair/deck/LawndeckManager.kt`: `// TODO`
- Many `TODO("Not yet implemented")` exceptions in `SmartspaceProvider`, `DeviceProfileOverrides`, `UploaderService`, `PreferenceManager`, `IconOverrideRepository`, `WallpaperService`, `FolderService`, `IconPackProvider`, `IconShapeManager`, `LawnchairLayoutFactory`, `HeadlessWidgetsManager`, `NotificationManager`, `ThemeProvider`, `GoogleFontsListing`, `FontManager`, `FontCache`.

## 5. Baseline Profile Module

The `baseline-profile` module is wired into the build. It contains a standard configuration.
- **Coverage**: Currently, it only covers the most basic startup journey: it presses home and waits for the default activity to start (`pressHome()`, `startActivityAndWait()`).
- **Gaps**: It has empty TODOs suggesting the need to add more advanced journeys, such as waiting for content to asynchronously load, scrolling the app drawer, or navigating to details.

## 6. External Network Calls, Analytics, and Telemetry

There is **no** telemetry or analytics tracking found in the source code.
Network calls are made using Retrofit and OkHttp for specific user-driven features, not startup tracking:
- **Search Providers**: `GoogleService`, `DuckDuckGoService`, `StartPageService`, and `KagiService` make API calls to fetch search suggestions during use.
- **Bug Reporting**: `KatbinService` is used to POST crash logs to a pastebin service (Katbin) when the user initiates a bug report.
- **About Page**: `GithubService` fetches release information from GitHub API.
- **Live Information**: `LiveInformationService` fetches some JSON configuration for dynamic features.
