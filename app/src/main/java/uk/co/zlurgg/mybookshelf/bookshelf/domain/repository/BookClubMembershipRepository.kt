package uk.co.zlurgg.mybookshelf.bookshelf.domain.repository

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubMembership
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Membership operations: observe, lookup, join, restore, clear.
 */
interface BookClubMembershipRepository {
    fun observeMyBookClubs(): Flow<List<BookClubMembership>>
    suspend fun getLocalShelfForClub(code: String): Bookshelf?
    suspend fun isMemberOfClub(code: String): Result<Boolean, DataError.Sync>
    suspend fun joinBookClub(code: String): Result<String, DataError.Sync>
    suspend fun getRemoteClubMemberships(userId: String): Result<List<String>, DataError.Sync>
    suspend fun restoreClubMembership(code: String): Result<String, DataError.Sync>
    suspend fun clearAllMemberships(): Result<Unit, DataError.Local>
}
