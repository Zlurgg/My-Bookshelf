package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.toBook
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.toBookEntity
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.model.SystemOwnerIds
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.map
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

class BookRepositoryImpl(
    private val remoteBookDataSource: RemoteBookDataSource,
    private val dao: BookshelfDao,
    private val currentUserProvider: CurrentUserProvider,
    private val timeProvider: TimeProvider
) : BookRepository {

    override suspend fun getBookById(bookId: String): Book? {
        return dao.getBookById(bookId)?.toBook()
    }

    override suspend fun upsertBook(book: Book) {
        val ownerId = currentUserProvider.getCurrentUserId()
        dao.upsertBookWithSyncInit(
            book.toBookEntity(ownerId),
            timeProvider.currentTimeMillis()
        )
    }

    override suspend fun deleteBook(bookId: String) {
        dao.deleteBook(bookId)
    }

    override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
        return remoteBookDataSource.getBookDetails(bookId)
            .map { bookDetails -> bookDetails.description }
    }

    override suspend fun upsertSystemBook(book: Book) {
        // System books are never synced to cloud - set syncStatus = "SYNCED" to exclude from sync queries
        val entity = book.toBookEntity(ownerId = SystemOwnerIds.TUTORIAL)
            .copy(syncStatus = "SYNCED")
        dao.upsert(entity)
    }
}
