# Library: Add Book

Add books directly to the library via an OpenLibrary search dialog, triggered by a FAB on the library screen.

## Problem

The library screen is read-only. Users can only get books into it by adding them to a shelf first. We need a direct "add to library" flow. The search functionality (`SearchBooksUseCase`) currently lives in `bookshelf/` which would create a cross-feature dependency if library imported it.

## Architecture Decisions

### Move SearchBooksUseCase to `book/` (shared domain)

Searching for books is a book-level concern, not a shelf concern. `SearchBooksUseCase` depends on `RemoteBookDataSource` which already lives in `book/data/network/`.

**Move:**
- `bookshelf/domain/usecase/SearchBooksUseCase.kt` -> `book/domain/usecase/SearchBooksUseCase.kt`
- `bookshelf/domain/usecase/SearchBooksUseCaseImpl.kt` -> `book/domain/usecase/SearchBooksUseCaseImpl.kt`

**Update imports in:**
- `bookshelf/domain/usecase/BookshelfUseCases.kt`
- `bookshelf/presentation/BookshelfViewModel.kt`
- `bookshelf/di/BookshelfModule.kt`
- Any tests referencing the old location

### Extract shared search UI to `book/presentation/searchcomponents/`

`BookSearchBar`, `AdvancedSearchFilters`, `BookSearchState`, and the base `BookSearchDialog` have zero shelf-specific logic. Move them to `book/` and make the dialog accept a `trailingContent` slot for feature-specific actions.

**Move to `book/presentation/searchcomponents/`:**
- `BookSearchBar.kt` (no changes)
- `AdvancedSearchFilters.kt` (no changes — keep original name, avoid unnecessary rename churn)
- `BookSearchState.kt` (rename `inShelfIds` -> `existingBookIds`, add `errorMessage`, add field defaults)

**`inShelfIds` rename blast radius** (all 4 source files):
- `bookshelf/presentation/searchcomponents/BookSearchState.kt` (field definition — moves to `book/`)
- `bookshelf/presentation/BookshelfScreen.kt:347` (constructs `BookSearchState` with `inShelfIds =`)
- `bookshelf/presentation/searchcomponents/BookSearchDialog.kt:127` (`state.inShelfIds.contains(book.id)` — moves to `ShelfBookSearchDialog`)
- `bookshelf/presentation/searchcomponents/BookSearchDialog.kt:200` (preview, `inShelfIds = emptySet()`)

**New in `book/presentation/searchcomponents/`:**
- `BookSearchDialog.kt` — shared dialog composable with a `trailingContent` slot

**Bookshelf keeps:**
- A thin wrapper composable (`ShelfBookSearchDialog.kt`) that calls the shared `BookSearchDialog` and provides the shelf-specific trailing content (add/remove toggle based on `existingBookIds`)
- `BookSearchCallbacks.kt` — shelf-specific callbacks interface (has `onRemoveBook`)

**Library gets:**
- A thin wrapper composable (`LibraryBookSearchDialog.kt`) that calls the shared `BookSearchDialog` and provides library-specific trailing content (add button / "already in library" indicator)

### Shared BookSearchDialog signature

```kotlin
// book/presentation/searchcomponents/BookSearchDialog.kt
@Composable
fun BookSearchDialog(
    state: BookSearchState,
    onQueryChange: (String) -> Unit,
    onToggleSearchByTitle: () -> Unit,
    onToggleSearchByAuthor: () -> Unit,
    onBookClick: (Book) -> Unit,
    onDismiss: () -> Unit,
    trailingContent: @Composable (book: Book, isExisting: Boolean) -> Unit
)
```

The dialog owns: AlertDialog layout, search bar, filters, loading/empty/error states, result list with `ListItem`. When `state.errorMessage` is non-null, show it inline above the results area (e.g. `Text` with `MaterialTheme.colorScheme.error`). Each feature provides only the trailing icon/button per result.

### "Already in library" tracking

No new query needed. `LibraryViewModel` already has `allBooks` in state. Derive `existingBookIds` from it:
```kotlin
val inLibraryIds = state.allBooks.map { it.id }.toSet()
```
Pass this as `BookSearchState.existingBookIds` when the search dialog is open.

**Book ID identity verified**: OpenLibrary returns `/works/OL123W`, the `toBook()` mapper extracts `OL123W` via `substringAfterLast("/")`, and that ID is preserved through upsert -> entity -> `getAllPersonalBooks()`. The `inLibraryIds` set uses the same extracted ID format. Roundtrip test exists in `BookMappersTest`.

### No AddBookToLibraryUseCase — use UpsertBookUseCase directly

`UpsertBookUseCase` already does exactly what "add to library" needs: preserves personal metadata for existing books, handles spine colour generation via the repository. Wrapping it in a single-line delegation use case would be a YAGNI violation. Add `upsertBook: UpsertBookUseCase` to `LibraryUseCases` directly.

