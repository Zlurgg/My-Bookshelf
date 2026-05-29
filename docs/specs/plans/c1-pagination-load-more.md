# C1 — Pagination + "Load more" for remote book search

**Status:** Plan v3 — second review pass on 2026-05-29 resolved the open `pageSize` provenance (now a required field on `BookSearchResponse`, not inferred from a constant), removed the use case's existing `cacheSearchPreviews` write to avoid double-caching with the VM, and added a min-length guard on `OnLoadMore`. Awaiting implementation in a fresh session.
**Scope:** Add pagination to the remote search path (Google Books + OpenLibrary). Surface a "Load more" button at the bottom of the shelf, library, and book-club search dialog result lists. Library-scope search (added in commit `1159e279`) is unaffected — local results return synchronously.

## Goal

Today every remote search returns the first page (40 Google / 15 OL) and stops. C1 lets the user request the next page on demand without re-typing, while keeping the same provider abstraction, the same fallback semantics for first-page requests, and the existing preview-cache → tap-row-into-detail flow intact across pages.

## Where it applies

| Dialog | Paginate? | Why |
|---|---|---|
| `BookSearchDialog` (base) | yes — new props | Button + `isLoadingMore` state live in the base composable |
| `ShelfBookSearchDialog` | yes | Primary use case |
| `LibraryBookSearchDialog` | yes | Library-tab remote search shape is identical |
| Book-club search | yes (automatic) | Reuses `ShelfBookSearchDialog` |
| Library-scope mode (`libraryScopeEnabled = true`) | **no** | Local results are returned in full |

## Existing code state (verified during planning recon, 2026-05-29)

- `BookSearchResponse.kt` has only `books: List<Book>` (no totals).
- `BookApiService.searchBooks(...)` takes `query / resultLimit / language / sort` — no `startIndex`.
- `GoogleBooksRemoteBookDataSource.searchBooks` **post-filters** the response: language filter (`it.volumeInfo?.language == "en"`) + blank-title filter (`GoogleBooksRemoteBookDataSource.kt:65-71`). Then `SearchBooksUseCaseImpl.invoke` post-filters again with Safe Search (`SearchBooksUseCaseImpl.kt:64-72`).
- `OpenLibraryRemoteBookDataSource.searchBooks` does **not** post-filter (`OpenLibraryRemoteBookDataSource.kt:52-57`).
- Both data sources log the provider-reported totals (`dto.totalItems` Google, `dto.numFound` OL) but discard them.
- `FallbackRemoteBookDataSource.searchBooks` (lines 14-51) falls back from Google to OL on `TOO_MANY_REQUESTS / FORBIDDEN / PROVIDER_UNAVAILABLE`.
- `BookRepositoryImpl.cacheSearchPreviews` **clears the cache** before writing the new batch (`BookRepositoryImpl.kt:31-37`). Naïvely calling it per page would lose page-1 entries.
- `BookshelfState.closeSearchDialog` (`BookshelfViewModel.kt:467-480`) **does NOT preserve `libraryScopeEnabled`** — a pre-existing asymmetry vs `LibraryViewModel`'s `OnDismissSearchDialog` which does preserve it.
- `BookSearchState.withLoading` (`BookSearchState.kt:29-33`) only resets `isLoading / isTyping / errorMessage` — adding pagination fields manually at every reset site would be fragile.

## Critical correctness — `startIndex` index space

**Provider asymmetry — this is the bug the review caught.**

Google's `startIndex` is into the *unfiltered* result stream. OL's `offset` likewise. But our data sources/use case post-filter:

```
Google page 1: maxResults=40 → 40 raw items
              → language filter drops 20 → 20 items
              → Safe Search filter drops 2 → 18 items
              → state.results.size == 18
```

Using `state.results.size == 18` as the next `startIndex` re-fetches rows 18–57 — overlapping the first page massively. OL doesn't post-filter so `state.results.size == 15` is accidentally correct there, which makes the bug provider-asymmetric and easy to miss in tests.

