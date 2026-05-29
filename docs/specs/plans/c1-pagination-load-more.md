# C1 — Pagination + "Load more" for remote book search

**Status:** Plan drafted 2026-05-29 during a planning pass on the `last-few-tweaks-I-promise` branch. Awaiting review before a fresh session implements. Originates from `google-books-followups.md` item 1, flagged C1 in `next-session-handover.md`.
**Scope:** Add pagination to the remote search path (Google Books + OpenLibrary). Surface a "Load more" button at the bottom of the shelf, library, and book-club search dialog result lists. Library-scope search (the local-library toggle added in commit `1159e279`) is unaffected — local results return synchronously and don't paginate.

## Goal

Today every remote search returns the first page (40 Google / 15 OL) and stops. Users who can't find a book in that window have to refine the query. C1 lets the user request the next page on demand without re-typing, while keeping the same provider abstraction and not breaking the existing fallback semantics.

## Where it applies

| Dialog | Paginate? | Why |
|---|---|---|
| `BookSearchDialog` (base) | yes — new props | The button + `isLoadingMore` state live in the base composable |
| `ShelfBookSearchDialog` | yes | Primary use case (shelf search) |
| `LibraryBookSearchDialog` | yes | Library-tab remote search is identical use case |
| Book-club search | yes (automatic) | Reuses `ShelfBookSearchDialog`, no separate wiring |
| Library-scope mode (`libraryScopeEnabled = true`) | **no** | Local books are returned in full; no paging needed |

## Existing code state (verified during planning recon, 2026-05-29)

