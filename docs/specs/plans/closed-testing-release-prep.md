# Closed Testing Release Prep

**Status:** Pending — runs after `google-books-integration-review-fixes.md` lands on `main`
**Goal:** make the Google Books–integrated build safe to promote to a Play Store closed-testing track
**Related plans:** `google-books-integration-review-fixes.md` (must be merged first), `google-books-api-integration.md`

## Purpose

`google-books-integration-review-fixes.md` makes the code merge-ready. This plan is the bridge from "merged on `main`" to "promoted to a closed-testing track." Most of the work here is release engineering and observability, not application code.

**Why a separate plan:** the code-review fixes have a different audience (engineering reviewers) and a different success criterion ("merge to `main`") than the release-prep work ("safely visible to external testers"). Mixing them produced a single doc that was too long and conflated correctness with operational readiness.

## Prerequisites

Before starting this plan:

- [ ] `google-books-integration-review-fixes.md` is fully complete and merged.
- [ ] In particular, item 1.5 of that plan (FORBIDDEN disambiguation) has landed — the *code* that emits the loud error log exists. Wiring that signal into a remote reporter is this plan's job.
- [ ] Item 1.6 of that plan (API key moved out of URL query into `X-Goog-Api-Key` header) **must** have landed before any debug APK reaches a tester device — even informally. The previous behavior leaks the key to logcat on every Google Books request; a debug APK in tester hands with that behavior is a credential exposure regardless of whether the build ever sees the Play Store.

## Before starting

Read:
- `docs/specs/plans/google-books-integration-review-fixes.md` (the predecessor)
- `docs/specs/constitution.md`

