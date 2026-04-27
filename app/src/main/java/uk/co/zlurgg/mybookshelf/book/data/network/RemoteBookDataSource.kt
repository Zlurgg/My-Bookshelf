package uk.co.zlurgg.mybookshelf.book.data.network

import uk.co.zlurgg.mybookshelf.book.data.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.book.data.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface RemoteBookDataSource {
    suspend fun searchBooks(
        query: String,
        resultLimit: Int? = null,
        language: String? = null,
        authorFilter: String? = null,
        titleFilter: String? = null,
        sort: String? = null
    ): Result<SearchResponseDto, DataError.Remote>

    suspend fun getBookDetails(bookWorkId: String): Result<BookWorkDto, DataError.Remote>
}
