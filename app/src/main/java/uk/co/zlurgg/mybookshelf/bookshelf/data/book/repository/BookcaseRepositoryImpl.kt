package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.toDomain
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.toEntity
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.domain.model.SystemOwnerIds
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

class BookcaseRepositoryImpl(
    private val dao: BookshelfDao,
    private val currentUserProvider: CurrentUserProvider,
    private val timeProvider: TimeProvider,
) : BookcaseRepository {
    override fun getAllShelves(): Flow<List<Bookshelf>> {
        val userId = currentUserProvider.getCurrentUserId()
        return dao.getShelvesForUser(userId).map { list -> list.map { it.toDomain() } }
    }

    override fun getBookCountForShelf(shelfId: String): Flow<Int> = dao.getBookCountForShelf(shelfId)

    override suspend fun getShelfById(shelfId: String): Bookshelf? = dao.getShelfById(shelfId)?.toDomain()

    override suspend fun addShelf(shelf: Bookshelf) {
        val ownerId = currentUserProvider.getCurrentUserId()
        dao.upsertShelfWithSyncInit(
            shelf.toEntity(ownerId),
            timeProvider.currentTimeMillis(),
        )
    }

    override suspend fun removeShelf(shelfId: String) {
        val timestamp = timeProvider.currentTimeMillis()
        // Soft delete: mark as DELETED so SyncEngine can push delete to Firestore
        // SyncEngine will hard delete after successful remote delete
        dao.markAllCrossRefsForShelfAs(shelfId, "DELETED", timestamp)
        dao.updateShelfSyncStatus(shelfId, "DELETED", timestamp)
    }

    override suspend fun hardDeleteShelf(shelfId: String) {
        // Hard delete: immediately remove from Room database
        // Use for book clubs where Firestore is source of truth
        dao.deleteAllCrossRefsForShelf(shelfId)
        dao.deleteShelf(shelfId)
    }

    override suspend fun updateShelf(shelf: Bookshelf) {
        val ownerId = currentUserProvider.getCurrentUserId()
        dao.upsertShelfWithSyncInit(
            shelf.toEntity(ownerId),
            timeProvider.currentTimeMillis(),
        )
    }

    override suspend fun addSystemShelf(shelf: Bookshelf) {
        // System shelves are never synced to cloud - set syncStatus = "SYNCED" to exclude from sync queries
        val entity =
            shelf.toEntity(ownerId = SystemOwnerIds.TUTORIAL)
                .copy(syncStatus = "SYNCED")
        dao.upsertShelf(entity)
    }
}