(An earlier draft pointed at section 10 of `google-books-api-integration.md` for a Cloud Console restrictions checklist — that section is "Fast-Follows (Post-Merge)" and doesn't cover this. Section 3.1 below is the source of truth.)

(An earlier version of this plan opened with a "Phase 1 — Data safety" section covering Room schema reset and destructive-migration removal. That work has moved to the predecessor as item 1.8 because it's pure code with no ops component. The current Phase 1 below is unrelated — it covers a preventative test added in response to a runtime DI crash. Later phases retain their original numbering so existing references still resolve.)

## Phase 1 — Preventative checks

### 1.1 Koin module `verify()` test

**Why.** On 2026-05-26 the app crashed on cold launch after S5 of the predecessor introduced an `apiKeyProvider: () -> String = { ApiConfig.GoogleBooks.apiKey }` test seam on `GoogleBooksApiService` and `GoogleBooksRemoteBookDataSource`, both wired via `singleOf(::Ctor)` in `BookModule`. Koin's `singleOf` ignores Kotlin default-parameter values and tries to resolve every constructor parameter from the graph — the unregistered `() -> String` blew up at runtime. All 685 unit tests passed (they construct these classes directly, never through Koin); only an actual launch surfaced it. Fix landed in the predecessor by switching to explicit `single { Ctor(...) }` so the Kotlin default applies.

This class of bug — `singleOf` ignoring defaults, a missing `bind`, an unresolved `Function0`/N type, a typo in a constructor parameter — will recur as long as no test exercises the modules end-to-end.

**Fix.** Add a unit test that calls `module.verify()` on every module passed to `startKoin` in `MyBookShelfApp`. Koin's `verify()` walks each definition and confirms every constructor parameter is resolvable from the merged graph; it catches the exact failure mode that caused this crash without needing a device. Likely requires adding `org.koin:koin-test` to `testImplementation` if not already present.

The module list in the test must mirror the `startKoin { modules(...) }` block — add a one-line comment in the test pointing at that as the source of truth so future module additions don't silently skip verification.

**Definition of done.**
- [ ] Test added under `app/src/test/.../di/`, runs in the standard `./gradlew test` flow, passes.
- [ ] Verify it would have caught the 2026-05-26 bug: locally revert `BookModule.kt` to `singleOf(::GoogleBooksApiService)` and `singleOf(::GoogleBooksRemoteBookDataSource)`, run the test, confirm it fails citing the unresolved `Function0`. Revert the revert before committing.
- [ ] The test covers every module currently passed to `startKoin`.

## Phase 2 — Observability

### 2.1 Wire up a remote error reporter — hard gate

Predecessor's 1.5 lands as the graceful-degradation path: blank/revoked key short-circuits to `PROVIDER_UNAVAILABLE`, `FallbackRemoteBookDataSource` includes that in `shouldFallback`, OL serves the user, and the only signal that something is wrong is a `Timber.e` log. Tester devices are not accessible for logcat — so without remote crash reporting, blank-key bugs are completely silent in the field.

That makes Crashlytics a hard gate for blank-key observability specifically, and the broader benefits stack on top:
- Compose runtime crashes from rendering edge cases
- Room schema mismatches or open failures on unusual devices
- Firebase auth issues during sign-in
- Any other release-build crash on a tester's device

Without 2.1, tester bug reports arrive as "the app crashed" with no stack trace. Do not ship a closed-testing cohort without this.

**Confirmed via grep: zero Crashlytics references in the codebase today.** This is full integration work, not "add a Timber tree." Expect a half-day to a day of work, not minutes.

**Step 1 — full Firebase Crashlytics integration.**

1. **Confirm or create the release Firebase project before any other work.** The repo's debug build is wired to a Firebase emulator (`app/src/debug/`); the release project may not exist yet or may have a different bundle ID. If the release project doesn't exist, factor in another half-day for project creation (Console UI clicks, billing scope, OAuth client setup if you'll use Auth there too, `google-services.json` generation). Don't start the rest of step 1 until this is settled — otherwise you'll integrate against the wrong project.
2. Add `com.google.firebase.crashlytics` Gradle plugin to `app/build.gradle.kts` and the Firebase BoM + `firebase-crashlytics` dep.
3. Ensure `google-services.json` for the release Firebase project is committed (or wired via CI secret) into `app/src/release/`. Do not check it in if the project policy forbids — gate via CI environment instead.
4. Confirm `FirebaseApp.initializeApp` runs before any Crashlytics call. Most likely already does via `google-services` plugin auto-init in `MyBookShelfApp.onCreate`; verify ordering if you have explicit Firebase init code.
5. Test the basic crash flow: throw a deliberate `RuntimeException` from a debug-only action, run a release build, force-close, confirm the crash appears in the Firebase console within ~5 minutes.

**Step 2 — route the blank-key signal to it.**
The 1.5 code logs at `Timber.tag(TAG).e(...)` on blank key. Add a Timber `Tree` (e.g. `CrashlyticsTree`) installed only in the release build type that forwards `Log.ERROR` priority to `FirebaseCrashlytics.recordException(...)`. Pattern is standard; see Crashlytics docs.

**Step 3 — verify end-to-end.**
- Build a release APK with `GOOGLE_BOOKS_API_KEY` explicitly blank in `local.properties`.
- Install on a device, run a search, force-close, wait for the next session to flush.
- Confirm a non-fatal appears in the Firebase console naming the blank-key path.
- Reset the key, rebuild, confirm the report stops.

### 2.2 Confirm `previewLink` / `infoLink` scheme validation landed

Predecessor's item 1.7 introduces both a mapper-level HTTPS coercion and a launch-site scheme allowlist. As of this plan's writing, that work is owned by the predecessor — this item is purely a verification step before promoting to closed testing:

- Inspect `GoogleBookMappers.toBook` and confirm non-HTTPS `previewLink` / `infoLink` are dropped to `null`.
- Inspect `PublicationDetailsCard.kt` and confirm `uriHandler.openUri(url)` is gated behind a scheme allowlist (typically `url.startsWith("https://")` plus an optional host check).
- Confirm `GoogleBookMappersTest` includes test cases for hostile schemes (`mailto:`, `intent://`, `javascript:`, etc.).

If any of these are missing, this is a regression in the predecessor's 1.7 work — block promotion and fix there.

## Phase 3 — Key hygiene

### 3.1 Restrict the Google Books API key in Cloud Console

Closed-testing APKs can be downloaded and decompiled by any tester. An unrestricted key embedded in the APK is effectively public — anyone can use it against your quota, and any usage cap or billing will apply to *all* of that traffic.

Apply in Google Cloud Console → Credentials → the Books API key:

- **Application restriction:** Android apps. Add an entry containing:
  - The package name (`uk.co.zlurgg.mybookshelf`)
  - The SHA-1 fingerprint of the **release signing certificate**. Get it via `keytool -list -v -keystore <release.keystore> -alias <alias>` or from the Play Console's App Signing page.
- **API restriction:** Books API only. Deny everything else.

**Trap — debug builds will start 403'ing if you only allow the release SHA-1.** `app/build.gradle.kts:61` and `:75` both read the same `GOOGLE_BOOKS_API_KEY` from `local.properties` — debug and release builds use the *same* key. After restriction lands, the debug keystore's SHA-1 is not on the allowlist, so every debug-build search 403s and falls back to OL silently. You'll end up developing against OL while shipping against Google, with no parity.

**Resolution for this project (single-developer, pre-release):** add **both** the debug-keystore SHA-1 and the release-keystore SHA-1 to the same key's Android-app allowlist. Get the debug SHA-1 via `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`.

Risk accepted: anyone with the debug keystore (in practice, anyone with the repo plus the standard debug keystore) can use the key against your quota. At this scale and quota size, that's fine.

**Revisit if any of these change:**
- The team grows beyond one developer.
- The debug keystore moves off your local machine (e.g. CI starts producing debug builds with a different keystore).
- Quota or billing on the key becomes non-trivial.

Then switch to two keys: add `GOOGLE_BOOKS_API_KEY_DEBUG` to `local.properties`, wire via the `debug` block of `app/build.gradle.kts`, restrict each key by its respective SHA-1. Out of scope for this branch.

**Verification after applying:** rebuild both a debug and a release APK and confirm Google Books serves results in both. If a debug-build search falls back to OL after restriction, the debug SHA-1 is missing from the allowlist — fix before declaring 3.1 done. If a release-build search falls back to OL, the release SHA-1 is wrong (likely you've used the local signing keystore's SHA-1 when Play App Signing is enabled — use the SHA-1 shown in Play Console, not your local keytool output).

### 3.2 Verify the release build actually carries the key

`app/build.gradle.kts:75` reads `GOOGLE_BOOKS_API_KEY` from `local.properties` for the release build type. CI or whoever cuts the release must populate this. Easy to forget.

**Note on verification after item 1.6:** the key is now sent via the `X-Goog-Api-Key` request header (per the predecessor plan's 1.6), not as a `?key=…` URL parameter. Grepping the built APK for `?key=` or scanning URL strings will return nothing — that does **not** mean the key is missing. The only places the key now appears in the APK are:
- The compiled `BuildConfig.GOOGLE_BOOKS_API_KEY` field (a `static final String` in the generated `BuildConfig` class).
- The runtime call site that reads `ApiConfig.GoogleBooks.apiKey` and attaches the header.

Belt and braces:
- Search for the literal key value (or a unique prefix of it) in the unpacked APK: `unzip -p <apk> classes*.dex | strings | grep '<unique-prefix>'`. The key prefix `AIza` (common to Google API keys) is a good search anchor if you don't want to handle the literal.
- Or simpler: just run the smoke test in Phase 4 and confirm Google Books actually serves results. If the response shows attribution and Google-formatted data, the header is being sent correctly; if every search falls back to OL, the key is missing or rejected — and the Crashlytics wiring from Phase 2 should be reporting it.

## Phase 4 — Smoke testing

### 4.1 End-to-end release smoke test

**Prerequisite — tell internal testers to uninstall first.** Predecessor's 1.8 collapses the schema from v5 to v1 without a downgrade migration. Any device that currently has a v5 install of the branch will crash on first launch under the new build (Room throws `IllegalStateException` at first DAO access). At this cohort size, the cheap fix is a single Slack/email message: "Uninstall MyBookshelf before installing the next build." Skip and you'll be debugging a startup crash that isn't a bug.

*Alternative considered, not recommended at this scale:* rename the application ID for the closed-testing track (e.g. `uk.co.zlurgg.mybookshelf.beta`). Side-installs alongside any existing v5, no out-of-band tester communication needed. Costs: a new Firebase project entry, a new API-key Android-app restriction (the beta bundle ID), a separate Play Console listing. Worth revisiting if the cohort grows past "people I can message individually," but not justified for a first closed-testing cut with a small known group.

Before submitting to a closed-testing track, install the **exact APK** that will be uploaded on a real device (or a fresh emulator with no prior install of this app). Walk through:

- [ ] Cold-launch app (proves Room opens cleanly at v1)
- [ ] Open search, search for a well-known book ("the great gatsby")
- [ ] Results appear; verify "Powered by Google" attribution shows (proves Google Books served, not OL fallback)
- [ ] Tap a result, verify book detail loads with description, subtitle (where present), preview/info links
- [ ] Add the book to a shelf, close the app, reopen — book is still there (proves write/read round-trip works)
- [ ] Edit personal notes, close the app, reopen — notes persist
- [ ] Trigger an obviously-failing search (e.g., 200+ char query) and confirm graceful error

**Optional explicit schema-version check** (only if you want to confirm v1 specifically, not just "write-then-read works"): `adb shell run-as uk.co.zlurgg.mybookshelf sqlite3 /data/data/uk.co.zlurgg.mybookshelf/databases/<dbname> "PRAGMA user_version;"` — should return `1`.

If any of the smoke-test items fail, do not submit.

### 4.2 Quota sanity-check

The closed-testing build will be running against a (possibly small) Google Books daily quota. Before opening to >5 testers, eyeball the Cloud Console traffic dashboard after a day of testing.

**Hold the ramp if any of these are true:**
- More than 5% of total Books API requests in the last 24h returned 429.
- Crashlytics shows blank-key reports (means restriction misconfigured or release build missing key).
- More than 20% of searches in your own usage triggered the OL fallback (means Google is serving substantially less than expected — usually a 4xx/5xx surge or a misconfigured restriction).

If 429s spike, item 1.4 of the predecessor plan should have eliminated redundant description fetches — confirm that's in place. Otherwise, consider raising the quota or trimming test cohort size before broadening.

## Definition of done

Promotion to closed testing is gated on **all** of the following:

- [ ] Predecessor's item 1.8 (schema reset) has merged on `main`; verified `version = 1` and `fallbackToDestructiveMigration` removed.
- [ ] 1.1 — Koin module `verify()` test added; passes; covers every module passed to `startKoin`. Confirmed (via local revert experiment) it would have caught the 2026-05-26 startup crash.
- [ ] 2.1 — Crashlytics integrated end-to-end (plugin, dep, release Firebase project, `google-services.json`, `CrashlyticsTree`). Required: 1.5 lands as graceful-degradation, so the `PROVIDER_UNAVAILABLE` log is the only signal a blank/revoked key is in production. Also captures Compose runtime crashes, Room failures, Firebase auth issues, and anything else release-build testers hit.
- [ ] 2.2 — `previewLink` / `infoLink` mapper coercion + launch-site allowlist verified present (work owned by predecessor's 1.7).
- [ ] 3.1 — Books API key restricted by Android app + release SHA-1 + bundle ID, restricted to Books API only. Debug-key 403 trap addressed (either debug SHA-1 added to same key or separate debug key wired).
- [ ] 3.1 — Restriction state recorded outside Cloud Console (screenshot in PR description, or a one-line entry in `docs/specs/operations/api-key-restrictions.md` noting who applied it and when). Cloud Console state is not source-controlled — capture it somewhere durable so future-you knows what's already in place.
- [ ] 3.2 — Release APK confirmed to carry a non-empty `GOOGLE_BOOKS_API_KEY` (via DEX strings grep or smoke-test signal).
- [ ] 4.1 — Tester-uninstall instruction sent. End-to-end smoke test passed on a fresh install of the exact APK to be uploaded.
- [ ] 4.2 — Quota sanity-check window scheduled; ramp thresholds documented.

None of these are code-review concerns. They are release gates.

## Out of scope

- Open testing or production track promotion. This plan covers closed testing only; widening cohort size and moving up the testing tracks is a separate effort.
- Server-side abuse protection (rate limiting per device, etc.) — not a concern at closed-testing scale.
- Crashlytics' broader instrumentation (analytics events, custom keys, breadcrumbs). 2.1 wires the *minimum* needed for blank-key observability; richer telemetry is a separate plan.