**Fix:** `BookSearchResponse` and `SearchResult` gain `rawPageSize: Int` — the count of items the provider *actually returned* before any post-filtering. The ViewModel accumulates `nextStartIndex += response.rawPageSize`. End-of-results is `rawPageSize < pageSize` (which also subsumes the empty-page case). `pageSize` is itself a required field on the response (see §Data model) — see "Provider asymmetry" below for why a hard-coded constant is wrong.

## End-of-results detection

For the same reason, `result.books.isEmpty()` (post-filter) is the wrong signal — a page where Safe Search nuked everything would falsely terminate pagination while the provider has more.

Use `rawPageSize < pageSize` as the **only** end-of-results signal. This is uniform across providers and doesn't depend on Google's estimated `totalItems` (which is sometimes high, sometimes low). The plan v1's "totalResults vs results.size" predicate is replaced entirely.

**Consequence:** `BookSearchResponse.totalResults` and `SearchResult.totalResults` are **not added** at all. One field shaved off every layer. Header copy "Showing N of M results" is dropped — the showing-count alone (`results.size`) is fine.

## Fallback during pagination

Plan v1 didn't address this. Scenario: Google succeeds at `startIndex=0`, returns 40 rows; user taps Load More; Google 429s on `startIndex=40`; `FallbackRemoteBookDataSource` swaps to OL with the same `startIndex=40`, but that's `offset=40` into a *completely different result set*. The accumulated list becomes Google rows + OL-from-offset-40 with no rational ordering.

**Decision: disable fallback for `startIndex != null` and surface the error.** Simplest and honest. `FallbackRemoteBookDataSource.searchBooks` short-circuits to `primary` only when `startIndex != null`:

```kotlin
override suspend fun searchBooks(
    query: String,
    ...,
    startIndex: Int?,
): Result<BookSearchResponse, DataError.Remote> {
    val result = primary.searchBooks(query, ..., startIndex)
    if (startIndex != null) return result  // load-more must not provider-switch
    return when {
        result is Result.Error && shouldFallback(result.error) -> fallback.searchBooks(...)
        else -> result
    }
}
```

The ViewModel sees the load-more error and shows it inline; the user can retry or refine the query. Fresh searches (page 1) keep the existing fallback behavior.

## Cancellation primitive

Plan v1 cancelled `loadMoreJob` only on `OnSearchQueryChange`. But filter toggles, safe-search toggle, and library-scope toggle all call `retriggerSearchIfNeeded()` which emits through `queryFlow` — `collectLatest` cancels the prior debounced `performSearch` but **does not touch the separately-launched `loadMoreJob`**. The load-more result would land on top of the fresh page-1 result.

**Revised approach: emit load-more requests through a `SharedFlow` collected inside the same `observeDebouncedQuery` scope.** One `collectLatest` cancels both. Avoids per-call-site `loadMoreJob?.cancel()` discipline that's easy to forget.

```kotlin
private val loadMoreFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

// in init / observeDebouncedQuery scope:
merge(
    queryFlow.debounce(SEARCH_DEBOUNCE_MS).map { ... -> SearchTrigger.Fresh(it) },
    loadMoreFlow.map { SearchTrigger.More },
).collectLatest { trigger ->
    when (trigger) {
        is SearchTrigger.Fresh -> { /* min-length gate; performSearch(append=false) */ }
        is SearchTrigger.More  -> performSearch(append = true)
    }
}

// in OnLoadMore handler:
loadMoreFlow.tryEmit(Unit)
```

`merge` + `collectLatest` means any new trigger cancels the previous in-flight `performSearch`, regardless of whether it was a query change, filter toggle, library-scope toggle, or another load-more.

## Data model changes

### `BookSearchResponse` (domain)

```kotlin
data class BookSearchResponse(
    val books: List<Book>,
    // Pre-filter count of items the provider actually returned. Used to
    // advance pagination correctly (post-filter `books.size` would lie) and
    // to detect end-of-results uniformly: rawPageSize < pageSize.
    // Required — no default — so a data source that forgets to populate it
    // fails to compile rather than silently advertising "page is empty."
    val rawPageSize: Int,
    // The page size the data source asked the provider for on THIS request.
    // Required for the same compile-time-safety reason. Google's effective
    // page is 40, OL's is 15 — and page 1 of a fresh search can fall back
    // from Google to OL, so the VM cannot assume a fixed constant. Returning
    // it from the data source makes "did we get a full page?" provider-aware
    // without any inference on the consumer side.
    val pageSize: Int,
)
```

