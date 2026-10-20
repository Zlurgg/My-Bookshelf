# Search: survive Google Books outages

**Status:** shipped — promoted to production 2026-10-20 as **1.0.3 (versionCode 9)**. See [Outcome](#outcome).
**Branch:** `fix/search-google-outage` (merged)
**Date:** 2026-10-20

## Problem

Production users can't search for books. The dialog shows:

> Failed to search books: Server error occurred. Please try again later.

Reproduced locally on a signed release build (current `main` + dependency upgrades) on the Pixel 8 Pro emulator. It's intermittent: one search succeeded, the next failed.

## Investigation

### Not our build

- Production is built from `main`. The latest `Release to Play Store` run (2026-07-20, workflow_dispatch) built `28210462` = `main` HEAD before this branch. No unmerged branches carry search work (`last-few-tweaks-I-promise` is fully merged).
- Production still uses the old dependency set, so today's AGP/library upgrades are not the cause.
- The API key and Android-app restriction are fine: requests sent with the release signing-cert SHA-1 (`9f2f9f7a…bbcc0`) get through. Without the right `X-Android-Cert` they get `403 API_KEY_ANDROID_APP_BLOCKED` instead, which is a different failure.

### Google Books is degraded, Google-wide

The same request the app sends (same key, `X-Android-Package`, `X-Android-Cert`, `maxResults`, `printType`), replayed 30× with curl across 10 queries:

| Outcome | Count | Notes |
|---|---|---|
| `503 backendFailed` "Service temporarily unavailable." | 23 | Same query alternates 503/200 |
| `200` with **zero** results for queries that obviously match | 5 | `isbn:9780441172719` (Dune), `intitle:dune`, `subject:fiction`, `intitle:test` |
| `200` with real results | 2 | "the hobbit", "pride and prejudice" |

Open Library answered the same queries with `200` in ~0.65s.

Other projects have reported the same throughout October 2026 (503 `backendFailed` with valid keys, `isbn:` queries returning 0 results):
[markleit/oldmansbookclub_ios#211](https://github.com/markleit/oldmansbookclub_ios/issues/211),
[alshstf/skriptes#506](https://github.com/alshstf/skriptes/issues/506),
[slucky31/mycomicsmanager#1155](https://github.com/slucky31/mycomicsmanager/issues/1155),
[bndct-devops/tome#247](https://github.com/bndct-devops/tome/issues/247),
[hans-fritz-pommes/isbnlib#54](https://github.com/hans-fritz-pommes/isbnlib/issues/54).

### Why the app surfaces an error instead of falling back

1. `HttpClientFactory` retries 5xx with exponential backoff (`HttpClientFactory.kt:74`), but for some queries every attempt fails.
2. `ErrorMapper` maps 5xx → `DataError.Remote.SERVER_ERROR` (`ErrorMapper.kt:50`).
3. `FallbackRemoteBookDataSource.shouldFallback()` only falls back on `TOO_MANY_REQUESTS`, `FORBIDDEN` and `PROVIDER_UNAVAILABLE`. That set came from the original integration (`24a7bfdb`, "fallback on 403/429"), which assumed 5xx was transient and retries would cover it. A test (`searchBooks does NOT fall back on SERVER_ERROR`) still encodes that assumption.
4. Empty-but-200 Google responses aren't treated as failures at all, so the user sees "No books found".

### Related bug found along the way: "Load more" after a fallback

When page 1 comes from Open Library, the VM sets `nextStartIndex` to OL's raw count (up to 100) and `canLoadMore` from OL's page size. "Load more" then sends `startIndex`, and `FallbackRemoteBookDataSource` always sends `startIndex != null` requests to the **primary** (Google). Result: Google rows from offset ~100 of a different result set get appended to OL's page 1, or the load-more fails with Google's 503.

Sites: `BookshelfViewModel.kt:481` / `:507`, `LibraryViewModel.kt:454` / `:473`, `FallbackRemoteBookDataSource.kt:39`.

## Changes

### 1. Fall back on 5xx and on an empty first page

`book/data/network/FallbackRemoteBookDataSource.kt`

- Add `DataError.Remote.SERVER_ERROR` to `shouldFallback()`.
- On page 1 (`startIndex == null`), if the primary returns `Result.Success` with `rawPageSize == 0`, call the fallback.
  - Fallback succeeds → return the fallback result (even if it is also empty).
  - Fallback fails → return the **original empty Google success**, not the OL error. An empty Google page is a legitimate answer, so it shouldn't turn into an error screen.
- Keep the existing page-2+ short-circuit (`if (startIndex != null) return result`) unchanged.
- Keep current behaviour where Google errors → fallback errors → the **fallback** error is surfaced (existing test `searchBooks surfaces fallback error when fallback also fails`).
- Update the class-level comment to say why 5xx is now included: retries don't cover multi-day `backendFailed` outages (Oct 2026).

Tests (`FallbackRemoteBookDataSourceTest`):
- Flip `searchBooks does NOT fall back on SERVER_ERROR` → `searchBooks falls back on SERVER_ERROR`. This is drift (the subject's behaviour changed deliberately), not a tombstone.
- Add: `falls back when primary returns empty first page`.
- Add: `returns primary empty result when fallback fails after empty first page`.
- Add: `does not fall back on empty page when startIndex non-null`.
- Keep `NO_INTERNET` / `REQUEST_TIMEOUT` no-fallback tests as is: OL won't do better without connectivity.

### 2. Carry the provider through search results

`BookSearchResponse` and `SearchResult` gain `val provider: BookProvider`. **No default value**, so each data source has to state it explicitly and the compiler flags every construction site.

- `GoogleBooksRemoteBookDataSource.kt:79` → `provider = BookProvider.GOOGLE_BOOKS`
- `OpenLibraryRemoteBookDataSource.kt:63` → `provider = BookProvider.OPEN_LIBRARY`
- `SearchBooksUseCaseImpl` copies `response.provider` into `SearchResult`.
- Test construction sites to update (counts at time of writing): `FallbackRemoteBookDataSourceTest` (3), `SearchBooksUseCaseTest` (6), `testutil/mocks/MockRemoteBookDataSource` (3), `BookshelfViewModelTest` (1), `LibraryViewModelTest` (1). Re-grep `BookSearchResponse(` and `SearchResult(` before editing.

### 3. Only paginate when page 1 came from Google

`BookshelfViewModel.kt:507` and `LibraryViewModel.kt:473`:

```kotlin
canLoadMore = searchResult.provider == BookProvider.GOOGLE_BOOKS &&
    searchResult.rawPageSize >= searchResult.pageSize,
```

OL serves up to 100 rows per page, so stopping there is acceptable for a degraded mode. Routing load-more to OL instead (passing the provider down through use case → data source) is the alternative. Rejected for now: it widens the `RemoteBookDataSource` / `SearchBooksUseCase` interfaces to fix a fallback-only path.

Tests: in both VM test classes, add `load more is disabled when results came from Open Library`. Configure `MockRemoteBookDataSource` (or the VM's use-case stub) to return a full OL page and assert `canLoadMore == false`. Follow CLAUDE.md: collect state first, `advanceUntilIdle()` after actions.

### 4. Tell the user when results came from Open Library

`book/presentation/searchcomponents/BookSearchDialog.kt` (next to the "Powered by Google" attribution, ~line 188)

- New derived flag, same pattern as `showGoogleAttribution` (line 98):
  `val showOpenLibraryNotice = remember(state.results, effectiveLibraryScope) { !effectiveLibraryScope && state.results.any { it.provider == BookProvider.OPEN_LIBRARY } }`
- When true, show a `bodySmall` / `onSurfaceVariant` line under the result count.
- New string `search_open_library_notice`:
  `Showing Open Library results. Google Books returned nothing for this search.`
  It's worded to be true for both the 503 and the empty-200 case, without claiming which. Per the copy-honesty rule, it doesn't sell the fallback as a feature. It explains why results look different from usual (fewer covers, different editions).
- An all-empty fallback (OL also returns nothing) shows the existing "No books found" state with no notice. That's fine.

### 5. About page credits Google Books

`res/values/strings.xml`

- `about_open_source_title`: `Open Source` → `Book Data` (the section isn't about open source). Rename the key to `about_book_data_title` and update `AboutDialog.kt:71` in the same change.
- `about_open_source_content` → `about_book_data_content`:
  `Book data is provided by Google Books, with the Open Library API (a project of the Internet Archive) as a backup.`
  This matches `docs/privacy.html` §Third-Party Services, which already lists both.

### 6. Fix the flaky `LibraryViewModelTest` teardown

It failed once in a full run (`existingBookIds updates when allBooks changes`, `IllegalStateException: Dispatchers.Main is used concurrently with setting it` from `resetMain()`), then passed 5/5 when run alone. It could randomly fail the release workflow, which runs `./gradlew test`.

Likely cause: each test builds a real `PreferenceDataStoreFactory.create { … }` with its own default IO scope that is never cancelled, and the ViewModel is never cleared, so background work can touch `Main` while teardown swaps it.

Fix direction: create the DataStore with `scope = TestScope(testDispatcher)` (or a `CoroutineScope(testDispatcher + Job())` cancelled in `@After`), and cancel it before `Dispatchers.resetMain()`. Check `BookshelfViewModelTest` for the same pattern. Verify by running the full suite 3× with `--rerun-tasks`.

### 7. Release

- Bump `app/build.gradle.kts`: `versionCode = 7`, `versionName = "1.0.2"` (1.0.1 / code 6 is what's live).
- Release notes: plain and user-actionable, e.g. "Search keeps working when Google Books is having problems".
- Merge `fix/search-google-outage` → `main`, then dispatch `Release to Play Store` from `main` (track: internal first, then promote).

## Verification

1. `./gradlew detekt test --rerun-tasks`: all green.
2. `./gradlew assembleRelease`, `adb install -r app/build/outputs/apk/release/app-release.apk` on the emulator.
3. Capture logs: `adb logcat -c && adb logcat -v threadtime > logcat.txt`. Release builds plant only `CrashlyticsTree`, so expect little app logging. Judge by UI plus timing.
4. In a shelf, search `test` (Google currently 503s this reliably):
   - results appear, with the Open Library notice, and **no** "Powered by Google"
   - scroll to the end: no "Load more"
   - note how long page 1 takes (retries + fallback); if it's more than ~10s, consider lowering `MAX_RETRIES` for 5xx
5. Search `harry potter`: when Google answers, you get Google results, "Powered by Google", no notice, and "Load more" works.
6. Library screen: same two checks.
7. About dialog: "Book Data" section credits Google Books and Open Library.
8. Install on a physical device and repeat 4–5.

To replay Google directly (key from `local.properties`; never paste the full key into docs):

```bash
curl -s -o /dev/null -w '%{http_code}\n' -G https://www.googleapis.com/books/v1/volumes \
  -d q=test -d maxResults=20 -d printType=books \
  -H "X-Goog-Api-Key: $KEY" \
  -H "X-Android-Package: uk.co.zlurgg.mybookshelf" \
  -H "X-Android-Cert: 9f2f9f7af4eae833b9cd3fb29a2b593d69ebbcc0"
```

## Outcome

All of §1–7 shipped as planned, one commit per section. Deviations and results:

- **§3 tests:** `LibraryViewModelTest` had `canLoadMore true when page 1 fell back to OL and returned a full 100 rows`, which asserted the opposite of the new rule. It was replaced by `load more is disabled when results came from Open Library` (drift, not a tombstone). Both VM tests also assert that `OnLoadMore` doesn't search again.
- **§6:** the DataStore now runs on `CoroutineScope(testDispatcher + Job())`, cancelled before `resetMain()`. `BookshelfViewModelTest` has no DataStore, so it needed no change. The full suite passed 3× with `--rerun-tasks` (748 tests).
- **5xx retries (follow-up in 1.0.3):** first left unchanged, because early on-device timings were 4–7s. On the Play-signed 1.0.2 build they rose to 9–12s, so 5xx now gets one retry (`HttpClientFactory.MAX_SERVER_ERROR_RETRIES = 1`) instead of `MAX_RETRIES = 3`. Network-exception retries are still 3. On 1.0.3, most fallback searches took about 4–9s on the phone. Timings are approximate (sampled every ~2s), and Open Library's own speed varied between 1.1s and 3.6s per request.
- **Verification:** steps 1–4 and 6–8 passed on the emulator and the physical device. **Step 5 (Google answering: "Powered by Google", no notice, working "Load more") is unverified**, because Google returned 503 for every query throughout testing. The owner chose not to chase it (Google was degraded across many services at the time). The path is unchanged from before this work, and unit tests cover the Google-provider pagination.
- **Seen during testing, not fixed:** an Open Library row showed `null` as its year, and an Open Library `harry potter` search included a Spanish edition (the English filter may be loose).
- **Slow outliers, not fixed:** two of about 15 timed searches on 1.0.3 took about 22s and over 60s; the same queries retried in about 5s. The likely cause is a stalled Open Library request, which waits the 20s HTTP timeout and retries socket timeouts up to 3 times. This predates this work. Release builds don't log, so it's unconfirmed. Only act on it if users report slow searches.

### Release history

| Version | Code | Contents | Track |
|---|---|---|---|
| 1.0.2 | 7 | §1–7 | internal (superseded) |
| 1.0.2 | 8 | + About privacy text no longer claims nothing goes to third parties | internal (superseded) |
| 1.0.3 | 9 | + 5xx retried once instead of three times | **production**, 2026-10-20 |

## Open question (owner decision, not part of this branch unless decided)

**Resolved 2026-10-20:** Analytics is not live. The Firebase console has no Analytics app for `uk.co.zlurgg.mybookshelf`, and the release APK has no measurement SDK. The "no analytics" wording stays. Separately, `about_privacy_content` claimed "we don't share your data with third parties", which ignored Books/Open Library searches and Crashlytics. That was reworded on `fix/about-privacy-copy`.


**Analytics wording.** `about_privacy_content` says "We don't collect analytics…" and `docs/privacy.html:249` says "We do not collect usage analytics beyond anonymous crash reports". The release runtime classpath contains no Analytics SDK (only `firebase-measurement-connector`), but the owner confirmed on 2026-06-14 that Analytics data appears in the Firebase console. Check the console (Analytics → events / data streams for `uk.co.zlurgg.mybookshelf`). If Analytics is live, fix both texts and the Play Data Safety form together.

## Out of scope

- Switching the primary provider to Open Library.
- Routing load-more to the fallback provider (see §3).
- Upgrading the CI JDK. CI sets up JDK 17, but `gradle/gradle-daemon-jvm.properties` pins toolchain 21 with foojay URLs, so Gradle downloads JDK 21 itself. That worked in the July run.
