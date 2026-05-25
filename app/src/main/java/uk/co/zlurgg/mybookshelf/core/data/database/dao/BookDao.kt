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

    @Query("DELETE FROM BookEntity WHERE id IN (:bookIds)")
    suspend fun deleteBooksById(bookIds: List<String>)

    /**
     * Targeted update for the description column only.
     *
     * Intentionally a column-scoped UPDATE rather than a full-row upsert: callers
     * (notably the book-detail description fetch in [BookRepository.updateDescription])
     * write the description in parallel with debounced personal-metadata writes
     * (notes/rating/status). A full-row upsert here would clobber any in-flight
     * personal-metadata write from the user.
     */
    @Query("UPDATE BookEntity SET description = :description WHERE id = :bookId")
    suspend fun updateDescription(bookId: String, description: String?)

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
