package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.EmptyResult
import uk.co.zlurgg.mybookshelf.core.domain.Result

interface BookRepository {
    suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote>
    suspend fun getBookDescription(bookId: String): Result<String?, DataError>
    fun getBookshelfBooks(): Flow<List<Book>>
    fun isBookOnBookshelf(id: String): Flow<Boolean>
    suspend fun addBookToBookshelf(book: Book): EmptyResult<DataError.Local>
    suspend fun removeFromBookshelf(id: String)
}