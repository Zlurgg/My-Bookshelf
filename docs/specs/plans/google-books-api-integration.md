# Google Books API Integration Plan

**Status:** ✅ Delivered on `spike-test-google-books` across sessions S1–S6 and subsequent UX polish. Two Section 10 fast-follows (pagination, printType filtering) remain explicitly out of scope and open for future work. Retained as historical design reference for the *why* behind architecture choices below.  
**Branch:** `spike-test-google-books`  
**Date:** 2026-05-22 (delivery confirmed 2026-05-26)  
**Revision:** v2 2026-05-22 — addressed retry/fallback conflict, provider tracking, layer placement, query building, HTML handling, enum typing, TOS analysis, test ordering, API key safety, pagination  
**Revision:** v3 2026-05-22 — fixed getBookDescription routing bug, added provider param to interface, HttpClientFactory comment, stripHtml visibility, quote stripping rationale, printType filtering deferred, commit strategy for UI changes

## Delivery summary (added post-delivery)

What landed beyond or differently from the plan as written:

- **Database version: reset to 1, not bumped to 5.** Item 1.8 of `google-books-integration-review-fixes.md` collapsed the schema since there are no real users to migrate. Same destructive-migration effect, cleaner starting state.
- **API key transport: header, not URL parameter.** Predecessor's 1.6 moved the key from `?key=…` to `X-Goog-Api-Key` to stop logcat exposure. `HttpClientFactory.redactSensitiveValues` provides defense-in-depth.
- **Blank API key handling refined.** Plan said "fail to FORBIDDEN → fallback to OL"; delivery introduced `DataError.Remote.PROVIDER_UNAVAILABLE` as a distinct signal (item 1.5) so the loud blank-key bug doesn't hide inside the same code path as a 403-from-Google. Both errors are in `FallbackRemoteBookDataSource.shouldFallback`.
- **Result cap reconciled.** Plan's `MAX_RESULTS = 15` was raised to 40 once on-device testing showed the ViewModel was capping below the API default (`google-books-search-quality.md` item 1).
- **English-language post-fetch filter added** (`google-books-search-quality.md` item 2) — `langRestrict` proved best-effort.
- **Blank-title filter added** during on-device testing — Google occasionally returns rows with metadata but no title.
- **DI wiring: `single { }` for Google services**, not `singleOf(::)`. The plan-suggested `singleOf` crashed at runtime because the `apiKeyProvider` test seam carries a Kotlin default value that `singleOf` ignores. Captured in `closed-testing-release-prep.md` as Phase 1.1 (Koin verify() test) so this class of bug fails fast in CI from now on.
- **Spine-vs-detail image parity.** Plan added `withMediumImage()` for the detail screen; spines still used the raw URL which both had Google's page-curl effect and a smaller `zoom=1` scan. Added `withSpineImage()` to normalize.
- **Search dialog UX redesigned beyond the original plan.** Custom Row layout, portrait cover, zebra stripe, conditional image slot, attribution promoted to the top.
- **Default search filter: title only.** Original UI defaulted to title + author both checked, which produced confusing AND-filter empty results on common queries; default changed to title only with user choices persisted.

**Still open (Section 10 fast-follows):**
- Pagination via `startIndex` / `offset`
- PrintType filtering as a user-toggleable option

These are unblocked from delivery but not in scope for the spike branch.

## Goal

Replace OpenLibrary with Google Books API as primary book search provider. Keep OpenLibrary as automatic fallback when Google quota is exhausted. Remove OL-specific UI features (community ratings, Internet Archive links, edition count). Add new fields Google Books provides that benefit the app.

No existing users, no data migration required — database can be rebuilt.

---

## 1. Architecture Overview

### Current Flow
```
UI -> ViewModel -> SearchBooksUseCase -> RemoteBookDataSource -> KtorRemoteBookDataSource -> OpenLibraryBookApi -> OpenLibrary API
                                                                                             |
                                                                                  SearchResponseDto (OL-specific)
                                                                                             |
                                                                                  BookMappers.toBook() -> Book domain model
```

### Target Flow
```
UI -> ViewModel -> SearchBooksUseCase -> RemoteBookDataSource -> FallbackRemoteBookDataSource
                                                                        |              |
                                                                  GoogleBooks     OpenLibrary
                                                                  (primary)       (fallback)
                                                                        |              |
                                                               GoogleBookDto    SearchedBookDto
                                                                        |              |
                                                                  toBook()        toBook() (existing)
                                                                        |              |
                                                                     Book domain model
                                                                   (with provider field)
```

### Key Architectural Decision: Where to Abstract

**Problem:** `RemoteBookDataSource` currently returns `SearchResponseDto` and `BookWorkDto` — these are OpenLibrary-specific DTOs. Returning provider-specific DTOs from the data source interface violates the principle of abstracting away implementation details.

**Solution:** Introduce a provider-neutral response type at the `RemoteBookDataSource` boundary. Each provider implementation maps its own DTOs -> `Book` internally, so the data source interface only exposes domain types. This is cleaner than having the UseCase know about DTOs.

**Why `getBookDescription` changes:** Google Books returns descriptions in search results — no separate call needed. The interface still exposes it for OL fallback, but the Google implementation returns it from a single volume GET.

---

## 2. Domain Model Changes

### Provider Tracking

Add a `BookProvider` enum to `Book` to explicitly track which API sourced the book. This replaces the fragile `isGoogleBooksId()` heuristic (checking for "OL" prefix) with an explicit, type-safe field.

```kotlin
enum class BookProvider {
    GOOGLE_BOOKS,
    OPEN_LIBRARY
}
```

This solves:
- **Description routing:** `FallbackRemoteBookDataSource.getBookDescription()` checks `provider` instead of guessing from ID format
- **Image URL handling:** `BookImageUtils` dispatches on `provider` instead of URL pattern matching
- **Book club dedup:** Two books with the same ISBN but different providers can be detected
- **Future extensibility:** Adding a third provider doesn't require updating heuristics

### Fields to Remove from `Book`

