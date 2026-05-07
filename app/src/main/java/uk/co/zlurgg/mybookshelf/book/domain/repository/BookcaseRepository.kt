package uk.co.zlurgg.mybookshelf.book.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
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
     * Adds a system shelf (e.g., tutorial shelf) with SystemOwnerIds.TUTORIAL as owner.
     * System shelves are visible to all users and not synced to cloud.
     */
    suspend fun addSystemShelf(shelf: Bookshelf): Result<Unit, DataError.Local>

    /**
     * Deletes all book club shelves owned by the given user, including their cross-refs.
     * Used during sign-out and account deletion to clean up identity-bound club data.
     */
    suspend fun deleteClubShelves(userId: String): Result<Unit, DataError.Local>
}