### `SearchResult` (domain)

```kotlin
data class SearchResult(
    val books: List<Book>,
    val filteredCount: Int,
    val rawPageSize: Int,
    val pageSize: Int,
)
```

### `BookSearchState`

```kotlin
data class BookSearchState(
    ...
    val nextStartIndex: Int = 0,
    val isLoadingMore: Boolean = false,
    // Stored, not computed. The ViewModel sets this explicitly after each
    // page resolves (`rawPageSize >= pageSize`) so the predicate logic lives
    // in one place and a state.copy() doesn't need to re-derive. Storing it
    // also means StateFlow equality compares the explicit value rather than
    // re-running the predicate on every copy.
    val canLoadMore: Boolean = false,
    ...
)
```

`canLoadMore` is stored (not a getter) and set explicitly by the ViewModel after each search resolves — keeps the predicate logic in one place and survives state copies cleanly.

**New state helper:**

```kotlin
fun BookSearchState.withFreshSearch(): BookSearchState = copy(
    isLoading = true,
    isLoadingMore = false,
    isTyping = false,
    errorMessage = null,
    results = emptyList(),
    nextStartIndex = 0,
    canLoadMore = false,
    filteredCount = 0,
)
```

Every fresh-search call site (`performSearch(append = false)`) uses `withFreshSearch()`. One place to update if pagination state grows.

## Architectural shape

### API layer

`BookApiService.searchBooks(...)` gains `startIndex: Int? = null`. Per-provider mapping in the impl:

- **Google** — `parameter("startIndex", it)` when non-null.
- **OL** — `parameter("offset", it)` when non-null.

Defensive `coerceAtLeast(0)` at the API call site — Google and OL both reject negatives, and a stale `-1` from a bad state transition shouldn't surface as 4xx.

### `RemoteBookDataSource` + impls

`searchBooks` gains `startIndex: Int? = null`. Each impl populates `BookSearchResponse.rawPageSize` from the raw provider items count (Google's `dto.items?.size ?: 0`, OL's `dto.results.size`) **before** post-filtering.

```kotlin
// GoogleBooksRemoteBookDataSource:
val requestedPageSize = resultLimit ?: ApiConfig.GoogleBooks.DefaultParams.MAX_RESULTS
val rawItems = dto.items ?: emptyList()
BookSearchResponse(
    books = rawItems
        .filter { it.volumeInfo?.language == "en" }
        .filter { !it.volumeInfo?.title.isNullOrBlank() }
        .map { it.toBook() },
    rawPageSize = rawItems.size,
    pageSize = requestedPageSize,
)
```

The same pattern in `OpenLibraryRemoteBookDataSource`: capture the value the impl actually passed to `parameter("limit", it)` and return it as `pageSize`.

`FallbackRemoteBookDataSource` threads `startIndex` through and short-circuits fallback when non-null per the §Fallback decision.

### `SearchBooksUseCase`

```kotlin
suspend operator fun invoke(
    query: String,
    resultLimit: Int? = null,
    language: String? = null,
    authorFilter: String? = null,
    titleFilter: String? = null,
    subjectFilter: String? = null,
    safeSearchEnabled: Boolean = true,
    startIndex: Int? = null,
): Result<SearchResult, DataError.Remote>
```

Returns `SearchResult(books = safeBooks, filteredCount = ..., rawPageSize = response.rawPageSize, pageSize = response.pageSize)` — passes through, doesn't shrink, the raw count or the page size.

**Cache call removed from the use case.** The current impl ends with `bookRepository.cacheSearchPreviews(result.books)` (`SearchBooksUseCaseImpl.kt:73-75`). Under pagination, the VM owns accumulation and is the sole cache writer (see §preview-cache below). Leaving the use case's call in place would clear-then-write the per-page batch, then the VM would immediately clear-then-write the accumulated list — two cycles per page with a transiently-incorrect cache state in between. **Drop the use case's `cacheSearchPreviews` call entirely as part of this work.**