| Field | Reason |
|-------|--------|
| `averageRating: Double?` | Google Books removed ratings from API. OL ratings gone when OL is fallback-only. |
| `ratingCount: Int?` | Same as above. |
| `numEditions: Int` | OL-specific concept. Google Books has no equivalent. |
| `internetArchiveId: String?` | OL-specific. No value with Google Books primary. |

### Fields to Add to `Book`

| Field | Type | Source | Value |
|-------|------|--------|-------|
| `provider` | `BookProvider` | Set by mapper | Tracks which API sourced this book. Persisted to DB. |
| `subtitle` | `String?` | `volumeInfo.subtitle` | Many books have subtitles. Improves display. |
| `previewLink` | `String?` | `volumeInfo.previewLink` | Lets users preview on Google Books. Required by TOS. |
| `maturityRating` | `MaturityRating` | `volumeInfo.maturityRating` | Enum: `NOT_MATURE`, `MATURE`, `UNKNOWN`. Server-side content rating — stronger than our client-side safe search. |
| `printType` | `PrintType` | `volumeInfo.printType` | Enum: `BOOK`, `MAGAZINE`, `UNKNOWN`. Can filter out magazines. |
| `infoLink` | `String?` | `volumeInfo.infoLink` | Required by Google Books TOS — must link to Google Books page. |

### Enums for Closed Value Sets

```kotlin
enum class MaturityRating {
    NOT_MATURE,
    MATURE,
    UNKNOWN;

    companion object {
        fun fromApiValue(value: String?): MaturityRating = when (value) {
            "NOT_MATURE" -> NOT_MATURE
            "MATURE" -> MATURE
            else -> UNKNOWN
        }
    }
}

enum class PrintType {
    BOOK,
    MAGAZINE,
    UNKNOWN;

    companion object {
        fun fromApiValue(value: String?): PrintType = when (value) {
            "BOOK" -> BOOK
            "MAGAZINE" -> MAGAZINE
            else -> UNKNOWN
        }
    }
}
```

### Fields That Stay (mapped differently)

| Field | OpenLibrary Source | Google Books Source |
|-------|-------------------|-------------------|
| `id` | `key` (e.g., "/works/OL123W" -> "OL123W") | `id` (e.g., "dXz5DwAAQBAJ") |
| `title` | `title` | `volumeInfo.title` |
| `authors` | `author_name` | `volumeInfo.authors` |
| `imageUrl` | `covers.openlibrary.org/b/...` | `volumeInfo.imageLinks.thumbnail` |
| `description` | Separate `/works/{id}.json` call | `volumeInfo.description` (in search results!) |
| `languages` | `language` (list) | `volumeInfo.language` (single code -> wrap in list) |
| `firstPublishYear` | `first_publish_year` | `volumeInfo.publishedDate` (parse year) |
| `numPages` | `number_of_pages_median` | `volumeInfo.pageCount` |
| `isbn` | `isbn[0]` | `volumeInfo.industryIdentifiers` (prefer ISBN_13) |
| `publisher` | `publisher[0]` | `volumeInfo.publisher` |
| `publishDate` | `publish_date[0]` | `volumeInfo.publishedDate` |
| `subjects` | `subject` | `volumeInfo.categories` |

### Updated `Book` Domain Model

```kotlin
data class Book(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val authors: List<String>,
    val imageUrl: String,
    val description: String?,
    val languages: List<String>,
    val firstPublishYear: String?,
    val numPages: Int?,
    val purchased: Boolean,
    val spineColor: Int, // 0 = unset, generated by BookRepositoryImpl on first persist

    // Provider tracking
    val provider: BookProvider = BookProvider.GOOGLE_BOOKS,

    // Personal metadata
    val readingStatus: ReadingStatus = ReadingStatus.NOT_READ,
    val personalRating: Float = 0f,
    val personalNotes: String = "",
    val dateAdded: Long? = null,
    val purchaseDate: Long? = null,

    // Enhanced metadata from API
    val isbn: String? = null,
    val publisher: String? = null,
    val publishDate: String? = null,
    val subjects: List<String> = emptyList(),

    // Google Books metadata (null for OL-sourced books)
    val previewLink: String? = null,
    val infoLink: String? = null,
    val maturityRating: MaturityRating = MaturityRating.UNKNOWN,
    val printType: PrintType = PrintType.UNKNOWN,
)
```

### Updated `BookEntity`

Mirror the domain model changes:
- Remove: `ratingsAverage`, `ratingsCount`, `numEditions`, `internetArchiveId`
- Add: `subtitle`, `provider` (stored as String), `previewLink`, `infoLink`, `maturityRating` (stored as String), `printType` (stored as String)
- Bump database version (destructive migration since no users)

### Updated `BookClubBookDto` (Firestore)

- Remove: `averageRating`, `ratingCount`, `workId`
- Add: `subtitle`, `provider` (stored as String for Firestore compatibility)
- The `id` field already stores the book ID — no need for separate `workId`

---

## 3. `BookSearchResponse` — Domain Layer Placement

The provider-neutral response type belongs in the **domain layer**, alongside `SearchResult`:

```kotlin
// book/domain/model/BookSearchResponse.kt
data class BookSearchResponse(
    val totalResults: Int,
    val books: List<Book>
)
```

**Rationale:** It contains only domain types (`Book`). It's consumed by `SearchBooksUseCase` (domain layer). Placing it in the data layer alongside `RemoteBookDataSource` would work but creates an odd dependency where a data-layer file imports domain types. Domain placement is cleaner.

The `RemoteBookDataSource` interface (data layer) imports from domain — this is allowed since data depends on domain, not the reverse.

```kotlin
// book/data/network/RemoteBookDataSource.kt
interface RemoteBookDataSource {
    suspend fun searchBooks(
        query: String,
        resultLimit: Int? = null,
        language: String? = null,
        authorFilter: String? = null,
        titleFilter: String? = null,
        subjectFilter: String? = null,
        sort: String? = null
    ): Result<BookSearchResponse, DataError.Remote>

    suspend fun getBookDescription(
        bookId: String,
        provider: BookProvider
    ): Result<String?, DataError.Remote>
}
```

