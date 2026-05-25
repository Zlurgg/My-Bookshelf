# Google Books Integration — Review Remediation Plan

**Status:** Pending — follow-up from code review of `spike-test-google-books`
**Branch:** `spike-test-google-books`
**Source:** Code review against `main` (2026-05-25)
**Scope:** Code-quality remediation only. Release-engineering concerns (Room schema reset, Crashlytics wiring, Cloud Console key restrictions, release smoke tests) are covered separately in `closed-testing-release-prep.md`, which runs **after** this plan is merged.
**Related plans:** `google-books-api-integration.md`, `google-books-search-quality.md`, `closed-testing-release-prep.md`

## Purpose

A code review of the Google Books integration branch surfaced bugs, DIP violations, test-coverage regressions, and dead code. This plan captures the findings so another session can pick up and execute the fixes without re-doing the review.

**Before starting**, the next session should read:
- `docs/specs/constitution.md` — especially the "no replicating known-wrong patterns" rule
- `docs/specs/patterns/repository.md` and `docs/specs/patterns/usecase.md`
- The original integration plan at `docs/specs/plans/google-books-api-integration.md`

## Review summary (what was found)

The architecture sketched in the integration plan is sound. The execution gaps cluster in eight areas: (a) a DIP violation that forced the fallback test to verify a duplicate class instead of production code, (b) network-layer tests promised in the plan but never written, (c) the description-fetch flow has correctness and concurrency bugs deeper than the surface-level "discarded result" comment suggests, (d) `FORBIDDEN` is overloaded (missing-key vs HTTP 403) and silently masks production breakage, (e) the Google Books API key leaks into logcat on every debug build via Ktor request logging, (f) image-URL helpers contain a copy-paste no-op plus an unverified live caller, (g) `previewLink` / `infoLink` are launched via `uriHandler.openUri` with no scheme validation and are round-tripped through Firestore (injection vector), and (h) destructive Room migration on every schema bump silently wipes user data — must be addressed before any tester device sees a v5+ build update.

**Release context:** this branch is targeting Play Store **closed testing** after sign-off. That has implications (Room migration strategy, Crashlytics wiring, key restrictions) which are handled in `closed-testing-release-prep.md`. This plan's job is to get the branch into a merge-ready state on the *code* axis; the release-prep plan picks up afterward.

## Fixes — in recommended order

### Phase 1 — Critical (blockers for merge)

> **Severity vs ordering:** items below are listed in *dependency* order — 1.1 unlocks 1.2, the description-flow fixes group together in 1.4, etc. They are **not** in severity order. By severity, items **1.6 (API key in logcat), 1.7 (URL-scheme injection via Firestore), and 1.8 (destructive Room migration)** are the highest-priority and must land before any build (including debug) leaves your local machine. If you're sequencing work, do 1.6/1.7/1.8 first; do 1.1–1.5 in the listed order around them.

#### 1.1 Fix `FallbackRemoteBookDataSource` to depend on the interface (DIP)

**File:** `app/src/main/java/uk/co/zlurgg/mybookshelf/book/data/network/FallbackRemoteBookDataSource.kt:9-12`

Currently:
```kotlin
class FallbackRemoteBookDataSource(
    private val primary: GoogleBooksRemoteBookDataSource,
    private val fallback: OpenLibraryRemoteBookDataSource,
) : RemoteBookDataSource {
```

Change both fields to type `RemoteBookDataSource`. The Koin module can keep wiring the concrete types via named qualifiers — just update the resolution to satisfy the interface parameter.

**Why first:** This unlocks fix 1.2. Without it, the production class can't be cleanly tested.

#### 1.2 Replace fake `FallbackRemoteBookDataSourceTest` with one that exercises the real class

**File:** `app/src/test/java/uk/co/zlurgg/mybookshelf/book/data/network/FallbackRemoteBookDataSourceTest.kt`

The current test instantiates a hand-rolled `FallbackDataSourceWrapper` (inner class around line 185) that duplicates the production fallback logic. The real `FallbackRemoteBookDataSource` is never under test — changing `shouldFallback` in production would not fail any test.

Once 1.1 is done, delete the wrapper and instantiate `FallbackRemoteBookDataSource` directly with two configurable stubs based on `RemoteBookDataSource` (extend the existing `StubRemoteBookDataSource` in that file, or use `MockRemoteBookDataSource` from `testutil/mocks/`).

**Design decision required before writing tests:** what should trigger fallback?

Production today (`shouldFallback` at `FallbackRemoteBookDataSource.kt:63`) only falls back on `TOO_MANY_REQUESTS` and `FORBIDDEN`. The original draft of this plan asked for fallback tests on `NO_INTERNET` and `SERVER`, which contradicts production. Pick one, then align both prod and tests:

- **Narrow (status quo):** Only quota/auth — `TOO_MANY_REQUESTS`, `FORBIDDEN`. Fallback exists *purely* to keep the app working when Google's API is gated. Recommended for now — fewer surprises, fewer doubled network calls.
- **Broad:** Add `SERVER_ERROR` (and possibly `REQUEST_TIMEOUT`). Rationale: Google 5xx with OL up is a graceful-degradation win. Cost: doubled latency on every transient Google blip; harder to alert on real Google outages because they're masked.
- **Never** fall back on `NO_INTERNET` — both APIs need the network. The original plan was wrong to ask for this test case.

Once decided, test cases to cover (assuming narrow):
- Primary succeeds → fallback never called
- Primary returns `TOO_MANY_REQUESTS` → fallback called, its result returned
- Primary returns `FORBIDDEN` → fallback called
- Primary returns `NO_INTERNET` / `SERVER_ERROR` / `REQUEST_TIMEOUT` → returned as-is, fallback not called
- Fallback also fails → fallback's error surfaces (current behavior — verify this is what we want; surfacing the *primary's* error might be more diagnostic)
- `getBookDescription` routes by `BookProvider` (does NOT fall through providers)

