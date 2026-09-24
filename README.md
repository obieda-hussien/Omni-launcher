# Omni Launcher

Omni Launcher, a Lawnchair 15-based Android launcher focused on stability and performance on low-RAM devices.

Taking Launcher3—Android’s default home app—as a starting point, it ports Pixel Launcher features and introduces rich customization options.

This branch houses the codebase of Omni Launcher, which is based on Launcher3 from Android 15.

## Features

-   **Material You Theming:** Adapts to your wallpaper and system theme.
-   **At a Glance Widget:** Displays information *at a glance* with support for [Smartspacer](https://github.com/KieronQuinn/Smartspacer).
-   **QuickSwitch Support:** Integrates with Android Recents on Android 10 and newer. (requires root)
-   **Global Search:** Allows quick access to apps, contacts, and web results from the home screen.
-   **Customization Options:** Provides options to tweak icons, fonts, and colors to your liking.
-   And more!

## Copyright & License

Lawnchair and Omni Launcher are licensed under the Apache License 2.0.

Lawnchair is a free, open-source home app for Android. Omni Launcher is a fork of Lawnchair 15.
Modifications and specific Omni Launcher features are provided under the same Apache 2.0 license.

See [LICENSE.txt](LICENSE.txt) for the full license text.


## Omni development / local signing

The `15-dev` branch is derived from Lawnchair 15. Android framework/Launcher3
internals intentionally retain their original namespaces and copyright headers.
The GitHub release variant is **unsigned**; install only after signing locally
with your own stable key. Debug variants use the standard Android debug key.
Unsigned builds must not be announced as installable updates.

Build on a Java 21/Android SDK-enabled host:

```sh
./gradlew assembleLawnWithQuickstepGithubRelease
```

The APK is under `build/outputs/apk/` with `.unsigned.apk` in its name.
Backing up launcher data before signing under a different application ID or
certificate is recommended; signing with a different certificate cannot update
an already installed package.

On OEM Android 11 builds, Recents/navigation belong to SystemUI unless
Quickstep has been integrated as the system Recents component. Installing a
third-party launcher alone cannot replace or repair that privileged component.


Backup restoration preserves other databases in the application's data directory.
A wallpaper-only restore does not reset the home layout.
