package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

interface BookDataRepository {
    suspend fun upsertBook(book: Book)
    suspend fun getBookById(bookId: String): Book?
    suspend fun getBookDescription(bookId: String): Result<String?, DataError.Remote>
    suspend fun addBookToShelf(shelfId: String, bookId: String)
    suspend fun removeBookFromShelf(shelfId: String, bookId: String)
}