package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf

interface BookcaseRepository {
    fun getAllShelves(): Flow<List<Bookshelf>>
    fun getBookCountForShelf(shelfId: String): Flow<Int>
    suspend fun getShelfById(shelfId: String): Bookshelf?
    suspend fun addShelf(shelf: Bookshelf)
    suspend fun removeShelf(shelfId: String)
    suspend fun updateShelf(shelf: Bookshelf)

    /**
     * Hard deletes a shelf and its cross-refs immediately from Room.
     * Use for book clubs where Firestore is source of truth and already deleted.
     * Unlike removeShelf() which soft-deletes for sync, this is immediate.
     */
    suspend fun hardDeleteShelf(shelfId: String)

    /**
     * Adds a system shelf (e.g., tutorial shelf) with SystemOwnerIds.TUTORIAL as owner.
     * System shelves are visible to all users and not synced to cloud.
     */
    suspend fun addSystemShelf(shelf: Bookshelf)
}
