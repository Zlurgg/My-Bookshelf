package uk.co.zlurgg.mybookshelf.book.data.repository

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.book.data.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBook
import uk.co.zlurgg.mybookshelf.book.data.mappers.toBookEntity
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.book.domain.service.BookColorGenerator
import uk.co.zlurgg.mybookshelf.core.data.database.dao.BookshelfDao
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class BookRepositoryImpl(
    private val remoteBookDataSource: RemoteBookDataSource,
    private val dao: BookshelfDao,
) : BookRepository {

    private val previewCache = ConcurrentHashMap<String, Book>()

    override suspend fun getBookById(bookId: String): Result<Book?, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.getBookById(bookId)?.toBook()
        }
    }

    override fun cacheSearchPreviews(books: List<Book>) {
        // Each search supersedes the previous: only the currently-visible
        // result set is reachable through the search dialog, so older entries
        // are dead weight. Bounds the cache at one search worth of books.
        previewCache.clear()
        books.forEach { previewCache[it.id] = it }
    }

    override fun peekPreview(bookId: String): Book? = previewCache[bookId]

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

    override suspend fun getBookDescription(
        bookId: String,
        provider: BookProvider
    ): Result<String?, DataError.Remote> {
        return remoteBookDataSource.getBookDescription(bookId, provider)
    }

    override suspend fun updateDescription(
        bookId: String,
        description: String?
    ): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.updateDescription(bookId, description)
        }
    }

    override suspend fun updatePersonalMetadata(
        bookId: String,
        readingStatus: String?,
        personalRating: Float?,
        personalNotes: String?,
    ): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.updatePersonalMetadata(
                id = bookId,
                readingStatus = readingStatus,
                personalRating = personalRating,
                personalNotes = personalNotes,
            )
        }
    }

    override suspend fun updatePurchased(
        bookId: String,
        purchased: Boolean,
    ): Result<Unit, DataError.Local> {
        return ErrorMapper.safeSuspendCall(TAG) {
            dao.updatePurchased(bookId, purchased)
        }
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
