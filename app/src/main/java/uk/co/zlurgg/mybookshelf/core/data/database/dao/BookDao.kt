package uk.co.zlurgg.mybookshelf.core.data.database.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity

/**
 * Book entity operations: CRUD.
 */
interface BookDao {
    @Upsert
    suspend fun upsert(book: BookEntity)

    /**
     * INSERT OR IGNORE: writes the book only if no row with the same id already
     * exists. Used by the book-club sync paths
     * ([BookClubRepositoryHelper.downloadClubBooksToShelf] and
     * [BookClubSyncRepositoryImpl.syncFromRemote]) where the book payload comes
     * from Firestore with NO personal metadata. A plain [upsert] there would
     * clobber `personalRating`, `personalNotes`, `readingStatus`, etc. on books
     * the user separately owns. With this no-op-on-exists shape, the user's
     * personal columns survive joining a club that shares an owned book.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(book: BookEntity)

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

    /**
     * Targeted update for the reading-status column only.
     *
     * Same parallel-write rationale as [updateDescription]: a full-row upsert
     * would clobber any in-flight write to another personal-metadata column.
     *
     * `status` is the [ReadingStatus] enum's `.name` — Room cannot convert the
     * enum directly without a registered TypeConverter, and the entity stores
     * the status as `String`.
     */
    @Query("UPDATE BookEntity SET readingStatus = :status WHERE id = :id")
    suspend fun updateReadingStatus(id: String, status: String)

    /**
     * Targeted update for the personal-rating column only.
     *
     * Same parallel-write rationale as [updateDescription].
     */
    @Query("UPDATE BookEntity SET personalRating = :rating WHERE id = :id")
    suspend fun updatePersonalRating(id: String, rating: Float)

    /**
     * Targeted update for the personal-notes column only.
     *
     * Same parallel-write rationale as [updateDescription].
     */
    @Query("UPDATE BookEntity SET personalNotes = :notes WHERE id = :id")
    suspend fun updatePersonalNotes(id: String, notes: String)

    /**
     * Targeted update for the purchased column only.
     *
     * Same parallel-write rationale as [updateDescription].
     */
    @Query("UPDATE BookEntity SET purchased = :purchased WHERE id = :id")
    suspend fun updatePurchased(id: String, purchased: Boolean)

    /**
     * Multi-column personal-metadata update wrapped in a single transaction so a
     * multi-field write keeps the all-or-none atomicity the full-row upsert
     * previously provided. Null parameters mean "leave this column alone."
     *
     * UPDATE on a row that doesn't exist is a silent no-op in SQLite — same
     * shape as [updateDescription], so the preview-cache path doesn't promote
     * a previewed book into local storage.
     */
    @Transaction
    suspend fun updatePersonalMetadata(
        id: String,
        readingStatus: String? = null,
        personalRating: Float? = null,
        personalNotes: String? = null,
    ) {
        readingStatus?.let { updateReadingStatus(id, it) }
        personalRating?.let { updatePersonalRating(id, it) }
        personalNotes?.let { updatePersonalNotes(id, it) }
    }

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
