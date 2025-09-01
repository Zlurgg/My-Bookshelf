package uk.co.zlurgg.mybookshelf.bookshelf.data.book.network

import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.BookWorkDto
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.SearchResponseDto
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

interface RemoteBookDataSource {
    suspend fun searchBooks(
        query: String,
        resultLimit: Int? = null
    ): Result<SearchResponseDto, DataError.Remote>

    suspend fun getBookDetails(bookWorkId: String): Result<BookWorkDto, DataError.Remote>
}