# Omni Launcher ↔ Workspace integration

Omni Launcher uses the published OmniLink SDK v3.0.0. There is no new AIDL transaction,
SDK release dependency, shared database, always-running agent, or connection during Home startup.

## Enable it

1. Install Workspace and this Launcher build. Public search handoff works even if the APKs use
   different signing certificates. Both must be updated to implement PUBLIC_OMNI_REQUEST.
2. In Launcher settings → Search bar → Search provider, choose **Omni**. The existing QSB shape,
   tint, transparency, and dock/drawer layout continue to work. Tapping opens Workspace chat.
3. In the same settings or the drawer Search settings, **Ask Omni in search results** controls
   suggestions. By default a nonblank query with no local result gets an Ask Omni row.
   Web suggestions, history, and market links do not count as local matches. Enable
   **Show Ask Omni even with local results** for an explicit assistant action alongside apps.
4. Tapping the row passes the query as a draft. Workspace preserves an existing composer draft
   and active agent run. Review and press Send there. Merely typing never calls Omni or sends text
   through OmniLink. Blank zero-state search does not display the row.
5. If several Workspace flavors support the public action, choose one in the visible dialog.
   Missing/outdated Workspace displays an installation/update message, without discarding to a web search.
6. For privileged control, sign Launcher and Workspace with **the same certificate**, then turn on
   **Allow Omni Dev to control the launcher**. Debug APKs from separate builds often have different
   debug certificates; a package name alone is never enough. Workspace owns BIND_EXTENSION.

App-only and on-device local search support the Omni row. The Android System Intelligence (ASI)
backend remains system-owned; choose App search or Global search (on device) for this fallback.
Omni is a search provider, not a Google Discover feed or Smartspace provider.

## Workspace control

Workspace's existing omni_link tool discovers the service and catalog and invokes advertised
capabilities. Mutations advertise requiresConfirmation, so Workspace's approval gate runs first.
The service independently checks the SDK-authenticated signer, exact Workspace package, receiving-side
flavor ceiling, and local consent. Consent is off by default and cannot be enabled through set_preference. Revoke it in the
local UI; queued operations recheck consent before execution and mutations recheck before writing.

| Capability | Payload | Result / restriction |
| --- | --- | --- |
| launcher.health | `{}` | ready, packageName, protocolVersion, requiresSameSigner; usable while consent is off |
| launcher.get_settings | `{}` | Configured provider (or `default` when unset), Omni toggles, valid provider IDs |
| launcher.list_apps | `{"offset":0,"limit":20}` | Up to 40 current-profile visible components per page, labels, total, nextOffset (-1 at end); hidden apps excluded |
| launcher.set_preference | `{"key":"search_provider","value":"omni"}` | One validated setting; supports SDK dryRun; approved change recreates Home to apply provider |
| launcher.set_preference | `{"key":"omni_suggestions","value":true}` | Boolean toggle; also accepts omni_always_suggest; no arbitrary preferences or consent keys |
| launcher.open_app | `{"component":"package/class"}` | Exact component from list_apps; hidden/nonlaunchable apps rejected; resumed Home required; supports dryRun |
| launcher.open_drawer | `{}` | Resumed Home required; supports dryRun |

All capabilities use asynchronous SDK execution. foreground_required is a real refusal, not a
successful background launch. Ask the user to return to Home; do not work around it with
accessibility or shell. Android may still refuse an app launch; SDK failure is propagated.

Flavor ceilings: ADMIN/PRO/OEM get all six known capabilities; NORM gets read capabilities plus
open_app/open_drawer; LITE gets health only. Every mutation still requires approval and local
control consent. Unknown launcher capabilities and spoofed package IDs fail closed.

## Performance and privacy

LOW tier uses the same lightweight vector icon and existing rows; no new blur, bitmap cache,
Home-startup query, persistent Binder connection, or polling timer. The service is started on
explicit discovery and caps concurrent actions at two with 16 KiB requests. PackageManager,
app inventory, and provider resolution happen on IO. Search results are generation-checked so
cancelled/older queries cannot replace newer results. Search coroutine scopes close on destroy.
Control logs contain capability name and caller, never prompt text or app inventory.

## Validation

