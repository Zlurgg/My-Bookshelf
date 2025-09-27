package uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig

/**
 * Open Library specific implementation of book API service.
 * Handles Open Library API interactions with proper URL construction and parameter handling.
 * Located in book domain as it's specific to book operations and OpenLibrary provider.
 */
class OpenLibraryApiService(
    private val httpClient: HttpClient
) : OpenLibraryBookApi {

    /**
     * Search for books using the Open Library search API.
     * Handles proper parameter construction and URL building.
     */
    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        sort: String?
    ): HttpResponse {
        return httpClient.get(ApiConfig.OpenLibrary.searchEndpoint) {
            parameter("q", query)
            resultLimit?.let { parameter("limit", it) }
            language?.let { parameter("language", it) }
            sort?.let { parameter("sort", it) }
            parameter("fields", ApiConfig.OpenLibrary.DefaultParams.SEARCH_FIELDS)
        }
    }

    /**
     * Get detailed information about a specific book work.
     */
    override suspend fun getBookDetails(bookId: String): HttpResponse {
        return httpClient.get(ApiConfig.OpenLibrary.workDetailsEndpoint(bookId))
    }
}