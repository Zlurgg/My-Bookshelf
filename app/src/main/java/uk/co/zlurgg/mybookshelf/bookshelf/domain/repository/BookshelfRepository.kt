package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book

interface BookshelfRepository {
    // Book-shelf relationship operations
    suspend fun addBookToShelf(shelfId: String, bookId: String)
    suspend fun removeBookFromShelf(shelfId: String, bookId: String)
    fun getBooksForShelf(shelfId: String): Flow<List<Book>>
    
    // Book library membership queries
    fun isBookInAnyShelf(bookId: String): Flow<Boolean>
    fun isBookOnShelf(bookId: String, shelfId: String): Flow<Boolean>
    fun getShelvesForBook(bookId: String): Flow<List<String>>
}