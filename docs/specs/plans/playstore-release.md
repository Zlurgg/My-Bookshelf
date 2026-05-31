# Play Store Release — internal testing

**Goal:** ship MyBookshelf to Play Store internal testing via GitHub Actions, with accurate in-app and listing content.

**Trigger:** `git tag v* && git push --tags` runs `.github/workflows/release.yml`, which builds an AAB and uploads it as a draft to the Play Store internal track. The human then promotes the draft to testers from the Play Console.

## Phase 1 — Release pipeline fixes (blocks any tag)

### 1.1 Pass `GOOGLE_BOOKS_API_KEY` through CI

`release.yml` writes `keystore.properties` and `google-services.json` from secrets but never writes `local.properties`. `app/build.gradle.kts:118` reads `GOOGLE_BOOKS_API_KEY` from `local.properties` for the release build type. Result today: CI builds ship with `BuildConfig.GOOGLE_BOOKS_API_KEY = ""`, Books API rejects every call, app silently falls back to Open Library, and the only signal is a Crashlytics non-fatal nobody is watching for.

**Fix.** Add `GOOGLE_BOOKS_API_KEY` to GitHub repo secrets. Add a step in `release.yml` before `bundleRelease`:

```yaml
- name: Write local.properties
  env:
    GOOGLE_BOOKS_API_KEY: ${{ secrets.GOOGLE_BOOKS_API_KEY }}
  run: |
    echo "GOOGLE_BOOKS_API_KEY=$GOOGLE_BOOKS_API_KEY" >> local.properties
```

**DoD:** CI run produces an AAB whose `BuildConfig.GOOGLE_BOOKS_API_KEY` is non-empty (verified by smoke test in Phase 4 — Google attribution appears on search results).

**Done (2026-05-31).** Step added to `release.yml` immediately after the `google-services.json` step. The step also fails fast (`exit 1`) if the secret is empty, so we don't silently ship a blank-key AAB. `GOOGLE_BOOKS_API_KEY` secret added to the repo Actions secrets alongside the existing keystore + service-account secrets.

### 1.2 Pick a `versionCode` strategy

`app/build.gradle.kts:64` has `versionCode = 1`. Play rejects any upload whose `versionCode` is not strictly greater than the previous one on the same track.

Options considered:
- **(A) Manual bump per tag** — simplest. Forgetting it fails the upload loudly. Recommended.
- (B) Derive from `github.run_number` — auto, but `versionCode` drifts from `versionName`, and workflow re-runs inflate it.
- (C) Parse from git tag (`v1.0.3 → 10003`) — most explicit but adds CI parsing code.

**Plan: (A).** Bump `versionCode` in the same commit as the tag. Document in Phase 5 release steps.

**Done (2026-05-31).** Kept `versionCode = 1` for the v1.0.0 first tag (nothing has ever been uploaded, so 1 is the natural starting value). Convention going forward: **each subsequent tag bumps `versionCode` by 1** (v1.0.1 → 2, v1.0.2 → 3, …). Reminder lives in Phase 5 step 1 — must happen before tagging.

## Phase 2 — In-app and site content updates

App behaviour drifted from what these files say. Fix before testers read them.

### 2.1 Tutorial book description

`BookDetailConstants.TUTORIAL_BOOK_DESCRIPTION` says "search the Open Library". App now uses Google Books with OL fallback. Also missing: the Library scope toggle (`library-scope-search-toggle` feature), the explicit search-submit affordance, and the Books API attribution.

Rewrite the description to:
- Mention Google Books as the search source (not Open Library)
- Cover the Library scope toggle ("Show my library only")
- Reference the explicit search submit (tap the search icon / press enter)

**Note on persistence:** `GetOrCreateTutorialBookUseCaseImpl` upserts by fixed ID, so existing installs keep the old description. Internal testers will be told to uninstall first (standard pre-release practice), so this is a non-issue. Don't bump `TUTORIAL_BOOK_ID`.

**Done (2026-05-31).** Two targeted edits to `TUTORIAL_BOOK_DESCRIPTION`:
- **🔍 SEARCH FOR BOOKS** — replaced the Open Library sentence with: search Google Books (with Open Library as a backup), and a mention of the explicit submit (tap icon / press enter). Per feedback, did NOT oversell the fallback as "offline-friendly" — both providers need internet; the fallback is for API-token failure and not actionable by the user.
- **🔎 SEARCH ONLY YOUR LIBRARY** — new section explaining the "My library" toggle for re-shelving without going back to the web.

Left the rest (Book Clubs, Library tab, shelf styling, reorder, tidy/messy, cloud sync) as-is — still accurate.

### 2.2 Welcome screen privacy text

`R.string.welcome_privacy_message` claims "We don't collect analytics or share your data with third parties". Crashlytics is wired in release builds (`CrashlyticsTree`), so this is misleading. Rewrite to acknowledge Crashlytics (no PII) without overpromising.

