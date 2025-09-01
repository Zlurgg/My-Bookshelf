package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book

interface BookshelfRepository {
    suspend fun searchBooks(query: String): List<Book>
    suspend fun addBookToShelf(shelfId: String, book: Book)
    suspend fun removeBookFromShelf(shelfId: String, bookId: String)
    fun getBooksForShelf(shelfId: String): Flow<List<Book>>
}