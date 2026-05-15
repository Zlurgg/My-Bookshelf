package uk.co.zlurgg.mybookshelf.core.data.database.dao

import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfBookCrossRef

/**
 * Book-shelf cross-reference operations: relationships, queries.
 */
interface CrossRefDao {
    @Upsert
    suspend fun upsertCrossRef(crossRef: BookshelfBookCrossRef)

    @Query("DELETE FROM BookshelfBookCrossRef WHERE shelfId = :shelfId AND bookId = :bookId")
    suspend fun deleteCrossRef(shelfId: String, bookId: String)

    @Query("DELETE FROM BookshelfBookCrossRef WHERE shelfId = :shelfId")
    suspend fun deleteAllCrossRefsForShelf(shelfId: String)

    @Query(
        """
        SELECT b.* FROM BookEntity b
        INNER JOIN BookshelfBookCrossRef s ON b.id = s.bookId
        WHERE s.shelfId = :shelfId
        ORDER BY s.addedAt DESC
        """
    )
    fun getBooksForShelf(shelfId: String): Flow<List<BookEntity>>

    @Query("SELECT COUNT(*) FROM BookshelfBookCrossRef WHERE shelfId = :shelfId")
    fun getBookCountForShelf(shelfId: String): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM BookshelfBookCrossRef WHERE bookId = :bookId)")
    fun isBookInAnyShelf(bookId: String): Flow<Boolean>

    @Query("SELECT shelfId FROM BookshelfBookCrossRef WHERE bookId = :bookId")
    fun getShelvesForBook(bookId: String): Flow<List<String>>

    @Query("SELECT addedByUserId FROM BookshelfBookCrossRef WHERE shelfId = :shelfId AND bookId = :bookId")
    suspend fun getAddedByUserId(shelfId: String, bookId: String): String?

    @Query(
        """
        DELETE FROM BookshelfBookCrossRef
        WHERE shelfId IN (SELECT id FROM BookshelfEntity WHERE ownerId = :userId AND isBookClub = 1)
        """
    )
    suspend fun deleteCrossRefsForClubShelves(userId: String)
}