### ViewModel changes (Bookshelf + Library)

**New action:** `OnLoadMore` on both `BookshelfAction` and `LibraryAction`.

**`performSearch(append: Boolean = false)`:**

```kotlin
private suspend fun performSearch(append: Boolean = false) {
    val current = _state.value.bookSearchState
    if (append) {
        if (!current.canLoadMore || current.libraryScopeEnabled) return
        _state.update { it.copy(bookSearchState = it.bookSearchState.copy(isLoadingMore = true)) }
    } else {
        _state.update { it.copy(bookSearchState = it.bookSearchState.withFreshSearch()) }
    }

    val searchState = _state.value.bookSearchState
    if (searchState.libraryScopeEnabled) {
        performLibrarySearch(searchState)  // already resets pagination via withFreshSearch
        return
    }

    val params = searchState.toSearchParams()
    val requestedStart = if (append) searchState.nextStartIndex else 0

    bookshelfUseCases.searchBooks(
        ...,
        startIndex = requestedStart.coerceAtLeast(0).takeIf { append },
    )
        .onSuccess { result ->
            // Dedupe by id with a HashSet so append is O(n) not O(n²).
            val mergedBooks = if (append) {
                val seen = HashSet(searchState.results.map { it.id })
                searchState.results + result.books.filter { seen.add(it.id) }
            } else {
                result.books
            }
            val baseStartIndex = if (append) searchState.nextStartIndex else 0
            _state.update {
                it.copy(
                    bookSearchState = it.bookSearchState.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        hasSearched = true,
                        errorMessage = null,
                        results = mergedBooks,
                        // Per-page reset (see "Filtered-count" decision).
                        filteredCount = result.filteredCount,
                        nextStartIndex = baseStartIndex + result.rawPageSize,
                        // Provider-aware: result.pageSize is the size the
                        // data source actually requested (Google 40, OL 15,
                        // possibly different if page 1 fell back to OL).
                        canLoadMore = result.rawPageSize >= result.pageSize,
                    )
                )
            }
            // Cache the accumulated list, not the per-page batch — the repo's
            // cacheSearchPreviews clears then writes, so passing only the
            // page-2 batch would invalidate page-1 entries for tap-into-detail.
            // (Use case no longer writes to the cache — see §SearchBooksUseCase
            // above for the rationale.)
            bookRepository.cacheSearchPreviews(mergedBooks)
        }
        .onError { error ->
            _state.update {
                it.copy(bookSearchState = it.bookSearchState.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    // Preserve results on load-more error; reset on fresh-search
                    // error (results were already cleared by withFreshSearch).
                    errorMessage = ErrorFormatter.formatDataErrorMessage(error, "search books"),
                ))
            }
        }
}
```

**`OnLoadMore` handler:**

```kotlin
BookshelfAction.OnLoadMore -> {
    val s = _state.value.bookSearchState
    // Edge case: user deleted query back below MIN_SEARCH_QUERY_LENGTH but
    // taps load-more before the 300ms debounce fires the below-min-length
    // reset. canLoadMore is still true from the prior successful page —
    // firing now would burn a request that the upcoming reset invalidates.
    if (s.query.trim().length < MIN_SEARCH_QUERY_LENGTH) return@onAction
    loadMoreFlow.tryEmit(Unit)
}
```

(Same shape on `LibraryViewModel`.)

**Library-scope guard for `performLibrarySearch`:** add explicit pagination reset so toggling into library scope from a paginated remote result doesn't leave `nextStartIndex` / `isLoadingMore` lingering:

```kotlin
private suspend fun performLibrarySearch(searchState: BookSearchState) {
    bookshelfUseCases.searchLibraryBooks(query = ..., ...)
        .onSuccess { books ->
            _state.update {
                it.copy(bookSearchState = it.bookSearchState.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    nextStartIndex = 0,
                    canLoadMore = false,
                    hasSearched = true,
                    errorMessage = null,
                    results = books,
                    filteredCount = 0,
                ))
            }
        }
        .onError { ... }
}
```

