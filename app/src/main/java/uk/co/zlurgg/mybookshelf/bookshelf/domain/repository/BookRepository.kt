package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

interface BookRepository {
    // Individual book operations
    suspend fun getBookById(bookId: String): Book?
    suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote>
    suspend fun upsertBook(book: Book)
    
    // Library membership operations (shelf-agnostic)
    fun isBookInAnyShelf(bookId: String): Flow<Boolean>
    fun getShelvesForBook(bookId: String): Flow<List<String>>
    
    // Shelf membership operations (book-centric)
    fun isBookOnShelf(bookId: String, shelfId: String): Flow<Boolean>
    suspend fun addBookToShelf(bookId: String, shelfId: String)
    suspend fun removeBookFromShelf(bookId: String, shelfId: String)
}