# Omni Launcher

Omni Launcher is a Lawnchair 15 / AOSP Launcher3 fork, focused on responsive home returns and low-memory Android devices.

Taking Launcher3—Android’s default home app—as a starting point, it ports Pixel Launcher features and introduces rich customization options.

This branch houses the codebase of Omni Launcher, which is based on Launcher3 from Android 15.

## Features

-   **Material You Theming:** Adapts to your wallpaper and system theme.
-   **At a Glance Widget:** Displays information *at a glance* with support for [Smartspacer](https://github.com/KieronQuinn/Smartspacer).
-   **QuickSwitch Support:** Integrates with Android Recents on Android 10 and newer. (requires root)
-   **Global Search:** Allows quick access to apps, contacts, and web results from the home screen.
-   **Customization Options:** Provides options to tweak icons, fonts, and colors to your liking.
-   And more!

## Build an unsigned APK for local signing

From a checkout with Android SDK build tools and JDK 21:

```bash
./gradlew assembleLawnWithQuickstepGithubRelease
find build/outputs/apk -name 'OmniLauncher.*.github.release.apk'
```

Or run **Build unsigned Omni Launcher release APK** in GitHub Actions. It verifies
that the release artifact is **not** signed and uploads the APK. Signing keys
must stay on your own device; there is intentionally no CI debug-signing fallback.
Debug APKs remain debug-signed for installation during development.

The package ID is currently `app.lawnchair`; changing it or the signing
certificate changes Android update compatibility. Always keep a backup of
your launcher layout before changing the installed build.

## Device and Quickstep limitations

A third-party launcher cannot replace an OEM's SystemUI/Recents implementation
just by declaring privileged permissions. On Android 11, QuickSwitch/Quickstep
integration requires a compatible rooted/system-level setup; ordinary unsigned
installation does **not** grant it. See
[home-return and Recents diagnostics](docs/home-return-recents-diagnostics.md)
for reproducible landscape/game stress tests.

## Copyright & License

Lawnchair and Omni Launcher are licensed under the Apache License 2.0.

Lawnchair is a free, open-source home app for Android. Omni Launcher is a fork of Lawnchair 15.
Modifications and specific Omni Launcher features are provided under the same Apache 2.0 license.

See [LICENSE.txt](LICENSE.txt) for the full license text.
