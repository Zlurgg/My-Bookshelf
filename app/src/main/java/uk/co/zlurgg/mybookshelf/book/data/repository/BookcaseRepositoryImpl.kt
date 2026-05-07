package uk.co.zlurgg.mybookshelf.book.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.book.data.mappers.toDomain
import uk.co.zlurgg.mybookshelf.book.data.mappers.toEntity
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.model.SystemOwnerIds
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

class BookcaseRepositoryImpl(
    private val dao: BookshelfDao,
    private val currentUserProvider: CurrentUserProvider,
    private val timeProvider: TimeProvider
) : BookcaseRepository {

    override fun getAllShelves(): Flow<List<Bookshelf>> {
        val userId = currentUserProvider.getCurrentUserId()
        return dao.getShelvesForUser(userId).map { list -> list.map { it.toDomain() } }
    }

    override fun getBookCountForShelf(shelfId: String): Flow<Int> =
        dao.getBookCountForShelf(shelfId)

    override suspend fun getShelfById(shelfId: String): Result<Bookshelf?, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.getShelfById(shelfId)?.toDomain()
        }
    }

    override suspend fun addShelf(shelf: Bookshelf): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            val ownerId = currentUserProvider.getCurrentUserId()
            dao.upsertShelfWithSyncInit(
                shelf.toEntity(ownerId),
                timeProvider.currentTimeMillis()
            )
        }
    }

    override suspend fun removeShelf(shelfId: String): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            val timestamp = timeProvider.currentTimeMillis()
            // Soft delete: mark as DELETED so SyncEngine can push delete to Firestore
            // SyncEngine will hard delete after successful remote delete
            dao.markAllCrossRefsForShelfAs(shelfId, "DELETED", timestamp)
            dao.updateShelfSyncStatus(shelfId, "DELETED", timestamp)
        }
    }

    override suspend fun hardDeleteShelf(shelfId: String): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            // Hard delete: immediately remove from Room database
            // Use for book clubs where Firestore is source of truth
            dao.deleteAllCrossRefsForShelf(shelfId)
            dao.deleteShelf(shelfId)
        }
    }

    override suspend fun updateShelf(shelf: Bookshelf): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            val ownerId = currentUserProvider.getCurrentUserId()
            dao.upsertShelfWithSyncInit(
                shelf.toEntity(ownerId),
                timeProvider.currentTimeMillis()
            )
        }
    }

    override suspend fun addSystemShelf(shelf: Bookshelf): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            // System shelves are never synced to cloud - set syncStatus = "SYNCED" to exclude from sync queries
            val entity = shelf.toEntity(ownerId = SystemOwnerIds.TUTORIAL)
                .copy(syncStatus = "SYNCED")
            dao.upsertShelf(entity)
        }
    }

    override suspend fun clearUserData(userId: String): Result<Int, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            // Count items before deletion
            val shelves = dao.getShelvesByOwner(userId)
            val books = dao.getBooksByOwner(userId)
            val totalItems = shelves.size + books.size

            // Delete in correct order to respect foreign key constraints:
            // 1. Cross-refs first (references both shelves and books)
            // 2. Books second
            // 3. Shelves last
            dao.deleteAllCrossRefsForOwner(userId)
            dao.deleteAllBooksForOwner(userId)
            dao.deleteAllShelvesForOwner(userId)

            totalItems
        }
    }

    override suspend fun revertUserDataToGuest(userId: String): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.revertAllUserDataToGuest(userId)
        }
    }

    override suspend fun revertOrphanedDataToGuest(): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.revertOrphanedDataToGuest()
        }
    }

    override suspend fun deleteClubShelves(userId: String): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            // Cross-refs first (no FK CASCADE), then shelves
            dao.deleteCrossRefsForClubShelves(userId)
            dao.deleteClubShelvesForOwner(userId)
        }
    }

    companion object {
        private const val TAG = "BookcaseRepository"
    }
}
