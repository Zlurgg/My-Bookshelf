package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDao
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBook
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookDataRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import uk.co.zlurgg.mybookshelf.core.domain.map

class BookshelfRepositoryImpl(
    private val remoteBookDataSource: RemoteBookDataSource,
    private val bookDataRepository: BookDataRepository,
    private val dao: BookshelfDao
) : BookshelfRepository {

    override suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote> {
        return remoteBookDataSource
            .searchBooks(query)
            .map { dto ->
                dto.results.map { it.toBook() }
            }
    }

    override suspend fun upsertBook(book: Book) {
        bookDataRepository.upsertBook(book)
    }

    override suspend fun getBookById(bookId: String): Book? {
        return bookDataRepository.getBookById(bookId)
    }

    override suspend fun getBookDescription(workId: String): Result<String?, DataError.Remote> {
        return bookDataRepository.getBookDescription(workId)
    }

    override suspend fun addBookToShelf(shelfId: String, book: Book) {
        // Upsert book then link to shelf
        bookDataRepository.upsertBook(book)
        bookDataRepository.addBookToShelf(shelfId, book.id)
    }

    override suspend fun removeBookFromShelf(shelfId: String, bookId: String) {
        bookDataRepository.removeBookFromShelf(shelfId, bookId)
    }

    override fun getBooksForShelf(shelfId: String): Flow<List<Book>> {
        return dao.getBooksForShelf(shelfId).map { list -> list.map { it.toBook() } }
    }
}