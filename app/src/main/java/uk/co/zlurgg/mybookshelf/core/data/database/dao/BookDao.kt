package uk.co.zlurgg.mybookshelf.core.data.database.dao

import androidx.room.Query
import androidx.room.Upsert
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity

/**
 * Book entity operations: CRUD.
 */
interface BookDao {
    @Upsert
    suspend fun upsert(book: BookEntity)

    @Query("SELECT * FROM BookEntity WHERE id = :id")
    suspend fun getBookById(id: String): BookEntity?

    @Query("DELETE FROM BookEntity WHERE id = :id")
    suspend fun deleteBook(id: String)
}