**Why `provider` is on the interface:** Without it, `FallbackRemoteBookDataSource` must guess which provider owns a book ID. Sending an OL ID to Google returns 404, which isn't in `shouldFallback` — so the description silently fails. The `provider` parameter makes routing explicit and prevents a real user-facing bug.

---

## 4. Retry/Fallback Conflict Fix

### Problem

`HttpClientFactory` retries 429 and 5xx errors 3 times with exponential backoff. When Google quota is hit (429/403), Ktor will retry 3 times against a quota wall (~15-20 seconds wasted) before `FallbackRemoteBookDataSource` even sees the error.

### Solution

Split errors into retryable (transient) and non-retryable (permanent) categories:

```kotlin
// In HttpClientFactory.kt
install(HttpRequestRetry) {
    maxRetries = MAX_RETRIES
    retryIf { _, httpResponse ->
        httpResponse.status == HttpStatusCode.InternalServerError ||
            httpResponse.status == HttpStatusCode.BadGateway ||
            httpResponse.status == HttpStatusCode.ServiceUnavailable ||
            httpResponse.status == HttpStatusCode.GatewayTimeout
        // REMOVED: HttpStatusCode.TooManyRequests — not transient, quota-based
    }
    retryOnExceptionIf { _, cause ->
        cause is SocketTimeoutException ||
            cause is UnknownHostException ||
            cause is ConnectException ||
            cause is io.ktor.client.network.sockets.SocketTimeoutException ||
            cause is io.ktor.util.network.UnresolvedAddressException
    }
    exponentialDelay(base = 1.0, maxDelayMs = ApiConfig.Http.MAX_RETRY_DELAY_MS)
}
```

**Key change:** Remove `HttpStatusCode.TooManyRequests` (429) from the retry condition. A 429 from Google means "quota exceeded for the day" — retrying won't help. The `FallbackRemoteBookDataSource` sees the 429 immediately and switches to OpenLibrary.

**What about OL rate limiting?** OL's informal rate limiting could also return 429. But OL is the fallback — if OL also 429s, there's nothing to fall back to. Retrying OL's 429 with backoff could help if it's a transient burst, but the current plan doesn't retry at the fallback level. If this becomes an issue, we can add OL-specific retry logic later.

**Note:** 403 (Forbidden) is already not retried — it's mapped to `DataError.Remote.FORBIDDEN` by `ErrorMapper`.

---

## 5. Implementation Steps

### Phase 1: Domain Model Changes (Foundation)

**Step 1.1 — New enums**

New files:
- `book/domain/model/BookProvider.kt`
- `book/domain/model/MaturityRating.kt`
- `book/domain/model/PrintType.kt`

**Step 1.2 — Update `Book` domain model**

File: `book/domain/model/Book.kt`
- Remove: `averageRating`, `ratingCount`, `numEditions`, `internetArchiveId`
- Add: `provider`, `subtitle`, `previewLink`, `infoLink`, `maturityRating`, `printType`

**Step 1.3 — New `BookSearchResponse`**

New file: `book/domain/model/BookSearchResponse.kt`

**Step 1.4 — Update `BookEntity`**

File: `core/data/database/entity/BookEntity.kt`
- Mirror domain changes. Store enums as Strings.

**Step 1.5 — Database version bump**

File: `core/data/database/MyBookshelfRoomDatabase.kt`
- Bump version to 5
- Use `fallbackToDestructiveMigration()`

### Phase 2: Update Existing Mappers and Callers (Keep Compiling)

**Step 2.1 — Update `BookMappers.kt`**

File: `book/data/mappers/BookMappers.kt`
- `SearchedBookDto.toBook()` — remove old fields, add `provider = BookProvider.OPEN_LIBRARY`, set new Google-specific fields to defaults
- `Book.toBookEntity()` — map new fields (enums to `.name`), remove old
- `BookEntity.toBook()` — map new fields (String to enum via `valueOf`), remove old

**Step 2.2 — Update `BookClubBookDto` and mappers**

File: `bookclub/data/dto/BookClubBookDto.kt` — remove `averageRating`, `ratingCount`, `workId`; add `subtitle`, `provider`

File: `bookclub/data/mappers/BookClubMappers.kt` — update both mapping functions

**Step 2.3 — Update UI (remove OL-specific displays)**

- Delete: `bookdetail/presentation/components/CommunityRatingsCard.kt`
- Modify: `BookDetailScreen.kt` — remove `CommunityRatingsCard` usage, `numEditions`, `internetArchiveId`
- Modify: `BookOverviewCard.kt` — remove `numEditions` parameter
- Modify: `BookHeroSection.kt` — remove `numEditions` parameter
- Modify: `PublicationDetailsCard.kt` — remove `internetArchiveId` parameter

**Step 2.4 — Write mapper tests alongside changes**

- Update `BookMappersTest` for field changes
- Update test builders (`TestBookBuilder`, `TestSearchedBookDtoBuilder`)
- Verify OL mapper still works correctly with fallback path

### Phase 3: Refactor `RemoteBookDataSource` Interface

**Step 3.1 — Update interface**

File: `book/data/network/RemoteBookDataSource.kt`
- Return `BookSearchResponse` instead of `SearchResponseDto`
- Change `getBookDetails` to `getBookDescription(bookId: String, provider: BookProvider): Result<String?, DataError.Remote>`

**Step 3.2 — Rename and update `KtorRemoteBookDataSource` -> `OpenLibraryRemoteBookDataSource`**

Rename file for clarity. Update to return `BookSearchResponse`:

```kotlin
class OpenLibraryRemoteBookDataSource(
    private val apiService: OpenLibraryBookApi,
    private val systemLanguageProvider: SystemLanguageProvider
) : RemoteBookDataSource {

    override suspend fun searchBooks(...): Result<BookSearchResponse, DataError.Remote> {
        // ... existing query building + sanitization logic (keep as-is) ...
        return ErrorMapper.httpNetworkCall<SearchResponseDto> {
            apiService.searchBooks(...)
        }.map { dto ->
            BookSearchResponse(
                totalResults = dto.numFound,
                books = dto.results.map { it.toBook() }
            )
        }
    }

    override suspend fun getBookDescription(
        bookId: String,
        provider: BookProvider
    ): Result<String?, DataError.Remote> {
        return ErrorMapper.httpNetworkCall<BookWorkDto> {
            apiService.getBookDetails(bookId)
        }.map { it.description }
    }
}
```

