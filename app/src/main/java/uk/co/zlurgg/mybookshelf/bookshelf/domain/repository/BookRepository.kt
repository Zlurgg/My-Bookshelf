package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface BookRepository {
    // Book CRUD operations
    suspend fun getBookById(bookId: String): Book?
    suspend fun upsertBook(book: Book)
    suspend fun deleteBook(bookId: String)

    // Book metadata operations
    suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote>
}