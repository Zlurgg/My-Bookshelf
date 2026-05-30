# C2 — Tap-to-search + keyboard dismiss

**Status:** Handoff from prior session — decisions baked in, ready to plan + execute.
**Scope:** Replace debounced-as-you-type remote search with explicit IME Search trigger. Dismiss the soft keyboard on submit, on Load More, and on dialog dismiss. Library-scope (local) search keeps its current type-to-filter behaviour.

## Why this exists

After C1 (`feat(search): paginate remote results with load-more button`, commit `e99ec4ff`) shipped, two related UX problems were observed on device:

1. **Wasted API quota from typing pauses.** Current debounce is 300ms. Typing "harry potter" with any mid-word pause fires 2-4 searches. Each search is now 1 of 1000 daily Google Books requests; with the silently-capped 20-results-per-call (see C1 §"Why MAX_RESULTS=20"), each wasted call hurts more than under the old 40-cap assumption.
2. **Keyboard never closes.** The IME's Search button is wired (`BookSearchBar.kt:68-76`) but the dialog's `onImeSearch` callback is a literal no-op with the comment "handled by onQueryChange as user types" (`BookSearchDialog.kt:85`). So tapping the keyboard's Search action does nothing, and the keyboard stays up — blocking the lower half of the results when the user tries to scroll.

The two problems share a root cause: search is driven entirely by the typing debounce path, and the IME action is decorative.

## Decisions made (do not re-litigate)

- **Option A** chosen over B (longer debounce) and C (hybrid). The 20-cap math means each saved call matters more than the cost of teaching users to tap. Pre-release, this is the right time to set the pattern.
- **Library-scope mode is unchanged.** Local filtering is free and instant — type-to-filter is the right pattern there (think iOS Contacts, Spotify local library). Forcing tap-to-search for local results would be a regression.
- **Keyboard dismisses on**: IME Search tap, Load More tap, dialog dismiss. Not on every recomposition (that would fight with focus while typing).

## Existing code state (verified during handoff, 2026-05-30)

- `BookSearchBar.kt:68-76` — IME action already configured as `ImeAction.Search`, `KeyboardActions(onSearch = { onImeSearch() })`. Callback is passed through correctly; only the dialog's wiring is dead.
- `BookSearchDialog.kt:82-86` — `BookSearchBar` called with `onImeSearch = { /* handled by onQueryChange as user types */ }`. This is the dead callback.
- `BookSearchDialog` has no `LocalSoftwareKeyboardController` import or wiring today.
- `BookshelfViewModel.kt:143-153` — `OnSearchQueryChange` updates query in state AND calls `queryFlow.tryEmit(action.query)` — this is the debounce-driven auto-search path.
- `BookshelfViewModel.kt:46` — `SEARCH_DEBOUNCE_MS = 300L`.
- `BookshelfViewModel.kt:60` — `private val queryFlow = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)`.
- `BookshelfViewModel.kt:334-366` — `observeDebouncedQuery()` with the merged `queryFlow + loadMoreFlow` `collectLatest` (load-bearing per C1).
- `BookshelfViewModel.kt:381-390` — `retriggerSearchIfNeeded()` emits to `queryFlow` when filters toggle. This MUST keep working in library-scope mode and after a remote search is in progress.
- `LibraryViewModel.kt:100, :141, :152, :213, :294, :376, :388` — same shape; remote uses `remoteQueryFlow.debounce`, library scope uses a separate flow with its own debounce (line 376) and is independent.
- `BookshelfAction` / `LibraryAction` have no `OnSubmitSearch` action yet — needs adding.
- `BookSearchCallbacks` interface needs an `onSubmitSearch: () -> Unit` callback.

## Critical correctness — what NOT to break

These were load-bearing decisions from C1 that this work must preserve:

1. **The merged `queryFlow + loadMoreFlow` `collectLatest` in both VMs is the load-bearing cancellation primitive.** Any new trigger cancels in-flight `performSearch`. Don't refactor to per-call `Job` tracking — the toggle-during-load-more race comes back.
2. **`retriggerSearchIfNeeded()`** must continue to fire when filter checkboxes toggle (title/author/subject/safe-search/library-scope) AND when a search is currently active. Otherwise toggling a filter mid-search becomes silent.
3. **Library-scope mode keeps type-to-filter.** The keyboard does NOT need dismissing for library scope (no scroll-blocking issue — list updates inline, dialog stays compact).
4. **Filter toggle handlers** (`OnToggleSearchByTitle`, `OnToggleSafeSearch`, etc.) currently call `retriggerSearchIfNeeded()` after updating state. They must continue to retrigger the search if one has already been submitted (otherwise toggling Safe Search mid-results does nothing visible). The exact retrigger mechanism may need rethinking — see §Design considerations.

