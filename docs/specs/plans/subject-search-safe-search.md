# Subject Search + Safe Search + Persisted Search Preferences

## Context

A user searching OpenLibrary found adult content in results. OpenLibrary has no safe-search API parameter and no content rating field. However, `Book.subjects` (already mapped from the API) contains tags like "Erotica", "Erotic fiction", etc. that can be used for best-effort client-side filtering.

Research showed:
- **Well-known explicit books**: 100% detectable via subjects (keywords: erotica, erotic, sexual, etc.)
- **Borderline dark romance**: ~20% detectable — subjects are crowd-sourced and inconsistent
- **Play Store**: Two OpenLibrary apps already exist. Google's "Catalog App" exception allows sexual content titles if it's a minor fraction of the catalog and the app protects minors. A best-effort filter + honest IARC rating + report button covers the requirement.

This plan adds three features:
1. **Subject search checkbox** — additive filter (combines with title/author), off by default
2. **Safe Search toggle** — client-side filter on `Book.subjects` against a blocklist, on by default
3. **Persisted search preferences** — all search mode choices + safe search saved atomically to DataStore

## Critical Analysis

### Edge Cases
- **All checkboxes false** (corrupt preferences): `toSearchParams()` has a defensive fallback to `general`. Preference loading validates and corrects on read.
- **Empty subjects on a book**: Safe search lets it through — absence of evidence is not evidence of explicit content. Blocking unknowns would hide most results.
- **Non-English subjects** (e.g., "Literatura erotica"): The keyword `erotica` still matches via `contains()`. German "erotische" would not. Documented limitation; blocklist is easy to extend.
- **Quote characters in search input**: `buildQuery()` wraps multi-word queries in quotes. A query containing `"` could break the syntax. Strip quote characters from all filter inputs in `buildQuery()` before formatting. This is a pre-existing issue with author/title — fix it for all three.
- **Safe search hides all results**: If all 15 results are filtered, user sees "No books found" which is misleading. Track `filteredCount` and show feedback in UI.

### Assumptions
- **OpenLibrary `subject:` combines with `title:` and `author:`**: Verified. `subject:dragons title:dragons` returns 3,400 results (narrower than either alone at 7,133 and 41,560). `subject:fiction title:ring author:tolkien` returns 26. Combinations are genuinely respected.
- **Client-side filtering sufficient**: Yes — OpenLibrary has no server-side safe search. Filtering 15 results against a small blocklist is sub-microsecond.
- **Blocklist completeness**: Not perfect. Best-effort for v1. The `SafeSearchFilter` object is trivial to extend.

### Simpler Alternatives Considered
- **Skip subject checkbox, only add safe search?** Both are small and naturally touch the same files. No reason to split.
- **Separate FilterBooksUseCase?** Over-engineering — the filter is a one-liner inside `SearchBooksUseCaseImpl` and isn't reused elsewhere.
- **Use cases for preferences?** No. The codebase pattern (ThemePreferences, WelcomePreferences) is direct injection into ViewModels, not wrapped in use cases.
- **~~Mutual exclusion between subject and title/author?~~** Rejected. OpenLibrary supports combining `subject:` with `title:` and `author:` and produces genuinely narrowed results. Subject is an additive checkbox like the others. This eliminates all the force-off/force-on toggle complexity.

### Performance
- Filtering: O(15 × 9 keywords) = negligible.
- DataStore: Single atomic read collected once in `init`, not re-read per search.
- String matching: `contains(keyword, ignoreCase = true)` — simpler and faster than regex.

### Security
- **Unicode bypass**: A user searching with homoglyphs or non-Latin characters could bypass the filter. Acceptable — this is an advisory filter, not parental control.
- **User can disable**: Intentional. It's a preference, not a restriction.
- **Query injection**: Ktor URL-encodes parameters. Quote characters stripped from filter inputs in `buildQuery()` to prevent syntax-breaking queries like `fiction" author:"evil`.

### Clean Architecture
- **SafeSearchFilter**: Domain layer (`book/domain/service/`) — it's content policy (business logic).
- **Filtering**: Inside `SearchBooksUseCaseImpl` — post-API, pre-UI. `safeSearchEnabled` passed as parameter to keep use case stateless.
- **SearchPreferences**: Interface in `core/domain/preferences/`, impl in `core/data/preferences/`. No DataStore imports in domain.

