# "Show my library" search-scope toggle

**Status:** Plan drafted 2026-05-28 on branch `follow-up-fixes`. Awaiting review before a fresh session implements.
**Origin:** Surfaced during manual verification of the v3 column-update fix (2026-05-28). User observation: when adding a book to a new shelf, common case is re-adding a book the user already owns — yet the only search source today is Google Books, so the user has to re-search the internet for a book they've previously curated. A scope toggle in the search dialog would let them search their own library instead.
**Scope:** Add a toggle to the Bookshelf and Book Club search dialogs (alongside Safe Search) that swaps the result source from Google Books to the user's local library. Toggle persists across sessions like Safe Search does. Library search dialog does **not** get this toggle — that screen *is* the user's library.

## Goal

When the user has owned books that they'd like to add to another shelf (typical case: re-shelving a re-read, or curating a club shelf from books they already have), let them search their library without re-querying Google Books. The existing "tap row to preview / `+` to add" flow stays the same; only the source of results changes.

## Where it applies

| Dialog | Toggle present? | Why |
|---|---|---|
| `BookSearchDialog` (base) | yes — new prop | Default off |
| `ShelfBookSearchDialog` (bookshelf) | yes | Add-to-shelf is the primary use case |
| `LibraryBookSearchDialog` (library) | **no** | Library IS the user's books; toggle would be redundant |
| Book-club search (uses `ShelfBookSearchDialog`?) | yes | Same use case as personal shelves |

(Implementer: verify whether book-club search reuses `ShelfBookSearchDialog` or has its own variant — the toggle should appear wherever a shelf-bound search dialog appears.)

## UX

The toggle sits in the same row as Safe Search. Label suggestion: **"My library only"** (concise; matches the noun-phrase style of "Safe Search"). When on:

- Result list shows books from the user's library (`getAllPersonalBooks()`), filtered by the query and the title/author/subject toggles.
- Safe Search toggle is **disabled** (greyed out) — Google's maturity filter doesn't apply to local books. Tooltip / disabled state communicates "Not used when searching your library."
- Subject toggle is **disabled** for the same reason — local books don't carry searchable subject data on the same shape as Google's `subject:` qualifier. (Implementer: confirm whether `BookEntity.subjects` is rich enough to support this; if yes, enable.)
- Empty query: show all library books (no min-length gate). Local source is in-memory, no network, no point gating.
- Empty result with non-empty query: `"No library books match \"$query\""`.
- Empty library entirely: `"You don't have any books in your library yet. Turn off this toggle to search Google Books."` (or similar — keep concise).

When off, behaviour is identical to today.

