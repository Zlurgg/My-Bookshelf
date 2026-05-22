package uk.co.zlurgg.mybookshelf.book.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface BookRepository {
    // Book CRUD operations
    suspend fun getBookById(bookId: String): Result<Book?, DataError.Local>
    suspend fun upsertBook(book: Book): Result<Unit, DataError.Local>

    // Book metadata operations
    suspend fun getBookDescription(bookId: String, provider: BookProvider): Result<String?, DataError.Remote>

    /**
     * Upserts a system book (e.g., tutorial book) with SystemOwnerIds.TUTORIAL as owner.
     * System books are visible to all users and not synced to cloud.
     */
    suspend fun upsertSystemBook(book: Book): Result<Unit, DataError.Local>

    // Library
    fun getAllPersonalBooks(): Flow<List<Book>>

    // Deletion
    suspend fun deleteBooks(bookIds: List<String>): Result<Unit, DataError.Local>
    fun getNonRemovableBookIds(): Flow<Set<String>>
}