### Add defaults and error field to BookSearchState

`BookSearchState` currently requires all fields at construction. During the move to `book/`, add default values and an `errorMessage` field:

```kotlin
data class BookSearchState(
    val query: String = "",
    val results: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val isTyping: Boolean = false,
    val hasSearched: Boolean = false,
    val existingBookIds: Set<String> = emptySet(),
    val searchByTitle: Boolean = true,
    val searchByAuthor: Boolean = true,
    val errorMessage: String? = null
)
```

**Why defaults:** Every consumer (`LibraryState`, `ShelfBookSearchDialog` preview, future features) benefits from `BookSearchState()` constructing a sensible empty state instead of spelling out all fields.

**Why `errorMessage`:** Bookshelf currently conflates search errors with empty results — `withSearchError()` clears results and sets `hasSearched = true`, so the user sees "No books found" for both API failures and genuinely empty searches. Adding `errorMessage` to the shared state lets the shared `BookSearchDialog` show an inline error message (e.g. "Search failed — check your connection") above the results area, distinct from the "no results" empty state. Bookshelf's `withSearchError()` should be updated to set this field instead of (or in addition to) the parent state's `errorMessage`.

### Compose BookSearchState into LibraryState

Rather than adding 8 flat search fields to `LibraryState`, embed `BookSearchState`:

```kotlin
data class LibraryState(
    // ... existing fields ...
    val isSearchDialogVisible: Boolean = false,
    val bookSearchState: BookSearchState = BookSearchState()
)
```

This avoids state field explosion, gives a single object to pass to the shared `BookSearchDialog`, and keeps the library-specific concern (`isSearchDialogVisible`) separate from the search-generic concern.

### OnSearchResultBookClick must upsert before navigating

Bookshelf's `OnBookClick` handler (`BookshelfViewModel.kt:78-88`) calls `upsertBook(action.book)` to persist the book before navigating to BookDetail. BookDetail reads from the local database, so if the book isn't cached first, it won't be found. Library's `OnSearchResultBookClick` must replicate this pattern: upsert the book, then navigate on success.

### Debounce: 300ms consistently

The current `LibraryViewModel` uses 300ms debounce for local search filtering. Use the same 300ms for remote search. The 50ms difference with bookshelf's 250ms is imperceptible but the inconsistency within the same ViewModel would be noticeable.

## Implementation Steps

### PR 1: refactor: extract shared search to `book/`

Phases 1+2 are pure refactoring with no new functionality. One PR keeps the review focused.

#### Phase 1: Relocate search to `book/` (shared domain)

1. Move `SearchBooksUseCase` + `SearchBooksUseCaseImpl` from `bookshelf/domain/usecase/` to `book/domain/usecase/`
2. Update all imports in bookshelf (ViewModel, UseCases aggregator, DI module)
3. Move `BookSearchBar.kt`, `AdvancedSearchFilters.kt`, `BookSearchState.kt` to `book/presentation/searchcomponents/`
4. **Do NOT rename `inShelfIds` yet** — the two `BookSearchDialog.kt` references move/rewrite in phase 2. Renaming the field while those references still exist creates a broken intermediate state. Defer the full `inShelfIds` -> `existingBookIds` rename to step 8.
5. Update all bookshelf import statements to reference `book.presentation.searchcomponents.*`
6. Move DI registration for `SearchBooksUseCase` from `BookshelfModule` to `BookModule`
7. Build + run existing bookshelf search tests to verify no regressions

#### Phase 2: Extract shared BookSearchDialog

8. Rename `BookSearchState.inShelfIds` -> `existingBookIds` atomically with the dialog extraction. All 4 references change in this step:
   - `book/presentation/searchcomponents/BookSearchState.kt` (field definition)
   - `bookshelf/presentation/BookshelfScreen.kt:347` (`inShelfIds =` -> `existingBookIds =`)
   - The two `BookSearchDialog.kt` references (rewritten as part of the shared dialog + wrapper below)
9. Create `book/presentation/searchcomponents/BookSearchDialog.kt` with the shared composable (extract from current bookshelf dialog, replace trailing content with slot parameter)
10. Create `bookshelf/presentation/searchcomponents/ShelfBookSearchDialog.kt` — thin wrapper providing shelf-specific trailing content (add/remove toggle based on `existingBookIds`)
11. Replace usage of old `BookSearchDialog` in `BookshelfScreen` with `ShelfBookSearchDialog`
12. Remove old `bookshelf/presentation/searchcomponents/BookSearchDialog.kt`
13. Verify bookshelf search renders identically (build + manual check + any existing preview tests)

#### Phase 3: Compose BookSearchState into BookshelfState

