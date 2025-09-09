package uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfBookCrossRef
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDao
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.mappers.toBook
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.TimeProvider

class BookshelfRepositoryImpl(
    private val dao: BookshelfDao,
    private val timeProvider: TimeProvider
) : BookshelfRepository {

    override suspend fun addBookToShelf(shelfId: String, bookId: String) {
        dao.upsertCrossRef(
            BookshelfBookCrossRef(
                shelfId = shelfId,
                bookId = bookId,
                addedAt = timeProvider.currentTimeMillis()
            )
        )
    }

    override suspend fun removeBookFromShelf(shelfId: String, bookId: String) {
        dao.deleteCrossRef(shelfId, bookId)
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