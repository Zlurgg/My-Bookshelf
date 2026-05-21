# Post-Library Improvements Plan

Follow-up improvements identified during Library screen implementation and testing.

## ~~Shared Search Bugs~~ ✅ Done

Fixed in `fix(search): retrigger search on filter toggle and prevent both-unchecked state` (branch `search-filter-bugs`). Switched query flows from `MutableStateFlow` to `MutableSharedFlow` to allow re-emission, removed `distinctUntilChanged` from remote pipelines, added `canToggleTitle`/`canToggleAuthor` guards and disabled UI state.

## ~~Shared Search Alignment~~ ✅ Done

Fixed in three commits on `search-filter-bugs` branch: aligned `withSearchError` to preserve results on error, moved `existingBookIds` derivation into BookshelfViewModel's `loadBooks()`, and extracted shared state transformations (`withLoading`, `withResults`, `withBelowMinLength`, `toSearchParams`) into `BookSearchState`. See `docs/specs/plans/shared-search-alignment.md` for full plan.

## Bookshelf Bugs

### OnBookClick navigation race condition
**Affects:** BookshelfViewModel + BookshelfScreenRoot.
**Bug:** `BookshelfViewModel.OnBookClick` (line 79-91) launches a coroutine to upsert the clicked search result, but `BookshelfScreenRoot` navigates immediately without waiting for the upsert to complete. If the upsert hasn't finished when BookDetailScreen loads, the book won't be in the local database. Same bug that was fixed in Library via `navigateToBook` state + `LaunchedEffect`. Apply the same pattern to Bookshelf.

