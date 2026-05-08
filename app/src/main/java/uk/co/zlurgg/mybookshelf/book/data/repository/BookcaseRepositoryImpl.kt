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

class BookcaseRepositoryImpl(
    private val dao: BookshelfDao,
    private val currentUserProvider: CurrentUserProvider,
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
            dao.upsertShelf(shelf.toEntity(ownerId))
        }
    }

    override suspend fun removeShelf(shelfId: String): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.deleteAllCrossRefsForShelf(shelfId)
            dao.deleteShelf(shelfId)
        }
    }

    override suspend fun updateShelf(shelf: Bookshelf): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            val ownerId = currentUserProvider.getCurrentUserId()
            dao.upsertShelf(shelf.toEntity(ownerId))
        }
    }

    override suspend fun addSystemShelf(shelf: Bookshelf): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            val entity = shelf.toEntity(ownerId = SystemOwnerIds.TUTORIAL)
            dao.upsertShelf(entity)
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