### DRY
- **Two consumers**: Both `BookshelfViewModel` and `LibraryViewModel` use `BookSearchState` and `BookSearchDialog`. Changes to shared components automatically serve both.
- **Toggle pattern**: Subject follows the exact same `_state.update {}` + `retriggerSearchIfNeeded()` pattern as title/author — same "at least one must be checked" constraint applies to all three.
- **buildQuery()**: Adds `subjectFilter` using the same pattern as `authorFilter`/`titleFilter`.
- **Preference loading**: Both VMs do `searchPreferences.observe().collect { ... }` — one line each. With the atomic data class approach, no `combine()` needed.

### Single Responsibility
- `SearchPreferences`: Only persists/reads one atomic preference state.
- `SafeSearchFilter`: Only decides if a book is safe based on subjects.
- `SearchBooksUseCaseImpl`: Searches + filters. Filtering is part of "return appropriate results" — single conceptual responsibility.
- `SearchFilters` composable: Renders all search filter controls (checkboxes + toggle).

---

## Implementation Steps

### Step 1: SearchPreferences (Domain Interface)

**New file**: `app/src/main/java/uk/co/zlurgg/mybookshelf/core/domain/preferences/SearchPreferences.kt`

Atomic data class + 2-method interface (not 8 methods with 4 separate flows):

```kotlin
data class SearchPreferenceState(
    val searchByTitle: Boolean = true,
    val searchByAuthor: Boolean = true,
    val searchBySubject: Boolean = false,
    val safeSearchEnabled: Boolean = true
)

interface SearchPreferences {
    fun observe(): Flow<SearchPreferenceState>
    suspend fun update(state: SearchPreferenceState)
}
```

One atomic read, one atomic write. Eliminates race conditions from toggling multiple booleans (e.g., enabling subject while disabling title would cause intermediate states with the 4-flow approach).

### Step 2: SearchPreferencesImpl (Data Layer)

**New file**: `app/src/main/java/uk/co/zlurgg/mybookshelf/core/data/preferences/SearchPreferencesImpl.kt`

- Constructor-injected `DataStore<Preferences>`
- Four `booleanPreferencesKey` entries in companion object
- `observe()`: single `dataStore.data.map { prefs -> SearchPreferenceState(...) }`
- `update()`: single `dataStore.edit { prefs -> ... }` writing all four keys atomically
- Follows `ThemePreferencesImpl` / `WelcomePreferencesImpl` pattern

### Step 3: Register in CoreModule

**Modify**: `app/src/main/java/uk/co/zlurgg/mybookshelf/core/di/CoreModule.kt`

Add: `singleOf(::SearchPreferencesImpl).bind<SearchPreferences>()`

### Step 4: SafeSearchFilter (Domain Object)

**New file**: `app/src/main/java/uk/co/zlurgg/mybookshelf/book/domain/service/SafeSearchFilter.kt`

```kotlin
object SafeSearchFilter {
    private val blockedKeywords = listOf(
        "erotica", "erotic", "pornograph", "sexual",
        "bdsm", "bondage", "smut", "fetish", "kink"
    )

    fun isBookSafe(book: Book): Boolean {
        if (book.subjects.isEmpty()) return true
        return book.subjects.none { subject ->
            val lower = subject.lowercase()
            blockedKeywords.any { keyword -> lower.contains(keyword) }
        }
    }
}
```

- `object` — stateless, no DI needed
- Single `blockedKeywords` list — no misleading `blockedPrefixes` with identical matching logic
- `contains` not `equals` — catches "Erotic fiction", "Amateur pornography", "BDSM literature"
- `lowercase()` for case-insensitive matching
- Empty subjects = safe

### Step 5: Extend BookSearchParams

**Modify**: `app/.../book/presentation/searchcomponents/BookSearchParams.kt`

Add `val subject: String? = null`

### Step 6: Extend BookSearchState

**Modify**: `app/.../book/presentation/searchcomponents/BookSearchState.kt`

