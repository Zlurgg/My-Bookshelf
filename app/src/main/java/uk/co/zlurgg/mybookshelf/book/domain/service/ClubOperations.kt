package uk.co.zlurgg.mybookshelf.book.domain.service

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Interface for book club operations consumed by bookshelf screens.
 * Provides a bridge between bookshelf and bookclub without direct dependency.
 *
 * This is a single interface rather than split by consumer because all methods
 * serve "things bookshelf screens need from clubs" — not a God interface.
 * Consumers are injected via Koin and only call the methods they need.
 * If the interface grows beyond ~20 methods, consider splitting by concern
 * (e.g. ClubMembershipOps, ClubSyncOps).
 */
interface ClubOperations {

    data class BookClubCreationResult(
        val clubCode: String
    )

    sealed class LookupResult {
        data class Found(val clubName: String, val clubCode: String, val memberCount: Int) : LookupResult()
        data class NotFound(val error: DataError) : LookupResult()
        data class InvalidCode(val error: DataError.Validation) : LookupResult()
    }

    data class SyncResult(
        val booksAdded: Int,
        val booksRemoved: Int
    )

    sealed class JoinResult {
        data class Success(val shelfName: String) : JoinResult()
        data object AlreadyMember : JoinResult()
    }

    suspend fun createBookClub(
        name: String,
        shelfStyle: String,
        sourceShelfId: String? = null,
    ): Result<BookClubCreationResult, DataError.Sync>

    suspend fun lookupBookClub(codeOrUrl: String): LookupResult

    suspend fun joinBookClub(): Result<JoinResult, DataError.Sync>

    suspend fun joinBookClub(code: String): Result<JoinResult, DataError.Sync>

    fun clearLookupState()

    suspend fun syncBooksFromClub(clubCode: String, localShelfId: String): Result<SyncResult, DataError.Sync>

    suspend fun leaveBookClub(shelfId: String): Result<Unit, DataError.Sync>

    suspend fun validateMemberships(): List<String>

    suspend fun deleteBookClub(clubCode: String): Result<Unit, DataError.Sync>

    suspend fun syncBookToClub(clubCode: String, book: Book): Result<Unit, DataError.Sync>

    suspend fun removeBookFromClub(clubCode: String, bookId: String): Result<Unit, DataError.Sync>

    suspend fun updateClubStyle(clubCode: String, styleName: String): Result<Unit, DataError.Sync>

    suspend fun clearAllMemberships(): Result<Unit, DataError.Local>

    suspend fun renameBookClub(clubCode: String, newName: String): Result<Unit, DataError>

    suspend fun getClubsCreatedByUser(userId: String): Result<List<String>, DataError.Sync>

    suspend fun getClubMembershipsForUser(userId: String): Result<List<String>, DataError.Sync>

    suspend fun removeUserFromClub(clubCode: String, userId: String): Result<Unit, DataError.Sync>
}
