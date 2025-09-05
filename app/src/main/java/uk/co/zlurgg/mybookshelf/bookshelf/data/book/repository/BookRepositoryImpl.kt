package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDao
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBook
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBookEntity
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import uk.co.zlurgg.mybookshelf.core.domain.map

class BookRepositoryImpl(
    private val remoteBookDataSource: RemoteBookDataSource,
    private val dao: BookshelfDao,
) : BookRepository {

    override suspend fun getBookById(bookId: String): Book? {
        return dao.getBookById(bookId)?.toBook()
    }

    override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
        return remoteBookDataSource.getBookDetails(bookId)
            .map { bookDetails -> bookDetails.description }
    }

    override suspend fun upsertBook(book: Book) {
        dao.upsert(book.toBookEntity())
    }

    override fun isBookInAnyShelf(bookId: String): Flow<Boolean> {
        return dao.isBookInAnyShelf(bookId)
    }

    override fun getShelvesForBook(bookId: String): Flow<List<String>> {
        return dao.getShelvesForBook(bookId)
    }

    override fun isBookOnShelf(bookId: String, shelfId: String): Flow<Boolean> {
        return dao.getBooksForShelf(shelfId).map { books ->
            books.any { it.id == bookId }
        }
    }

    override suspend fun addBookToShelf(bookId: String, shelfId: String) {
        // Ensure book exists in database first
        val book = dao.getBookById(bookId)
        if (book != null) {
            dao.upsertCrossRef(
                BookshelfBookCrossRef(
                    shelfId = shelfId,
                    bookId = bookId,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun removeBookFromShelf(bookId: String, shelfId: String) {
        dao.deleteCrossRef(shelfId, bookId)
    }
}