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

    @Query("SELECT * FROM book_club_memberships")
    fun observeAllMemberships(): Flow<List<BookClubMembershipEntity>>

    @Query("DELETE FROM book_club_memberships WHERE clubCode = :clubCode")
    suspend fun deleteMembership(clubCode: String)

    @Query("DELETE FROM book_club_memberships")
    suspend fun deleteAllMemberships()

    // ========== Shelf and Book Queries ==========

    @Query("SELECT * FROM BookshelfEntity WHERE clubCode = :clubCode")
    suspend fun getShelfByClubCode(clubCode: String): BookshelfEntity?

    @Query("SELECT bookId FROM BookshelfBookCrossRef WHERE shelfId = :shelfId")
    suspend fun getBookIdsForShelf(shelfId: String): List<String>
}
