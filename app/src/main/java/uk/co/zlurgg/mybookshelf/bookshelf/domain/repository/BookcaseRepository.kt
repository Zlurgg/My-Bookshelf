package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface BookcaseRepository {
    fun getAllShelves(): Flow<List<Bookshelf>>
    fun getBookCountForShelf(shelfId: String): Flow<Int>
    suspend fun getShelfById(shelfId: String): Result<Bookshelf?, DataError.Local>
    suspend fun addShelf(shelf: Bookshelf): Result<Unit, DataError.Local>
    suspend fun removeShelf(shelfId: String): Result<Unit, DataError.Local>
    suspend fun updateShelf(shelf: Bookshelf): Result<Unit, DataError.Local>

    /**
     * Hard deletes a shelf and its cross-refs immediately from Room.
     * Use for book clubs where Firestore is source of truth and already deleted.
     * Unlike removeShelf() which soft-deletes for sync, this is immediate.
     */
    suspend fun hardDeleteShelf(shelfId: String): Result<Unit, DataError.Local>

    /**
     * Adds a system shelf (e.g., tutorial shelf) with SystemOwnerIds.TUTORIAL as owner.
     * System shelves are visible to all users and not synced to cloud.
     */
    suspend fun addSystemShelf(shelf: Bookshelf): Result<Unit, DataError.Local>

    /**
     * Clears all local data for a user during sign-out.
     * Deletes cross-refs, books, and shelves owned by the user.
     * @return The total count of items deleted (shelves + books)
     */
    suspend fun clearUserData(userId: String): Result<Int, DataError.Local>
}
