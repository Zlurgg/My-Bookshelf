package uk.co.zlurgg.mybookshelf.bookshelf.data.book.network

import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.core.domain.service.SystemLanguageProvider
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.api.OpenLibraryBookApi

class KtorRemoteBookDataSource(
    private val apiService: OpenLibraryBookApi,
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
        return ErrorMapper.httpNetworkCall<SearchResponseDto> {
            // Build query with field-specific filters
            val finalQuery = buildQuery(query, authorFilter, titleFilter)

            apiService.searchBooks(
                query = finalQuery,
                resultLimit = resultLimit,
                language = language ?: systemLanguageProvider.getCurrentLanguageCode(),
                sort = sort
            )
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
        return ErrorMapper.httpNetworkCall<BookWorkDto> {
            apiService.getBookDetails(bookWorkId)
        }
    }
}