## Design considerations (need to be decided in the plan)

### A. Should typing still update some piece of state?

Yes — the text field is uncontrolled by the user otherwise. `BookshelfState.bookSearchState.query` should still update on every keystroke so the text field renders. The change is that `queryFlow.tryEmit` is no longer called from `OnSearchQueryChange`. Only `OnSubmitSearch` emits.

### B. What about filter toggles after a search submitted?

Currently, toggling Safe Search re-runs the same query through `retriggerSearchIfNeeded() → queryFlow.tryEmit(searchState.query)`. Under tap-to-search this is fine — the query is whatever was last submitted. But: if the user **types a new query** without submitting, then toggles a filter, the retrigger fires the new (unsubmitted) query. Probably acceptable (filter toggle = explicit user action, treat as implicit submit), but worth deciding.

Alternative: track `lastSubmittedQuery` separately from typed `query`, retrigger on the submitted one. Cleaner semantics, ~5 more lines.

### C. Submit button visibility

The IME Search button is on the keyboard. But the keyboard is only visible while the text field has focus. What if the user dismisses the keyboard (via system back) without submitting, then taps the Search icon on the leading icon of the text field?

Today the leading icon (`BookSearchBar.kt:60-66`) is decorative (no `onClick`). Options:
- **Leave as-is** — user re-focuses field to get keyboard back, then taps Search. One extra tap.
- **Make leading Search icon clickable** — submits on tap. Doubles the affordance.
- **Add a "Search" button next to the text field** — more discoverable but uses more vertical space in the dialog title.

Recommendation: leave the leading icon decorative for v1; revisit if testers complain. The IME Search button on the keyboard is the primary affordance, and users who dismiss the keyboard without submitting probably meant to.

### D. Empty query submission

Today the min-length guard at `BookshelfViewModel.kt:354` blocks searches below `MIN_SEARCH_QUERY_LENGTH`. This must still apply — tapping IME Search with "h" in the field should do nothing (and stay quiet). The IME Search button might still be tappable visually; the VM must not fire a search.

### E. Library-scope toggle while typing remote

User types "harry" in remote mode, doesn't submit, toggles library scope ON. Today this would not have fired a remote search anyway (debounce hadn't fired yet). Under tap-to-search, same outcome: library scope kicks in with "harry" as the filter, no remote call. No change needed.

### F. Soft keyboard controller wiring

`LocalSoftwareKeyboardController.current?.hide()` is the standard way. Capture once at the top of `BookSearchDialog`, expose a lambda that the dialog uses internally and also passes to `BookSearchBar.onImeSearch`. Don't propagate the controller itself across the boundary — keep it inside the dialog.

Alternative: `LocalFocusManager.current.clearFocus()`. Subtly different — `clearFocus` also drops focus from the text field; `hide` only hides the keyboard. For a dialog that's about to scroll results, `clearFocus` is probably more appropriate (so the text field doesn't auto-re-show the keyboard if anything taps near it).

## Architectural shape

### Action additions

```kotlin
// BookshelfAction.kt
data object OnSubmitSearch : BookshelfAction()

// LibraryAction.kt
data object OnSubmitSearch : LibraryAction()
```

### Callback addition

```kotlin
// BookSearchCallbacks.kt
val onSubmitSearch: () -> Unit
```

### ViewModel changes

```kotlin
// BookshelfViewModel — OnSearchQueryChange becomes typing-only:
is BookshelfAction.OnSearchQueryChange -> {
    _state.update { it.copy(
        bookSearchState = it.bookSearchState.copy(
            query = action.query,
            isTyping = false, // dropped — no debounce-driven search to wait for
        )
    ) }
    // NO queryFlow.tryEmit here anymore.
    // Library scope is still driven by retriggerSearchIfNeeded below.
    if (_state.value.bookSearchState.libraryScopeEnabled) {
        retriggerSearchIfNeeded()
    }
}

// NEW handler:
is BookshelfAction.OnSubmitSearch -> {
    queryFlow.tryEmit(_state.value.bookSearchState.query)
}
```

