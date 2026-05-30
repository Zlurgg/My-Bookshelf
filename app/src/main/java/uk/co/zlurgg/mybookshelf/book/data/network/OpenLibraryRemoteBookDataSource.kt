package uk.co.zlurgg.mybookshelf.book.data.network

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.data.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.book.data.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBook
import uk.co.zlurgg.mybookshelf.book.data.network.api.OpenLibraryBookApi
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.BookSearchResponse
import uk.co.zlurgg.mybookshelf.core.data.network.ApiConfig
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.map
import uk.co.zlurgg.mybookshelf.core.domain.service.SystemLanguageProvider

class OpenLibraryRemoteBookDataSource(
    private val apiService: OpenLibraryBookApi,
    private val systemLanguageProvider: SystemLanguageProvider
) : RemoteBookDataSource {

    private val queryBuilder = BookSearchQueryBuilder(OPEN_LIBRARY_PREFIXES)

    override suspend fun searchBooks(
        query: String,
        resultLimit: Int?,
        language: String?,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String?,
        sort: String?,
        startIndex: Int?,
    ): Result<BookSearchResponse, DataError.Remote> {
        val finalQuery = queryBuilder.build(query, authorFilter, titleFilter, subjectFilter)
        val finalLanguage = language ?: systemLanguageProvider.getCurrentLanguageCode()
        // Resolve null → server-side default (100) here so the API service stays
        // a thin pass-through and `BookSearchResponse.pageSize` is deterministic.
        val requestedLimit = resultLimit ?: ApiConfig.OpenLibrary.DefaultParams.MAX_RESULTS

        Timber.tag(TAG).d("=== OPEN LIBRARY SEARCH ===")
        Timber.tag(TAG).d(
            "Raw inputs - query: '%s', author: '%s', title: '%s', subject: '%s'",
            query,
            authorFilter,
            titleFilter,
            subjectFilter
        )
        Timber.tag(TAG).d("Final query: '%s', language: %s", finalQuery, finalLanguage)

        return ErrorMapper.httpNetworkCall<SearchResponseDto> {
            apiService.searchBooks(
                query = finalQuery,
                resultLimit = requestedLimit,
                language = finalLanguage,
                sort = sort,
                startIndex = startIndex,
            )
        }.map { dto ->
            Timber.tag(TAG).d("Results: %d total, %d returned", dto.numFound, dto.results.size)
            BookSearchResponse(
                // OL doesn't post-filter, so rawPageSize == books.size.
                books = dto.results.map { it.toBook() },
                rawPageSize = dto.results.size,
                pageSize = requestedLimit,
            )
        }
    }

    override suspend fun getBookDescription(
        bookId: String,
        provider: BookProvider
    ): Result<String?, DataError.Remote> {
        return ErrorMapper.httpNetworkCall<BookWorkDto> {
            apiService.getBookDetails(bookId)
        }.map { it.description }
    }

    companion object {
        private const val TAG = "OpenLibrarySearch"

        private val OPEN_LIBRARY_PREFIXES = mapOf(
            BookSearchQueryBuilder.FilterField.AUTHOR to "author",
            BookSearchQueryBuilder.FilterField.TITLE to "title",
            BookSearchQueryBuilder.FilterField.SUBJECT to "subject",
        )
    }
}
