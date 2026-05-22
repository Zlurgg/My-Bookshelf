# Google Books Search Quality Fixes

**Status:** Pending — follow-up from integration spike  
**Branch:** `spike-test-google-books`  
**Date:** 2026-05-22  

## Problem

Google Books API returns noisy results despite `langRestrict=en` and `printType=books`:

1. **Non-English books leak through** — `langRestrict` is best-effort. Google returns books with English descriptions but non-English content (e.g. Urdu Harry Potter translation with `"language": "ur"`).
2. **Result limit not applied** — `ApiConfig.GoogleBooks.DefaultParams.MAX_RESULTS` was increased to 40, but the ViewModel hardcodes `resultLimit = 15`, overriding the default.
3. **Junk results** — PediaPress Wikipedia compilations (no authors, 1000+ pages) pass all API-level filters since they're technically `printType: BOOK`.

## Fixes

### 1. Remove hardcoded result limit in ViewModel

**File:** `bookshelf/presentation/BookshelfViewModel.kt` (and any other ViewModel that calls `searchBooks`)

Change `resultLimit = 15` to `resultLimit = null` so it falls through to `ApiConfig.GoogleBooks.DefaultParams.MAX_RESULTS` (40). Same quota cost per request.

Check `bookcase/` or `library/` ViewModels for the same pattern.

### 2. Post-fetch language filtering in GoogleBooksRemoteBookDataSource

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

### 3. Optional: filter no-author junk

Low priority. The PediaPress result is rare and users can scroll past it. If it becomes a pattern, filter in the same place: `?.filter { !it.volumeInfo?.authors.isNullOrEmpty() }`.

## Impact

- No API cost change (still 1 request)
- Users see ~30-35 English-only results instead of 15 mixed-language
- Existing UI caps display at scroll length, so more results just means better coverage
