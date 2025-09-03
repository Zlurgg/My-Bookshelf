package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDao
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toDomain
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toEntity
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository

class BookcaseRepositoryImpl(
    private val dao: BookshelfDao
): BookcaseRepository {

    override fun getAllShelves(): Flow<List<Bookshelf>> =
        dao.getAllShelves().map { list -> list.map { it.toDomain() } }

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