#### 1.3 Audit Google Books image-size helpers

**File:** `app/src/main/java/uk/co/zlurgg/mybookshelf/book/presentation/util/BookImageUtils.kt`

Three problems in this file, not just one:

**a) `withSmallImage` is a copy-paste no-op:**
```kotlin
BookProvider.GOOGLE_BOOKS ->
    imageUrl.replace("zoom=1", "zoom=1").replace("&edge=curl", "")
```
The earlier draft of this plan proposed `replace("zoom=2", "zoom=5")` — that is **also a no-op**, because Google's `volumeInfo.imageLinks.thumbnail` (what `GoogleBookMappers.kt:17` stores) contains `zoom=1`, not `zoom=2`. Verify before "fixing".

**b) `withSmallImage` and `withLargeImage` have no callers.** Only `withMediumImage` is used (`BookDetailScreen.kt:125`). Delete both per the constitution's YAGNI stance — re-introduce with their first real caller.

**c) `withMediumImage` itself is unverified.** It does `replace("zoom=1", "zoom=2")` — but Google's `zoom` parameter is not publicly documented in stable form, and `zoom=2` empirically often returns a thumbnail of similar dimensions to `zoom=1`. Don't just delete the siblings and walk away assuming the survivor is correct. Audit it:
- Build the app, open a book detail screen, inspect the actual loaded URL and image dimensions in the network log.
- If `zoom=2` doesn't visibly upscale, switch the implementation. Two options:
  1. Use `replace("zoom=1", "zoom=3")` (medium per Google's loose convention) or `"zoom=0"` (largest available).
  2. Store `imageLinks.smallThumbnail` (a *separate* URL Google returns) on `Book` alongside `thumbnail` and pick the larger of the two at render time. The DTO field already exists (`GoogleBooksSearchResponseDto.kt:40`).

Recommended: delete the unused helpers, then audit and fix the one live caller in the same change.

#### 1.4 Fix the description-fetch flow end to end

**Files:**
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookdetail/domain/usecase/GetBookDetailsUseCaseImpl.kt:77-87`
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookdetail/presentation/BookDetailViewModel.kt:53-89`

This is more than the original plan flagged. Four distinct problems, all in this flow:

**a) The fetched description is discarded** (the original finding). `loadBookDescription` retrieves the description and immediately throws it away. The comment claims it updates the book; it doesn't. Pre-existing on `main`, but the constitution forbids replicating known-wrong patterns.

**b) The fetch fires on every screen open, even when the description is already cached.** `BookDetailViewModel.kt:79` calls `loadBookDescription` unconditionally. Any book that already has a description (i.e., every book on a shelf after the first open) triggers a wasted API call. With Google Books' daily quota, this directly accelerates 429s. Guard: `if (bookDetails.book?.description.isNullOrBlank()) { … }`.

**c) The "Fix here" option from the original plan (`bookRepository.upsertBook(book.copy(...))`) would clobber personal metadata.** If `book` is the search-result `Book`, it carries default `readingStatus = NOT_READ`, `personalRating = 0f`, `personalNotes = ""`, `dateAdded = null`, `purchaseDate = null`. Upserting that overwrites the user's notes, rating, status, and timestamps on every detail-screen open. **Don't use `upsertBook` here.** Add a targeted `BookRepository.updateDescription(bookId: String, description: String?): Result<Unit, DataError.Local>` that issues a Room `UPDATE … SET description = ?` (DAO-level), and have the use case call that.

**d) The ViewModel re-queries the DB after the fetch, racing against in-flight saves.** `BookDetailViewModel.kt:82` re-runs `getBookDetails(bookId, shelfId).first()` — which re-executes `getShelfById` and `getAddedByUserId` (extra DB hits) **and** replaces `_state.book` wholesale. Between the first state update (line 58) and this second one (line 83) the user can edit personal notes (debounced 2s, well within a slow network call), toggle purchase, or change reading status. The wholesale replace silently drops any in-flight save. Replace the re-query with an in-place merge: `_state.update { it.copy(book = it.book?.copy(description = fetched)) }`.

