package uk.co.zlurgg.mybookshelf.book.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.book.data.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBook
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBookEntity
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.book.domain.service.BookColorGenerator
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.result.map as resultMap

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
        val safeBook = if (book.spineColor == 0) {
            book.copy(spineColor = BookColorGenerator.generateSpineColor())
        } else {
            book
        }
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.upsert(safeBook.toBookEntity())
        }
    }

    override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
        return remoteBookDataSource.getBookDetails(bookId)
            .resultMap { bookDetails -> bookDetails.description }
    }

    override suspend fun upsertSystemBook(book: Book): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.upsert(book.toBookEntity())
        }
    }

    override fun getAllPersonalBooks(): Flow<List<Book>> {
        return dao.getAllPersonalBooks().map { entities ->
            entities.map { it.toBook() }
        }
    }

    override suspend fun deleteBooks(bookIds: List<String>): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            bookIds.chunked(SQLITE_BATCH_SIZE).forEach { batch ->
                dao.deleteBooks(batch)
            }
        }
    }

    override fun getNonRemovableBookIds(): Flow<Set<String>> {
        return dao.getBookIdsOnClubShelves().map { it.toSet() }
    }

    companion object {
        private const val TAG = "BookRepository"
        private const val SQLITE_BATCH_SIZE = 500
    }
}