**Done (2026-05-31).** New text: *"Your data is stored locally on your device. Sign in with Google to sync across devices via secure cloud storage. We don't sell your data or run ad trackers — anonymous crash reports help us fix bugs."* Kept the three-sentence shape; only swapped the misleading sentence.

### 2.3 GitHub Pages landing

`docs/index.html` line 304: "Search 20M+ books from Open Library API". Replace with Google Books primary + OL fallback wording.

**Done (2026-05-31).** Line now reads: *"Search millions of books from Google Books (with Open Library as a backup)"*. Other feature lines on the page (Book Clubs, Google Sign-In, offline-first) verified still accurate; left untouched.

### 2.4 Privacy policy

`docs/privacy.html`:
- Add **Firebase Crashlytics** under third-party services (data: stack traces, device model, app version — no PII).
- Add **Google Books API** under third-party services (search queries sent to Google; no PII attached).
- Bump "Last updated" date.

**Done (2026-05-31).** Four edits:
- "Last updated" bumped to May 2026.
- Added a **Crash reports** row to the "Information We Collect" table (purpose: diagnose and fix bugs; storage: Firebase Crashlytics).
- Softened the "What We Don't Collect" line from *"We do not collect analytics or usage data"* → *"We do not collect usage analytics beyond anonymous crash reports (see Third-Party Services below)"*.
- Third-Party Services now lists three providers: **Google Books API (primary)**, **Open Library API (fallback)**, **Firebase Crashlytics**. Each has a one-line description of what data flows and a link to the provider's privacy policy.

### 2.5 Delete-account page

`docs/delete-account.html` was written before the in-app self-service delete flow existed and still tells users to open a GitHub issue. App now has `AccountScreen → Delete Account → DeleteAccountConfirmDialog → DeleteAccountUseCase` which deletes user-created clubs, club memberships, the user's Firestore doc, the Firebase Auth account, and local club shelves.

