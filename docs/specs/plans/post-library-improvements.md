# Post-Library Improvements Plan

Follow-up improvements identified during Library screen implementation and testing.

## ~~Shared Search Bugs~~ ✅ Done

Fixed in `fix(search): retrigger search on filter toggle and prevent both-unchecked state` (branch `search-filter-bugs`). Switched query flows from `MutableStateFlow` to `MutableSharedFlow` to allow re-emission, removed `distinctUntilChanged` from remote pipelines, added `canToggleTitle`/`canToggleAuthor` guards and disabled UI state.

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

## Bookshelf Bugs

### OnBookClick navigation race condition
**Affects:** BookshelfViewModel + BookshelfScreenRoot.
**Bug:** `BookshelfViewModel.OnBookClick` (line 79-91) launches a coroutine to upsert the clicked search result, but `BookshelfScreenRoot` navigates immediately without waiting for the upsert to complete. If the upsert hasn't finished when BookDetailScreen loads, the book won't be in the local database. Same bug that was fixed in Library via `navigateToBook` state + `LaunchedEffect`. Apply the same pattern to Bookshelf.