Add fields:
- `val searchBySubject: Boolean = false`
- `val safeSearchEnabled: Boolean = true`
- `val filteredCount: Int = 0` — tracks how many results were hidden by safe search

Subject is additive — all three checkboxes participate in the same "at least one must be checked" constraint:

```kotlin
/** Title can be unchecked if at least one other is checked. */
val canToggleTitle: Boolean get() = searchByAuthor || searchBySubject

/** Author can be unchecked if at least one other is checked. */
val canToggleAuthor: Boolean get() = searchByTitle || searchBySubject

/** Subject can be unchecked if at least one other is checked. */
val canToggleSubject: Boolean get() = searchByTitle || searchByAuthor
```

Update `toSearchParams()` — subject is always an additive `subject:` field query. Title+author together use general `q=` for broadest matching:

```kotlin
fun toSearchParams(): BookSearchParams {
    val trimmedQuery = query.trim()
    // Title+Author both checked = use general q= (searches across all metadata)
    // Subject is always an explicit subject: qualifier when checked
    val useGeneral = searchByTitle && searchByAuthor
    return BookSearchParams(
        general = if (useGeneral) trimmedQuery else null,
        title = if (!useGeneral && searchByTitle) trimmedQuery else null,
        author = if (!useGeneral && searchByAuthor) trimmedQuery else null,
        subject = if (searchBySubject) trimmedQuery else null
    )
}
```

Result matrix:
| Checkboxes | Query produced | Behavior |
|---|---|---|
| Title+Author | `q=X` | Current behavior (broadest) |
| Title+Author+Subject | `q=X subject:X` | General narrowed by topic |
| Title+Subject | `title:X subject:X` | Title AND topic |
| Author+Subject | `author:X subject:X` | Author AND topic |
| Title only | `title:X` | Current behavior |
| Author only | `author:X` | Current behavior |
| Subject only | `subject:X` | Topic search only |

Verified: `q=dragons subject:dragons` returns 7,133 results (good). `title:dragons author:dragons subject:dragons` returns 3 (catastrophically restrictive). The `useGeneral = searchByTitle && searchByAuthor` approach ensures all-three-checked is useful.

Add `withFilteredResults()` helper alongside existing `withResults()`:
```kotlin
fun withFilteredResults(allResults: List<Book>, safeResults: List<Book>): BookSearchState = copy(
    isLoading = false,
    hasSearched = true,
    errorMessage = null,
    results = safeResults,
    filteredCount = allResults.size - safeResults.size
)
```

**Existing test updates required**: `canToggleTitle`/`canToggleAuthor` tests now depend on `searchBySubject`. Existing tests still pass (default `searchBySubject = false`), but new tests needed for the three-way interaction.

### Step 7: Extend Search Pipeline (UseCase + DataSource)

**Modify**: `app/.../book/domain/usecase/SearchBooksUseCase.kt`
- Add params: `subjectFilter: String? = null`, `safeSearchEnabled: Boolean = true`

**Modify**: `app/.../book/domain/usecase/SearchBooksUseCaseImpl.kt`
- Add `MAX_SUBJECT_FILTER_LENGTH = 200` validation (same as title — subject queries are typically short tags but no reason to restrict more tightly)
- Pass `subjectFilter` to `remoteBookDataSource.searchBooks()`
- After mapping to `Book` list, apply safe search and return both counts:
  ```kotlin
  val books = dto.results.map { it.toBook() }
  if (safeSearchEnabled) books.filter { SafeSearchFilter.isBookSafe(it) } else books
  ```
- Note: the use case returns `Result<List<Book>>`. The filtered count is computed in the ViewModel by comparing `resultLimit` with `results.size`, or by having the use case return a richer type. Simpler: ViewModel passes `safeSearchEnabled` and does the filter + count itself. This keeps the use case simple and gives the VM the numbers it needs for UI feedback.

**Final decision**: Use case applies the filter and returns metadata:

```kotlin
data class SearchResult(
    val books: List<Book>,
    val filteredCount: Int
)
```

Use case returns `Result<SearchResult, DataError.Remote>`. This keeps filtering in the domain layer and gives the VM the count for UI.