- Target build: `./gradlew assembleLawnWithQuickstepGithubDebug` (verified from this branch's CI).
- Run `./gradlew spotlessCheck` and the existing CI builds before merging.
- OmniSearchPolicyTest is in the conventional JVM test location. The existing Launcher JVM source
  set is known to return NO-SOURCE (see AGENTS.md); do not claim these tests executed via Gradle.
- Workspace tests cover malformed/oversized public ingress and tier/package boundaries.
- Device checks: Arabic/English QSB, empty/local-hit/local-miss searches, rapid typing then clearing,
  missing Workspace, multiple flavors, cold/warm Workspace, existing composer draft/active run,
  consent off/on/revoked, mismatched signer, hidden apps, paging, dryRun, and foreground refusal.
- Direct Kotlin/JUnit execution passed all 13 tests across launcher suggestion/receiver policies and
  Workspace ingress/outbound policy, using the actual SDK public contract sources and serialization
  compiler plugin (Kotlin 2.2.21 / serialization 1.9.0). This verifies shared decision logic, not an APK.
- Changed Launcher Kotlin files pass ktlint 1.8.0; XML parsing and git diff whitespace checks pass.
- Capability calls return `launcher_service_unavailable` for missing LauncherApps services.
- Production source roots explicitly include `lawnchair/src/app`, `com` and `dev`; the parent
  `lawnchair/src` must not be a production root because it also contains conventional JVM tests.
  AGP/KGP 9.0.1/2.3.0 variant compiler inputs did not honor the attempted source-set exclusions.
  The conventional test files remain intact; this does not rewire the known JVM test gap.
- `./gradlew help --no-configuration-cache --init-script scripts/verify_apk_sources.gradle`
  checks actual Kotlin compiler inputs for all three CI APK variants. It rejects leaked unit tests
  and missing production Kotlin files. This passed locally for all 451 Lawnchair production Kotlin
  files in each variant. Android compilation still requires CI confirmation.
- The CI matrix keeps other variants running after a failure so their diagnostics are retained.
- Gradle wrapper downloads initially failed. A proxy-aware Gradle 9.3.0 attempt configured the
  projects, then stopped because the installed Java 17 toolchain lacks JAVA_COMPILER. No Kotlin/Java
  Android compilation or APK/device performance result is claimed.

## Maintainer and provenance

Omni-specific integration development and documentation are maintained by
**Abdelrahman Hussein (عبدالرحمن حسين / Obieda)**. Launcher remains a Lawnchair/AOSP-derived fork.
Original copyrights, license texts and contributor credits are retained; see [NOTICE.md](../NOTICE.md).
The SDK keeps its own separate license. [OMNI_CHANGELOG.md](../OMNI_CHANGELOG.md) records
fork integration work without overwriting historical upstream release notes.

## Search handoff fixes (1 October 2026)

Recycled search rows reset their click listener on every target change. Ask Omni opens the
explicit in-app handoff from the row, icon and keyboard quick launch; it never falls back to a
web search. Queued icon updates cannot overwrite a newly bound result. The QSB Omni provider
bypasses website and local-drawer preferences and does not synchronously read settings on tap.
Workspace discovery runs on IO with a three-second UI deadline, a visible Cancel button and
recoverable error handling. Workspace task reuse delivers the public request to MainActivity,
which puts the validated question into the existing chat composer without auto-submission.
Install the Workspace build from PR #124: older builds without the public entry point must be
updated. The launcher cannot insert a draft into an older app that has no receiver for it.
The assistant icon uses the original Workspace vector path instead of a generic star.

Device regression checks: show a Google/web result, replace the query with a local miss, then
tap the Ask Omni row, its icon and keyboard action separately. Each should open Workspace with
the exact question in the composer. Repeat with cold/warm Workspace and an existing draft.
Select Omni in QSB with both match-drawer-style and force-website settings on/off. It should
open Workspace directly in each combination. Without an updated Workspace, the update message
must be dismissible; while discovering, Cancel/Back must return to Home. Confirm the Workspace
robot logo in provider settings, QSB and search results in Arabic/English and light/dark themes.
New handoff behavior requires Android CI and physical-device validation; prior policy tests
do not validate touch routing or freeze behavior.
