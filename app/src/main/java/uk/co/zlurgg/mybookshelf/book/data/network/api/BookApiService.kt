package uk.co.zlurgg.mybookshelf.book.data.network.api

import io.ktor.client.statement.HttpResponse

/**
 * Generic interface for book API service implementations.
 * Provides a contract for book API providers.
 */
interface BookApiService {

    /**
     * Search for books across the API provider.
     * @param query The search query
     * @param resultLimit Maximum number of results to return
     * @param language Language code for filtering results
     * @param sort Sort order for results
     * @param startIndex Zero-based offset into the provider's unfiltered result
     *   stream for pagination. `null` means the provider's default starting
     *   page. Google maps this to `startIndex`; OpenLibrary maps to `offset`.
     * @return HTTP response containing search results
     */
    suspend fun searchBooks(
        query: String,
        resultLimit: Int? = null,
        language: String? = null,
        sort: String? = null,
        startIndex: Int? = null,
    ): HttpResponse

    /**
     * Get detailed information about a specific book.
     * @param bookId The unique identifier for the book in this API provider
     * @return HTTP response containing book details
     */
    suspend fun getBookDetails(bookId: String): HttpResponse
}

/**
 * Marker interface to identify Open Library specific API services.
 * Useful for dependency injection and provider-specific configurations.
 */
interface OpenLibraryBookApi : BookApiService

/**
 * Marker interface to identify Google Books specific API services.
 */
interface GoogleBooksBookApi : BookApiService
