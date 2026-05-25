package uk.co.zlurgg.mybookshelf.book.data.network

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.book.data.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.book.data.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBook
import uk.co.zlurgg.mybookshelf.book.data.network.api.OpenLibraryBookApi
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.BookSearchResponse
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.map
import uk.co.zlurgg.mybookshelf.core.domain.service.SystemLanguageProvider

class OpenLibraryRemoteBookDataSource(
    private val apiService: OpenLibraryBookApi,
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
        val finalQuery = buildQuery(query, authorFilter, titleFilter, subjectFilter)
        val finalLanguage = language ?: systemLanguageProvider.getCurrentLanguageCode()

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
                resultLimit = resultLimit,
                language = finalLanguage,
                sort = sort
            )
        }.map { dto ->
            Timber.tag(TAG).d("Results: %d total, %d returned", dto.numFound, dto.results.size)
            BookSearchResponse(
                books = dto.results.map { it.toBook() }
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

    private fun sanitizeFilterInput(input: String): String = input.trim().replace("\"", "")

    private fun formatFilterField(raw: String, prefix: String): String {
        val sanitized = sanitizeFilterInput(raw)
        val quoted = if (sanitized.contains(" ")) "\"$sanitized\"" else sanitized
        return "$prefix:$quoted"
    }

    private fun buildQuery(
        baseQuery: String,
        authorFilter: String?,
        titleFilter: String?,
        subjectFilter: String? = null
    ): String {
        val queryParts = mutableListOf<String>()

        if (baseQuery.isNotBlank()) {
            queryParts.add(sanitizeFilterInput(baseQuery))
        }

        authorFilter?.takeIf { it.isNotBlank() }?.let {
            queryParts.add(formatFilterField(it, prefix = "author"))
        }

        titleFilter?.takeIf { it.isNotBlank() }?.let {
            queryParts.add(formatFilterField(it, prefix = "title"))
        }

        subjectFilter?.takeIf { it.isNotBlank() }?.let {
            queryParts.add(formatFilterField(it, prefix = "subject"))
        }

        return queryParts.joinToString(" ")
    }

    companion object {
        private const val TAG = "OpenLibrarySearch"
    }
}