**Important — `filteredCount` lifecycle**: When `safeSearchEnabled = false`, the use case must return `SearchResult(books, filteredCount = 0)` explicitly — not skip the field. This ensures that toggling safe search off and re-searching resets the count. The ViewModel uses `withFilteredResults()` (which sets `filteredCount`) for all search completions, never `withResults()` for remote search. This prevents stale `filteredCount` from a previous safe-search-on query leaking into a safe-search-off display.

**Modify**: `app/.../book/data/network/RemoteBookDataSource.kt`
- Add `subjectFilter: String? = null` to `searchBooks()`

**Modify**: `app/.../book/data/network/KtorRemoteBookDataSource.kt`
- Add `subjectFilter` param to `searchBooks()` and `buildQuery()`
- In `buildQuery()`, add subject block following the exact author/title pattern
- **Add quote stripping** to all filter branches (author, title, subject) — strip `"` characters before formatting to prevent query syntax injection:
  ```kotlin
  private fun sanitizeFilterInput(input: String): String = input.trim().replace("\"", "")
  ```
  Apply to all three filter params in `buildQuery()`. This fixes a pre-existing edge case.

### Step 8: Extend BookshelfAction

**Modify**: `app/.../bookshelf/presentation/BookshelfAction.kt`

Add:
- `data object OnToggleSearchBySubject : BookshelfAction`
- `data object OnToggleSafeSearch : BookshelfAction`

### Step 9: Extend BookshelfViewModel

**Modify**: `app/.../bookshelf/presentation/BookshelfViewModel.kt`

- Add constructor param: `private val searchPreferences: SearchPreferences`
- Add `observeSearchPreferences()` call in `init`
- New method — simple single-flow collection, no `combine()` needed:
  ```kotlin
  private fun observeSearchPreferences() {
      searchPreferences.observe()
          .onEach { prefs ->
              _state.update {
                  it.copy(bookSearchState = it.bookSearchState.copy(
                      searchByTitle = prefs.searchByTitle,
                      searchByAuthor = prefs.searchByAuthor,
                      searchBySubject = prefs.searchBySubject,
                      safeSearchEnabled = prefs.safeSearchEnabled
                  ))
              }
          }
          .launchIn(viewModelScope)
  }
  ```
- Handle `OnToggleSearchBySubject`: same pattern as title/author — check `canToggleSubject`, toggle, persist atomically, retrigger
- Handle `OnToggleSafeSearch`: toggle, persist atomically, retrigger
- Update existing `OnToggleSearchByTitle`/`OnToggleSearchByAuthor`: persist atomically (read current state, toggle one field, write whole state)
- Update `performSearch()`: pass `subjectFilter = params.subject`, `safeSearchEnabled = searchState.safeSearchEnabled`. Handle `SearchResult` return type to set both `results` and `filteredCount`.
- **No changes to `closeSearchDialog()`**: Preferences are continuously observed from DataStore via `observeSearchPreferences()`. When the dialog reopens, the preference flow has already re-populated `bookSearchState`. No manual preservation needed.

### Step 10: Update BookshelfModule DI

**Modify**: `app/.../bookshelf/di/BookshelfModule.kt`

Add `searchPreferences = get()` to ViewModel factory.

### Step 11: Extend LibraryAction + LibraryViewModel

**Modify**: `app/.../library/presentation/LibraryAction.kt`

Add:
- `data object OnToggleSearchBySubject : LibraryAction`
- `data object OnToggleSafeSearch : LibraryAction`

**Modify**: `app/.../library/presentation/LibraryViewModel.kt`

- Add constructor param: `searchPreferences: SearchPreferences`
- Same `searchPreferences.observe().onEach { ... }.launchIn(viewModelScope)` in `init`
- Same toggle handlers + performSearch updates as BookshelfViewModel
- **No changes to `OnDismissSearchDialog`** — preference flow re-populates on reopen
- Update LibraryModule DI

### Step 12: Extend Shared UI Components

**Modify**: `app/.../book/presentation/searchcomponents/AdvancedSearchFilters.kt`

Add parameters: `searchBySubject`, `subjectEnabled`, `safeSearchEnabled`, `onToggleSubject`, `onToggleSafeSearch`

Layout:
- Row 1: `[✓] Title  [✓] Author  [ ] Subject` (checkboxes)
- Row 2: `Safe Search [Switch]` (Switch, not Checkbox — visually distinct as a content filter vs. search mode)

