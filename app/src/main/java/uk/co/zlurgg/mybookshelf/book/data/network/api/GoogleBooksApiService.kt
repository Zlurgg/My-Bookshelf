package uk.co.zlurgg.mybookshelf.book.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig
import uk.co.zlurgg.mybookshelf.core.data.service.AndroidAppAttestation

class GoogleBooksApiService(
    private val httpClient: HttpClient,
    private val attestation: AndroidAppAttestation,
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
            attachCredentials()
            parameter("q", query)
            parameter("maxResults", resultLimit ?: ApiConfig.GoogleBooks.DefaultParams.MAX_RESULTS)
            parameter("printType", ApiConfig.GoogleBooks.DefaultParams.PRINT_TYPE_BOOKS)
            language?.let { parameter("langRestrict", it) }
            sort?.let { parameter("orderBy", it) }
        }
    }

    override suspend fun getBookDetails(bookId: String): HttpResponse {
        return httpClient.get(ApiConfig.GoogleBooks.volumeEndpoint(bookId)) {
            attachCredentials()
        }
    }

    /**
     * API key + Android-app attestation. Both `X-Android-Package` and
     * `X-Android-Cert` are required when the key has an Android-app
     * Application restriction — without them every request 403s.
     */
    private fun HttpRequestBuilder.attachCredentials() {
        header(GOOGLE_API_KEY_HEADER, apiKeyProvider())
        header(ANDROID_PACKAGE_HEADER, attestation.packageName)
        header(ANDROID_CERT_HEADER, attestation.signingCertSha1Hex)
    }

    private companion object {
        const val GOOGLE_API_KEY_HEADER = "X-Goog-Api-Key"
        const val ANDROID_PACKAGE_HEADER = "X-Android-Package"
        const val ANDROID_CERT_HEADER = "X-Android-Cert"
    }
}
