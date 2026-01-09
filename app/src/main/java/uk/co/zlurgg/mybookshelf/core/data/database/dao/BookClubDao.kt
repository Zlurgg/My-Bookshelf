package uk.co.zlurgg.mybookshelf.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookClubMembershipEntity
import uk.co.zlurgg.mybookshelf.core.data.database.entity.BookshelfEntity

@Dao
interface BookClubDao {

    // ========== Membership Operations ==========

    @Upsert
    suspend fun upsertMembership(membership: BookClubMembershipEntity)

    @Query("SELECT * FROM book_club_memberships WHERE clubCode = :clubCode")
    suspend fun getMembershipByClubCode(clubCode: String): BookClubMembershipEntity?

    @Query("SELECT * FROM book_club_memberships WHERE localShelfId = :shelfId")
    suspend fun getMembershipByShelfId(shelfId: String): BookClubMembershipEntity?

    @Query("SELECT * FROM book_club_memberships WHERE syncStatus != 'DELETED'")
    fun observeAllMemberships(): Flow<List<BookClubMembershipEntity>>

    @Query("DELETE FROM book_club_memberships WHERE clubCode = :clubCode")
    suspend fun deleteMembership(clubCode: String)

    @Query("DELETE FROM book_club_memberships")
    suspend fun deleteAllMemberships()

    @Query(
        "UPDATE book_club_memberships SET syncStatus = :status, lastSyncedAt = :timestamp WHERE clubCode = :clubCode"
    )
    suspend fun updateMembershipSyncStatus(clubCode: String, status: String, timestamp: Long)

    // ========== Book Club Shelf Queries ==========

    @Query("SELECT * FROM BookshelfEntity WHERE isBookClub = 1 AND syncStatus != 'DELETED'")
    fun observeBookClubShelves(): Flow<List<BookshelfEntity>>

    @Query("SELECT * FROM BookshelfEntity WHERE clubCode = :clubCode")
    suspend fun getShelfByClubCode(clubCode: String): BookshelfEntity?

    @Query("UPDATE BookshelfEntity SET isBookClub = :isBookClub, clubCode = :clubCode WHERE id = :shelfId")
    suspend fun updateShelfBookClubStatus(shelfId: String, isBookClub: Boolean, clubCode: String?)

    // ========== Book Queries for Club Creation ==========

    /**
     * Gets all book IDs for a shelf (used when creating a book club from existing shelf).
     */
    @Query("SELECT bookId FROM BookshelfBookCrossRef WHERE shelfId = :shelfId AND syncStatus != 'DELETED'")
    suspend fun getBookIdsForShelf(shelfId: String): List<String>

    // ========== Sync Queries ==========

    @Query("SELECT * FROM book_club_memberships WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncMemberships(): List<BookClubMembershipEntity>
}