**Step 3.3 — Update `SearchBooksUseCase`**

File: `book/domain/usecase/SearchBooksUseCaseImpl.kt`

The UseCase no longer maps DTOs — it receives `BookSearchResponse` containing `Book` objects:

```kotlin
class SearchBooksUseCaseImpl(
    private val remoteBookDataSource: RemoteBookDataSource
) : SearchBooksUseCase {

    override suspend operator fun invoke(...): Result<SearchResult, DataError.Remote> {
        // ... existing validation ...

        return remoteBookDataSource.searchBooks(
            query, resultLimit, language, authorFilter, titleFilter, subjectFilter, sort = null
        ).map { response ->
            val safeBooks = if (safeSearchEnabled) {
                response.books.filter { SafeSearchFilter.isBookSafe(it) }
            } else {
                response.books
            }
            SearchResult(
                books = safeBooks,
                filteredCount = response.books.size - safeBooks.size
            )
        }
    }
}
```

**Step 3.4 — Update `BookRepository` interface and `BookRepositoryImpl`**

File: `book/domain/repository/BookRepository.kt` — add `provider` parameter:

```kotlin
suspend fun getBookDescription(bookId: String, provider: BookProvider): Result<String?, DataError.Remote>
```

File: `book/data/repository/BookRepositoryImpl.kt` — pass through:

```kotlin
override suspend fun getBookDescription(
    bookId: String,
    provider: BookProvider
): Result<String?, DataError.Remote> {
    return remoteBookDataSource.getBookDescription(bookId, provider)
}
```

File: `bookdetail/domain/usecase/GetBookDetailsUseCase.kt` (interface) and `GetBookDetailsUseCaseImpl.kt` — add `provider` parameter to `loadBookDescription`:

```kotlin
suspend fun loadBookDescription(bookId: String, provider: BookProvider): Result<Unit, DataError.Local>
```

The ViewModel already has the `Book` object (from search results or DB), so it passes `book.provider` directly. No DB lookup needed for provider resolution — avoids the null-book edge case where a search result hasn't been persisted yet.

**Step 3.5 — Update tests for refactored interface**

- `SearchBooksUseCaseTest` — update since UseCase no longer maps DTOs
- `BookRepositoryImplTest` — update `getBookDescription` test

### Phase 4: Google Books Network Layer (New Files)

**Step 4.1 — API Key Configuration**

Files to modify:
- `app/build.gradle.kts` — add `GOOGLE_BOOKS_API_KEY` BuildConfig field with empty default
- `core/data/network/ApiConfig.kt` — add `GoogleBooks` config object
- `local.properties` (gitignored) — store actual key
- `.gitignore` — verify `local.properties` is excluded

```kotlin
// In build.gradle.kts — both debug and release
val googleBooksApiKey: String = project.findProperty("GOOGLE_BOOKS_API_KEY")?.toString() ?: ""
buildConfigField("String", "GOOGLE_BOOKS_API_KEY", "\"$googleBooksApiKey\"")
```

```kotlin
// In ApiConfig.kt
object GoogleBooks {
    val apiKey: String = BuildConfig.GOOGLE_BOOKS_API_KEY
    const val BASE_URL = "https://www.googleapis.com/books/v1"
    val searchEndpoint = "$BASE_URL/volumes"
    fun volumeEndpoint(volumeId: String) = "$BASE_URL/volumes/$volumeId"

    object DefaultParams {
        const val MAX_RESULTS = 15
        // No PROJECTION constant — we use full projection because we want descriptions
    }
}
```

**API key safety:** If `GOOGLE_BOOKS_API_KEY` is empty (contributor clones without setup), the Google data source will get 403 errors. `FallbackRemoteBookDataSource` catches this and falls back to OpenLibrary. No crash at class-load time. Log a warning at search time if key is empty.

**Security:** API key stored in `local.properties` (gitignored) -> read by `build.gradle.kts` -> compiled into BuildConfig. Restrict key in Google Cloud Console to Android apps only (package name + SHA-1) and Books API only. CI/CD uses environment variables.

**Step 4.2 — Google Books DTOs**

New file: `book/data/dto/google/GoogleBooksSearchResponseDto.kt`