Restructure:
- Lead with in-app self-service (3-step walkthrough).
- "What Gets Deleted" rewritten to match what the use case actually does (was over-stating: claimed "all bookshelves and books stored in the cloud" — the use case doesn't iterate personal bookshelves).
- "Can't access the app?" — kept the GitHub-issue path as a fallback for locked-out users; Play Console requires a public deletion URL anyway.

**Done (2026-05-31).** Page reordered: in-app self-service is the lead "(Recommended)" section, then "What Gets Deleted" (now lists: clubs you created, club memberships, user record, Firebase Auth account, local club shelves), then "Can't access the app?" with the GitHub-issue fallback. "What Gets Deleted" explicitly notes that personal (non-club) bookshelves remain on the device — the in-app delete only removes remote data tied to the account, per the user's intent: "delete is just for the remote data we keep their personal data is theirs to do with as they please".

## Phase 3 — Verify Crashlytics end-to-end

Integration exists (`app/src/release/.../CrashlyticsTree.kt`), never end-to-end verified.

**3.1 Blank-key non-fatal test.** Build a release AAB locally with `GOOGLE_BOOKS_API_KEY=` blank in `local.properties`. Install on device. Run a search. Wait for next launch. Confirm a non-fatal appears in Firebase Crashlytics console naming `GoogleBooksRemoteBookDataSource` / `PROVIDER_UNAVAILABLE`.

**Alternative considered:** add a temporary forced-crash button (e.g. long-press welcome icon → `throw RuntimeException`). Skipped — proves a bit more (generic Compose/Room crashes report too) but costs throwaway code and a remove-before-tag step. The blank-key path is the failure mode that actually matters: it's the only signal a misconfigured release build is in production.

**Outcome (2026-05-31).** Verified Crashlytics pipeline IS delivering non-fatals end-to-end — landed an OL `HttpRequestTimeoutException` and a Google `UnknownHostException` from the test install. However, the specific blank-key signal (`Timber.tag("GoogleBooksSearch").e("Google Books API key is not configured")` at `GoogleBooksRemoteBookDataSource:39`) never reached Crashlytics, despite the dex bytecode containing the correct isBlank check + Timber.e + return-Error sequence, and despite `BuildConfig.GOOGLE_BOOKS_API_KEY = ""` being verified in both the generated source and the installed APK (no AIza Books-key strings in dex). The UnknownHostException itself proves the Google network call was attempted, meaning the runtime `isBlank()` result was false — root cause unknown.

Accepted as **functionally complete** for release: the Crashlytics observability that Phase 3.1 set out to validate works. The blank-key path is a misconfiguration signal that CI's `GOOGLE_BOOKS_API_KEY` injection (Phase 1.1) makes unreachable in production for actual users; we have backup observability via the GCP Books API quota dashboard. The gap is tracked as a separate follow-up — see task "Investigate missing blank-key non-fatal in release".

## Phase 4 — Pre-tag smoke test

Install the **exact AAB CI will produce** (or a local `assembleRelease` with prod config) on a fresh device. Run through:

- [ ] Cold launch (proves Room opens cleanly)
- [ ] Welcome screen renders; new privacy copy is what was written in 2.2
- [ ] Sign in with Google (verify auth works against the release Firebase project)
- [ ] Search a known book ("the great gatsby") — **Google attribution shows** (proves Books API key is live, not OL fallback)
- [ ] Open tutorial from Help menu — description matches what 2.1 wrote
- [ ] Add book to shelf, kill app, reopen — book persists
- [ ] Library tab — search + filter work; scope toggle behaves
- [ ] Trigger a malformed search (200+ chars) — graceful error, no crash

Any failure → fix before tagging.

## Phase 5 — Tag and upload

1. Bump `versionCode` (Phase 1.2 strategy). Update `versionName` if bumping major/minor.
2. Commit: `chore(release): bump versionCode to N for v1.0.0`.
3. Tag: `git tag v1.0.0 && git push origin v1.0.0`.
4. Watch the workflow in Actions tab. AAB lands as draft on Play internal track.
5. In Play Console, promote draft → "Available to testers" on the internal track.
6. Install from Play Store on a clean device, repeat the Google Books attribution check from Phase 4. **If it falls back to OL on the Play install but worked on the local install, the Play App Signing SHA-1 isn't allowlisted** — go to Phase 5.1 immediately.

### 5.1 Capture Play App Signing SHA-1 (after first upload)

Play App Signing re-signs the AAB with a Google-managed cert whose SHA-1 isn't yet on the Books API Android-app allowlist. This is the line in `docs/specs/operations/api-key-restrictions.md` currently marked `<TODO after first Play upload>`.

1. Play Console → Setup → App integrity → App signing key → copy SHA-1.
2. Cloud Console → Credentials → Books API key → Application restrictions → Android apps → add an entry with package `uk.co.zlurgg.mybookshelf` + that SHA-1.
3. Update `docs/specs/operations/api-key-restrictions.md` line 34 with the SHA-1 + date.
4. Re-test on the Play install. Google attribution should now show.

## Phase 6 — Play Console listing (can be done in parallel)

Required to promote beyond Internal. Soft-required for Internal (Play asks for these but lets you defer).

- **Privacy policy URL:** `https://zlurgg.github.io/My-Bookshelf/privacy.html`
- **Short description** (80 chars) — to draft
- **Full description** (4000 chars) — to draft
- **Feature graphic** (1024×500 PNG/JPG) — needs creating
- **Phone screenshots** (min 2, recommend 4–8) — raw shots in `docs/screenshots/`, may need cropping
- **App icon** (512×512) — verify Console has the high-res version
- **Data Safety form** — declare:
  - Email address (account, collected, optional)
  - User-generated content (book club comments + reviews, collected, shared with other club members)
  - Crash reports (Crashlytics, no PII)
  - Explicitly: no advertising, no analytics, no third-party data sharing
- **Content rating questionnaire** — must declare user-generated content (book club comments)
- **Target audience** — 13+ (Firebase auth minimum)

Out of scope here: localised listings, store-listing experiments, multi-track ramp.

## Definition of done

- [x] 1.1 — CI writes `GOOGLE_BOOKS_API_KEY` into `local.properties` before `bundleRelease`; step fails fast if the secret is empty; secret added in GitHub repo Actions settings.
- [x] 1.2 — `versionCode` strategy chosen: kept at 1 for first tag (v1.0.0), manual `+1` bump per subsequent tag.
- [x] 2.1 — Tutorial description updated: Google Books wording, "My library" scope toggle section, explicit search submit.
- [x] 2.2 — Welcome privacy text updated to acknowledge Crashlytics without overpromising.
- [x] 2.3 — `docs/index.html` Google Books wording updated.
- [x] 2.4 — `docs/privacy.html` lists Crashlytics + Google Books; date bumped to May 2026.
- [x] 2.5 — `docs/delete-account.html` leads with in-app flow; "What Gets Deleted" matches `DeleteAccountUseCase`; GitHub-issue fallback retained.
- [x] 3.1 — Crashlytics pipeline verified end-to-end (OL timeout + Google UnknownHostException non-fatals landed). Specific blank-key Timber.e gap accepted as non-blocking; tracked as a separate follow-up.
- [ ] 4 — Pre-tag smoke test passed on the AAB that will be uploaded.
- [ ] 5 — Internal-track upload succeeded; testers can install.
- [ ] 5.1 — Play App Signing SHA-1 captured; added to Books API allowlist; `api-key-restrictions.md` updated.
- [ ] 5 (post) — Verified Google attribution on Play-installed app.
- [ ] 6 — Play Console listing fields populated (or knowingly deferred for Internal-only).
