package uk.co.zlurgg.mybookshelf.book.data.network

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.data.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.book.data.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.book.data.network.api.OpenLibraryBookApi
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.SystemLanguageProvider

class KtorRemoteBookDataSource(
    private val apiService: OpenLibraryBookApi,
    private val systemLanguageProvider: SystemLanguageProvider
) : RemoteBookDataSource {

    companion object {
        private const val TAG = "BookSearch"
    }

    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String?,
        sort: String?
    ): Result<SearchResponseDto, DataError.Remote> {
        // Build query with field-specific filters
        val finalQuery = buildQuery(query, authorFilter, titleFilter, subjectFilter)
        val finalLanguage = language ?: systemLanguageProvider.getCurrentLanguageCode()

        Timber.tag(TAG).d("=== SEARCH REQUEST ===")
        Timber.tag(TAG).d(
            "Raw inputs - query: '%s', author: '%s', title: '%s', subject: '%s'",
            query,
            authorFilter,
            titleFilter,
            subjectFilter
        )
        Timber.tag(TAG).d("Final query: '$finalQuery'")
        Timber.tag(TAG).d("Parameters - limit: %s, language: %s, sort: %s", resultLimit, finalLanguage, sort)

        val result = ErrorMapper.httpNetworkCall<SearchResponseDto> {
            apiService.searchBooks(
                query = finalQuery,
                resultLimit = resultLimit,
                language = finalLanguage,
                sort = sort
            )
        }

        // Log the response
        when (result) {
            is Result.Success -> {
                Timber.tag(TAG).d("=== SEARCH RESPONSE: SUCCESS ===")
                Timber.tag(TAG).d("Total results found by API: %d", result.data.numFound)
                Timber.tag(TAG).d("Results returned in response: %d", result.data.results.size)
                if (result.data.results.isNotEmpty()) {
                    Timber.tag(TAG).d("First result title: %s", result.data.results.first().title)
                } else {
                    Timber.tag(TAG).w("WARNING: No results returned for query: '$finalQuery'")
                }
            }
            is Result.Error -> {
                Timber.tag(TAG).e("=== SEARCH RESPONSE: ERROR ===")
                Timber.tag(TAG).e("Error type: %s", result.error)
                Timber.tag(TAG).e("Query that failed: '$finalQuery'")

                // Provide hints for common errors
                when (result.error) {
                    DataError.Remote.REQUEST_TIMEOUT -> {
                        Timber.tag(TAG).e("Timeout after 20 seconds (configured in BuildConfig)")
                        Timber.tag(TAG).e("This query may be too slow for OpenLibrary API")
                    }
                    DataError.Remote.UNKNOWN -> {
                        Timber.tag(TAG).e("UNKNOWN error - could be:")
                        Timber.tag(TAG).e("  - Timeout after retries (~30s total)")
                        Timber.tag(TAG).e("  - IOException from network layer")
                        Timber.tag(TAG).e("  - Unrecognized exception type")
                        Timber.tag(TAG).e("Check if OpenLibrary API is down or slow")
                    }
                    DataError.Remote.NO_INTERNET -> {
                        Timber.tag(TAG).e("No internet connection detected")
                    }
                    DataError.Remote.SERVER_ERROR -> {
                        Timber.tag(TAG).e("OpenLibrary server returned 5xx error")
                    }
                    else -> {
                        Timber.tag(TAG).e("See ErrorMapper for error type details")
                    }
                }
            }
        }

        return result
    }

    private fun sanitizeFilterInput(input: String): String = input.trim().replace("\"", "")

    private fun formatFilterField(
        raw: String,
        fieldName: String,
        prefix: String? = null
    ): String {
        val sanitized = sanitizeFilterInput(raw)
        val quoted = if (sanitized.contains(" ")) "\"$sanitized\"" else sanitized
        val result = if (prefix != null) "$prefix:$quoted" else quoted
        Timber.tag(TAG).d(
            "Query construction - %s: '%s' → '%s' (multi-word: %b)",
            fieldName,
            raw,
            result,
            sanitized.contains(" ")
        )
        return result
    }

    private fun buildQuery(
        baseQuery: String,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String? = null
    ): String {
        val queryParts = mutableListOf<String>()

        if (baseQuery.isNotBlank()) {
            queryParts.add(formatFilterField(baseQuery, "base"))
        }

        authorFilter?.takeIf { it.isNotBlank() }?.let {
            queryParts.add(formatFilterField(it, "author", prefix = "author"))
        }

        titleFilter?.takeIf { it.isNotBlank() }?.let {
            queryParts.add(formatFilterField(it, "title", prefix = "title"))
        }

        subjectFilter?.takeIf { it.isNotBlank() }?.let {
            queryParts.add(formatFilterField(it, "subject", prefix = "subject"))
        }

        // Join with spaces (Open Library treats multiple terms as AND)
        return queryParts.joinToString(" ")
    }

    override suspend fun getBookDetails(bookWorkId: String): Result<BookWorkDto, DataError.Remote> {
        return ErrorMapper.httpNetworkCall<BookWorkDto> {
            apiService.getBookDetails(bookWorkId)
        }
    }
}
