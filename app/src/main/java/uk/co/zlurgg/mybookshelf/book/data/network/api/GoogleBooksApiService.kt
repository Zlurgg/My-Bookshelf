package uk.co.zlurgg.mybookshelf.book.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig

class GoogleBooksApiService(
    private val httpClient: HttpClient,
    // Injection seam for tests. Production wires the BuildConfig-backed value.
    private val apiKeyProvider: () -> String = { ApiConfig.GoogleBooks.apiKey },
) : GoogleBooksBookApi {

    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        sort: String?
    ): HttpResponse {
        return httpClient.get(ApiConfig.GoogleBooks.searchEndpoint) {
            // API key is sent as a header (not ?key=) to keep it out of request logs.
            header(GOOGLE_API_KEY_HEADER, apiKeyProvider())
            parameter("q", query)
            parameter("maxResults", resultLimit ?: ApiConfig.GoogleBooks.DefaultParams.MAX_RESULTS)
            parameter("printType", ApiConfig.GoogleBooks.DefaultParams.PRINT_TYPE_BOOKS)
            language?.let { parameter("langRestrict", it) }
            sort?.let { parameter("orderBy", it) }
        }
    }

    override suspend fun getBookDetails(bookId: String): HttpResponse {
        return httpClient.get(ApiConfig.GoogleBooks.volumeEndpoint(bookId)) {
            header(GOOGLE_API_KEY_HEADER, apiKeyProvider())
        }
    }

    private companion object {
        const val GOOGLE_API_KEY_HEADER = "X-Goog-Api-Key"
    }
}
