# Post-Library Improvements Plan

Follow-up improvements identified during Library screen implementation and testing.

## Library

### Add Book to the library
See [library-add-book.md](library-add-book.md)

### Delete book from library
See [library-delete-books.md](library-delete-books.md)

## Shared Search Bugs

### Filter toggle does not retrigger search
**Affects:** Both bookshelf and library search dialogs.
**Bug:** Toggling the title/author filter checkboxes does not retrigger the search. The user has to add or remove a character to see updated results.
**Root cause:** `distinctUntilChanged()` on the query flow swallows the re-emitted value since the query string hasn't changed — only the filter state has. The fix needs to either combine filter state into the debounced flow or use a separate mechanism to force a new search when filters change.

### Both filters unchecked has no distinct behavior
**Affects:** Both bookshelf and library search dialogs.
**Issue:** When both "Search by Title" and "Search by Author" are unchecked, the search falls back to the general `q=` parameter — identical to both checked. This is confusing since the user explicitly unchecked both. Consider either preventing the last checkbox from being unchecked (disable it) or showing a hint explaining the fallback behavior.

## Shared Search Alignment

### withSearchError behavior diverges
**Affects:** BookshelfViewModel vs LibraryViewModel.
**Issue:** `BookshelfViewModel.withSearchError()` clears `results = emptyList()` on error. `LibraryViewModel.withSearchError()` preserves previous results. Both use the shared `BookSearchDialog`, so the user experience differs for the same visual component. Pick one behavior and apply consistently — likely preserve results (less jarring, error banner is sufficient).

### BookshelfScreen existingBookIds per-recomposition
**Affects:** BookshelfScreen only.
**Issue:** `BookshelfScreen.kt` still does `state.bookSearchState.copy(existingBookIds = state.books.map { it.id }.toSet())` per recomposition. Library fixed this by deriving `existingBookIds` in the ViewModel's `observeBooks()`. Apply the same pattern to `BookshelfViewModel`.

### DRY: Remote search orchestration duplicated
**Affects:** LibraryViewModel + BookshelfViewModel.
**Issue:** ~80 lines of near-identical search logic: debounce setup, query-length guard, query mapping (`when { searchByTitle && searchByAuthor -> ... }`), `withSearchResults`/`withSearchError` helpers. Extract a shared `RemoteSearchHandler` or utility that both ViewModels delegate to.

