package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDao
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBook
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBookEntity
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.util.BookSorter
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookSearchSort
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import uk.co.zlurgg.mybookshelf.core.domain.map

class BookRepositoryImpl(
    private val remoteBookDataSource: RemoteBookDataSource,
    private val dao: BookshelfDao,
    private val bookSorter: BookSorter
) : BookRepository {

    override suspend fun getBookById(bookId: String): Book? {
        return dao.getBookById(bookId)?.toBook()
    }

    override suspend fun upsertBook(book: Book) {
        dao.upsert(book.toBookEntity())
    }

    override suspend fun deleteBook(bookId: String) {
        dao.deleteBook(bookId)
    }

    override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
        return remoteBookDataSource.getBookDetails(bookId)
            .map { bookDetails -> bookDetails.description }
    }

    override suspend fun searchBooks(
        query: String,
        sortBy: BookSearchSort,
        language: String?
    ): Result<List<Book>, DataError.Remote> {
        return remoteBookDataSource.searchBooks(query, language = language)
            .map { dto ->
                val books = dto.results.map { it.toBook() }
                bookSorter.sortBooks(books, sortBy, query)
            }
    }
}