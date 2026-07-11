# CI Notes

This document clarifies the status and execution behavior of the CI workflows present in `.github/workflows/` on this fork.

## 1. Failing Workflows on Push

The following workflows heavily depend on secrets (`KEYSTORE*`, `TELEGRAM_*`, `LAWNCHAIR_BOT_TOKEN`, `CROWDIN_*`) and repository context:

- **`ci.yml`**:
  - The `build-debug-apk` job conditionally handles secrets (it checks `github.repository_owner == 'LawnchairLauncher'` before writing the keystore). Since this fork is not `LawnchairLauncher`, the signing step is safely skipped, allowing the debug build to succeed without failing.
  - The `send-notifications` job is guarded by `if: github.repository_owner == 'LawnchairLauncher'`, so it simply gets skipped and won't fail here.
  - The `crowdin-auto-merge` job is guarded by PR conditions and the `lawnchair-bot` user. It will be skipped.
  - The `nightly-release` job is guarded by `if: github.repository_owner == 'LawnchairLauncher'`, so it will be skipped.
- **`build_release_apk.yml`**: The signing step has a `github.repository_owner == 'LawnchairLauncher'` condition. The build step does not, but the job has `continue-on-error: true`. It will attempt to run `./gradlew assembleLawnWithQuickstepGithubRelease bundleLawnWithQuickstepPlayRelease` without the keystore, which might fall back to debug signing or fail, but the workflow won't block overall due to `continue-on-error: true`.
- **`release_update.yml`**: Triggered via `workflow_dispatch`. The signing step is guarded, but the `build-release-apk` step will try to build without keys and likely fail since it lacks `continue-on-error: true`. The `publish-github-release` job would fail if it lacked `GITHUB_TOKEN` permissions, but it runs on dispatch.
- **`crowdin.yml`, `crowdin_download.yml`, `crowdin_upload.yml`**: These are guarded by `if: github.repository_owner == 'LawnchairLauncher'` except for `crowdin_upload.yml` which was completely missing the check on the job level (it had it on the step level but the workflow itself wasn't fully protected). All crowdin workflows have been disabled (commented out) in this fork to prevent silent or noisy failures.

## 2. Debug Build Path in `ci.yml`

The primary day-to-day workflow (`ci.yml -> build-debug-apk`) passes cleanly regardless of missing secrets because the keystore step conditionally bypasses itself:

```yaml
      - name: Write sign info
        if: github.repository_owner == 'LawnchairLauncher'
```

If it's not the official repository, it doesn't try to write or decode missing secrets. The Gradle build continues and generates unsigned/debug APKs perfectly fine.

## 3. `build_release_apk.yml` vs `release_update.yml`

- **`build_release_apk.yml`**: A simple workflow triggered manually (`workflow_dispatch`) that builds a release APK and Play Store App Bundle (`bundleLawnWithQuickstepPlayRelease`) and uploads them as artifacts. It has `continue-on-error: true`.
- **`release_update.yml`**: A distinct, more complex workflow also triggered manually (`workflow_dispatch`). It builds the release APK, publishes a GitHub Release using `softprops/action-gh-release`, and then runs a Python script (`ci.py`) to publish an update post to a Telegram channel.

They do not completely overlap. `build_release_apk.yml` is likely for testing release builds locally via artifacts, whereas `release_update.yml` is the official pipeline to cut a release on GitHub and notify users on Telegram.

## 4. Crowdin Workflows

Since this fork is not part of Lawnchair's Crowdin project, the files `crowdin.yml`, `crowdin_download.yml`, and `crowdin_upload.yml` have been commented out to completely disable them and prevent them from running or failing silently.

## 5. Broken State

What remains broken on this fork:
- If a user manually triggers `release_update.yml`, it will fail during the build step because the `KEYSTORE` is missing (and not bypassed with debug signing gracefully for the release task).
- If a user manually triggers `build_release_apk.yml`, it will generate artifacts, but they won't be signed with the expected release keys.
