# Preview-cache library leak

**Status:** Fixed 2026-05-27 on branch `follow-up-fixes`. The chosen approach is **Option C — repository-level preview cache**, not the Option A nav-route seed originally documented below. Reasons recorded in the "Chosen fix" section at the bottom of this doc; the original Option A design is retained for historical context.
**Origin:** Surfaced during Phase 4.1 smoke test ([closed-testing-release-prep.md](closed-testing-release-prep.md) §4.1). Latent since commit `cb611f02` (2025-09-03); made user-visible by the introduction of the Library screen (commits `d5ec0f76` → `9b839e03`).
**Scope:** Eliminates the side-effect that "tapping a search result writes the book to the local database." Net effect: the local DB only contains books the user explicitly added.

## The bug

Tapping a row in the search dialog from either Bookshelf or Library screens causes the book to appear in the user's Library, even though the user only intended to preview it.

**Reproduction:**

1. Open Library or any Bookshelf.
2. Open the search dialog, search for a book you don't already have.
3. Tap the row (not the `+` button) to see the detail screen.
4. Navigate back. Open the Library.
5. The previewed book is in the Library.

## Why this happens

Two intersecting design decisions, neither wrong in isolation:

1. **`BookDetailViewModel` reads books from local DB by ID only.** See `bookdetail/presentation/BookDetailViewModel.kt:71` (`bookDetailUseCases.getBookDetails(bookId, shelfId).first()`) and `bookdetail/domain/usecase/GetBookDetailsUseCaseImpl.kt:56` (`bookRepository.getBookById(bookId)`). `BookRepositoryImpl.getBookById` at `book/data/repository/BookRepositoryImpl.kt:22-24` is purely a DAO read — no network fallback. If the row isn't in the DB the detail screen has nothing to render.

2. **Search-result row clicks upsert the book before navigating** so the detail screen has something to read. See `bookshelf/presentation/BookshelfViewModel.kt:102-123` and `library/presentation/LibraryViewModel.kt:192-213`. Both call `upsertBook(action.book)` and then set `navigateToBook` on success.

The collision: `BookDao.getAllPersonalBooks()` (the Library query) at `core/data/database/dao/BookDao.kt:33-49` selects any `BookEntity` that is on a personal shelf **or on no shelf at all**. The OR-branch was designed for "added to library without picking a specific shelf" but it has no signal to distinguish that from "merely cached during search preview." Both look identical at the storage layer.

## Eight-month history

The bug is not a regression from recent work. Git history:

| Date | Commit | Author | Change |
|------|--------|--------|--------|
| 2025-09-03 | `cb611f02` | you | Wired `BookDetailViewModel` to load by `bookId` from local DB. To avoid empty detail screens for never-seen search results, added the upsert-on-click handler to `BookshelfViewModel.OnBookClick` with the comment `// Persist clicked book so details screen can load it by ID safely`. This is the origin of the leak. |
| 2026-04 (approx.) | `d5ec0f76` & follow-ups | you | Added the Library screen with `getAllPersonalBooks()` query — first surface that could *display* the leaked rows. The bug became visible. |
| 2026-05-19 | `9b839e03` | you | Added Library's own search dialog, ported the same upsert-before-navigate pattern. |
| 2026-05-21 | `4016bbd5` | you | Fixed a navigation race in Bookshelf (UI was navigating before the upsert completed). Split `OnBookClick` into `OnBookClick` (direct UI navigation, no upsert) and `OnSearchResultBookClick` (upserts then state-drives navigation). **Did not introduce** the upsert — only renamed and restructured around it. |

The handler that does the upsert is well-named (`OnSearchResultBookClick`) and well-commented (`Persist clicked book so details screen can load it by ID safely`). The intent was "cache so the screen works." The unintended consequence was "cache rows show up in the user's library because that's what the library query treats them as."

## Why the obvious alternative (a `cachedAt` flag) is wrong

The tempting fix is to keep the upsert and add an `isPreviewOnly: Boolean` (or `cachedAt: Long?`) column to `BookEntity`, then exclude those rows from the library query.

