package uk.co.zlurgg.mybookshelf.book.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBook
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

class BookshelfRepositoryImpl(
    private val dao: BookshelfDao,
    private val timeProvider: TimeProvider
) : BookshelfRepository {

    override suspend fun addBookToShelf(
        shelfId: String,
        bookId: String,
        addedByUserId: String?,
    ): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            val now = timeProvider.currentTimeMillis()
            dao.upsertCrossRef(
                BookshelfBookCrossRef(
                    shelfId = shelfId,
                    bookId = bookId,
                    addedAt = now,
                    addedByUserId = addedByUserId
                )
            )
        }
    }

    override suspend fun removeBookFromShelf(shelfId: String, bookId: String): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.deleteCrossRef(shelfId, bookId)
        }
    }

    override suspend fun getAddedByUserId(shelfId: String, bookId: String): Result<String?, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.getAddedByUserId(shelfId, bookId)
        }
    }

    companion object {
        private const val TAG = "BookshelfRepository"
    }

    override fun getBooksForShelf(shelfId: String): Flow<List<Book>> {
        return dao.getBooksForShelf(shelfId).map { list ->
            list.map { it.toBook() }
        }
    }

    override fun isBookInAnyShelf(bookId: String): Flow<Boolean> {
        return dao.isBookInAnyShelf(bookId)
    }

    override fun isBookOnShelf(bookId: String, shelfId: String): Flow<Boolean> {
        return dao.getBooksForShelf(shelfId).map { books ->
            books.any { it.id == bookId }
        }
    }

    override fun getShelvesForBook(bookId: String): Flow<List<String>> {
        return dao.getShelvesForBook(bookId)
    }
}
