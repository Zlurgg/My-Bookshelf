package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDao
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBook
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookDataRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

class BookRepositoryImpl(
    private val bookDataRepository: BookDataRepository,
    private val dao: BookshelfDao
) : BookRepository {

    override suspend fun getBookById(bookId: String): Book? {
        return bookDataRepository.getBookById(bookId)
    }

    override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
        return bookDataRepository.getBookDescription(bookId)
    }

    override suspend fun upsertBook(book: Book) {
        bookDataRepository.upsertBook(book)
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
        val book = bookDataRepository.getBookById(bookId)
        if (book != null) {
            bookDataRepository.addBookToShelf(shelfId, bookId)
        }
    }

    override suspend fun removeBookFromShelf(bookId: String, shelfId: String) {
        bookDataRepository.removeBookFromShelf(shelfId, bookId)
    }
}