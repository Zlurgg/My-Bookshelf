package uk.co.zlurgg.mybookshelf.book.data.network

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.data.dto.google.GoogleBookItemDto
import uk.co.zlurgg.mybookshelf.book.data.dto.google.GoogleBooksSearchResponseDto
import uk.co.zlurgg.mybookshelf.book.data.mappers.stripHtml
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBook
import uk.co.zlurgg.mybookshelf.book.data.network.api.GoogleBooksBookApi
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.BookSearchResponse
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.map
import uk.co.zlurgg.mybookshelf.core.domain.service.SystemLanguageProvider

class GoogleBooksRemoteBookDataSource(
    private val apiService: GoogleBooksBookApi,
    private val systemLanguageProvider: SystemLanguageProvider
) : RemoteBookDataSource {

    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String?,
        sort: String?
    ): Result<BookSearchResponse, DataError.Remote> {
        val apiKey = ApiConfig.GoogleBooks.apiKey
        if (apiKey.isBlank()) {
            Timber.tag(TAG).w("Google Books API key is not configured, returning FORBIDDEN")
            return Result.Error(DataError.Remote.FORBIDDEN)
        }

        val finalQuery = buildQuery(query, authorFilter, titleFilter, subjectFilter)
        // Google Books uses ISO 639-1 two-letter codes (e.g. "en"), not OL three-letter codes
        val finalLanguage = language ?: systemLanguageProvider.getRawLanguageCode()

        Timber.tag(TAG).d("=== GOOGLE BOOKS SEARCH ===")
        Timber.tag(TAG).d("Final query: '%s', language: %s", finalQuery, finalLanguage)

        return ErrorMapper.httpNetworkCall<GoogleBooksSearchResponseDto> {
            apiService.searchBooks(
                query = finalQuery,
                resultLimit = resultLimit,
                language = finalLanguage,
                sort = sort
            )
        }.map { dto ->
            Timber.tag(TAG).d("Results: %d total, %d returned", dto.totalItems, dto.items?.size ?: 0)
            BookSearchResponse(
                totalResults = dto.totalItems,
                books = dto.items?.map { it.toBook() } ?: emptyList()
            )
        }
    }

    override suspend fun getBookDescription(
        bookId: String,
        provider: BookProvider
    ): Result<String?, DataError.Remote> {
        return ErrorMapper.httpNetworkCall<GoogleBookItemDto> {
            apiService.getBookDetails(bookId)
        }.map { stripHtml(it.volumeInfo?.description) }
    }

    private fun buildQuery(
        baseQuery: String,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String?
    ): String {
        val parts = mutableListOf<String>()

        if (baseQuery.isNotBlank()) {
            parts.add(sanitizeFilterInput(baseQuery))
        }

        authorFilter?.takeIf { it.isNotBlank() }?.let {
            parts.add(formatFilterField(it, "inauthor"))
        }
        titleFilter?.takeIf { it.isNotBlank() }?.let {
            parts.add(formatFilterField(it, "intitle"))
        }
        subjectFilter?.takeIf { it.isNotBlank() }?.let {
            parts.add(formatFilterField(it, "subject"))
        }

        return parts.joinToString(" ")
    }

    private fun sanitizeFilterInput(input: String): String = input.trim().replace("\"", "")

    private fun formatFilterField(raw: String, prefix: String): String {
        val sanitized = sanitizeFilterInput(raw)
        val quoted = if (sanitized.contains(" ")) "\"$sanitized\"" else sanitized
        return "$prefix:$quoted"
    }

    companion object {
        private const val TAG = "GoogleBooksSearch"
    }
}
