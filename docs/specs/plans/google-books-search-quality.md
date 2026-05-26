# Google Books Search Quality Fixes

**Status:** Items 1 + 2 delivered on `spike-test-google-books`; item 3 deferred (low priority); item 4 added during testing and also delivered; item 5 captured for future work.  
**Branch:** `spike-test-google-books`  
**Date:** 2026-05-22 (updated 2026-05-26)  

## Problem

Google Books API returns noisy results despite `langRestrict=en` and `printType=books`:

1. **Non-English books leak through** — `langRestrict` is best-effort. Google returns books with English descriptions but non-English content (e.g. Urdu Harry Potter translation with `"language": "ur"`).
2. **Result limit not applied** — `ApiConfig.GoogleBooks.DefaultParams.MAX_RESULTS` was increased to 40, but the ViewModel hardcodes `resultLimit = 15`, overriding the default.
3. **Junk results** — PediaPress Wikipedia compilations (no authors, 1000+ pages) pass all API-level filters since they're technically `printType: BOOK`.

## Fixes

### 1. Remove hardcoded result limit in ViewModel — ✅ delivered (`db02bbe2`)

**File:** `bookshelf/presentation/BookshelfViewModel.kt` (and any other ViewModel that calls `searchBooks`)

Change `resultLimit = 15` to `resultLimit = null` so it falls through to `ApiConfig.GoogleBooks.DefaultParams.MAX_RESULTS` (40). Same quota cost per request.

Check `bookcase/` or `library/` ViewModels for the same pattern.

### 2. Post-fetch language filtering in GoogleBooksRemoteBookDataSource — ✅ delivered (`db02bbe2`)

**File:** `book/data/network/GoogleBooksRemoteBookDataSource.kt`

Filter results after mapping where `volumeInfo.language != "en"`. Do this in the data source (not the UseCase) since it's provider-specific — OL already handles language via its own parameter.

```kotlin
// In the .map {} block after toBook()
books = dto.items
    ?.filter { it.volumeInfo?.language == "en" }
    ?.map { it.toBook() }
    ?: emptyList()
```

This filters before mapping to domain objects, so we don't waste effort mapping books we'll discard.

### 3. Optional: filter no-author junk — ⏸ deferred

Low priority. The PediaPress result is rare and users can scroll past it. If it becomes a pattern, filter in the same place: `?.filter { !it.volumeInfo?.authors.isNullOrEmpty() }`.

### 4. Filter no-title results — ✅ delivered (`59bea873`)

**Not in the original plan; surfaced during on-device testing.** Google occasionally returns rows with a populated `imageUrl`, `pageCount`, and ISBN but a blank `volumeInfo.title` — they render as a search row with nothing for the user to identify. Filter alongside the language filter so they never reach the result list.

### 5. Preserve search-dialog state across preview-and-back navigation

**Surfaced during on-device testing. Out of scope for `spike-test-google-books` — this is a navigation/state-preservation change with its own UX considerations.**

When a user searches, taps a result to preview it, then navigates back from the detail screen, the search dialog comes up empty — the query and result list are gone. The user has to retype and re-search, which is friction when they're browsing several books in the same search to decide which to add.

Likely causes (investigate before fixing):
- `OnSearchResultBookClick` in the relevant ViewModel may close the dialog as a side-effect of triggering navigation.
- The `navigateToBook` flag is cleared on return — that clear may also reset `bookSearchState.query` and `.results`.
- The dialog visibility flag (`isSearchDialogVisible`) is being toggled off as part of the navigation flow rather than preserved.

Possible fixes, by increasing scope:
- **Cheapest:** keep `bookSearchState` intact through the navigate-out + navigate-back round trip; reopen the dialog automatically on return so the user lands back on their results.
- **Moderate:** keep the dialog visible while the detail screen sits in front of it (so back drops the user straight into the still-open dialog, no flicker).
- **Largest:** convert the preview pane from a separate navigation destination to a bottom-sheet overlay above the search dialog — the user never leaves the search context.

Affects both `BookshelfScreen` and `LibraryScreen` (both invoke `BookSearchDialog` and both call `OnSearchResultBookClick`).

## Impact

- No API cost change (still 1 request)
- Users see ~30-35 English-only results instead of 15 mixed-language
- Existing UI caps display at scroll length, so more results just means better coverage
