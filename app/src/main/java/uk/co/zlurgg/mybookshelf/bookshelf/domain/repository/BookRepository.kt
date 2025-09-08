package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

interface BookRepository {
    // Book CRUD operations
    suspend fun getBookById(bookId: String): Book?
    suspend fun upsertBook(book: Book)
    suspend fun deleteBook(bookId: String)
    
    // Book metadata operations
    suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote>
    
    // Book search
    suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote>
}