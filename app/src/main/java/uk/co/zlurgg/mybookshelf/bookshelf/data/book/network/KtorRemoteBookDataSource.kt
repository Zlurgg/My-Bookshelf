package uk.co.zlurgg.mybookshelf.bookshelf.data.book.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.core.domain.service.SystemLanguageProvider
import uk.co.zlurgg.mybookshelf.core.data.network.safeCall
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

private const val BASE_URL = "https://openlibrary.org"

class KtorRemoteBookDataSource(
    private val httpClient: HttpClient,
    private val systemLanguageProvider: SystemLanguageProvider
): RemoteBookDataSource {

    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        authorFilter: String?,
        titleFilter: String?,
        sort: String?
    ): Result<SearchResponseDto, DataError.Remote> {
        return safeCall<SearchResponseDto> {
            httpClient.get(
                urlString = "$BASE_URL/search.json"
            ) {
                // Build query with field-specific filters
                val finalQuery = buildQuery(query, authorFilter, titleFilter)
                parameter("q", finalQuery)

                parameter("limit", resultLimit)
                parameter("language", language ?: systemLanguageProvider.getCurrentLanguageCode())

                // Add server-side sort if specified
                sort?.let { parameter("sort", it) }

                parameter("fields", "key,title,author_name,author_key,cover_edition_key,cover_i,ratings_average,ratings_count,first_publish_year,language,number_of_pages_median,edition_count")
            }
        }
    }

    private fun buildQuery(
        baseQuery: String,
        authorFilter: String?,
        titleFilter: String?
    ): String {
        val queryParts = mutableListOf<String>()

        // Add base query if provided
        if (baseQuery.isNotBlank()) {
            queryParts.add(baseQuery.trim())
        }

        // Add author filter using Open Library field syntax
        authorFilter?.takeIf { it.isNotBlank() }?.let {
            queryParts.add("author:${it.trim()}")
        }

        // Add title filter using Open Library field syntax
        titleFilter?.takeIf { it.isNotBlank() }?.let {
            queryParts.add("title:${it.trim()}")
        }

        // Join with spaces (Open Library treats multiple terms as AND)
        return queryParts.joinToString(" ").ifBlank { "*" }
    }

    override suspend fun getBookDetails(bookWorkId: String): Result<BookWorkDto, DataError.Remote> {
        return safeCall<BookWorkDto> {
            httpClient.get(
                urlString = "$BASE_URL/works/$bookWorkId.json"
            )
        }
    }
}