This is wrong because:

- Every previewed book becomes a permanent invisible row in the user's local DB.
- There is no UI path to surface or delete those rows (they're filtered out).
- Browsing a few dozen search results per session means dozens of orphan rows per session, accumulating forever.
- Mitigating this requires an eviction policy (e.g. `cachedAt` + nightly sweep deleting preview-only rows older than N days), which adds plumbing, a background-job failure mode, and still leaves "user data" containing things the user never asked for.
- It also requires a Room migration (v1 → v2), which conflicts with the schema freeze in [closed-testing-release-prep.md](closed-testing-release-prep.md) Prerequisites and would force testers to uninstall again.

The right invariant is *the local DB contains only books the user owns*. Any solution that violates that invariant invites future bugs.

## Fix design — Option A (nav-route seed)

Pass the `Book` payload from the search row through the navigation route as a serialised argument. `BookDetailViewModel` reads the local DB first (so personal notes / rating / status take precedence); if absent, falls back to the seed. The local DB is only written when the user explicitly adds the book to a shelf or library.

### Why this and not an in-memory holder

A small scoped `MutableStateFlow<Book?>` keyed by `id` was the other zero-schema-change option. It works but doesn't survive process death — if Android kills the app between the search tap and the detail screen settling, the user lands on an empty detail screen and has to re-tap from search. Nav-route encoding survives process death because the route is part of the saved back-stack state. For a small extra cost (serialisation), it's the correct durable fix.

### File-by-file changes

**Domain model — make `Book` and its enum types serialisable.**
- `book/domain/model/Book.kt` — add `@Serializable` to the `Book` data class.
- `book/domain/model/BookProvider.kt` — add `@Serializable`.
- `book/domain/model/ReadingStatus.kt` — add `@Serializable`.
- `book/domain/model/MaturityRating.kt` — add `@Serializable`.
- `book/domain/model/PrintType.kt` — add `@Serializable`.

Verify nothing else in `Book` is a non-serialisable nested type. As of writing, all fields are primitives, nullable primitives, `List<String>`, or the five enums above. `spineColor: Int` is just an Int — fine.

**Navigation route — accept an optional seed.**
- Find the existing `Route.BookDetail` (the project uses Compose type-safe routes — see the commented-out `savedStateHandle.toRoute<Route.BookDetail>().id` in the historical `BookDetailViewModel` for confirmation the pattern is in use). Add `seedBook: Book? = null` as an optional route argument.
- Decide on transport: simplest is to add a custom `NavType<Book?>` that JSON-encodes via `kotlinx.serialization.json.Json`. Alternatively pass a `seedBookJson: String?` and decode in the VM — uglier but no `NavType` plumbing. Pick the first if the project already has serialisable route args wired up; pick the second otherwise.

**ViewModel — accept and prefer-DB-fallback-to-seed.**
- `bookdetail/presentation/BookDetailViewModel.kt`: add `seedBook: Book?` constructor parameter (after `shelfId`).
- In `loadInitialBookState()`, after the DB read returns null, push the seed into state instead of null. Still attempt the description-fetch (since the seed may have a stale description).
- DI wiring in `bookdetail/di/BookDetailModule.kt` needs to forward the new route argument through `parametersOf(...)` at the `koinViewModel<BookDetailViewModel>(...)` call site in `app/presentation/MyBookShelfApp.kt:288`.

**Search-click handlers — remove the upsert.**
- `bookshelf/presentation/BookshelfViewModel.kt:102-123`: simplify `OnSearchResultBookClick` to just `_state.update { it.copy(navigateToBook = action.book) }`. Remove `upsertBook` call and its error branch.
- `library/presentation/LibraryViewModel.kt:192-213`: same simplification.
- The `LaunchedEffect(state.navigateToBook)` pattern in `BookshelfScreen.kt:78-83` and `LibraryScreenRoot.kt:17-20` continues to work — it just no longer waits on a DB write before firing. The nav-route encoding of the Book happens at the `onBookClick(book)` call site in the screen Root composables.

**Nav callsites — encode the Book into the route.**
- `app/presentation/MyBookShelfApp.kt:231` and `:258` are the two `onBookClick = { book -> ... }` handlers that turn a `Book` into a navigation event. Update both to pass `book` as the seed argument when navigating to `Route.BookDetail`.

### Test updates

- `bookshelf/presentation/BookshelfViewModelTest.kt` — `4016bbd5` added 53 lines of test coverage for the upsert-before-navigate path. Those tests need to flip: assert that `OnSearchResultBookClick` *does not* call `upsertBook`, and that `navigateToBook` is set unconditionally. The cache-error branch goes away entirely.
- `library/presentation/LibraryViewModelTest.kt` — same surgery.
- Add a `BookDetailViewModelTest` case: when local DB returns null and a seed is supplied, the detail screen renders from the seed.
- Add a `BookDetailViewModelTest` case: when local DB returns a real book, the seed is ignored (local takes precedence so personal metadata is preserved).
- Consider an integration-style test that constructs a `Route.BookDetail` with a seed, round-trips it through Compose nav's argument serialisation, and confirms the deserialised `Book` matches.

### Manual verification

1. Search for a book you have never previewed before. Tap the row. Navigate back. Library should NOT contain it.
2. Add a book to a shelf via the `+` button. Library should contain it (existing behaviour, must still work).
3. Tap a book already on a shelf to open detail. Edit personal notes. Navigate back, reopen detail. Notes still present (proves DB-first ordering).
4. Force-stop the app after tapping a search result but before the detail screen fully renders. Reopen. Detail screen should restore from the nav-route seed (proves process-death durability).
5. Quota sanity-check: this change should *reduce* network traffic slightly, since search-previewed books no longer trigger a description-fetch in the detail screen unless the user actually opens them.

## Definition of done

- [ ] `Book` and its 5 nested enum types annotated `@Serializable`. Compiles cleanly.
- [ ] `Route.BookDetail` accepts an optional `seedBook: Book?` argument; nav callsites in `MyBookShelfApp.kt:231` + `:258` pass the book through.
- [ ] `BookDetailViewModel` accepts `seedBook: Book?`; uses it as fallback when local DB returns null. DI wiring updated.
- [ ] `OnSearchResultBookClick` in both `BookshelfViewModel` and `LibraryViewModel` no longer calls `upsertBook`; navigation fires immediately.
- [ ] Existing race-fix invariants from commit `4016bbd5` still hold: no flicker, no double-navigation. (The race no longer exists because there's nothing async between click and navigate.)
- [ ] `BookshelfViewModelTest` and `LibraryViewModelTest` updated to match new behaviour; pass.
- [ ] New `BookDetailViewModelTest` cases for seed-fallback and DB-precedence; pass.
- [ ] Manual verification steps 1–4 above pass on a real device build.
- [ ] `[bookshelf-navigation-race.md](bookshelf-navigation-race.md)` annotated to note it's been superseded — the race it fixed no longer exists, and the `4016bbd5` commit's design rationale should be cross-referenced from this plan.

## Out of scope

- Pagination of search results (covered separately in [google-books-followups.md](google-books-followups.md) item 1).
- Schema migration to add a `cachedAt` column. The orphan-row problem makes that approach wrong on its own merits, regardless of release stage.
- Surfacing "books the user previewed recently" as a UI feature. If we ever want that, it should be a deliberate UX (e.g. a "Recently viewed" carousel), not a side-effect of caching. Build the feature on its own merits with its own storage.

---

## Chosen fix — Option C (repository-level preview cache)

Adopted 2026-05-27, replaces Option A. Smaller surface, no domain-model changes, no navigation-layer changes.

### Why Option A was set aside

Two things came to light after the original Option A design was written:

1. **The project's navigation layer is string-based, not type-safe.** `NavigationRoute` entries are annotated `@Serializable`, but at runtime navigation flows through `createRoute(id, shelfId) → "bookdetail/$id?shelfId=$shelfId"` with `navArgument(...) { type = NavType.StringType }`. Option A's preferred "custom `NavType<Book?>`" transport assumes type-safe routes. Falling back to a JSON-encoded `seedBookJson: String?` works but is the ugly half of the original A/B framing.
2. **Pre-release constraints relaxed.** The original plan emphasised the schema-freeze; that no longer applies (the app has no live users). With the freeze gone, the cleaner industry-standard pattern — ID-only nav + repository cache — becomes available without compatibility cost.

The orphan-row argument against the original `cachedAt`/`isPreviewOnly` schema-flag approach still stands and rules it out regardless.

### What Option C does

A process-scoped in-memory cache lives inside `BookRepositoryImpl`. `SearchBooksUseCase` writes the safe-filtered result set into it after a successful search. `BookRepository.getBookById` checks the DB first; on miss, it falls back to the cache. The DB-first ordering means personal metadata (notes, rating, reading status) always wins for books the user actually owns.

Net effect: detail screen renders any tapped search result, no DB write ever happens until the user explicitly adds the book to a shelf or library.

### File-by-file changes (as landed)

- **`book/domain/repository/BookRepository.kt`** — new method `fun cacheSearchPreviews(books: List<Book>)` with kdoc.
- **`book/data/repository/BookRepositoryImpl.kt`** — `ConcurrentHashMap<String, Book>` cache; `getBookById` falls back to it; new `cacheSearchPreviews` implementation.
- **`book/domain/usecase/SearchBooksUseCaseImpl.kt`** — new `BookRepository` constructor param; `.onSuccess { bookRepository.cacheSearchPreviews(it.books) }` after the existing `.map { … }` block.
- **`bookshelf/presentation/BookshelfViewModel.kt`** — `OnSearchResultBookClick` collapses to a single `_state.update { it.copy(navigateToBook = action.book) }`. No coroutine, no upsert, no error branch.
- **`library/presentation/LibraryViewModel.kt`** — same collapse for its `OnSearchResultBookClick`.
- **`bookshelf/domain/usecase/BookshelfUseCases.kt`** + **`bookshelf/di/BookshelfModule.kt`** — `upsertBook: UpsertBookUseCase` field removed (orphaned after the handler change). `LibraryUseCases` keeps its `upsertBook` field because `addBookToLibrary` (the explicit add flow) still uses it.

DI auto-wires the new `BookRepository` dep on `SearchBooksUseCaseImpl` via `singleOf`; no Koin module edit needed there.

### Tests landed

- **`SearchBooksUseCaseTest`** — two new cases: cache write happens exactly once on success and contains only safe-filtered books; cache write does not happen on failure. Existing assertion shape preserved.
- **`BookRepositoryImplTest`** — three new cases: `getBookById` falls back to cache when DB has no row; DB wins over cache for the same id (preserves personal metadata); `cacheSearchPreviews` stores all books in one call.
- **`BookshelfViewModelTest`** — old "upserts then sets navigateToBook" test rewritten to assert no upsert happens. Error-branch test deleted (no error path remains).
- **`LibraryViewModelTest`** — same surgery; explicit `assertNull` on `lastUpsertedBook` enforces "tap must not write to DB."
- **`MockBookRepository`** — gains `cacheSearchPreviewsCallCount` + `lastCachedPreviewIds` tracking for assertion access.

### Manual verification (still owed)

1. Search a never-previewed book. Tap row, navigate back, open Library — book must NOT appear.
2. Add a book via the `+` button — book must appear in Library (existing flow unchanged).
3. Open an already-owned book detail, edit personal notes, back, reopen — notes preserved (proves DB-precedence).
4. Force-stop the app between tap and detail render — accept empty detail screen (user re-taps from search). This is the documented trade-off for the in-memory cache.

### Behaviours that did NOT change

- The `Book` domain model is untouched. No `@Serializable` annotations added.
- `NavigationRoute.BookDetail` is untouched. No new nav arguments.
- `BookDetailViewModel`, `GetBookDetailsUseCase`, and the description-fetch flow are untouched — they still read by ID from the repository, which is now transparently cache-aware.
- The Room schema is untouched. No migration, no schema bump.
- `LibraryViewModel.addBookToLibrary` still upserts via `LibraryUseCases.upsertBook`. That's the legitimate "+ button" path, not the preview path. It stays.

### Cost / trade-off accepted

The preview cache is process-scoped only. If Android kills the app between the search-tap and the detail screen's first render, the user lands on an empty detail screen and has to re-tap. The window is small (typically sub-second on modern devices), and the cost of the alternative (Option A's `@Serializable` plumbing on the domain model + JSON in nav-route strings) is judged not worth paying for that edge case. Revisit if it bites.

### Bound on growth — clear-on-search

`cacheSearchPreviews` clears the cache before writing the new result set. Each search supersedes the previous; older entries are unreachable through the UI (the search dialog only shows the latest result set), so they would be dead weight. Memory ceiling is one search worth of books — roughly 20 visible English titles × ~2 KB each = ~40 KB. Avoids unbounded growth across long sessions.

Without this, the cache would only be reclaimed on process kill, since the `BookRepositoryImpl` singleton holds a strong reference to it for the process lifetime. GC cannot help in that arrangement.

### Bonus: search dialog now persists across the round trip

The cache enabled a UX fix that previously required a deeper change: the search dialog now stays open and retains its results when the user taps a result, previews it, and returns. Previously the click handler in `MyBookShelfApp.kt` explicitly dispatched `OnDismissSearchDialog`, which rebuilt `bookSearchState` from scratch (preserving only preferences) — so on return the user saw an empty dialog and had to re-search. With the cache in place there is no architectural need to dismiss; the dispatch was removed, and a new ViewModel test asserts that `OnSearchResultBookClick` does not toggle `isSearchDialogVisible`.

The Library callsite never auto-dismissed, so no change there — but the same VM assertion now locks that behaviour in for both screens. This retires (or downgrades) [google-books-followups.md](google-books-followups.md) item 6 for the Bookshelf surface.

### Bonus: scroll position now survives the round trip

`BookSearchDialog` renders a `LazyColumn` inside an `AlertDialog`. AlertDialog uses a platform-level window, and Compose-nav tears that window down when the user navigates to a different destination — even when the parent screen's composable scope stays alive. The `LazyListState` lived inside the dialog's saveable scope, so it died with the window and the list snapped back to the top on return.

Fix: hoist the `LazyListState` into the parent screen (`LibraryScreen` and `BookshelfScreen`) via `rememberSaveable(saver = LazyListState.Saver)`. State now lives in the screen's saveable scope (which survives nav round-trips), and is passed down through `LibraryBookSearchDialog` / `ShelfBookSearchDialog` into the shared `BookSearchDialog`. The dialog's previous internal `rememberLazyListState()` is kept as the parameter default so existing tests and previews still compile unchanged.

### Bonus: detail screen now offers "Add to Library" for previewed books

Before this fix the detail screen had a `ShelfActionsCard` gated on `state.hasShelfContext`. Books opened from Library (no shelfId) had no add affordance — the user could preview but couldn't save without going back. New `LibraryActionsCard` renders in the bottomBar when `!hasShelfContext && !isInLibrary && !isTutorialBook && !(isBookClub && !isSignedIn)`, calling `BookDetailAction.OnAddToLibraryClick(book)` which routes through `bookDetailUseCases.upsertBook(book)`. State carries new `isInLibrary: Boolean` populated from a new `getAllPersonalBooks()` flow combined into `GetBookDetailsUseCase`. Asymmetric by design — removal stays gated behind the Library screen's selection-mode + confirmation dialog so the only delete path keeps its safety net.

### Supersession

The race-fix from commit `4016bbd5` (`OnBookClick` / `OnSearchResultBookClick` split, `LaunchedEffect(state.navigateToBook)` pattern in screen roots) is preserved. The race itself no longer exists — the click handler is now synchronous so there's nothing async between tap and navigate — but the split is still useful for clarity and the `LaunchedEffect` pattern continues to work correctly. `bookshelf-navigation-race.md` should be annotated as "race no longer reachable" rather than "superseded."
