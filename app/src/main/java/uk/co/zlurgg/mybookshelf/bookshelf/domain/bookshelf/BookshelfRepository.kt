package uk.co.zlurgg.mybookshelf.bookshelf.domain.bookshelf

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.book_detail.Book

interface BookshelfRepository {
    suspend fun searchBooks(query: String): List<Book>

    suspend fun addBookToShelf(shelfId: String, book: Book)

    suspend fun removeBookFromShelf(shelfId: String, bookId: String)

    fun getBooksForShelf(shelfId: String): Flow<List<Book>>

    fun getAllShelves(): Flow<List<Bookshelf>>
}