Per-result interaction is unchanged:
- Tap row → preview detail screen (uses the preview cache; the local book is also persisted, so cache fallback isn't needed for already-owned books).
- `+` → add to current shelf via `AddBookToShelfUseCase`. Already correctly preserves all personal metadata when the book is already in the DB (`AddBookToShelfUseCaseImpl.kt:48-63`).

## Data model changes

**`SearchPreferences.kt`** — add one field, mirror the Safe Search pattern:

```kotlin
data class SearchPreferenceState(
    val searchByTitle: Boolean = true,
    val searchByAuthor: Boolean = false,
    val searchBySubject: Boolean = false,
    val safeSearchEnabled: Boolean = true,
    val libraryScopeEnabled: Boolean = false,  // new
)
```

`SearchPreferencesImpl` adds a `BOOLEAN_PREFERENCES_KEY("library_scope_enabled")` and reads/writes it through the same DataStore. Default false so existing users (and the Library dialog, which won't surface the toggle) behave as today.

**`BookSearchState.kt`** — add one field:

```kotlin
data class BookSearchState(
    ...
    val libraryScopeEnabled: Boolean = false,
    ...
)
```

Plumb through the existing `observeSearchPreferences()` chain in each ViewModel that consumes `SearchPreferences` (currently `BookshelfViewModel`, `LibraryViewModel`, and any book-club variant). `LibraryViewModel` reads it but `LibraryBookSearchDialog` doesn't surface the toggle, so a stale persisted `true` from a previous session has no UI effect there.

## Architectural shape

Two paths under consideration. Pick **option B** unless implementer surfaces a reason against it.

### Option A — branch inside `SearchBooksUseCase`

Add a `source: SearchSource` enum parameter. The use case dispatches to either `RemoteBookDataSource.searchBooks(...)` or a new repository method `searchLibraryBooks(query, byTitle, byAuthor): List<Book>`. Single entry point for the ViewModel.

Downside: bloats one use case with two unrelated responsibilities (network vs in-memory) and one signature with conditional-meaning params.

### Option B — separate use case (Recommended)

Add `SearchLibraryBooksUseCase` alongside the existing `SearchBooksUseCase`. The ViewModel's `performSearch()` chooses which to call based on `state.bookSearchState.libraryScopeEnabled`. Each use case has one job.

```kotlin
class SearchLibraryBooksUseCaseImpl(
    private val bookRepository: BookRepository,
) : SearchLibraryBooksUseCase {
    override suspend operator fun invoke(
        query: String,
        searchByTitle: Boolean,
        searchByAuthor: Boolean,
    ): Result<List<Book>, DataError.Local> {
        // bookRepository.getAllPersonalBooks() is a Flow — take the current value
        // via .first() and filter in-memory. No need to expose a SQL-level filter
        // when results are small (typical libraries: 10s-100s of books).
        val books = bookRepository.getAllPersonalBooks().first()
        val trimmedQuery = query.trim()
        val filtered = if (trimmedQuery.isEmpty()) {
            books
        } else {
            val q = trimmedQuery.lowercase()
            books.filter { book ->
                (searchByTitle && book.title.lowercase().contains(q)) ||
                    (searchByAuthor && book.authors.any { it.lowercase().contains(q) })
            }
        }
        return Result.Success(filtered)
    }
}
```

No new repository method needed — `getAllPersonalBooks()` exists. SQL-level filtering is unnecessary at typical library sizes; if libraries grow to thousands, revisit with a DAO `@Query` that does `LIKE` filtering.

### ViewModel changes

In `BookshelfViewModel.performSearch()` (and the book-club variant if separate):

```kotlin
private suspend fun performSearch() {
    _state.update { it.copy(bookSearchState = it.bookSearchState.withLoading()) }
    val searchState = _state.value.bookSearchState

    val result = if (searchState.libraryScopeEnabled) {
        bookshelfUseCases.searchLibraryBooks(
            query = searchState.query,
            searchByTitle = searchState.searchByTitle,
            searchByAuthor = searchState.searchByAuthor,
        ).map { BookSearchResult(books = it, filteredCount = 0) }
    } else {
        // existing remote search
    }

    result.onSuccess { ... }.onError { ... }
}
```

A new `BookshelfAction.OnToggleLibraryScope` action mirrors `OnToggleSafeSearch`:

```kotlin
BookshelfAction.OnToggleLibraryScope -> {
    _state.update {
        it.copy(
            bookSearchState = it.bookSearchState.copy(
                libraryScopeEnabled = !it.bookSearchState.libraryScopeEnabled
            )
        )
    }
    persistSearchPreferences()
    retriggerSearchIfNeeded()
}
```

`retriggerSearchIfNeeded()` already handles the case where the current query needs to be re-run after a filter change — no changes there.

Library scope being on overrides `MIN_SEARCH_QUERY_LENGTH`: when the toggle is on AND query is empty, we still want to show all library books rather than the "type something" empty state. Easiest shape: special-case in `observeDebouncedQuery` to skip the length check when `state.bookSearchState.libraryScopeEnabled`.

## UI changes

`BookSearchDialog.kt` gains two props:

```kotlin
@Composable
fun BookSearchDialog(
    ...
    showLibraryScopeToggle: Boolean,             // new — false from LibraryBookSearchDialog
    onToggleLibraryScope: () -> Unit,            // new
)
```

The toggle row that currently houses Safe Search gets a sibling toggle (or replaces Safe Search when library scope is on — exact layout is implementer's call; the visual contract is "two related toggles in the same row, library scope visually adjacent to the result source it controls").

`ShelfBookSearchDialog.kt` and any book-club variant pass `showLibraryScopeToggle = true` and wire `onToggleLibraryScope` to the new action. `LibraryBookSearchDialog.kt` passes `showLibraryScopeToggle = false` and the no-op callback.

`BookSearchCallbacks` interface gains `val onToggleLibraryScope: () -> Unit`. Default the implementations in `LibraryScreen` to no-op so the Library screen ignores the toggle even if the prop sneaks through.

## Tests

| Layer | Test | What it locks |
|---|---|---|
| Use case | `SearchLibraryBooksUseCaseTest` — empty query returns all books | Default behaviour when toggle is on with no query |
| Use case | filters by title when only `searchByTitle = true` | Toggle interactions |
| Use case | filters by author when only `searchByAuthor = true` | Toggle interactions |
| Use case | `searchByTitle || searchByAuthor` when both — OR semantics | Matches existing remote semantics |
| Use case | case-insensitive match | Locks the `.lowercase()` choice |
| ViewModel | `OnToggleLibraryScope flips state, persists, re-runs query` | Lifecycle parity with Safe Search |
| ViewModel | search with `libraryScopeEnabled = true` calls `SearchLibraryBooksUseCase`, not `SearchBooksUseCase` | Dispatcher choice |
| ViewModel | empty query is allowed when `libraryScopeEnabled = true` | The min-length override |
| Repository | not touched | `getAllPersonalBooks()` already covered |
| Screen | (optional) Compose UI test that the toggle renders in Shelf dialog and not in Library dialog | Visual contract |

## Out of scope (defer)

- **SQL-level filtering.** If user libraries grow to thousands of books, in-memory filtering becomes noticeable. Add a DAO `@Query("... WHERE title LIKE :q OR authors LIKE :q")` then. Pre-release latitude doesn't pressure this.
- **Combining sources** ("search both my library AND Google Books, library first"). The simpler scope-toggle UX is the right starting point. Combining is a separate UX question — would need either a third toggle state ("both") or a different control entirely.
- **Search by subject when toggle on.** `BookEntity.subjects` exists but the filtering semantics don't map cleanly to a `subject:` qualifier. If the implementer finds it trivial they can include it; otherwise disable the subject toggle when library-scope is on, with a tooltip.
- **Sort ordering.** Local books come back in `getAllPersonalBooks()` order (likely insertion or alphabetical — implementer to check). If a different order matters for this use case, add a `sortBy` arg to the use case. Not in v1.

## Open decisions

1. **Label wording.** "My library only" / "Search my library" / "Local search" / "Owned books." Implementer's call — pick whatever sits well in the toggle row layout.
2. **Layout when both toggles enabled in the row.** Same line with a divider, or stacked? Implementer's call based on screen width.
3. **Disabling Subject and Safe Search toggles vs. hiding them when library scope is on.** Disabling preserves the user's mental model of the dialog ("the toggles are still there, just not used right now"); hiding gives more vertical space. Recommend disable, but flexible.
4. **Min-query override** — show all library books on empty query (current recommendation) vs. require some text input for parity with remote. Recommend show-all; empty-query in a library that's small enough to scroll is genuinely useful.

## Execution order

1. **Data model.** Add `libraryScopeEnabled` to `SearchPreferenceState`, persist via `SearchPreferencesImpl`. Add to `BookSearchState`.
2. **Use case.** New `SearchLibraryBooksUseCase` + impl + DI wiring in `BookshelfModule` (and book-club module if separate).
3. **ViewModel.** Wire toggle observation, add `OnToggleLibraryScope` action, branch `performSearch()`, allow empty-query when toggle is on. Apply to `BookshelfViewModel` and any book-club variant. **Do not** apply to `LibraryViewModel` — toggle is hidden there.
4. **UI.** Add `showLibraryScopeToggle` + `onToggleLibraryScope` to `BookSearchDialog`, `ShelfBookSearchDialog`, `LibraryBookSearchDialog`. Library passes false + no-op. Render the toggle in the existing toggle row; disable Safe Search (and Subject if not implementing local subject search) when library scope is on.
5. **Tests.** Use-case + ViewModel tests per the table above.
6. **Manual device verification.**
   - Open a Bookshelf search → toggle "My library only" → confirm results switch to owned books.
   - Type a query → results filter case-insensitively.
   - Clear the query → all library books visible.
   - Try with an empty library → empty-state message.
   - Toggle off → back to Google Books.
   - Toggle persists across app restart.
   - Library dialog does not show the toggle.
   - Tap a result → detail screen renders (preview cache path).
   - `+` adds to the current shelf and (importantly) preserves the existing personal metadata — confirms `AddBookToShelfUseCaseImpl`'s preservation branch is on the happy path here.
7. **Commit.** Single commit: `feat(search): add "My library only" scope toggle to shelf search dialogs`.
