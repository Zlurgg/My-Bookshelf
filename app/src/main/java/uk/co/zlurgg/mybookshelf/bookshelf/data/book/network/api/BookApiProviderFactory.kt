package uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.api

import io.ktor.client.HttpClient
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig

/**
 * Factory for creating book API service providers based on configuration.
 * Enables easy switching between different book API providers (OpenLibrary, GoogleBooks, etc.)
 * and supports fallback mechanisms for production resilience.
 *
 * Located in book domain as it's specific to book API operations.
 */
class BookApiProviderFactory(
    private val httpClient: HttpClient
) {

    /**
     * Creates the appropriate book API service based on the current configuration.
     * Defaults to OpenLibrary if the configured provider is not available.
     */
    fun createBookApiService(): BookApiService {
        return when (ApiConfig.getCurrentProvider()) {
            ApiConfig.BookApiProvider.OPEN_LIBRARY -> OpenLibraryApiService(httpClient)
            ApiConfig.BookApiProvider.GOOGLE_BOOKS -> {
                // GoogleBooksApiService would be implemented here
                // For now, fallback to OpenLibrary
                OpenLibraryApiService(httpClient)
            }
        }
    }

    /**
     * Creates a specific OpenLibrary API service.
     * Useful for dependency injection or when you specifically need OpenLibrary.
     */
    fun createOpenLibraryApi(): OpenLibraryBookApi {
        return OpenLibraryApiService(httpClient)
    }

    /**
     * Future: Creates a specific GoogleBooks API service.
     * Placeholder for when GoogleBooks integration is implemented.
     */
    fun createGoogleBooksApi(): GoogleBooksBookApi? {
        // TODO: Implement GoogleBooksApiService
        // return GoogleBooksApiService(httpClient)
        return null
    }

    companion object {
        /**
         * Quick factory method for getting the current configured book API service.
         */
        fun getCurrentBookApi(httpClient: HttpClient): BookApiService {
            return BookApiProviderFactory(httpClient).createBookApiService()
        }
    }
}