**Modify**: `app/.../book/presentation/searchcomponents/BookSearchDialog.kt`

- Add `onToggleSearchBySubject` and `onToggleSafeSearch` params. Pass through to `SearchFilters`.
- Show filtered count feedback when `state.filteredCount > 0`:
  ```
  "X results (Y hidden by Safe Search)"
  ```
  Next to the existing `"${state.results.size} results found"` text.

### Step 13: Update Feature-Specific Search Dialogs + Screens

**Modify**: `app/.../bookshelf/presentation/searchcomponents/BookSearchCallbacks.kt`
- Add `val onToggleSearchBySubject: () -> Unit` and `val onToggleSafeSearch: () -> Unit`

**Modify**: `app/.../bookshelf/presentation/searchcomponents/ShelfBookSearchDialog.kt`
- Pass new callbacks through to `BookSearchDialog`

**Modify**: `app/.../bookshelf/presentation/BookshelfScreen.kt`
- Add new callback implementations in `object : BookSearchCallbacks`

**Modify**: `app/.../library/presentation/searchcomponents/LibraryBookSearchDialog.kt`
- Add `onToggleSearchBySubject` and `onToggleSafeSearch` params, pass through

**Modify**: `app/.../library/presentation/LibraryScreen.kt`
- Wire new callbacks to `LibraryAction.OnToggleSearchBySubject` and `LibraryAction.OnToggleSafeSearch`

### Step 14: String Resources

**Modify**: `app/src/main/res/values/strings.xml`

```xml
<string name="search_by_subject">Subject</string>
<string name="safe_search_label">Safe Search</string>
<string name="safe_search_filtered">%1$d results (%2$d hidden by Safe Search)</string>
```

### Step 15: Tests

**New file**: `app/src/test/.../book/domain/service/SafeSearchFilterTest.kt`
- Book with no subjects → safe
- Book with "Erotic fiction" → blocked
- Book with "Pornography" → blocked
- Book with "Science fiction" → safe
- Case insensitivity ("EROTICA", "Erotic", "eRoTiC")
- Mixed safe/unsafe subjects → blocked (one bad subject blocks the book)
- Book with "Adult education" → safe (not in blocklist)

**Modify**: `app/src/test/.../book/presentation/searchcomponents/BookSearchStateTest.kt`
- `toSearchParams` returns subject param when subject checked
- `toSearchParams` returns combined title+subject when both checked
- `toSearchParams` returns general when title+author checked (no subject) — existing behavior preserved
- `canToggleTitle` true when subject is checked (subject provides the "at least one other")
- `canToggleSubject` false when title and author are both unchecked
- Existing `canToggleTitle`/`canToggleAuthor` tests still pass (default searchBySubject=false doesn't change behavior)
- Defensive fallback (all false) still works

**Modify**: `app/src/test/.../bookshelf/presentation/BookshelfViewModelTest.kt`
- Add `StubSearchPreferences` mock implementing `SearchPreferences`
- Subject toggle changes state correctly
- Safe search toggle persists preference atomically
- Update `SimpleSearchBooksUseCase` to accept new params (`subjectFilter`, `safeSearchEnabled`) and return `SearchResult`
- Update `createViewModel()` to pass search preferences

---

## Verification

1. **Unit tests**: `./gradlew testDebugUnitTest` — all new and existing tests pass
2. **Lint**: `./gradlew detekt` — no new violations
3. **Manual testing**:
   - Open search dialog → checkboxes match persisted state
   - Check Subject only → search uses `subject:` query
   - Check Subject + Title → search uses `subject:X title:X` (combined, narrowed results)
   - Check Title + Author (no Subject) → uses general `q=` (existing behavior)
   - At least one checkbox always remains checked
   - Safe Search ON → search "erotica" → explicit results filtered, count shown
   - Safe Search OFF → search "erotica" → explicit results shown
   - Safe Search filters all results → user sees "0 results (15 hidden by Safe Search)" not misleading "No books found"
   - Dismiss and reopen dialog → preferences restored from DataStore
   - Kill and restart app → preferences preserved
   - Test in Library screen too — same behavior