- `BookSearchResponse.kt` has only `books: List<Book>`. **No `totalResults`** — was deliberately dropped during spike delivery (no consumer at the time, see `google-books-followups.md` §1).
- `BookApiService.searchBooks(...)` takes `query / resultLimit / language / sort` only. **No `startIndex`.**
- `GoogleBooksApiService:28` uses `parameter("maxResults", resultLimit ?: ApiConfig.GoogleBooks.DefaultParams.MAX_RESULTS)`. No `startIndex` parameter sent.
- `OpenLibraryApiService:50` uses `parameter("limit", it)`. No `offset` parameter sent.
- Both data sources already see the totals in the DTO response: `dto.totalItems` (Google) and `dto.numFound` (OL) — currently logged via Timber and discarded.
- `FallbackRemoteBookDataSource.searchBooks` (lines 14-51) passes all params through; needs a new `startIndex` slot.
- `BookSearchState` has `results`, `existingBookIds`, `filteredCount` — no current-page or loading-more state.
- `BookshelfViewModel.performSearch()` (lines 340-375) replaces results wholesale on every call. `observeDebouncedQuery` uses `collectLatest` — already cancels in-flight prior calls when a new query arrives.
- `LibraryViewModel` has its own `performRemoteSearch` (analog of Bookshelf's `performSearch`). Same shape.
- `BookSearchDialog` renders a `LazyColumn` with `itemsIndexed(state.results)`. A footer row is the natural place for the load-more button.

## Data model changes

### `BookSearchResponse` (domain)

Reintroduce the total:

```kotlin
data class BookSearchResponse(
    val books: List<Book>,
    // Page-of-results total reported by the provider. Google's `totalItems`
    // is an estimate — sometimes over, sometimes under the true count. OL's
    // `numFound` is exact. Used as a non-authoritative cap; the actual
    // end-of-results signal is "we got fewer rows than we asked for OR an
    // empty page came back."
    val totalResults: Int = 0,
)
```

### `SearchResult` (domain)

```kotlin
data class SearchResult(
    val books: List<Book>,
    val filteredCount: Int,
    val totalResults: Int,
)
```

### `BookSearchState`

```kotlin
data class BookSearchState(
    ...
    val totalResults: Int = 0,
    val nextStartIndex: Int = 0,   // 0 means "fresh search"; n means "next page begins at row n"
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,  // derived from totalResults vs results.size (+ last-page-empty)
    ...
)
```

`canLoadMore` could be a `val` getter rather than a stored field if preferred — derive from `totalResults > results.size && !isLoadingMore && libraryScopeEnabled == false`.

## Architectural shape

### API layer

`BookApiService.searchBooks(...)` gains `startIndex: Int? = null`. Each provider maps it to its own wire param:

- **Google** — `parameter("startIndex", it)` when non-null (alongside `maxResults`).
- **OL** — `parameter("offset", it)` when non-null (alongside `limit`).

Single uniform parameter name on the interface, per-provider mapping in the impl. No need to split the interface for now.

### `RemoteBookDataSource` + impls

Add `startIndex: Int? = null` to the interface method, thread through both `GoogleBooksRemoteBookDataSource.searchBooks` and `OpenLibraryRemoteBookDataSource.searchBooks` and `FallbackRemoteBookDataSource.searchBooks`. Both impls already log the total — promote it into the returned `BookSearchResponse`.

### `SearchBooksUseCase`

Gains `startIndex: Int? = null` parameter. Forwards to `RemoteBookDataSource.searchBooks(...)`. The Safe Search filter still applies per-page (returned `filteredCount` is per-page; the ViewModel accumulates).

Existing call shape:

```kotlin
suspend operator fun invoke(
    query: String,
    resultLimit: Int? = null,
    language: String? = null,
    authorFilter: String? = null,
    titleFilter: String? = null,
    subjectFilter: String? = null,
    safeSearchEnabled: Boolean = true,
    startIndex: Int? = null,   // new
): Result<SearchResult, DataError.Remote>
```

### ViewModel changes (Bookshelf + Library)

**New action:** `OnLoadMore` (`BookshelfAction.OnLoadMore`, `LibraryAction.OnLoadMore`).

**Handler:**

```kotlin
is BookshelfAction.OnLoadMore -> {
    val s = _state.value.bookSearchState
    if (!s.canLoadMore || s.isLoadingMore || s.libraryScopeEnabled) return@onAction
    viewModelScope.launch { performSearch(append = true) }
}
```

**`performSearch(append: Boolean = false)`** is refactored:

- On `append = false` (new query / filter toggle / safe-search toggle / library-scope toggle):
  - Reset `nextStartIndex = 0`, clear `results`, set `isLoading = true`, `isLoadingMore = false`.
- On `append = true`:
  - Set `isLoadingMore = true` (keep existing `results` visible).
  - Pass `startIndex = state.nextStartIndex` to the use case.
- On success:
  - If appending, **dedupe by book id** before concatenating — Google occasionally returns the same id across pages when results shift.
  - Update `nextStartIndex = newResults.size`.
  - Update `totalResults = result.totalResults`.
  - Accumulate `filteredCount += result.filteredCount`.
  - Compute `canLoadMore` (see below).
- On error during load-more:
  - **Preserve existing results.** Set `isLoadingMore = false` and set the error message — the user sees the rows they already loaded plus an inline error banner.

**`canLoadMore` predicate:**

```kotlin
fun canLoadMore(state: BookSearchState, lastPageWasEmpty: Boolean): Boolean {
    if (state.libraryScopeEnabled) return false
    if (lastPageWasEmpty) return false
    if (state.totalResults <= 0) return false
    return state.results.size < state.totalResults
}
```

Three reasons to hide the button: library scope, the last page came back empty (Google's `totalItems` was an over-estimate), or we've reached the total. Note that `totalResults` is an estimate from Google — the `lastPageWasEmpty` short-circuit is what actually stops us at the real end.

**Cancelling in-flight load-more on new query:** the existing `collectLatest` in `observeDebouncedQuery` only covers the debounced path. A `performSearch(append = true)` call from `OnLoadMore` launches its own coroutine. To cancel it on a new query, track the load-more `Job` and cancel it from `OnSearchQueryChange`:

```kotlin
private var loadMoreJob: Job? = null

// in OnLoadMore handler:
loadMoreJob = viewModelScope.launch { performSearch(append = true) }

// in OnSearchQueryChange handler:
loadMoreJob?.cancel()
```

Or — simpler — emit the "append" intent through a separate `SharedFlow` collected with `collectLatest` so cancellation is automatic. Implementer's call; both are fine.

**Filter-toggle reset:** every existing toggle handler currently calls `persistSearchPreferences()` + `retriggerSearchIfNeeded()`. `retriggerSearchIfNeeded` should be updated so the re-emitted query resets `nextStartIndex = 0` (since the underlying `performSearch` defaults `append = false`).

### UI

`BookSearchDialog` gains two props:

```kotlin
isLoadingMore: Boolean,
canLoadMore: Boolean,
onLoadMore: () -> Unit,
```

The `LazyColumn` adds a footer `item { ... }` that renders the load-more button when `canLoadMore` and `!isLoadingMore`, or a small `CircularProgressIndicator` when `isLoadingMore`, or nothing when neither.

`ShelfBookSearchDialog` + `LibraryBookSearchDialog` pass these through. `BookSearchCallbacks` (in `bookshelf/presentation/searchcomponents/`) gains `val onLoadMore: () -> Unit`. `BookshelfScreen` + `LibraryScreen` wire the new callback to the action.

`R.string.search_load_more` = `"Load more results"`. `R.string.search_results_count_of_total` could be added for richer header copy (e.g. `"Showing 40 of 128 results"`) but isn't required.

## Edge cases addressed

1. **Pagination skipped in library scope.** `OnLoadMore` is a no-op when `libraryScopeEnabled` is true; the button is hidden. Local results are returned in full from the use case.
2. **Filter toggle / safe-search toggle / library-scope toggle resets to page 1.** Each toggle handler retriggers a fresh search; `performSearch(append = false)` zeroes `nextStartIndex` and replaces `results`.
3. **New query cancels in-flight load-more.** Via tracked `Job.cancel()` or `collectLatest` on a load-more `SharedFlow`.
4. **End-of-results detection.** Two predicates combined: `results.size >= totalResults` AND `lastPage.isEmpty()`. The empty fallback handles Google's bad estimates.
5. **Error on load-more.** Existing results preserved; inline error message; `isLoadingMore` reset to false. The user can retry the load-more or refine the query.
6. **Duplicate ids across pages.** Dedupe by id on append.
7. **Safe-search `filteredCount` accumulates.** `state.filteredCount += result.filteredCount` per page.
8. **Tap row → detail → back during load-more.** Existing `lazyListState` is hoisted by `ShelfBookSearchDialog` callers (per the existing scroll-preservation work in commit `04fb9aad`). Load-more state should also survive the round trip — kept in the ViewModel, not the dialog.

## Tests

| Layer | Test | What it locks |
|---|---|---|
| Use case | `SearchBooksUseCase` forwards `startIndex` to `RemoteBookDataSource` | Per-page passthrough |
| Data source | `GoogleBooksRemoteBookDataSource` sets `startIndex` query param when non-null | Wire format |
| Data source | `OpenLibraryRemoteBookDataSource` sets `offset` query param when non-null | Wire format |
| Data source | `FallbackRemoteBookDataSource` threads `startIndex` to both primary and fallback | Doesn't drop it on fallback |
| VM | `OnLoadMore appends to results and updates nextStartIndex` | Append shape |
| VM | `OnLoadMore dedupes by id when Google returns duplicates` | Dedupe |
| VM | `OnLoadMore is a no-op when libraryScopeEnabled is true` | Library-scope guard |
| VM | `Filter toggle during pagination resets nextStartIndex to 0` | Re-trigger semantics |
| VM | `Load-more error preserves existing results and sets error message` | Error path |
| VM | `New query cancels in-flight load-more` | Cancellation |
| VM | `canLoadMore false when results.size >= totalResults` | End detection |
| VM | `canLoadMore false when last page came back empty` | Estimate-fallback |
| Screen | (optional) Compose UI test that the button renders when canLoadMore | Visual contract |

## Open decisions

1. **`canLoadMore` as a stored state field or a computed getter on `BookSearchState`?** Either fine. Stored is more transparent in test assertions; computed avoids state-update boilerplate. Recommend computed.
2. **Where to put the cancellation primitive — `Job` ref or `SharedFlow` + `collectLatest`?** Both work. Recommend the explicit `Job` since the existing `collectLatest` on the debounced path is already in use.
3. **Filter accumulating across pages vs per-page reset.** Plan accumulates. If the user toggles safe search after loading 3 pages, `filteredCount` resets to 0 and starts again on the fresh page-1 search (because the toggle calls `performSearch(append = false)`).
4. **Library scope mode `OnLoadMore` — silent no-op or assertion?** Silent no-op (button just isn't rendered when `libraryScopeEnabled`).
5. **Page-size constant.** Stays at the existing `MAX_RESULTS` defaults per provider. Not parameterised in v1.

## Out of scope / deferred

- **Per-provider page sizes.** Google's 40 and OL's 15 stay hard-coded as today.
- **Infinite-scroll trigger.** Plan uses an explicit button (one click handler, no scroll-position threshold). If the button feels clunky, switch to `LazyListState.firstVisibleItemIndex + visibleItemsInfo` threshold detection in a follow-up.
- **Persisting "what page am I on" across app death.** State is process-scoped only.
- **OL `numFound` cross-checking.** OL's count is exact, Google's is an estimate. Plan treats them uniformly via the empty-page fallback.

## Execution order

1. **Data model.** Add `totalResults` to `BookSearchResponse` and `SearchResult`.
2. **API services.** Add `startIndex: Int?` to `BookApiService`. Implement in `GoogleBooksApiService` (`parameter("startIndex", it)`) and `OpenLibraryApiService` (`parameter("offset", it)`).
3. **Data sources.** Thread `startIndex` through `RemoteBookDataSource` interface, both impls, and `FallbackRemoteBookDataSource`. Each impl populates `totalResults` from the existing logged DTO field.
4. **Use case.** `SearchBooksUseCase` adds `startIndex` parameter and forwards. Update the impl to map `SearchResult` correctly.
5. **State + actions.** Add `BookSearchState` fields; add `OnLoadMore` to `BookshelfAction` and `LibraryAction`.
6. **ViewModels.** Refactor `performSearch` to `performSearch(append: Boolean)`. Wire `OnLoadMore`. Reset `nextStartIndex` on filter/safe/library-scope toggles. Cancel load-more on new query. Apply to both `BookshelfViewModel` and `LibraryViewModel`.
7. **Library-scope guard.** When `libraryScopeEnabled` is true, `canLoadMore = false`; `OnLoadMore` is a no-op.
8. **UI.** `BookSearchDialog` props + footer item; `ShelfBookSearchDialog` + `LibraryBookSearchDialog` thread the callback; `BookSearchCallbacks` adds `onLoadMore`; `BookshelfScreen` + `LibraryScreen` wire to the action.
9. **Tests** per the table above.
10. **Manual device verification.**
    - Type a popular query → load page 1 (40 Google results visible) → tap "Load more" → another 40 append, dedupe verified by absence of repeats.
    - Toggle Safe Search → list resets to page 1, `filteredCount` resets.
    - Toggle Library-scope → button hidden, all local books shown synchronously.
    - Network-loss mid-load-more → inline error, existing results stay.
    - Type a new query during load-more → in-flight request cancels cleanly; new search starts.
    - Library dialog: load-more works identically.
    - Book-club shelf search: load-more works (via shared `ShelfBookSearchDialog`).
11. **Commit.** Single commit per provider/feature: `feat(search): paginate remote results with load-more button`.

## Review notes for the next session

- Re-verify the existing-code state at top of this doc hasn't drifted (especially `BookSearchState` and `performSearch` if other PRs land first).
- The "in-flight load-more cancellation" design (Job ref vs SharedFlow) is the only meaningful design call left. Defer until the implementer can see the existing flow shapes side by side.
- Estimated implementation time from cold: ~3-4 hours for full plan (both providers, button, tests), assuming no major shape changes from review.
