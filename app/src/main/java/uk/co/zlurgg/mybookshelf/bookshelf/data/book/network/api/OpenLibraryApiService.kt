package uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig

/**
 * Open Library specific implementation of book API service.
 * Handles Open Library API interactions with proper URL construction and parameter handling.
 * Located in book domain as it's specific to book operations and OpenLibrary provider.
 */
class OpenLibraryApiService(
    private val httpClient: HttpClient,
) : OpenLibraryBookApi {
    companion object {
        private const val TAG = "BookSearch"
    }

    /**
     * Search for books using the Open Library search API.
     * Handles proper parameter construction and URL building.
     */
    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        sort: String?,
    ): HttpResponse {
        val endpoint = ApiConfig.OpenLibrary.searchEndpoint

        // Build URL preview for logging
        val params =
            buildString {
                append("?q=$query")
                resultLimit?.let { append("&limit=$it") }
                language?.let { append("&language=$it") }
                sort?.let { append("&sort=$it") }
                append("&fields=${ApiConfig.OpenLibrary.DefaultParams.SEARCH_FIELDS}")
            }

        Timber.tag(TAG).d("=== HTTP REQUEST ===")
        Timber.tag(TAG).d("Endpoint: %s", endpoint)
        Timber.tag(TAG).d("Full URL (preview): %s%s", endpoint, params)

        val response =
            httpClient.get(endpoint) {
                parameter("q", query)
                resultLimit?.let { parameter("limit", it) }
                language?.let { parameter("language", it) }
                sort?.let { parameter("sort", it) }
                parameter("fields", ApiConfig.OpenLibrary.DefaultParams.SEARCH_FIELDS)
            }

        Timber.tag(TAG).d("Response status: %d", response.status.value)

        return response
    }

    /**
     * Get detailed information about a specific book work.
     */
    override suspend fun getBookDetails(bookId: String): HttpResponse {
        return httpClient.get(ApiConfig.OpenLibrary.workDetailsEndpoint(bookId))
    }
}