All Google DTOs in one file (they're small and tightly related):

```kotlin
@Serializable
data class GoogleBooksSearchResponseDto(
    val totalItems: Int = 0,
    val items: List<GoogleBookItemDto>? = null  // null when no results (not empty list)
)

@Serializable
data class GoogleBookItemDto(
    val id: String,
    val volumeInfo: GoogleVolumeInfoDto? = null,
    val searchInfo: GoogleSearchInfoDto? = null,
)

@Serializable
data class GoogleVolumeInfoDto(
    val title: String? = null,
    val subtitle: String? = null,
    val authors: List<String>? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val categories: List<String>? = null,
    val imageLinks: GoogleImageLinksDto? = null,
    val language: String? = null,
    val previewLink: String? = null,
    val infoLink: String? = null,
    val maturityRating: String? = null,
    val printType: String? = null,
    val industryIdentifiers: List<GoogleIndustryIdentifierDto>? = null,
)

@Serializable
data class GoogleImageLinksDto(
    val smallThumbnail: String? = null,
    val thumbnail: String? = null,
)

@Serializable
data class GoogleIndustryIdentifierDto(
    val type: String,
    val identifier: String,
)

@Serializable
data class GoogleSearchInfoDto(
    val textSnippet: String? = null,
)
```

**Step 4.3 — Google Books API Service**

New file: `book/data/network/api/GoogleBooksApiService.kt`

```kotlin
interface GoogleBooksBookApi : BookApiService

class GoogleBooksApiService(
    private val httpClient: HttpClient
) : GoogleBooksBookApi {

    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        sort: String?
    ): HttpResponse {
        return httpClient.get(ApiConfig.GoogleBooks.searchEndpoint) {
            parameter("q", query)
            parameter("key", ApiConfig.GoogleBooks.apiKey)
            parameter("maxResults", resultLimit ?: ApiConfig.GoogleBooks.DefaultParams.MAX_RESULTS)
            language?.let { parameter("langRestrict", it) }
            sort?.let { parameter("orderBy", it) }
        }
    }

    override suspend fun getBookDetails(bookId: String): HttpResponse {
        return httpClient.get(ApiConfig.GoogleBooks.volumeEndpoint(bookId)) {
            parameter("key", ApiConfig.GoogleBooks.apiKey)
        }
    }
}
```

**Step 4.4 — Google Books Mapper**

New file: `book/data/mappers/GoogleBookMappers.kt`

```kotlin
fun GoogleBookItemDto.toBook(): Book {
    val volumeInfo = this.volumeInfo
    val isbn = volumeInfo?.industryIdentifiers
        ?.firstOrNull { it.type == "ISBN_13" }?.identifier
        ?: volumeInfo?.industryIdentifiers?.firstOrNull()?.identifier

    // Google serves HTTP URLs — force HTTPS
    val imageUrl = volumeInfo?.imageLinks?.thumbnail
        ?.replace("http://", "https://") ?: ""

    return Book(
        id = this.id,
        title = volumeInfo?.title ?: "",
        subtitle = volumeInfo?.subtitle,
        authors = volumeInfo?.authors ?: emptyList(),
        imageUrl = imageUrl,
        description = stripHtml(volumeInfo?.description),
        languages = listOfNotNull(volumeInfo?.language),
        firstPublishYear = volumeInfo?.publishedDate?.take(4),
        numPages = volumeInfo?.pageCount,
        purchased = false,
        spineColor = 0,
        provider = BookProvider.GOOGLE_BOOKS,
        isbn = isbn,
        publisher = volumeInfo?.publisher,
        publishDate = volumeInfo?.publishedDate,
        subjects = volumeInfo?.categories ?: emptyList(),
        previewLink = volumeInfo?.previewLink,
        infoLink = volumeInfo?.infoLink,
        maturityRating = MaturityRating.fromApiValue(volumeInfo?.maturityRating),
        printType = PrintType.fromApiValue(volumeInfo?.printType),
    )
}

/**
 * Strips HTML tags and decodes entities from Google Books descriptions.
 * Google returns descriptions with HTML formatting (<b>, <i>, <br>, &amp;, etc.).
 * We strip to plain text at the data layer to keep the domain model clean.
 *
 * Internal visibility: used by both GoogleBookMappers and GoogleBooksRemoteBookDataSource.
 */
internal fun stripHtml(html: String?): String? {
    if (html == null) return null
    return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        .toString()
        .trim()
}
```

**Why `HtmlCompat.fromHtml()` instead of regex:** Handles HTML entities (`&amp;`, `&nbsp;`, `&#39;`), malformed tags, and `<br>` whitespace correctly. It's the Android-standard approach and avoids the false simplicity of regex-based stripping.

**Step 4.5 — Google Books Data Source**

New file: `book/data/network/GoogleBooksRemoteBookDataSource.kt`

```kotlin
class GoogleBooksRemoteBookDataSource(
    private val apiService: GoogleBooksBookApi,
    private val systemLanguageProvider: SystemLanguageProvider
) : RemoteBookDataSource {

    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String?,
        sort: String?
    ): Result<BookSearchResponse, DataError.Remote> {
        val apiKey = ApiConfig.GoogleBooks.apiKey
        if (apiKey.isBlank()) {
            Timber.tag(TAG).w("Google Books API key is not configured, returning FORBIDDEN")
            return Result.Error(DataError.Remote.FORBIDDEN)
        }

        val finalQuery = buildQuery(query, authorFilter, titleFilter, subjectFilter)
        val finalLanguage = language ?: systemLanguageProvider.getCurrentLanguageCode()

        Timber.tag(TAG).d("=== GOOGLE BOOKS SEARCH ===")
        Timber.tag(TAG).d("Final query: '%s', language: %s", finalQuery, finalLanguage)

        return ErrorMapper.httpNetworkCall<GoogleBooksSearchResponseDto> {
            apiService.searchBooks(
                query = finalQuery,
                resultLimit = resultLimit,
                language = finalLanguage,
                sort = sort
            )
        }.map { dto ->
            Timber.tag(TAG).d("Results: %d total, %d returned", dto.totalItems, dto.items?.size ?: 0)
            BookSearchResponse(
                totalResults = dto.totalItems,
                books = dto.items?.map { it.toBook() } ?: emptyList()
            )
        }
    }

    override suspend fun getBookDescription(
        bookId: String,
        provider: BookProvider
    ): Result<String?, DataError.Remote> {
        return ErrorMapper.httpNetworkCall<GoogleBookItemDto> {
            apiService.getBookDetails(bookId)
        }.map { stripHtml(it.volumeInfo?.description) }
    }

    /**
     * Builds a Google Books query string from base query and optional filters.
     * Uses Google's search prefixes: intitle:, inauthor:, subject:.
     * Multi-word filter values are quoted to ensure correct parsing.
     */
    private fun buildQuery(
        baseQuery: String,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String?
    ): String {
        val parts = mutableListOf<String>()

        if (baseQuery.isNotBlank()) {
            parts.add(sanitizeFilterInput(baseQuery))
        }

        authorFilter?.takeIf { it.isNotBlank() }?.let {
            parts.add(formatFilterField(it, "inauthor"))
        }
        titleFilter?.takeIf { it.isNotBlank() }?.let {
            parts.add(formatFilterField(it, "intitle"))
        }
        subjectFilter?.takeIf { it.isNotBlank() }?.let {
            parts.add(formatFilterField(it, "subject"))
        }

        // Join with spaces — Ktor URL-encodes as %20 automatically
        return parts.joinToString(" ")
    }

    // Strips double quotes from input. This loses quotes that are part of actual titles
    // (e.g., "Hello" World → Hello World), but neither API supports escaped quotes in
    // field filters, so stripping is the only safe option.
    private fun sanitizeFilterInput(input: String): String = input.trim().replace("\"", "")

    private fun formatFilterField(raw: String, prefix: String): String {
        val sanitized = sanitizeFilterInput(raw)
        val quoted = if (sanitized.contains(" ")) "\"$sanitized\"" else sanitized
        return "$prefix:$quoted"
    }

    companion object {
        private const val TAG = "GoogleBooksSearch"
    }
}
```

**Step 4.6 — Write tests alongside**

- `GoogleBookMappersTest` — ISBN preference (13 over 10), HTTP->HTTPS, null volumeInfo, HTML stripping, maturityRating/printType enum mapping
- `GoogleBooksRemoteBookDataSourceTest` — query building with multi-word authors, empty query guard, blank API key handling

### Phase 5: Fallback Strategy

**Step 5.1 — `FallbackRemoteBookDataSource`**

New file: `book/data/network/FallbackRemoteBookDataSource.kt`

```kotlin
class FallbackRemoteBookDataSource(
    private val primary: GoogleBooksRemoteBookDataSource,
    private val fallback: OpenLibraryRemoteBookDataSource,
) : RemoteBookDataSource {

    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String?,
        sort: String?
    ): Result<BookSearchResponse, DataError.Remote> {
        val result = primary.searchBooks(
            query, resultLimit, language, authorFilter, titleFilter, subjectFilter, sort
        )

        return when {
            result is Result.Error && shouldFallback(result.error) -> {
                Timber.tag(TAG).w(
                    "Google Books unavailable (%s), falling back to OpenLibrary",
                    result.error
                )
                fallback.searchBooks(
                    query, resultLimit, language, authorFilter, titleFilter, subjectFilter, sort
                )
            }
            else -> result
        }
    }

    override suspend fun getBookDescription(
        bookId: String,
        provider: BookProvider
    ): Result<String?, DataError.Remote> {
        // Route directly to the correct provider — no guessing from ID format
        return when (provider) {
            BookProvider.GOOGLE_BOOKS -> primary.getBookDescription(bookId, provider)
            BookProvider.OPEN_LIBRARY -> fallback.getBookDescription(bookId, provider)
        }
    }

    private fun shouldFallback(error: DataError.Remote): Boolean {
        return error == DataError.Remote.TOO_MANY_REQUESTS ||
            error == DataError.Remote.FORBIDDEN
    }

    companion object {
        private const val TAG = "BookSearchFallback"
    }
}
```

**Why this works:** The `provider` parameter on `getBookDescription` means the fallback routes directly to the correct provider — no wasted requests, no ID-format guessing. The `BookRepository` has the `Book` object (with `provider` field) and passes it through.

**Step 5.2 — Write `FallbackRemoteBookDataSourceTest`**

- Verify search fallback triggers on `TOO_MANY_REQUESTS` and `FORBIDDEN`
- Verify no search fallback on `SERVER_ERROR`, `REQUEST_TIMEOUT`, `NO_INTERNET`
- Verify `getBookDescription` routes to Google for `GOOGLE_BOOKS` provider
- Verify `getBookDescription` routes to OpenLibrary for `OPEN_LIBRARY` provider

### Phase 6: Retry Policy Fix

**Step 6.1 — Update `HttpClientFactory`**

File: `core/data/network/HttpClientFactory.kt`

Remove `HttpStatusCode.TooManyRequests` from `retryIf` condition. This ensures 429 errors reach `FallbackRemoteBookDataSource` immediately instead of being retried 3 times against a quota wall.

Add an inline comment explaining the decision:

```kotlin
// 429 not retried — Google Books returns 429 for daily quota exhaustion (not transient).
// FallbackRemoteBookDataSource handles this by switching providers.
```

### Phase 7: Wire DI

**Step 7.1 — Update `BookModule.kt`**

File: `book/di/BookModule.kt`

```kotlin
// Network — both providers
singleOf(::GoogleBooksApiService).bind<GoogleBooksBookApi>()
singleOf(::OpenLibraryApiService).bind<OpenLibraryBookApi>()

// Data sources — both providers
singleOf(::GoogleBooksRemoteBookDataSource)
singleOf(::OpenLibraryRemoteBookDataSource)

// Fallback wrapper as the single RemoteBookDataSource
single<RemoteBookDataSource> {
    FallbackRemoteBookDataSource(
        primary = get<GoogleBooksRemoteBookDataSource>(),
        fallback = get<OpenLibraryRemoteBookDataSource>()
    )
}
```

### Phase 8: Presentation Updates

**Step 8.1 — Update `BookImageUtils`**

File: `book/presentation/util/BookImageUtils.kt`

Make provider-aware using the `provider` field (not URL pattern matching):

```kotlin
fun Book.withLargeImage(): String = when (provider) {
    BookProvider.OPEN_LIBRARY ->
        imageUrl.replace(Regex("-[SML]\\.jpg$"), "-L.jpg")
    BookProvider.GOOGLE_BOOKS ->
        imageUrl.replace("zoom=1", "zoom=2").replace("&edge=curl", "")
}
```

Similarly for `withSmallImage()` and `withMediumImage()`.

**Step 8.2 — Enhance `SafeSearchFilter` with `maturityRating`**

File: `book/domain/service/SafeSearchFilter.kt`

Add `maturityRating` check:

```kotlin
fun isBookSafe(book: Book): Boolean {
    // Server-side content rating from Google Books — most reliable signal
    if (book.maturityRating == MaturityRating.MATURE) return false

    // Existing client-side keyword filtering (still needed for OL fallback books)
    return !containsBlockedKeyword(book.title) && !containsBlockedSubject(book.subjects)
}
```

**Step 8.3 — Add Google Books TOS attribution**

- Add "Powered by Google" text near search results
- Add "View on Google Books" link using `infoLink` in book detail
- Add "Preview" button using `previewLink` in book detail

**Step 8.4 — Add subtitle display**

- `BookHeroSection.kt` — display subtitle under title
- `BookDetailScreen.kt` — pass subtitle through

**Step 8.5 — Update remaining UI**

- `BookDetailViewModelTest` — remove community ratings assertions
- `BookshelfDaoTest` — update for schema changes

### Phase 9: Final Test Pass

**New tests (written alongside phases above):**
- `GoogleBookMappersTest` (Phase 4.6)
- `GoogleBooksRemoteBookDataSourceTest` (Phase 4.6)
- `FallbackRemoteBookDataSourceTest` (Phase 5.2)

**Updated tests:**
- `BookMappersTest` (Phase 2.4)
- `SearchBooksUseCaseTest` (Phase 3.5)
- `BookRepositoryImplTest` (Phase 3.5)
- `BookDetailViewModelTest` (Phase 8.5)
- `BookshelfDaoTest` (Phase 8.5)
- All test builders: `TestBookBuilder`, `TestSearchedBookDtoBuilder` (Phase 2.4)

---

## 6. File Change Summary

### New Files
| File | Purpose |
|------|---------|
| `book/domain/model/BookProvider.kt` | Enum: `GOOGLE_BOOKS`, `OPEN_LIBRARY` |
| `book/domain/model/MaturityRating.kt` | Enum: `NOT_MATURE`, `MATURE`, `UNKNOWN` |
| `book/domain/model/PrintType.kt` | Enum: `BOOK`, `MAGAZINE`, `UNKNOWN` |
| `book/domain/model/BookSearchResponse.kt` | Provider-neutral search response |
| `book/data/dto/google/GoogleBooksSearchResponseDto.kt` | Google Books DTOs |
| `book/data/network/api/GoogleBooksApiService.kt` | Google Books HTTP API service |
| `book/data/network/GoogleBooksRemoteBookDataSource.kt` | Google Books data source |
| `book/data/network/FallbackRemoteBookDataSource.kt` | Primary/fallback routing |
| `book/data/mappers/GoogleBookMappers.kt` | Google DTO -> Book mapper + HTML stripping |

### Modified Files
| File | Changes |
|------|---------|
| `book/domain/model/Book.kt` | Remove 4 fields, add 6 fields (including `provider`) |
| `core/data/database/entity/BookEntity.kt` | Mirror domain changes, enums as Strings |
| `book/data/network/RemoteBookDataSource.kt` | Return `BookSearchResponse` instead of `SearchResponseDto` |
| `book/data/network/KtorRemoteBookDataSource.kt` | Rename -> `OpenLibraryRemoteBookDataSource`, return `BookSearchResponse` |
| `book/data/mappers/BookMappers.kt` | Update for field changes, add `provider = OPEN_LIBRARY` |
| `book/domain/usecase/SearchBooksUseCaseImpl.kt` | Simplify — no longer maps DTOs |
| `book/data/repository/BookRepositoryImpl.kt` | Simplify `getBookDescription` |
| `core/data/network/ApiConfig.kt` | Add `GoogleBooks` config |
| `core/data/network/HttpClientFactory.kt` | Remove 429 from retry policy |
| `app/build.gradle.kts` | Add `GOOGLE_BOOKS_API_KEY` BuildConfig with empty default |
| `book/di/BookModule.kt` | Wire both providers + fallback |
| `book/presentation/util/BookImageUtils.kt` | Provider-aware via `BookProvider` enum |
| `book/domain/service/SafeSearchFilter.kt` | Add `maturityRating` check |
| `bookclub/data/dto/BookClubBookDto.kt` | Remove old fields, add `subtitle`, `provider` |
| `bookclub/data/mappers/BookClubMappers.kt` | Update field mappings |
| `core/data/database/MyBookshelfRoomDatabase.kt` | Bump version, destructive migration |

### Deleted Files
| File | Reason |
|------|--------|
| `bookdetail/presentation/components/CommunityRatingsCard.kt` | No ratings data available |

### UI Files Modified
| File | Changes |
|------|---------|
| `bookdetail/presentation/BookDetailScreen.kt` | Remove ratings card, numEditions, internetArchiveId; add subtitle, preview/info links, attribution |
| `bookdetail/presentation/components/BookHeroSection.kt` | Remove numEditions, add subtitle |
| `bookdetail/presentation/components/BookOverviewCard.kt` | Remove numEditions |
| `bookdetail/presentation/components/PublicationDetailsCard.kt` | Remove internetArchiveId |

---

## 7. Testing the Fallback

To verify fallback works without waiting for real quota exhaustion:

1. **Unit tests:** `FallbackRemoteBookDataSourceTest` with stubbed data sources returning specific errors
2. **Manual QA:** Add a debug-only developer settings toggle that forces the Google data source to return `Result.Error(DataError.Remote.TOO_MANY_REQUESTS)` before making the actual call
3. **Live test:** Temporarily use an unrestricted API key and set a daily budget cap in Google Cloud Console billing

Recommended approach: **#1 for correctness, #2 for QA confidence.**

---

## 8. Critical Analysis

### Edge Cases Not Addressed

1. **Mixed-provider book clubs:** If a user adds a Google Books result to a club, then quota is hit, and another user searches the same book — they'll get an OL result with a different ID. The `provider` field makes this visible, and ISBN-based dedup could be added later. **For this spike: accept the risk.** Book clubs are a secondary feature and quota exhaustion is rare.

2. **Description encoding mismatch:** Google returns HTML, OL returns plain text. **Solved:** `HtmlCompat.fromHtml()` in the Google mapper normalizes to plain text.

3. **Image URL longevity:** Google Books image URLs may expire. Persisted books reference these URLs. **Mitigation:** Coil caching handles this gracefully. Broken URL = no cover, but book still works.

4. **Language filtering difference:** OL uses `language` param with a list. Google uses `langRestrict` with a single code. May differ for multilingual searches. **Accept for now** — the common case (English) works identically.

5. **Pagination:** Neither the current OL integration nor this plan implement pagination (both fetch a fixed 15 results). Google Books supports `startIndex` for proper pagination. **Not a regression.** Can be added as a future enhancement — the `totalResults` field in `BookSearchResponse` is already there to support it.

### Assumptions That Might Not Hold

1. **1,000 req/day is enough.** Probably fine for a personal bookshelf app. If not, Google allows requesting quota increases. The fallback is a safety net.

2. **Google Books search query syntax maps cleanly.** Verified: `intitle:`, `inauthor:`, `subject:` are documented. Multi-word values need quoting — addressed in the query builder.

3. **Google Books always returns `items`.** Returns `null` for `items` when no results. Handled: `dto.items?.map { ... } ?: emptyList()`.

4. **`maturityRating` is populated for most books.** May be absent. `MaturityRating.UNKNOWN` handles this — we don't filter unknown, only explicit `MATURE`.

5. **`HtmlCompat` is available in the data layer.** It's part of `androidx.core` which is already a dependency. If there's a concern about Android framework dependency in the data layer, we can extract to a `DescriptionSanitizer` interface. **Pragmatic choice: just use it.**

### Simpler Alternatives Considered

1. **Just swap, no fallback.** Simpler, but risky. The fallback adds ~1 extra file.
2. **Abstract at UseCase level.** Would duplicate business logic. DataSource is the right boundary.
3. **Shared DTO.** Wrong — provider DTOs have different shapes and semantics.

### Potential Performance Issues

1. **Google Books is faster than OL** — improvement.
2. **Fallback latency when triggered.** 429/403 responses are fast (no timeout). With the retry fix (Phase 6), there's no wasted retry delay. Fallback adds only the OL call latency.
3. **Description included in search results.** Eliminates one network round-trip for book details — performance win.
4. **`HtmlCompat.fromHtml()` on every description.** Negligible — it's a string parse, not a network call.

### Security Concerns

1. **API key in APK.** Unavoidable. Restrict in Google Cloud Console.
2. **API key in source control.** `local.properties` is gitignored. CI uses env vars.
3. **HTML in descriptions.** `HtmlCompat.fromHtml()` strips to plain text. No XSS risk.
4. **Image URLs.** Force HTTPS in mapper.
5. **Empty API key.** Fails gracefully with FORBIDDEN -> fallback to OL. No crash.

### Clean Architecture Compliance

- **SRP:** `FallbackRemoteBookDataSource` routes. Each provider data source talks to its API. UseCase handles business logic only.
- **DIP:** UseCase depends on `RemoteBookDataSource` interface, not providers.
- **OCP:** Third provider = new `RemoteBookDataSource` implementation, no existing code changes.
- **ISP:** `RemoteBookDataSource` is minimal: `searchBooks` + `getBookDescription`.

### DRY Assessment

- Query building is provider-specific (`author:` vs `inauthor:`, different join semantics). Not a DRY violation — it's genuinely different logic.
- Sanitization (`sanitizeFilterInput`) is similar between providers. Could extract to a shared utility, but it's 1 line. Not worth the abstraction.

### TOS Compliance — Detailed Analysis

**"Do not mix Google results with competing search services on same page":**
- **Search results page:** Only shows results from one provider at a time (Google or OL fallback). Not mixed. Compliant.
- **Shelf view:** Shows saved books from potentially both providers. These are **user-saved data**, not search results. The TOS restriction applies to search result display, not user collections. Compliant.
- **Book clubs:** Same as shelf view — user-curated collections. Compliant.

**Required attribution:** "Powered by Google" must appear adjacent to search results. Only needs to show when displaying Google-sourced results. When fallback is active (showing OL results), the Google attribution is not required.

---

## 9. Implementation Order for Next Session

Execute in this order to maintain a compilable project at each step. Tests are interleaved with their corresponding phases.

| Step | Phase | What | Tests |
|------|-------|------|-------|
| 1 | 1 | Domain model changes (Book, enums, BookEntity, BookSearchResponse, DB version) | - |
| 2 | 2 | Update OL BookMappers, BookClub DTOs/mappers | BookMappersTest, test builders |
| 3 | 2 | Remove OL-specific UI (ratings card, numEditions, internetArchiveId) — **separate commit** | - |
| 4 | 3 | Refactor RemoteBookDataSource interface, rename/update OL data source | - |
| 5 | 3 | Update SearchBooksUseCase, BookRepositoryImpl | SearchBooksUseCaseTest, BookRepositoryImplTest |
| 6 | 4 | API key config (build.gradle, ApiConfig) | - |
| 7 | 4 | Google Books DTOs, API service, mapper, data source | GoogleBookMappersTest, GoogleBooksRemoteBookDataSourceTest |
| 8 | 5 | FallbackRemoteBookDataSource | FallbackRemoteBookDataSourceTest |
| 9 | 6 | HttpClientFactory retry policy fix | - |
| 10 | 7 | DI module wiring | - |
| 11 | 8 | BookImageUtils (provider-aware), SafeSearchFilter (maturityRating) | - |
| 12 | 8 | UI: attribution, subtitle, preview/info links — **separate commit from step 3** | - |
| 13 | 9 | Remaining test updates (ViewModel, DAO, integration) | BookDetailViewModelTest, BookshelfDaoTest |

---

## 10. Fast-Follows (Post-Merge)

- **Pagination:** Thread `startIndex`/`offset` through `RemoteBookDataSource` -> API services. `BookSearchResponse.totalResults` already supports this. Separate PR — keep this spike focused on the API swap. Note: `BookApiService` will likely need provider-specific interfaces at that point, since OL uses `offset` and Google uses `startIndex`.
- **PrintType filtering:** `PrintType` enum is added in this spike but not used for filtering. Add magazine filtering to `SafeSearchFilter` or `SearchBooksUseCase` as a user-toggleable option.

---

## 11. Google Books TOS Checklist

- [x] Display "Powered by Google" near search results (only when showing Google results) — `BookSearchDialog.kt`, promoted from list-footer to dialog body so it's visible without scrolling
- [x] Each book result links to Google Books page (via `infoLink`) — `PublicationDetailsCard.kt` via `openExternalUrl` with HTTPS allowlist
- [x] Use only approved terminology: "Google Books", "Google Preview" — verified via grep
- [x] Search results from one provider at a time (fallback, not mixed) — `FallbackRemoteBookDataSource` returns a single provider's result set per call
- [x] Do not charge users without Google's written permission (app is free)