Refactor `BookshelfState` to use the shared `BookSearchState` instead of 7 flat search fields. This ensures both consumers (bookshelf and library) handle the shared state identically.

14. Replace the flat search fields in `BookshelfState` with composed `BookSearchState`:
    ```kotlin
    data class BookshelfState(
        val shelfId: String,
        val shelfName: String = "",
        val books: List<Book> = emptyList(),
        val isLoading: Boolean = true,
        val isSearchDialogVisible: Boolean = false,
        val bookSearchState: BookSearchState = BookSearchState(),
        val recentlyDeleted: Book? = null,
        val errorMessage: String? = null,
        // ... remaining non-search fields unchanged ...
    )
    ```
    **Fields removed:** `searchQuery`, `searchResults`, `isSearchLoading`, `isTyping`, `hasSearched`, `searchByTitle`, `searchByAuthor`

15. Update `BookshelfViewModel` — all search state reads/writes go through `bookSearchState`:
    - `OnSearchQueryChange`: `_state.update { it.copy(bookSearchState = it.bookSearchState.copy(query = action.query, isTyping = ...)) }`
    - `OnToggleSearchByTitle/Author`: update `bookSearchState.searchByTitle`/`searchByAuthor`
    - `withSearchResults()`: update `bookSearchState` with results, `hasSearched = true`, `errorMessage = null`
    - `withSearchError()`: set `bookSearchState.errorMessage` via `ErrorFormatter` (search errors now display inline in the dialog rather than conflating with parent `errorMessage`)
    - `closeSearchDialog()`: reset `bookSearchState = BookSearchState()` (defaults handle the reset — much cleaner than resetting 7 fields)
    - Search filter logic (title/author/both): read from `bookSearchState.searchByTitle`/`searchByAuthor`

16. Update `BookshelfScreen` — remove the manual `BookSearchState` construction at the call site:
    ```kotlin
    // Before: constructed BookSearchState from 7 flat fields
    // After: pass state.bookSearchState directly, just override existingBookIds
    if (state.isSearchDialogVisible) {
        ShelfBookSearchDialog(
            state = state.bookSearchState.copy(
                existingBookIds = state.books.map { it.id }.toSet()
            ),
            ...
        )
    }
    ```
    Note: `existingBookIds` is still derived from `state.books` at the call site since it's a shelf-level concern (which books are already on this shelf), not a search-level concern.

17. Update bookshelf tests that reference flat search fields to use `bookSearchState.*`

18. Build + verify bookshelf search still works (build + existing tests + manual check)

### PR 2: feat: library add book

#### Phase 4: Library add book feature

19. Update `LibraryUseCases` to include `searchBooks: SearchBooksUseCase` and `upsertBook: UpsertBookUseCase`
20. Update `LibraryModule` DI with new use case bindings
21. Add search state to `LibraryState`:
    - `isSearchDialogVisible: Boolean = false`
    - `bookSearchState: BookSearchState` (composed, not flat fields)
22. Add search actions to `LibraryAction`:
    - `OnSearchClick` (FAB)
    - `OnDismissSearchDialog`
    - `OnRemoteSearchQueryChange(query: String)`
    - `OnToggleSearchByTitle`
    - `OnToggleSearchByAuthor`
    - `OnAddBookToLibrary(book: Book)`
    - `OnSearchResultBookClick(book: Book)`
23. Add search handling to `LibraryViewModel`:
    - Debounced remote search (300ms, consistent with local search debounce)
    - `addBookToLibrary()` calls `upsertBook` use case directly
    - Derive `existingBookIds` from `allBooks` for the `BookSearchState`
    - On search error: set `bookSearchState.errorMessage` via `ErrorFormatter.formatDataErrorMessage()`
    - `OnSearchResultBookClick`: upsert book first (cache for BookDetail), then navigate on success
    - On dismiss: reset `bookSearchState` to `BookSearchState()` (defaults handle the reset)
24. Create `library/presentation/searchcomponents/LibraryBookSearchDialog.kt` — wrapper providing library-specific trailing content (add icon for new books, check/already-added indicator for existing)
25. Add FAB to `LibraryScreen` via `Scaffold.floatingActionButton` slot:
    - Dispatches `OnSearchClick`
    - Hide when `isSearchDialogVisible` is true
    - Hide when library is in selection mode (from delete-books plan)
    - Note: FAB does not need scroll-hide behavior — the search dialog is modal (AlertDialog), so the FAB is just a trigger that disappears when the dialog opens
26. Show `LibraryBookSearchDialog` when `state.isSearchDialogVisible`
27. Update `LibraryScreenRoot` to handle `OnSearchResultBookClick` navigation to BookDetail

#### Phase 5: Tests

