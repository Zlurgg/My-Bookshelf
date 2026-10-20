# Handover: search Google-outage fix

Start a new session on branch `fix/search-google-outage` and implement **`docs/specs/plans/search-google-outage.md`**. Read that plan first: it has the investigation, exact files and line numbers, test cases and verification steps.

## State at handover (2026-10-20)

- Branch `fix/search-google-outage`, cut from `main` @ `28210462` (what production was built from).
- Already committed on the branch (done, don't redo):
  - `chore(build): upgrade Gradle wrapper to 9.8.2` (latest release as of 2026-10-19)
  - `chore(deps): bump AGP to 9.4.1, Kotlin to 2.4.21 and libraries`
  - Verified: detekt clean, 743/743 unit tests (`--rerun-tasks`), signed `assembleRelease` with R8 installs and runs.
- Root cause is confirmed: Google Books is returning 503 `backendFailed` and empty 200s Google-wide, and the app doesn't fall back on either. It is **not** a key/cert/R8/upgrade problem.
- Nothing from the plan's §1–7 is implemented yet.

## Order of work

1. §2 provider field (compile-driven; touches the most files, so do it first)
2. §1 fallback triggers + tests
3. §3 pagination guard + VM tests
4. §4 Open Library notice in `BookSearchDialog`
5. §5 About strings
6. §6 flaky `LibraryViewModelTest` teardown
7. Verification (plan §Verification): tests, then release APK on the emulator over adb, then physical device
8. §7 version bump to 1.0.2 / code 7. Commit separately so it can be dropped if a different release cadence is wanted.

Commit per section, conventional style (`fix(search): …`, `test(library): …`, `fix(about): …`, `chore(release): …`). Per CLAUDE.md: no Co-Authored-By footer.

## Before you start

- Read `docs/specs/patterns/state-management.md` and `docs/specs/style/code-style.md` (CLAUDE.md requires reading the relevant spec before touching a pattern).
- Re-grep construction sites (`BookSearchResponse(`, `SearchResult(`); line numbers in the plan may have drifted.
- Detekt requires braces on multi-line `if/else`.

## Environment notes

- Emulator: AVD `Pixel_8_Pro` (`~/Library/Android/sdk/emulator/emulator -avd Pixel_8_Pro`). The owner prefers adb from Android Studio's emulator/device over external installs, so logcat is available.
- Release signing and the Books key are present locally (`keystore.properties`, `local.properties`), so `./gradlew assembleRelease` works.
- Release builds log only via `CrashlyticsTree`, so expect almost no app output in logcat. Use the UI and the curl replay in the plan.

## Needs the owner

- Analytics wording in About / privacy policy (plan §Open question). Don't change privacy copy without a decision.
- Merging to `main` and dispatching the Play release.
