package uk.co.zlurgg.mybookshelf.bookshelf.data.book.network

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.api.OpenLibraryBookApi
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
        sort: String?
    ): Result<SearchResponseDto, DataError.Remote> {
        // Build query with field-specific filters
        val finalQuery = buildQuery(query, authorFilter, titleFilter)
        val finalLanguage = language ?: systemLanguageProvider.getCurrentLanguageCode()

        Timber.tag(TAG).d("=== SEARCH REQUEST ===")
        Timber.tag(TAG).d("Raw inputs - query: '$query', author: '$authorFilter', title: '$titleFilter'")
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

    private fun buildQuery(
        baseQuery: String,
        authorFilter: String?,
        titleFilter: String?
    ): String {
        val queryParts = mutableListOf<String>()

        // Add base query with smart quoting for exact phrase matching
        if (baseQuery.isNotBlank()) {
            val trimmed = baseQuery.trim()
            // Multi-word queries get quotes for exact phrase matching
            val formatted = if (trimmed.contains(" ")) "\"$trimmed\"" else trimmed
            Timber.tag(
                TAG
            ).d("Query construction - base: '%s' → '%s' (multi-word: %b)", baseQuery, formatted, trimmed.contains(" "))
            queryParts.add(formatted)
        }

        // Add author filter using Open Library field syntax with smart quoting
        authorFilter?.takeIf { it.isNotBlank() }?.let {
            val trimmed = it.trim()
            val formatted = if (trimmed.contains(" ")) "\"$trimmed\"" else trimmed
            val fieldQuery = "author:$formatted"
            Timber.tag(
                TAG
            ).d("Query construction - author: '%s' → '%s' (multi-word: %b)", it, fieldQuery, trimmed.contains(" "))
            queryParts.add(fieldQuery)
        }

        // Add title filter using Open Library field syntax with smart quoting
        titleFilter?.takeIf { it.isNotBlank() }?.let {
            val trimmed = it.trim()
            val formatted = if (trimmed.contains(" ")) "\"$trimmed\"" else trimmed
            val fieldQuery = "title:$formatted"
            Timber.tag(
                TAG
            ).d("Query construction - title: '%s' → '%s' (multi-word: %b)", it, fieldQuery, trimmed.contains(" "))
            queryParts.add(fieldQuery)
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
