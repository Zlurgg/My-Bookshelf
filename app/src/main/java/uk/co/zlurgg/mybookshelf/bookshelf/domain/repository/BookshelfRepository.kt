package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.Book
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

interface BookshelfRepository {
    suspend fun searchBooks(query: String): Result<List<Book>, DataError.Remote>
    suspend fun addBookToShelf(shelfId: String, book: Book)
    suspend fun removeBookFromShelf(shelfId: String, bookId: String)
    fun getBooksForShelf(shelfId: String): Flow<List<Book>>
}