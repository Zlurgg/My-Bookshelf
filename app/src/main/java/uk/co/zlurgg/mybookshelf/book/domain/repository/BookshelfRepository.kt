package uk.co.zlurgg.mybookshelf.book.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

interface BookshelfRepository {
    // Book-shelf relationship operations
    suspend fun addBookToShelf(
        shelfId: String,
        bookId: String,
        addedByUserId: String? = null,
    ): Result<Unit, DataError.Local>
    suspend fun removeBookFromShelf(shelfId: String, bookId: String): Result<Unit, DataError.Local>
    suspend fun getAddedByUserId(shelfId: String, bookId: String): Result<String?, DataError.Local>
    fun getBooksForShelf(shelfId: String): Flow<List<Book>>

    // Book library membership queries
    fun isBookInAnyShelf(bookId: String): Flow<Boolean>
    fun isBookOnShelf(bookId: String, shelfId: String): Flow<Boolean>
    fun getShelvesForBook(bookId: String): Flow<List<String>>
}
