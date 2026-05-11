package uk.co.zlurgg.mybookshelf.testutil.mocks

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Reusable stub [ClubOperations] for tests.
 * Constructor params control return values; defaults return errors for most operations.
 * When ClubOperations gains new methods, only this file needs updating.
 */
class StubClubOperations(
    private val clearAllMembershipsResult: Result<Unit, DataError.Local> = Result.Success(Unit),
    private val deleteUserDocumentResult: Result<Unit, DataError.Sync> = Result.Success(Unit),
    private val syncBookToClubResult: Result<Unit, DataError.Sync> = Result.Error(DataError.Sync.UNKNOWN),
    private val leaveBookClubResult: Result<Unit, DataError.Sync> = Result.Error(DataError.Sync.UNKNOWN),
    private val deleteBookClubResult: Result<Unit, DataError.Sync> = Result.Error(DataError.Sync.UNKNOWN),
) : ClubOperations {

    var clearAllMembershipsCalled = false
        private set

    override suspend fun createBookClub(
        name: String,
        shelfStyle: String,
        sourceShelfId: String?,
    ): Result<ClubOperations.BookClubCreationResult, DataError.Sync> =
        Result.Error(DataError.Sync.UNKNOWN)

    override suspend fun lookupBookClub(codeOrUrl: String): ClubOperations.LookupResult =
        ClubOperations.LookupResult.NotFound(DataError.Sync.CLUB_NOT_FOUND)

    override suspend fun joinBookClub(): Result<ClubOperations.JoinResult, DataError.Sync> =
        Result.Error(DataError.Sync.UNKNOWN)

    override suspend fun joinBookClub(code: String): Result<ClubOperations.JoinResult, DataError.Sync> =
        Result.Error(DataError.Sync.UNKNOWN)

    override fun clearLookupState() = Unit

    override suspend fun syncBooksFromClub(
        clubCode: String,
        localShelfId: String,
    ): Result<ClubOperations.SyncResult, DataError.Sync> =
        Result.Error(DataError.Sync.UNKNOWN)

    override suspend fun leaveBookClub(shelfId: String): Result<Unit, DataError.Sync> =
        leaveBookClubResult

    override suspend fun validateMemberships(): List<String> = emptyList()

    override suspend fun deleteBookClub(clubCode: String): Result<Unit, DataError.Sync> =
        deleteBookClubResult

    override suspend fun syncBookToClub(
        clubCode: String,
        book: Book,
    ): Result<Unit, DataError.Sync> = syncBookToClubResult

    override suspend fun removeBookFromClub(
        clubCode: String,
        bookId: String,
    ): Result<Unit, DataError.Sync> = Result.Error(DataError.Sync.UNKNOWN)

    override suspend fun updateClubStyle(
        clubCode: String,
        styleName: String,
    ): Result<Unit, DataError.Sync> = Result.Error(DataError.Sync.UNKNOWN)

    override suspend fun clearAllMemberships(): Result<Unit, DataError.Local> {
        clearAllMembershipsCalled = true
        return clearAllMembershipsResult
    }

    override suspend fun renameBookClub(
        clubCode: String,
        newName: String,
    ): Result<Unit, DataError> = Result.Error(DataError.Sync.UNKNOWN)

    override suspend fun getClubsCreatedByUser(userId: String): Result<List<String>, DataError.Sync> =
        Result.Success(emptyList())

    override suspend fun getClubMembershipsForUser(userId: String): Result<List<String>, DataError.Sync> =
        Result.Success(emptyList())

    override suspend fun removeUserFromClub(
        clubCode: String,
        userId: String,
    ): Result<Unit, DataError.Sync> = Result.Error(DataError.Sync.UNKNOWN)

    override suspend fun deleteUserDocument(userId: String): Result<Unit, DataError.Sync> =
        deleteUserDocumentResult
}