**Recommended implementation order:**
1. Add `BookshelfDao.updateDescription(bookId: String, description: String?)` — targeted SQL `UPDATE BookEntity SET description = :description WHERE id = :bookId`.
2. Add `BookRepository.updateDescription(bookId: String, description: String?): Result<Unit, DataError.Local>` calling the DAO via `ErrorMapper.safeSuspendCall`.
3. Split `loadBookDescription` out of `GetBookDetailsUseCase` into a new `GetBookDescriptionUseCase` (returns `Result<String?, DataError.Remote>`). Add it to the `BookDetailUseCases` bundle and remove the method from `GetBookDetailsUseCase`. This isolates the fetch concern and gives the ViewModel a single-purpose injection point — see also 2.9.
4. Decide where persistence lives. Cleanest: a thin `UpdateBookDescriptionUseCase` that calls `BookRepository.updateDescription`. Acceptable: ViewModel calls the repository directly via Koin. (Strictly the constitution says ViewModels don't touch repos — go with the use case.)
5. In `BookDetailViewModel.loadBookDetails`: guard on `description.isNullOrBlank()`, fetch via `getBookDescription`, persist via `updateBookDescription`, then merge into state with `_state.update { it.copy(book = it.book?.copy(description = fetched)) }`. No re-query of `getBookDetails`.
6. Update the misleading comment in the old method (which now no longer exists if step 3 is done).

**Tests required (do not skip; this is new persistence code):**
- `BookRepositoryImpl.updateDescription` unit test — success + DataError mapping.
- Room integration test for `BookshelfDao.updateDescription` — verify the targeted UPDATE writes only the `description` column and **does not clobber** `personalNotes`, `personalRating`, `readingStatus`, `dateAdded`, `purchaseDate`. This is the test that catches a regression to the `upsertBook` anti-pattern in a future refactor.
- ViewModel test: `loadBookDetails` skips the fetch when description is non-blank.
- ViewModel test: successful fetch persists via `updateBookDescription` and the in-state book reflects the new description.

**Add a comment at the top of the DAO test class** noting that the "doesn't clobber other columns" assertion is regression insurance, not a sanity check — strictly the SQL `UPDATE … SET description = ?` cannot affect other columns by language semantics, but the test exists to fail loudly if a future "refactor" replaces it with `upsertBook`-style writes. Without that comment, a future reader will see it as tautological and delete it.

This also makes the shadowing/import issue in original section 2.3 moot — when you rewrite this block you'll naturally fix both.

**Related anti-pattern (flagged, not fixed here):** `UpdateBookMetadataUseCaseImpl.kt:44-62` does the same load-modify-save dance for personal metadata (status, rating, notes, purchaseDate). Today it's safe by accident — sequential calls re-read before each write. But if a debounced notes save is in flight while a description update lands via the new `updateDescription` path, the metadata upsert can still clobber the description (it writes the whole row). The targeted-UPDATE pattern introduced here should eventually replace `upsertBook` across personal-metadata writes too. Out of scope for this branch; track as a follow-up.

#### 1.5 Disambiguate "API key missing" from "HTTP 403"

**File:** `app/src/main/java/uk/co/zlurgg/mybookshelf/book/data/network/GoogleBooksRemoteBookDataSource.kt:33-36`

Currently, a blank `apiKey` short-circuits to `Result.Error(DataError.Remote.FORBIDDEN)`. An HTTP 403 from Google (revoked key, billing issue, IP/referer restriction) also maps to `FORBIDDEN`. `FallbackRemoteBookDataSource.shouldFallback` treats either as "fall back to OL."

Consequence: a build shipped without `GOOGLE_BOOKS_API_KEY` in `local.properties` runs without any visible failure — every search silently routes to OL. Google Books is effectively never exercised, but everything *looks* fine.

**Approach: introduce a `PROVIDER_UNAVAILABLE` error variant, fall back to OL, log loudly.**

1. Add `data object PROVIDER_UNAVAILABLE : Remote` to `DataError.Remote` in `core/domain/error/`.
2. In `GoogleBooksRemoteBookDataSource`, short-circuit the blank-key path with `Result.Error(DataError.Remote.PROVIDER_UNAVAILABLE)` plus `Timber.tag(TAG).e("Google Books API key is not configured")`.
3. Update `FallbackRemoteBookDataSource.shouldFallback` to **include** `PROVIDER_UNAVAILABLE` — OL still serves the user. The fallback's design goal is "Google unavailable → OL takes over"; missing-key is one form of Google unavailable.

**Rationale for graceful degradation over visible failure:** the fallback architecture exists precisely so that Google being unreachable — for any reason, including misconfigured/revoked/quota-exhausted keys — doesn't break search. Returning a hard error to the UI on blank-key throws away that resilience design. In production this matters because keys get revoked, billing lapses, and restrictions get misconfigured post-launch; users should keep using the app while you fix it.

**Why a new variant rather than overloading `UNKNOWN`:** `UNKNOWN` is a generic bucket. `ErrorMapper.httpNetworkCall` already emits it for unanticipated network anomalies. Excluding it from fallback to handle the blank-key case would also stop fallback on those anomalies, silently changing behavior in unrelated cases. A new variant keeps semantics unambiguous — `PROVIDER_UNAVAILABLE` is only ever emitted by this explicit short-circuit, never by the HTTP layer.

**Trade-off accepted:** with graceful degradation, the user sees a working app on blank-key — they have no way to know Google is down. The log + Crashlytics non-fatal is the only signal. This makes Crashlytics a hard prerequisite for closed testing — see `closed-testing-release-prep.md` item 2.1, which is a hard gate explicitly because 1.5 lands as graceful-degradation. The two plans share a single design here; changing one without the other breaks the chain.

#### 1.6 Stop leaking the Google Books API key in request logs

**Files:**
- `app/src/main/java/uk/co/zlurgg/mybookshelf/core/di/CoreModule.kt:35` — currently `HttpClientFactory.create(get(), enableLogging = BuildConfig.DEBUG)`
- `app/src/main/java/uk/co/zlurgg/mybookshelf/core/data/network/HttpClientFactory.kt:62-69` — Ktor `Logging` plugin at `LogLevel.ALL`, writing via `println`
- `app/src/main/java/uk/co/zlurgg/mybookshelf/book/data/network/api/GoogleBooksApiService.kt:21, 31` — key passed as `parameter("key", …)`

**Why Phase 1, not "outstanding":** every debug APK currently dumps every Google Books request URL — including `?key=…` — to logcat (via `println`, so anything that captures logcat captures the key: bug-report dumps, crash reporters, IDE session logs pasted into Slack). Closed testing distributes debug-ish APKs to people you don't fully control. This is credential exposure on every build the team has produced this branch, not a hypothetical.

**Fix — recommended approach: move the API key out of the URL into the `X-Goog-Api-Key` header.**

**Step 0 — verify header auth works for the Books API before touching code (10-minute spike).** Google's public Books API docs primarily show `?key=` query-param auth; the `X-Goog-Api-Key` header is a Google-wide convention but is not explicitly documented for this API. Run both:
```sh
curl -s -o /dev/null -w "%{http_code}\n" \
  -H "X-Goog-Api-Key: $KEY" "https://www.googleapis.com/books/v1/volumes?q=test"
curl -s -o /dev/null -w "%{http_code}\n" \
  -H "X-Goog-Api-Key: $KEY" "https://www.googleapis.com/books/v1/volumes/zyTCAlFPjgYC"
```
(Any public volume ID works for the detail call; `zyTCAlFPjgYC` is just a stable example.)
If both return `200`, proceed with the steps below. If either returns `401`/`403`, the gateway doesn't accept header auth for this API — skip to the **regex-redact fallback** below.

**Steps (assuming step 0 succeeded):**
1. In `GoogleBooksApiService.searchBooks` / `getBookDetails`, remove `parameter("key", ApiConfig.GoogleBooks.apiKey)` and replace with `header("X-Goog-Api-Key", ApiConfig.GoogleBooks.apiKey)`.
2. Leave the Ktor logging plugin at `LogLevel.ALL` for debug — it's fine to log headers; the previous version of S1 was wrong to recommend dropping log level. With the key out of the URL, headers logged is acceptable (the request body for these endpoints is empty).

**Regex-redact fallback (only if step 0 fails):** keep `?key=` and add a custom `Logger` implementation that strips the `key=…` portion before forwarding to `Timber`. Concretely:
```kotlin
logger = object : Logger {
    private val keyPattern = Regex("([?&])key=[^&\\s]*")
    override fun log(message: String) {
        Timber.tag("HttpClient").v(keyPattern.replace(message, "$1key=REDACTED"))
    }
}
```
This is brittle (a future code path that logs the key from elsewhere won't be covered) but is the second-best option if header auth isn't supported. Document the brittleness in a code comment so a future reader doesn't simplify it away.

Lighter-touch alternative if the regex feels overengineered: drop the Ktor log level from `LogLevel.ALL` to `LogLevel.HEADERS` or `LogLevel.INFO` — the URL query string is omitted entirely, keeping the key out of logs at the cost of some debug utility (no request body/response body in logs). Reach for this only if step 0 fails *and* the regex approach is rejected.

**Verification:**
- Build a debug APK after the fix, run a search, `adb logcat | grep -E "AIza[A-Za-z0-9_-]{30,}"` — should produce no matches. (Don't use `grep -i key` — it matches "keyboard," "keyguard," and every Android system log containing the word "key," all false positives.)
- Confirm Google Books still serves results (proves auth still works).

#### 1.7 Allowlist URL schemes before launching `previewLink` / `infoLink`

**Files:**
- `app/src/main/java/uk/co/zlurgg/mybookshelf/bookdetail/presentation/components/PublicationDetailsCard.kt:87, 93` — `uriHandler.openUri(url)` launches with no scheme filtering
- `app/src/main/java/uk/co/zlurgg/mybookshelf/book/data/mappers/GoogleBookMappers.kt:37-38` — where `previewLink` / `infoLink` enter the domain unchecked

**Why Phase 1 (not deferred):** the launch site exists today, the trust boundary is broken, and the attack surface is broader than "search results."
- `LocalUriHandler.openUri` resolves via `Intent.ACTION_VIEW` with no scheme filter. `intent://`, `tel:`, `javascript:`, custom schemes all fire.
- Both URLs are persisted to Room and **round-tripped through Firestore** for club shelves. A hostile club admin can write a poisoned `previewLink` into shared club state; every member who opens that book detail tries to launch the malicious URL.
- This is not a hypothetical/future-API concern — it's a live injection vector via the existing sync path.

**Fix — defense in depth, both layers:**

1. **At the mapper boundary** (`GoogleBookMappers.toBook`): coerce non-HTTPS to `null`. Don't rewrite (`http://` → `https://`) — that's correct for Google's own thumbnails but unsafe here, because a `mailto:foo` rewritten to `https://mailto:foo` is worse than dropping. Reject anything not starting with `https://`.
2. **At the launch site** (`PublicationDetailsCard.kt:87, 93`): wrap `uriHandler.openUri(url)` behind a single helper, `openExternalUrl(uriHandler, url)`, that enforces an allowlist. The strictest practical rule: `url.startsWith("https://books.google.com/")`. Looser but still safe: `url.startsWith("https://")` plus a host allowlist (`books.google.com`, `play.google.com`).

**What `openExternalUrl` does when a URL fails the allowlist:** do **not** silently no-op. A silent no-op means the user clicks "View on Google Books" and nothing happens — under attack conditions (poisoned club-shelf link), that's a UX bug at best and a "looks broken, file a bug" report at worst. Required behavior:
- Skip the `uriHandler.openUri` call (defensive — don't launch).
- Log at `Timber.tag(TAG).w(...)` with the rejected URL (truncated to avoid logging arbitrarily long strings).
- Forward to Crashlytics as a non-fatal once that's wired (`closed-testing-release-prep.md` 2.1) — a rejected URL means either the mapper coercion is broken or a poisoned link reached storage. Either is worth knowing about.
- Optionally: surface a one-shot toast/snackbar to the user ("Couldn't open link"). Lowest priority; the log + reporter are the important part.

The mapper coercion blocks the Firestore-injection vector — even if a poisoned URL is written to a club shelf elsewhere, it's null by the time it reaches the UI. The launch-site allowlist is the second line of defense against any other path that might land an unvalidated string in `previewLink`/`infoLink` (today there isn't one, but the cost of the guard is one `if`).

**Tests:**
- `GoogleBookMappersTest`: confirm `http://`, `mailto:`, `intent://`, `javascript:`, empty-string, and null inputs all produce `previewLink = null` / `infoLink = null`.
- New helper `openExternalUrlTest` if extracted into a util — verify non-allowlisted schemes are no-ops.

#### 1.8 Reset Room schema to v1 and remove destructive-migration fallback

**Files:**
- `app/src/main/java/uk/co/zlurgg/mybookshelf/core/data/database/MyBookshelfRoomDatabase.kt:20` — set `version = 1`
- `app/schemas/uk.co.zlurgg.mybookshelf.core.data.database.MyBookshelfRoomDatabase/` — delete `1.json`–`5.json`; Room regenerates a fresh `1.json` on the next build with `exportSchema = true`
- `app/src/main/java/uk/co/zlurgg/mybookshelf/core/data/database/DatabaseFactory.kt:18` — remove `.fallbackToDestructiveMigration(dropAllTables = true)`

**Why this lives in the merge-fixes plan, not the release-prep plan:** it's a pure code change with a forward-looking policy implication. It compiles, tests, and merges like any other code change. No Cloud Console or ops work. Lower inter-plan dependency surface to land it here.

**Why this branch:** the schema has crept v1 → v5 during development. With `fallbackToDestructiveMigration(dropAllTables = true)` in place, every future schema bump silently wipes books, shelves, personal ratings, reading status, and notes (club shelves rehydrate from Firestore; personal metadata does not). There are no public v1–v4 users to migrate, so backfilling `Migration(4, 5)` is wasted work — collapse the schema history, start clean at v1, and commit to real migrations from this point on.

**Steps:**
1. Set `version = 1` in `MyBookshelfRoomDatabase`.
2. Delete `1.json` through `5.json` in the schema export directory.
3. Build once locally to regenerate the schema export (a fresh `1.json`).
4. Remove `fallbackToDestructiveMigration(dropAllTables = true)` from `DatabaseFactory`. Replace with a clear comment: `// No destructive fallback. Every schema bump must register a Migration(N, N+1) here.`
5. Run integration tests — `BookshelfDaoTest` and other Room tests should be unaffected (they use in-memory databases at the current schema).
6. Commit the regenerated `1.json` so CI builds match local.

**Going forward:** any subsequent schema change must register a real `Migration` in `DatabaseFactory.databaseBuilder(...)`. The Room compiler will refuse to build without it now that the destructive fallback is gone — failures surface at build time, not install time.

**Important — existing-install impact:** any device that currently has a v5 install (your dev devices, internal testers, anyone with a sideloaded build of this branch) will **crash on launch** when v1 lands without a registered downgrade migration. Room throws `IllegalStateException` at the first DAO access; there's no graceful UX, just a startup crash.

Two ways to handle:
- **Tell affected installers to uninstall first.** Trivial at this scale (small known cohort, no public users). Add to closed-testing smoke-test prep.
- **Ship a one-cut v6 with destructive migration before collapsing to v1.** Forces a single clean wipe everywhere, then this commit follows in a separate cut. No manual instructions to testers, but adds an extra build/release cycle.

Recommend the first: uninstall before reinstall. Cheap, no extra cuts, and the cohort is small enough to message individually.

**Verification:**
- Build a release APK from this commit.
- (Optional) On a fresh emulator, install and confirm boot succeeds.
- Grep the merged branch: no `fallbackToDestructiveMigration`, `version = 1`, schema dir contains exactly `1.json`.

### Phase 2 — Significant

#### 2.1 Write `GoogleBooksRemoteBookDataSourceTest`

**Promised in plan section 4.6, not delivered.**

New file: `app/src/test/java/uk/co/zlurgg/mybookshelf/book/data/network/GoogleBooksRemoteBookDataSourceTest.kt`

Use Ktor `MockEngine` (pattern from the deleted `KtorRemoteBookDataSourceTest.kt` — recover from git history via `git log --diff-filter=D --summary` if needed for reference).

Cover:
- Query building with single-word and multi-word author filters
- Title + author + subject combined query
- Empty / blank query guard
- Blank API key returns appropriate error (or skips request — verify current behavior)
- 429 not retried (matches HttpClient retry policy change in Phase 6 of integration plan)
- DTO deserialization → domain mapping for a representative success response
- Error mapping for 4xx and 5xx responses

#### 2.2 Write `OpenLibraryRemoteBookDataSourceTest`

The deleted `KtorRemoteBookDataSourceTest.kt` (201 lines) exercised the OL data source. After the rename it was never replaced.

New file: `app/src/test/java/uk/co/zlurgg/mybookshelf/book/data/network/OpenLibraryRemoteBookDataSourceTest.kt`

Port the deleted tests' coverage to the renamed class. Same MockEngine pattern.

#### 2.3 Tidy inline-qualified `BookProvider` references in tests

**Files:** `BookRepositoryImplTest.kt:249, 263`, `BookDetailViewModelTest.kt:427`, `MockBookRepository.kt:69`

The production-code instances of inline-qualified `uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider.GOOGLE_BOOKS` (e.g. `BookDetailViewModel.kt:78`) and the local shadowing of `bookDetails` are addressed by the 1.4 rewrite — don't fix them twice. Test files still carry the same inline-qualified pattern; add a normal `import` and clean up.

#### 2.4 Delete dead code: `BookOverviewCard`

**File:** `app/src/main/java/uk/co/zlurgg/mybookshelf/bookdetail/presentation/components/BookOverviewCard.kt`

Confirmed via grep — no callers. The refactor folded its responsibility into `BookHeroSection`. KDoc still references the removed "Edition Count" field. Delete the file.

#### 2.5 Resolve `BookSearchResponse.totalResults` decision

**File:** `app/src/main/java/uk/co/zlurgg/mybookshelf/book/domain/model/BookSearchResponse.kt`

`totalResults` is plumbed through data sources and the fallback but never consumed (UI counts `state.results.size`). The plan justifies it as "ready for pagination later," which the constitution discourages.

Either:
- **Use it now:** thread into `SearchResult` and display Google's `totalItems` (more accurate than `.size`).
- **Drop it:** remove the field until pagination lands.

#### 2.6 Map `GoogleSearchInfoDto.textSnippet` into a separate `searchSnippet` field

**File:** `app/src/main/java/uk/co/zlurgg/mybookshelf/book/data/dto/google/GoogleBooksSearchResponseDto.kt:14, 50-52`

Currently parsed but never read. **Map it, don't delete.** Google's `/volumes` search response routinely returns books with `description = null` but a populated `searchInfo.textSnippet` — that's the short, context-relevant preview Google shows on its own search results. Mapping it gives the search dialog something meaningful to display when full description is absent.

**Implementation — required approach: separate field.**

Add `searchSnippet: String?` to the domain `Book` (and the corresponding column on `BookEntity` plus mapper plumbing). In `GoogleBookMappers.toBook`, read `searchInfo?.textSnippet`, run it through `stripHtml`, and store on the new field. Leave `description` set to `volumeInfo?.description` (which is `null` for most search results).

**Ordering relative to 1.8:** this item adds a column to `BookEntity`, which is a schema change. Two clean paths:
- **Land 2.6 before 1.8** (recommended) — the new column is captured in the regenerated v1 snapshot. No migration needed. Cleaner.
- **Land 2.6 after 1.8** — this becomes the first real `Migration(1, 2)`, registered in `DatabaseFactory.databaseBuilder(...)`. Workable, but you've consumed the "fresh v1 with no migrations yet" property for a feature add.

Recommend the first ordering. If you can't, the migration approach is a fine real-world exercise in the new no-destructive-fallback regime.

**Firestore sync impact:** `searchSnippet` is a per-user search artifact and **must not** be included in `BookClubBookDto` — it's not meaningful for shared club shelves, and round-tripping per-user snippets through Firestore would conflate identities and waste sync bandwidth. Confirm `BookClubBookDto` and `BookClubMappers` do not reference the new field.

**UI rendering rule:** prefer `description` when present, fall back to `searchSnippet` otherwise. Apply at:
- `BookSearchDialog.kt` — `supportingContent` on result rows (today shows authors + year; extend to show description/snippet when one exists).
- `BookDetailScreen.kt` — wherever description renders, including the gap between initial book load and the 1.4 fetch completing. Snippet acts as a stopgap; once the real description lands via 1.4's persist-then-merge, the UI naturally upgrades.

**Why not the simpler "use textSnippet as a fallback for description" approach:** it breaks the description-fetch guard from item 1.4b. If textSnippet populates `Book.description` at search time, then `if (book.description.isNullOrBlank()) fetchDescription(...)` never fires — the user is stuck looking at a truncated 100-char snippet forever (or until they hit a manual refresh that doesn't exist). The separate-field approach keeps the guard clean: `description.isNullOrBlank()` still drives the full-description fetch, and the UI displays the snippet as a stopgap until the real description arrives.

Don't drop the field — losing search-result preview text is a real UX regression.

#### 2.7 Update stale `SearchBooksUseCase` KDoc

**File:** `app/src/main/java/uk/co/zlurgg/mybookshelf/book/domain/usecase/SearchBooksUseCase.kt:13`

KDoc still says *"UseCase interface for searching books via OpenLibrary API."* Rewrite to reflect the Google-first / OL-fallback design, or drop the comment (the type signature is self-documenting).

#### 2.8 Consolidate HTML stripping (two paths, not one)

`stripHtml` runs in two places: at the mapper `GoogleBookMappers.kt:26` (during search-result mapping) and at the data source `GoogleBooksRemoteBookDataSource.kt:67` (during description fetch). An earlier draft of this plan recommended "pick the mapper, remove the data-source call" — that would **break description sanitization** because the description-fetch path does not go through any mapper.

So the real options are:

- **Status quo, documented:** keep both calls and add a one-line comment at each site explaining why (search mapper handles `volumeInfo.description`; data source handles `getBookDescription` return). This is the smallest change.
- **Introduce a description-path mapper:** something like `GoogleBookItemDto.toDescription(): String?` that owns the strip, and have `getBookDescription` call it. Cleaner separation; one place in the codebase owns "Google → domain description."
- **Move the strip to a use case:** strip in `GetBookDescriptionUseCase` (the new use case from 1.4) rather than in either data-layer site. Has the appeal of "stripping is a domain concern, not a data concern" but moves an Android dep (`HtmlCompat`) into the domain layer, which the constitution forbids.

Recommend option two: a one-liner mapper for description, used by both call sites. Removes the duplication and keeps Android deps in the data layer.

#### 2.9 Split `BookDetailViewModel.loadBookDetails` for SRP

**File:** `app/src/main/java/uk/co/zlurgg/mybookshelf/bookdetail/presentation/BookDetailViewModel.kt:53-89`

`loadBookDetails` currently does four things in one ~36-line `launch` block: (1) load book + shelf + club metadata via the use case, (2) update initial state, (3) fan out to club reviews/comments loaders, (4) fan out to the description fetch. After 1.4 lands, the body is structurally easy to split. Make `loadBookDetails` a thin orchestrator and extract two private suspend helpers (the club loaders already live in their own methods).

Recommended shape (assuming 1.4 landed first, so `getBookDescription` is its own use case and `updateBookDescription` exists):
```kotlin
private fun loadBookDetails() {
    viewModelScope.launch {
        val details = loadInitialBookState()
        if (details.isBookClub && details.clubCode != null) {
            loadClubReviews(details.clubCode)
            loadClubComments(details.clubCode)
        }
        details.book?.let { maybeFetchDescription(it) }
    }
}

private suspend fun loadInitialBookState(): BookDetailsWithShelfStatus {
    val details = bookDetailUseCases.getBookDetails(bookId, shelfId).first()
    _state.update {
        it.copy(
            book = details.book,
            onShelf = details.isOnShelf,
            isBookClub = details.isBookClub,
            clubCode = details.clubCode,
            clubCreatorId = details.clubCreatorId,
            addedByUserId = details.addedByUserId,
            isLoading = false,
        )
    }
    return details
}

private suspend fun maybeFetchDescription(book: Book) {
    if (!book.description.isNullOrBlank()) return
    bookDetailUseCases.getBookDescription(book.id, book.provider)
        .onSuccess { fetched ->
            // Persist first so the next screen open doesn't re-fetch (see 1.4b)
            bookDetailUseCases.updateBookDescription(book.id, fetched)
            // Then merge into in-memory state for the current session (see 1.4d)
            _state.update { it.copy(book = it.book?.copy(description = fetched)) }
        }
    // errors intentionally ignored — UI stays usable without description
}
```

Notes:
- The persist-then-merge order matters: persistence is the source of truth for "we don't need to re-fetch this." Merging into state without persisting (an earlier draft of this snippet did this) would reproduce the original 1.4 bug on the next screen open.
- `getBookDescription` and `updateBookDescription` are the new use cases introduced by 1.4. Don't write this until 1.4 is in.
- `loadClubReviews` and `loadClubComments` already exist as their own methods — no change needed.
- `loadInitialBookState` returns the details so the orchestrator can route without re-reading state. Avoids the "did the state actually update?" question entirely.
- Each helper has a single responsibility and can be unit-tested in isolation if needed.

Out of scope for this item: extracting the club-data fan-out into a single `loadClubData(clubCode: String)` helper would also be fine but is style polish, not SRP. Skip unless trivial.

**Related SRP smell (flagged, not fixed here):** `GetBookDetailsUseCaseImpl.invoke()` does four things — fetch shelf, fetch addedBy, observe on-shelf Flow, fetch book — combined via `combine`. The 1.4 split of `loadBookDescription` into its own use case already starts to disentangle this. A further split (e.g. `GetShelfContextUseCase`, `ObserveBookOnShelfUseCase`) would be cleaner but expands scope. Track as follow-up.

### Phase 3 — Minor / DRY / Style

#### 3.1 Extract shared query-builder helpers

**Files:** `OpenLibraryRemoteBookDataSource.kt:68-101`, `GoogleBooksRemoteBookDataSource.kt:70-101`

Both files duplicate `sanitizeFilterInput`, `formatFilterField`, and the `buildQuery` skeleton — ~30 lines each, differing only by prefix mapping (OL uses `author:`, `title:`, `subject:`; Google uses `inauthor:`, `intitle:`, `subject:`).

Extract a `BookSearchQueryBuilder` (in `book/data/network/`) parameterized by a `prefix` map. Single place to test query construction.

#### 3.2 Remove dead branch in OL `formatFilterField`

**File:** `app/src/main/java/uk/co/zlurgg/mybookshelf/book/data/network/OpenLibraryRemoteBookDataSource.kt:70-74`

Accepts a nullable prefix but every caller passes non-null. Tighten the signature.

#### 3.3 Magic-string cleanup

- `app/src/main/java/uk/co/zlurgg/mybookshelf/book/data/network/api/GoogleBooksApiService.kt:23` — extract `"books"` to `ApiConfig.GoogleBooks.DefaultParams.PRINT_TYPE_BOOKS`.
- `app/src/main/java/uk/co/zlurgg/mybookshelf/book/data/mappers/GoogleBookMappers.kt:13` — extract `"ISBN_13"` to a constant.

#### 3.4 Reconcile `MAX_RESULTS` with plan

**File:** `app/src/main/java/uk/co/zlurgg/mybookshelf/core/data/network/ApiConfig.kt`

Plan specifies 15; code uses 40 (Google's per-page hard max). See `google-books-search-quality.md` — that plan already prefers 40 because the ViewModel was capping to 15. Decide which doc wins and align: either update the integration plan to 40 or update the code to 15.

#### 3.5 Inline string resources in `BookSearchDialog`

**File:** `app/src/main/java/uk/co/zlurgg/mybookshelf/book/presentation/searchcomponents/BookSearchDialog.kt:138, 144`

`"No books found for \"…\""` and `"Try different keywords or check your spelling"` are hardcoded. Pre-existing (blame: 2025-10-16), flagged per constitution. Move to `strings.xml`.

#### 3.6 Defensive parsing for `publishedDate`

**File:** `app/src/main/java/uk/co/zlurgg/mybookshelf/book/data/mappers/GoogleBookMappers.kt:28`

`firstPublishYear = volumeInfo?.publishedDate?.take(YEAR_LENGTH)` assumes ISO format. Usually correct (`"2010"`, `"2010-05"`, `"2010-05-15"`) but Google also returns `"c. 2010"`, `"19??"`, `"2010s"` for historical/uncertain dates — `.take(4)` then yields `"c. 2"` / `"19??"` / `"2010"`. The garbage strings reach the DB and display verbatim via `publication_first_published_label`.

Cheap fix: `publishedDate?.let { Regex("\\d{4}").find(it)?.value }`.

#### 3.7 Case-insensitive enum parsing

**Files:** `MaturityRating.kt:9-13`, `PrintType.kt:9-13`

`fromApiValue` uses exact-match `when (value) { "NOT_MATURE" -> … }`. If Google ever returns `"mature"` or `"Book"` it silently falls to `UNKNOWN`. Switch to `equals(..., ignoreCase = true)` (or normalize with `value?.uppercase()` before the when). Trivial change, removes a brittle assumption.

### Phase 4 — Performance (low priority)

#### 4.1 Memoize provider check in `BookSearchDialog`

**File:** `app/src/main/java/uk/co/zlurgg/mybookshelf/book/presentation/searchcomponents/BookSearchDialog.kt:195`

`state.results.any { it.provider == BookProvider.GOOGLE_BOOKS }` runs every recomposition. Wrap in `remember(state.results) { … }`. Negligible unless results grow large.

## Security review

Verified clean:
- API key gated via `local.properties` → `BuildConfig` (`app/build.gradle.kts:61, 75`) with empty-string default.
- HTTP `imageUrl` (thumbnail) forced to HTTPS in Google mapper.
- 429 not retried (avoids amplification under quota exhaustion).

Note: the previous version of this section claimed HTML sanitization prevents XSS. That isn't accurate — nothing renders `description` in a WebView; Compose `Text` renders plain strings safely regardless. `stripHtml` is a UX/readability concern, not a security boundary.

Outstanding items (added from re-review):

#### S1. API key leaks via Ktor request logging — **see Phase 1.6**

Promoted to Phase 1 after verifying `CoreModule.kt:35` wires `enableLogging = BuildConfig.DEBUG`. Every debug APK genuinely emits the key to logcat. See item 1.6 for the fix (header auth via `X-Goog-Api-Key`).

#### S2. Validate scheme before launching `previewLink` / `infoLink` — **see Phase 1.7**

Promoted to Phase 1 after verifying the launch site already exists (`PublicationDetailsCard.kt:87, 93`) and recognizing the Firestore round-trip attack surface (a hostile club admin can inject a poisoned link into shared state). See item 1.7 for the fix.

#### S3. `SafeSearchFilter` known false-positive class

Documented trade-off, not a bug: the keyword list (`"sexual"`, etc.) over-blocks legitimate academic titles (e.g. *Sexual Selection in Primates*). Acceptable for now given the offline-first + family-friendly stance, but add a one-line comment in `SafeSearchFilter.kt` so the next reader doesn't "fix" it by removing keywords.

## Definition of done

A merge-ready branch satisfies all Phase 1 items, all Phase 2 items, and as many Phase 3/4 items as time allows. The previous draft of this plan allowed 2.5/2.6 to be deferred — that is no longer the recommended posture for 2.6 (map `textSnippet`); 2.5 (`totalResults`) can still be deferred with a written rationale.

"Merge-ready" here means the branch can land on `main` cleanly — it does **not** mean "ready for closed testing." The latter is gated by `closed-testing-release-prep.md`.

Specifically:
- [ ] `FallbackRemoteBookDataSource` depends only on `RemoteBookDataSource` (1.1)
- [ ] `FallbackRemoteBookDataSourceTest` instantiates the production class (no wrapper) (1.2)
- [ ] Fallback semantics decision recorded (narrow vs broad) and prod + tests agree (1.2)
- [ ] `withSmallImage` / `withLargeImage` deleted; `withMediumImage` audited and confirmed to produce a meaningfully different image (or switched to `smallThumbnail`) (1.3)
- [ ] Description fetch no longer fires when `book.description` is already populated (1.4b)
- [ ] Description fetch persists via targeted `BookshelfDao.updateDescription` + `BookRepository.updateDescription` (NOT `upsertBook`) (1.4)
- [ ] `loadBookDescription` is split into its own use case (`GetBookDescriptionUseCase`) (1.4)
- [ ] `BookDetailViewModel.loadBookDetails` merges the description into state without re-querying the DB (1.4d)
- [ ] `updateDescription` DAO test verifies targeted UPDATE does not clobber personal metadata (1.4)
- [ ] Blank-API-key path short-circuits with the new `DataError.Remote.PROVIDER_UNAVAILABLE` variant; `shouldFallback` includes it; `Timber.e` log emitted at the short-circuit site (1.5)
- [ ] Google Books API key sent via `X-Goog-Api-Key` header, not URL query param — verified via `adb logcat` grep (1.6)
- [ ] `previewLink` / `infoLink` HTTPS-coerced in the mapper and gated behind a scheme allowlist at the launch site; mapper tests cover hostile schemes (1.7)
- [ ] Room schema reset to v1, `fallbackToDestructiveMigration` removed, schema dir contains exactly `1.json`; uninstall instructions communicated to internal testers (1.8)
- [ ] `GoogleBooksRemoteBookDataSourceTest` exists and covers the listed cases (2.1)
- [ ] `OpenLibraryRemoteBookDataSourceTest` exists and recovers coverage from the deleted file (2.2)
- [ ] Inline-qualified `BookProvider` references removed from test files (2.3)
- [ ] `BookOverviewCard.kt` is deleted (2.4)
- [ ] `totalResults` is either used or removed with a written rationale (2.5)
- [ ] `textSnippet` is mapped into a separate `Book.searchSnippet` field (not into `description`) (2.6)
- [ ] `BookClubBookDto` and `BookClubMappers` do not reference `searchSnippet` (verified by grep) (2.6)
- [ ] `SearchBooksUseCase` KDoc updated or removed (2.7)
- [ ] HTML stripping has a single owner per path (search vs description) — comment or shared mapper (2.8)
- [ ] `BookDetailViewModel.loadBookDetails` is a thin orchestrator with extracted `loadInitialBookState` / `maybeFetchDescription` helpers (2.9)
- [ ] All Detekt rules pass; all tests pass (including the new `updateDescription` tests in 1.4)

## Out of scope

- Pagination UI (would be a separate plan)
- Replacing OL fallback entirely (kept as designed)
- Shared `HttpClient` retry policy tuning per provider (current one-size-fits-all is a known small inefficiency, not a bug)
- All release-engineering work — Room schema reset, Crashlytics wiring, Cloud Console API key restrictions, release-build smoke tests. See `closed-testing-release-prep.md`.