**Filter-toggle reset:** every existing toggle handler calls `retriggerSearchIfNeeded()`. Since pagination is driven through the merged `loadMoreFlow`/`queryFlow` `collectLatest`, any new `queryFlow.tryEmit(...)` from the retrigger automatically cancels an in-flight load-more — no per-handler `loadMoreJob?.cancel()` needed.

### `closeSearchDialog` asymmetry fix

While we're here, fix `BookshelfState.closeSearchDialog` to preserve `libraryScopeEnabled` (matching `LibraryViewModel`'s `OnDismissSearchDialog`). One line. Tangential but cheap.

```kotlin
bookSearchState = BookSearchState(
    existingBookIds = bookSearchState.existingBookIds,
    searchByTitle = bookSearchState.searchByTitle,
    searchByAuthor = bookSearchState.searchByAuthor,
    searchBySubject = bookSearchState.searchBySubject,
    safeSearchEnabled = bookSearchState.safeSearchEnabled,
    libraryScopeEnabled = bookSearchState.libraryScopeEnabled,
)
```

### UI

`BookSearchDialog` gains:

```kotlin
isLoadingMore: Boolean,
canLoadMore: Boolean,
onLoadMore: () -> Unit,
```

The `LazyColumn` adds a footer `item` that renders:

- A `Button(onClick = onLoadMore)` labelled `R.string.search_load_more` when `canLoadMore && !isLoadingMore`.
- A small `CircularProgressIndicator` when `isLoadingMore`.
- Nothing otherwise.

`ShelfBookSearchDialog` + `LibraryBookSearchDialog` pass these through. `BookSearchCallbacks` gains `val onLoadMore: () -> Unit`. `BookshelfScreen` + `LibraryScreen` wire the callback to the action.

`R.string.search_load_more` = `"Load more results"`.

## Edge cases addressed (v2)

1. **Provider-asymmetric `startIndex`** — fixed by `rawPageSize` field.
2. **Filter-killed page falsely terminating pagination** — fixed by `rawPageSize < pageSize` predicate.
3. **Provider switch via fallback mid-pagination** — fallback disabled when `startIndex != null`; error surfaced.
4. **Cancellation across all retrigger paths** — single `collectLatest` over merged `queryFlow + loadMoreFlow`.
5. **`withLoading()` doesn't reset pagination** — `withFreshSearch()` helper centralises the reset.
6. **`closeSearchDialog` libraryScopeEnabled asymmetry** — fixed inline.
7. **`performLibrarySearch` stale pagination state** — explicit reset on the local-search branch.
8. **Preview-cache clobber on append** — VM passes the accumulated book list to `cacheSearchPreviews`, not the per-page batch.
9. **Defensive `startIndex` validation** — `coerceAtLeast(0)` at the API call site.
10. **HashSet-backed dedupe** — O(n) append cost.
11. **Filtered-count semantics** — per-page reset (decision below), not accumulation.
12. **Library-scope skip** — `performSearch(append = true)` returns early when `libraryScopeEnabled`; button hidden via `canLoadMore = false`.

## Decisions changed from v1

| v1 | v2 | Why |
|---|---|---|
| Add `totalResults` to response/state | **Drop entirely.** Use `rawPageSize` only. | Google's estimate is unreliable; the empty-page (`rawPageSize < pageSize`) predicate is uniform and honest. |
| Track `loadMoreJob` ref | Use `loadMoreFlow` + merged `collectLatest` | All cancellation paths share one primitive — toggle handlers can't forget. |
| Accumulate `filteredCount` across pages | Per-page reset | "M filtered" climbing across loads is confusing in the dialog header. Per-page is what the current UX implies. |
| `BookSearchResponse.totalResults: Int = 0` default | `rawPageSize: Int` (no default) | A data source that forgets to populate now fails to compile rather than silently advertising "page is empty." |
| Page-size constant unspecified | `pageSize: Int` returned by each data source on `BookSearchResponse` / `SearchResult` | Page 1 fallback (Google→OL) is still enabled by design, so the effective page size can change between requests. Inferring from a constant would hide the OL-served-page-1 case where the VM would compare a 15-row response against Google's 40 and incorrectly conclude end-of-results. The data source already knows what it asked for; returning it removes all inference. |

