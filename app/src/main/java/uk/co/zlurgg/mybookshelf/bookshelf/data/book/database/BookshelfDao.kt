package uk.co.zlurgg.mybookshelf.bookshelf.data.book.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookshelfDao {
    // Books
    @Upsert
    suspend fun upsert(book: BookEntity)

    @Query("SELECT * FROM BookEntity WHERE id = :id")
    suspend fun getBookById(id: String): BookEntity?

    // Shelves
    @Upsert
    suspend fun upsertShelf(shelf: BookshelfEntity)

    @Query("SELECT * FROM BookshelfEntity ORDER BY name ASC")
    fun getAllShelves(): Flow<List<BookshelfEntity>>

    @Query("DELETE FROM BookshelfEntity WHERE id = :id")
    suspend fun deleteShelf(id: String)

    // Cross-ref
    @Upsert
    suspend fun upsertCrossRef(crossRef: BookshelfBookCrossRef)

    @Query("DELETE FROM BookshelfBookCrossRef WHERE shelfId = :shelfId AND bookId = :bookId")
    suspend fun deleteCrossRef(shelfId: String, bookId: String)

    @Query("DELETE FROM BookshelfBookCrossRef WHERE shelfId = :shelfId")
    suspend fun deleteAllCrossRefsForShelf(shelfId: String)

    // Queries
    @Query(
        "SELECT b.* FROM BookEntity b INNER JOIN BookshelfBookCrossRef s ON b.id = s.bookId WHERE s.shelfId = :shelfId ORDER BY s.addedAt DESC"
    )
    fun getBooksForShelf(shelfId: String): Flow<List<BookEntity>>
}