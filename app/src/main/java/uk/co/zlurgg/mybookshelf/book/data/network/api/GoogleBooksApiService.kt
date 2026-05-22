package uk.co.zlurgg.mybookshelf.book.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig

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
            parameter("printType", "books")
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
