package uk.co.zlurgg.mybookshelf.book.domain.repository

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface BookRepository {
    // Book CRUD operations
    suspend fun getBookById(bookId: String): Result<Book?, DataError.Local>
    suspend fun upsertBook(book: Book): Result<Unit, DataError.Local>
    suspend fun deleteBook(bookId: String): Result<Unit, DataError.Local>

    // Book metadata operations
    suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote>

    /**
     * Upserts a system book (e.g., tutorial book) with SystemOwnerIds.TUTORIAL as owner.
     * System books are visible to all users and not synced to cloud.
     */
    suspend fun upsertSystemBook(book: Book): Result<Unit, DataError.Local>
}
