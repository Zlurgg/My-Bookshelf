package uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.api

import io.ktor.client.HttpClient

/**
 * Factory for creating book API service providers.
 * Currently uses OpenLibrary as the book data source.
 *
 * Located in book domain as it's specific to book API operations.
 */
class BookApiProviderFactory(
    private val httpClient: HttpClient
) {

    /**
     * Creates the book API service.
     * Returns OpenLibrary API service.
     */
    fun createBookApiService(): BookApiService {
        return OpenLibraryApiService(httpClient)
    }

    /**
     * Creates a specific OpenLibrary API service.
     * Useful for dependency injection or when you specifically need OpenLibrary.
     */
    fun createOpenLibraryApi(): OpenLibraryBookApi {
        return OpenLibraryApiService(httpClient)
    }

    companion object {
        /**
         * Quick factory method for getting the book API service.
         */
        fun getCurrentBookApi(httpClient: HttpClient): BookApiService {
            return BookApiProviderFactory(httpClient).createBookApiService()
        }
    }
}