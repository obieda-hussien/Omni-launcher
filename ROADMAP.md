# Omni Launcher roadmap

Base: Lawnchair 15 / Android Launcher3. All inherited AOSP and Lawnchair notices
remain applicable. This plan supersedes the inherited Lawnchair product roadmap
for the Omni fork; it does not represent promises by the upstream project.

## P0 — launcher reliability
- Keep wallpaper decode, blur and heavy I/O out of launch/return-to-home.
- Bound image jobs and respond to memory pressure.
- Measure cold start, warm return from heavy games, rotation and process death on
  Infinix HOT 10S (Android 11, 4 GB RAM) before claiming performance wins.
- Diagnose Recents separately from the launcher: OEM SystemUI owns the hardware
  navigation button unless Quickstep is installed as the privileged Recents component.

## P1 — coherent Omni UX
- Unify visible application name, Arabic translations, onboarding and accessibility.
- Preserve Launcher3 hot paths instead of performing an all-at-once Java/Compose rewrite.

## P3 — offline intelligence
- Prioritize local app search and robust Arabic spelling normalization.
- Add optional suggestions only with explicit user choice; never block home startup
  on internet, a local LLM or the Omni Dev app.

## P4 — maintenance
- CI provides unsigned release artifacts only; signing remains local.
- Preserve base attribution, test execution visibility, and independently verify
  OEM navigation behavior rather than treating an APK build as a device test.

See [recovery guide](docs/omni-recovery.md) for reproducible Recents and game-return diagnostics.