28. Unit test `LibraryViewModel` search flow:
    - Debounce triggers search after 300ms
    - Search results populate `bookSearchState.results`
    - `existingBookIds` updates reactively when `allBooks` changes (e.g. after adding a book)
    - Add book calls `upsertBook` and book appears in `allBooks` via Room Flow
    - Search error sets `bookSearchState.errorMessage`, preserves previous results
    - `OnSearchResultBookClick` upserts book before emitting navigation event
    - Dismiss resets `bookSearchState` to defaults (including clearing `errorMessage`)
    - Min 2 chars before search triggers
29. Verify existing bookshelf search tests still pass after relocation and state refactor (should be covered by PR 1 but double-check)
30. Verify `ShelfBookSearchDialog` wrapper renders same trailing content behavior as the old monolithic dialog (preview comparison)
## Edge Cases

| Case | Handling |
|------|----------|
| Book already in library | Show "already added" indicator in search results; `UpsertBookUseCase` preserves metadata if re-added |
| Search with no internet | `SearchBooksUseCase` returns `DataError.Remote.NO_INTERNET`; set `bookSearchState.errorMessage`, shown inline in dialog via shared error display |
| API rate limiting (429) | Ktor client auto-retries 3x with exponential backoff; if exhausted, `DataError.Remote.TOO_MANY_REQUESTS` -> inline error message |
| Empty search query | Don't trigger search (min 2 chars, matching bookshelf behavior) |
| Very fast typing | 300ms debounce prevents excessive API calls |
| Add book then immediately search again | `allBooks` flow updates reactively; `existingBookIds` stays current |
| FAB visibility during search | Hidden while search dialog is open (dialog is modal) |
| Book clicked in search results | Upsert book first (cache for BookDetail), then navigate (no shelfId context). Matches bookshelf's `OnBookClick` pattern at `BookshelfViewModel.kt:78-88` |
| Empty library (no books yet) | FAB still visible on empty state screen — primary way to add first book |

## Dependency Flow (After)

```
library/ ---> book/ (SearchBooksUseCase, UpsertBookUseCase, shared search UI)
bookshelf/ -> book/ (SearchBooksUseCase, AddBookToShelfUseCase, shared search UI)
bookclub/ --> book/ (unchanged)
```

No cross-feature dependencies between library, bookshelf, or bookclub.

## Files Changed/Created

### PR 1: refactor: extract shared search + compose BookSearchState

| Action | File |
|--------|------|
| **Move** | `SearchBooksUseCase` + Impl -> `book/domain/usecase/` |
| **Move+Edit** | `BookSearchBar.kt`, `AdvancedSearchFilters.kt`, `BookSearchState.kt` -> `book/presentation/searchcomponents/` (add defaults, `errorMessage` field, rename `inShelfIds`) |
| **Create** | `book/presentation/searchcomponents/BookSearchDialog.kt` (shared, with trailing slot) |
| **Create** | `bookshelf/presentation/searchcomponents/ShelfBookSearchDialog.kt` (wrapper) |
| **Edit** | `bookshelf/presentation/BookshelfState.kt` (replace 7 flat search fields with composed `bookSearchState: BookSearchState`) |
| **Edit** | `bookshelf/presentation/BookshelfViewModel.kt` (all search state access via `bookSearchState`, `withSearchError()` uses `BookSearchState.errorMessage`) |
| **Edit** | `bookshelf/presentation/BookshelfScreen.kt` (use `ShelfBookSearchDialog`, pass `state.bookSearchState` directly) |
| **Edit** | `bookshelf/domain/usecase/BookshelfUseCases.kt` (import update) |
| **Edit** | `bookshelf/di/BookshelfModule.kt` (remove SearchBooksUseCase binding) |
| **Edit** | `book/di/BookModule.kt` (add SearchBooksUseCase binding) |
| **Delete** | `bookshelf/presentation/searchcomponents/BookSearchDialog.kt` (replaced by shared + wrapper) |

### PR 2: feat: library add book

| Action | File |
|--------|------|
| **Create** | `library/presentation/searchcomponents/LibraryBookSearchDialog.kt` (wrapper) |
| **Edit** | `library/domain/usecase/LibraryUseCases.kt` (add `searchBooks`, `upsertBook`) |
| **Edit** | `library/di/LibraryModule.kt` (new bindings) |
| **Edit** | `library/presentation/LibraryState.kt` (add `isSearchDialogVisible`, `bookSearchState`) |
| **Edit** | `library/presentation/LibraryAction.kt` (add search actions) |
| **Edit** | `library/presentation/LibraryViewModel.kt` (search handling) |
| **Edit** | `library/presentation/LibraryScreen.kt` (add FAB, show search dialog) |
| **Edit** | `library/presentation/LibraryScreenRoot.kt` (search result navigation) |
