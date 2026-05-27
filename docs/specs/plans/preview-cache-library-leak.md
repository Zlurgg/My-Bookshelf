# Preview-cache library leak

**Status:** Documented 2026-05-27 during closed-testing smoke test. Not yet scheduled. Targeted for the session immediately after closed testing ships, before opening to wider testers.
**Origin:** Surfaced during Phase 4.1 smoke test ([closed-testing-release-prep.md](closed-testing-release-prep.md) §4.1). Latent since commit `cb611f02` (2025-09-03); made user-visible by the introduction of the Library screen (commits `d5ec0f76` → `9b839e03`).
**Scope:** Eliminates the side-effect that "tapping a search result writes the book to the local database." Switches search-result navigation to seed `BookDetailViewModel` via a nav-route argument rather than via a pre-emptive upsert. Net effect: the local DB only contains books the user explicitly added.

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
- Schema migration to add a `cachedAt` column. The whole point of choosing Option A is to avoid this.
- Surfacing "books the user previewed recently" as a UI feature. If we ever want that, it should be a deliberate UX (e.g. a "Recently viewed" carousel), not a side-effect of caching. Build the feature on its own merits with its own storage.
