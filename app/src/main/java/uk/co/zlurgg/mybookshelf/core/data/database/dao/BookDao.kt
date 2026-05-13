package uk.co.zlurgg.mybookshelf.core.data.database.dao

import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity

/**
 * Book entity operations: CRUD.
 */
interface BookDao {
    @Upsert
    suspend fun upsert(book: BookEntity)

    @Query("SELECT * FROM BookEntity WHERE id = :id")
    suspend fun getBookById(id: String): BookEntity?

    @Query(
        """
        SELECT * FROM BookEntity b
        WHERE b.id != 'tutorial-book-welcome'
        AND (
            EXISTS (
                SELECT 1 FROM BookshelfBookCrossRef cr
                INNER JOIN BookshelfEntity s ON cr.shelfId = s.id
                WHERE cr.bookId = b.id AND s.isBookClub = 0 AND s.id != 'shelf-tutorial'
            )
            OR NOT EXISTS (
                SELECT 1 FROM BookshelfBookCrossRef cr WHERE cr.bookId = b.id
            )
        )
        """
    )
    fun getAllPersonalBooks(): Flow<List<BookEntity>>
}
