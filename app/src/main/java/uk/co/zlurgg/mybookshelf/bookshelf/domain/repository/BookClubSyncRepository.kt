package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Book sync operations: get club books, sync to/from club.
 */
interface BookClubSyncRepository {
    suspend fun getClubBooks(code: String): Result<List<Book>, DataError.Sync>
    suspend fun syncBookToClub(code: String, book: Book): Result<Unit, DataError.Sync>
    suspend fun removeBookFromClub(code: String, bookId: String): Result<Unit, DataError.Sync>
    suspend fun syncBooksFromClub(code: String, localShelfId: String): Result<SyncResult, DataError.Sync>
}
