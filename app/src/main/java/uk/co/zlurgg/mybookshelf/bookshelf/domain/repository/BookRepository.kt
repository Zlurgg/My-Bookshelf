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

    /**
     * Upserts a system book (e.g., tutorial book) with SystemOwnerIds.TUTORIAL as owner.
     * System books are visible to all users and not synced to cloud.
     */
    suspend fun upsertSystemBook(book: Book)
}
