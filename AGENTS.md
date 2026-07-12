# AGENTS.md — Omni Launcher

## Mission
This repo is a fork of **Lawnchair 15** (tag `v15.0.0-beta3.0`, built on AOSP Launcher3 from Android 15). We switched here from a Lawnchair 14 base after real hands-on testing showed 14 had bad lag/freezing on this hardware (especially returning from WhatsApp), while 15 Beta 3 felt noticeably smoother. We are **not** rewriting a launcher from scratch — we are extending a mature, battle-tested codebase in controlled, reviewable layers.

**Project scope, as of now:** this is a personal launcher for the author's own phone — an Infinix HOT 10S (MediaTek Helio G85, 4GB RAM). It is not being distributed, published, or shared with anyone else at this stage. Sharing it with someone else later is a possibility, not a current requirement.
- Play Store compliance, release signing, and broad multi-device compatibility are **not** current concerns — don't let them block or slow down any task.
- The one device above is the real, primary validation target. When a task says "test on LOW tier," that means this exact device, not a hypothetical.
- Keep the door open for "later": don't hardcode single-device assumptions into shared logic, and keep license/attribution correct from day one.

Priority order, always:
1. **Never lag or hang** — this is the entire reason we're on this branch instead of 14. Including on low-RAM devices (2–4GB RAM, e.g. MediaTek Helio G-series class hardware).
2. Smooth, physics-based, purposeful animation.
3. Strong Arabic / RTL support.
4. Everything else — features, theming, customization — comes after 1–3 hold.

A missing feature is forgivable. A stutter returning to the home screen is not. Speed is the first impression; everything else is decoration on top of it.

## Repository Map — read this before touching anything, verified against this exact tag
- `lawnchair/` — our own code. New features and changes go here by default. Notably includes `data/folder/` (FolderEntity.kt, FolderDao.kt, FolderService.kt, FolderViewModel.kt — Room-backed App Drawer Folders, new in Lawnchair 15) and `deck/` (AddFoldersWithItemsTask.kt).
- `src/` — a clone of AOSP Launcher3 with modifications. Touch only when strictly necessary.
- `quickstep/` — Recents/Overview integration. Root-dependent on some Android versions. Do not touch without a task explicitly targeting it.
- `systemUI/` — unlike the 14-era layout (which had separate `systemUIAnim`/`systemUICommon`/etc. top-level folders), this tag consolidates them into subfolders: `systemUI/anim`, `systemUI/common`, `systemUI/log`, `systemUI/plugin`, `systemUI/plugin_core`, `systemUI/shared`, `systemUI/unfold`, `systemUI/viewcapture`. Treat as vendored/third-party unless a task explicitly targets it.
- `flags/` + `aconfig/` — **new in this tag, didn't exist in the 14 fork.** This is AOSP's real Aconfig flag system: `aconfig/launcher.aconfig` and `aconfig/launcher_search.aconfig` define flags, and `flags/src/.../FeatureFlags.java` + `CustomFeatureFlags.java` exist per-domain (launcher3, systemui, systemui.shared, window.flags2, wm.shell). The old ad-hoc `DynamicFlag.kt` pattern from Lawnchair 14 is gone — this replaced it.
- `compatLib/`, `hidden-api/` — reflection and hidden-API shims for cross-Android-version support. Do not remove without understanding why each shim exists.
- `baseline-profile/` — already configured. Keep it in sync with any startup-path change.
- New-to-this-tag top-level folders worth knowing about: `androidx-lib/`, `nightly/`, `src_no_quickstep/`, `wmshell/`, `fastlane/`, `flowerpot/`, `github/`, `go/`. Nobody has audited these yet in this project — first real task (below) covers it.

## Golden Rules
- Never block the main thread — no I/O, no PackageManager queries, no bitmap decode on Main.
- Never introduce visible jank. Target 60fps minimum, always, with zero dropped frames tolerated on the home-return path specifically — this is the literal bug we're trying to leave behind from the 14 fork.
- Never increase steady-state memory without a documented reason in the PR.
- Never duplicate logic that already exists in `src/`, `lawnchair/`, or now `flags/` — search first, including the new Aconfig-based flag system before building any new toggle/config mechanism.
- Never silently change public/user-visible behavior without calling it out in the PR description.
- **Known gap, confirmed during Task 3:** the `lawnchair` module's JVM unit tests are not wired to a working source set (NO-SOURCE, confirmed via actual test-summary output, not assumption). Write tests in the conventional location and write correct code regardless, but don't spend a task's effort re-attempting Gradle fixes for this — it needs one dedicated, human-led investigation in Android Studio, not repeated per-task guessing. Report actual test-execution status honestly either way.

## Device Tiers — the rule that shapes every other decision
Not every device is a flagship. Before adding *any* visually expensive effect (blur, frosted glass, heavy shadows, large animated surfaces), gate it behind a device-tier check:
- Detect tier **once** at process start via `ActivityManager.isLowRamDevice()` + `getMemoryClass()`. Never recompute per-frame. (Verified: as of this tag, nothing equivalent already exists in this codebase — this is genuinely new work, not a duplicate.)
- **LOW** tier (~2–4GB RAM class): disable or cheaply approximate blur, cap concurrent widget/bitmap memory, use a smaller icon cache, prefer simpler transition curves. Right now, "LOW tier" concretely means the author's Infinix HOT 10S.
- **MID/HIGH** tier: full visual fidelity. Keep this path correct, but it isn't being actively validated against real hardware right now — forward-compatible, not urgent.
- Every new visual effect's PR must state its LOW-tier fallback explicitly. "Looks great on a Pixel" is not sufficient — this project is judged on the author's own 4GB device first.

## Performance Budget
| Metric | MID/HIGH tier | LOW tier |
|---|---|---|
| Cold start → first frame | < 400ms | < 600ms |
| Home return (warm) | < 100ms | < 150ms |
| Scroll / drag frame time | 16ms (60fps) | 16ms (60fps) — never sacrificed |
| Icon decode | off main thread, always | off main thread, always |
| Steady-state idle-home memory | as low as practical | hard ceiling — profile before merging |

## Code Philosophy
Kotlin for new code. Readability over cleverness. Composition over inheritance. Immutable models where practical. Small, focused, reviewable PRs over sweeping rewrites — if a task feels like it wants to touch more than a handful of files outside `lawnchair/`, stop and flag it instead of proceeding.

## Before Any Task
1. Read this file in full.
2. State your plan and which files you expect to touch, before writing code.
3. If the plan touches `src/`, `quickstep/`, or `systemUI/`, explain in the plan why an equivalent change in `lawnchair/` isn't possible.
4. Implement. Then self-check against the Review Checklist below before opening the PR.

## Review Checklist — apply to every PR
- [ ] Builds successfully — confirm the exact debug variant name for this tag (flavor dimensions are `app`, `recents`, `channel`; verify the precise Github-channel debug task name rather than assuming it matches the 14-era `assembleLawnWithQuickstepGithubDebug`)
- [ ] No new main-thread I/O, PackageManager calls, or bitmap decoding
- [ ] LOW device-tier behavior considered and stated (or explicitly marked N/A with reasoning)
- [ ] No new allocations introduced in scroll/drag hot paths
- [ ] RTL layout checked where the change touches UI (mirroring, start/end vs. left/right)
- [ ] No dead code, unused imports, or commented-out blocks left behind
- [ ] PR description explains what changed and why, in plain language, and lists every file touched
