# Google Books — Future Follow-ups

**Status:** Captured during `spike-test-google-books` delivery; none yet scheduled.
**Origin:** Consolidates open items from three plans archived after delivery in 2026-05-26: `google-books-api-integration.md` Section 10 (pagination, PrintType filter), `google-books-search-quality.md` items 3 + 5 (no-author junk, navigation-state preservation). The original plans are recoverable from git history if rationale is needed.
**Scope:** Items deferred or surfaced after the spike branch closed. Each is small enough to land independently; they don't form a single feature.

## Items

### 1. Pagination

Neither the OL integration before the spike nor the Google Books integration after it implements pagination. Both fetch a fixed cap (40 for Google, 15 for the legacy OL path). `BookSearchResponse.totalResults` was dropped during spike delivery (no consumer), so adding pagination needs:

- Re-introduce a `totalResults` (or equivalent) at the `BookSearchResponse` / `SearchResult` layer.
- Thread `startIndex` (Google) / `offset` (OL) through `RemoteBookDataSource.searchBooks(...)`.
- `BookApiService` will likely need provider-specific interfaces at this point — OL uses `offset`, Google uses `startIndex`, and a unified parameter name has to map to both.
- UI surface for "load more" or infinite scroll in `BookSearchDialog`.

Not blocking. Users who can't find a book in the first 40 results can refine their query.

### 2. PrintType filtering

`PrintType` enum (`BOOK` / `MAGAZINE` / `UNKNOWN`) was added to `Book` during the spike but is not consumed for filtering. Add a user-toggleable filter (alongside the existing safe-search switch) to exclude magazines, or extend `SafeSearchFilter` to filter on `printType` when the user opts in.

Trivial change once a UX placement is chosen.

### 3. Optional: no-author junk filter

Google occasionally returns books with no authors — typically PediaPress Wikipedia compilations. If they become a visible pattern, filter at the data source in `GoogleBooksRemoteBookDataSource` alongside the existing language and blank-title filters:

```kotlin
?.filter { !it.volumeInfo?.authors.isNullOrEmpty() }
```

Currently rare; not worth blanket-applying without a real user-visible problem. Keep this as a "fix when noticed" item.

### 4. Preserve search-dialog state across preview-and-back navigation

Surfaced during on-device testing. When a user searches, taps a result to preview it, then navigates back from the detail screen, the search dialog comes up empty — query and result list are gone. The user has to retype and re-search, which is friction when they're browsing several books in the same search.

**Likely causes (investigate before fixing):**
- `OnSearchResultBookClick` in the relevant ViewModel may close the dialog as a side-effect of triggering navigation.
- The `navigateToBook` flag is cleared on return — that clear may also reset `bookSearchState.query` and `.results`.
- The dialog visibility flag (`isSearchDialogVisible`) is being toggled off as part of the navigation flow.

**Possible fixes, by increasing scope:**
- **Cheapest:** keep `bookSearchState` intact through the navigate-out + navigate-back round trip; reopen the dialog automatically on return so the user lands back on their results.
- **Moderate:** keep the dialog visible while the detail screen sits in front of it (so back drops the user straight into the still-open dialog, no flicker).
- **Largest:** convert the preview pane from a separate navigation destination to a bottom-sheet overlay above the search dialog — the user never leaves the search context.

Affects both `BookshelfScreen` and `LibraryScreen` (both invoke `BookSearchDialog` and both call `OnSearchResultBookClick`).