LibraryViewModel mirrors this. Note that LibraryVM has both a remote flow and a library-scope flow — only the remote one moves to tap-to-search; the library-scope flow keeps its existing debounce path.

### Debounce constant

Can be left at `SEARCH_DEBOUNCE_MS = 300L` — it now only debounces back-to-back rapid taps (e.g., user double-taps Submit), not typing. Effectively becomes a tap-rate-limiter. Or drop the debounce entirely from the remote path; library-scope still uses it.

Decision needed in the plan: remove debounce from remote path (simpler) vs keep as rate-limiter (defensive).

### Dialog changes

```kotlin
// BookSearchDialog.kt — capture keyboard controller, dismiss on submit/load-more:
val keyboardController = LocalSoftwareKeyboardController.current
val focusManager = LocalFocusManager.current

val dismissKeyboard: () -> Unit = {
    focusManager.clearFocus()
    keyboardController?.hide()
}

BookSearchBar(
    searchQuery = state.query,
    onSearchQueryChange = onQueryChange,
    onImeSearch = {
        dismissKeyboard()
        onSubmitSearch()
    },
)

// In the Load More item:
Button(onClick = {
    dismissKeyboard()
    onLoadMore()
}) { ... }

// In the onDismissRequest:
onDismissRequest = {
    if (!state.isLoading) {
        dismissKeyboard()
        onDismiss()
    }
},
```

## Test impact

Existing tests that assume "OnSearchQueryChange → debounce → search fires" will break and need updating:

- `BookshelfViewModelTest.kt` — search across for tests that do `viewModel.onAction(OnSearchQueryChange("harry"))` followed by `advanceTimeBy(SEARCH_DEBOUNCE_MS)` then assert results. These must add `viewModel.onAction(OnSubmitSearch)` between the two.
- `LibraryViewModelTest.kt` — same shape. Library-scope tests are unaffected.
- Tests that assert filter toggles retrigger search after results exist — should still pass since `retriggerSearchIfNeeded` keeps emitting to `queryFlow`, just with the last-submitted query.

New tests to add:
- `OnSubmitSearch with query length < MIN does not fire search` (min-length guard)
- `OnSearchQueryChange alone does not fire remote search` (the whole point of this change)
- `OnSubmitSearch fires remote search with current query`
- `Library scope: OnSearchQueryChange still drives filter` (regression guard for the asymmetry)

## Execution order

1. **Decide design questions B (lastSubmittedQuery) + debounce-removal vs rate-limiter.** Pick one of each before coding.
2. **Add `OnSubmitSearch` to both Action sealed classes.**
3. **Add `onSubmitSearch: () -> Unit` to `BookSearchCallbacks`.**
4. **Refactor `OnSearchQueryChange` in both VMs** — remove `queryFlow.tryEmit`; keep state update; preserve library-scope retrigger.
5. **Add `OnSubmitSearch` handler in both VMs** — emit current query to `queryFlow`.
6. **Wire `onSubmitSearch` from `BookshelfScreen` / `LibraryScreen`** through to `BookSearchDialog`.
7. **Update `BookSearchDialog`** — capture `LocalSoftwareKeyboardController` + `LocalFocusManager`, replace dead `onImeSearch` no-op, dismiss keyboard on Load More tap and on dismiss.
8. **Update `ShelfBookSearchDialog` + `LibraryBookSearchDialog`** to thread `onSubmitSearch` through.
9. **Tests** — update existing, add new per §Test impact.
10. **Manual device verification.**
    - Type "harry potter" → no search fires while typing → IME Search tap → search fires, keyboard closes, results render.
    - Tap Load More → keyboard stays dismissed; if user re-focused field, keyboard dismisses on tap.
    - Clear via X → text field empties; no search fires.
    - Toggle Safe Search after results render → search retriggers with last-submitted query.
    - Switch to library scope → type "h" → instant local filter (no submit needed).
    - Dialog dismiss with keyboard up → keyboard dismisses.
11. **Commit.** Single commit: `feat(search): require explicit submit for remote search + dismiss keyboard`.

## Out of scope

- Submit button outside the keyboard (the leading icon). Revisit if testers can't find the IME Search button.
- Library-scope ever requiring an explicit submit. It's local; keep it free.
- Auto-suggest / typeahead. Separate feature.
