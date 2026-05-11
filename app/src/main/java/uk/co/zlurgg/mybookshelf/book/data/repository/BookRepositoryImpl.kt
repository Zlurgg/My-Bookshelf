package uk.co.zlurgg.mybookshelf.book.data.repository

import uk.co.zlurgg.mybookshelf.book.data.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBook
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBookEntity
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.map

class BookRepositoryImpl(
    private val remoteBookDataSource: RemoteBookDataSource,
    private val dao: BookshelfDao,
) : BookRepository {

    override suspend fun getBookById(bookId: String): Result<Book?, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.getBookById(bookId)?.toBook()
        }
    }

    override suspend fun upsertBook(book: Book): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.upsert(book.toBookEntity())
        }
    }

    override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
        return remoteBookDataSource.getBookDetails(bookId)
            .map { bookDetails -> bookDetails.description }
    }

    override suspend fun upsertSystemBook(book: Book): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.upsert(book.toBookEntity())
        }
    }

    companion object {
        private const val TAG = "BookRepository"
    }
}
