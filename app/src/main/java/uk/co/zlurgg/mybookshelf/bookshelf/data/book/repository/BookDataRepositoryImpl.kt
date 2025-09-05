package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDao
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBook
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBookEntity
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookDataRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result
import uk.co.zlurgg.mybookshelf.core.domain.map

class BookDataRepositoryImpl(
    private val remoteBookDataSource: RemoteBookDataSource,
    private val dao: BookshelfDao
) : BookDataRepository {
    
    override suspend fun upsertBook(book: Book) {
        dao.upsert(book.toBookEntity())
    }
    
    override suspend fun getBookById(bookId: String): Book? {
        return dao.getBookById(bookId)?.toBook()
    }
    
    override suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote> {
        return remoteBookDataSource.getBookDetails(bookId)
            .map { bookDetails -> bookDetails.description }
    }
    
    override suspend fun addBookToShelf(shelfId: String, bookId: String) {
        dao.upsertCrossRef(
            BookshelfBookCrossRef(
                shelfId = shelfId,
                bookId = bookId,
                addedAt = System.currentTimeMillis()
            )
        )
    }
    
    override suspend fun removeBookFromShelf(shelfId: String, bookId: String) {
        dao.deleteCrossRef(shelfId, bookId)
    }
}