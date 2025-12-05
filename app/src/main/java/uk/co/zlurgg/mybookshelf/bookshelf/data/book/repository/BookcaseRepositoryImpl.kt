package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toDomain
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toEntity
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository

class BookcaseRepositoryImpl(
    private val dao: BookshelfDao,
    private val currentUserProvider: CurrentUserProvider
): BookcaseRepository {

    override fun getAllShelves(): Flow<List<Bookshelf>> {
        val userId = currentUserProvider.getCurrentUserId()
        return dao.getShelvesForUser(userId).map { list -> list.map { it.toDomain() } }
    }

    override fun getBookCountForShelf(shelfId: String): Flow<Int> =
        dao.getBookCountForShelf(shelfId)

    override suspend fun getShelfById(shelfId: String): Bookshelf? =
        dao.getShelfById(shelfId)?.toDomain()

    override suspend fun addShelf(shelf: Bookshelf) {
        dao.upsertShelf(shelf.toEntity())
    }

    override suspend fun removeShelf(shelfId: String) {
        // Remove cross-refs first to keep DB clean
        dao.deleteAllCrossRefsForShelf(shelfId)
        dao.deleteShelf(shelfId)
    }

    override suspend fun updateShelf(shelf: Bookshelf) {
        dao.upsertShelf(shelf.toEntity())
    }
}