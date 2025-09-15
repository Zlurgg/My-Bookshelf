# Search & API Improvements Issue

## Overview
Analysis of current search implementation revealed several areas for improvement, both for API compliance and user experience enhancements.

**✅ STATUS UPDATE**: Compilation issues resolved, search tests are now PASSING!

## API Compliance Issues (HIGH PRIORITY)

### 1. Missing User-Agent Header ❌
**Problem**: Open Library API documentation requires User-Agent header for API calls
**Current**: No User-Agent header set in HttpClientFactory
**Fix**: Add proper User-Agent to defaultRequest block

```kotlin
defaultRequest {
    contentType(ContentType.Application.Json)
    userAgent("MyBookshelf/1.0 (Android App; github.com/yourusername/mybookshelf)")
}
```

### 2. Production Logging Security Issue ❌
**Problem**: `LogLevel.ALL` logs user search queries and API calls in production
**Current**: Full logging enabled regardless of build type
**Privacy Risk**: User search data exposed in production logs
**Fix**: Conditional logging based on build type

```kotlin
install(Logging) {
    level = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE
}
```

## Current Search Limitations

### Search Functionality
- ✅ 450ms debouncing prevents API spam
- ✅ Proper error handling and loading states
- ✅ 20s network timeouts configured
- ❌ Language hardcoded to English only (`parameter("language", "eng")`)
- ❌ No search filters (genre, year, rating)
- ❌ No sorting options (relevance, date, rating)
- ❌ No result limit control
- ❌ No search history or suggestions

### Available But Unused Data
Based on API response, we have access to but don't utilize:
- `author_key` - Could enable author-specific searches
- `first_publish_year` - Year-based filtering
- `ratings_average` - Rating-based filtering
- `number_of_pages_median` - Length-based filtering
- `edition_count` - Popularity indicators

## Proposed Search Improvements

### Phase 1: API Compliance & Basic Improvements
1. **Fix User-Agent header** (required for API compliance)
2. **Fix production logging** (privacy & security)
3. **Add language selection** (remove hardcoded English)
4. **Add result limits** (10, 25, 50, 100 options)
5. **Add sorting** (relevance, date, rating, title)

### Phase 2: Advanced Search Features
1. **Filter panel** with:
   - Publication year range slider
   - Rating range filter
   - Page count range
   - Language selection
2. **Search suggestions** from recent searches
3. **Advanced search form** with separate title/author/ISBN fields

### Phase 3: UX Enhancements
1. **Search history** persistence and recall
2. **Better empty states** with helpful suggestions
3. **Search result caching** to reduce API calls
4. **Voice search** integration
5. **Keyboard shortcuts** and accessibility improvements

## Test Status Update
**✅ RESOLVED**: All search-related tests are now PASSING after compilation fix!

**Current Test Status** (as of latest run):
- Total tests: 53
- Passing: 51
- Failing: 2 (non-search related)
  - `BookDetailViewModelTest.onPurchaseClick_marks_book_as_purchased`
  - `BookcaseViewModelTest.showAddDialog_toggles_dialog_visibility`

**Search Test File**: `BookshelfSearchViewModelTest.kt` - ✅ ALL PASSING

## Implementation Priority
1. **CRITICAL**: Fix API compliance issues (User-Agent, logging) - 🔴 STILL NEEDED
2. **HIGH**: ~~Fix failing search tests~~ - ✅ RESOLVED
3. **MEDIUM**: Basic search improvements (language, limits, sorting)
4. **LOW**: Advanced features and UX enhancements

**Next Priority**: API compliance fixes in HttpClientFactory.kt

## Files to Modify

### API Compliance:
- `HttpClientFactory.kt` - Add User-Agent, fix logging
- `KtorRemoteBookDataSource.kt` - Remove hardcoded language

### Search Enhancements:
- `BookshelfViewModel.kt` - Add search parameters
- `RemoteBookDataSource.kt` - Update interface for new parameters
- `BookSearchDialog.kt` - Add filter UI components
- `BookSearchBar.kt` - Enhance with suggestions/history

### Tests:
- `BookshelfSearchTest.kt` - Fix failing test cases
- `BookshelfDebounceTest.kt` - Fix debounce test failures
- Add integration tests for new search features

## Notes
- Open Library API supports Solr query syntax for advanced searches
- API allows field-specific searches like `title:`, `author:`, etc.
- Current 450ms debounce may be too aggressive for typing experience
- Consider implementing search result caching to improve performance
- Ensure proper error messages for network issues vs. no results

## Definition of Done
- [ ] User-Agent header properly set
- [ ] Production logging disabled
- [✅] All existing search tests passing
- [ ] Language selection implemented
- [ ] Basic sorting and limits added
- [ ] Search improvements tested on various network conditions
- [ ] API usage compliant with Open Library guidelines

## Recent Progress
- ✅ Fixed critical compilation error in AppModule.kt (ImageLoader context injection)
- ✅ All search tests now passing (53 total tests, 2 non-search failures)
- ✅ Project builds successfully
- 🔴 API compliance issues (User-Agent, logging) still need addressing