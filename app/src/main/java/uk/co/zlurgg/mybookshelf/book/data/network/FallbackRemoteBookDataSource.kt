package uk.co.zlurgg.mybookshelf.book.data.network

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.BookSearchResponse
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Google Books first, Open Library on page 1 when Google can't answer.
 *
 * Falls back on quota/auth/unavailable errors and on 5xx: `HttpClientFactory`
 * already retries 5xx, but retries don't cover multi-day `backendFailed`
 * outages like Google's in Oct 2026. Also falls back when Google returns an
 * empty first page, since the same outage produced empty 200s for queries that
 * obviously match. Connectivity errors don't fall back: OL won't do better.
 */
class FallbackRemoteBookDataSource(
    private val primary: RemoteBookDataSource,
    private val fallback: RemoteBookDataSource,
) : RemoteBookDataSource {

    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String?,
        sort: String?,
        startIndex: Int?,
    ): Result<BookSearchResponse, DataError.Remote> {
        val result = primary.searchBooks(
            query = query,
            resultLimit = resultLimit,
            language = language,
            authorFilter = authorFilter,
            titleFilter = titleFilter,
            subjectFilter = subjectFilter,
            sort = sort,
            startIndex = startIndex,
        )

        // Provider-switch mid-pagination would interleave Google rows 0..N with
        // OL rows from offset=N+1 of a totally different result set — there's no
        // sane way to merge them. Surface the error instead so the user can
        // retry or refine the query. Fresh searches (page 1) keep falling back.
        if (startIndex != null) return result

        return when {
            result is Result.Error && shouldFallback(result.error) -> {
                Timber.tag(TAG).w(
                    "Google Books unavailable (%s), falling back to OpenLibrary",
                    result.error
                )
                searchFallback(query, resultLimit, language, authorFilter, titleFilter, subjectFilter, sort)
            }
            result is Result.Success && result.data.rawPageSize == 0 -> {
                Timber.tag(TAG).w("Google Books returned no results, trying OpenLibrary")
                val fallbackResult =
                    searchFallback(query, resultLimit, language, authorFilter, titleFilter, subjectFilter, sort)
                when (fallbackResult) {
                    is Result.Success -> fallbackResult
                    // An empty Google page is a legitimate answer; don't turn it
                    // into an error screen because the fallback also failed.
                    is Result.Error -> result
                }
            }
            else -> result
        }
    }

    private suspend fun searchFallback(
        query: String,
        resultLimit: Int?,
        language: String?,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String?,
        sort: String?,
    ): Result<BookSearchResponse, DataError.Remote> = fallback.searchBooks(
        query = query,
        resultLimit = resultLimit,
        language = language,
        authorFilter = authorFilter,
        titleFilter = titleFilter,
        subjectFilter = subjectFilter,
        sort = sort,
        startIndex = null,
    )

    override suspend fun getBookDescription(
        bookId: String,
        provider: BookProvider
    ): Result<String?, DataError.Remote> {
        return when (provider) {
            BookProvider.GOOGLE_BOOKS -> primary.getBookDescription(bookId, provider)
            BookProvider.OPEN_LIBRARY -> fallback.getBookDescription(bookId, provider)
        }
    }

    private fun shouldFallback(error: DataError.Remote): Boolean {
        return error == DataError.Remote.TOO_MANY_REQUESTS ||
            error == DataError.Remote.FORBIDDEN ||
            error == DataError.Remote.PROVIDER_UNAVAILABLE ||
            error == DataError.Remote.SERVER_ERROR
    }

    companion object {
        private const val TAG = "BookSearchFallback"
    }
}
