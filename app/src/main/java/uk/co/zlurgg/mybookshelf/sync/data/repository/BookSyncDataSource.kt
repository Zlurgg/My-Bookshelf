package uk.co.zlurgg.mybookshelf.sync.data.repository

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookFirestoreDto

/**
 * Remote book sync operations: upload, download, delete.
 */
interface BookSyncDataSource {
    suspend fun uploadBook(userId: String, book: BookFirestoreDto): Result<Unit, DataError.Sync>
    suspend fun downloadBook(userId: String, bookId: String): Result<BookFirestoreDto?, DataError.Sync>
    suspend fun downloadBooksSince(userId: String, sinceTimestamp: Long): Result<List<BookFirestoreDto>, DataError.Sync>
    suspend fun deleteBook(userId: String, bookId: String): Result<Unit, DataError.Sync>
    suspend fun uploadBooks(userId: String, books: List<BookFirestoreDto>): Result<Int, DataError.Sync>
}