## Open decisions (v3)

1. **Per-provider page size discrepancy mid-search.** If Google has 200 results and runs out at page 5, falling back to OL is disabled by design (§Fallback). So a single load-more chain uses one provider end-to-end — no need to worry about mixing page sizes. Page 1 can still fall back from Google to OL, which is why `pageSize` is per-response rather than a constant.
2. **Bookshelf-only first vs both VMs.** Plan v3 wires both. If the diff is uncomfortably large, ship `BookshelfViewModel` first and follow up with `LibraryViewModel`; the data-layer plumbing is identical.

(v2's Open Decision §1 — "where does `pageSize` come from" — was load-bearing rather than open; resolved in §Data model as a required field on `BookSearchResponse` and propagated through every layer.)

## Tests (v2)

| Layer | Test | What it locks |
|---|---|---|
| Use case | Forwards `startIndex` to `RemoteBookDataSource` | Per-page pass-through |
| Use case | Returns `SearchResult.rawPageSize` from the underlying response | Raw-count propagation |
| Use case | Per-page `filteredCount` does not accumulate inside the use case | Per-page reset semantics |
| Data source | Google: `parameter("startIndex", ...)` set when non-null; `rawPageSize` populated from raw items count (before language/blank-title filter) | Wire format + raw-count source |
| Data source | OL: `parameter("offset", ...)` set when non-null; `rawPageSize` populated | Wire format |
| Data source | Fallback short-circuits to primary when `startIndex != null`, returns Google's error without falling to OL | Fallback-during-pagination guarantee |
| VM | `OnLoadMore` advances `nextStartIndex` by `rawPageSize`, not `books.size` | The provider-asymmetric bug |
| VM | `OnLoadMore` dedupes by id via `HashSet` (Google duplicate-id scenario) | Dedupe |
| VM | `OnLoadMore` returns silently when `libraryScopeEnabled` | Library-scope guard |
| VM | Filter toggle during pagination cancels in-flight load-more and resets to page 1 | Cancellation + reset |
| VM | Load-more error preserves `results` and surfaces `errorMessage` | Error UX |
| VM | New query during load-more cancels via merged `collectLatest` | Cancellation primitive |
| VM | `canLoadMore` false when `rawPageSize < pageSize` (partial last page) | End detection |
| VM | `canLoadMore` false after a page where Safe Search filtered everything but `rawPageSize == pageSize` | Filter-killed page does NOT terminate prematurely |
| VM | `performLibrarySearch` zeros `nextStartIndex` / `isLoadingMore` / `canLoadMore` | Library-scope state cleanliness |
| VM | `closeSearchDialog` preserves `libraryScopeEnabled` | Asymmetry fix |
| VM | `cacheSearchPreviews` called with accumulated list, not per-page batch | Preview-cache invariant |
| Use case | `cacheSearchPreviews` no longer called from `SearchBooksUseCaseImpl` | Single-writer invariant — VM owns the cache under pagination |
| VM | `canLoadMore` true when page 1 fell back to OL and returned a full 15 rows; false when it returned 14 | `pageSize` propagation across the fallback path |
| VM | `OnLoadMore` is a no-op when query is below `MIN_SEARCH_QUERY_LENGTH` | Defensive guard against in-flight load-more during query delete |
| Screen | (optional) Load-more button visible when `canLoadMore`; spinner when `isLoadingMore` | Visual contract |

## Out of scope / deferred

- **Per-provider page sizes parameterised.** Stays at the existing defaults.
- **Infinite-scroll trigger.** Plan uses the explicit button. Switch later if it feels clunky.
- **Persisting "what page am I on" across app death.** State is process-scoped only.
- **OL `numFound` cross-checking.** Plan ignores it; `rawPageSize` handles end detection uniformly.
- **Header copy "Showing N of M".** Dropped with `totalResults`; only `results.size` shown.

## Execution order

1. **Data model.** Add `rawPageSize: Int` and `pageSize: Int` to `BookSearchResponse` and `SearchResult` (no defaults).
2. **API services.** Add `startIndex: Int?` to `BookApiService`. Implement in `GoogleBooksApiService` (`parameter("startIndex", ...)`) and `OpenLibraryApiService` (`parameter("offset", ...)`). Apply `coerceAtLeast(0)` at both sites.
3. **Data sources.** Thread `startIndex` through `RemoteBookDataSource` interface, Google + OL impls, and `FallbackRemoteBookDataSource`. Each impl populates `rawPageSize` from the raw items count *before* its own post-filter step, and `pageSize` from the limit it just asked the provider for. Fallback short-circuits when `startIndex != null`.
4. **Use case.** `SearchBooksUseCase` gains `startIndex` parameter. Returns per-page `filteredCount` (not accumulated). Returns `rawPageSize` and `pageSize`. **Removes** the existing `bookRepository.cacheSearchPreviews(result.books)` call — the VM is the sole cache writer under pagination.
5. **State + actions + helpers.** Add `BookSearchState` fields (`nextStartIndex`, `isLoadingMore`, `canLoadMore`). Add `BookSearchState.withFreshSearch()` helper. Add `OnLoadMore` to `BookshelfAction` and `LibraryAction`.
6. **ViewModels.** Refactor `performSearch` into `performSearch(append: Boolean)`. Replace standalone `observeDebouncedQuery` with a merged `queryFlow + loadMoreFlow` `collectLatest`. Wire `OnLoadMore`. Apply to both `BookshelfViewModel` and `LibraryViewModel`. Fix `closeSearchDialog` libraryScopeEnabled asymmetry. Update `performLibrarySearch` to zero pagination fields.
7. **UI.** `BookSearchDialog` props + footer item; `ShelfBookSearchDialog` + `LibraryBookSearchDialog` thread the callback; `BookSearchCallbacks` adds `onLoadMore`; `BookshelfScreen` + `LibraryScreen` wire to the action.
8. **Tests** per the table above.
9. **Manual device verification.**
   - Type a popular query → 40 Google results → tap "Load more" → 40 more, dedupe verified.
   - Force Google to 429 mid-load-more (e.g. via toggling airplane mode briefly) → load-more error inline, existing results intact, **no fallback to OL** (verify Timber log).
   - Toggle Safe Search → list resets to page 1, `filteredCount` resets to fresh page's count.
   - Type a new query during a load-more → in-flight cancels cleanly; new search starts.
   - Tap a row from page 2 → detail screen renders (preview cache hit).
   - Tap a row from page 1 after page 2 has loaded → detail still renders (cache invariant).
   - Toggle library-scope on → button hidden, all local books shown synchronously, `nextStartIndex`/`isLoadingMore` cleanly reset.
   - Toggle library-scope off again → next remote search starts at page 1.
   - Book-club shelf search: load-more works automatically (via shared `ShelfBookSearchDialog`).
10. **Commit.** Single commit: `feat(search): paginate remote results with load-more button`.

## Review notes for the next session

- The `rawPageSize` field is the load-bearing simplification — anything that "improves" the data model by dropping it should be rejected. Same goes for `pageSize`: a future "just use the constant" refactor reintroduces the OL-served-page-1 bug where a 15-row response gets compared against Google's 40.
- The merged `queryFlow + loadMoreFlow` `collectLatest` is the load-bearing cancellation primitive — if someone refactors to per-call `Job` tracking, the toggle-during-load-more race comes back.
- `SearchBooksUseCaseImpl` no longer writes to the preview cache. If a future reviewer "restores" it for consistency, the result is double-write per page with a transiently-wrong cache between the two writes.
- Re-verify the existing-code state at the top of this doc hasn't drifted (especially `BookSearchState`, `performSearch`, `cacheSearchPreviews`, and `closeSearchDialog`) if other PRs land first.
- Estimated implementation time from cold: ~4-5 hours for full plan.
