# Omni Launcher roadmap

Omni Launcher is an Apache-2.0 fork of Lawnchair 15 / Android 15 Launcher3.
This file tracks **Omni-specific** work; upstream Lawnchair's roadmap remains
at https://github.com/LawnchairLauncher/lawnchair.

## P0 — Home-return performance and stability

- [x] Move optional wallpaper blur out of the root-view constructor and bound
      pending jobs; provide a no-blur LOW-tier path.
- [x] Conservative classification when device memory service is unavailable.
- [ ] Profile game -> Home cold/warm first frame on the physical HOT 10S.
- [ ] Analyze icon and widget population, blocking preference reads and ANRs
      against recorded traces; patch confirmed bottlenecks in separate PRs.
- [ ] Reproduce and isolate landscape / permanently inert Recents with logcat
      and SystemUI / Quickstep state. No unprivileged APK can guarantee an
      OEM SystemUI repair.

## P1 — Core experience

- [x] Omni app branding and Egyptian Arabic translations (previous PRs).
- [x] Preserve upstream copyright in the Arabic translation.
- [ ] Full Android 11 landscape, RTL, widget/dock and accessibility audit.
- [ ] Improve Compose settings and home UI only where that does not regress
      the AOSP Launcher3 fast rendering path.
- [ ] Review package identity and provide backup/migration before changing it.

## P2 — OmniLink (intentionally a separate milestone)

- [ ] Optional, permission-scoped client to Omni Dev Workspace.
- [ ] Keep Home functional with Omni Dev stopped, missing or disconnected.

## P3 — Search, personalization and performance policies

- [x] Normalize Arabic diacritics/hamza/alef variants for fuzzy app matching.
- [ ] Improve local search and app-index caching with benchmarks.
- [ ] Add bounded icon/bitmap cache budgets and trim-memory policies.
- [ ] Make advanced personalization opt-in and avoid startup network traffic.

## P4 — Build, security and release

- [x] Remove release debug-key fallback; verify unsigned CI artifact.
- [x] Remove hardcoded debuggable flag in base Manifest.
- [x] Add bounded Nova backup extraction and forbid XML external entities.
- [x] Correct required-CI final-status logic.
- [ ] Wire and execute Lawnchair-specific JVM / instrumentation tests.
- [ ] Build/validate release on CI; sign locally only, never commit keys.
- [ ] Keep upstream Launcher3 / Lawnchair notices intact and periodically
      review security changes without blindly rebasing onto 16-dev.

## Gate for declaring the game/Recents bugs fixed

A runnable release APK, reproducible ADB logs, physical-device performance
traces, and repeatable portrait/landscape stress results are required.
See [diagnostics](docs/home-return-recents-diagnostics.md).
