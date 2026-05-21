# Plan: Fix Bookshelf Navigation Race Condition

## Context

When a user clicks a search result in the Bookshelf search dialog, the app navigates to the BookDetail screen before the book has been persisted to the local database. If the detail screen loads before the upsert completes, the book won't be found.

**The bug is worse than initially described.** `BookshelfScreenRoot` intercepts `OnBookClick` in its `when` block and navigates immediately — the action **never reaches the ViewModel**. The upsert code in `BookshelfViewModel.OnBookClick` (lines 79-91) is dead code. Search results are navigated to without any database persistence.

Library already fixed this same bug by separating search result clicks from regular book clicks and using a `navigateToBook` state + `LaunchedEffect` pattern.

### Pre-conditions

None — this fix is independent of the search filter and search alignment plans.

## The Fix: Apply Library's navigation pattern to Bookshelf

The pattern: instead of navigating synchronously in the UI callback, the ViewModel controls when navigation happens (only after upsert succeeds), and the UI reacts to state via `LaunchedEffect`.

### Step 1: Add `OnSearchResultBookClick` action and `navigateToBook` state

**BookshelfAction.kt (lines 5-18)** — add two new actions:

```kotlin
data class OnSearchResultBookClick(val book: Book) : BookshelfAction
data object OnNavigationHandled : BookshelfAction
```

Uses `OnNavigationHandled` to match Library's naming (`LibraryAction.OnNavigationHandled` at line 158 of LibraryViewModel). Consistent naming across features prevents grep misses.

| File | Change |
|------|--------|
| `BookshelfAction.kt` | Add `OnSearchResultBookClick(book)` and `OnNavigationHandled` |

**BookshelfState.kt (lines 7-26)** — add navigation state field:

```kotlin
val navigateToBook: Book? = null,
```

| File | Change |
|------|--------|
| `BookshelfState.kt` | Add `navigateToBook: Book? = null` |

### Step 2: Handle new actions in BookshelfViewModel

**BookshelfViewModel.kt** — replace the dead `OnBookClick` handler (lines 79-91) with the new actions:

```kotlin
is BookshelfAction.OnSearchResultBookClick -> {
    viewModelScope.launch {
        when (val cacheResult = bookshelfUseCases.upsertBook(action.book)) {
            is Result.Success -> {
                _state.update { it.copy(navigateToBook = action.book) }
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to cache book: %s", cacheResult.error)
                _state.update {
                    it.copy(
                        bookSearchState = it.bookSearchState.copy(
                            errorMessage = ErrorFormatter.formatDataErrorMessage(
                                cacheResult.error,
                                "open book"
                            )
                        )
                    )
                }
            }
        }
    }
}
is BookshelfAction.OnNavigationHandled -> {
    _state.update { it.copy(navigateToBook = null) }
}
```

**Delete the existing `OnBookClick` handler** (lines 79-91). It was dead code — the ScreenRoot intercepted the action before it reached the ViewModel. `OnBookClick` will continue to exist as an action (for shelf books that are already persisted), but it's handled entirely at the ScreenRoot level.

