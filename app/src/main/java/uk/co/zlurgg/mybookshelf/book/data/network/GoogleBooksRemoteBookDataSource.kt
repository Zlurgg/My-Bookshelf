package uk.co.zlurgg.mybookshelf.book.data.network

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.data.dto.google.GoogleBookItemDto
import uk.co.zlurgg.mybookshelf.book.data.dto.google.GoogleBooksSearchResponseDto
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBook
import uk.co.zlurgg.mybookshelf.book.data.mappers.toDescription
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
    private val systemLanguageProvider: SystemLanguageProvider,
    // Injection seam for tests. Production wires the BuildConfig-backed value.
    private val apiKeyProvider: () -> String = { ApiConfig.GoogleBooks.apiKey },
) : RemoteBookDataSource {

    private val queryBuilder = BookSearchQueryBuilder(GOOGLE_BOOKS_PREFIXES)

    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String?,
        sort: String?
    ): Result<BookSearchResponse, DataError.Remote> {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            Timber.tag(TAG).e("Google Books API key is not configured")
            return Result.Error(DataError.Remote.PROVIDER_UNAVAILABLE)
        }

        val finalQuery = queryBuilder.build(query, authorFilter, titleFilter, subjectFilter)
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
        }.map { it.toDescription() }
    }

    companion object {
        private const val TAG = "GoogleBooksSearch"

        private val GOOGLE_BOOKS_PREFIXES = mapOf(
            BookSearchQueryBuilder.FilterField.AUTHOR to "inauthor",
            BookSearchQueryBuilder.FilterField.TITLE to "intitle",
            BookSearchQueryBuilder.FilterField.SUBJECT to "subject",
        )
    }
}
