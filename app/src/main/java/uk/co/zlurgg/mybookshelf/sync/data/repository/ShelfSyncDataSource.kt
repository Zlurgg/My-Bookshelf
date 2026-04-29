package uk.co.zlurgg.mybookshelf.sync.data.repository

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.sync.data.dto.BookshelfFirestoreDto
import uk.co.zlurgg.mybookshelf.sync.data.dto.SharedShelfDto

/**
 * Remote shelf sync operations: upload, download, delete, share.
 */
interface ShelfSyncDataSource {
    suspend fun uploadBookshelf(userId: String, shelf: BookshelfFirestoreDto): Result<Unit, DataError.Sync>
    suspend fun downloadBookshelf(userId: String, shelfId: String): Result<BookshelfFirestoreDto?, DataError.Sync>
    suspend fun downloadBookshelvesSince(
        userId: String,
        sinceTimestamp: Long
    ): Result<List<BookshelfFirestoreDto>, DataError.Sync>
    suspend fun deleteBookshelf(userId: String, shelfId: String): Result<Unit, DataError.Sync>
    suspend fun uploadBookshelves(userId: String, shelves: List<BookshelfFirestoreDto>): Result<Int, DataError.Sync>
    suspend fun shareShelf(sharedShelf: SharedShelfDto): Result<Unit, DataError.Sync>
    suspend fun unshareShelf(shareCode: String): Result<Unit, DataError.Sync>
    suspend fun getSharedShelf(shareCode: String): Result<SharedShelfDto?, DataError.Sync>
    suspend fun subscribeToShelf(shareCode: String, userId: String): Result<Unit, DataError.Sync>
    suspend fun unsubscribeFromShelf(shareCode: String, userId: String): Result<Unit, DataError.Sync>
    suspend fun deleteAllBookshelves(userId: String): Result<Unit, DataError.Sync>
}