Note: the error is surfaced in `bookSearchState.errorMessage` (matching Library's pattern), because the click originates from the search dialog. The error banner in the dialog will display it.

**Existing test impact:** `BookshelfViewModelTest` has a `cache book handles error correctly` test (lines 161-182) that exercises the dead `OnBookClick` handler. This test must be **deleted** — it tests dead code that no longer exists. The new `search result book click error surfaces in bookSearchState` test replaces it with correct assertions (error in `bookSearchState.errorMessage` instead of top-level `errorMessage`).

| File | Lines | Change |
|------|-------|--------|
| `BookshelfViewModel.kt` | 79-91 | Replace dead `OnBookClick` handler with `OnSearchResultBookClick` + `OnNavigationHandled` |

### Step 3: Update BookshelfScreenRoot to use LaunchedEffect

**BookshelfScreen.kt (lines 60-90)** — two changes:

1. Add `LaunchedEffect` to watch `navigateToBook` state:

```kotlin
LaunchedEffect(state.navigateToBook) {
    state.navigateToBook?.let { book ->
        onBookClick(book)
        viewModel.onAction(BookshelfAction.OnNavigationHandled)
    }
}
```

2. Update the `onAction` callback — `OnSearchResultBookClick` goes to ViewModel (not intercepted):

```kotlin
onAction = { action ->
    when (action) {
        is BookshelfAction.OnBookClick -> onBookClick(action.book)  // shelf books — already persisted
        is BookshelfAction.OnAddBookClick -> onAddBookClick(action.book)
        is BookshelfAction.OnBackClick -> onBackClick()
        BookshelfAction.OnCreateBookClub -> onCreateBookClub()
        else -> viewModel.onAction(action)  // OnSearchResultBookClick falls through to ViewModel
    }
}
```

`OnSearchResultBookClick` is **not** intercepted — it falls through to `viewModel.onAction(action)`, which runs the upsert and sets `navigateToBook` on success. The `LaunchedEffect` then triggers navigation.

The `else ->` catch-all matches Library's pattern (`LibraryScreenRoot.kt` lines 26-32 uses the same structure: explicitly enumerate navigation actions, delegate the rest). This is consistent and acceptable.

| File | Lines | Change |
|------|-------|--------|
| `BookshelfScreen.kt` | 60-90 | Add `LaunchedEffect(state.navigateToBook)`, no change to `when` block (new action falls through to `else`) |

### Step 4: Wire search dialog to use the new action

**BookshelfScreen.kt** — in the search dialog's `BookSearchCallbacks`, change the `onBookClick` callback from `OnBookClick` to `OnSearchResultBookClick`:

```kotlin
override val onBookClick: (Book) -> Unit = { book ->
    onAction(BookshelfAction.OnSearchResultBookClick(book))
}
```

This is the only place where the mapping changes. Shelf book clicks (outside the search dialog) continue to use `OnBookClick` and navigate immediately — those books are already in the local database.

| File | Change |
|------|--------|
| `BookshelfScreen.kt` | Change search dialog's `onBookClick` callback to emit `OnSearchResultBookClick` |

## Loading indicator: not needed

`upsertBook` is a local-only Room write (`BookRepositoryImpl.upsertBook` calls `dao.upsert()` via `safeSuspendCall` — no Firestore sync). Sub-millisecond latency. No user-perceptible delay between click and navigation. Library uses the same pattern with no loading indicator and it hasn't been an issue.

The `safeSuspendCall` wrapper in the repository confirms proper error handling — unexpected exceptions are caught and mapped to `Result.Error`, so the `viewModelScope.launch` block won't silently swallow crashes.

## Edge Cases

**Upsert fails:** `navigateToBook` stays null, no navigation occurs. Error message appears in the search dialog's error banner. User can retry.

**Double-click before upsert completes:** The second `OnSearchResultBookClick` launches a second coroutine. Both attempt upsert (idempotent) and set `navigateToBook`. `LaunchedEffect` fires once for the state change. Worst case: two upserts of the same book (harmless) and one navigation.

**Dialog dismissed while upsert is in-flight:** The coroutine continues in `viewModelScope`. If it succeeds, `navigateToBook` is set and `LaunchedEffect` navigates even though the dialog is closed. This matches Library's behavior and is correct — the user intended to view the book.

**Back navigation after `navigateToBook` fires:** `OnNavigationHandled` clears the state immediately after navigation. Returning to the Bookshelf screen won't re-trigger navigation because `navigateToBook` is already null.

## Tests

**Delete:** `cache book handles error correctly` (lines 161-182) — tests the dead `OnBookClick` handler that no longer exists.

**Add** three new tests mirroring Library's patterns (from `LibraryViewModelTest` lines 440-497):

1. **`search result book click upserts then sets navigateToBook`** — click a search result, assert `upsertBook` was called and `navigateToBook` matches the clicked book
2. **`search result book click error surfaces in bookSearchState`** — fail the upsert, assert `bookSearchState.errorMessage` is set and `navigateToBook` is null
3. **`OnNavigationHandled clears navigateToBook`** — set `navigateToBook` via click, then dispatch `OnNavigationHandled`, assert `navigateToBook` is null

The mock `SimpleUpsertBookUseCase` in `BookshelfViewModelTest` already supports `shouldSucceed = false` (line 165), so no mock changes needed.

Note: `BookshelfViewModelTest` uses `UnconfinedTestDispatcher` so `advanceUntilIdle()` should be sufficient (no `advanceTimeBy` needed — no debounce involved in this flow).

## Verification

1. Run existing tests: `./gradlew testDebugUnitTest --tests "*.BookshelfViewModelTest"`
2. Run new tests to confirm they pass
3. Run detekt: `./gradlew detekt`
4. Manual smoke test: open Bookshelf search dialog, click a search result, verify navigation waits for upsert, verify BookDetail screen loads the book correctly. Test with airplane mode to verify error handling.
